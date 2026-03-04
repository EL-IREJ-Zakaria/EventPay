# EventPay Development Prompts

> **How to Use**: Copy each prompt section and paste it into your AI coding assistant. Each prompt is designed to be self-contained and produce a specific, working component.

---

## Phase 1: Firebase Backend Setup

### Prompt 1.1: Firebase Project Configuration

```
I need to set up a Firebase project for an event management system with the following requirements:

PROJECT NAME: EventPay

REQUIRED SERVICES:
1. Firebase Authentication (Email/Password)
2. Cloud Firestore
3. Firebase Storage (for event images)
4. Firebase Hosting (for web app)
5. Firebase Cloud Functions

CONFIGURATION REQUIREMENTS:
- Enable Email/Password authentication provider
- Configure Firestore in locked mode (start with strict security rules)
- Set up Storage bucket with organized folders: /event-images, /user-profiles
- Create a web app in Firebase for hosting

DELIVERABLES:
1. Step-by-step Firebase console setup instructions
2. google-services.json structure for Android
3. Firebase configuration object for web app (JavaScript)
4. Initial Firestore security rules (locked down - no access)

The system has two roles: ADMIN and SCANNER. Keep this in mind for future security rule design.
```

### Prompt 1.2: Firestore Database Schema & Security Rules

```
Create comprehensive Firestore security rules for an event ticketing system with these collections:

COLLECTIONS NEEDED:
1. users - User profiles with roles (ADMIN, SCANNER, ATTENDEE)
2. events - Event details with assignedScanners array
3. tickets - Ticket records with status and QR code data
4. checkIns - Check-in records for attendance tracking
5. scannerAssignments - Links scanners to events
6. auditLogs - System activity logs

ROLE PERMISSIONS:
- ADMIN: Full CRUD access to all collections
- SCANNER: Read assigned events, read tickets for assigned events, create checkIns only for assigned events
- ATTENDEE: Read own user data, read own tickets only

SECURITY REQUIREMENTS:
1. Users can only read their own user document unless they're admin
2. Scanners can only see events they're assigned to
3. Scanners can only create check-ins for events they're assigned to
4. No one can delete checkIn records (append-only)
5. Admins can read/write everything
6. Validate data types in write operations

DELIVERABLE:
Complete firestore.rules file with:
- Helper functions (isAuthenticated, isAdmin, isScanner, isOwner, isAssignedToEvent)
- Per-collection rules with clear comments
- Data validation for critical fields
- Protection against common vulnerabilities
```

### Prompt 1.3: Firestore Indexes Configuration

```
Create a firestore.indexes.json file for the EventPay system with these query patterns:

COMMON QUERIES:
1. Get events by organizerId sorted by createdAt (Admin dashboard)
2. Get events by status where assignedScanners contains scannerId (Scanner home)
3. Get tickets by eventId sorted by purchaseDate
4. Get tickets by userId sorted by purchaseDate
5. Get checkIns by eventId sorted by scannedAt
6. Get checkIns by scannedBy (scanner) sorted by scannedAt
7. Get users by role (for admin user management)
8. Get scannerAssignments by scannerId where isActive == true

COMPOUND INDEXES NEEDED:
- events: organizerId (Ascending) + createdAt (Descending)
- events: status (Ascending) + date (Ascending)
- tickets: eventId (Ascending) + purchaseDate (Descending)
- tickets: userId (Ascending) + purchaseDate (Descending)
- checkIns: eventId (Ascending) + scannedAt (Descending)
- checkIns: scannedBy (Ascending) + scannedAt (Descending)
- users: role (Ascending) + createdAt (Descending)
- scannerAssignments: scannerId (Ascending) + isActive (Ascending) + assignedAt (Descending)

DELIVERABLE:
Complete firestore.indexes.json with all necessary indexes and comments explaining each query pattern.
```

---

## Phase 2: Data Models & Domain Layer

### Prompt 2.1: User Domain Models

