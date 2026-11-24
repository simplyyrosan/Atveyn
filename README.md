# Atveyn 🎓

**Atveyn** is a modern, smart, and 100% offline-first Android attendance tracking and timetable management application designed specifically for college and university students.

Featuring on-device OCR timetable scanning, intelligent noon and merged laboratory detection, safe margin calculations, interactive timetable-dimmed calendars, and built-in GitHub in-app updates.

---

## ✨ Features

- 📸 **Smart Timetable OCR Scanner**:
  - Point your camera or choose an image/PDF of your timetable.
  - Powered on-device by Google ML Kit Text Recognition — zero data sent to any cloud or external servers.
  - Accurately distinguishes critical noon boundaries (`11:00 AM – 12:00 NOON` vs `12:00 NOON – 01:00 PM`).
  - Seamlessly detects merged multi-hour lab periods (e.g., 2-hour or 3-hour lab blocks) and binds them accurately to weekday slots.
  - Full interactive preview to edit, add, or remove subjects before saving.

- 📊 **Intelligent Attendance Management**:
  - Customizable attendance target thresholds (default 75% or custom per subject).
  - Clear **Overall Attendance** summary displayed by default with instant visual progress rings.
  - Actionable guidance:
    - ⚠️ **Shortage Advisory**: *"Attend next X classes consecutively to reach target."*
    - ✓ **Bunk Calculator**: *"You can miss Y classes and still remain above target."*
  - Expandable **Monthly Breakdown** to inspect individual month statistics at a glance.

- 🗓️ **Timetable-Aware Interactive Calendar**:
  - Visual monthly calendar with day-by-day attendance history.
  - **Timetable Day Dimming**: Automatically dims and marks weekdays where the subject has scheduled classes in your timetable.
  - Quick-tap actions to log **Present**, **Absent**, or **Cancelled / Holiday** for past, present, or future dates.

- 📈 **Analytics, Trends & What-If Simulator**:
  - Clean weekly activity trends showing present vs absent distributions without visual clutter.
  - Interactive **Attendance Simulator**: Test hypothetical scenarios (*"What happens to my percentage if I attend the next 3 classes and miss 1?"*).

- ⏰ **Smart Class-End Reminders**:
  - Exact alarms trigger prompt notifications right when a class ends, allowing you to mark attendance directly from the notification shade.

- 🔄 **In-App GitHub Updates**:
  - Built-in updater that queries GitHub Releases for new versions.
  - View release notes and download/install APK updates directly from within the app.

- 🔒 **100% Offline & Private**:
  - All data (subjects, schedules, attendance logs) resides strictly on your local device in an encrypted/private SQLite database via Room. No accounts, no ads, no trackers.

---

## 📥 Download & Installation

You can download the latest installable Android package (`.apk`) directly from GitHub:

👉 **[Download Latest APK from Releases](https://github.com/simplyyrosan/Atveyn/releases/latest)**

### Requirements:
- Android 8.0 (Oreo / API level 26) or newer.
- No Google Play Services required (fully functional on de-Googled devices; OCR runs locally on-device).

---

## 🛠️ Tech Stack & Architecture

- **Language**: Kotlin 1.9+
- **UI Toolkit**: Jetpack Compose with Material 3 Design
- **Architecture**: Clean Architecture + MVVM + Repository Pattern
- **Dependency Injection**: Dagger Hilt 2.51
- **Local Persistence**: Room Database (SQLite) + Jetpack DataStore Preferences
- **OCR Engine**: Google ML Kit Text Recognition (On-device)
- **Background Scheduling**: Android AlarmManager (Exact Alarms) + WorkManager
- **Build System**: Gradle Kotlin DSL (`build.gradle.kts`) with Java 17

---

## 🚀 Building from Source

### Prerequisites:
- Java Development Kit (JDK) 17
- Android SDK (API 34, Build Tools 34.0.0)

### Clone & Build:
```bash
git clone https://github.com/simplyyrosan/Atveyn.git
cd Atveyn

# Run automated unit tests
./gradlew testDebugUnitTest

# Build debug APK
./gradlew assembleDebug
```

The output APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 📄 License

This project is open-source under the [MIT License](LICENSE).
