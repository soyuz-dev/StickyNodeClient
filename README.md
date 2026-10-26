# StickyNodes

**A lightweight, cross-platform vector whiteboard built with Kotlin and Compose Multiplatform.**

StickyNodes is a simple canvas editor for sketching ideas, drawing shapes, adding text, and organising things visually.

It's currently in early development, with a focus on keeping the editing experience straightforward and responsive.

**[Try it online](https://stickynodes.vercel.app/) · [Download for desktop](https://github.com/soyuz-dev/StickyNodeClient/releases) · [Report an issue](https://github.com/soyuz-dev/StickyNodeClient/issues)**

## Features

- **Infinite canvas** — Pan, zoom, and navigate freely.
- **Drawing tools** — Create rectangles, ellipses, and freehand paths.
- **Text** — Place and edit text directly on the canvas.
- **Selection and styling** — Select objects and customise their appearance.
- **Undo and redo** — Easily reverse or restore changes.
- **Fit to content** — Automatically frame your drawing with an animated camera transition.
- **Document persistence** — Save and open `.stn` documents.

## Getting started

### Web

Use StickyNodes directly in your browser:

**https://stickynodes.vercel.app/**

No installation required.

### Desktop

Download the latest build from the [GitHub Releases page](https://github.com/soyuz-dev/StickyNodeClient/releases).

| Platform | Architecture          |
|----------|-----------------------|
| Windows  | x64                   |
| Linux    | x64                   |
| macOS    | Apple Silicon (ARM64) |
| macOS    | Intel (x64)           |

The desktop application is currently distributed as an executable JAR and requires a compatible Java runtime.

Run it using:

```bash
java -jar StickyNodes-0.1.0-windows-x64.jar
```

Replace the filename with the downloaded JAR for your platform.

Depending on your operating system and Java configuration, double-clicking the JAR may also work.

> **Note:** Windows has been tested locally. Linux and macOS builds are generated through GitHub Actions but have not yet been manually runtime-tested.

## Building from source

### Requirements

- A JDK compatible with the project's Gradle version
- Git

Clone the repository:

```bash
git clone https://github.com/soyuz-dev/StickyNodeClient.git
cd StickyNodeClient
```

### Desktop

Run the application:

```bash
./gradlew :desktopApp:run
```

Build an executable JAR for your current operating system:

```bash
./gradlew :desktopApp:packageUberJarForCurrentOS
```

The generated JAR can be found under:

```text
desktopApp/build/compose/jars/
```

For development with Compose Hot Reload:

```bash
./gradlew :desktopApp:hotRun --auto
```

On Windows, use `.\gradlew` instead of `./gradlew`.

### Web

Run the Kotlin/Wasm development server:

```bash
./gradlew :webApp:wasmJsBrowserDevelopmentRun
```

Or use the JavaScript target:

```bash
./gradlew :webApp:jsBrowserDevelopmentRun
```

### Android

Build the Android debug APK:

```bash
./gradlew :androidApp:assembleDebug
```

Android and iOS are not currently part of the published desktop release.

## Project structure

```text
StickyNodeClient/
├── shared/       Shared editor UI, models and logic
├── desktopApp/   Desktop JVM application
├── webApp/       Web application
├── androidApp/   Android application
└── iosApp/       iOS application
```

Most of the editor's functionality lives in the `shared` module, allowing platform-specific applications to reuse the same underlying implementation.

## Technology

StickyNodes is built using:

- **Kotlin Multiplatform** — Shared application logic across platforms
- **Compose Multiplatform** — Declarative user interface and canvas rendering
- **Kotlinx Serialization** — Document serialisation
- **Gradle** — Builds and dependency management
- **GitHub Actions** — Automated desktop builds for Windows, Linux and macOS

## Development status

StickyNodes is an early-stage project.

The current priority is building a reliable single-user vector editor before introducing more advanced functionality.

Features and the `.stn` document format may change as development continues.

Bug reports, feedback and suggestions are welcome through [GitHub Issues](https://github.com/soyuz-dev/StickyNodeClient/issues).

## Links

- **Web app:** https://stickynodes.vercel.app/
- **Desktop releases:** https://github.com/soyuz-dev/StickyNodeClient/releases
- **Source code:** https://github.com/soyuz-dev/StickyNodeClient

---

Made with Kotlin, Compose Multiplatform, and a healthy amount of experimentation.