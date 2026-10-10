$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$jarFile = Join-Path $projectRoot 'peoplesoft-xml-extractor.jar'
$fixture = Join-Path $projectRoot 'tests\ae-and-sql-variants.xml'
$tempRoot = [IO.Path]::GetFullPath([IO.Path]::GetTempPath())
$output = Join-Path $tempRoot ('peoplesoft-xml-extractor-test-' + [Guid]::NewGuid().ToString('N'))

function Assert-File([string]$RelativePath, [string]$ExpectedText) {
    $path = Join-Path $output $RelativePath
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
        throw "Expected extracted file was not created: $RelativePath"
    }
    $actual = Get-Content -LiteralPath $path -Raw
    if ($actual -notmatch [regex]::Escape($ExpectedText)) {
        throw "Extracted file did not contain the expected text: $RelativePath"
    }
}

try {
    if (-not (Test-Path -LiteralPath $jarFile -PathType Leaf)) {
        throw 'Build the XML extractor before running its tests.'
    }

    & java -jar $jarFile --input $fixture --output $output
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

    Assert-File 'Application Engine PeopleCode\TEST_AE\MAIN\Step01.pcode' 'main section'
    Assert-File 'Application Engine PeopleCode\TEST_AE\AUDIT\Step01.pcode' 'audit section'
    Assert-File 'Application Engine PeopleCode\TEST_AE\MAIN\Step01.USA.pcode' 'USA market'
    Assert-File 'SQL\MULTI_SQL\MULTI_SQL.sql' "SELECT 'generic'"
    Assert-File 'SQL\MULTI_SQL\MULTI_SQL.USA.ORACLE.2024-01-01.sql' "SELECT 'qualified'"
    Assert-File 'SQL\MULTI_SQL\MULTI_SQL.2.sql' "SELECT 'unexpected collision'"
    Assert-File 'SQL_AE\TEST_AE\MAIN\Step01.sql' "SELECT 'AE generic'"
    Assert-File 'SQL_AE\TEST_AE\MAIN\Step01.DB2.sql' "SELECT 'AE DB2'"

    $manifest = Get-Content -LiteralPath (Join-Path $output 'extraction-manifest.json') -Raw | ConvertFrom-Json
    if ($manifest.summary.peopleCode -ne 3) { throw "Expected 3 PeopleCode files; got $($manifest.summary.peopleCode)." }
    if ($manifest.summary.sqlOrXslt -ne 5) { throw "Expected 5 SQL files; got $($manifest.summary.sqlOrXslt)." }
    if ($manifest.summary.exactDuplicatesSkipped -ne 1) { throw 'The exact duplicate was not counted.' }
    if ($manifest.summary.pathCollisionsPreserved -ne 1) { throw 'The differing path collision was not preserved.' }

    Write-Host 'XML extractor regression tests passed.'
} finally {
    $resolvedOutput = [IO.Path]::GetFullPath($output)
    if (($resolvedOutput.StartsWith($tempRoot, [StringComparison]::OrdinalIgnoreCase)) -and (Test-Path -LiteralPath $resolvedOutput)) {
        Remove-Item -LiteralPath $resolvedOutput -Recurse -Force
    }
}
