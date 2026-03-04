@echo off
REM Firebase Deployment Script for EventPay
REM This script deploys Firestore rules, indexes, and Storage rules

echo ==========================================
echo EventPay Firebase Deployment Script
echo ==========================================
echo.

REM Check if firebase CLI is installed
call firebase --version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Firebase CLI is not installed.
    echo Please install it with: npm install -g firebase-tools
    pause
    exit /b 1
)

echo [1/4] Checking Firebase authentication...
call firebase login
echo.

echo [2/4] Deploying Firestore rules...
call firebase deploy --only firestore:rules
echo.

echo [3/4] Deploying Firestore indexes...
call firebase deploy --only firestore:indexes
echo.

echo [4/4] Deploying Storage rules...
call firebase deploy --only storage
echo.

echo ==========================================
echo Deployment Complete!
echo ==========================================
echo.
echo You can verify the deployment at:
echo https://console.firebase.google.com/project/eventpay-5f152
echo.
pause
