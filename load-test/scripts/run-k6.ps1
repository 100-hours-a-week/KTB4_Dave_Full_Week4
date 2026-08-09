[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidateSet(
        'login',
        'posts-list',
        'post-detail',
        'post-detail-popular',
        'popular-posts',
        'comment-list',
        'comment-list-popular',
        'post-create',
        'comment-create',
        'post-like'
    )]
    [string]$Scenario,

    [Parameter(Mandatory = $true)]
    [ValidatePattern('^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$')]
    [string]$ComparisonId,

    [int[]]$Rps = @(1, 2, 5, 10, 20, 40, 80, 160),

    [ValidatePattern('^[0-9]+[smh]$')]
    [string]$Duration = '45s',

    [switch]$SkipWarmup
)

$ErrorActionPreference = 'Stop'

$loadTestRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..')).Path
$envFile = Join-Path $loadTestRoot '.env'
$currentRunFile = Join-Path $loadTestRoot '.current-run'
$currentRunMetadataFile = Join-Path $loadTestRoot '.current-run.json'
$k6Script = Join-Path $loadTestRoot 'k6/run.js'
$resultsRoot = Join-Path $loadTestRoot 'results'

if (-not (Get-Command k6 -ErrorAction SilentlyContinue)) {
    throw 'k6 was not found on PATH. Install k6 before running endpoint tests.'
}

if (-not (Test-Path -LiteralPath $envFile -PathType Leaf)) {
    throw 'Missing load-test/.env. Copy load-test/.env.example and configure it first.'
}

if (-not (Test-Path -LiteralPath $currentRunFile -PathType Leaf) -or
    -not (Test-Path -LiteralPath $currentRunMetadataFile -PathType Leaf)) {
    throw 'No classified A/B/C load-test environment was found. Run lifecycle.ps1 -Action up -Variant <A|B|C> first.'
}

function Get-EnvValue([string]$Name) {
    $entry = Get-Content -LiteralPath $envFile |
        Where-Object { $_ -match "^\s*$([regex]::Escape($Name))\s*=" } |
        Select-Object -Last 1

    if ($null -eq $entry) {
        return $null
    }

    return (($entry -split '=', 2)[1]).Trim().Trim('"').Trim("'")
}

function Get-Metric($Summary, [string]$MetricName) {
    $property = $Summary.metrics.PSObject.Properties[$MetricName]
    if ($null -eq $property) {
        return $null
    }
    return $property.Value
}

function Get-MetricValue($Metric, [string]$ValueName) {
    if ($null -eq $Metric -or $null -eq $Metric.values) {
        return $null
    }

    $property = $Metric.values.PSObject.Properties[$ValueName]
    if ($null -eq $property) {
        return $null
    }
    return $property.Value
}

function Append-CsvRow([string]$Path, $Row) {
    if (Test-Path -LiteralPath $Path -PathType Leaf) {
        $Row | Export-Csv -LiteralPath $Path -NoTypeInformation -Append -Encoding UTF8
    }
    else {
        $Row | Export-Csv -LiteralPath $Path -NoTypeInformation -Encoding UTF8
    }
}

function Get-HarnessHash {
    $relativePaths = @(
        'compose.load-test.yaml',
        'k6/run.js',
        'mysql/002-seed.sql',
        'scripts/build-cache-comparison.ps1',
        'scripts/lifecycle.ps1',
        'scripts/run-k6.ps1',
        'scripts/run-suite.ps1'
    )
    $hashEntries = foreach ($relativePath in $relativePaths) {
        $absolutePath = Join-Path $loadTestRoot $relativePath
        if (-not (Test-Path -LiteralPath $absolutePath -PathType Leaf)) {
            throw "Missing load-test harness file: $absolutePath"
        }
        "$relativePath=$((Get-FileHash -LiteralPath $absolutePath -Algorithm SHA256).Hash)"
    }

    $algorithm = [System.Security.Cryptography.SHA256]::Create()
    try {
        $bytes = [System.Text.Encoding]::UTF8.GetBytes(($hashEntries -join "`n"))
        return ([BitConverter]::ToString($algorithm.ComputeHash($bytes))).Replace('-', '').ToLowerInvariant()
    }
    finally {
        $algorithm.Dispose()
    }
}

