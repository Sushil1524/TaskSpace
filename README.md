# TaskSpace

**TaskSpace** is a comprehensive, open-source productivity app built natively for Android. It combines task management, habit tracking, daily journaling, and long-term goal setting into one clean, premium interface. Whether you want to reflect on your day, conquer daily errands, or maintain long-term streaks, TaskSpace has you covered without any clutter.

## Features

- **✅ Smart To-Do List:** Organize your tasks by category (Work, Personal, Study) and priority. Add rich notes with clickable URLs.
- **🔁 Weekly Planning & Deadlines:** See a 7-day overview with the Weekly Tasks screen.
- **🔄 Habit Tracker:** Dedicated Habit tracking with current and max streak calculations to keep you motivated.
- **🎯 Long-Term Goals:** Set your sights on larger aspirations with "Complete by End of Month" deadlines and dedicated views for active and completed targets.
- **📝 Daily Journal:** Chronicle your experiences right where you manage your tasks.
- **📈 Advanced Analytics & History:** Visually gorgeous charts tracking your activity, alongside an immutable history logs to see previous completions.
- **📱 Clean, Premium UI:** Utilizing Android's latest Material Design 3 guidelines (`Jetpack Compose`) with dynamic colors, animations, and rounded geometric patterns. 

## Technology Stack

- **UI:** Jetpack Compose, Material Design 3 (M3)
- **Architecture:** MVVM (Model-View-ViewModel) + Clean Architecture patterns
- **Data Persistence:** Room Database for fast, secure local storage
- **Asynchronous Operations:** Kotlin Coroutines & StateFlow
- **Automation:** GitHub Actions for continuous integration and automated release APK generation.

## Build Your Own APK / CI

TaskSpace runs a GitHub Action called **Android Release** that automatically generates an APK whenever you tag a new version (e.g. `v1.0.0`) and pushes it. 

By default, the auto-built APKs are unsigned builds. If you wish to implement automated signed Play Store releases:
1. Follow standard Android procedures to generate an `.jks` or `.keystore` file (`Build > Generate Signed Bundle / APK...`).
2. In your GitHub repository, go to `Settings` -> `Secrets and variables` -> `Actions`.
3. Add your base64 encoded keystore, KeyAlias, KeyPassword, and StorePassword as Secrets.
4. Update the `.github/workflows/release.yml` with a signing step (e.g. `uses: r0adkll/sign-android-release@v1`).
