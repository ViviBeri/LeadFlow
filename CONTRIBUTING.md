# Contributing to LeadFlow

Thank you for helping improve LeadFlow. Contributions of all sizes are welcome, including documentation, bug reports, accessibility improvements, UI fixes, tests and Android development.

LeadFlow is an Android application built with Kotlin, Jetpack Compose, Room and OpenStreetMap services. The current discovery flow also provides an external Google Maps fallback; it does not import Google Maps results into the app.

## Choose a contribution

- **Documentation:** clarify setup, explain a workflow or improve screenshots and examples.
- **Accessibility and UI:** improve labels, touch targets, screen-reader behavior, layout and error messages.
- **Reliability:** add focused tests for search parsing, HTTP errors, timeouts and empty responses.
- **Android development:** improve discovery, lead management, duplicate detection and export behavior.
- **Data responsibility:** document provider limitations and ensure integrations respect source terms and privacy requirements.

## Good first issues

Look for issues labelled `good first issue`. If you have another idea, open an issue first and explain the problem you want to solve.

Before beginning:
1. Check that the issue is still open and has not been claimed by another contributor.
2. Comment with the part you plan to work on.
3. Wait for maintainer confirmation before making a substantial or potentially overlapping change.

If you are new to Kotlin or Android, start with documentation, reproducible bug reports, UI text/accessibility or a small unit test.

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
.\\gradlew.bat testDebugUnitTest assembleDebug
```

A successful build verifies compilation and packaging; it does not replace testing network behavior on a physical Android device.

## Pull request guidelines

- Keep each pull request focused on one issue or improvement.
- Explain the problem, the approach and any relevant trade-offs.
- Link the related issue using `Closes #123` when appropriate.
- Include clear verification steps and screenshots for visible UI changes.
- Add or update tests where practical.
- Run the relevant Gradle checks before submitting.
- Do not include API keys, passwords, personal data or generated build files.
- Use your own GitHub account and ensure commit authorship accurately reflects who performed the work.
- Be respectful in reviews and be open to requested changes.

## Recognition and GitHub achievements

Contributors should work from their own GitHub accounts so their commits, issues and accepted pull requests are attributed correctly. The project can acknowledge meaningful contributions in release notes or a future contributors list, with the contributor's consent.

GitHub decides whether activity counts toward a profile contribution graph or earns an achievement. LeadFlow maintainers cannot award or guarantee GitHub badges, achievements, profile placement or employment outcomes. Contributions should be genuine work, not artificial activity created to chase badges.

## Code quality

- Follow existing Kotlin and Jetpack Compose patterns.
- Prefer clear names and small functions.
- Handle network and parsing failures explicitly.
- Avoid unrelated formatting or refactoring in bug-fix pull requests.
- Respect OpenStreetMap and any other data provider's terms, attribution and usage limits.

## Communication

Introduce yourself in the relevant issue with the task you want to take and any relevant experience. Maintainers will help clarify scope and review focused contributions. Please be patient: this is a community project maintained on a best-effort basis.
