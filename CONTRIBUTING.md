# Contributing to LeadFlow

Thank you for considering a contribution to LeadFlow.

LeadFlow is an Android application built with Kotlin, Jetpack Compose, Room, and OpenStreetMap services.

## Ways to contribute

- Report reproducible bugs with device and Android version details.
- Improve accessibility, UI behavior, and error messages.
- Add unit tests for data and networking code.
- Improve Nominatim and Overpass search reliability.
- Improve documentation.

## Development setup

1. Fork this repository.
2. Clone your fork.
3. Open the project in Android Studio.
4. Use JDK 17 and the Gradle 8.2 wrapper.
5. Allow Gradle to sync and download dependencies.
6. Run the unit tests and build the debug APK:

```bash
./gradlew testDebugUnitTest assembleDebug
```

On Windows PowerShell:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

## First contribution

A suitable starter task is improving tests and error handling around place search:
- HTTP error responses
- Network timeouts
- Empty response bodies
- Invalid or malformed JSON
- Successful result parsing

Before starting a large change, comment on the related GitHub issue to coordinate the work.

## Pull request guidelines

- Keep each pull request focused on one change.
- Explain the problem and the solution.
- Include steps to reproduce and verify behavior.
- Add or update tests where practical.
- Run the relevant Gradle checks before submitting.
- Do not include API keys, passwords, personal data, or generated build files.
- Use your own GitHub account and ensure commit authorship accurately reflects who performed the work.

## Code quality

- Follow the existing Kotlin and Compose patterns in the project.
- Prefer clear names and small functions.
- Handle network and parsing failures explicitly.
- Avoid unrelated formatting or refactoring in bug-fix pull requests.

## Communication

Introduce yourself in the relevant issue with your Android/Kotlin experience and the task you would like to take. Maintainers will help coordinate a focused first contribution.
