# EventPay System Design Document

## Executive Summary

EventPay is a comprehensive event management and ticketing system with QR code check-in capabilities. The system supports two primary user roles: **Admin** and **Scanner (Staff)**, each with distinct responsibilities and access levels.

---

## 1. System Architecture Overview

### 1.1 Technology Stack

| Layer | Technology |
|-------|------------|
| **Mobile App** | Kotlin + Jetpack Compose |
| **Web App** | React/Next.js (recommended) |
| **Backend** | Firebase (Auth + Firestore + Functions) |
| **QR Security** | HMAC-SHA256 + Encryption |
| **Offline Support** | Room Database + WorkManager |

### 1.2 Architecture Pattern

```
┌─────────────────────────────────────────────────────────────┐
│                     PRESENTATION LAYER                       │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐  │
│  │  Admin UI    │  │ Scanner UI   │  │  Web Dashboard   │  │
│  └──────────────┘  └──────────────┘  └──────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                      DOMAIN LAYER                            │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐  │
│  │  Use Cases   │  │   Entities   │  │  Repository IF   │  │
│  └──────────────┘  └──────────────┘  └──────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                      DATA LAYER                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐  │
│  │  Firebase    │  │  Local DB    │  │  Sync Manager    │  │
│  └──────────────┘  └──────────────┘  └──────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

---

## 2. Role-Based Access Control (RBAC)

### 2.1 Admin Role

**Purpose**: Event organizers and system administrators who manage the entire event lifecycle.

#### Core Capabilities:

| Feature | Description | Priority |
|---------|-------------|----------|
| **Event Management** | Create, edit, publish, and archive events | Critical |
| **User Management** | Create scanner accounts, activate/deactivate users | Critical |
| **Ticket Oversight** | View all tickets, issue refunds, transfer tickets | High |
| **Analytics Dashboard** | Real-time check-in stats, revenue reports, attendance | High |
| **QR Code Generation** | Generate secure QR codes for tickets | Critical |
| **Financial Control** | View transactions, manage wallet balances | Medium |
| **System Settings** | Configure event settings, notifications | Medium |
| **Scanner Assignment** | Assign scanners to specific events | High |

#### Admin Permissions Matrix:

```kotlin
enum class Permission {
    // Event Management
    CREATE_EVENT, READ_EVENT, UPDATE_EVENT, DELETE_EVENT, PUBLISH_EVENT,
    
    // User Management
    CREATE_USER, READ_USER, UPDATE_USER, DELETE_USER, MANAGE_ROLES,
    
    // Ticket Management
    CREATE_TICKET, READ_TICKET, UPDATE_TICKET, REFUND_TICKET,
    
    // Check-in (Admin can also scan)
    SCAN_QR, CHECK_IN_ATTENDEE, VIEW_CHECK_IN_HISTORY,
    
    // Financial
    VIEW_TRANSACTIONS, PROCESS_SALE, PROCESS_REFUND, VIEW_REPORTS, MANAGE_WALLET,
    
    // System
    MANAGE_SETTINGS, VIEW_ANALYTICS, EXPORT_DATA
}
```

### 2.2 Scanner Role (Staff)

**Purpose**: Event staff responsible for checking in attendees by scanning QR codes at event entrances.

#### Core Capabilities:

| Feature | Description | Priority |
|---------|-------------|----------|
| **QR Code Scanning** | Scan and validate attendee QR codes | Critical |
| **Event Selection** | View assigned events and select active event | Critical |
| **Check-in Processing** | Validate tickets and record attendance | Critical |
| **Offline Scanning** | Continue scanning without internet connection | High |
| **Scan History** | View recent scans and check-in statistics | Medium |
| **Manual Entry** | Manual ticket lookup for damaged QR codes | Medium |

#### Scanner Permissions Matrix:

```kotlin
// Scanner has LIMITED permissions:
enum class ScannerPermission {
    SCAN_QR,                    // Can scan QR codes
    CHECK_IN_ATTENDEE,          // Can check in attendees
    VIEW_CHECK_IN_HISTORY,      // Can view own scan history
    VIEW_ASSIGNED_EVENTS        // Can only see assigned events
}
```

#### Scanner Workflow:

```
1. Login → 2. Select Event → 3. Scan QR → 4. Validate → 5. Confirm Check-in
```

---

## 3. Firebase Backend Design

### 3.1 Firebase Project Structure

```
firebase-project/
├── authentication/
│   ├── Email/Password Provider (enabled)
│   ├── Email Verification (optional)
│   └── Password Reset (enabled)
│
├── firestore/
│   ├── database.rules
│   ├── indexes/
│   └── collections/
│
├── storage/
│   └── event-images/
│
├── functions/
│   ├── auth-triggers/
│   ├── ticket-triggers/
│   └── analytics-aggregators/
│
└── hosting/ (for web app)
```

### 3.2 Firestore Collections Schema

#### Collection: `users`

```typescript
interface UserDocument {
  id: string;                          // Firebase Auth UID
  email: string;
  fullName: string;
  role: 'ADMIN' | 'SCANNER' | 'ATTENDEE';
  
