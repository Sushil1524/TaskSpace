# TaskSpace

**TaskSpace** is a comprehensive, productivity app built natively for Android. It combines task management, habit tracking, daily journaling, and long-term goal setting into one clean, premium interface. Whether you want to reflect on your day, conquer daily errands, or maintain long-term streaks, TaskSpace has you covered without any clutter.


---

## Key Features

### **Smart Task & Project Management**
- **Priority System:** Categorize tasks by priority (**High**, **Medium**, **Low**) with visual color coding.
- **Smart Tag Colors & Icons:** Automatically tags `#work`, `#finance`, `#study`, `#urgent`, `#health`, `#quick-win`, and more with curated colors and Material icon badges.
- **Projects Integration:** Organize tasks into dedicated projects with due dates, progress tracking, and reminders.
- **Repeating Tasks:** Daily, Weekly, or Custom repeating schedules with automatic instance generation.

### **Habit Consistency Heatmap**
- **Contribution Grid:** GitHub-style **12-week (84-day) activity heatmap** tracking daily habit completions.
- **Dynamic Intensity:** Grid cells scale from subtle darks to vibrant emerald greens based on completion density per day.
- **Streak Counters:** Track current streaks and all-time maximum streaks per habit.

### **Daily Journal & Reflection**
- **Mood Tracking:** Record daily mood emoji chips alongside journal reflections.
- **Word Count & Search:** Live word counter and instant text search across past journal entries.
- **Task Alignment:** Journal entries naturally align with task completion dates for holistic daily reviews.

### **Long-Term Goal Tracker**
- Set month-end or custom goal deadlines with visual progress meters.
- Filter between active and completed targets to stay aligned with long-term aspirations.

### **Optional App Lock — PIN + Biometric Security**
- **PIN & Biometric Unlock:** Secure your tasks, journals, and goals using a **4-Digit PIN** or native **Biometric Fingerprint / Face Recognition**.
- **Configurable Auto-Lock Timeout:** Set auto-lock sensitivity (**Immediately**, **1 minute**, **5 minutes**, **15 minutes**, **30 minutes**).
- **Hashed Security:** PINs are stored securely using salted **SHA-256** hashing.

### **Secure AES-256 Encrypted Local Backups**
- **AES Encryption:** Toggle **Secure Backup** in Settings to encrypt exported JSON backups using AES-256 encryption (`TASKSPACE_ENCRYPTED_V1:` payload format).
- **Automated Local Backups:** Schedule automated background exports (**Daily**, **Weekly**, or **Off**) saved directly to secure internal app storage.
- **Seamless Restore:** Intelligent importer auto-detects encrypted vs. plaintext backups and restores Room database state flawlessly.

### **Color Palettes & AMOLED Pitch-Black Engine**
- **7 Curated Color Palettes:** Choose between **Indigo**, **Emerald**, **Rose**, **Amber**, **Violet**, **Teal**, and **Monochrome (Black & White)**.
- **Theme Modes:** **AMOLED Dark (Pitch Black `#000000`)**, **Standard Dark**, **Light Mode**, and **System Default**.
- **Dynamic Font Engine:** Switch typography dynamically between **Default**, **Rounded**, **Serif**, and **Monospace**.

### **History & Insights Analytics**
- **Monthly Insights:** Visual completion rate charts, best completion day, streak records, and focus area analysis.
- **Task Copying:** Copy any past history task directly to **Today**, **Tomorrow**, or **Any Day of the Week** while keeping original history logs intact.

---

## Technology Stack & Architecture

TaskSpace follows Android's recommended **MVVM (Model-View-ViewModel)** architecture paired with **Clean Architecture** principles.

| Layer / Domain | Technology Used |
| :--- | :--- |
| **Language** | Kotlin 1.9.0 (100% Native) |
| **UI Framework** | Jetpack Compose + Material Design 3 (M3) |
| **Data Persistence** | Room Database (SQLite ORM with Migration History v1–v5) |
| **Preferences** | Jetpack DataStore (Preferences Key-Value Store) |
| **Async & Reactive** | Kotlin Coroutines, StateFlow, SharedFlow |
| **Navigation** | Jetpack Compose Navigation (`NavHost`, `NavController`) |
| **Scheduling** | Android `AlarmManager`, `PendingIntent`, `BroadcastReceiver` |
| **JSON Serialization** | Gson |
| **Build & Automation** | Gradle Kotlin DSL, GitHub Actions CI/CD |

---

## Getting Started & Building Your Own APK

### Prerequisites
- **Android Studio:** Ladybug (2024.2.1+) or newer
- **JDK:** Version 17 or higher
- **Android SDK:** API 34 (Android 14) compile SDK, API 26 (Android 8.0) minimum SDK

### 1. Clone the Repository
```bash
git clone https://github.com/Sushil1524/TaskSpace.git
cd TaskSpace
```

### 2. Build Debug APK Locally
Open a terminal in the project root directory and run:

**On Windows (PowerShell / Command Prompt):**
```powershell
.\gradlew assembleDebug
```

**On macOS / Linux:**
```bash
./gradlew assembleDebug
```

After compilation finishes, your Debug APK will be generated at:
```
app/build/outputs/apk/debug/app-debug.apk
```

### 3. Build Signed Release APK Locally
To build an optimized, signed production APK:

1. Configure your release keystore credentials in `keys/keystore.properties`:
```properties
storeFile=keys/android-sign-keys-new.jks
storePassword=your_store_password
keyAlias=your_key_alias
keyPassword=your_key_password
```

2. Run the release build command:
```bash
.\gradlew assembleRelease
```

The signed Release APK will be created at:
```
app/build/outputs/apk/release/app-release.apk
```

### 4. Verify Release APK Signature
To verify if your generated Release APK is properly signed with your keystore:

**Using `apksigner` (Android SDK Tool):**
```powershell
& "$env:LOCALAPPDATA\Android\Sdk\build-tools\36.0.0\apksigner.bat" verify --verbose app\build\outputs\apk\release\app-release.apk
```

**Using `keytool` (Inspect Certificate & SHA-256):**
```powershell
& "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe" -printcert -jarfile app\build\outputs\apk\release\app-release.apk
```

---

## Continuous Integration & Automated Releases

TaskSpace includes a GitHub Actions workflow (`.github/workflows/release.yml`) that automatically builds and attaches an APK to GitHub Releases whenever a version tag is pushed, or it can be configured to Build Automatically whenever a push happens on a branch as well:

```bash
git tag -a v1.1.0 -m "Release v1.1.0"
git push origin v1.1.0
```

### 🗝️ Signing your Automated Releases (Optional)
To output signed APKs directly from GitHub Actions:
1. Generate your production keystore file (`.jks` / `.keystore`).
2. Encode your keystore in Base64: `base64 -w 0 your-keystore.jks`.
3. In your GitHub repository, navigate to **Settings → Secrets and variables → Actions**.
4. Add the following secrets:
   - `KEYSTORE_BASE64`
   - `KEY_ALIAS`
   - `KEY_PASSWORD`
   - `STORE_PASSWORD`
5. Configure a signing step (such as `r0adkll/sign-android-release@v1`) in `.github/workflows/build-release.yml`.

---
