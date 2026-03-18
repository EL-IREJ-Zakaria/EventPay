# EventPay

**EventPay** is a comprehensive Android event management and ticketing application built with Jetpack Compose and Firebase. It provides a complete solution for event organizers to manage events, sell tickets, and validate attendees through QR code scanning.

## 📱 Features

### Admin Features
- **Event Management**: Create, edit, and manage events with detailed information
- **User Management**: Create and manage scanner accounts with role-based access
- **Participant Management**: Add manual participants and track attendees
- **Analytics Dashboard**: Real-time event statistics and insights
- **Multi-Event Support**: Manage multiple events simultaneously
- **Scanner Assignment**: Assign specific scanners to events

### Scanner Features
- **QR Code Scanning**: Fast and reliable ticket validation using camera
- **Offline Mode**: Continue scanning even without internet connection
- **Real-time Sync**: Automatic synchronization when connection is restored
- **Check-in History**: View all scanned tickets and check-in records
- **Event Selection**: Access assigned events for scanning

### Security Features
- **Biometric Authentication**: Fingerprint/Face unlock support
- **Anti-Fraud System**: Duplicate ticket detection and validation
- **Screenshot Protection**: Prevent QR code screenshot fraud
- **Audit Logging**: Complete audit trail of all operations
- **Encrypted QR Codes**: Secure QR code generation with cryptographic signatures

### Technical Features
- **Offline-First Architecture**: Full offline support with automatic sync
- **Multi-Device Sync**: Real-time synchronization across multiple devices
- **Conflict Resolution**: Intelligent conflict resolution for concurrent operations
- **Push Notifications**: Firebase Cloud Messaging for real-time updates
- **Room Database**: Local data persistence with SQLite
- **Firebase Integration**: Cloud storage, authentication, and real-time database

## 🏗️ Architecture

EventPay follows Clean Architecture principles with clear separation of concerns:

```
app/
├── data/                    # Data layer
│   ├── auth/               # Authentication implementation
│   ├── firebase/           # Firebase services
│   ├── local/              # Room database (DAO, entities)
│   ├── mapper/             # Data mappers
│   ├── model/              # Data models
│   ├── repository/         # Repository implementations
│   └── sync/               # Sync managers
├── domain/                  # Domain layer
│   ├── antifraud/          # Fraud detection logic
│   ├── auth/               # Auth domain models
│   ├── model/              # Domain models
│   ├── qrcode/             # QR code generation/validation
│   ├── repository/         # Repository interfaces
│   ├── sync/               # Multi-device sync
│   └── usecase/            # Business logic use cases
├── di/                      # Dependency injection
├── notification/            # Push notifications
├── security/                # Security managers
├── ui/                      # Presentation layer
│   ├── admin/              # Admin screens
│   ├── auth/               # Authentication screens
│   ├── cashier/            # Cashier functionality
│   ├── components/         # Reusable UI components
│   ├── dashboard/          # Analytics dashboard
│   ├── event/              # Event management
│   ├── navigation/         # Navigation setup
│   ├── qrcode/             # QR code UI
│   ├── scanner/            # Scanner screens
│   ├── screens/            # All app screens
│   └── theme/              # Material Design theme
└── util/                    # Utilities
```

## 🛠️ Tech Stack

### Core
- **Kotlin** - Primary programming language
- **Jetpack Compose** - Modern declarative UI framework
- **Material Design 3** - UI design system
- **Coroutines & Flow** - Asynchronous programming

### Architecture Components
- **ViewModel** - UI state management
- **Room** - Local database
- **Navigation Compose** - Navigation framework
- **DataStore** - Preferences storage
- **WorkManager** - Background tasks

### Firebase
- **Firebase Authentication** - User authentication
- **Cloud Firestore** - Cloud database
- **Realtime Database** - Real-time sync
- **Cloud Messaging** - Push notifications
- **Cloud Storage** - File storage
- **Crashlytics** - Crash reporting
- **Analytics** - Usage analytics

### Camera & QR
- **CameraX** - Camera API
- **ML Kit Barcode Scanning** - QR code scanning
- **ZXing** - QR code generation

### Security
- **Biometric API** - Fingerprint/Face authentication
- **Custom Crypto** - QR code encryption

### Networking
- **Retrofit** - REST API client
- **OkHttp** - HTTP client
- **Gson** - JSON serialization

### Dependency Injection
- **Hilt** - Dependency injection framework

