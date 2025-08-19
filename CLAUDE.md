# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Build the project
./gradlew build

# Build and install debug APK on connected device
./gradlew installDebug

# Run unit tests
./gradlew test

# Run instrumented tests on connected device/emulator
./gradlew connectedAndroidTest

# Clean build artifacts
./gradlew clean

# Build release APK
./gradlew assembleRelease
```

## Architecture Overview

This is an Android commerce application built with Kotlin following MVVM architecture:

- **Main Package**: `com.oax.comercioapp`
- **Architecture Pattern**: MVVM with LiveData and ViewModels
- **Navigation**: Bottom Navigation with Android Navigation Component
- **UI Structure**: Three main sections accessed via bottom navigation:
  - Home (`ui/home/`) - HomeFragment with HomeViewModel
  - Dashboard (`ui/dashboard/`) - DashboardFragment with DashboardViewModel  
  - Notifications (`ui/notifications/`) - NotificationsFragment with NotificationsViewModel

## Key Technical Details

- **Min SDK**: 29 (Android 10)
- **Target/Compile SDK**: 35 (Android 15)
- **Kotlin Version**: 1.9.24
- **View Binding**: Enabled for type-safe view access
- **Dependencies**: AndroidX, Material Design Components, Navigation Component, Lifecycle components

## Development Workflow

1. **Adding new features**: Follow MVVM pattern - create ViewModel, Fragment, and layout in appropriate package
2. **Navigation**: Use Navigation Component with actions defined in `res/navigation/mobile_navigation.xml`
3. **UI Components**: Use Material Design components and follow existing layout patterns
4. **View Binding**: Access views through generated binding classes (e.g., `FragmentHomeBinding`)
5. **Testing**: Add unit tests in `src/test/` and instrumented tests in `src/androidTest/`