  // Status
  isActive: boolean;
  createdAt: Timestamp;
  lastLoginAt: Timestamp;
  
  // Profile
  phone?: string;
  profileImageUrl?: string;
  organization?: string;
  
  // Admin-specific
  createdScanners?: string[];          // List of scanner UIDs created by this admin
  
  // Scanner-specific
  createdBy?: string;                  // Admin UID who created this scanner
  assignedEvents?: string[];           // Events this scanner can access
  
  // Attendee-specific
  walletBalance: number;
  purchasedTickets?: string[];
  
  // Metadata
  preferences: {
    notificationsEnabled: boolean;
    emailNotifications: boolean;
    darkMode: boolean;
    language: string;
  };
}
```

#### Collection: `events`

```typescript
interface EventDocument {
  id: string;
  name: string;
  description: string;
  location: string;
  
  // Dates
  date: Timestamp;                     // Event start date/time
  endDate: Timestamp;                  // Event end date/time
  
  // Organizer
  organizerId: string;                 // Admin UID
  organizerName: string;
  
  // Ticketing
  ticketPrice: number;
  totalTickets: number;
  soldTickets: number;
  availableTickets: number;
  
  // VIP Tickets (optional)
  vipPrice?: number;
  vipTickets?: number;
  vipSold?: number;
  
  // Status
  status: 'DRAFT' | 'PUBLISHED' | 'ONGOING' | 'COMPLETED' | 'CANCELLED';
  isPublished: boolean;
  
  // Media
  imageUrl?: string;
  bannerUrl?: string;
  
  // Settings
  category: string;
  tags: string[];
  contactEmail?: string;
  contactPhone?: string;
  website?: string;
  
  // Check-in Tracking
  checkedInCount: number;
  checkInPercentage: number;
  
  // Scanner Assignment
  assignedScanners: string[];          // Scanner UIDs allowed for this event
  
  // Metadata
  createdAt: Timestamp;
  updatedAt: Timestamp;
}
```

#### Collection: `tickets`

```typescript
interface TicketDocument {
  id: string;                          // Unique ticket ID
  eventId: string;
  userId: string;                      // Attendee UID
  
  // Ticket Details
  ticketType: 'STANDARD' | 'VIP' | 'PREMIUM' | 'EARLY_BIRD' | 'STUDENT' | 'GROUP';
  price: number;
  purchaseDate: Timestamp;
  
  // Status
  status: 'ACTIVE' | 'USED' | 'REFUNDED' | 'CANCELLED' | 'EXPIRED';
  
  // QR Code
  qrCode: string;                      // Encrypted QR payload
  qrCodeData: {
    ticketId: string;
    eventId: string;
    userId: string;
    nonce: string;                     // Unique scan identifier
    timestamp: number;
    checksum: string;                  // HMAC signature
  };
  
  // Check-in Info
  checkedInAt?: Timestamp;
  checkedInBy?: string;                // Scanner UID
  checkedInByName?: string;
  deviceId?: string;
  location?: {
    latitude: number;
    longitude: number;
  };
  
