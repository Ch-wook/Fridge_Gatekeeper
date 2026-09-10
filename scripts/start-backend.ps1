param()
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $projectRoot '.env'
# .env는 Spring Boot가 자동으로 읽지 않으므로 이 실행 스크립트에서 읽습니다.
# 비밀 값을 화면에 출력하거나 문자열을 명령으로 실행하지 않습니다.
$allowed = @('DB_URL','DB_USERNAME','DB_PASSWORD','SERVER_PORT','SESSION_COOKIE_SECURE','OPENAI_API_KEY','OPENAI_MODEL','OPENAI_BASE_URL')
if (Test-Path -LiteralPath $envFile) {
    foreach ($line in (Get-Content -Encoding UTF8 -LiteralPath $envFile)) {
        $entry = $line.Trim()
        if (-not $entry -or $entry.StartsWith('#')) { continue }
        $parts = $entry.Split(@('='), 2)
        if ($parts.Length -ne 2) { continue }
        $key = $parts[0].Trim()
        if ($key -notin $allowed) { continue }
        $value = $parts[1].Trim()
        if ($value.Length -ge 2 -and (($value.StartsWith('"') -and $value.EndsWith('"')) -or ($value.StartsWith("'") -and $value.EndsWith("'")))) {
            $value = $value.Substring(1, $value.Length - 2)
        }
        [Environment]::SetEnvironmentVariable($key, $value, 'Process')
    }
}
Push-Location (Join-Path $projectRoot 'backend')
try { & .\mvnw.cmd spring-boot:run; $runExitCode = $LASTEXITCODE }
finally { Pop-Location }
exit $runExitCode
