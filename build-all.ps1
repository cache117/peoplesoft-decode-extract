$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$artifacts = Join-Path $root 'artifacts'

if (Test-Path -LiteralPath $artifacts) {
    Remove-Item -LiteralPath $artifacts -Recurse -Force
}
New-Item -ItemType Directory -Path $artifacts | Out-Null

& (Join-Path $root 'apps\xml-extractor\build.ps1')
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$msbuildCandidates = @(
    'C:\Program Files\Microsoft Visual Studio\2022\Enterprise\MSBuild\Current\Bin\MSBuild.exe',
    'C:\Program Files\Microsoft Visual Studio\2022\Professional\MSBuild\Current\Bin\MSBuild.exe',
    'C:\Program Files\Microsoft Visual Studio\2022\Community\MSBuild\Current\Bin\MSBuild.exe',
    'C:\Program Files (x86)\Microsoft Visual Studio\2019\BuildTools\MSBuild\Current\Bin\MSBuild.exe'
)
$msbuild = $msbuildCandidates | Where-Object { Test-Path -LiteralPath $_ } | Select-Object -First 1
if (-not $msbuild) {
    $command = Get-Command MSBuild.exe -ErrorAction SilentlyContinue
    if ($command) { $msbuild = $command.Source }
}
if (-not $msbuild) { throw 'MSBuild was not found.' }

& $msbuild (Join-Path $root 'apps\webux-dat-extractor\WebUxDatExtractor.sln') /t:Build /p:Configuration=Release /p:UseSharedCompilation=false /m:1 /v:minimal
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$xmlArtifact = Join-Path $artifacts 'xml-extractor'
$datArtifact = Join-Path $artifacts 'webux-dat-extractor'
New-Item -ItemType Directory -Path $xmlArtifact, $datArtifact | Out-Null
Copy-Item -LiteralPath (Join-Path $root 'apps\xml-extractor\peoplesoft-xml-extractor.jar') -Destination $xmlArtifact
Copy-Item -LiteralPath (Join-Path $root 'apps\xml-extractor\run-gui.bat') -Destination $xmlArtifact
Copy-Item -LiteralPath (Join-Path $root 'apps\xml-extractor\README.md') -Destination $xmlArtifact
Copy-Item -LiteralPath (Join-Path $root 'apps\xml-extractor\licenses') -Destination $xmlArtifact -Recurse
Copy-Item -Path (Join-Path $root 'apps\webux-dat-extractor\src\WebUxDatExtractor\bin\Release\*') -Destination $datArtifact -Recurse
Copy-Item -LiteralPath (Join-Path $root 'apps\webux-dat-extractor\run.cmd') -Destination $datArtifact
Copy-Item -LiteralPath (Join-Path $root 'apps\webux-dat-extractor\README.md') -Destination $datArtifact

Write-Host "Build artifacts are in $artifacts"
