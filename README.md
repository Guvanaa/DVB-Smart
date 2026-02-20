# <img src="logo.svg" width="64" height="64" valign="middle"> DVB-Smart

[![Platform](https://img.shields.io/badge/Platform-Android-3DDC84.svg)](https://www.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-7F52FF.svg)](https://kotlinlang.org/)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-1.5.8-4285F4.svg)](https://developer.android.com/jetpack/compose)
[![Developer](https://img.shields.io/badge/Developer-Guvana-FF2D55.svg)](#credits)

**DVB-Smart** is a premium transit companion for Dresden (VVO/DVB), meticulously crafted with an **Apple-inspired Liquid Glass** design language. Featuring **Gemini**, your intelligent AI guide, it combines real-time data accuracy with a sophisticated glassmorphism aesthetic to provide the ultimate commuting experience.

---

## 📱 Features

### ✨ Design & Experience
- **Liquid Glass UI**: High-fidelity glassmorphism effects including frosted glass surfaces, vibrant gradients, and smooth spring animations.
- **Adaptive Modes**: Native support for both **Light and Dark modes**, with color palettes optimized for readability and elegance.
- **Micro-interactions**: Subtle tactile feedback and fluid transitions that mimic a premium OS experience.

### 🗺️ Live Navigation
- **Real-time Vehicle Map**: Track buses, trams, and trains across Dresden. Icons show line numbers and are color-coded by punctuality.
- **Punctuality Indicators**:
    - 🟢 **Green**: Ahead of schedule
    - 🟡 **Yellow**: On-time
    - 🔴 **Red**: Delayed
- **Interactive Routes**: Select any vehicle to view its full path and a vertical (stop-by-stop) view with live delays.

### 🔍 Smart Utilities
- **Integrated Search**: Find stops, specific addresses, or Points of Interest (POIs) using high-performance geocoding.
- **One-Tap Favorites**: Quick-launch connections from your current location to saved destinations like Home or Work.
- **Gemini AI Assistant**: A creative, intelligent companion to help you navigate the DVB network through conversational queries.
- **Home Screen Widget**: Stay updated at a glance with the dedicated Favorites widget.

---

## 📸 Screenshots

<p align="center">
  <img src="app/screenshots/preview.png" width="800" alt="DVB-Smart Interface Preview">
</p>

---

## 🛠️ Technical Stack

- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) for modern declarative UI.
- **Map Engine**: [MapLibre SDK](https://maplibre.org/) for high-performance vector maps.
- **Networking**: [Retrofit](https://square.github.io/retrofit/) & [OkHttp](https://square.github.io/okhttp/) for robust API communication.
- **Data Sources**:
    - **VVO WebAPI**: Real-time transit data.
    - **Photon API**: Open-source geocoding for POIs and addresses.
- **Architecture**: MVVM with a clean Repository pattern.

---

## 🚀 Getting Started

### Prerequisites
- **Android Studio Iguana** or newer.
- **JDK 17**.
- Physical device or emulator with **Android 8.0 (API 26)** or higher.

### Installation
1. Clone the repository:
   ```bash
   git clone https://github.com/Guvana/DVB-Smart.git
   ```
2. Open the project in Android Studio.
3. Synchronize Gradle and run the `app` module.

Alternatively, build the APK via command line:
```bash
./gradlew assembleDebug
```

---

## 👥 Credits

Developed with ❤️ by **Guvana** & **Gemini** (AI Assistant).

Special thanks to:
- **VVO (Verkehrsverbund Oberelbe)** & **DVB (Dresdner Verkehrsbetriebe)** for providing open transit data.
- The **MapLibre** community for their excellent open-source map engine.

---
<p align="center">© 2026 Guvana. All rights reserved.</p>