```
Create domain models for user management in Kotlin with the following requirements:

REQUIREMENTS:
1. UserRole enum: ADMIN, SCANNER, ATTENDEE
2. User data class with:
   - id, email, fullName, role
   - isActive, createdAt, lastLoginAt
   - phone, profileImageUrl, organization (optional)
   - assignedEvents (for scanners)
   - walletBalance (for attendees)
   - preferences (notifications, darkMode, language)

3. Permission enum with all granular permissions:
   - User Management: CREATE_USER, READ_USER, UPDATE_USER, DELETE_USER, MANAGE_ROLES
   - Event: CREATE_EVENT, READ_EVENT, UPDATE_EVENT, DELETE_EVENT, PUBLISH_EVENT
   - Ticket: CREATE_TICKET, READ_TICKET, UPDATE_TICKET, REFUND_TICKET
   - Check-in: SCAN_QR, CHECK_IN_ATTENDEE, VIEW_CHECK_IN_HISTORY
   - Financial: VIEW_TRANSACTIONS, PROCESS_SALE, PROCESS_REFUND, VIEW_REPORTS
   - System: MANAGE_SETTINGS, VIEW_ANALYTICS, EXPORT_DATA

4. Extension functions on UserRole:
   - getDefaultPermissions(): Set<Permission>
   - canManageEvents(), canScanQR(), canAccessDashboard(), canManageUsers(), canViewReports()

5. AuthenticatedUser class with session info and permission checking

6. ValidationResult sealed class for input validation

DELIVERABLE:
Complete Kotlin domain models in domain/model/ directory with:
- All data classes with proper immutability
- Business logic methods (validation, permission checks)
- Comprehensive documentation
- Type-safe enum implementations
```

### Prompt 2.2: Event Domain Models

```
Create domain models for event management in Kotlin:

REQUIREMENTS:
1. Event data class with:
   - id, name, description, location
   - date (start), endDate
   - ticketPrice, totalTickets, soldTickets, availableTickets
   - vipPrice, vipTickets, vipSold (optional VIP tier)
   - organizerId, organizerName
   - status: DRAFT, PUBLISHED, ONGOING, COMPLETED, CANCELLED
   - isPublished boolean
   - imageUrl, bannerUrl (optional)
   - category, tags
   - contactEmail, contactPhone, website (optional)
   - checkedInCount, checkInPercentage
   - assignedScanners (List<String>)
   - createdAt, updatedAt

2. EventStatus enum with status transitions logic
3. EventCategory enum (GENERAL, CONCERT, CONFERENCE, SPORTS, etc.)

4. Business logic methods on Event:
   - hasAvailableTickets(): Boolean
   - availableTickets(): Int
   - hasVipTicketsAvailable(): Boolean
   - availableVipTickets(): Int
   - isUpcoming(): Boolean
   - isOngoing(): Boolean
   - hasEnded(): Boolean
   - calculateRevenue(): Double
   - checkInPercentage(): Float
   - validate(): ValidationResult
   - canTransitionTo(newStatus): Boolean

5. EventFilter class for searching/filtering events

DELIVERABLE:
Complete Kotlin domain model in domain/model/Event.kt with all business logic methods and proper validation.
```

### Prompt 2.3: Ticket Domain Models

```
Create domain models for ticket management in Kotlin:

REQUIREMENTS:
1. Ticket data class with:
   - id, eventId, userId
   - ticketType: STANDARD, VIP, PREMIUM, EARLY_BIRD, STUDENT, GROUP
   - price, purchaseDate
   - status: ACTIVE, USED, REFUNDED, CANCELLED, EXPIRED
   - checkedInAt, checkedInBy, deviceId (optional)
   - qrCode (encrypted string)
   - qrCodeData (object with validation fields)
   - seatNumber, notes (optional)
   - refundedAt, refundReason, refundProcessedBy (optional)
   - location (lat/long from check-in)

2. TicketType enum with display names and pricing modifiers
3. TicketStatus enum with valid transition rules

4. Business logic methods on Ticket:
   - isUsed: Boolean (computed)
   - canCheckIn(): Boolean
   - isCheckedIn(): Boolean
   - isValid(): Boolean
   - canBeRefunded(): Boolean
   - ticketTypeDisplayName(): String
   - qrValidationKey(): String
   - validate(): ValidationResult
   - getRefundEligibility(): RefundEligibility

5. QRCodePayload data class for QR generation:
   - ticketId, eventId, userId, nonce, timestamp, checksum
   - toEncryptedString(): String
   - verifyChecksum(expected): Boolean

6. CheckInResult sealed class for scan results:
   - Success, AlreadyScanned, Invalid, NotFound, Expired, WrongEvent, Error

DELIVERABLE:
Complete Kotlin domain models in domain/model/Ticket.kt with QR code structure and all business logic.
```

