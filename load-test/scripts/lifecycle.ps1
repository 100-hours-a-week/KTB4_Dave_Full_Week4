[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidateSet('up', 'down', 'status', 'logs')]
    [string]$Action,

    [ValidatePattern('^[A-Za-z0-9][A-Za-z0-9-]{0,30}$')]
    [string]$RunId,

    [string]$EnvFile,

    [ValidateSet('A', 'B', 'C')]
    [string]$Variant,

    [switch]$AllowDirty
)

$ErrorActionPreference = 'Stop'

$loadTestRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..')).Path
$workspaceRoot = (Resolve-Path -LiteralPath (Join-Path $loadTestRoot '..')).Path
$composeFile = Join-Path $loadTestRoot 'compose.load-test.yaml'
$currentRunFile = Join-Path $loadTestRoot '.current-run'
$currentRunMetadataFile = Join-Path $loadTestRoot '.current-run.json'

if ([string]::IsNullOrWhiteSpace($EnvFile)) {
    $EnvFile = Join-Path $loadTestRoot '.env'
}

if (-not (Test-Path -LiteralPath $EnvFile -PathType Leaf)) {
    throw "Missing load-test environment file: $EnvFile`nCopy load-test/.env.example to load-test/.env and set MYSQL_VERSION first."
}

function Get-EnvValue([string]$Name) {
    $entry = Get-Content -LiteralPath $EnvFile |
        Where-Object { $_ -match "^\s*$([regex]::Escape($Name))\s*=" } |
        Select-Object -Last 1

    if ($null -eq $entry) {
        return $null
    }

    return (($entry -split '=', 2)[1]).Trim().Trim('"').Trim("'")
}

function Get-ProjectName {
    if (-not [string]::IsNullOrWhiteSpace($RunId)) {
        return "ktb-perf-$($RunId.ToLowerInvariant())"
    }

    if (Test-Path -LiteralPath $currentRunFile -PathType Leaf) {
        return (Get-Content -LiteralPath $currentRunFile -Raw).Trim()
    }

    if ($Action -eq 'up') {
        return "ktb-perf-$(Get-Date -Format 'yyyyMMdd-HHmmss')".ToLowerInvariant()
    }

    throw 'No active load-test run was found. Pass -RunId or start the environment first.'
}

function Assert-SafeProjectName([string]$ProjectName) {
    if ($ProjectName -notmatch '^ktb-perf-[a-z0-9][a-z0-9-]{0,30}$') {
        throw "Refusing to manage an unexpected Docker Compose project: $ProjectName"
    }
}

function Invoke-Compose([string[]]$ComposeArguments) {
    & docker compose `
        --env-file $EnvFile `
        --file $composeFile `
        --project-name $projectName `
        @ComposeArguments

    if ($LASTEXITCODE -ne 0) {
        throw "docker compose failed with exit code $LASTEXITCODE"
    }
}

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw 'Docker CLI was not found. Install and start Docker Desktop first.'
}

& docker info --format '{{.ServerVersion}}' | Out-Null
if ($LASTEXITCODE -ne 0) {
    throw 'Docker Desktop is not running or the Docker server is unavailable.'
}

$projectName = Get-ProjectName
Assert-SafeProjectName $projectName

