# EventPay System Documentation

Welcome to the EventPay system documentation. This folder contains comprehensive guides for building and deploying your event management and QR scanning application.

## 📚 Documentation Structure

```
docs/
├── README.md                          # This file
├── EventPay_System_Design.md          # Complete system architecture
├── Development_Prompts.md             # AI prompts for building the system
├── firebase/
│   ├── firestore.rules               # Security rules
│   ├── firestore.indexes.json        # Database indexes
│   └── Firebase_Setup_Guide.md       # Firebase configuration
```

---

## 🎯 System Overview

EventPay is a dual-role event management system:

| Role | Purpose | Key Features |
|------|---------|--------------|
| **ADMIN** | Event organizers | Create events, manage scanners, view analytics, generate tickets |
| **SCANNER** | Event staff | Scan QR codes, validate tickets, record check-ins |

---

## 🚀 Quick Start

### 1. Read the System Design
Start with [`EventPay_System_Design.md`](EventPay_System_Design.md) to understand:
- Architecture overview
- Role definitions and permissions
- Firebase schema design
- Security rules
- QR code security

### 2. Set Up Firebase
Follow [`firebase/Firebase_Setup_Guide.md`](firebase/Firebase_Setup_Guide.md) to:
- Create Firebase project
- Configure authentication
- Deploy security rules
- Set up database indexes

### 3. Use Development Prompts
Use [`Development_Prompts.md`](Development_Prompts.md) to build the system:
- Copy prompts into your AI coding assistant
- Follow the phased approach
- Each prompt produces a working component

---

## 📋 Development Phases

| Phase | Focus | Duration |
|-------|-------|----------|
| **1** | Firebase Backend Setup | Week 1 |
| **2** | Data Models & Domain Layer | Week 1 |
| **3** | Repository Layer | Week 2 |
| **4** | Authentication UI | Week 2 |
| **5** | Admin Features | Week 3 |
| **6** | Scanner Features | Week 3 |
| **7** | Security & Offline Support | Week 4 |
| **8** | Web Application | Week 5 |
| **9** | Testing | Week 6 |
| **10** | Deployment | Week 6 |

---

## 🔐 Security Highlights

- **Role-Based Access Control**: ADMIN vs SCANNER permissions
- **Secure QR Codes**: HMAC-SHA256 signatures with expiration
- **Firestore Security Rules**: Document-level access control
- **Audit Logging**: Track all system actions
- **Offline Protection**: Queue and validate on sync

---

## 🏗️ Architecture

### Clean Architecture Layers

```
Presentation Layer (UI/Compose)
         ↓
   Domain Layer (Use Cases/Entities)
         ↓
    Data Layer (Repositories)
         ↓
   Firebase / Local DB
```

### Key Technologies

| Layer | Technology |
|-------|------------|
| Mobile | Kotlin + Jetpack Compose |
| Web | Next.js + TypeScript |
| Backend | Firebase (Auth, Firestore, Storage) |
| Database | Cloud Firestore + Room (local) |
| QR Security | AES-256-GCM + HMAC-SHA256 |

---

## 📱 User Flows

### Admin Flow
```
Login → Dashboard → Create Event → Assign Scanners → Monitor Analytics
```

### Scanner Flow
```
Login → Select Event → Scan QR → Validate → Confirm Check-in
```

---

## 🎨 UI/UX Design

- **Modern Material Design 3**
- **Role-based navigation**
- **Offline indicators**
- **Real-time updates**
- **Responsive layouts**

---

## 🧪 Testing Strategy

- Unit tests for domain logic
- Integration tests for repositories
- UI tests for critical flows
- Security rule tests
- End-to-end testing

---

## 📦 Deliverables

By following this documentation, you will have:

1. ✅ Fully functional Android app
2. ✅ Web admin dashboard
3. ✅ Secure Firebase backend
4. ✅ QR code scanning system
5. ✅ Offline support
6. ✅ Analytics dashboard
7. ✅ User management
8. ✅ Event management

---

## 🆘 Support

### Common Issues

| Issue | Solution |
|-------|----------|
| Permission denied | Check Firestore rules, verify user role |
| QR not scanning | Check camera permissions, lighting |
| Offline not working | Verify Room database setup |
| Sync failures | Check network, review sync manager logs |

### Firebase Console URLs

- Authentication: `https://console.firebase.google.com/project/YOUR_PROJECT/authentication`
- Firestore: `https://console.firebase.google.com/project/YOUR_PROJECT/firestore`
- Storage: `https://console.firebase.google.com/project/YOUR_PROJECT/storage`

---

**Ready to start?** → Go to [`Development_Prompts.md`](Development_Prompts.md) and follow Phase 1!