---

## Phase 3: Repository Layer

### Prompt 3.1: Authentication Repository

```
Create a complete authentication repository implementation using Firebase Auth:

REQUIREMENTS:

1. AuthRepository interface with methods:
   - getAuthState(): Flow<AuthState>
   - getCurrentUser(): AuthenticatedUser?
   - login(request: LoginRequest): LoginResult
   - register(request: RegistrationRequest): RegistrationResult
   - logout(): Result<Unit>
   - resetPassword(email: String): Result<Unit>
   - changePassword(oldPassword, newPassword): Result<Unit>
   - updateProfile(fullName, phone, image): Result<Unit>
   - refreshToken(): Result<Unit>

2. AuthRepositoryImpl using Firebase Auth and Firestore:
   - Use FirebaseAuth for authentication
   - Store user data in Firestore users collection
   - Cache user in local Room database
   - Handle all FirebaseAuthException error codes
   - Support role-based user creation
   - Create session info with expiration

3. Data classes:
   - LoginRequest (email, password, deviceId)
   - RegistrationRequest (email, password, fullName, role, organization)
   - LoginResult sealed class (Success, InvalidCredentials, UserNotFound, AccountDisabled, TooManyAttempts, Error)
   - RegistrationResult sealed class (Success, EmailAlreadyExists, WeakPassword, Error)
   - AuthState sealed class (Authenticated, Unauthenticated, Loading, Error)

4. Special admin method:
   - createScannerAccount(email, password, fullName, assignedEvents): Result<String>
   - Uses secondary FirebaseAuth instance to avoid logging out current admin

DELIVERABLE:
Complete implementation in data/auth/AuthRepositoryImpl.kt with error handling, session management, and role support.
```

### Prompt 3.2: Event Repository

```
Create a complete event repository with Firebase Firestore integration:

REQUIREMENTS:

1. EventRepository interface:
   - getEvents(): Flow<List<Event>>
   - getEventById(eventId): Flow<Event?>
   - getEventsByOrganizer(organizerId): Flow<List<Event>>
   - getEventsForScanner(scannerId): Flow<List<Event>>
   - createEvent(event): Result<String>
   - updateEvent(event): Result<Unit>
   - deleteEvent(eventId): Result<Unit>
   - publishEvent(eventId): Result<Unit>
   - cancelEvent(eventId, reason): Result<Unit>
   - assignScanner(eventId, scannerId): Result<Unit>
   - removeScanner(eventId, scannerId): Result<Unit>
   - updateCheckInStats(eventId, increment): Result<Unit>
   - searchEvents(query): Flow<List<Event>>

2. FirestoreEventRepository implementation:
   - Use Firebase Firestore for data storage
   - Use Room for local caching
   - Implement real-time updates with Flow
   - Handle offline scenarios
   - Batch operations for efficiency
   - Transaction support for check-in stats

3. Firestore collection structure:
   - events/{eventId}
   - scannerAssignments/{scannerId_eventId}

4. Query optimization:
   - Use indexes for common queries
   - Implement pagination for large event lists
   - Cache frequently accessed events

DELIVERABLE:
Complete implementation in data/firebase/FirestoreEventRepository.kt with real-time sync and offline support.
```

### Prompt 3.3: Ticket Repository

```
Create a complete ticket repository with QR code generation:

REQUIREMENTS:

1. TicketRepository interface:
   - getTickets(): Flow<List<Ticket>>
   - getTicketById(ticketId): Flow<Ticket?>
   - getTicketsByEvent(eventId): Flow<List<Ticket>>
   - getTicketsByUser(userId): Flow<List<Ticket>>
   - createTicket(eventId, userId, type, price): Result<Ticket>
   - createMultipleTickets(eventId, count, type): Result<List<Ticket>>
   - updateTicket(ticket): Result<Unit>
   - markAsCheckedIn(ticketId, scannerId, deviceId): Result<Unit>
   - processRefund(ticketId, reason): Result<Unit>
   - validateTicket(qrCode): Result<TicketValidation>
   - getTicketByQRCode(qrCode): Result<Ticket?>

2. QR Code generation:
   - Generate unique nonce for each ticket
   - Create encrypted payload with HMAC signature
   - Include expiration timestamp
   - Return QR code as string and bitmap

3. FirestoreTicketRepository implementation:
   - Store tickets in Firestore
   - Use transactions for check-in operations
   - Prevent duplicate check-ins
   - Update event stats atomically

4. Security considerations:
   - Validate QR code signature before accepting
   - Check scanner permissions
   - Log all check-in attempts

DELIVERABLE:
Complete implementation in data/firebase/FirestoreTicketRepository.kt with QR generation and secure validation.
```