function Start-ContainerMonitor([string]$ContainerId, [string]$OutputPath) {
    return Start-Job -ArgumentList $ContainerId, $OutputPath -ScriptBlock {
        param($MonitoredContainerId, $CsvPath)

        while ($true) {
            $statsJson = & docker stats --no-stream --format '{{json .}}' $MonitoredContainerId 2>$null
            if ($LASTEXITCODE -eq 0 -and -not [string]::IsNullOrWhiteSpace($statsJson)) {
                $stats = $statsJson | ConvertFrom-Json
                $row = [pscustomobject]@{
                    timestamp_utc = [DateTime]::UtcNow.ToString('o')
                    cpu_percent = $stats.CPUPerc
                    memory_usage = $stats.MemUsage
                    memory_percent = $stats.MemPerc
                    network_io = $stats.NetIO
                    block_io = $stats.BlockIO
                    pids = $stats.PIDs
                }

                if (Test-Path -LiteralPath $CsvPath -PathType Leaf) {
                    $row | Export-Csv -LiteralPath $CsvPath -NoTypeInformation -Append -Encoding UTF8
                }
                else {
                    $row | Export-Csv -LiteralPath $CsvPath -NoTypeInformation -Encoding UTF8
                }
            }
            Start-Sleep -Seconds 1
        }
    }
}

function Stop-ContainerMonitor($MonitorJob) {
    if ($null -eq $MonitorJob) {
        return
    }

    Stop-Job -Job $MonitorJob -ErrorAction SilentlyContinue
    Receive-Job -Job $MonitorJob -ErrorAction SilentlyContinue | Out-Null
    Remove-Job -Job $MonitorJob -ErrorAction SilentlyContinue
}