### UI
- **Coil** - Image loading
- **Accompanist** - Compose utilities
- **Material Icons Extended** - Icon library

## 📋 Prerequisites

- Android Studio Hedgehog or later
- JDK 11 or higher
- Android SDK 24+ (Android 7.0+)
- Firebase project with:
  - Authentication enabled
  - Cloud Firestore database
  - Realtime Database
  - Cloud Messaging
  - Cloud Storage

## 🚀 Getting Started

### 1. Clone the Repository
```bash
git clone https://github.com/yourusername/eventpay.git
cd eventpay
```

### 2. Firebase Setup
1. Create a Firebase project at [Firebase Console](https://console.firebase.google.com/)
2. Add an Android app to your Firebase project
3. Download `google-services.json`
4. Place it in `app/` directory
5. Enable Authentication (Email/Password)
6. Create Firestore database
7. Enable Realtime Database
8. Set up Cloud Messaging

### 3. Firestore Security Rules
Deploy the security rules from `docs/firebase/`:
```bash
cd docs/firebase
firebase deploy --only firestore:rules
firebase deploy --only storage
```

### 4. Build and Run
```bash
./gradlew assembleDebug
```

Or open in Android Studio and click Run.

## 📱 User Roles

### Admin
- Full access to all features
- Create and manage events
- Create scanner accounts
- View analytics and reports
- Manage participants

### Scanner
- Scan QR codes for ticket validation
- View assigned events
- Access check-in history
- Offline scanning capability

## 🔐 Default Credentials

For testing purposes, create an admin account through the registration screen. The first user registered can be set as admin by updating the Firestore document.

## 📊 Database Schema

### Events Collection
```
events/
  {eventId}/
    - name: string
    - description: string
    - date: timestamp
    - location: string
    - capacity: number
    - ticketPrice: number
    - status: enum
    - organizerId: string
```

### Tickets Collection
```
tickets/
  {ticketId}/
    - eventId: string
    - userId: string
    - qrCode: string
    - ticketType: enum
    - isCheckedIn: boolean
    - checkedInAt: timestamp
```

### Users Collection
```
users/
  {userId}/
    - email: string
    - fullName: string
    - role: enum (ADMIN, SCANNER)
    - assignedEvents: array
    - isActive: boolean
```

## 🧪 Testing

Run unit tests:
```bash
./gradlew test
```

Run instrumented tests:
```bash
./gradlew connectedAndroidTest
```

## 📦 Build Variants

- **Debug**: Development build with debugging enabled
- **Release**: Production build with ProGuard optimization

## 🔒 Security Considerations

- QR codes are encrypted with cryptographic signatures
- Biometric authentication for sensitive operations
- Anti-fraud detection for duplicate tickets
- Screenshot protection on QR code screens
- Secure communication with Firebase
- Audit logging for all critical operations

## 🌐 Offline Support

EventPay works seamlessly offline:
- All data cached locally using Room database
- Automatic sync when connection restored
- Conflict resolution for concurrent edits
- Queue-based sync mechanism

## 📈 Performance

- Optimized for low-end devices (minSdk 24)
- Efficient image loading with Coil
- Lazy loading for large lists
- ProGuard optimization in release builds
- Resource shrinking enabled

## 🤝 Contributing

Contributions are welcome! Please follow these steps:
1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 👥 Authors

- Your Name - Initial work

## 🙏 Acknowledgments

- Firebase for backend infrastructure
- Jetpack Compose team for the amazing UI framework
- ZXing for QR code generation
- ML Kit for barcode scanning

## 📞 Support

For support, email support@eventpay.com or open an issue in the repository.

## 🗺️ Roadmap

- [ ] Payment gateway integration
- [ ] Email ticket delivery
- [ ] Advanced analytics
- [ ] Multi-language support
- [ ] Dark mode
- [ ] Export reports (PDF/Excel)
- [ ] Social media integration
- [ ] Event templates
- [ ] Bulk ticket import
- [ ] Custom branding

## 📱 Screenshots

_Add screenshots of your app here_

## 🔧 Troubleshooting

### Camera Permission Issues
Ensure camera permissions are granted in device settings.

### Firebase Connection Issues
Check `google-services.json` is properly configured.

### Build Errors
Run `./gradlew clean` and rebuild the project.

### Sync Issues
Clear app data and re-login to force a fresh sync.

---

**Made with ❤️ using Jetpack Compose**
