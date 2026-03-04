# Actionable Prompts for Your EventPay Project

These prompts are specifically designed for YOUR existing codebase, addressing the gaps identified in the analysis.

---

## 🔴 CRITICAL: Deploy Firebase Security (Do This First)

### Prompt 1: Deploy Firestore Rules

```
I have an existing EventPay Android project with Firebase. I need to deploy Firestore security rules from the file docs/firebase/firestore.rules to my Firebase project.

STEPS NEEDED:
1. Install Firebase CLI if not already installed
2. Login to Firebase
3. Initialize Firestore in the project
4. Copy the rules file to the correct location
5. Deploy the rules

CURRENT PROJECT STRUCTURE:
- Project root: C:/Users/AdMin/AndroidStudioProjects/EventPay
- Rules file: docs/firebase/firestore.rules
- Indexes file: docs/firebase/firestore.indexes.json

Provide the exact commands to run in order.
```

### Prompt 2: Fix Scanner Event Visibility (CRITICAL SECURITY FIX)

```
I need to fix a security issue in my EventPay app. Currently, scanners can see ALL events, but they should only see events they're assigned to.

CURRENT IMPLEMENTATION:
File: app/src/main/java/com/example/eventpay/data/firebase/FirebaseService.kt
Method: getActiveEvents() returns all events

File: app/src/main/java/com/example/eventpay/ui/scanner/ScannerViewModel.kt
Method: loadActiveEvents() calls getActiveEvents()

REQUIRED CHANGES:
1. Modify FirestoreEventRepository to add method:
   getEventsForScanner(scannerId: String): Result<List<Event>>
   - Query: whereArrayContains("assignedScanners", scannerId)
   - Only return events where date >= today - 1 day

2. Update ScannerViewModel.loadActiveEvents() to:
   - Get current user ID from firebaseService
   - Call the new getEventsForScanner() method
   - Handle null user case

3. Update the Event data model to include assignedScanners field if missing

The Firestore collection "events" has an array field "assignedScanners" that contains user IDs of assigned scanners.

DELIVERABLE:
Complete modified files with the security fix implemented.
```

---

## 🟠 HIGH PRIORITY: Admin Scanner Management

### Prompt 3: Create Scanner Creation Flow for Admin

```
I need to implement a feature where Admin users can create Scanner accounts without logging out.

CURRENT STATE:
- AdminHomeScreen exists (app/src/main/java/com/example/eventpay/ui/screens/admin/AdminHomeScreen.kt)
- AuthRepositoryImpl has authentication logic
- No UI for creating scanners yet

REQUIREMENTS:

1. Create CreateScannerDialog composable:
   - Full name input field
   - Email input field with validation
   - Password generation (auto-generate strong password)
   - Event assignment (multi-select from available events)
   - Create button with loading state
   - Success message showing credentials

2. Add method to FirebaseService:
   createScannerAccount(email, password, fullName, assignedEvents, createdByAdminId): Result<String>
   - Use FirebaseAuth.getInstance() for secondary auth (don't log out current admin)
   - Create user in Auth
   - Create user document in Firestore with role = "SCANNER"
   - Create scannerAssignments documents for each assigned event

3. Update AdminUserManagementScreen:
   - Add FAB to open CreateScannerDialog
   - Show list of existing scanners
   - Show scanner status (active/inactive)

4. Add to AdminViewModel:
   - createScanner() method
   - loadScanners() method
   - State management for dialog

UI DESIGN:
- Use existing theme from com.example.eventpay.ui.theme
- Match the modern design style of existing screens
- Use Material3 components

DELIVERABLE:
Complete implementation files with scanner creation flow.
```

### Prompt 4: Implement Event CRUD for Admin

