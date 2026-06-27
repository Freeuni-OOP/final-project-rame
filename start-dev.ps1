param(
    [switch]$DownAfter
)

$root = Split-Path -Parent $MyInvocation.MyCommand.Path

Write-Host "Starting MySQL (docker-compose)..." -ForegroundColor Cyan
Push-Location $root
docker-compose up -d
Pop-Location

Write-Host "Starting backend (Spring Boot, https://localhost:8443)..." -ForegroundColor Cyan
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$root\backend'; ./mvnw spring-boot:run"

Write-Host "Starting frontend (Vite dev server)..." -ForegroundColor Cyan
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$root\frontend'; npm run dev"

Write-Host "All three are starting in separate windows. Close those windows (or Ctrl+C inside them) to stop backend/frontend." -ForegroundColor Green
Write-Host "MySQL keeps running in Docker in the background. Run 'docker-compose down' here when you're fully done." -ForegroundColor Yellow
