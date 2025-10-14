
# Streakly - Habit Tracker App 📱

Video link of app demonstration: https://youtu.be/eMeFkdI_a40

<div align="center">

![Streakly Banner](https://via.placeholder.com/800x200/4F46E5/FFFFFF?text=Streakly+-+Build+Better+Habits+Every+Day)

[![Android CI](https://github.com/your-username/streakly/actions/workflows/android-ci.yml/badge.svg)](https://github.com/your-username/streakly/actions/workflows/android-ci.yml)
[![Backend CI](https://github.com/your-username/streakly/actions/workflows/backend-ci.yml/badge.svg)](https://github.com/your-username/streakly/actions/workflows/backend-ci.yml)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

*A modern, intuitive habit tracking application designed to help you build consistent routines and maintain streaks*

</div>

## 📋 Table of Contents
- [App Overview](#-app-overview)
- [Key Features](#-key-features)
- [Technical Architecture](#-technical-architecture)
- [Design Considerations](#-design-considerations)
- [GitHub Utilization](#-github-utilization)
- [GitHub Actions CI/CD](#-github-actions-cicd)
- [Installation & Setup](#-installation--setup)
- [Development](#-development)
- [Contributing](#-contributing)
- [License](#-license)

## 🎯 App Overview

Streakly is a comprehensive habit tracking application built with modern Android development practices. The app empowers users to establish and maintain positive habits through intuitive tracking, reminders, and progress analytics.

**Core Purpose**: To provide a seamless, motivating experience for users to build lasting habits through consistent daily tracking and visual progress indicators.

### Problem Statement
Many people struggle with habit consistency due to:
- Lack of accountability
- Poor progress visibility
- No reminder systems
- Complicated tracking methods

### Solution
Streakly addresses these challenges with:
- Simple, intuitive habit creation
- Visual streak counters
- Smart reminder system
- Progress analytics
- Cross-platform accessibility

## ✨ Key Features

### 🔐 User Authentication
- Secure registration and login system
- Session management
- User profile management

### 📝 Habit Management
- Create custom habits with categories
- Set frequency (Daily, Weekly, Custom days)
- Add notes and reminders
- Flexible scheduling options

### 🔄 Streak Tracking
- Automatic streak calculation
- Visual progress indicators
- Completion history
- Streak milestones

### ⏰ Smart Reminders
- Customizable notification times
- Missed habit alerts
- Progress encouragement

### 📊 Analytics & Insights
- Completion statistics
- Habit success rates
- Weekly/Monthly overviews
- Progress visualization

### 🌐 Cloud Sync
- Real-time data synchronization
- Multi-device support
- Offline capability with auto-sync

## 🏗️ Technical Architecture

### Frontend (Android)
```
📱 Android App (Kotlin)
├── UI Layer (Compose/Views)
├── Domain Layer (Use Cases)
├── Data Layer (Repository Pattern)
└── Framework Layer (Android SDK)
```

### Backend (Node.js)
```
🖥️ Express.js Server
├── RESTful API
├── SQLite Database
├── Authentication Middleware
└── Business Logic
```

### Technology Stack
| Component | Technology | Purpose |
|-----------|------------|---------|
| **Frontend** | Kotlin, Jetpack Compose | Native Android UI |
| **Backend** | Node.js, Express.js | API Server |
| **Database** | SQLite | Local & Server Storage |
| **Networking** | Retrofit/OkHttp | API Communication |
| **Authentication** | JWT, bcrypt | Secure Access |
| **CI/CD** | GitHub Actions | Automated Pipelines |

## 🎨 Design Considerations

### User Experience (UX)
- **Minimalist Design**: Clean interface focused on habits
- **Progressive Disclosure**: Advanced features revealed when needed
- **Consistent Navigation**: Predictable user flows
- **Accessibility**: WCAG 2.1 compliant design

### User Interface (UI)
- **Material Design 3**: Modern Android design language
- **Dark/Light Theme**: User preference support
- **Custom Color Schemes**: Brand-consistent palette
- **Responsive Layout**: Adaptive to various screen sizes

### Performance
- **Efficient Data Loading**: Pagination and caching
- **Background Operations**: Coroutines for async tasks
- **Memory Management**: Proper lifecycle awareness
- **Battery Optimization**: Efficient background services

### Security
- **Secure Authentication**: Password hashing with bcrypt
- **Data Encryption**: Local and transmission security
- **Input Validation**: Client and server-side checks
- **Session Management**: Secure token handling

## 📁 GitHub Utilization

### Repository Structure
```
streakly/
├── 📱 app/                    # Android Application
│   ├── src/main/java/        # Kotlin Source Code
│   ├── res/                  # Android Resources
│   └── build.gradle.kts      # Build Configuration
├── 🖥️ server/                # Backend Server
│   ├── server.js            # Express Server
│   ├── package.json         # Dependencies
│   └── database/            # SQLite Files
├── 📚 docs/                  # Documentation
├── 🔧 .github/workflows/     # GitHub Actions
└── 📄 README.md             # Project Documentation
```

### Branch Strategy
- `main` - Production-ready code
- `develop` - Development integration branch
- `feature/*` - Feature development
- `hotfix/*` - Critical bug fixes
- `release/*` - Release preparation

### Commit Convention
```
feat: Add habit completion tracking
fix: Resolve crash on habit deletion
docs: Update API documentation
style: Improve code formatting
refactor: Optimize database queries
test: Add unit tests for HabitManager
```

## ⚙️ GitHub Actions CI/CD

### Automated Workflows

#### 1. Android CI Pipeline
```yaml
name: Android CI
on: [push, pull_request]
jobs:
  build:
    - Build APK & Run Tests
    - Lint Analysis
    - Security Scanning
```

#### 2. Backend CI Pipeline
```yaml
name: Backend CI  
on: [push, pull_request]
jobs:
  test:
    - Node.js Testing
    - API Validation
    - Security Audit
```

#### 3. Release Automation
```yaml
name: Release
on: tag
jobs:
  release:
    - Build Signed APK
    - Create GitHub Release
    - Upload Assets
```

### Quality Gates
- ✅ Code Compilation
- ✅ Unit Tests Passing
- ✅ Integration Tests
- ✅ Code Quality Standards
- ✅ Security Vulnerabilities Check
- ✅ Performance Benchmarks

## 🚀 Installation & Setup

### Prerequisites
- Android Studio Arctic Fox or later
- Java JDK 17+
- Node.js 18+ (for backend)
- Git

### Backend Setup
```bash
# Clone repository
git clone https://github.com/your-username/streakly.git
cd streakly/server

# Install dependencies
npm install

# Start development server
npm start

# Server running on http://localhost:3000
```

### Android Setup
1. Open Android Studio
2. Open project from `streakly/app` directory
3. Sync project with Gradle files
4. Build and run on emulator or device

### Environment Configuration
Create `local.properties` in app directory:
```properties
# For emulator
BASE_URL=http://10.0.2.2:3000/api

# For physical device
BASE_URL=http://your-local-ip:3000/api
```

## 💻 Development

### Building from Source
```bash
# Clone the repository
git clone https://github.com/your-username/streakly.git

# Backend setup
cd server && npm install

# Android setup
cd ../app
# Open in Android Studio and sync
```

### Running Tests
```bash
# Backend tests
cd server && npm test

# Android unit tests
cd app && ./gradlew test

# Android instrumentation tests  
./gradlew connectedAndroidTest
```

### Code Style
- Kotlin: Follow official style guide
- Java: Google Java Style
- JavaScript: Standard JS with ESLint

## 🤝 Contributing

We welcome contributions! Please see our [Contributing Guidelines](CONTRIBUTING.md) for details.

### Development Process
1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Issue Reporting
- Use GitHub Issues template
- Provide detailed description
- Include steps to reproduce
- Attach relevant logs/screenshots

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- Android Jetpack Libraries
- Express.js Community
- Material Design Components
- GitHub Actions Team

---

<div align="center">

**Streakly** - *Building better habits, one day at a time* 🚀

[Report Bug](https://github.com/your-username/streakly/issues) · 
[Request Feature](https://github.com/your-username/streakly/issues) · 
[Documentation](docs/) · 
[Download](releases/)

</div>
```

## 🖼️ Suggested Images to Add

1. **App Screenshots**: 
   - Main habit list screen
   - Habit creation form
   - Analytics dashboard
   - Settings screen

2. **Architecture Diagrams**:
   - System architecture
   - Database schema
   - CI/CD pipeline flow

3. **Badges**: 
   - Build status
   - Code coverage
   - License
   - Download counts

## 📝 Customization Instructions

1. **Replace placeholder content** with your actual:
   - GitHub username in URLs
   - App screenshots
   - Specific feature details
   - Team acknowledgments

2. **Add actual images** by:
   - Taking screenshots of your app
   - Creating architecture diagrams
   - Adding your app icon/logo

3. **Update the badges** with your actual repository information

4. **Add specific technical details** about:
   - Your unique features
   - Technical challenges solved
   - Performance optimizations

This README provides a comprehensive overview that meets all the requirements from your screenshot while showcasing your app's purpose, design considerations, and GitHub utilization effectively.
