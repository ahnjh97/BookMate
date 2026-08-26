[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
$utf8 = [Text.UTF8Encoding]::new($false)
[Console]::InputEncoding = $utf8
[Console]::OutputEncoding = $utf8
$OutputEncoding = $utf8

$TomcatVersion = '10.1.59'
$MavenVersion = '3.9.16'
$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$ToolsRoot = Join-Path $ProjectRoot '.tools'
$TomcatRoot = Join-Path $ToolsRoot "apache-tomcat-$TomcatVersion"
$MavenRoot = Join-Path $ToolsRoot "apache-maven-$MavenVersion"

function Get-ExpectedHash {
    param([string]$ChecksumUrl)

    $checksumFile = Join-Path $ToolsRoot ([IO.Path]::GetRandomFileName())

    try {
        Invoke-WebRequest -Uri $ChecksumUrl -OutFile $checksumFile -UseBasicParsing

        $checksumText = Get-Content -Raw $checksumFile
        $match = [regex]::Match(
            $checksumText,
            '(?i)\b[0-9a-f]{128}\b'
        )

        if (-not $match.Success) {
            throw "The checksum response from $ChecksumUrl is invalid."
        }

        return $match.Value.ToUpperInvariant()
    }
    finally {
        Remove-Item `
            -LiteralPath $checksumFile `
            -Force `
            -ErrorAction SilentlyContinue
    }
}

function Install-ApacheZip {
    param(
        [string]$Name,
        [string]$DownloadUrl,
        [string]$Destination,
        [string]$RequiredFile
    )

    if (Test-Path (Join-Path $Destination $RequiredFile)) {
        return
    }

    New-Item `
        -ItemType Directory `
        -Path $ToolsRoot `
        -Force | Out-Null

    $archivePath = Join-Path $ToolsRoot "$Name.zip"

    try {
        Write-Host "Downloading $Name from Apache..."

        Invoke-WebRequest `
            -Uri $DownloadUrl `
            -OutFile $archivePath `
            -UseBasicParsing

        Write-Host "Verifying $Name SHA-512 checksum..."

        $expectedHash = Get-ExpectedHash "$DownloadUrl.sha512"
        $actualHash = (
            Get-FileHash `
                -LiteralPath $archivePath `
                -Algorithm SHA512
        ).Hash

        if ($actualHash -ne $expectedHash) {
            throw "$Name checksum verification failed."
        }

        Write-Host "Installing $Name into $ToolsRoot..."

        Expand-Archive `
            -LiteralPath $archivePath `
            -DestinationPath $ToolsRoot `
            -Force

        if (-not (Test-Path (Join-Path $Destination $RequiredFile))) {
            throw "$Name was extracted, but $RequiredFile was not found."
        }
    }
    finally {
        Remove-Item `
            -LiteralPath $archivePath `
            -Force `
            -ErrorAction SilentlyContinue
    }
}

try {

    # ------------------------------------------------------------
    # Java 확인
    # ------------------------------------------------------------

    if (-not (Get-Command java.exe -ErrorAction SilentlyContinue)) {
        throw 'Java 21 was not found. Install JDK 21 and add java.exe to PATH.'
    }

    # ------------------------------------------------------------
    # 8080 포트 확인
    # ------------------------------------------------------------

    $portInUse = Get-NetTCPConnection `
        -LocalPort 8080 `
        -State Listen `
        -ErrorAction SilentlyContinue

    if ($portInUse) {
        throw 'Port 8080 is already in use. Stop the IntelliJ Tomcat or other server and run this file again.'
    }

    New-Item `
        -ItemType Directory `
        -Path $ToolsRoot `
        -Force | Out-Null

    # ------------------------------------------------------------
    # Maven 확인 / 설치
    # ------------------------------------------------------------

    $mavenCommand = Get-Command mvn.cmd -ErrorAction SilentlyContinue

    if ($mavenCommand) {
        $MavenExecutable = $mavenCommand.Source
    }
    elseif (
        $env:MAVEN_HOME -and
        (Test-Path (Join-Path $env:MAVEN_HOME 'bin\mvn.cmd'))
    ) {
        $MavenExecutable = Join-Path $env:MAVEN_HOME 'bin\mvn.cmd'
    }
    elseif (
        $env:M2_HOME -and
        (Test-Path (Join-Path $env:M2_HOME 'bin\mvn.cmd'))
    ) {
        $MavenExecutable = Join-Path $env:M2_HOME 'bin\mvn.cmd'
    }
    else {
        Install-ApacheZip `
            -Name "apache-maven-$MavenVersion" `
            -DownloadUrl "https://archive.apache.org/dist/maven/maven-3/$MavenVersion/binaries/apache-maven-$MavenVersion-bin.zip" `
            -Destination $MavenRoot `
            -RequiredFile 'bin\mvn.cmd'

        $MavenExecutable = Join-Path $MavenRoot 'bin\mvn.cmd'
    }

    # ------------------------------------------------------------
    # Tomcat 확인 / 설치
    # ------------------------------------------------------------

    $configuredTomcat = $env:CATALINA_HOME

    if (-not $configuredTomcat) {
        $configuredTomcat = $env:TOMCAT_HOME
    }

    if (
        $configuredTomcat -and
        (Test-Path (Join-Path $configuredTomcat 'bin\catalina.bat'))
    ) {
        $TomcatRoot = (Resolve-Path $configuredTomcat).Path
    }
    else {
        Install-ApacheZip `
            -Name "apache-tomcat-$TomcatVersion" `
            -DownloadUrl "https://dlcdn.apache.org/tomcat/tomcat-10/v$TomcatVersion/bin/apache-tomcat-$TomcatVersion.zip" `
            -Destination $TomcatRoot `
            -RequiredFile 'bin\catalina.bat'
    }

    # ------------------------------------------------------------
    # .env 확인
    # ------------------------------------------------------------

    if (-not (Test-Path (Join-Path $ProjectRoot '.env'))) {
        Write-Warning 'No .env file was found. DB_URL, DB_USER, and DB_PASSWORD must be system environment variables.'
    }

    # ------------------------------------------------------------
    # 1. Maven clean + package
    # ------------------------------------------------------------

    Write-Host '[1/3] Cleaning and building the BookMate WAR file...'

    & $MavenExecutable `
        -f (Join-Path $ProjectRoot 'backend\pom.xml') `
        clean package

    if ($LASTEXITCODE -ne 0) {
        throw "Maven build failed with exit code $LASTEXITCODE."
    }

    # ------------------------------------------------------------
    # 2. 기존 Tomcat 배포본 삭제 후 새 WAR 배포
    # ------------------------------------------------------------

    Write-Host '[2/3] Deploying the WAR file to Tomcat...'

    $warPath = Join-Path `
        $ProjectRoot `
        'backend\target\bookmate.war'

    $deployedWar = Join-Path `
        $TomcatRoot `
        'webapps\bookmate.war'

    $deployedDirectory = Join-Path `
        $TomcatRoot `
        'webapps\bookmate'

    if (-not (Test-Path $warPath)) {
        throw "WAR file was not found: $warPath"
    }

    # 이전 WAR 삭제
    if (Test-Path $deployedWar) {
        Write-Host 'Removing previous bookmate.war...'

        Remove-Item `
            -LiteralPath $deployedWar `
            -Force
    }

    # 이전 exploded deployment 삭제
    if (Test-Path $deployedDirectory) {
        Write-Host 'Removing previous Tomcat deployment directory...'

        Remove-Item `
            -LiteralPath $deployedDirectory `
            -Recurse `
            -Force
    }

    # 새 WAR 복사
    Copy-Item `
        -LiteralPath $warPath `
        -Destination $deployedWar `
        -Force

    # ------------------------------------------------------------
    # 환경 설정
    # ------------------------------------------------------------

    $env:BOOKMATE_ENV_DIR = $ProjectRoot
    $env:CATALINA_HOME = $TomcatRoot

    # ------------------------------------------------------------
    # 3. Tomcat 실행
    # ------------------------------------------------------------

    Write-Host '[3/3] Starting Tomcat...'
    Write-Host 'URL: http://localhost:8080/bookmate/'
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