function Write-StageSummaries(
    [string]$ResultFile,
    [string]$ResourceFile,
    [int]$Rate,
    [int]$Attempt,
    [bool]$Passed,
    [string]$StageStartedAt
) {
    if (-not (Test-Path -LiteralPath $ResultFile -PathType Leaf)) {
        Write-Warning "k6 did not create a summary file: $ResultFile"
        return
    }

    $summary = Get-Content -LiteralPath $ResultFile -Raw | ConvertFrom-Json
    $durationMetric = Get-Metric $summary 'http_req_duration'
    $requestMetric = Get-Metric $summary 'http_reqs'
    $failureMetric = Get-Metric $summary 'http_req_failed'
    $checkMetric = Get-Metric $summary 'checks'
    $droppedMetric = Get-Metric $summary 'dropped_iterations'

    $baseRow = [ordered]@{
        comparison_id = $ComparisonId
        variant = $runMetadata.variant
        branch = $runMetadata.branch
        commit = $runMetadata.commit
        cache_mode = $runMetadata.cacheMode
        working_tree_dirty = $runMetadata.workingTreeDirty
        scenario = $Scenario
        requested_rps = $Rate
        attempt = $Attempt
        duration = $Duration
        passed = $Passed
        requests = Get-MetricValue $requestMetric 'count'
        achieved_rps = Get-MetricValue $requestMetric 'rate'
        avg_ms = Get-MetricValue $durationMetric 'avg'
        med_ms = Get-MetricValue $durationMetric 'med'
        p90_ms = Get-MetricValue $durationMetric 'p(90)'
        p95_ms = Get-MetricValue $durationMetric 'p(95)'
        p99_ms = Get-MetricValue $durationMetric 'p(99)'
        max_ms = Get-MetricValue $durationMetric 'max'
        error_rate = Get-MetricValue $failureMetric 'rate'
        check_rate = Get-MetricValue $checkMetric 'rate'
        dropped_iterations = Get-MetricValue $droppedMetric 'count'
        started_at_utc = $StageStartedAt
        raw_result = $ResultFile
        resource_samples = $ResourceFile
    }
    Append-CsvRow $overallSummaryFile ([pscustomobject]$baseRow)

    $cacheMetricNames = @{
        'popular-posts' = @{
            duration = 'cache_popular_list_duration'
            failed = 'cache_popular_list_failed'
            requests = 'cache_popular_list_requests'
            path = 'popular-list'
        }
        'post-detail-popular' = @{
            duration = 'cache_popular_detail_duration'
            failed = 'cache_popular_detail_failed'
            requests = 'cache_popular_detail_requests'
            path = 'popular-detail'
        }
        'comment-list-popular' = @{
            duration = 'cache_popular_comments_duration'
            failed = 'cache_popular_comments_failed'
            requests = 'cache_popular_comments_requests'
            path = 'popular-comments'
        }
    }

    if (-not $cacheMetricNames.ContainsKey($Scenario)) {
        return
    }

    $cacheNames = $cacheMetricNames[$Scenario]
    $cacheDuration = Get-Metric $summary $cacheNames.duration
    $cacheFailures = Get-Metric $summary $cacheNames.failed
    $cacheRequests = Get-Metric $summary $cacheNames.requests

    $cacheRow = [ordered]@{
        comparison_id = $ComparisonId
        variant = $runMetadata.variant
        branch = $runMetadata.branch
        commit = $runMetadata.commit
        cache_mode = $runMetadata.cacheMode
        working_tree_dirty = $runMetadata.workingTreeDirty
        cache_path = $cacheNames.path
        scenario = $Scenario
        requested_rps = $Rate
        attempt = $Attempt
        duration = $Duration
        passed = $Passed
        requests = Get-MetricValue $cacheRequests 'count'
        achieved_rps = Get-MetricValue $cacheRequests 'rate'
        avg_ms = Get-MetricValue $cacheDuration 'avg'
        med_ms = Get-MetricValue $cacheDuration 'med'
        p90_ms = Get-MetricValue $cacheDuration 'p(90)'
        p95_ms = Get-MetricValue $cacheDuration 'p(95)'
        p99_ms = Get-MetricValue $cacheDuration 'p(99)'
        max_ms = Get-MetricValue $cacheDuration 'max'
        error_rate = Get-MetricValue $cacheFailures 'rate'
        dropped_iterations = Get-MetricValue $droppedMetric 'count'
        started_at_utc = $StageStartedAt
        raw_result = $ResultFile
        resource_samples = $ResourceFile
    }
    Append-CsvRow $cacheSummaryFile ([pscustomobject]$cacheRow)
}

$appPort = Get-EnvValue 'APP_PORT'
if ([string]::IsNullOrWhiteSpace($appPort)) {
    $appPort = '8080'
}

$jwtSecret = Get-EnvValue 'JWT_SECRET'
$userPassword = Get-EnvValue 'LOAD_TEST_USER_PASSWORD'
$runMetadata = Get-Content -LiteralPath $currentRunMetadataFile -Raw | ConvertFrom-Json

$comparisonRoot = Join-Path $resultsRoot $ComparisonId
$rawResultRoot = Join-Path $comparisonRoot "raw/$($runMetadata.variant)/$Scenario"
$resourceResultRoot = Join-Path $comparisonRoot "resources/$($runMetadata.variant)/$Scenario"
$metadataRoot = Join-Path $comparisonRoot 'metadata'
$overallSummaryFile = Join-Path $comparisonRoot 'overall.csv'
$cacheSummaryFile = Join-Path $comparisonRoot 'cache-only.csv'

New-Item -ItemType Directory -Force -Path $rawResultRoot, $resourceResultRoot, $metadataRoot | Out-Null

$k6Version = (& k6 version 2>&1 | Select-Object -First 1).ToString().Trim()
if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($k6Version)) {
    throw 'Unable to read the installed k6 version.'
}
$harnessHash = Get-HarnessHash