---

## Phase 4: UI Layer - Authentication

### Prompt 4.1: Login Screen

```
Create a modern login screen in Jetpack Compose:

REQUIREMENTS:

1. Screen design:
   - Clean, modern UI with app branding
   - Email input field with validation
   - Password input field with visibility toggle
   - Login button with loading state
   - Forgot password link
   - Register link (if self-registration enabled)

2. State management:
   - Email and password state
   - Loading state
   - Error state with specific messages
   - Form validation (email format, password length)

3. Role-based navigation:
   - After login, detect user role
   - Navigate to AdminHomeScreen if ADMIN
   - Navigate to ScannerHomeScreen if SCANNER
   - Show error if role not recognized

4. Features:
   - Input validation with real-time feedback
   - Keyboard handling (next/done actions)
   - Error message display
   - Success animation on login
   - Biometric authentication option (if available)

5. Integration:
   - Use AuthViewModel
   - Collect auth state
   - Handle navigation

DELIVERABLE:
Complete LoginScreen.kt in ui/screens/ with full state management, validation, and role-based navigation.
```

### Prompt 4.2: Role-Based Navigation

```
Create a navigation system that routes users based on their role:

REQUIREMENTS:

1. Navigation graph structure:
   - Auth routes: Login, Register, ResetPassword
   - Admin routes: AdminHome, Events, EventDetail, CreateEvent, Users, Analytics
   - Scanner routes: ScannerHome, QRScanner, ScanHistory

2. Role detection:
   - Check user role on app startup
   - Redirect to appropriate home screen
   - Deep linking support for specific routes

3. Protected routes:
   - Block admin routes for scanners
   - Block scanner-specific actions for admins (optional)
   - Show access denied screen for unauthorized access

4. Navigation state:
   - Bottom navigation for main tabs
   - Drawer navigation for admin
   - Simple top bar for scanner

5. Logout handling:
   - Clear back stack on logout
   - Navigate to login screen
   - Clean up session data

DELIVERABLE:
Complete NavGraph.kt in ui/navigation/ with role-based route protection and navigation state management.
```

---

## Phase 5: UI Layer - Admin Features

### Prompt 5.1: Admin Dashboard

```
Create a comprehensive admin dashboard screen:

REQUIREMENTS:

1. Dashboard layout:
   - Welcome header with admin name
   - Quick stats cards (total events, tickets sold, revenue, check-ins)
   - Recent events list
   - Quick action buttons (Create Event, Manage Users, View Analytics)
   - Recent activity feed

2. Stats to display:
   - Total events created
   - Total tickets sold (all events)
   - Total revenue
   - Total check-ins
   - Active scanners count
   - Upcoming events count

3. Interactive elements:
   - Pull-to-refresh
   - Card click actions
   - Floating action button for quick create
   - Menu for settings and logout

4. State management:
   - Loading state with shimmer effect
   - Empty state for no events
   - Error state with retry
   - Real-time stats updates

5. Integration:
   - Use AdminViewModel
   - Fetch data from repositories
   - Handle navigation callbacks

DELIVERABLE:
Complete AdminHomeScreen.kt in ui/screens/admin/ with stats display, quick actions, and proper state management.
```

### Prompt 5.2: Event Management

```
Create complete event management screens for admins:

REQUIREMENTS:

1. Event List Screen:
   - List of all events with filter (All, Draft, Published, Completed)
   - Event card with image, name, date, status
   - Ticket stats on each card
   - Swipe actions (Edit, Delete, Duplicate)
   - Search functionality
   - FAB to create new event

2. Create/Edit Event Screen:
   - Form with all event fields
   - Image picker with preview
   - Date/time pickers
   - Ticket pricing section (regular + VIP)
   - Scanner assignment section
   - Publish toggle
   - Validation and error display
   - Save as draft option

3. Event Detail Screen:
   - Full event information
   - Large header image
   - Ticket statistics with progress bars
   - Check-in statistics
   - Assigned scanners list
   - Action buttons (Edit, Publish, Cancel, Delete)
   - Tab for tickets list

4. State management:
   - Form state with validation
   - Image upload progress
   - Success/error feedback
   - Unsaved changes warning

DELIVERABLE:
Complete event management screens in ui/screens/ with full CRUD operations and image handling.
```

