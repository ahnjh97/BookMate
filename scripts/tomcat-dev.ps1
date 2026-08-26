[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$utf8 = [Text.UTF8Encoding]::new($false)
[Console]::InputEncoding = $utf8
[Console]::OutputEncoding = $utf8
$OutputEncoding = $utf8

$TomcatVersion = '10.1.59'
$MavenVersion = '3.9.16'
$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$FrontendRoot = Join-Path $ProjectRoot 'frontend'
$ToolsRoot = Join-Path $ProjectRoot '.tools'
$BundledTomcatRoot = Join-Path $ToolsRoot "apache-tomcat-$TomcatVersion"
$BundledMavenRoot = Join-Path $ToolsRoot "apache-maven-$MavenVersion"

function Get-MavenExecutable {
    $command = Get-Command mvn.cmd -ErrorAction SilentlyContinue
    if ($command) {
        return $command.Source
    }

    foreach ($variableName in @('MAVEN_HOME', 'M2_HOME')) {
        $root = [Environment]::GetEnvironmentVariable($variableName)
        if ($root) {
            $candidate = Join-Path $root 'bin\mvn.cmd'
            if (Test-Path -LiteralPath $candidate) {
                return $candidate
            }
        }
    }

    $bundled = Join-Path $BundledMavenRoot 'bin\mvn.cmd'
    if (Test-Path -LiteralPath $bundled) {
        return $bundled
    }

    throw 'Maven을 찾을 수 없습니다. scripts\tomcat-run.bat을 한 번 실행해 개발 도구를 준비해 주세요.'
}

function Get-TomcatRoot {
    foreach ($variableName in @('CATALINA_HOME', 'TOMCAT_HOME')) {
        $root = [Environment]::GetEnvironmentVariable($variableName)
        if ($root -and (Test-Path -LiteralPath (Join-Path $root 'bin\catalina.bat'))) {
            return (Resolve-Path -LiteralPath $root).Path
        }
    }

    if (Test-Path -LiteralPath (Join-Path $BundledTomcatRoot 'bin\catalina.bat')) {
        return (Resolve-Path -LiteralPath $BundledTomcatRoot).Path
    }

    throw 'Tomcat을 찾을 수 없습니다. scripts\tomcat-run.bat을 한 번 실행해 개발 도구를 준비해 주세요.'
}

function Assert-ChildPath {
    param(
        [string]$Parent,
        [string]$Child
    )

    $parentPath = [IO.Path]::GetFullPath($Parent).TrimEnd('\') + '\'
    $childPath = [IO.Path]::GetFullPath($Child)

    if (-not $childPath.StartsWith($parentPath, [StringComparison]::OrdinalIgnoreCase)) {
        throw "안전하지 않은 배포 경로입니다: $childPath"
    }
}

function Remove-DeploymentItem {
    param(
        [string]$WebappsRoot,
        [string]$Target
    )

    Assert-ChildPath -Parent $WebappsRoot -Child $Target
    if (Test-Path -LiteralPath $Target) {
        Remove-Item -LiteralPath $Target -Recurse -Force
    }
}

try {
    if (-not (Get-Command java.exe -ErrorAction SilentlyContinue)) {
        throw 'Java 21을 찾을 수 없습니다. JDK 21을 설치하고 java.exe를 PATH에 등록해 주세요.'
    }

    $portInUse = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue
    if ($portInUse) {
        throw '8080 포트를 이미 사용 중입니다. 실행 중인 Tomcat을 종료하고 다시 실행해 주세요.'
    }

    $MavenExecutable = Get-MavenExecutable
    $TomcatRoot = Get-TomcatRoot
    $MavenRepository = Join-Path $env:USERPROFILE '.m2\repository'
    $WebappsRoot = Join-Path $TomcatRoot 'webapps'
    $ExplodedSource = Join-Path $ProjectRoot 'backend\target\bookmate'
    $DeployedDirectory = Join-Path $WebappsRoot 'bookmate'
    $DeployedWar = Join-Path $WebappsRoot 'bookmate.war'

    if (-not (Test-Path -LiteralPath (Join-Path $ProjectRoot '.env'))) {
        Write-Warning '.env 파일이 없습니다. DB 환경변수가 별도로 설정되어 있어야 합니다.'
    }

    Write-Host '[1/3] Compiling the application for development mode...'
    & $MavenExecutable `
        -f (Join-Path $ProjectRoot 'backend\pom.xml') `
        package `
        -DskipTests `
        "-Dmaven.repo.local=$MavenRepository"

    if ($LASTEXITCODE -ne 0) {
        throw "Maven build failed with exit code $LASTEXITCODE."
    }

    if (-not (Test-Path -LiteralPath $ExplodedSource)) {
        throw "Exploded web application was not found: $ExplodedSource"
    }

    Write-Host '[2/3] Preparing an exploded deployment with the live frontend root...'
    Remove-DeploymentItem -WebappsRoot $WebappsRoot -Target $DeployedWar
    Remove-DeploymentItem -WebappsRoot $WebappsRoot -Target $DeployedDirectory
    New-Item -ItemType Directory -Path $DeployedDirectory -Force | Out-Null

    foreach ($directoryName in @('META-INF', 'WEB-INF')) {
        $sourcePath = Join-Path $ExplodedSource $directoryName
        if (Test-Path -LiteralPath $sourcePath) {
            Copy-Item -LiteralPath $sourcePath -Destination $DeployedDirectory -Recurse -Force
        }
    }

    $contextTemplate = Get-Content -Raw (Join-Path $PSScriptRoot 'tomcat-dev-context.xml')
    $escapedFrontendRoot = [Security.SecurityElement]::Escape($FrontendRoot)
    $contextContent = $contextTemplate.Replace('__FRONTEND_ROOT__', $escapedFrontendRoot)
    [IO.File]::WriteAllText(
        (Join-Path $DeployedDirectory 'META-INF\context.xml'),
        $contextContent,
        $utf8
    )

    $env:BOOKMATE_ENV_DIR = $ProjectRoot
    $env:BOOKMATE_DEV_MODE = 'true'
    $env:CATALINA_HOME = $TomcatRoot

    Write-Host '[3/3] Starting Tomcat in development mode...'
    Write-Host 'URL: http://localhost:8080/bookmate/'
    Write-Host 'HTML/CSS/JS: save the file and refresh the browser.'
    Write-Host 'Java: stop this server and run tomcat-dev.bat again.'
    Write-Host 'Press Ctrl+C to stop the server.'
    Write-Host

    & (Join-Path $TomcatRoot 'bin\catalina.bat') run
    exit $LASTEXITCODE
}
catch {
    Write-Host
    Write-Error $_.Exception.Message
    exit 1
}
