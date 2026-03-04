#!/bin/bash
# Firebase Deployment Script for EventPay
# This script deploys Firestore rules, indexes, and Storage rules

set -e

echo "=========================================="
echo "EventPay Firebase Deployment Script"
echo "=========================================="
echo ""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Check if firebase CLI is installed
if ! command -v firebase &> /dev/null; then
    echo -e "${RED}[✗] Firebase CLI is not installed.${NC}"
    echo "Please install it with: npm install -g firebase-tools"
    read -p "Press Enter to exit"
    exit 1
fi

FIREBASE_VERSION=$(firebase --version)
echo -e "${GREEN}[✓] Firebase CLI detected: $FIREBASE_VERSION${NC}"
echo ""

# Check Firebase authentication
echo -e "${YELLOW}[1/4] Checking Firebase authentication...${NC}"
if firebase projects:list 2>/dev/null | grep -q "eventpay"; then
    echo -e "${GREEN}[✓] Already authenticated${NC}"
else
    firebase login
fi

echo ""
echo -e "${YELLOW}[2/4] Deploying Firestore rules...${NC}"
firebase deploy --only firestore:rules

echo ""
echo -e "${YELLOW}[3/4] Deploying Firestore indexes...${NC}"
firebase deploy --only firestore:indexes

echo ""
echo -e "${YELLOW}[4/4] Deploying Storage rules...${NC}"
firebase deploy --only storage

echo ""
echo -e "${GREEN}=========================================="
echo "Deployment Complete!"
echo "==========================================${NC}"
echo ""
echo "You can verify the deployment at:"
echo -e "${CYAN}https://console.firebase.google.com/project/eventpay-5f152${NC}"
echo ""
read -p "Press Enter to exit"