switch ($Action) {
    'up' {
        if ([string]::IsNullOrWhiteSpace($Variant)) {
            throw 'Variant is required for startup. Use -Variant A (main), B (develop/cache off), or C (develop/cache on).'
        }

        if (Test-Path -LiteralPath $currentRunFile -PathType Leaf) {
            $activeProject = (Get-Content -LiteralPath $currentRunFile -Raw).Trim()
            throw "A load-test project is already recorded: $activeProject`nRun lifecycle.ps1 -Action down first."
        }

        $mysqlVersion = Get-EnvValue 'MYSQL_VERSION'
        if ([string]::IsNullOrWhiteSpace($mysqlVersion) -or $mysqlVersion -match '^REPLACE_') {
            throw 'Set MYSQL_VERSION in load-test/.env to the exact MySQL patch version used by RDS.'
        }

        $gitBranch = (& git -C $workspaceRoot branch --show-current).Trim()
        $gitCommit = (& git -C $workspaceRoot rev-parse --short=12 HEAD).Trim()
        $gitStatus = (& git -C $workspaceRoot status --porcelain)
        $isDirty = -not [string]::IsNullOrWhiteSpace(($gitStatus -join "`n"))

        if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($gitCommit)) {
            throw 'Unable to read Git branch and commit metadata.'
        }

        if ($isDirty -and -not $AllowDirty) {
            throw 'The working tree is dirty. Commit the load-test harness before running a comparison, or pass -AllowDirty for a non-comparable diagnostic run.'
        }

        $expectedBranch = if ($Variant -eq 'A') { 'main' } else { 'develop' }
        if ($gitBranch -ne $expectedBranch) {
            throw "Variant $Variant must run from branch '$expectedBranch', but the current branch is '$gitBranch'."
        }

        $cacheEnabled = $Variant -eq 'C'
        $cacheMode = switch ($Variant) {
            'A' { 'absent' }
            'B' { 'off' }
            'C' { 'on' }
        }

        Push-Location $workspaceRoot
        try {
            & (Join-Path $workspaceRoot 'gradlew.bat') bootJar
            if ($LASTEXITCODE -ne 0) {
                throw "Gradle bootJar failed with exit code $LASTEXITCODE"
            }
        }
        finally {
            Pop-Location
        }

        $previousCacheSetting = $env:POPULAR_POST_CACHE_ENABLED
        try {
            $env:POPULAR_POST_CACHE_ENABLED = $cacheEnabled.ToString().ToLowerInvariant()
            Invoke-Compose @('config', '--quiet')
            Set-Content -LiteralPath $currentRunFile -Value $projectName -NoNewline

            $runMetadata = [ordered]@{
                project = $projectName
                variant = $Variant
                branch = $gitBranch
                commit = $gitCommit
                cacheMode = $cacheMode
                cacheEnabled = $cacheEnabled
                workingTreeDirty = $isDirty
                mysqlVersion = $mysqlVersion
                startedAtUtc = [DateTime]::UtcNow.ToString('o')
            }
            $runMetadata | ConvertTo-Json | Set-Content -LiteralPath $currentRunMetadataFile -Encoding UTF8

            Invoke-Compose @('up', '--detach', '--build', '--wait')
        }
        finally {
            if ($null -eq $previousCacheSetting) {
                Remove-Item -Path 'Env:POPULAR_POST_CACHE_ENABLED' -ErrorAction SilentlyContinue
            }
            else {
                $env:POPULAR_POST_CACHE_ENABLED = $previousCacheSetting
            }
        }

        $appPort = Get-EnvValue 'APP_PORT'
        if ([string]::IsNullOrWhiteSpace($appPort)) {
            $appPort = '8080'
        }

        $healthUrl = "http://127.0.0.1:$appPort/posts?page=0&size=1"
        $deadline = (Get-Date).AddSeconds(120)
        $ready = $false

        while ((Get-Date) -lt $deadline) {
            try {
                $response = Invoke-WebRequest -Uri $healthUrl -UseBasicParsing -TimeoutSec 3
                if ($response.StatusCode -eq 200) {
                    $ready = $true
                    break
                }
            }
            catch {
                Start-Sleep -Seconds 2
            }
        }

        if (-not $ready) {
            Invoke-Compose @('logs', '--tail', '200', 'app', 'mysql')
            throw "Application did not become ready within 120 seconds: $healthUrl"
        }

        Write-Output 'Load-test environment is ready.'
        Write-Output "Project: $projectName"
        Write-Output "Variant: $Variant ($gitBranch@$gitCommit, cache=$cacheMode)"
        Write-Output "Base URL: http://127.0.0.1:$appPort"
    }

    'down' {
        Write-Output "Removing load-test containers and volumes for: $projectName"
        Invoke-Compose @('down', '--volumes', '--remove-orphans')

        if (Test-Path -LiteralPath $currentRunFile -PathType Leaf) {
            $activeProject = (Get-Content -LiteralPath $currentRunFile -Raw).Trim()
            if ($activeProject -eq $projectName) {
                Remove-Item -LiteralPath $currentRunFile -Force
                if (Test-Path -LiteralPath $currentRunMetadataFile -PathType Leaf) {
                    Remove-Item -LiteralPath $currentRunMetadataFile -Force
                }
            }
        }

        Write-Output 'The disposable load-test database volume was removed.'
    }

    'status' {
        Invoke-Compose @('ps')
    }

    'logs' {
        Invoke-Compose @('logs', '--tail', '200', '--follow', 'app', 'mysql')
    }
}
