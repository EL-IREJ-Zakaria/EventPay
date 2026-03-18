# Cahier de Charges — EventPay

**Projet :** EventPay  
**Type :** Application Mobile Android  
**Version du document :** 1.0  
**Date :** Mars 2026  
**Statut :** Production-Ready  

---

## Table des Matières

1. [Présentation Générale](#1-présentation-générale)
2. [Contexte et Objectifs](#2-contexte-et-objectifs)
3. [Périmètre Fonctionnel](#3-périmètre-fonctionnel)
4. [Rôles et Acteurs](#4-rôles-et-acteurs)
5. [Spécifications Fonctionnelles Détaillées](#5-spécifications-fonctionnelles-détaillées)
6. [Spécifications Techniques](#6-spécifications-techniques)
7. [Architecture Logicielle](#7-architecture-logicielle)
8. [Base de Données](#8-base-de-données)
9. [Sécurité](#9-sécurité)
10. [Mode Hors-ligne et Synchronisation](#10-mode-hors-ligne-et-synchronisation)
11. [Interface Utilisateur et Ergonomie](#11-interface-utilisateur-et-ergonomie)
12. [Notifications Push](#12-notifications-push)
13. [Performance et Contraintes](#13-performance-et-contraintes)
14. [Tests et Qualité](#14-tests-et-qualité)
15. [Déploiement et Distribution](#15-déploiement-et-distribution)
16. [Maintenance et Support](#16-maintenance-et-support)
17. [Évolutions Futures](#17-évolutions-futures)
18. [Risques et Mitigations](#18-risques-et-mitigations)
19. [Glossaire](#19-glossaire)

---

## 1. Présentation Générale

**EventPay** est une application mobile Android dédiée à la **gestion complète d'événements et à la validation de billets par QR code**. Elle s'adresse aux organisateurs d'événements, au personnel de sécurité/caisse, et aux agents de contrôle à l'entrée.

L'application couvre l'intégralité du cycle de vie d'un événement :

- Création et configuration d'événements
- Génération et attribution de billets sécurisés
- Validation à l'entrée par scan QR
- Suivi en temps réel des entrées et statistiques
- Fonctionnement hors-ligne avec synchronisation automatique

---

## 2. Contexte et Objectifs

### 2.1 Problématique

Les organisateurs d'événements font face à plusieurs défis :

- **Fraude sur les billets** : duplications, faux QR codes, revente illicite
- **Connectivité instable** : les lieux d'événements n'ont pas toujours de bonne connexion internet
- **Multi-équipes** : plusieurs agents de scan travaillant en parallèle sur différentes entrées
- **Suivi en temps réel** : besoin de statistiques instantanées sur les entrées

### 2.2 Objectifs du Projet

| Priorité | Objectif |
|----------|----------|
| P1 | Permettre la validation de billets QR offline avec synchronisation différée |
| P1 | Assurer un système anti-fraude robuste (détection de doublons, codes signés cryptographiquement) |
| P1 | Fournir une gestion complète des événements (création, édition, participants) |
| P2 | Offrir un tableau de bord analytique en temps réel |
| P2 | Supporter plusieurs appareils simultanément avec résolution de conflits |
| P3 | Proposer une interface caisse pour la vente de billets sur site |

---

## 3. Périmètre Fonctionnel

### 3.1 Fonctionnalités Incluses

- Authentification (email/mot de passe + biométrie)
- Gestion des rôles (Admin, Scanner)
- Création et gestion d'événements
- Génération de billets avec QR codes chiffrés
- Scan et validation de billets
- Gestion manuelle des participants
- Tableau de bord analytique
- Mode caisse (vente de billets sur site)
- Notifications push
- Fonctionnement hors-ligne complet
- Synchronisation multi-appareils
- Journal d'audit
- Protection anti-fraude et anti-capture d'écran

### 3.2 Fonctionnalités Exclues (hors périmètre v1.0)

- Passerelle de paiement en ligne (Stripe, PayPal)
- Envoi des billets par email en PDF
- Application attendees (public)
- Tableau de bord web (desktop)
- Multi-langue (i18n)
- Mode sombre
- Export de rapports (PDF/Excel)
- Intégration réseaux sociaux

---

## 4. Rôles et Acteurs

### 4.1 Administrateur (Admin)

L'administrateur dispose d'un accès complet à toutes les fonctionnalités de l'application.

**Responsabilités :**

- Créer, modifier et gérer les événements
- Créer et gérer les comptes scanners
- Affecter des scanners à des événements précis
- Ajouter manuellement des participants
- Consulter les statistiques et rapports
- Consulter les journaux d'audit
- Gérer les types de billets et les capacités

### 4.2 Scanner

Le scanner est un agent de terrain chargé uniquement du contrôle à l'entrée.

**Responsabilités :**

- Sélectionner l'événement qui lui est assigné
- Scanner les QR codes des participants
- Consulter l'historique des entrées de sa session
- Travailler en mode hors-ligne si nécessaire

### 4.3 Caissier (Cashier)

Rôle dédié à la vente de billets sur site lors de l'événement.

**Responsabilités :**

- Créer des billets à la volée pour des acheteurs
- Gérer les transactions de vente

---

## 5. Spécifications Fonctionnelles Détaillées

### 5.1 Module Authentification

| ID | Fonctionnalité | Description |
|----|---------------|-------------|
| AUTH-01 | Connexion email/mot de passe | Via Firebase Authentication |
| AUTH-02 | Inscription | Création de compte avec validation email |
| AUTH-03 | Authentification biométrique | Empreinte digitale ou reconnaissance faciale |
| AUTH-04 | Déconnexion automatique | Session expirée après inactivité |
| AUTH-05 | Gestion de session | Token sécurisé, persistance locale |
| AUTH-06 | Réinitialisation mot de passe | Par email via Firebase |

### 5.2 Module Gestion des Événements (Admin)

| ID | Fonctionnalité | Description |
|----|---------------|-------------|
| EVT-01 | Créer un événement | Nom, description, date de début/fin, lieu, capacité, types de billets |
| EVT-02 | Modifier un événement | Édition de tous les champs sauf si des billets vendus le bloquent |
| EVT-03 | Statuts de l'événement | Draft → Published → Ongoing → Completed / Cancelled |
| EVT-04 | Types de billets | Standard, VIP, Early Bird avec prix distincts |
| EVT-05 | Gestion de capacité | Suivi tickets disponibles vs réservés en temps réel |
| EVT-06 | Catégories d'événements | Conférence, Concert, Workshop, Sport, Festival, etc. |
| EVT-07 | Affecter des scanners | Lier des comptes scanner à un événement spécifique |
| EVT-08 | Supprimer un événement | Uniquement si aucun ticket réservé (règle Firestore) |
| EVT-09 | Voir les participants | Liste complète des participants avec statut check-in |

### 5.3 Module Gestion des Utilisateurs (Admin)

| ID | Fonctionnalité | Description |
|----|---------------|-------------|
| USR-01 | Créer un compte scanner | Génération de credentials pour agents de terrain |
| USR-02 | Activer/Désactiver un compte | Contrôle d'accès sans suppression définitive |
| USR-03 | Affecter à des événements | Définir les événements accessibles par chaque scanner |
| USR-04 | Consulter la liste des utilisateurs | Vue complète de tous les comptes avec rôles |
| USR-05 | Modifier un utilisateur | Mise à jour du profil, rôle, statut |
| USR-06 | Supprimer un utilisateur | Admin uniquement, impossible de se supprimer soi-même |

### 5.4 Module Gestion des Participants (Admin)

| ID | Fonctionnalité | Description |
|----|---------------|-------------|
| PAR-01 | Ajout manuel de participant | Enregistrer un participant sans inscription en ligne |
| PAR-02 | Attribution de type de billet | Assigner Standard, VIP ou Early Bird |
| PAR-03 | Génération de QR code | Généré automatiquement à la création du billet |
| PAR-04 | Suivi du statut check-in | Voir qui est arrivé, qui ne l'est pas encore |
| PAR-05 | Recherche de participants | Par nom, email, ou numéro de billet |

### 5.5 Module Scanner / Validation QR

| ID | Fonctionnalité | Description |
|----|---------------|-------------|
| SCN-01 | Sélection d'événement | Le scanner voit uniquement ses événements assignés |
| SCN-02 | Scan QR via caméra | Reconnaissance instantanée par ML Kit (CameraX) |
| SCN-03 | Validation en ligne | Vérification en temps réel via Firestore |
| SCN-04 | Validation hors-ligne | Validation via cache local Room |
| SCN-05 | Détection de doublon | Refus si billet déjà scanné (en ligne et hors-ligne) |
| SCN-06 | Feedback visuel | Indicateur vert/rouge clair selon résultat |
| SCN-07 | Feedback sonore et vibration | Alerte audio + vibration pour chaque scan |
| SCN-08 | Résultats possibles | SUCCESS, ALREADY_SCANNED, INVALID, NOT_FOUND, WRONG_EVENT, EXPIRED, CANCELLED, NO_PERMISSION |
| SCN-09 | Historique de scan | Liste des derniers scans de la session |
| SCN-10 | Filtrage de l'historique | Par heure, statut, type de billet |

### 5.6 Module Tableau de Bord Analytique (Admin)

| ID | Fonctionnalité | Description |
|----|---------------|-------------|
| ANA-01 | Statistiques en temps réel | Nombre d'entrées en direct |
| ANA-02 | Taux de check-in | % de participants enregistrés vs total |
| ANA-03 | Distribution par type de billet | Répartition Standard / VIP / Early Bird |
| ANA-04 | Courbe temporelle des entrées | Flux d'entrées par tranche horaire |
| ANA-05 | Comparaison multi-événements | Vue comparative de plusieurs événements |
| ANA-06 | Capacité restante | Tickets disponibles en temps réel |

### 5.7 Module Caisse (Cashier)

| ID | Fonctionnalité | Description |
|----|---------------|-------------|
| CSH-01 | Vente de billet sur site | Création immédiate de billet avec génération QR |
| CSH-02 | Sélection du type de billet | Standard, VIP, etc. |
| CSH-03 | Enregistrement de transaction | Traçabilité de la vente |
| CSH-04 | Historique des ventes | Consultation des transactions de la caisse |

### 5.8 Module Notifications Push

| ID | Fonctionnalité | Description |
|----|---------------|-------------|
| NOT-01 | Notification push Firebase | Via Firebase Cloud Messaging (FCM) |
| NOT-02 | Notifications aux scanners | Alertes d'événement, changements d'affectation |
| NOT-03 | Notifications admin | Alertes de sécurité, activité suspecte |
| NOT-04 | Marquer comme lu | Le destinataire peut marquer une notification comme lue |

---

## 6. Spécifications Techniques

### 6.1 Environnement Cible

| Paramètre | Valeur |
|-----------|--------|
| Plateforme | Android uniquement |
| Version minimale | Android 7.0 (API 24) |
| Version cible | Android 15 (API 35) |
| SDK de compilation | API 36 |
| Langage | Kotlin |
| Version Kotlin | JVM Target 11 |

### 6.2 Stack Technologique

#### Couche UI

| Technologie | Usage |
|-------------|-------|
| Jetpack Compose | Framework UI déclaratif |
| Material Design 3 | Système de design |
| Navigation Compose | Routing entre écrans |
| Coil | Chargement d'images avec cache |
| Material Icons Extended | Bibliothèque d'icônes |
| Google Fonts (Nunito) | Typographie brand |

#### Couche Données

| Technologie | Usage |
|-------------|-------|
| Room (SQLite) | Base de données locale |
| Firebase Firestore | Base de données cloud (principale) |
| Firebase Realtime Database | Synchronisation temps réel |
| Firebase Storage | Stockage de fichiers/images |
| DataStore | Stockage des préférences utilisateur |

#### Couche Métier / Architecture

| Technologie | Usage |
|-------------|-------|
| ViewModel + StateFlow | Gestion d'état UI (MVVM) |
| Kotlin Coroutines & Flow | Programmation asynchrone |
| Hilt | Injection de dépendances |
| WorkManager | Synchronisation en arrière-plan |

#### Authentification et Sécurité

| Technologie | Usage |
|-------------|-------|
| Firebase Authentication | Auth email/mot de passe |
| Biometric API | Empreinte / Face ID |
| Android Keystore | Stockage sécurisé des clés |
| HMAC-SHA256 | Signature cryptographique des QR codes |

#### Caméra et QR Code

| Technologie | Usage |
|-------------|-------|
| CameraX | Accès caméra Android |
| ML Kit Barcode Scanning | Détection et lecture de QR codes |
| ZXing (Core) | Génération de QR codes |

#### Réseau

| Technologie | Usage |
|-------------|-------|
| Retrofit 2 | Client REST (intégration API paiement futur) |
| OkHttp 4 | Client HTTP sous-jacent |
| Gson | Sérialisation/désérialisation JSON |
| Kotlinx Serialization | Sérialisation Kotlin native |

#### Monitoring et Qualité

| Technologie | Usage |
|-------------|-------|
| Firebase Crashlytics | Rapport de crashs en temps réel |
| Firebase Analytics | Suivi d'usage |

### 6.3 Permissions Android Requises

| Permission | Justification |
|------------|---------------|
| `CAMERA` | Scan des QR codes |
| `USE_BIOMETRIC` | Authentification biométrique |
| `USE_FINGERPRINT` | Authentification empreinte (legacy) |
| `INTERNET` | Synchronisation Firebase |
| `ACCESS_NETWORK_STATE` | Détection mode hors-ligne |
| `POST_NOTIFICATIONS` | Notifications push FCM |
| `RECEIVE_BOOT_COMPLETED` | Redémarrage des tâches de sync |
| `VIBRATE` | Feedback vibration lors du scan |
| `WRITE_EXTERNAL_STORAGE` | Export (Android ≤ 9) |
| `READ_EXTERNAL_STORAGE` | Lecture médias (Android ≤ 12) |
| `READ_MEDIA_IMAGES` | Accès images (Android 13+) |

---

## 7. Architecture Logicielle

### 7.1 Pattern Architectural

L'application suit la **Clean Architecture** avec une séparation stricte en 3 couches :

```
┌─────────────────────────────────────────┐
│          COUCHE PRÉSENTATION (UI)        │
│  Jetpack Compose · ViewModel · StateFlow │
└────────────────────┬────────────────────┘
                     │
┌────────────────────▼────────────────────┐
│           COUCHE DOMAINE                 │
│  Use Cases · Domain Models · Interfaces  │
│  Anti-fraude · QR Code · Audit           │
└────────────────────┬────────────────────┘
                     │
┌────────────────────▼────────────────────┐
│           COUCHE DONNÉES                 │
│  Room · Firebase · Repositories · Sync  │
└─────────────────────────────────────────┘
```

### 7.2 Structure des Packages

```
com.example.eventpay/
├── data/
│   ├── auth/               # Implémentation authentification
│   ├── firebase/           # Services Firebase (Firestore, Storage)
│   ├── local/              # Room Database, DAO, Entities, Converters
│   ├── mapper/             # Mappers data ↔ domain
│   ├── model/              # Modèles de données (Event, Ticket, User, Transaction)
│   ├── repository/         # Implémentations des repositories
│   └── sync/               # OfflineSyncManager, RealTimeSyncService
├── domain/
│   ├── antifraud/          # FraudDetectionEngine, AuditLogManager,
│   │                       # SecurityRulesEngine, ScreenshotProtectionManager
│   ├── auth/               # Modèles et interfaces auth
│   ├── model/              # Modèles domaine (Event, Ticket, User, CheckInRecord…)
│   ├── qrcode/             # QRCodeGenerator, QRCodeValidator, QRCodeModels
│   ├── repository/         # Interfaces des repositories
│   ├── sync/               # ConflictResolutionEngine, MultiDeviceSyncManager
│   └── usecase/            # Use Cases par domaine métier
│       ├── antifraud/
│       ├── cashier/
│       ├── event/
│       ├── offline/
│       ├── qrcode/
│       ├── ticket/
│       └── user/
├── di/                     # Modules Hilt (AppContainer, RepositoryModule)
├── notification/           # FCM Service, NotificationHelper, NotificationRepository
├── security/               # BiometricAuthManager, QRCryptoManager
├── ui/
│   ├── admin/              # AdminViewModel, CreateScannerDialog
│   ├── auth/               # AuthViewModel
│   ├── cashier/            # CashierViewModel
│   ├── components/         # Composants UI réutilisables (ModernUI)
│   ├── dashboard/          # AnalyticsViewModel, DashboardViewModel
│   ├── event/              # EventViewModel
│   ├── navigation/         # NavGraph, NavRoutes
│   ├── qrcode/             # QRCodeViewModel
│   ├── scanner/            # QRScannerScreen, QRScannerViewModel, ScannerViewModel
│   ├── screens/            # Tous les écrans de l'application
│   └── theme/              # Color, Theme, Type (Material 3)
├── util/                   # NetworkUtils
├── EventPayApplication.kt  # Point d'entrée Hilt
└── MainActivity.kt         # Activité principale
```

### 7.3 Pattern MVVM

Chaque écran suit le pattern **MVVM** :

```
Screen (Composable) ──observe──► ViewModel (StateFlow)
         │                              │
         └──events──►           Use Cases / Repositories
```

---

## 8. Base de Données

### 8.1 Base de Données Locale — Room (SQLite)

#### Table `events`
| Colonne | Type | Description |
|---------|------|-------------|
| id | String (PK) | Identifiant unique |
| name | String | Nom de l'événement |
| description | String | Description détaillée |
| date | Long | Date de début (timestamp) |
| endDate | Long | Date de fin (timestamp) |
| location | String | Lieu de l'événement |
| totalTickets | Int | Capacité totale |
| reservedTickets | Int | Billets réservés |
| checkedInCount | Int | Entrées effectuées |
| status | Enum | DRAFT, PUBLISHED, ONGOING, COMPLETED, CANCELLED |
| ticketPrice | Double | Prix de base |
| organizerId | String | UID de l'admin créateur |
| assignedScanners | List\<String\> | UIDs des scanners affectés |
| isPublished | Boolean | Visibilité publique |

#### Table `tickets`
| Colonne | Type | Description |
|---------|------|-------------|
| id | String (PK) | Identifiant unique |
| eventId | String (FK) | Référence à l'événement |
| userId | String (FK) | Référence à l'utilisateur |
| qrCode | String | Contenu du QR code chiffré |
| ticketType | Enum | STANDARD, VIP, EARLY_BIRD |
| status | Enum | ACTIVE, USED, CANCELLED, EXPIRED |
| checkedInAt | Long? | Timestamp de validation |
| checkedInBy | String? | UID du scanner |
| price | Double | Prix payé |

#### Table `users`
| Colonne | Type | Description |
|---------|------|-------------|
| id | String (PK) | UID Firebase |
| email | String | Adresse email (unique) |
| fullName | String | Nom complet |
| role | Enum | ADMIN, SCANNER |
| assignedEvents | List\<String\> | Événements assignés |
| isActive | Boolean | Compte actif/inactif |
| createdAt | Long | Date de création |
| lastLoginAt | Long | Dernière connexion |

#### Table `transactions`
| Colonne | Type | Description |
|---------|------|-------------|
| id | String (PK) | Identifiant unique |
| eventId | String (FK) | Événement concerné |
| ticketId | String (FK) | Billet associé |
| userId | String | Client |
| amount | Double | Montant de la transaction |
| type | Enum | SALE, REFUND, CHECK_IN |
| createdAt | Long | Date/heure |

#### Table `sync_queue`
| Colonne | Type | Description |
|---------|------|-------------|
| id | String (PK) | Identifiant de l'opération |
| operationType | Enum | CREATE, UPDATE, DELETE |
| collectionName | String | Collection Firestore cible |
| documentId | String | Document concerné |
| payload | String (JSON) | Données à synchroniser |
| retryCount | Int | Nombre de tentatives |
| createdAt | Long | Timestamp de création |

### 8.2 Base de Données Cloud — Firestore

#### Collections et règles d'accès

| Collection | Lecture | Écriture |
|------------|---------|----------|
| `users` | Owner ou Admin | Owner (champs limités) ou Admin |
| `events` | Admin / Scanner assigné / Public si publié | Admin uniquement |
| `tickets` | Admin / Owner / Scanner assigné | Admin (création) / Scanner (check-in uniquement) |
| `checkIns` | Admin / Scanner auteur | Authentifié + assigné (append-only) |
| `scannerAssignments` | Admin / Scanner concerné | Admin |
| `auditLogs` | Admin | Authentifié (append-only, jamais modifiable) |
| `eventAnalytics` | Admin | Admin |
| `notifications` | Admin / Destinataire | Admin (création) / Destinataire (marquage lu) |

### 8.3 Base de Données Temps Réel — Firebase Realtime Database

Utilisée pour la synchronisation instantanée entre appareils (présence en ligne, état de scan en cours).

---

## 9. Sécurité

### 9.1 Authentification

- **Firebase Authentication** : gestion des tokens JWT, rotation automatique
- **Authentification biométrique** : `BiometricPrompt` Android pour les opérations sensibles
- **Auto-déconnexion** : session invalidée après période d'inactivité configurable
- **Validation email** : format vérifié côté Firestore Rules (regex)

### 9.2 Sécurité des QR Codes

| Mécanisme | Description |
|-----------|-------------|
| Signature HMAC-SHA256 | Chaque QR code contient une signature cryptographique vérifiable sans serveur |
| Validation temporelle | QR codes liés à une fenêtre de validité temporelle |
| Usage unique | Une fois scanné, le billet est marqué USED — tout re-scan est rejeté |
| Android Keystore | Les clés de chiffrement sont stockées dans le Keystore sécurisé du device |

### 9.3 Anti-Fraude

| Mécanisme | Description |
|-----------|-------------|
| `FraudDetectionEngine` | Détection des tentatives de réutilisation de billets |
| `SecurityRulesEngine` | Règles métier de sécurité (vitesse de scan anormale, etc.) |
| `ScreenshotProtectionManager` | Détection et blocage des captures d'écran des QR codes |
| `AuditLogManager` | Journal immuable de toutes les opérations critiques |
| Device fingerprinting | Liaison de l'appareil aux opérations de scan |

### 9.4 Sécurité Réseau

- Toutes les communications transitent par **HTTPS/TLS** (Firebase SDK natif)
- **Règles Firestore** côté serveur : validation complète indépendamment du client
- Principe du moindre privilège : les scanners ne voient que leurs événements assignés
- **Parameterized queries** Room : protection contre l'injection SQL

### 9.5 Sécurité des Données Locales

- Base de données Room chiffrée
- DataStore Preferences pour les données de session
- Aucun secret en clair dans le code (pas de clés API hardcodées)

### 9.6 Règles Firestore (résumé des règles déployées)

```
- users     : Lecture/écriture owner + Admin ; suppression Admin ≠ soi-même
- events    : CRUD Admin ; update check-in count par Scanner assigné uniquement
- tickets   : CRUD Admin ; update status/checkedInAt par Scanner assigné uniquement
- checkIns  : Append-only, jamais modifiable/supprimable
- auditLogs : Append-only, jamais modifiable/supprimable
```

---

## 10. Mode Hors-ligne et Synchronisation

### 10.1 Architecture Offline-First

L'application fonctionne **100% hors-ligne** pour les opérations de scan. Les données nécessaires sont préchargées dans Room avant l'événement.

```
Requête utilisateur
      │
      ▼
Room (cache local) ──► Réponse immédiate à l'UI
      │
      ▼
Si en ligne : Firestore synchronisé en arrière-plan
Si hors-ligne : Opération mise en queue (sync_queue)
```

### 10.2 Mécanismes de Synchronisation

| Mécanisme | Description |
|-----------|-------------|
| `OfflineSyncManager` | Gestion de la file de synchronisation locale |
| `RealTimeSyncService` | Écoute les changements Firestore en temps réel |
| `MultiDeviceSyncManager` | Coordination entre plusieurs appareils simultanés |
| `ConflictResolutionEngine` | Résolution intelligente des conflits (last-write-wins + règles métier) |
| `SyncWorker` (WorkManager) | Synchronisation périodique en arrière-plan |

### 10.3 Détection de Connectivité

- `NetworkUtils` surveille en continu l'état réseau
- L'UI indique clairement à l'utilisateur le mode de fonctionnement (en ligne / hors-ligne)
- À la reconnexion, la file de sync est drainée automatiquement

### 10.4 Résolution de Conflits

Cas de conflit possible : deux agents scannent le même billet simultanément hors-ligne.

**Règle appliquée :** Le premier check-in synchronisé avec Firestore prévaut. Le second est rejeté avec le statut `ALREADY_SCANNED`.

---

## 11. Interface Utilisateur et Ergonomie

### 11.1 Design System

- **Framework UI** : Jetpack Compose (100% déclaratif)
- **Design System** : Material Design 3
- **Typographie** : Police Nunito (Google Fonts)
- **Palette de couleurs** :
  - Primary : Bleu Material (personnalisable)
  - Error : Rouge (#B00020)
  - Success : Vert
  - Surface : Blanc / fond sombre

### 11.2 Navigation

#### Flux Admin

```
Splash
  └─► Login
        └─► Home (Admin)
              ├─► Gestion des Événements
              │     ├─► Créer un Événement
              │     ├─► Modifier un Événement
              │     └─► Voir les Participants
              │           └─► Détail d'un Billet
              ├─► Gestion des Utilisateurs
              │     └─► Créer un Scanner
              ├─► Dashboard Analytique
              ├─► Écran Caisse
              └─► Paramètres
```

#### Flux Scanner

```
Splash
  └─► Login
        └─► Home (Scanner)
              ├─► Sélection d'Événement
              ├─► Scanner QR
              │     └─► Résultat de Scan
              ├─► Historique des Entrées
              └─► Paramètres
```

### 11.3 Écrans Principaux

| Écran | Rôle | Description |
|-------|------|-------------|
| `SplashScreen` | Tous | Chargement initial, détection rôle |
| `LoginScreen` | Tous | Authentification |
| `RegisterScreen` | Admin | Création de compte |
| `HomeScreen` | Tous | Hub de navigation selon rôle |
| `DashboardScreen` | Admin | Vue synthétique événements |
| `CreateEventScreen` | Admin | Formulaire création/édition d'événement |
| `EventDetailScreen` | Admin | Détail + participants d'un événement |
| `TicketDetailScreen` | Admin/Scanner | Détail d'un billet |
| `QRScannerScreen` | Scanner | Interface de scan caméra |
| `AnalyticsDashboardScreen` | Admin | Statistiques et graphiques |
| `CashierScreen` | Cashier | Interface caisse |

### 11.4 Accessibilité

- Conformité WCAG 2.1 AA (contrastes, tailles de texte)
- Support RTL (`android:supportsRtl="true"`)
- Descriptions de contenu pour lecteurs d'écran

### 11.5 Responsive Design

- Adaptation aux différentes tailles d'écran via `Material3 WindowSizeClass`
- Support tablettes en lecture (scan plus confortable)

---

## 12. Notifications Push

### 12.1 Infrastructure

- **Firebase Cloud Messaging (FCM)** via `EventPayMessagingService`
- Service déclaré dans `AndroidManifest.xml` avec filtre `MESSAGING_EVENT`

### 12.2 Types de Notifications

| Type | Destinataire | Déclencheur |
|------|-------------|-------------|
| Alerte de sécurité | Admin | Tentative de fraude détectée |
| Changement d'affectation | Scanner | Affectation à un nouvel événement |
| Événement imminent | Scanner | Rappel avant début d'événement |
| Capacité atteinte | Admin | Événement complet |
| Synchronisation requise | Scanner | Données locales obsolètes |

### 12.3 Gestion des Notifications

- Les notifications sont persistées dans la collection Firestore `notifications`
- Le destinataire peut les marquer comme lues (`read: true`)
- Seul l'Admin peut créer des notifications ; les destinataires ne peuvent que les lire/marquer

---

## 13. Performance et Contraintes

### 13.1 Objectifs de Performance

| Indicateur | Cible |
|------------|-------|
| Temps de démarrage (cold start) | < 2 secondes |
| Temps de scan QR | < 1 seconde |
| Taille de l'APK release | < 20 MB |
| Taux de crash | < 1% |
| Disponibilité hors-ligne | 100% des fonctions de scan |
| Fluidité UI | 60 fps |

### 13.2 Optimisations Implémentées

- **ProGuard** : minification + obfuscation en release (`isMinifyEnabled = true`)
- **Resource Shrinking** : suppression des ressources inutilisées (`isShrinkResources = true`)
- **Lazy Loading** : listes paginées via Compose `LazyColumn`
- **Coil** : chargement d'images avec cache mémoire et disque
- **Indexation Room** : requêtes SQL optimisées sur les colonnes fréquemment filtrées
- **Guava ListenableFuture** : interopérabilité CameraX sans blocage du thread principal

### 13.3 Contraintes Matérielles

- Caméra non obligatoire (`android.hardware.camera` : `required="false"`)
- Fonctionne sur appareils d'entrée de gamme (minSdk 24 = Android 7.0)
- Biométrie optionnelle (fallback sur PIN/mot de passe)

---

## 14. Tests et Qualité

### 14.1 Stratégie de Tests

| Niveau | Framework | Périmètre |
|--------|-----------|-----------|
| Tests unitaires | JUnit 4 | Use cases, moteur anti-fraude, logique QR code |
| Tests d'intégration | JUnit 4 + Room | Repositories, DAO, sync |
| Tests UI | Compose Testing + Espresso | Flux utilisateurs complets |
| Tests End-to-End | Manuel sur device réel | Scénarios scan, offline, reconnexion |

### 14.2 Objectifs de Couverture

| Domaine | Couverture cible |
|---------|-----------------|
| Use Cases (domaine) | 80%+ |
| Moteur anti-fraude | 100% |
| Validation QR code | 100% |
| Sync / Conflict Resolution | 70%+ |
| Couverture globale | 70%+ |

### 14.3 Commandes de Test

```bash
# Tests unitaires
./gradlew test

# Tests instrumentés (sur device/émulateur)
./gradlew connectedAndroidTest

# Build de vérification
./gradlew assembleRelease
```

### 14.4 Qualité du Code

- **Kotlin Style Guide** officiel JetBrains
- **Android Lint** : zéro warning bloquant en release
- **Principe SOLID** appliqué dans la couche domaine
- Injection de dépendances systématique via **Hilt**

---

## 15. Déploiement et Distribution

### 15.1 Variantes de Build

| Variante | Minification | Debug logs | Usage |
|----------|-------------|------------|-------|
| `debug` | Non | Activés | Développement local |
| `release` | Oui (ProGuard) | Désactivés | Production |

### 15.2 Prérequis Firebase

Avant toute compilation, les éléments suivants doivent être configurés :

1. Projet Firebase créé sur [console.firebase.google.com](https://console.firebase.google.com)
2. Application Android ajoutée au projet Firebase
3. Fichier `google-services.json` placé dans `app/`
4. **Firebase Authentication** activé (Email/Mot de passe)
5. **Cloud Firestore** créé et initialisé
6. **Realtime Database** activée
7. **Cloud Messaging** configuré
8. **Cloud Storage** activé
9. Règles Firestore et Storage déployées :
   ```bash
   cd docs/firebase
   firebase deploy --only firestore:rules
   firebase deploy --only storage
   ```
10. Index Firestore déployés :
    ```bash
    firebase deploy --only firestore:indexes
    ```

### 15.3 Canaux de Distribution

| Canal | Audience | Usage |
|-------|----------|-------|
| Google Play Store | Grand public | Distribution principale |
| Firebase App Distribution | Équipe QA | Tests internes |
| Play Store (beta track) | Testeurs externes | Beta testing |
| APK direct | Entreprises | Déploiement interne sans Play Store |

### 15.4 Processus de Release

1. Gel du code (code freeze) + campagne de tests
2. Mise à jour du numéro de version (`versionCode` / `versionName`)
3. Build de l'APK/AAB signé en release
4. Upload sur Google Play Console
5. Déploiement progressif : 10% → 50% → 100%
6. Surveillance Crashlytics pendant le déploiement

---

## 16. Maintenance et Support

### 16.1 Monitoring en Production

| Outil | Métrique surveillée |
|-------|-------------------|
| Firebase Crashlytics | Taux de crash, stack traces |
| Firebase Analytics | Sessions, événements, rétention |
| Firebase Performance | Temps réseau, rendu d'écrans |
| Firestore Rules Logs | Tentatives d'accès non autorisées |

### 16.2 Politique de Mises à Jour

| Type | Fréquence | Déploiement |
|------|-----------|-------------|
| Correctifs de sécurité | Immédiat dès détection | Hotfix urgence |
| Correctifs de bugs | Hebdomadaire si critique | Release courante |
| Évolutions fonctionnelles | Mensuelle | Versioning sémantique |
| Mise à jour OS Android | Trimestrielle | Adaptation targetSdk |

### 16.3 Canaux de Support

- **Email** : support@eventpay.com
- **Issue Tracker** : GitHub Issues
- **Documentation in-app** : Aide contextuelle
- **FAQ** : Questions fréquentes intégrées

---

## 17. Évolutions Futures

### 17.1 Court Terme (3–6 mois)

- [ ] **Passerelle de paiement** : intégration Stripe ou PayPal pour vente en ligne
- [ ] **Email de billets** : envoi automatique avec PDF en pièce jointe
- [ ] **Rapports avancés** : export PDF/Excel des statistiques
- [ ] **Multi-langue (i18n)** : internationalisation (FR, EN, AR, ES...)
- [ ] **Mode sombre** : thème dark complet Material 3

### 17.2 Moyen Terme (6–12 mois)

- [ ] **Application attendees** : app mobile pour les participants (achat + portefeuille de billets)
- [ ] **Intégration réseaux sociaux** : promotion d'événements sur Facebook, Instagram
- [ ] **Templates d'événements** : créer rapidement depuis un modèle pré-configuré
- [ ] **Import CSV/Excel** : import en masse de listes de participants
- [ ] **White-labeling** : personnalisation de la marque pour chaque organisateur

### 17.3 Long Terme (12+ mois)

- [ ] **Dashboard Web** : interface desktop pour les admins
- [ ] **API publique** : intégration avec des systèmes tiers (CRM, ERP)
- [ ] **Machine Learning** : détection avancée de fraude par comportement
- [ ] **Blockchain** : vérification décentralisée des billets
- [ ] **Événements virtuels** : support du streaming en ligne

---

## 18. Risques et Mitigations

### 18.1 Risques Techniques

| Risque | Probabilité | Impact | Mitigation |
|--------|-------------|--------|------------|
| Indisponibilité Firebase | Faible | Élevé | Architecture offline-first, Room comme fallback |
| Incompatibilité device Android | Moyenne | Moyen | Tests sur devices variés, minSdk conservateur |
| Problèmes de performance sur entrée de gamme | Moyenne | Moyen | ProGuard, lazy loading, profiling |
| Faille de sécurité QR code | Faible | Très élevé | Audits réguliers, HMAC-SHA256, one-time use |
| Perte de données lors de sync | Faible | Élevé | ConflictResolutionEngine, queue persistante |

### 18.2 Risques Métier

| Risque | Probabilité | Impact | Mitigation |
|--------|-------------|--------|------------|
| Adoption utilisateur insuffisante | Moyenne | Élevé | UX intuitive, formation, documentation |
| Concurrence marché | Moyenne | Moyen | Différenciation par offline-first et sécurité |
| Dépassement de coûts Firebase | Faible | Faible | Monitoring quotas, alertes de facturation |
| Scalabilité sur grands événements | Faible | Moyen | Firebase auto-scaling, pagination côté client |

---

## 19. Glossaire

| Terme | Définition |
|-------|------------|
| **QR Code** | Quick Response code — code 2D encodant les informations du billet |
| **Check-in** | Processus de validation de l'entrée d'un participant |
| **Scanner** | Rôle utilisateur chargé de la validation des billets à l'entrée |
| **Admin** | Rôle utilisateur avec accès complet à toutes les fonctionnalités |
| **Offline-first** | Architecture où l'application fonctionne sans connexion, avec sync différée |
| **HMAC-SHA256** | Algorithme de signature cryptographique utilisé pour sécuriser les QR codes |
| **Firestore** | Base de données NoSQL cloud de Firebase (documents/collections) |
| **Room** | ORM Android pour base de données SQLite locale |
| **ViewModel** | Composant MVVM gérant l'état UI survivant aux rotations d'écran |
| **StateFlow** | Flow Kotlin émettant le dernier état connu, utilisé pour l'UI réactive |
| **Hilt** | Framework d'injection de dépendances pour Android (basé sur Dagger) |
| **WorkManager** | API Android pour tâches d'arrière-plan garanties même après redémarrage |
| **FCM** | Firebase Cloud Messaging — service de notifications push |
| **ML Kit** | Bibliothèque Google de machine learning pour Android (scan de codes-barres) |
| **Conflict Resolution** | Algorithme déterminant quelle version d'une donnée prime lors d'un conflit de sync |
| **Audit Log** | Journal immuable de toutes les opérations critiques (append-only) |
| **ProGuard** | Outil de minification, optimisation et obfuscation du bytecode Java/Kotlin |
| **KSP** | Kotlin Symbol Processing — processeur d'annotations utilisé par Room et Hilt |

---

**Fin du Cahier de Charges**

---

*Document préparé sur la base du code source du projet EventPay*  
*Version : 1.0 — Mars 2026*