### Prompt 5.3: User Management (Scanner Creation)

```
Create user management screens for admins to manage scanner accounts:

REQUIREMENTS:

1. User List Screen:
   - Tabs: All Users, Admins, Scanners
   - User card with avatar, name, email, role, status
   - Active/Inactive indicator
   - Search by name or email
   - Filter by role and status

2. Create Scanner Dialog/Screen:
   - Email input (required)
   - Full name input (required)
   - Temporary password generation
   - Event assignment (multi-select)
   - Create button with validation
   - Success message with credentials

3. User Detail/Edit Screen:
   - Profile information display
   - Edit name and phone
   - Toggle active/inactive status
   - Assigned events list
   - Recent activity log
   - Reset password option
   - Delete account option

4. Features:
   - Confirmation dialogs for destructive actions
   - Batch operations (activate/deactivate multiple)
   - Scanner statistics (check-ins performed)
   - Email invitation for new scanners

5. Integration:
   - Use Firebase Auth to create users
   - Store additional data in Firestore
   - Handle email already exists error

DELIVERABLE:
Complete user management screens in ui/screens/admin/ with scanner creation and management features.
```

### Prompt 5.4: Analytics Dashboard

```
Create an analytics dashboard for admins:

REQUIREMENTS:

1. Dashboard layout:
   - Date range selector (Today, This Week, This Month, Custom)
   - Event selector (All Events or specific event)
   - Summary cards at top
   - Charts and graphs section
   - Detailed data tables

2. Analytics to display:
   - Total check-ins vs tickets sold (percentage)
   - Revenue over time (line chart)
   - Tickets by type (pie chart)
   - Check-ins by hour (bar chart)
   - Top performing events
   - Scanner performance ranking
   - Attendee demographics (if available)

3. Interactive features:
   - Chart touch interactions
   - Drill-down to detailed data
   - Export to CSV option
   - Share report option

4. Real-time updates:
   - Live check-in counter
   - Auto-refresh option
   - Push notifications for milestones

5. Charts library:
   - Use MPAndroidChart or Compose Charts
   - Custom styling to match app theme
   - Responsive layouts

DELIVERABLE:
Complete AnalyticsDashboardScreen.kt in ui/screens/ with multiple chart types and data visualization.
```

---

## Phase 6: UI Layer - Scanner Features

### Prompt 6.1: Scanner Home Screen

```
Create the scanner home screen for staff users:

REQUIREMENTS:

1. Screen states:
   - Event selection state (when no event selected)
   - Active scanning state (when event selected)

2. Event Selection State:
   - List of assigned events for today/upcoming
   - Event card with name, date, location
   - Ticket count (sold/checked in)
   - Tap to select event
   - Empty state if no assigned events
   - Refresh button

3. Active Scanning State:
   - Selected event header (name, time remaining)
   - Scan statistics (checked in / total)
   - Big "Scan QR Code" button
   - Recent scans list
   - Change event button
   - Manual entry option (for damaged QR)

4. Scanner profile:
   - Profile section with name and role
   - Logout button
   - Scanner ID display

5. State management:
   - Selected event state
   - Scan statistics state
   - Recent scans list
   - Loading states

DELIVERABLE:
Complete ScannerHomeScreen.kt in ui/screens/scanner/ with event selection and active scanning modes.
```

### Prompt 6.2: QR Scanner Screen

