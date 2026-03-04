# Firebase Deployment Script for EventPay
# This script deploys Firestore rules, indexes, and Storage rules

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "EventPay Firebase Deployment Script" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""

# Check if firebase CLI is installed
try {
    $firebaseVersion = firebase --version 2>$null
    Write-Host "[✓] Firebase CLI detected: $firebaseVersion" -ForegroundColor Green
} catch {
    Write-Host "[✗] Firebase CLI is not installed." -ForegroundColor Red
    Write-Host "Please install it with: npm install -g firebase-tools" -ForegroundColor Yellow
    Read-Host "Press Enter to exit"
    exit 1
}

Write-Host ""
Write-Host "[1/4] Checking Firebase authentication..." -ForegroundColor Yellow
try {
    $projects = firebase projects:list 2>$null | Select-String "eventpay"
    if ($projects) {
        Write-Host "[✓] Already authenticated" -ForegroundColor Green
    } else {
        firebase login
    }
} catch {
    firebase login
}

Write-Host ""
Write-Host "[2/4] Deploying Firestore rules..." -ForegroundColor Yellow
firebase deploy --only firestore:rules

Write-Host ""
Write-Host "[3/4] Deploying Firestore indexes..." -ForegroundColor Yellow
firebase deploy --only firestore:indexes

Write-Host ""
Write-Host "[4/4] Deploying Storage rules..." -ForegroundColor Yellow
firebase deploy --only storage

Write-Host ""
Write-Host "==========================================" -ForegroundColor Green
Write-Host "Deployment Complete!" -ForegroundColor Green
Write-Host "==========================================" -ForegroundColor Green
Write-Host ""
Write-Host "You can verify the deployment at:"
Write-Host "https://console.firebase.google.com/project/eventpay-5f152" -ForegroundColor Cyan
Write-Host ""
Read-Host "Press Enter to exit"
