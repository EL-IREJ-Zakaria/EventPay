# EventPay Firebase Setup - Complete Guide

This document provides comprehensive instructions for setting up Firebase for the EventPay project.

---

## ✅ Current Project Status

### Android App Configuration
| Component | Status | Details |
|-----------|--------|---------|
| Firebase Project | ✅ Connected | `eventpay-5f152` |
| Package Name | ✅ Configured | `com.example.eventpay` |
| google-services.json | ✅ In Place | `app/google-services.json` |
| Firebase Auth | ✅ Ready | Email/Password |
| Firebase Firestore | ✅ Ready | Rules defined |
| Firebase BOM | ✅ Configured | v33.7.0 |

### Web App Configuration
| Component | Status | Details |
|-----------|--------|---------|
| Firebase Config | ✅ Updated | `web/src/lib/firebase.ts` |
| Environment Variables | ✅ Created | `web/.env.local` |
| Web App ID | ⚠️ Required | Register in Firebase Console |

---

## 📋 Prerequisites

- Google Account
- Node.js 18+ installed
- Firebase CLI: `npm install -g firebase-tools`
- Android Studio (latest version)
- Java JDK 17 or later

---

## 🚀 Quick Start

### Step 1: Verify Android Setup

The Android app is already configured. Verify by:

```bash
# Check if google-services.json exists
dir app\google-services.json

# Sync project in Android Studio
# Or use Gradle command
./gradlew :app:dependencies --configuration implementation | findstr firebase
```

### Step 2: Register Web App in Firebase

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select project `eventpay-5f152`
3. Click **Project Overview** → **Web** icon (`</>`)
4. **Nickname**: `EventPay Web`
5. Check "Also set up Firebase Hosting" (optional)
6. **Register app**
7. Copy the **`appId`** from the config object
8. Update the following files with the Web App ID:
   - `web/.env.local` → `NEXT_PUBLIC_FIREBASE_APP_ID`
   - `web/src/lib/firebase.ts` → `appId`

### Step 3: Deploy Firebase Rules

```bash
# Login to Firebase
firebase login

# Initialize (if not already done)
firebase init

# Deploy Firestore rules and indexes
cd docs/firebase
firebase deploy --only firestore

# Deploy Storage rules
firebase deploy --only storage
```

### Step 4: Enable Authentication

1. Go to **Build** → **Authentication**
2. Click **Get started**
3. Enable **Email/Password** provider
4. Save

---

## 📁 Firebase Configuration Files

```
docs/firebase/
├── firebase.json          # CLI deployment configuration
├── firestore.rules        # Firestore security rules
├── firestore.indexes.json # Firestore composite indexes
├── storage.rules          # Storage security rules
└── Firebase_Setup_Guide.md # Original setup guide
```

---

## 🔐 Security Rules Summary

### Firestore Rules
- **Users**: Admins can manage all users, users can only read/update own profile
- **Events**: Admins can create/update, scanners can read assigned events
- **Tickets**: Admins can create, scanners can update check-in status
- **CheckIns**: Append-only for assigned scanners

### Storage Rules
- **Event Images**: Admin only, max 5MB
- **User Profiles**: Own profile only, max 2MB
- **QR Codes**: Own tickets + admin, max 1MB

---

## 🔧 Firestore Indexes

The following indexes are pre-configured:

| Collection | Fields | Query Scope |
|------------|--------|-------------|
| events | organizerId ASC, createdAt DESC | Collection |
| events | status ASC, date ASC | Collection |
| events | isPublished ASC, date ASC | Collection |
| events | assignedScanners CONTAINS, date ASC | Collection |
| tickets | eventId ASC, purchaseDate DESC | Collection |
| tickets | userId ASC, purchaseDate DESC | Collection |
| tickets | eventId ASC, status ASC, purchaseDate DESC | Collection |
| checkIns | eventId ASC, scannedAt DESC | Collection |
| checkIns | scannedBy ASC, scannedAt DESC | Collection |
| users | role ASC, createdAt DESC | Collection |
| users | role ASC, isActive ASC, createdAt DESC | Collection |
| auditLogs | userId ASC, timestamp DESC | Collection |
| auditLogs | category ASC, timestamp DESC | Collection |

---

## 👤 Creating Initial Admin User

### Via Firebase Console (Recommended)

1. Go to **Authentication** → **Users**
2. Click **Add user**
3. Enter admin email and password
4. Note the **UID**
5. Go to **Firestore** → **+ Start collection**
6. Collection ID: `users`
7. Document ID: Paste the UID
8. Add the following fields:

