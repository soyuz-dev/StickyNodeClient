# StickyNodes

**A lightweight, cross-platform vector whiteboard built with Kotlin Multiplatform and Compose Multiplatform.**

StickyNodes is an open-source graphical editor for sketching ideas, creating diagrams, writing notes, and organising information visually.

Draw shapes, manipulate objects individually or in groups, arrange your workspace, and save your documents for later.

The project is currently in active development, with a focus on responsive interactions, precise transformations, and a maintainable shared codebase.

**[Try it online](https://stickynodes.vercel.app/) · [Desktop releases](https://github.com/soyuz-dev/StickyNodeClient/releases) · [Report an issue](https://github.com/soyuz-dev/StickyNodeClient/issues)**

## Features

### Canvas and drawing

- **Infinite canvas** — Pan and zoom around your workspace.
- **Shapes** — Create rectangles and ellipses.
- **Freehand drawing** — Draw paths with configurable stroke widths and colours.
- **Text editing** — Create and edit text directly on the canvas.
- **Styling** — Customise fill colours, strokes, and supported text properties.
- **Camera controls** — Zoom, reset the view, and automatically fit your content to the viewport.

### Selection and transformations

- **Multi-selection** — Select nodes using Shift-click or rectangular marquee selection.
- **Movement** — Drag individual objects or entire selections.
- **Keyboard nudging** — Move selected objects using the arrow keys.
- **Resizing** — Resize shapes using eight handles, including proportional resizing.
- **Rotation** — Rotate shapes using a dedicated handle.
- **Group transformations** — Move, resize, and rotate multiple selected shapes.
- **Angle snapping** — Hold Shift while rotating to snap to 15° increments.
- **Rotation-aware geometry** — Accurate interaction and selection bounds for rotated shapes.
- **Duplication** — Duplicate selected nodes while preserving their properties.

Group resizing currently has some limitations when rotated shapes are involved.

### Layout and arrangement

StickyNodes includes layout tools for organising multiple objects.

- **Alignment** — Align selected objects along common edges or centres.
- **Distribution** — Evenly distribute selected objects.
- **Match dimensions** — Make selected shapes share the same width, height, or dimensions.
- **Layer ordering** — Bring objects forward or to the front, or send them backward or to the back.
- **Viewport selection** — Select objects visible within the current viewport.

These operations are integrated into the editor's existing selection and history systems.

### Transform Inspector

The floating Transform Inspector provides precise numerical control over selected shapes.

- Edit absolute X and Y coordinates.
- Set exact width and height.
- Specify rotation in degrees.
- Validate numerical input.
- Commit edits with Enter or by leaving a field.
- Cancel edits using Escape.
- Undo and redo committed transformations.

The inspector uses dedicated geometry operations, keeping transformation calculations separate from the UI.

### Editing and history

- Undo and redo document changes.
- Cut, copy, and paste nodes.
- Duplicate and delete selections.
- Modify node ordering.
- Group related movement actions into single undoable operations.
- Preview interactive transformations before committing them.

### Documents

StickyNodes supports saving and reopening documents using its own `.stn` format.

Documents preserve node positions, dimensions, styling, text, and shape rotations.

Persistence uses Kotlinx Serialization with platform-specific storage implementations. Documents can be edited locally without requiring an account.

The format is still evolving and may change between development versions.

## Getting started

### Web

Open the application directly in your browser:

https://stickynodes.vercel.app/

No installation required.

### Desktop

Download a build from [GitHub Releases](https://github.com/soyuz-dev/StickyNodeClient/releases).

Desktop builds are generated for:

| Platform | Architecture          |
|----------|-----------------------|
| Windows  | x64                   |
| Linux    | x64                   |
| macOS    | ARM64 (Apple Silicon) |
| macOS    | x64 (Intel)           |

Desktop releases are currently distributed as executable JAR files and require a compatible Java runtime.

```bash
java -jar <downloaded-file>.jar
```

**Platform testing:** Windows has been tested locally. Linux and macOS builds are generated through GitHub Actions but have not yet been manually runtime-tested.

### Android

An Android target is included in the project and can be built from source.

```bash
./gradlew :androidApp:assembleDebug
```

Android is not currently included in the published desktop releases.

## Building from source

### Requirements

- Git
- A compatible JDK
- Android SDK for Android builds
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

Build an executable JAR:

```bash
./gradlew :desktopApp:packageUberJarForCurrentOS
```

Generated JAR files are placed in `desktopApp/build/compose/jars/`.

For desktop development with Compose Hot Reload:

```bash
./gradlew :desktopApp:hotRun --auto
```

On Windows, use `.\gradlew` instead of `./gradlew`.

### Web

Start the Kotlin/Wasm development server:

```bash
./gradlew :webApp:wasmJsBrowserDevelopmentRun
```

Alternatively, use the Kotlin/JS target:

```bash
./gradlew :webApp:jsBrowserDevelopmentRun
```

## Architecture

StickyNodes uses Kotlin Multiplatform to share its editor implementation across supported targets.

The majority of editor logic resides in `shared/src/commonMain`, allowing rendering, geometry, document history, and user interactions to be implemented without duplicating them for every platform.

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
│       └── commonTest/
├── desktopApp/
├── webApp/
├── androidApp/
└── iosApp/
```

### Design principles

**Geometry separate from rendering**

Shape transformations, hit-testing, selection bounds, and related calculations are implemented as reusable geometry operations.

The canvas renderer and interaction system use these calculations without embedding all transformation logic directly into UI components.

**Dedicated gesture handling**

Pointer interactions are divided into separate handlers for movement, resizing, rotation, and selection.

This keeps individual operations easier to understand, test, and maintain.

**Preview before commit**

Interactive transformations can be previewed during a gesture before being committed to the document.

Completed gestures are recorded as undoable operations rather than requiring a separate history entry for every pointer movement.

**Immutable document history**

Document changes are managed using immutable snapshots, separating undo/redo management from rendering and pointer interactions.

**Shared models and platform-specific services**

The document model and editor logic are shared across platforms, while services such as document storage can use platform-specific implementations.

## Keyboard shortcuts

| Shortcut                     | Action                                      |
|------------------------------|---------------------------------------------|
| `S`                          | Selection tool                              |
| `R`                          | Rectangle tool                              |
| `E`                          | Ellipse tool                                |
| `D`                          | Freehand drawing tool                       |
| `T`                          | Text tool                                   |
| `W`                          | Pan tool                                    |
| Arrow keys                   | Nudge selection                             |
| `Ctrl+A`                     | Select all                                  |
| `Ctrl+C`                     | Copy                                        |
| `Ctrl+X`                     | Cut                                         |
| `Ctrl+V`                     | Paste                                       |
| `Ctrl+D`                     | Duplicate                                   |
| `Ctrl+Z`                     | Undo                                        |
| `Ctrl+Y` / `Ctrl+Shift+Z`    | Redo                                        |
| `Ctrl+S`                     | Save                                        |
| `Ctrl+O`                     | Open                                        |
| `Delete`                     | Delete selection                            |
| `Escape`                     | Clear selection or return to selection tool |
| `Shift+Click`                | Toggle node selection                       |
| `Shift+Drag` on empty canvas | Add nodes using marquee selection           |
| `Shift+Resize`               | Preserve aspect ratio                       |
| `Shift+Rotate`               | Snap rotation to 15° increments             |

Shortcuts describe the current Ctrl-based implementation. Browser and operating-system shortcuts may interfere with some key combinations.

## Tech stack

- **Kotlin Multiplatform** — Shared editor implementation
- **Compose Multiplatform** — UI, canvas rendering, and interaction handling
- **Material 3** — User interface components and theming
- **Kotlinx Serialization** — Document serialization
- **Gradle** — Build automation
- **Compose Hot Reload** — Desktop development workflow
- **GitHub Actions** — Automated desktop builds
- **Vercel** — Web deployment

## Testing

StickyNodes includes a common test suite for deterministic editor operations.

Tests cover areas including:

- Geometry and shape transformations
- Selection and hit-testing
- Rotation and resizing
- Node positioning and arrangement
- Alignment and distribution
- Document history
- Clipboard and duplication operations
- Document serialization
- Numerical transformation validation

Geometry and editor operations are tested separately from the user interface wherever practical.

Interactive behavior is additionally tested manually during development.

## Development status

StickyNodes is currently an early-stage, single-user graphical editor.

The immediate focus is on improving editing correctness, interaction consistency, platform usability, and test coverage.

Future development may include:

- Real-time collaborative editing
- Additional drawing and editing tools
- Further improvements to the document format
- More advanced object and layout controls
- Better cross-platform desktop and mobile experiences

Collaboration is a longer-term goal and is not yet implemented.

Contributions, bug reports, and suggestions are welcome through [GitHub Issues](https://github.com/soyuz-dev/StickyNodeClient/issues).

## Links

- [Live web app](https://stickynodes.vercel.app/)
- [Desktop releases](https://github.com/soyuz-dev/StickyNodeClient/releases)
- [Source code](https://github.com/soyuz-dev/StickyNodeClient)
- [Issue tracker](https://github.com/soyuz-dev/StickyNodeClient/issues)

---

Made with Kotlin, Compose Multiplatform, and a suspicious amount of geometric manipulation.

**The nodes have learned to move, rotate, reproduce, and organise themselves.**