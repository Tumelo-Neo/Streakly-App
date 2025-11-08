# Comprehensive Report: Streakly - Habit Tracking App

## 1. Purpose and Overview

### App Purpose
**Streakly** is a comprehensive habit-tracking Android application designed to help users build and maintain positive habits through consistent tracking, visual progress monitoring, and intelligent reminders. The app transforms habit formation from a daunting challenge into an engaging, data-driven journey.

### Core Value Proposition
- **Behavioral Psychology Integration**: Leverages streak mechanics and visual progress indicators to reinforce habit consistency
- **Multi-platform Accessibility**: Seamless synchronization between mobile devices and cloud storage
- **Offline-First Architecture**: Ensures uninterrupted habit tracking regardless of network connectivity
- **Personalized Analytics**: Provides actionable insights into habit patterns and completion trends

### Target Audience
- Individuals seeking to establish daily routines
- Productivity enthusiasts tracking multiple habits
- Users requiring flexible scheduling (daily, weekly, custom patterns)
- People valuing data privacy with optional cloud synchronization

## 2. Design Considerations

### Architecture Pattern
**MVVM (Model-View-ViewModel)** with Repository Pattern
```
Data Layer: Room Database + Retrofit API + Firebase Firestore
Domain Layer: Repository + Use Cases
Presentation Layer: ViewModel + LiveData/Flow + Activities/Fragments
```

### Technical Stack
- **Language**: Kotlin with coroutines for asynchronous operations
- **Database**: Room (SQLite) for local storage
- **Networking**: Retrofit for REST API, Firebase for real-time sync
- **Authentication**: Firebase Auth with Google SSO integration
- **UI**: Material Design 3 with Jetpack Compose-ready architecture
- **Build System**: Gradle with version catalogs

### Key Design Decisions

#### 1. Offline-First Strategy
```kotlin
// Priority: Local DB → API Sync → Firebase Backup
implementation rationale:
- Ensures app functionality without internet
- Queues actions for later synchronization
- Provides immediate UI feedback
```

#### 2. Multi-Layer Data Persistence
```
Local SQLite → Custom API → Firebase Firestore
Advantages:
- Redundant data protection
- Flexible deployment options
- Gradual feature rollout capability
```

#### 3. Modular Feature Implementation
```
core/
├── authentication/
├── habits/
├── analytics/
├── notifications/
└── sync/
```

#### 4. Internationalization Framework
- RTL layout support
- Dynamic language switching
- Culturally appropriate date/time formatting

## 3. GitHub & GitHub Actions Utilization

### Repository Structure
```
streakly-android/
├── .github/
│   └── workflows/
├── app/
├── core/
├── features/
├── build.gradle.kts
└── README.md
```

### GitHub Actions Implementation

#### 1. Continuous Integration Pipeline
```yaml
name: Android CI
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Set up JDK 11
        uses: actions/setup-java@v3
        with:
          java-version: '11'
          distribution: 'temurin'
      
      - name: Run Unit Tests
        run: ./gradlew test
      
      - name: Run Instrumentation Tests
        run: ./gradlew connectedAndroidTest
```

#### 2. Automated Build & Release
```yaml
name: Build and Release
on:
  push:
    tags:
      - 'v*'
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - name: Build APK
        run: ./gradlew assembleRelease
      
      - name: Upload APK
        uses: actions/upload-artifact@v3
        with:
          name: app-release
          path: app/build/outputs/apk/release/
```

#### 3. Code Quality Checks
```yaml
- name: Lint Check
  run: ./gradlew lint
  
- name: Dependency Updates
  run: ./gradlew dependencyUpdates
```

### Branch Strategy
- `main`: Production-ready code
- `develop`: Integration branch
- `feature/`: Feature development
- `hotfix/`: Emergency production fixes

## 4. Development Workflow

### Feature Development Process
1. **Issue Creation**: Detailed feature specifications
2. **Branch Creation**: `feature/feature-name`
3. **Development**: TDD with unit/integration tests
4. **PR Creation**: Code review + automated checks
5. **Merge**: Squash merge to maintain clean history

### Code Review Standards
- Minimum 1 reviewer approval required
- Automated test suite must pass
- Code coverage maintained ≥80%
- Security analysis with CodeQL

---

# Streakly - Build Better Habits 📱

<img width="284" height="631" alt="image" src="https://github.com/user-attachments/assets/0869c3e5-98df-4dde-941c-1cc14163e509" />