```
I need to implement full event management for Admin users.

CURRENT STATE:
- Admin can view events list (AdminEventListScreen exists)
- No create/edit event functionality
- No event detail view

REQUIREMENTS:

1. Create CreateEventScreen:
   - Event name input
   - Description textarea
   - Location input
   - Date picker (start date/time)
   - End date picker
   - Ticket price input
   - Total tickets input
   - VIP ticket options (price, count)
   - Event category dropdown
   - Event image picker (from gallery)
   - Scanner assignment section
   - Save as draft / Publish toggle
   - Form validation

2. Create EventDetailScreen:
   - Display full event info
   - Large header image
   - Ticket statistics (sold/available)
   - Check-in statistics
   - Assigned scanners list
   - Action buttons: Edit, Publish, Cancel, Delete
   - List of tickets for this event

3. Update FirestoreEventRepository:
   - createEvent(event): Result<String>
   - updateEvent(event): Result<Unit>
   - deleteEvent(eventId): Result<Unit>
   - publishEvent(eventId): Result<Unit>
   - uploadEventImage(imageUri): Result<String> (returns download URL)

4. Update AdminEventListScreen:
   - Add FAB to navigate to CreateEventScreen
   - Add click handler to navigate to EventDetailScreen
   - Add filter chips (All, Draft, Published, Completed)

5. Add to AdminViewModel:
   - Event creation state management
   - Image upload handling
   - Form validation

NAVIGATION:
- Integrate with existing MainActivity.kt navigation
- Handle back navigation properly

DELIVERABLE:
Complete event management implementation with all screens and functionality.
```

---

## 🟡 CHECK-IN SYSTEM COMPLETION

### Prompt 5: Complete Check-In Recording

```
I need to complete the check-in recording system. Currently QR codes can be scanned but check-ins aren't properly recorded.

CURRENT STATE:
- QR scanning works (ScannerHomeScreen.kt, QRScannerScreen.kt)
- QR validation exists (QRCodeValidator.kt)
- ScannerViewModel.processQRCode() exists but incomplete

REQUIRED CHANGES:

1. Update FirestoreTicketRepository:
   Add method recordCheckIn(checkInData): Result<Unit>
   - Use Firestore transaction for atomic operation
   - Create document in "checkIns" collection
   - Update ticket document: status = "USED", checkedInAt, checkedInBy
   - Update event document: increment checkedInCount
   - All operations must succeed or all fail (transaction)

2. Create data class CheckInRecord:
   - id: String
   - ticketId: String
   - eventId: String
   - userId: String (attendee)
   - scannedBy: String (scanner)
   - scannedByName: String
   - scannedAt: Timestamp
   - deviceId: String
   - result: String (SUCCESS, ALREADY_SCANNED, etc.)
   - location: GeoPoint (optional)

3. Update ScannerViewModel.processQRCode():
   After successful QR validation:
   - Call recordCheckIn()
   - Update UI state to show success
   - Play success sound
   - Increment session check-in count
   - Add to scannedInSession set
   - Show attendee name from ticket

4. Handle errors:
   - Duplicate scan (ticket already checked in)
   - Wrong event (ticket for different event)
   - Invalid QR code
   - Network error (queue for offline sync)

5. Create ScanHistoryScreen:
   - List of today's check-ins by this scanner
   - Filter by success/error
   - Search by ticket ID
   - Statistics (total scans, success rate)

DELIVERABLE:
Complete check-in recording system with transaction safety and history tracking.
```

---

## 🟢 WEB APPLICATION

### Prompt 6: Setup Next.js Web App

```
I need to create a web application that connects to the same Firebase backend as my EventPay Android app.

REQUIREMENTS:

1. Project Setup:
   Create Next.js 14 project with:
   - TypeScript
   - Tailwind CSS
   - Firebase SDK v10+
   - Shadcn/ui components
   - Zustand for state management

2. Firebase Configuration:
   - Use the same Firebase project as Android app
   - Configure Firebase Auth
   - Configure Firestore
   - Configure Storage

3. Project Structure:
   web/
   ├── src/
   │   ├── app/
   │   │   ├── (auth)/
   │   │   │   └── login/page.tsx
   │   │   ├── (admin)/
   │   │   │   ├── dashboard/page.tsx
   │   │   │   ├── events/page.tsx
   │   │   │   └── users/page.tsx
   │   │   └── layout.tsx
   │   ├── components/
   │   ├── lib/
   │   │   └── firebase.ts
   │   └── types/
   ├── public/
   └── package.json

4. Authentication:
   - Login page with email/password
   - Role-based redirect (same as Android)
   - Protected routes middleware
   - Session persistence

5. Admin Dashboard (Web):
   - Stats cards (reuse logic from Android)
   - Events list
   - Create event form
   - User management

DELIVERABLE:
Complete Next.js project setup with Firebase integration and basic admin dashboard.
```

---

## 🔵 CODE QUALITY IMPROVEMENTS

### Prompt 7: Implement Jetpack Navigation

