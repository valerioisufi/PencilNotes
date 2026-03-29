# PencilNotes

PencilNotes is a native **Android note‑taking app designed for stylus input**, built with **Jetpack Compose** and backed by a local database.  
The project is currently structured as a multi-module Gradle build and includes **DrawView** (my drawing/document engine) as a **Git submodule**.

> This repository includes `DrawView` as a submodule at `draw-view/` (see `.gitmodules`) and it is wired into Gradle as `:draw-view` (see `settings.gradle.kts`).

## Modules

- `:app` — the PencilNotes Android application
- `:draw-view` — DrawView module (included from `draw-view/draw-view`), responsible for drawing/editing documents

## Features (current codebase)

- **Document & folder library** (create, rename, delete)
- **Sort & organization**
    - sort by name / created date / modified date / last opened
    - ascending/descending toggle
    - optional “keep folders on top”
- **Multi-select actions** in the file list
    - select all
    - delete multiple items
    - move items between folders
- **Recents screen** (based on “last opened”)
- **Drawing editor** (powered by DrawView)
    - the app launches the editor via `DrawRoute(documentId = …)` from the `:draw-view` module
- **Automatic persistence** (Room + serialization happen in the DrawView data layer)

## Tech stack

- **Kotlin**
- **Jetpack Compose**
- **Material 3**
- **Room** (with KSP)
- **kotlinx.serialization**
- **AndroidX Ink** (through DrawView)
- Gradle Wrapper: **9.4.0**
- Android Gradle Plugin: **9.1.0**
- Kotlin: **2.3.20**
- `minSdk 29`, `targetSdk 36`, `compileSdk 36`

## Requirements

- Android Studio (recent version recommended)
- Android device/emulator on Android 10+ (API 29+)

## Getting started

### Clone (including the DrawView submodule)

```bash
git clone --recurse-submodules https://github.com/valerioisufi/PencilNotes.git
```

If you already cloned without submodules:

```bash
git submodule update --init --recursive
```

### Run

1. Open the project in Android Studio.
2. Sync Gradle.
3. Run the `app` configuration.

## App navigation (high level)

The app declares three activities:

- `MainActivity` — the launcher activity; hosts the main Compose UI (file explorer / recents)
- `DrawActivity` — full-screen drawing editor host; opens a document by ID
- `SettingsActivity` — settings screen (currently a scaffold placeholder in code)

## How PencilNotes uses DrawView

PencilNotes treats DrawView as an internal module:

- Gradle includes it as `:draw-view` and the app depends on it:
  ```kotlin
  dependencies {
      implementation(project(":draw-view"))
  }
  ```

- Opening a note in the editor is delegated to DrawView’s route:
  ```kotlin
  DrawRoute(
      documentId = documentId,
      onNavigateBack = { finish() }
  )
  ```

- File & document management in the PencilNotes UI is backed by DrawView’s data layer:
    - `DataModule.getFileRepository(context)`
    - `FileRepository` APIs for folders/documents and “recent documents”

## Project structure (main pieces)

- `app/src/main/java/com/studiomath/pencilnotes/ui/`
    - `MainActivity.kt` — main UI, selection mode, actions (move/delete/select all), FAB new note/folder flow
    - `DrawActivity.kt` — hosts DrawView editor
    - `SettingsActivity.kt` — settings screen scaffold
- `app/src/main/java/com/studiomath/pencilnotes/file/`
    - `FileExplorerViewModel.kt` — folder navigation + sorting + selection + move/delete actions
- `app/src/main/java/com/studiomath/pencilnotes/ui/composeComponents/`
    - `FileListComponent.kt` — file list UI (+ drag/drop WIP logic)
    - `HomeComponent.kt` — recents list UI
    - `DialogComponents.kt` — dialogs (rename/create/confirm etc.)

## Notes

- The `draw-view` directory in this repo is a **Git submodule** pointing to the DrawView repository.
- The `:draw-view` Gradle project is mapped to `draw-view/draw-view` (nested module inside the submodule).
- Release builds enable R8 (`isMinifyEnabled = true`).

## Contributing

Issues and PRs are welcome. For larger changes, please open an issue first describing the feature/bug and the intended approach.

## License

MIT — see `LICENSE`.