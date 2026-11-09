# StickyNodes

**A cross-platform vector whiteboard built with Kotlin Multiplatform and Compose Multiplatform.**

StickyNodes is an open-source graphical editor for sketches, diagrams, notes, and visual ideas. Draw on an expansive canvas, organise objects into nested groups, connect objects, transform your selection, and save your work locally.

The project is under active development. Its current focus is a reliable single-user editing experience, with shared editor logic across platforms and careful separation between geometry, interaction, and document state.

[**Try the web app**](https://stickynodes.vercel.app/) · [**Desktop releases**](https://github.com/soyuz-dev/StickyNodeClient/releases) · [**Report an issue**](https://github.com/soyuz-dev/StickyNodeClient/issues)

## Features

### Draw and navigate

- **Canvas:** Pan, zoom, reset zoom to 100%, and fit content or selections to the viewport.
- **Shapes:** Draw rectangles and ellipses, with configurable fill and stroke styling.
- **Freehand paths:** Sketch using configurable stroke properties.
- **Text:** Place and edit text directly on the canvas.
- **Selection:** Click, Shift-click, or use a selection marquee to select multiple objects.
- **Smart snapping:** Snap dragged objects to neighbouring edges and centres, with visual alignment guides and equal-spacing indicators.

### Transform and arrange

- Move, rotate, resize, and duplicate shapes.
- Resize with eight handles, including proportional resizing.
- Rotate using an on-canvas handle; hold **Shift** to snap to 15° increments.
- Nudge selections using the arrow keys.
- Align selected objects to edges or centres and distribute them with equal spacing.
- Match the width, height, or both dimensions of selected individual shapes.
- Reorder objects to control their stacking order.
- Use the **Transform Inspector** to edit coordinates, dimensions, and rotation numerically.

### Nested groups

Groups are part of the document hierarchy, not merely visual selection outlines.

- Group related objects, including groups inside other groups.
- Select and transform groups as logical objects while retaining their descendants.
- Move, rotate, and resize compatible groups, including selections combining groups and standalone shapes.
- Align and distribute selected members as units, preserving their internal arrangement.
- Duplicate, ungroup, delete, and restore groups through document history.
- Navigate between group and child selections.

**Transform details:** Oriented group resizing is available for compatible shape-only selections sharing a common orientation. Mixed rotations and selections containing unsupported node types do not expose those resize handles. Matching exact dimensions remains an operation on individual shapes rather than a group property.

### Connectors

StickyNodes supports connectors for linking objects and drawing relationships between them.

- Create connectors directly on the canvas using the Connector tool.
- Route connections using straight segments or 45°-constrained paths.
- Smooth corners using quadratic Bézier curves.
- Independently attach either endpoint to a node or leave it freely positioned.
- Automatically resolve attached endpoint positions as connected nodes move.
- Preserve fallback positions for unresolved attachments.
- Include connector creation, updates, and deletion in document history.
- Save and reopen connectors and their endpoint bindings using `.stn` files.

Connector routing and endpoint resolution are separated from rendering and document storage, allowing additional attachment and routing strategies to be introduced without redesigning the persistent model.

**Current limitations:** Attached endpoints currently resolve to node centres rather than shape boundaries. Connector-specific selection, endpoint manipulation after creation, and more advanced routing remain under development.

### Editing and history

- Undo and redo committed edits.
- Cut, copy, paste, and duplicate selections.
- Preview transformations while dragging, committing completed gestures as individual history operations.
- Preserve group hierarchy while editing descendant geometry.
- Include connectors in immutable document history snapshots.

### Save and reopen documents

StickyNodes uses its own `.stn` document format, backed by Kotlinx Serialization.

Documents retain nodes, styling, positions, sizes, rotations, recursive group structure, and connectors with free or attached endpoints.

The current document format is **version 3**, with backward compatibility for supported versions 1 and 2.

| Version | Features |
| --- | --- |
| 1 | Flat node documents |
| 2 | Recursive group hierarchy |
| 3 | Connectors and endpoint bindings |

Document loading validates format versions and IDs, including nested group members and connectors. Attached endpoints may reference missing nodes, in which case their fallback coordinates are used.

The format remains under development and may evolve.

## Getting started

### Web

Open [stickynodes.vercel.app](https://stickynodes.vercel.app/) in a browser. No installation required.

### Desktop

Download a build from [GitHub Releases](https://github.com/soyuz-dev/StickyNodeClient/releases).

The release workflow produces executable JAR builds for these platforms:

| Operating system | Architecture |
| --- | --- |
| Windows | x64 |
| Linux | x64 |
| macOS | ARM64 (Apple Silicon) |
| macOS | x64 (Intel) |

Run a downloaded JAR with a compatible Java runtime:

```bash
java -jar <downloaded-file>.jar
```

**Testing note:** Windows has been tested locally. Linux and macOS builds are generated by GitHub Actions but have not yet been manually runtime-tested.

### Android

An Android application module is available for building from source:

```bash
./gradlew :androidApp:assembleDebug
```

## Build from source

**Requirements:** Git, a suitable JDK, Gradle (via the included wrapper), and an Android SDK for Android builds. Some targets require platform-specific tooling.

```bash
git clone https://github.com/soyuz-dev/StickyNodeClient.git
cd StickyNodeClient
```

| Task                      | Command                                            |
|---------------------------|----------------------------------------------------|
| Run desktop app           | `./gradlew :desktopApp:run`                        |
| Desktop Hot Reload        | `./gradlew :desktopApp:hotRun --auto`              |
| Package desktop JAR       | `./gradlew :desktopApp:packageUberJarForCurrentOS` |
| Run Kotlin/Wasm web app   | `./gradlew :webApp:wasmJsBrowserDevelopmentRun`    |
| Run Kotlin/JS web app     | `./gradlew :webApp:jsBrowserDevelopmentRun`        |
| Build Android debug APK   | `./gradlew :androidApp:assembleDebug`              |
| Run common JVM tests      | `./gradlew :shared:jvmTest`                        |
| Deploy production web app | `./gradlew deployWeb`                              |

On Windows, use `.\gradlew` instead of `./gradlew`. Desktop JAR output is under `desktopApp/build/compose/jars/`.

The `deployWeb` task builds the production Kotlin/JS application, stages the output in a local deployment directory, and invokes the Vercel CLI with `--prod`. This requires an authenticated Vercel CLI and an appropriately linked project.

## How it's organised

```text
StickyNodeClient/
├── shared/
│   └── src/
│       ├── commonMain/kotlin/org/stickynodes/
│       │   ├── data/                 # Document DTOs, mapping, persistence
│       │   └── editor/
│       │       ├── draw/
│       │       │   ├── camera/       # Viewport transformations
│       │       │   ├── geometry/
│       │       │   │   ├── connector/
│       │       │   │   ├── path/
│       │       │   │   ├── selection/
│       │       │   │   ├── snapping/
│       │       │   │   ├── text/
│       │       │   │   └── transform/
│       │       │   ├── gesture/      # Pointer interactions
│       │       │   ├── model/        # Nodes, groups, connectors
│       │       │   ├── render/       # Canvas drawing
│       │       │   └── style/        # Fill and stroke models
│       │       ├── history/          # Documents, selection, undo/redo
│       │       └── tools/
│       └── commonTest/              # Shared editor tests
├── desktopApp/
├── webApp/
├── androidApp/
└── iosApp/                           # Not in the Gradle build
```

### Design principles

**A recursive document model.** Documents contain individual nodes and groups, and groups may contain other members recursively. Connectors are currently stored as separate document entities, avoiding assumptions that every drawable object owns an independent position.

**Geometry independent of UI.** Bounds, hit-testing, transformations, snapping, and connector routing are implemented as reusable geometric operations rather than being embedded directly in the renderer.

**Declarative endpoint bindings.** Connector endpoints represent either free positions or attachments to nodes. Callback-based resolution keeps the persistent connector model independent of document lookup and rendering.

**Preview, then commit.** Pointer gestures preview changes before committing document updates. Undo/redo stores document snapshots rather than every intermediate pointer movement.

**Shared core, platform-specific boundaries.** Kotlin Multiplatform shares editor models and behavior while services such as file storage adapt to each platform.

## Keyboard shortcuts

These are the current Ctrl-based shortcuts. Operating-system and browser shortcuts may occasionally take precedence.

| Shortcut                       | Action                                      |
|--------------------------------|---------------------------------------------|
| `S`                            | Select tool                                 |
| `R`                            | Rectangle tool                              |
| `E`                            | Ellipse tool                                |
| `D`                            | Pen tool                                    |
| `T`                            | Text tool                                   |
| `W`                            | Pan tool                                    |
| `C`                            | Connector tool                              |
| Arrow keys                     | Nudge selection                             |
| `Shift+Arrow keys`             | Nudge by a larger increment                 |
| `Ctrl+A`                       | Select all                                  |
| `Ctrl+C` / `Ctrl+X` / `Ctrl+V` | Copy / cut / paste                          |
| `Ctrl+D`                       | Duplicate                                   |
| `Ctrl+G` / `Ctrl+Shift+G`      | Group / ungroup                             |
| `Ctrl+Z`                       | Undo                                        |
| `Ctrl+Y` / `Ctrl+Shift+Z`      | Redo                                        |
| `Ctrl+S` / `Ctrl+O`            | Save / open                                 |
| `Ctrl+1`                       | Reset zoom to 100%                          |
| `Ctrl+Shift+F`                 | Fit to selection                            |
| `Ctrl+Shift+H`                 | Fit to content                              |
| `Delete`                       | Delete selection                            |
| `Escape`                       | Clear selection or return to selection tool |
| `Shift+Click`                  | Toggle a node's selection                   |
| `Shift+Drag` on empty canvas   | Add nodes with a marquee                    |
| `Shift+Resize`                 | Preserve aspect ratio                       |
| `Shift+Rotate`                 | Snap rotation to 15° increments             |

## Technology

- **Kotlin Multiplatform** for shared editor code
- **Compose Multiplatform** for the UI and canvas
- **Material 3** for interface components and theming
- **Kotlinx Serialization** for `.stn` documents
- **Gradle** for builds and testing
- **Compose Hot Reload** for desktop development
- **GitHub Actions** for desktop release builds
- **Vercel** for web deployment

## Testing and project status

The shared test suite covers document history, geometry, hit-testing, transformations, serialization, arrangement, nested groups, smart snapping, connector routing, endpoint resolution, and connector persistence.

Manual desktop testing has covered nested selections, group transformations, save/reopen workflows, undo/redo, connector rendering, and connector creation and attachment.

**StickyNodes remains an early-stage editor.** Some features are incomplete, and document formats and APIs may evolve.

Real-time collaborative editing is a longer-term goal and is **not implemented yet**.

Bug reports, suggestions, and contributions are welcome through [GitHub Issues](https://github.com/soyuz-dev/StickyNodeClient/issues).

---

Made with Kotlin, Compose Multiplatform, and a suspicious amount of geometric manipulation.

*The nodes have learned to move, rotate, reproduce, organise themselves, survive application restarts, and form relationships.*