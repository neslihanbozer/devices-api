$ErrorActionPreference = "Stop"

Set-Location $PSScriptRoot

if (-not (Test-Path ".env")) {
    $bytes = New-Object byte[] 32
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()

    try {
        $rng.GetBytes($bytes)
    }
    finally {
        $rng.Dispose()
    }

    $password = [BitConverter]::ToString($bytes).Replace("-", "")

    Set-Content -Path ".env" -Value "DB_PASSWORD=$password" -Encoding ASCII

    Write-Host "Database credentials generated."
}

Remove-Item Env:DB_PASSWORD -ErrorAction SilentlyContinue

docker compose up --build -d

if ($LASTEXITCODE -ne 0) {
    throw "Docker Compose failed."
}

docker compose ps