$existingMetadataFiles = @(Get-ChildItem -LiteralPath $metadataRoot -Filter '*.json' -File -ErrorAction SilentlyContinue |
    Where-Object { $_.Name -notlike 'scenario-*' })
foreach ($existingMetadataFile in $existingMetadataFiles) {
    $existingMetadata = Get-Content -LiteralPath $existingMetadataFile.FullName -Raw | ConvertFrom-Json

    if ($existingMetadata.mysqlVersion -ne $runMetadata.mysqlVersion) {
        throw "Comparison '$ComparisonId' already contains a different MySQL version: $($existingMetadata.mysqlVersion)"
    }

    if ($existingMetadata.variant -eq $runMetadata.variant -and
        $existingMetadata.commit -ne $runMetadata.commit) {
        throw "Variant $($runMetadata.variant) is already recorded at commit $($existingMetadata.commit). Use a new ComparisonId."
    }

    if ($runMetadata.variant -in @('B', 'C') -and
        $existingMetadata.variant -in @('B', 'C') -and
        $existingMetadata.commit -ne $runMetadata.commit) {
        throw "Variants B and C must use the same develop commit. Existing: $($existingMetadata.commit), current: $($runMetadata.commit)"
    }

    if ($existingMetadata.k6Version -and $existingMetadata.k6Version -ne $k6Version) {
        throw "Comparison '$ComparisonId' already contains a different k6 version: $($existingMetadata.k6Version)"
    }

    if ($existingMetadata.harnessHash -and $existingMetadata.harnessHash -ne $harnessHash) {
        throw "Comparison '$ComparisonId' already contains a different load-test harness. Apply the same harness commit to every variant."
    }
}

$scenarioConfigFile = Join-Path $metadataRoot "scenario-$Scenario.json"
$rpsKey = $Rps -join ','
$warmupEnabled = -not $SkipWarmup
if (Test-Path -LiteralPath $scenarioConfigFile -PathType Leaf) {
    $existingScenarioConfig = Get-Content -LiteralPath $scenarioConfigFile -Raw | ConvertFrom-Json
    $existingRpsKey = @($existingScenarioConfig.rps) -join ','

    if ($existingRpsKey -ne $rpsKey -or
        $existingScenarioConfig.duration -ne $Duration -or
        [bool]$existingScenarioConfig.warmupEnabled -ne $warmupEnabled) {
        throw "Scenario '$Scenario' already has different RPS, duration, or warm-up settings in comparison '$ComparisonId'."
    }
}
else {
    [ordered]@{
        scenario = $Scenario
        rps = $Rps
        duration = $Duration
        warmupEnabled = $warmupEnabled
    } | ConvertTo-Json | Set-Content -LiteralPath $scenarioConfigFile -Encoding UTF8
}

$comparisonMetadata = [ordered]@{
    comparisonId = $ComparisonId
    project = $runMetadata.project
    variant = $runMetadata.variant
    branch = $runMetadata.branch
    commit = $runMetadata.commit
    cacheMode = $runMetadata.cacheMode
    cacheEnabled = $runMetadata.cacheEnabled
    workingTreeDirty = $runMetadata.workingTreeDirty
    mysqlVersion = $runMetadata.mysqlVersion
    k6Version = $k6Version
    harnessHash = $harnessHash
    environmentStartedAtUtc = $runMetadata.startedAtUtc
    recordedAtUtc = [DateTime]::UtcNow.ToString('o')
}
$metadataFile = Join-Path $metadataRoot "$($runMetadata.variant)-$($runMetadata.commit).json"
$comparisonMetadata | ConvertTo-Json | Set-Content -LiteralPath $metadataFile -Encoding UTF8