```
Create a full-featured QR code scanner screen:

REQUIREMENTS:

1. Camera integration:
   - CameraX preview with QR scanning
   - Camera permission handling
   - Torch/flashlight toggle
   - Camera selector (front/back) if needed
   - Focus indicator

2. Scanning overlay:
   - QR code frame overlay
   - Scanning animation
   - Instructions text
   - Cancel button

3. Validation feedback:
   - Success: Green overlay with checkmark, attendee name, ticket type
   - Already scanned: Orange overlay with previous scan time
   - Invalid: Red overlay with error reason
   - Processing: Loading indicator

4. Sound and haptics:
   - Success beep sound
   - Error sound
   - Vibration on scan

5. Auto-processing:
   - Continuous scanning mode
   - Auto-dismiss after success (configurable delay)
   - Batch scan mode for quick entry

6. Manual entry fallback:
   - Button to switch to manual ticket ID entry
   - Number pad input
   - Validate button

7. Integration:
   - Use QRScannerViewModel
   - Validate against selected event
   - Record check-in on success
   - Handle offline scenarios

DELIVERABLE:
Complete QRScannerScreen.kt in ui/screens/ with CameraX integration, validation feedback, and sound effects.
```

### Prompt 6.3: Scan History

```
Create a scan history screen for scanners:

REQUIREMENTS:

1. History list:
   - Chronological list of scans
   - Filter by result (Success, Already Scanned, Invalid)
   - Group by time (Today, Yesterday, Earlier)
   - Pull to refresh

2. Scan item display:
   - Attendee name (if available)
   - Ticket ID (masked)
   - Scan time
   - Result status with icon
   - Scanner name (for admin view)

3. Statistics:
   - Total scans today
   - Successful check-ins
   - Already scanned count
   - Invalid count

4. Search and filter:
   - Search by ticket ID
   - Filter by time range
   - Filter by result type

5. Detail view:
   - Tap to see full scan details
   - Ticket information
   - Location data (if captured)
   - Device ID

6. Export:
   - Export scan log option
   - Share via email/message

DELIVERABLE:
Complete ScanHistoryScreen.kt in ui/screens/scanner/ with filtering, search, and statistics.
```

---

## Phase 7: Security & Offline Support

### Prompt 7.1: QR Code Security

```
Create a secure QR code generation and validation system:

REQUIREMENTS:

1. QRCodePayload data class:
   - ticketId, eventId, userId
   - nonce (random unique identifier)
   - timestamp (generation time)
   - checksum (HMAC-SHA256 signature)

2. Encryption:
   - AES-256-GCM encryption for payload
   - Server-side secret key (stored securely)
   - Initialization vector per encryption
   - URL-safe Base64 encoding

3. Checksum generation:
   - HMAC-SHA256 with secret key
   - Include all payload fields
   - Prevent tampering

4. Validation process:
   - Decrypt payload
   - Verify checksum signature
   - Check timestamp (not expired)
   - Verify nonce uniqueness
   - Match against Firestore ticket record
   - Validate event match

5. Security features:
   - Time-limited validity (configurable, e.g., 24 hours)
   - Single-use nonce tracking
   - Device ID logging
   - Replay attack prevention

6. QRCryptoManager class:
   - generateQRCode(ticket): Result<String>
   - parseQRCodeString(qrString): Result<QRCodePayload>
   - validateChecksum(payload, expected): Boolean
   - isExpired(timestamp): Boolean
   - generateNonce(): String

DELIVERABLE:
Complete security package in security/ with QRCryptoManager.kt and comprehensive validation logic.
```

### Prompt 7.2: Offline Sync

```
Create an offline synchronization system:

REQUIREMENTS:

1. Local database (Room):
   - PendingCheckIn entity
   - CachedEvent entity
   - CachedTicket entity
   - PendingOperation entity (generic queue)

2. Offline capabilities:
   - Cache assigned events for scanners
   - Cache ticket data for validation
   - Queue check-ins when offline
   - Store scan history locally

3. SyncManager:
   - Monitor network connectivity
   - Sync pending check-ins when online
   - Download updated event data
   - Handle sync conflicts
   - Retry with exponential backoff

4. WorkManager integration:
   - Periodic sync worker
   - One-time sync on connectivity restored
   - Battery-aware syncing
   - Failed sync notification

5. Conflict resolution:
   - Check-in already processed (by another device)
   - Ticket status changed (refunded/cancelled)
   - Event no longer active
   - Scanner no longer assigned

6. UI indicators:
   - Offline mode badge
   - Pending sync count
   - Last sync time
   - Sync in progress indicator

DELIVERABLE:
Complete offline support in data/local/ and data/sync/ with Room entities, SyncManager, and WorkManager workers.
```

---

## Phase 8: Web Application

### Prompt 8.1: Web App Setup

