[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidatePattern('^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$')]
    [string]$ComparisonId
)

$ErrorActionPreference = 'Stop'
$loadTestRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..')).Path
$comparisonRoot = Join-Path (Join-Path $loadTestRoot 'results') $ComparisonId
$cacheSummaryFile = Join-Path $comparisonRoot 'cache-only.csv'
$comparisonFile = Join-Path $comparisonRoot 'cache-abc-comparison.csv'

if (-not (Test-Path -LiteralPath $cacheSummaryFile -PathType Leaf)) {
    Write-Output "No cache-only results exist yet: $cacheSummaryFile"
    return
}

function Get-Median($Values) {
    $numbers = @($Values |
        Where-Object { $null -ne $_ -and "$_" -ne '' } |
        ForEach-Object { [double]$_ } |
        Sort-Object)

    if ($numbers.Count -eq 0) {
        return $null
    }

    $middle = [math]::Floor($numbers.Count / 2)
    if ($numbers.Count % 2 -eq 1) {
        return $numbers[$middle]
    }
    return ($numbers[$middle - 1] + $numbers[$middle]) / 2
}

function Get-VariantAggregate($Rows, [string]$Variant) {
    $variantRows = @($Rows | Where-Object {
        $_.variant -eq $Variant -and $_.passed -eq 'True'
    })

    if ($variantRows.Count -eq 0) {
        return $null
    }

    return [pscustomobject]@{
        samples = $variantRows.Count
        commit = $variantRows[0].commit
        achievedRps = Get-Median ($variantRows | ForEach-Object { $_.achieved_rps })
        avg = Get-Median ($variantRows | ForEach-Object { $_.avg_ms })
        med = Get-Median ($variantRows | ForEach-Object { $_.med_ms })
        p95 = Get-Median ($variantRows | ForEach-Object { $_.p95_ms })
        p99 = Get-Median ($variantRows | ForEach-Object { $_.p99_ms })
        max = Get-Median ($variantRows | ForEach-Object { $_.max_ms })
        errorRate = Get-Median ($variantRows | ForEach-Object { $_.error_rate })
        droppedIterations = Get-Median ($variantRows | ForEach-Object { $_.dropped_iterations })
    }
}

function Get-ImprovementPercent($Before, $After) {
    if ($null -eq $Before -or $null -eq $After -or [double]$Before -eq 0) {
        return $null
    }
    return [math]::Round((([double]$Before - [double]$After) / [double]$Before) * 100, 2)
}

$cacheRows = Import-Csv -LiteralPath $cacheSummaryFile
if ($cacheRows | Where-Object { $_.working_tree_dirty -eq 'True' }) {
    throw "Comparison '$ComparisonId' contains a dirty-working-tree run. Keep its raw records for diagnosis, but use a new ComparisonId for A/B/C comparison."
}

$bCommits = @($cacheRows | Where-Object { $_.variant -eq 'B' } | Select-Object -ExpandProperty commit -Unique)
$cCommits = @($cacheRows | Where-Object { $_.variant -eq 'C' } | Select-Object -ExpandProperty commit -Unique)
if ($bCommits.Count -gt 1 -or $cCommits.Count -gt 1 -or
    ($bCommits.Count -eq 1 -and $cCommits.Count -eq 1 -and $bCommits[0] -ne $cCommits[0])) {
    throw 'Variants B and C must contain results from exactly the same develop commit.'
}

$groupedRows = $cacheRows | Group-Object cache_path, requested_rps
$comparisonRows = foreach ($group in $groupedRows) {
    $first = $group.Group | Select-Object -First 1
    $a = Get-VariantAggregate $group.Group 'A'
    $b = Get-VariantAggregate $group.Group 'B'
    $c = Get-VariantAggregate $group.Group 'C'

    [pscustomobject][ordered]@{
        comparison_id = $ComparisonId
        cache_path = $first.cache_path
        requested_rps = [int]$first.requested_rps
        a_samples = if ($null -eq $a) { 0 } else { $a.samples }
        a_commit = if ($null -eq $a) { $null } else { $a.commit }
        a_achieved_rps = if ($null -eq $a) { $null } else { $a.achievedRps }
        a_avg_ms = if ($null -eq $a) { $null } else { $a.avg }
        a_med_ms = if ($null -eq $a) { $null } else { $a.med }
        a_p95_ms = if ($null -eq $a) { $null } else { $a.p95 }
        a_p99_ms = if ($null -eq $a) { $null } else { $a.p99 }
        a_max_ms = if ($null -eq $a) { $null } else { $a.max }
        a_error_rate = if ($null -eq $a) { $null } else { $a.errorRate }
        a_dropped_iterations = if ($null -eq $a) { $null } else { $a.droppedIterations }
        b_samples = if ($null -eq $b) { 0 } else { $b.samples }
        b_commit = if ($null -eq $b) { $null } else { $b.commit }
        b_achieved_rps = if ($null -eq $b) { $null } else { $b.achievedRps }
        b_avg_ms = if ($null -eq $b) { $null } else { $b.avg }
        b_med_ms = if ($null -eq $b) { $null } else { $b.med }
        b_p95_ms = if ($null -eq $b) { $null } else { $b.p95 }
        b_p99_ms = if ($null -eq $b) { $null } else { $b.p99 }
        b_max_ms = if ($null -eq $b) { $null } else { $b.max }
        b_error_rate = if ($null -eq $b) { $null } else { $b.errorRate }
        b_dropped_iterations = if ($null -eq $b) { $null } else { $b.droppedIterations }
        c_samples = if ($null -eq $c) { 0 } else { $c.samples }
        c_commit = if ($null -eq $c) { $null } else { $c.commit }
        c_achieved_rps = if ($null -eq $c) { $null } else { $c.achievedRps }
        c_avg_ms = if ($null -eq $c) { $null } else { $c.avg }
        c_med_ms = if ($null -eq $c) { $null } else { $c.med }
        c_p95_ms = if ($null -eq $c) { $null } else { $c.p95 }
        c_p99_ms = if ($null -eq $c) { $null } else { $c.p99 }
        c_max_ms = if ($null -eq $c) { $null } else { $c.max }
        c_error_rate = if ($null -eq $c) { $null } else { $c.errorRate }
        c_dropped_iterations = if ($null -eq $c) { $null } else { $c.droppedIterations }
        cache_med_improvement_b_to_c_pct = if ($null -eq $b -or $null -eq $c) { $null } else { Get-ImprovementPercent $b.med $c.med }
        cache_p95_improvement_b_to_c_pct = if ($null -eq $b -or $null -eq $c) { $null } else { Get-ImprovementPercent $b.p95 $c.p95 }
        cache_p99_improvement_b_to_c_pct = if ($null -eq $b -or $null -eq $c) { $null } else { Get-ImprovementPercent $b.p99 $c.p99 }
        total_med_improvement_a_to_c_pct = if ($null -eq $a -or $null -eq $c) { $null } else { Get-ImprovementPercent $a.med $c.med }
        total_p95_improvement_a_to_c_pct = if ($null -eq $a -or $null -eq $c) { $null } else { Get-ImprovementPercent $a.p95 $c.p95 }
        total_p99_improvement_a_to_c_pct = if ($null -eq $a -or $null -eq $c) { $null } else { Get-ImprovementPercent $a.p99 $c.p99 }
    }
}

$comparisonRows |
    Sort-Object cache_path, requested_rps |
    Export-Csv -LiteralPath $comparisonFile -NoTypeInformation -Encoding UTF8

Write-Output "A/B/C cache comparison: $comparisonFile"