  // Seat/Group Info
  seatNumber?: string;
  groupSize?: number;
  
  // Refund Info
  refundedAt?: Timestamp;
  refundReason?: string;
  refundProcessedBy?: string;
}
```

#### Collection: `checkIns` (Check-in Records)

```typescript
interface CheckInDocument {
  id: string;                          // Auto-generated
  ticketId: string;
  eventId: string;
  userId: string;                      // Attendee
  
  // Scanner Info
  scannedBy: string;                   // Scanner UID
  scannedByName: string;
  scannedByRole: 'ADMIN' | 'SCANNER';
  
  // Scan Details
  scannedAt: Timestamp;
  deviceId: string;
  location?: {
    latitude: number;
    longitude: number;
  };
  
  // Result
  result: 'SUCCESS' | 'ALREADY_SCANNED' | 'INVALID' | 'NOT_FOUND' | 'ERROR' | 'WRONG_EVENT';
  message?: string;
  
  // For duplicate scans
  previousScanId?: string;
  previousScanTime?: Timestamp;
}
```

#### Collection: `scannerAssignments`

```typescript
interface ScannerAssignmentDocument {
  id: string;                          // Composite: scannerId_eventId
  scannerId: string;
  scannerName: string;
  scannerEmail: string;
  
  eventId: string;
  eventName: string;
  
  assignedBy: string;                  // Admin UID
  assignedAt: Timestamp;
  
  // Status
  isActive: boolean;
  revokedAt?: Timestamp;
  revokedBy?: string;
  revokeReason?: string;
  
  // Stats
  totalScans: number;
  successfulCheckIns: number;
  lastScanAt?: Timestamp;
}
```

#### Collection: `auditLogs`

```typescript
interface AuditLogDocument {
  id: string;
  timestamp: Timestamp;
  userId: string;
  userEmail: string;
  userRole: string;
  
  action: string;
  category: 'AUTH' | 'USER_MANAGEMENT' | 'EVENT' | 'TICKET' | 'CHECK_IN' | 'FINANCIAL';
  
  details: {
    targetId?: string;
    targetType?: string;
    oldValue?: any;
    newValue?: any;
    metadata?: Record<string, any>;
  };
  
  ipAddress?: string;
  deviceId?: string;
  userAgent?: string;
}
```

### 3.3 Firestore Security Rules

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // Helper functions
    function isAuthenticated() {
      return request.auth != null;
    }
    
    function isAdmin() {
      return isAuthenticated() && 
        get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == 'ADMIN';
    }
    
    function isScanner() {
      return isAuthenticated() && 
        get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == 'SCANNER';
    }
    
    function isOwner(userId) {
      return isAuthenticated() && request.auth.uid == userId;
    }
    
    function isAssignedToEvent(eventId) {
      return isScanner() && 
        request.auth.uid in get(/databases/$(database)/documents/events/$(eventId)).data.assignedScanners;
    }
    
    // Users Collection
    match /users/{userId} {
      // Users can read their own data
      allow read: if isOwner(userId) || isAdmin();
      
      // Only admins can create/update users (scanners)
      allow create: if isAdmin();
      allow update: if isAdmin() || (isOwner(userId) && 
        // Users can only update their own profile fields
        request.resource.data.diff(resource.data).affectedKeys()
          .hasOnly(['fullName', 'phone', 'profileImageUrl', 'preferences']));
      
      // Only admins can delete users
      allow delete: if isAdmin();
    }
    
    // Events Collection
    match /events/{eventId} {
      // Admins can read all events, scanners only assigned events
      allow read: if isAdmin() || 
        (isScanner() && request.auth.uid in resource.data.assignedScanners);
      
      // Only admins can create/update/delete events
      allow create, update, delete: if isAdmin();
    }
    
    // Tickets Collection
    match /tickets/{ticketId} {
      // Admins can read all tickets
      // Scanners can read tickets for assigned events during event time
      // Attendees can read their own tickets
      allow read: if isAdmin() || 
        isOwner(resource.data.userId) ||
        (isScanner() && isAssignedToEvent(resource.data.eventId));
      
      // Only admins can create/update/delete tickets
      allow create, update, delete: if isAdmin();
    }
    
    // CheckIns Collection
    match /checkIns/{checkInId} {
      // Admins can read all check-ins
      // Scanners can read their own check-ins
      allow read: if isAdmin() || 
        (isScanner() && resource.data.scannedBy == request.auth.uid);
      
      // Admins and assigned scanners can create check-ins
      allow create: if isAdmin() || 
        (isScanner() && isAssignedToEvent(request.resource.data.eventId));
      
      // No updates or deletes allowed
      allow update, delete: if false;
    }
    
    // Scanner Assignments
    match /scannerAssignments/{assignmentId} {
      allow read: if isAdmin() || 
        (isScanner() && resource.data.scannerId == request.auth.uid);
      allow write: if isAdmin();
    }
    
    // Audit Logs (Admin only)
    match /auditLogs/{logId} {
      allow read: if isAdmin();
      allow write: if isAdmin() || isScanner();
    }
  }
}
```