```
Set up a Next.js web application for the EventPay admin dashboard:

REQUIREMENTS:

1. Project setup:
   - Next.js 14 with App Router
   - TypeScript configuration
   - Tailwind CSS for styling
   - Shadcn/ui component library
   - Firebase SDK v10+

2. Folder structure:
   ```
   web/
   ├── src/app/
   │   ├── (auth)/login/page.tsx
   │   ├── (admin)/dashboard/page.tsx
   │   ├── (admin)/events/page.tsx
   │   ├── (admin)/users/page.tsx
   │   └── layout.tsx
   ├── src/components/
   ├── src/hooks/
   ├── src/lib/firebase.ts
   └── src/types/
   ```

3. Firebase configuration:
   - Initialize Firebase app
   - Configure Auth, Firestore, Storage
   - Environment variables setup
   - Emulator support for development

4. Authentication:
   - Login page with Firebase Auth
   - Protected route middleware
   - Role-based access control
   - Session persistence

5. Shared components:
   - Navigation sidebar
   - Header with user menu
   - Data table component
   - Form components
   - Loading states

6. State management:
   - Zustand for global state
   - React Query for server state
   - Firebase real-time listeners

DELIVERABLE:
Complete Next.js project setup with Firebase integration, authentication, and admin layout.
```

### Prompt 8.2: Web Admin Dashboard

```
Create a web-based admin dashboard with these features:

REQUIREMENTS:

1. Dashboard overview:
   - Stats cards (events, tickets, revenue, check-ins)
   - Recent events table
   - Activity feed
   - Quick actions

2. Events management:
   - Events list with filtering and sorting
   - Create event form with image upload
   - Event detail view with statistics
   - Ticket management per event
   - Scanner assignment interface

3. User management:
   - Users list with role filters
   - Create scanner form
   - User detail and edit
   - Activity logs

4. Analytics:
   - Charts (revenue, check-ins, ticket types)
   - Date range filtering
   - Export reports

5. Scanner interface (web):
   - QR code scanner using camera
   - Manual ticket lookup
   - Check-in history
   - Event selector

6. Responsive design:
   - Desktop optimized
   - Tablet support
   - Mobile-friendly tables

DELIVERABLE:
Complete web admin dashboard with all management features and responsive design.
```

---

## Phase 9: Testing & Quality Assurance

### Prompt 9.1: Unit Tests

```
Create comprehensive unit tests for the domain layer:

REQUIREMENTS:

1. User tests:
   - testUserValidation_ValidData_ReturnsSuccess()
   - testUserValidation_InvalidEmail_ReturnsError()
   - testAdminRole_HasAllPermissions()
   - testScannerRole_HasLimitedPermissions()

2. Event tests:
   - testEventValidation_ValidData_ReturnsSuccess()
   - testEventValidation_MissingName_ReturnsError()
   - testEvent_CalculateRevenue_CorrectTotal()
   - testEvent_IsUpcoming_FutureDate_ReturnsTrue()
   - testEvent_CheckInPercentage_CorrectCalculation()

3. Ticket tests:
   - testTicket_CanCheckIn_ActiveStatus_ReturnsTrue()
   - testTicket_CanCheckIn_AlreadyUsed_ReturnsFalse()
   - testTicket_CanBeRefunded_ValidConditions_ReturnsTrue()
   - testTicket_ValidateQRCode_ValidSignature_ReturnsValid()
   - testTicket_ValidateQRCode_Expired_ReturnsExpired()

4. Use case tests:
   - Mock repositories
   - Test use case logic
   - Verify interactions
   - Test error scenarios

5. Test utilities:
   - Test data factories
   - Coroutine test rules
   - Flow testing utilities

DELIVERABLE:
Complete unit test suite in src/test/ with high coverage of domain logic.
```

### Prompt 9.2: Integration Tests

```
Create integration tests for data layer:

REQUIREMENTS:

1. Repository tests:
   - Test Firebase interactions (using Firebase Emulator)
   - Test local database operations
   - Test sync behavior
   - Test offline scenarios

2. Authentication flow:
   - Test login with valid credentials
   - Test login with invalid credentials
   - Test role-based access
   - Test session management

3. QR code flow:
   - Test QR generation
   - Test QR validation
   - Test check-in recording
   - Test duplicate prevention

4. Test setup:
   - Firebase Emulator Suite configuration
   - Test data seeding
   - Cleanup after tests

DELIVERABLE:
Complete integration tests in src/androidTest/ using Firebase Emulator.
```

