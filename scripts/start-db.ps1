$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$portableStart = Join-Path $projectRoot '.local/start-mysql.ps1'
if (Test-Path -LiteralPath $portableStart) {
    & $portableStart
} else {
    Push-Location $projectRoot
    try { docker compose up -d --wait mysql; $runExitCode = $LASTEXITCODE }
    finally { Pop-Location }
    if ($runExitCode -ne 0) { throw 'MySQL을 시작하지 못했습니다. Docker Desktop 실행 상태를 확인하세요.' }
}