---

## 4. Mobile App Module Structure

### 4.1 Clean Architecture Layers

```
app/src/main/java/com/example/eventpay/
├── data/                          # Data Layer
│   ├── auth/                      # Authentication
│   │   └── AuthRepositoryImpl.kt
│   ├── firebase/                  # Firebase services
│   │   ├── FirebaseService.kt
│   │   ├── FirestoreEventRepository.kt
│   │   ├── FirestoreTicketRepository.kt
│   │   └── FirestoreTransactionRepository.kt
│   ├── local/                     # Room database
│   │   ├── AppDatabase.kt
│   │   ├── dao/
│   │   └── entity/
│   ├── model/                     # Data models
│   ├── mapper/                    # Data mappers
│   └── repository/                # Repository implementations
│
├── domain/                        # Domain Layer (Business Logic)
│   ├── auth/                      # Auth domain models
│   ├── model/                     # Domain entities
│   ├── qrcode/                    # QR code generation/validation
│   ├── antifraud/                 # Security & fraud detection
│   ├── repository/                # Repository interfaces
│   ├── usecase/                   # Use cases
│   └── sync/                      # Sync logic
│
├── ui/                            # Presentation Layer
│   ├── auth/                      # Login/Register
│   ├── admin/                     # Admin-specific UI
│   ├── scanner/                   # Scanner-specific UI
│   ├── cashier/                   # Cashier features
│   ├── screens/                   # Main screens
│   ├── components/                # Reusable UI components
│   ├── navigation/                # Navigation graph
│   └── theme/                     # Theme & styling
│
├── di/                            # Dependency Injection
│   ├── AppContainer.kt
│   └── RepositoryModule.kt
│
├── security/                      # Security utilities
│   └── QRCryptoManager.kt
│
└── util/                          # Utilities
```

### 4.2 Feature Modules by Role

#### Admin Features:

| Module | Files | Description |
|--------|-------|-------------|
| Event Management | `CreateEventScreen.kt`, `EventDetailScreen.kt`, `AdminEventListScreen.kt` | CRUD operations for events |
| User Management | `AdminUserManagementScreen.kt`, `CreateScannerDialog.kt` | Manage scanner accounts |
| Analytics | `AnalyticsDashboardScreen.kt`, `AnalyticsViewModel.kt` | View statistics and reports |
| Dashboard | `AdminHomeScreen.kt`, `AdminViewModel.kt` | Admin main dashboard |

#### Scanner Features:

| Module | Files | Description |
|--------|-------|-------------|
| Event Selection | `ScannerHomeScreen.kt` | Select assigned events |
| QR Scanning | `QRScannerScreen.kt`, `QRScannerViewModel.kt` | Camera-based QR scanning |
| Check-in | `ScannerViewModel.kt` | Process check-ins |
| History | `ScanHistoryScreen.kt` | View scan records |

---

## 5. Web Application Architecture

### 5.1 Recommended Tech Stack