---

## Phase 10: Deployment

### Prompt 10.1: Production Deployment

```
Create deployment configurations for production:

REQUIREMENTS:

1. Firebase deployment:
   - Firestore security rules deployment
   - Indexes deployment
   - Cloud Functions deployment
   - Hosting configuration

2. Android app:
   - Release build configuration
   - ProGuard/R8 rules
   - Signing configuration
   - Play Store deployment
   - Firebase App Distribution

3. Web app:
   - Production build optimization
   - Firebase Hosting deployment
   - Environment variable configuration
   - Custom domain setup (optional)

4. CI/CD pipeline:
   - GitHub Actions workflow
   - Automated testing
   - Build artifacts
   - Deployment stages

5. Monitoring:
   - Firebase Crashlytics
   - Performance monitoring
   - Analytics setup
   - Error alerting

DELIVERABLE:
Complete deployment documentation and configuration files for production release.
```

---

## Quick Reference: File Structure

```
app/src/main/java/com/example/eventpay/
├── data/
│   ├── auth/
│   │   └── AuthRepositoryImpl.kt
│   ├── firebase/
│   │   ├── FirebaseService.kt
│   │   ├── FirestoreEventRepository.kt
│   │   ├── FirestoreTicketRepository.kt
│   │   └── FirestoreTransactionRepository.kt
│   ├── local/
│   │   ├── AppDatabase.kt
│   │   ├── dao/
│   │   │   ├── EventDao.kt
│   │   │   ├── TicketDao.kt
│   │   │   ├── UserDao.kt
│   │   │   └── OfflineDao.kt
│   │   └── entity/
│   │       ├── UserEntity.kt
│   │       ├── EventEntity.kt
│   │       ├── TicketEntity.kt
│   │       └── PendingCheckInEntity.kt
│   ├── model/
│   │   ├── User.kt
│   │   ├── Event.kt
│   │   └── Ticket.kt
│   ├── mapper/
│   │   └── Mappers.kt
│   └── repository/
│       ├── EventRepositoryImpl.kt
│       └── TicketRepositoryImpl.kt
├── domain/
│   ├── auth/
│   │   ├── AuthRepository.kt
│   │   └── AuthModels.kt
│   ├── model/
│   │   ├── User.kt
│   │   ├── Event.kt
│   │   └── Ticket.kt
│   ├── qrcode/
│   │   ├── QRCodeGenerator.kt
│   │   ├── QRCodeValidator.kt
│   │   └── QRCodeModels.kt
│   ├── repository/
│   │   ├── EventRepository.kt
│   │   └── TicketRepository.kt
│   └── usecase/
│       ├── auth/
│       ├── event/
│       ├── ticket/
│       └── scanner/
├── ui/
│   ├── auth/
│   │   ├── AuthViewModel.kt
│   │   ├── LoginScreen.kt
│   │   └── RegisterScreen.kt
│   ├── admin/
│   │   ├── AdminViewModel.kt
│   │   └── AdminComponents.kt
│   ├── scanner/
│   │   ├── ScannerViewModel.kt
│   │   ├── QRScannerViewModel.kt
│   │   └── ScannerComponents.kt
│   ├── screens/
│   │   ├── SplashScreen.kt
│   │   ├── HomeScreen.kt
│   │   ├── DashboardScreen.kt
│   │   ├── admin/
│   │   │   ├── AdminHomeScreen.kt
│   │   │   ├── AdminEventListScreen.kt
│   │   │   └── AdminUserManagementScreen.kt
│   │   └── scanner/
│   │       ├── ScannerHomeScreen.kt
│   │       └── QRScannerScreen.kt
│   ├── navigation/
│   │   ├── NavGraph.kt
│   │   └── NavRoutes.kt
│   └── theme/
│       ├── Color.kt
│       ├── Theme.kt
│       └── Type.kt
├── di/
│   ├── AppContainer.kt
│   └── RepositoryModule.kt
├── security/
│   └── QRCryptoManager.kt
└── util/
    └── NetworkUtils.kt
```

---

*Document Version: 1.0*
*Last Updated: 2026-02-28*
