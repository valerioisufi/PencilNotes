# PencilNotes

PencilNotes is a native Android note-taking application designed for stylus input. It provides an intuitive interface and advanced tools for creating, editing, and managing digital documents.

This project started as a personal endeavor during high school, with the first version being independently developed and published on the Google Play Store. It is currently undergoing a complete architectural redesign to improve performance, and maintainability, and to add new features.

## Key Features

* **Freehand Drawing**: Utilize tools like a pen, highlighter, eraser, and lasso selection for a natural writing experience.
* **Document Management**: Easily create, save, and manage multi-page documents.
* **Tool Customization**: Adjust the color, size, and style of your drawing tools to fit your needs.
* **Smooth Navigation**: Enjoy fluid zooming, panning, and page management with smooth animations.
* **Automatic Saving**: Your documents are saved automatically to prevent data loss.

## Technologies Used

* **Kotlin** and **Java**
* **Android SDK**
* **Room Database** for local storage
* **MVVM Architecture**

## Getting Started

To get a local copy up and running, follow these simple steps.

### Prerequisites

* Android Studio
* An Android device or emulator running Android 8.0 (API 26) or higher.

### Installation

1.  Clone the repository:
    ```bash
    git clone https://github.com/valerioisufi/PencilNotes.git
    ```
2.  Open the project in Android Studio.
3.  Sync the Gradle dependencies.
4.  Connect an Android device or start an emulator.
5.  Run the application.

## Project Structure

The project is organized as follows:

* `app/src/main/java/com/studiomath/pencilnotes/ui`: Contains the activities and UI components for the user interface.
* `app/src/main/java/com/studiomath/pencilnotes/document`: Manages the data and logic for creating and modifying documents.
* `app/src/main/java/com/studiomath/pencilnotes/ui/composeComponents`: Reusable components built with Jetpack Compose.
* `app/src/main/java/com/studiomath/pencilnotes/file`: Handles file management and storage.
* `app/src/main/java/com/studiomath/pencilnotes/ui/theme`: Defines the themes and styles for the app.

## Contributing

Contributions, bug reports, and suggestions are welcome! To contribute:

1.  Fork the Project.
2.  Create your Feature Branch (`git checkout -b feature/AmazingFeature`).
3.  Commit your Changes (`git commit -m 'Add some AmazingFeature'`).
4.  Push to the Branch (`git push origin feature/AmazingFeature`).
5.  Open a Pull Request.

## License

Distributed under the MIT License. See `LICENSE` for more information.