```
Frontend: Next.js 14 (App Router) + TypeScript + Tailwind CSS
State Management: Zustand or Redux Toolkit
Firebase SDK: firebase v10+
Authentication: Firebase Auth
Database: Firestore (same as mobile)
Hosting: Firebase Hosting
```

### 5.2 Web App Structure

```
web/
├── src/
│   ├── app/                      # Next.js App Router
│   │   ├── (auth)/               # Auth routes group
│   │   │   ├── login/
│   │   │   └── reset-password/
│   │   ├── (admin)/              # Admin routes (protected)
│   │   │   ├── dashboard/
│   │   │   ├── events/
│   │   │   ├── users/
│   │   │   ├── tickets/
│   │   │   └── analytics/
│   │   ├── layout.tsx
│   │   └── page.tsx
│   │
│   ├── components/               # React components
│   │   ├── auth/
│   │   ├── events/
│   │   ├── users/
│   │   ├── scanner/              # QR scanner for web
│   │   ├── analytics/
│   │   └── ui/                   # Shared UI components
│   │
│   ├── hooks/                    # Custom React hooks
│   │   ├── useAuth.ts
│   │   ├── useFirestore.ts
│   │   └── useScanner.ts
│   │
│   ├── lib/                      # Utilities
│   │   ├── firebase.ts           # Firebase config
│   │   ├── auth.ts
│   │   └── utils.ts
│   │
│   ├── store/                    # State management
│   │   ├── authStore.ts
│   │   ├── eventStore.ts
│   │   └── scannerStore.ts
│   │
│   └── types/                    # TypeScript types
│       ├── user.ts
│       ├── event.ts
│       └── ticket.ts
│
├── public/
├── firebase.json
├── next.config.js
└── package.json
```

---

## 6. QR Code Security Design

### 6.1 QR Code Payload Structure

```kotlin
data class QRCodePayload(
    val ticketId: String,           // Unique ticket identifier
    val eventId: String,            // Event identifier
    val userId: String,             // Attendee identifier
    val nonce: String,              // One-time use code
    val timestamp: Long,            // Generation timestamp
    val checksum: String            // HMAC-SHA256 signature
)
```

### 6.2 Encryption Flow

```
┌──────────────────────────────────────────────────────────────┐
│                    QR CODE GENERATION                        │
└──────────────────────────────────────────────────────────────┘

1. Create Payload (ticketId + eventId + userId + nonce + timestamp)
2. Calculate Checksum using HMAC-SHA256 (server-side secret)
3. Encrypt payload using AES-256-GCM
4. Encode as Base64 URL-safe string
5. Generate QR code image

┌──────────────────────────────────────────────────────────────┐
│                    QR CODE VALIDATION                        │
└──────────────────────────────────────────────────────────────┘

1. Scan QR code
2. Decode Base64 payload
3. Decrypt using AES-256-GCM
4. Verify checksum signature
5. Check timestamp (not expired)
6. Verify nonce uniqueness
7. Validate against Firestore ticket record
8. Record check-in
```

### 6.3 Security Measures

| Threat | Mitigation |
|--------|------------|
| QR Code Copying | Nonce tracking + timestamp expiration |
| Tampering | HMAC-SHA256 checksum verification |
| Replay Attacks | Time-limited validity (e.g., 24 hours) |
| Unauthorized Scanning | Scanner assignment validation |
| Screen Recording | Screenshot protection in mobile app |
| Offline Fraud | Device ID tracking + sync validation |

---

## 7. Offline Support Strategy

### 7.1 Offline Capabilities by Role

#### Admin:
- View cached events and tickets
- Queue event updates for sync
- View offline analytics (stale data)

#### Scanner:
- **Full offline scanning capability**
- Queue check-ins for sync
- Local validation using cached ticket data
- Conflict resolution on reconnect

### 7.2 Sync Strategy

```
┌─────────────────────────────────────────────────────────────┐
│                    SYNC ARCHITECTURE                         │
└─────────────────────────────────────────────────────────────┘

[Local Database] ←→ [Sync Manager] ←→ [Firebase]
       ↑                                    ↑
[WorkManager]                        [Firestore]
(Background Sync)

Sync Priorities:
1. Check-ins (highest priority)
2. Ticket status updates
3. New scanner assignments
4. Event updates (lowest priority)
```

