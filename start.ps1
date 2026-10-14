$ErrorActionPreference = "Stop"

Set-Location $PSScriptRoot

function New-RandomSecret {
    $bytes = New-Object byte[] 32
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()

    try {
        $rng.GetBytes($bytes)
    }
    finally {
        $rng.Dispose()
    }

    return [BitConverter]::ToString($bytes).Replace("-", "")
}

function Get-EnvValue([string] $key) {
    $line = Get-Content ".env" | Where-Object { $_ -match "^$key=" } | Select-Object -First 1
    if ($line) { return $line.Substring($key.Length + 1) }
    return $null
}

if (-not (Test-Path ".env")) {
    New-Item -ItemType File -Path ".env" | Out-Null
}

# Make sure appended values start on a new line.
$content = [System.IO.File]::ReadAllText((Join-Path $PSScriptRoot ".env"))
if ($content.Length -gt 0 -and -not $content.EndsWith("`n")) {
    Add-Content -Path ".env" -Value "" -Encoding ASCII
}

# Generate every missing secret; existing values are kept.
foreach ($key in @("DB_PASSWORD", "API_ADMIN_PASSWORD", "API_VIEWER_PASSWORD")) {
    if (-not (Get-EnvValue $key)) {
        Add-Content -Path ".env" -Value "$key=$(New-RandomSecret)" -Encoding ASCII
        Write-Host "Generated $key."
    }
}

# Values set in the shell would override the ones in .env.
foreach ($key in @("DB_PASSWORD", "API_ADMIN_PASSWORD", "API_VIEWER_PASSWORD")) {
    Remove-Item "Env:$key" -ErrorAction SilentlyContinue
}

docker compose up --build -d

if ($LASTEXITCODE -ne 0) {
    throw "Docker Compose failed."
}

docker compose ps

$adminUser = Get-EnvValue "API_ADMIN_USERNAME"
if (-not $adminUser) { $adminUser = "admin" }
$viewerUser = Get-EnvValue "API_VIEWER_USERNAME"
if (-not $viewerUser) { $viewerUser = "viewer" }

Write-Host ""
Write-Host "API credentials (HTTP Basic, also used in Swagger UI 'Authorize'):"
Write-Host "  ADMIN  (full access): $adminUser / $(Get-EnvValue 'API_ADMIN_PASSWORD')"
Write-Host "  VIEWER (read-only)  : $viewerUser / $(Get-EnvValue 'API_VIEWER_PASSWORD')"
Write-Host ""
Write-Host "Swagger UI: http://localhost:8080/swagger-ui/index.html"
