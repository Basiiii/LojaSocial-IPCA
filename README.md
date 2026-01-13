# Loja Social IPCA – Android Application

A comprehensive mobile application for managing IPCA's Social Store operations, serving beneficiaries, employees, and the community with modern digital solutions.

## Overview

Loja Social IPCA is a native Android application built with Kotlin and Jetpack Compose that streamlines social aid distribution, inventory management, and beneficiary services. The app features role-based access with dedicated portals for different user types.

## Key Features

### 🏢 Employee Portal
- **Inventory Management**: Barcode scanning, stock tracking, and expiration alerts
- **Beneficiary Management**: Application processing and profile administration
- **Campaign Management**: Create and manage donation campaigns
- **Analytics**: Activity logs and audit trails
- **Calendar Integration**: Schedule pickups and deliveries

### 👥 Beneficiary Portal  
- **Application System**: Apply for social aid benefits with document upload
- **Stock Viewing**: Browse available products and resources
- **Request Management**: Submit and track pickup requests
- **Profile Management**: Update personal information and preferences

### 🌐 Community Website (Separate Project)
- **Public Information**: View available stock and donation opportunities
- **Campaign Updates**: Stay informed about ongoing initiatives  
- **Support Resources**: Access FAQs and contact information
- *Note: Community features are available via our Next.js website, not this mobile app*

## Technology Stack

- **Frontend**: Android (Kotlin), Jetpack Compose, Material Design 3
- **Architecture**: MVVM with Repository Pattern, Hilt Dependency Injection
- **Backend**: Node.js with Express, Firebase Authentication & Firestore
- **Database**: Firebase Firestore, Realtime Database
- **APIs**: ML Kit (Barcode Scanning), CameraX, Retrofit
- **Notifications**: Firebase Cloud Messaging (FCM)

## Installation

### Prerequisites
- Android Studio Hedgehog | 2023.1.1 or later
- Android SDK API 24+ (Android 7.0)
- Java 17

### Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/Basiiii/LojaSocial-IPCA.git
   cd LojaSocial-IPCA
   ```

2. **Configure Firebase**
   - Create a Firebase project at [Firebase Console](https://console.firebase.google.com)
   - Download `google-services.json` and place it in `app/app/`
   - Enable Authentication, Firestore, and Cloud Messaging

3. **Backend Configuration**
   ```bash
   cd backend
   npm install
   cp .env.example .env
   # Configure your environment variables in .env
   npm start
   ```

4. **Android App Configuration**
   - Open the project in Android Studio
   - Sync Gradle and run the application

## Project Structure

```
LojaSocial-IPCA/
├── app/                    # Android application
│   └── app/
│       ├── src/main/java/com/lojasocial/app/
│       │   ├── navigation/     # Navigation and routing
│       │   ├── repository/     # Data layer (repositories)
│       │   ├── ui/            # UI components and ViewModels
│       │   └── utils/         # Utilities and helpers
│       └── build.gradle.kts   # App dependencies
├── backend/                # Node.js API server
├── docs/                   # Documentation
└── website/               # Web presence (Next.js)
```