```json
{
  "id": "PASTE_UID_HERE",
  "email": "admin@eventpay.com",
  "fullName": "System Admin",
  "role": "ADMIN",
  "isActive": true,
  "createdAt": 1709131200000,
  "walletBalance": 0,
  "preferences": {
    "notificationsEnabled": true,
    "emailNotifications": true,
    "darkMode": false,
    "language": "en"
  }
}
```

---

## 📱 Running the Android App

### From Android Studio
1. Open project in Android Studio
2. Sync project with Gradle files
3. Connect Android device or start emulator
4. Click **Run** ▶️

### From Command Line
```bash
# Build debug APK
./gradlew :app:assembleDebug

# Install on connected device
./gradlew :app:installDebug
```

---

## 🌐 Running the Web App

```bash
cd web

# Install dependencies
npm install

# Run development server
npm run dev

# Build for production
npm run build
```

---

## 🧪 Testing Firebase Connection

### Android
```kotlin
// Add this to MainActivity.kt temporarily for testing
import com.google.firebase.firestore.FirebaseFirestore

val db = FirebaseFirestore.getInstance()
db.collection("users").get()
    .addOnSuccessListener { documents ->
        Log.d("Firebase", "Connected! Found ${documents.size()} users")
    }
    .addOnFailureListener { e ->
        Log.e("Firebase", "Connection failed", e)
    }
```

### Web
```typescript
// Add this to any component
import { db } from '@/lib/firebase'
import { collection, getDocs } from 'firebase/firestore'

const testConnection = async () => {
  try {
    const querySnapshot = await getDocs(collection(db, 'users'))
    console.log('Connected! Found', querySnapshot.size, 'users')
  } catch (error) {
    console.error('Connection failed:', error)
  }
}
```

---

## 📝 Environment Variables Reference

### Web (.env.local)
```env
NEXT_PUBLIC_FIREBASE_API_KEY=AIzaSyBxx-uiTyUcTy02zz1e3vIbiTlU2AGkEXw
NEXT_PUBLIC_FIREBASE_AUTH_DOMAIN=eventpay-5f152.firebaseapp.com
NEXT_PUBLIC_FIREBASE_PROJECT_ID=eventpay-5f152
NEXT_PUBLIC_FIREBASE_STORAGE_BUCKET=eventpay-5f152.firebasestorage.app
NEXT_PUBLIC_FIREBASE_MESSAGING_SENDER_ID=944250967967
NEXT_PUBLIC_FIREBASE_APP_ID=1:944250967967:web:YOUR_WEB_APP_ID
NEXT_PUBLIC_USE_EMULATORS=false
```

---

## 🛠️ Troubleshooting

### Android Issues

**Gradle sync fails with "Cannot find google-services.json"**
- Ensure `google-services.json` is in `app/` directory
- File should not be gitignored (unless using template)

**Firebase Auth not working**
- Verify `app/google-services.json` has correct `package_name`
- Check if Email/Password provider is enabled in Firebase Console

**Firestore permission denied**
- Rules may not be deployed yet
- Run `firebase deploy --only firestore:rules`

### Web Issues

**"Invalid API key" error**
- Verify Web App is registered in Firebase Console
- Check that `NEXT_PUBLIC_FIREBASE_API_KEY` matches Firebase Console

**"Missing or insufficient permissions"**
- Rules may not be deployed
- Check that user has correct role in Firestore `users` collection

---

## ✅ Verification Checklist

Before running the app, ensure:

- [x] Firebase project created (`eventpay-5f152`)
- [x] Android app registered (`google-services.json` in place)
- [ ] Web app registered (get Web App ID from console)
- [x] Firestore rules created
- [x] Firestore indexes defined
- [x] Storage rules created
- [x] Email/Password auth enabled
- [ ] Firestore rules deployed
- [ ] Firestore indexes deployed
- [ ] Storage rules deployed
- [ ] Initial admin user created in Firestore
- [ ] Web `.env.local` updated with Web App ID
- [ ] Android dependencies synced

---

## 📚 Additional Resources

- [Firebase Console](https://console.firebase.google.com/project/eventpay-5f152)
- [Firestore Rules Playground](https://firebase.google.com/docs/firestore/security/get-started)
- [Firebase CLI Reference](https://firebase.google.com/docs/cli)
- [Android Firebase Setup](https://firebase.google.com/docs/android/setup)
- [Web Firebase Setup](https://firebase.google.com/docs/web/setup)

---

## 📞 Support

For issues with:
- **Firebase Console**: Check [Firebase Status Dashboard](https://status.firebase.google.com/)
- **Android App**: Review [Firebase Android SDK docs](https://firebase.google.com/docs/reference/android)
- **Web App**: Review [Firebase Web SDK docs](https://firebase.google.com/docs/reference/js)

---

**Last Updated**: 2026-03-02  
**Firebase Project**: `eventpay-5f152`  
**Android Package**: `com.example.eventpay`
