[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidatePattern('^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$')]
    [string]$ComparisonId,

    [ValidateSet('Overall', 'Cache', 'All')]
    [string]$Suite = 'All',

    [int[]]$Rps = @(1, 2, 5, 10, 20, 40, 80, 160),

    [ValidatePattern('^[0-9]+[smh]$')]
    [string]$Duration = '45s'
)

$ErrorActionPreference = 'Stop'
$runK6Script = Join-Path $PSScriptRoot 'run-k6.ps1'
$buildComparisonScript = Join-Path $PSScriptRoot 'build-cache-comparison.ps1'

$overallScenarios = @(
    'login',
    'posts-list',
    'post-detail',
    'popular-posts',
    'comment-list',
    'post-create',
    'comment-create',
    'post-like'
)

$cacheScenarios = @(
    'popular-posts',
    'post-detail-popular',
    'comment-list-popular'
)

$scenarios = switch ($Suite) {
    'Overall' { $overallScenarios }
    'Cache' { $cacheScenarios }
    'All' { $overallScenarios + @('post-detail-popular', 'comment-list-popular') }
}

foreach ($scenario in $scenarios) {
    Write-Output "=== Suite $Suite / scenario $scenario ==="
    & $runK6Script `
        -Scenario $scenario `
        -ComparisonId $ComparisonId `
        -Rps $Rps `
        -Duration $Duration
}

& $buildComparisonScript -ComparisonId $ComparisonId