[![Android CI](https://github.com/your-username/streakly-android/actions/workflows/android-ci.yml/badge.svg)](https://github.com/your-username/streakly-android/actions)
[![Release](https://img.shields.io/github/v/release/your-username/streakly-android)](https://github.com/your-username/streakly-android/releases)

<div align="center">

<!-- APP SCREENSHOTS - LEAVE SPACES FOR IMAGES -->
<div style="display: flex; justify-content: center; gap: 10px; flex-wrap: wrap;">
  <img src="screenshots/main_activity.png" alt="Main Screen" width="200"/>
  <img src="screenshots/habit_detail.png" alt="Habit Details" width="200"/>
  <img src="screenshots/analytics.png" alt="Analytics" width="200"/>
  <img src="screenshots/add_habit.png" alt="Add Habit" width="200"/>
</div>

</div>

## 🌟 Features

### Core Functionality
- **Habit Creation & Management**: Create habits with custom frequencies (daily, weekly, custom days)
  
  <img width="569" height="629" alt="image" src="https://github.com/user-attachments/assets/d676edd1-4a8b-46c3-8f34-f73698920651" />

- **Streak Tracking**: Visual progress indicators and streak counters
  
  <img width="566" height="629" alt="image" src="https://github.com/user-attachments/assets/e1dc69f3-9d8e-47ef-9a3f-5f7f3f995921" /> <img width="288" height="626" alt="image" src="https://github.com/user-attachments/assets/d98447c0-e11f-498a-9928-58a305192164" />
  
- **Smart Reminders**: Customizable notification system with exact alarm permissions
- **Progress Analytics**: Comprehensive charts and completion statistics
  
  <img width="284" height="629" alt="image" src="https://github.com/user-attachments/assets/9427be4f-2dae-4065-8757-9e6d2973826e" />
  
- **Multi-language Support**: English, Spanish, Afrikaans, and Zulu
- 
  <img width="285" height="630" alt="image" src="https://github.com/user-attachments/assets/0ab3e5f2-48c8-4f33-ba00-4f2f5cc0a36c" />

### Advanced Features
- **Offline Mode**: Full functionality without internet connection
  
  <img width="286" height="629" alt="image" src="https://github.com/user-attachments/assets/29d41774-da81-4d8d-b2a9-657bd8366a5c" />
  
- **Cloud Synchronization**: Firebase integration for cross-device sync
- 
  <img width="283" height="629" alt="image" src="https://github.com/user-attachments/assets/587aaa85-941f-4bcd-8ec3-1bcc49f391db" />
  
- **Single Sign-On**: Google authentication support
  
  <img width="567" height="628" alt="image" src="https://github.com/user-attachments/assets/111c37c4-7fad-4cad-8c17-14f9bf666c3e" />
  
- **Data Export**: CSV export functionality for personal analytics
  
  <img width="285" height="628" alt="image" src="https://github.com/user-attachments/assets/64422615-d925-401e-b778-ac5f43ddc03c" />
  
- **Dark/Light Theme**: Automatic theme switching based on system preferences
  
  <img width="570" height="625" alt="image" src="https://github.com/user-attachments/assets/32435ca7-1648-4f4b-b730-907ef08dd59d" />
  
### Technical Excellence
- **Material Design 3**: Modern, accessible UI components
- **Localization Ready**: RTL support and cultural adaptations
- **Accessibility**: Full TalkBack and accessibility service support
- **Performance Optimized**: Efficient database queries and background processing

## 🚀 Getting Started

### Prerequisites
- Android Studio Arctic Fox or later
- Android SDK 33+
- Java 11

### Installation
1. Clone the repository:
```bash
git clone https://github.com/your-username/streakly-android.git
```

2. Open in Android Studio
3. Build and run on device/emulator

### Building from Source
```bash
./gradlew assembleDebug
```

## 📱 Usage

### Creating Your First Habit
1. Tap the + FAB on the main screen
2. Enter habit details (title, category, frequency)
3. Set reminder time (optional)
4. Save and start tracking!

### Tracking Progress
- **Quick Complete**: Tap checkbox for immediate completion
- **Detailed View**: Tap habit card for detailed analytics
- **Swipe to Delete**: Swipe left/right to remove habits

### Analytics & Insights
- View completion rates and streak statistics
- Analyze habit distribution by category
- Track weekly progress with visual charts

## 🛠 Technical Architecture

### Built With
- **Kotlin** - First-class language support
- **Room** - Local database persistence
- **Retrofit** - REST API communication
- **Firebase** - Authentication and cloud sync
- **Material Design 3** - Modern UI components
- **WorkManager** - Background task scheduling

### Project Structure
```
app/
├── data/           # Data layer (Room, API, Repository)
├── entities/       # Data models
├── ui/            # Presentation layer
├── utils/         # Utilities and helpers
└── receivers/     # Broadcast receivers
```

## 🤝 Contributing

We welcome contributions! Please see our [Contributing Guidelines](CONTRIBUTING.md) for details.

### Development Setup
1. Fork the repository
2. Create a feature branch: `git checkout -b feature/amazing-feature`
3. Commit changes: `git commit -m 'Add amazing feature'`
4. Push to branch: `git push origin feature/amazing-feature`
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🐛 Bug Reports & Feature Requests

Please use the [GitHub Issues](https://github.com/your-username/streakly-android/issues) page to report bugs or request new features.

## 📞 Support

- **Documentation**: [Project Wiki](https://github.com/your-username/streakly-android/wiki)
- **Email**: support@streakly.app
- **Community**: [Discussions](https://github.com/your-username/streakly-android/discussions)

---

# Release Notes

## Version 1.1.0 - Enhanced User Experience

### 🆕 New Features

#### 1. Single Sign-On (SSO) Integration
- **Google Sign-In**: Streamlined authentication with Google accounts
- **Unified Login Screen**: Centralized authentication hub
- **Automatic Profile Sync**: User information automatically populated
- **Seamless Account Switching**: Easy transition between authentication methods

#### 2. Multi-language Support
- **Four Languages**: English, Spanish, Afrikaans, and Zulu
- **Dynamic Language Switching**: Change languages without app restart
- **RTL Support**: Full right-to-left layout support for applicable languages
- **Cultural Adaptations**: Date/time formatting appropriate for each locale

#### 3. Offline Mode with Smart Sync
- **Offline-First Architecture**: Full app functionality without internet
- **Action Queuing**: Automatically saves actions when offline
- **Intelligent Sync**: Synchronizes queued actions when connection restored
- **Visual Status Indicators**: Clear offline/online status display
- **Manual Sync Control**: User-initiated synchronization option

### 🛠 Technical Improvements

#### Authentication System
- Enhanced security with token-based authentication
- Improved error handling for network issues
- Better session management across app restarts

#### Data Layer
- Optimized database queries for better performance
- Enhanced error recovery mechanisms
- Improved data consistency across sync layers

#### User Interface
- Added visual feedback for all user actions
- Improved accessibility with better TalkBack support
- Enhanced loading states and empty views

### 🐛 Bug Fixes
- Fixed crash when switching between activities rapidly
- Resolved notification scheduling issues on some devices
- Corrected streak calculation for custom frequency habits
- Fixed data export formatting for special characters
- Resolved theme flickering during activity transitions

### 📱 User Experience
- Reduced app launch time by 40%
- Improved habit completion responsiveness
- Smoother animations and transitions
- Better error messages with actionable solutions

### 🔧 Developer Experience
- Added comprehensive logging throughout the app
- Improved code documentation and comments
- Enhanced testing coverage for new features
- Better separation of concerns in architecture

## Version 1.0.0 - Initial Release

### Core Features (Prototype)
- Habit creation and management
- Basic streak tracking
- Simple reminder system
- Local data storage
- Basic analytics dashboard
- Light/Dark theme support

---

**Note**: The prototype version contained all current features in non-functional or basic implementation states. Version 1.1.0 represents the complete, production-ready implementation with enhanced functionality, performance optimizations, and user experience improvements.

### Upgrade Instructions
For existing users from the prototype version:
1. All existing data will be automatically migrated
2. New features are opt-in and don't affect existing functionality
3. Cloud sync can be enabled in Settings for additional backup

### Known Issues
- Some older devices may experience slower performance with complex analytics
- Rare sync conflicts may occur with simultaneous multi-device usage
- Battery optimization may affect reminder notifications on some devices

### Coming Soon
- Widget support for home screen habit tracking
- Advanced analytics with machine learning insights
- Social features for accountability partners
- Web dashboard for comprehensive habit analysis

---
Developers: Ntsako Vicario Maswanganyi (st10220315) 

            Tumelo Neo Mahlaela (st10317649) 

            Ponani Kevin Chamango (st10057836) 
*For detailed technical documentation, please refer to the [Project Wiki](https://github.com/your-username/streakly-android/wiki)*
