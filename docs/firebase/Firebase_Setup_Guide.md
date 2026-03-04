# Firebase Setup Guide for EventPay

This guide walks you through setting up Firebase for the EventPay system with ADMIN and SCANNER roles.

## Prerequisites

- Google Account
- Firebase CLI: `npm install -g firebase-tools`
- Android Studio
- Node.js 18+

---

## Step 1: Create Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click "Create a project"
3. Project name: `EventPay`
4. Disable Google Analytics (can enable later)
5. Click "Create project"

---

## Step 2: Register Android App

1. Click Android icon (`android`) to add app
2. **Package name**: `com.example.eventpay`
3. **Nickname**: EventPay Android
4. Get SHA-1 (optional):
   ```bash
   cd ~/.android
   keytool -list -v -keystore debug.keystore -alias androiddebugkey -storepass android -keypass android
   ```
5. Download `google-services.json` → place in `app/google-services.json`

---

## Step 3: Register Web App

1. Click web icon (`</>`) to add web app
2. **Nickname**: EventPay Web
3. Check "Set up Firebase Hosting"
4. Copy Firebase config for later use

---

## Step 4: Enable Authentication

1. Go to **Build** > **Authentication**
2. Click "Get started"
3. Enable **Email/Password** provider
4. Click "Save"

---

## Step 5: Set Up Firestore

1. Go to **Build** > **Firestore Database**
2. Click "Create database" → "Start in production mode"
3. Select location (e.g., `europe-west`)
4. Go to **Rules** tab
5. Copy contents from `docs/firebase/firestore.rules`
6. Click "Publish"

### Deploy Indexes

```bash
firebase login
firebase init firestore
firebase deploy --only firestore:indexes
```

---

## Step 6: Set Up Storage

1. Go to **Build** > **Storage**
2. Click "Get started" → "Start in production mode"
3. Match Firestore location
4. Update rules:

```javascript
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /{allPaths=**} {
      allow read: if request.auth != null;
    }
    match /event-images/{imageId} {
      allow write: if request.auth != null 
        && request.auth.token.role == 'ADMIN'
        && request.resource.size < 5 * 1024 * 1024
        && request.resource.contentType.matches('image/.*');
    }
    match /user-profiles/{userId}/{imageId} {
      allow write: if request.auth != null 
        && request.auth.uid == userId
        && request.resource.size < 2 * 1024 * 1024
        && request.resource.contentType.matches('image/.*');
    }
  }
}
```

---

## Step 7: Deploy All Rules

```bash
firebase deploy --only firestore
firebase deploy --only storage
```

---

## Step 8: Android Configuration

Project-level `build.gradle.kts`:
```kotlin
plugins {
    id("com.google.gms.google-services") version "4.4.0" apply false
}
```

App-level `build.gradle.kts`:
```kotlin
plugins {
    id("com.google.gms.google-services")
}

dependencies {
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
}
```

---

## Step 9: Create Initial Admin

### Via Firebase Console

1. **Authentication** > **Users** > "Add user"
2. Enter admin email/password
3. Note the UID
4. **Firestore** > Create document in `users` collection:

```json
{
  "id": "AUTH_UID_HERE",
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

## Web App Environment Variables

Create `web/.env.local`:

```env
NEXT_PUBLIC_FIREBASE_API_KEY=your_api_key
NEXT_PUBLIC_FIREBASE_AUTH_DOMAIN=your_project.firebaseapp.com
NEXT_PUBLIC_FIREBASE_PROJECT_ID=your_project
NEXT_PUBLIC_FIREBASE_STORAGE_BUCKET=your_project.appspot.com
NEXT_PUBLIC_FIREBASE_MESSAGING_SENDER_ID=your_sender_id
NEXT_PUBLIC_FIREBASE_APP_ID=your_app_id
```

---

## Verification Checklist

- [ ] Firebase project created
- [ ] Android app registered (`google-services.json` in place)
- [ ] Web app registered
- [ ] Email/Password auth enabled
- [ ] Firestore rules deployed
- [ ] Firestore indexes deployed
- [ ] Storage rules deployed
- [ ] Initial admin user created
- [ ] Android dependencies synced

---

**You're ready to start development using the prompts in `docs/Development_Prompts.md`!**
