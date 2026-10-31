# StickyNodes

**A lightweight, cross-platform vector whiteboard built with Kotlin Multiplatform and Compose Multiplatform.**

StickyNodes is an open-source canvas editor for sketching ideas, drawing shapes, writing notes, and organising things visually.

Create objects, move them around, transform them individually or together, and save your work for later.

It's currently in early development, with an emphasis on responsive editing, straightforward controls, and a maintainable shared codebase.

**[Try it online](https://stickynodes.vercel.app/) · [Desktop releases](https://github.com/soyuz-dev/StickyNodeClient/releases) · [Report an issue](https://github.com/soyuz-dev/StickyNodeClient/issues)**

## Features

### Canvas and drawing

- **Infinite canvas** — Pan and zoom freely around your workspace.
- **Basic shapes** — Create rectangles and ellipses.
- **Freehand drawing** — Draw paths with configurable stroke widths and colours.
- **Text** — Place text directly on the canvas and customise its appearance.
- **Styling** — Change fill colours, strokes, and other supported properties.
- **Fit to content** — Automatically frame your work using an animated camera transition.

### Selection and transformations

- **Multi-selection** — Select multiple objects using Shift-click or rectangular marquee selection.
- **Movement** — Drag individual objects or entire selections.
- **Resizing** — Resize shapes using eight handles, with optional aspect-ratio preservation.
- **Rotation** — Rotate individual shapes using a dedicated handle.
- **Group rotation** — Rotate multiple shapes around a shared centre while preserving their relative arrangement.
- **Angle snapping** — Hold Shift while rotating to snap to 15° increments.
- **Duplication** — Duplicate individual objects or entire selections with Ctrl+D.
- **Undo and redo** — Reverse or restore document changes.

Rotated shapes use rotation-aware hit-testing and selection geometry, so interaction remains consistent with what appears on screen.

**Current limitation:** Group resizing is disabled when the selection includes rotated shapes. Group rotation and movement remain supported.

### Documents

- Save and reopen documents using the `.stn` format.
- Preserve object positions, dimensions, styling, and shape rotations.
- Retain compatibility with older documents that do not contain rotation information.
- Work with documents locally without requiring an account or a collaboration server.

The document format is still evolving and may change during development.

## Keyboard shortcuts

| Shortcut | Action |
|---|---|
| `S` | Selection tool |
| `R` | Rectangle tool |
| `E` | Ellipse tool |
| `D` | Freehand drawing tool |
| `T` | Text tool |
| `W` | Pan tool |
| `Ctrl+D` | Duplicate selection |
| `Ctrl+Z` | Undo |
| `Ctrl+Y` / `Ctrl+Shift+Z` | Redo |
| `Ctrl+S` | Save document |
| `Ctrl+O` | Open document |
| `Delete` | Delete selection |
| `Escape` | Clear selection or return to selection tool |
| `Shift+Click` | Toggle an object in the selection |
| `Shift+Drag` on empty canvas | Add objects using marquee selection |
| `Shift+Resize` | Preserve aspect ratio |
| `Shift+Rotate` | Snap rotation to 15° increments |

These shortcuts describe the current Ctrl-based implementation, primarily used on desktop. Browser and operating-system shortcuts may interfere with certain combinations.

## Getting started

### Web

Open StickyNodes in your browser:

**https://stickynodes.vercel.app/**

No installation required.

### Desktop

Download a build from [GitHub Releases](https://github.com/soyuz-dev/StickyNodeClient/releases).

Desktop builds are provided for:

| Platform | Architecture |
|---|---|
| Windows | x64 |
| Linux | x64 |
| macOS | ARM64 (Apple Silicon) |
| macOS | x64 (Intel) |

Desktop releases are currently distributed as executable JAR files and require a compatible Java runtime.

Run a downloaded JAR using:

```bash
java -jar <downloaded-file>.jar
```

Replace `<downloaded-file>` with the actual filename.

**Platform testing:** Windows has been tested locally. Linux and macOS builds are generated through GitHub Actions but have not yet been manually runtime-tested.

## Building from source

### Requirements

- Git
- A JDK compatible with the project's Gradle version
- The Android SDK if building for Android
- Additional platform tooling where required

Clone the repository:

```bash
git clone https://github.com/soyuz-dev/StickyNodeClient.git
cd StickyNodeClient
```

### Desktop

Run the desktop application:

```bash
./gradlew :desktopApp:run
```

Create an executable JAR:

```bash
./gradlew :desktopApp:packageUberJarForCurrentOS
```

The JAR will be generated under:

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

Alternatively, run the Kotlin/JS target:

```bash
./gradlew :webApp:jsBrowserDevelopmentRun
```

### Android

Build the Android debug APK:

```bash
./gradlew :androidApp:assembleDebug
```

Alternatively, run the Android app directly from IntelliJ IDEA or Android Studio using a connected device or emulator, or install the APK using ADB.

Android and iOS are not currently part of the published desktop releases.

## Architecture

StickyNodes uses **Kotlin Multiplatform** to share its editor implementation across platforms.

Most application logic and rendering live in `shared/src/commonMain`, allowing the desktop, web, and mobile applications to reuse the same editor.

### Project structure

```text
StickyNodeClient/
├── shared/
│   └── src/
│       ├── commonMain/
│       │   └── kotlin/org/stickynodes/
│       │       ├── data/
│       │       └── editor/
│       │           ├── draw/
│       │           │   ├── camera/
│       │           │   ├── geometry/
│       │           │   ├── gesture/
│       │           │   ├── model/
│       │           │   ├── render/
│       │           │   └── style/
│       │           ├── history/
│       │           └── tools/
│       ├── commonTest/
│       └── ...
├── desktopApp/
├── webApp/
├── androidApp/
└── iosApp/
```

### Design principles

**Shared document model**

Rectangles, ellipses, freehand paths, and text share a sealed node hierarchy. Shape-specific geometry and transformations are implemented separately from document persistence.

**Geometry separate from rendering**

Pure geometry functions handle operations such as translation, resizing, rotation, bounding-box calculations, and hit-testing.

Rendering uses Compose Canvas and reuses the same geometry calculations employed by the interaction system.

**Gesture dispatch and handling**

A central interaction detector determines which operation owns a pointer gesture. Dedicated handlers manage movement, resizing, rotation, and marquee selection.

**Preview before commit**

Interactive transformations produce temporary previews while dragging. Completed operations are committed to document history, allowing a drag to become a single undoable action.

**Document history**

Undo and redo use immutable document snapshots, keeping history management separate from drawing and gesture recognition.

**Cross-platform persistence**

Documents are serialized using Kotlinx Serialization, with platform-specific storage implementations.

## Tech stack

- **Kotlin Multiplatform** — Shared application implementation
- **Compose Multiplatform** — UI, canvas rendering, pointer and keyboard input
- **Kotlinx Serialization** — JSON document serialization
- **Gradle** — Build system and dependency management
- **Compose Hot Reload** — Iterative desktop development
- **GitHub Actions** — Automated desktop builds
- **Vercel** — Web deployment

## Testing

The shared test suite covers editor logic, including geometry, transformations, selection behaviour, document history, and serialization.

Recent tests cover rotated geometry, group transformations, selection-handle calculations, and duplication.

The emphasis is on testing deterministic editor operations independently of the user interface, alongside manual interaction testing.

## Development status

StickyNodes is an early-stage, single-user vector editor.

Current priorities include:

- Improving editor correctness and consistency
- Refining the existing tools and interaction experience
- Keeping the shared architecture maintainable
- Expanding automated tests
- Improving platform-specific usability

Collaborative editing, advanced drawing tools, and further document-format improvements may be explored later, but they are not currently guaranteed features.

Contributions, bug reports, and suggestions are welcome through [GitHub Issues](https://github.com/soyuz-dev/StickyNodeClient/issues).

## Links

- **Web app:** https://stickynodes.vercel.app/
- **Desktop releases:** https://github.com/soyuz-dev/StickyNodeClient/releases
- **Source code:** https://github.com/soyuz-dev/StickyNodeClient
- **Issue tracker:** https://github.com/soyuz-dev/StickyNodeClient/issues

---

Made with Kotlin, Compose Multiplatform, and a suspicious amount of geometric manipulation.

**The nodes have learned to move, rotate, and reproduce.**