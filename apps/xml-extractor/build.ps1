$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$buildRoot = Join-Path $projectRoot 'build'
$classes = Join-Path $buildRoot 'classes'
$jarFile = Join-Path $projectRoot 'peoplesoft-xml-extractor.jar'
$dependencyJars = Get-ChildItem -Path (Join-Path $projectRoot 'lib') -Filter '*.jar' | ForEach-Object { $_.FullName }

if (Test-Path -LiteralPath $classes) {
    Remove-Item -Recurse -Force -LiteralPath $classes
}
New-Item -ItemType Directory -Force -Path $classes | Out-Null

$sources = Get-ChildItem -Path (Join-Path $projectRoot 'src') -Filter '*.java' -Recurse | ForEach-Object { $_.FullName }
$dependencyClasspath = $dependencyJars -join [IO.Path]::PathSeparator
javac --release 17 -encoding UTF-8 -classpath $dependencyClasspath -d $classes $sources
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

# Bundle JNA into the application so end users only need this one JAR.
Push-Location $classes
try {
    foreach ($dependency in $dependencyJars) {
        jar --extract --file $dependency
    }
    Get-ChildItem -Path (Join-Path $classes 'META-INF') -Include '*.SF','*.RSA','*.DSA' -Recurse -ErrorAction SilentlyContinue |
        Remove-Item -Force
} finally {
    Pop-Location
}

jar --create --file $jarFile --main-class com.gideontaylor.peoplesoft.extractor.Main -C $classes .
Write-Host "Built $jarFile"