$appContainerId = [string](& docker ps `
    --filter "label=com.docker.compose.project=$($runMetadata.project)" `
    --filter 'label=com.docker.compose.service=app' `
    --format '{{.ID}}' | Select-Object -First 1)

if ($LASTEXITCODE -ne 0) {
    throw 'Unable to query the application container. Is Docker Desktop running?'
}

$appContainerId = $appContainerId.Trim()

if ([string]::IsNullOrWhiteSpace($appContainerId)) {
    throw "The application container is not running for project: $($runMetadata.project)"
}

$trackedEnvironmentNames = @(
    'BASE_URL',
    'SCENARIO',
    'JWT_SECRET',
    'LOAD_TEST_USER_PASSWORD',
    'RPS',
    'DURATION',
    'COMPARISON_ID',
    'TEST_VARIANT',
    'GIT_BRANCH',
    'GIT_COMMIT',
    'CACHE_MODE',
    'SUMMARY_EXPORT_PATH'
)
$previousValues = @{}
foreach ($name in $trackedEnvironmentNames) {
    $previousValues[$name] = [Environment]::GetEnvironmentVariable($name)
}

try {
    $env:BASE_URL = "http://127.0.0.1:$appPort"
    $env:SCENARIO = $Scenario
    $env:JWT_SECRET = $jwtSecret
    $env:LOAD_TEST_USER_PASSWORD = $userPassword
    $env:COMPARISON_ID = $ComparisonId
    $env:TEST_VARIANT = $runMetadata.variant
    $env:GIT_BRANCH = $runMetadata.branch
    $env:GIT_COMMIT = $runMetadata.commit
    $env:CACHE_MODE = $runMetadata.cacheMode

    if (-not $SkipWarmup) {
        Write-Output "Warming up $Scenario at 1 RPS for 15s..."
        $env:RPS = '1'
        $env:DURATION = '15s'
        Remove-Item -Path 'Env:SUMMARY_EXPORT_PATH' -ErrorAction SilentlyContinue
        & k6 run --quiet $k6Script
        if ($LASTEXITCODE -ne 0) {
            throw "Warm-up failed for scenario: $Scenario"
        }
    }

    foreach ($rate in $Rps) {
        if ($rate -lt 1) {
            throw "RPS values must be positive: $rate"
        }

        $stagePassed = $false

        for ($attempt = 1; $attempt -le 2; $attempt++) {
            $env:RPS = [string]$rate
            $env:DURATION = $Duration
            $stageStamp = Get-Date -Format 'yyyyMMdd-HHmmss'
            $stageStartedAt = [DateTime]::UtcNow.ToString('o')
            $fileBase = "$stageStamp-$($rate)rps-attempt$attempt"
            $resultFile = Join-Path $rawResultRoot "$fileBase.json"
            $resourceFile = Join-Path $resourceResultRoot "$fileBase.csv"
            $monitorJob = $null
            $stageExitCode = 1

            Write-Output "Running $Scenario at $rate RPS ($Duration), attempt $attempt..."
            try {
                $monitorJob = Start-ContainerMonitor $appContainerId $resourceFile
                $env:SUMMARY_EXPORT_PATH = $resultFile
                & k6 run $k6Script
                $stageExitCode = $LASTEXITCODE
            }
            finally {
                Stop-ContainerMonitor $monitorJob
            }

            $attemptPassed = $stageExitCode -eq 0
            Write-StageSummaries $resultFile $resourceFile $rate $attempt $attemptPassed $stageStartedAt

            if ($attemptPassed) {
                $stagePassed = $true
                break
            }

            Write-Warning "The $rate RPS stage did not satisfy the error/check thresholds."
        }

        if (-not $stagePassed) {
            Write-Warning "Stopping after two failed attempts at $rate RPS. The previous stage is the last stable stage."
            break
        }
    }
}
finally {
    foreach ($name in $previousValues.Keys) {
        $previousValue = $previousValues[$name]
        if ($null -eq $previousValue) {
            Remove-Item -Path "Env:$name" -ErrorAction SilentlyContinue
        }
        else {
            Set-Item -Path "Env:$name" -Value $previousValue
        }
    }
}

Write-Output "Overall summary: $overallSummaryFile"
if (Test-Path -LiteralPath $cacheSummaryFile -PathType Leaf) {
    Write-Output "Cache-only summary: $cacheSummaryFile"
}