```
I want to refactor my existing navigation system to use Jetpack Navigation Compose instead of manual routing.

CURRENT STATE:
File: app/src/main/java/com/example/eventpay/MainActivity.kt
Uses manual screen routing with sealed class Screen and when expression

REQUIREMENTS:

1. Add dependencies:
   - androidx.navigation:navigation-compose:2.7.7
   - Use type-safe navigation with @Serializable

2. Define routes:
   @Serializable
   sealed class Route {
       @Serializable data object Splash : Route()
       @Serializable data object Login : Route()
       @Serializable data object Register : Route()
       @Serializable data object AdminHome : Route()
       @Serializable data object AdminEvents : Route()
       @Serializable data object AdminUsers : Route()
       @Serializable data class EventDetail(val eventId: String) : Route()
       @Serializable data object ScannerHome : Route()
       @Serializable data class QRScanner(val eventId: String) : Route()
   }

3. Create NavGraph:
   - Replace manual routing in MainActivity
   - Use NavHost with composable<Route>()
   - Handle navigation in ViewModels using NavigationManager
   - Pass arguments safely

4. Update all screens:
   - Remove onNavigate callbacks
   - Use navController.navigate(Route.XXX)
   - Handle back navigation properly

5. Deep linking support:
   - Handle event detail deep links
   - Handle scanner assignment links

DELIVERABLE:
Complete migration to Jetpack Navigation with type-safe routes.
```

### Prompt 8: Unify Error Handling

```
I want to improve error handling in my EventPay app by using sealed classes instead of string messages.

CURRENT STATE:
- Errors are passed as String? in UI state
- Inconsistent error handling across ViewModels
- No typed error categories

REQUIREMENTS:

1. Create sealed class AppError:
   sealed class AppError(@StringRes val messageResId: Int) {
       object NetworkError : AppError(R.string.error_network)
       object AuthError : AppError(R.string.error_auth)
       object PermissionError : AppError(R.string.error_permission)
       object NotFoundError : AppError(R.string.error_not_found)
       object ValidationError : AppError(R.string.error_validation)
       object DuplicateError : AppError(R.string.error_duplicate)
       data class UnknownError(val throwable: Throwable) : AppError(R.string.error_unknown)
   }

2. Create Result wrapper:
   sealed class Result<out T> {
       data class Success<T>(val data: T) : Result<T>()
       data class Error(val error: AppError) : Result<Nothing>()
       object Loading : Result<Nothing>()
   }

3. Update all ViewModels:
   - Replace UiState with sealed class
   - Use Result<T> for operations
   - Provide user-friendly error messages
   - Add error recovery actions

4. Update UI:
   - Create ErrorDialog composable
   - Show retry buttons where applicable
   - Display error messages from string resources

5. Add error logging:
   - Log errors to Firebase Crashlytics
   - Include context in error reports

DELIVERABLE:
Complete error handling refactoring with sealed classes and user-friendly messages.
```

---

## 📱 TESTING

### Prompt 9: Add Unit Tests

```
I need to add comprehensive unit tests for my EventPay app.

CURRENT STATE:
- No unit tests exist yet
- app/src/test/java/ is empty

REQUIREMENTS:

1. Test Setup:
   - Add testing dependencies (JUnit, MockK, Coroutines Test)
   - Create test utilities
   - Setup test dispatchers

2. Domain Layer Tests:
   - Test User model validation
   - Test UserRole permissions
   - Test Event business logic
   - Test Ticket validation

3. ViewModel Tests:
   - Test AuthViewModel login/logout
   - Test ScannerViewModel event loading
   - Test AdminViewModel dashboard loading
   - Mock repositories for isolation

4. Repository Tests:
   - Test Firebase service calls
   - Test Room database operations
   - Test error handling

5. Test Data:
   - Create test data factories
   - Provide fake data for tests

DELIVERABLE:
Complete unit test suite with good coverage of critical paths.
```

---

## 📋 Summary: What to Do First

### Week 1 (Critical)
1. Deploy Firebase rules and indexes
2. Fix scanner event visibility security issue
3. Create initial admin user

### Week 2 (High Priority)
4. Implement scanner creation flow
5. Complete event CRUD operations

### Week 3 (Features)
6. Complete check-in recording
7. Add scan history screen

### Week 4+ (Enhancement)
8. Create web application
9. Add tests
10. Refactor navigation

---

*Use these prompts one at a time with your AI coding assistant for best results.*