---

## 8. API Endpoints (Firebase Functions)

### 8.1 Cloud Functions

```typescript
// Authentication
- createScannerAccount(data: { email, password, fullName, assignedEvents })
- deactivateUser(userId: string)
- resetUserPassword(userId: string)

// Event Management
- publishEvent(eventId: string)
- cancelEvent(eventId: string, reason: string)
- duplicateEvent(eventId: string)

// Scanner Management
- assignScannerToEvent(scannerId: string, eventId: string)
- removeScannerFromEvent(scannerId: string, eventId: string)
- getScannerStats(scannerId: string)

// Ticket Operations
- generateTickets(eventId: string, count: number, type: TicketType)
- validateTicketBatch(ticketIds: string[])
- processRefund(ticketId: string, reason: string)

// Analytics
- getEventAnalytics(eventId: string)
- getScannerPerformance(scannerId: string)
- exportEventData(eventId: string, format: 'csv' | 'json')

// Real-time
- onCheckInCreated(checkInData)  // Trigger for live updates
- onTicketUpdated(ticketData)    // Trigger for sync
```

---

## 9. Development Roadmap

### Phase 1: Core Authentication & Roles (Week 1-2)
- Firebase Auth setup
- Login system with role detection
- Admin dashboard skeleton
- Scanner interface skeleton

### Phase 2: Event Management (Week 3-4)
- Event CRUD operations
- Event publishing workflow
- Image upload to Firebase Storage

### Phase 3: Scanner Management (Week 5)
- Create scanner accounts
- Assign scanners to events
- Scanner authentication flow

### Phase 4: QR Code System (Week 6-7)
- QR code generation
- QR code validation
- Check-in recording

### Phase 5: Offline Support (Week 8)
- Room database integration
- Sync mechanism
- Conflict resolution

### Phase 6: Web Application (Week 9-10)
- Next.js setup
- Admin web dashboard
- Scanner web interface

### Phase 7: Analytics & Polish (Week 11-12)
- Analytics dashboard
- Export functionality
- UI/UX refinement
- Testing & bug fixes

---

## 10. Testing Strategy

### 10.1 Test Types

| Type | Focus | Tools |
|------|-------|-------|
| Unit Tests | ViewModels, Use Cases | JUnit, Mockito |
| Integration Tests | Repository operations | Android Instrumentation |
| UI Tests | Screen flows | Espresso, Compose Test |
| E2E Tests | Complete user journeys | Firebase Test Lab |

### 10.2 Critical Test Scenarios

```kotlin
// Authentication Tests
- testAdminLogin_Success()
- testScannerLogin_Success()
- testInvalidCredentials_ShowsError()
- testInactiveAccount_ShowsError()

// QR Scanning Tests
- testValidQR_CheckInSuccess()
- testDuplicateQR_ShowsAlreadyScanned()
- testExpiredQR_ShowsExpiredError()
- testWrongEventQR_ShowsWrongEventError()
- testOfflineScan_QueuesForSync()

// Permission Tests
- testScanner_CannotAccessAdminFeatures()
- testScanner_CannotSeeUnassignedEvents()
- testAdmin_CanAccessAllFeatures()
```

---

## 11. Deployment Checklist

### Pre-Deployment

- [ ] Firebase project configured
- [ ] Firestore security rules tested
- [ ] Firebase Auth providers enabled
- [ ] Storage buckets configured
- [ ] Cloud Functions deployed
- [ ] Indexes created in Firestore

### Mobile App

- [ ] Release build signed
- [ ] ProGuard/R8 rules configured
- [ ] Firebase crashlytics enabled
- [ ] App distributed (Play Console / Firebase App Distribution)

### Web App

- [ ] Firebase hosting configured
- [ ] Environment variables set
- [ ] Build optimized
- [ ] Deployed to Firebase Hosting

---

*Document Version: 1.0*
*Last Updated: 2026-02-28*
