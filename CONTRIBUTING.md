# Contributing to LeadFlow

Thank you for helping improve LeadFlow. Contributions of all sizes are welcome, including documentation, bug reports, accessibility improvements, UI fixes, tests and Android development.

LeadFlow is an Android application built with Kotlin, Jetpack Compose, Room and OpenStreetMap services. Place discovery currently uses OpenStreetMap Nominatim and Overpass; the app can open a place in Google Maps, but it does not import Google Maps results.

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

### Requirements

| Tool | Version / requirement |
|---|---|
| Android Studio | A current stable release |
| JDK | 17 |
| Gradle | 8.2 |
| Android SDK | Platform 34 and the matching build tools |
| Android device/emulator | API 24 or newer for runtime checks |

The project uses Android Gradle Plugin 8.2.2, Kotlin 1.9.22, KSP 1.9.22-1.0.17 and Compose compiler extension 1.5.8. The app's `minSdk` is 24 and `targetSdk` is 34.

**Important:** this repository currently contains `gradle/wrapper/gradle-wrapper.properties`, but does not contain the Gradle wrapper scripts or wrapper JAR. Do not use `./gradlew` or `gradlew.bat` until a complete wrapper is committed. Install Gradle 8.2 and use the `gradle` command below.

### Get the source

1. Fork the repository on GitHub.
2. Clone your fork and enter the project directory:

```bash
git clone https://github.com/<your-github-username>/LeadFlow.git
cd LeadFlow
git remote add upstream https://github.com/ViviBeri/LeadFlow.git
```

3. Open the project folder in Android Studio.
4. Set the Gradle JDK to JDK 17 in **Settings > Build, Execution, Deployment > Build Tools > Gradle**.
5. Install Android SDK Platform 34 and its build tools through **Tools > SDK Manager**.
6. Allow Android Studio to sync the project and download dependencies.

If Android Studio reports a missing SDK package, install the exact platform/build-tools version it requests through SDK Manager. Do not commit local SDK paths or machine-specific settings.

### Verify your environment

Run these commands from the repository root:

```bash
java -version
gradle --version
```

Confirm Java reports version 17 and Gradle reports version 8.2. If your system has multiple Java installations, set `JAVA_HOME` to the JDK 17 installation before running Gradle.

## Build and test

Run the same core checks used by the repository's GitHub Actions build:

```bash
gradle testDebugUnitTest assembleDebug --stacktrace
```

Run the checks separately when debugging:

```bash
# Run local JVM unit tests
gradle :app:testDebugUnitTest

# Build the debug APK
gradle :app:assembleDebug

# Run Android lint checks
gradle :app:lintDebug

# Generate the third-party dependency licence report
gradle :app:generateLicenseReport
```

Current local JVM tests are under `app/src/test/java/com/leadflow/app/`, including:
- `OpenPlaceSearchTest.kt` — place parsing and search-related behavior.
- `PhoneFormatTest.kt` — phone number normalization.

Useful output locations:

- Unit test HTML report: `app/build/reports/tests/testDebugUnitTest/index.html`
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Lint report, when generated: `app/build/reports/lint-results-debug.html`
- Dependency licence report: `app/build/reports/dependency-license/`

A successful unit-test task means the local JVM tests passed. A successful APK build verifies compilation and packaging; neither proves that live Nominatim/Overpass requests, location permissions, exports or device-specific behavior work correctly. For changes affecting those flows, test on an Android emulator or physical device and include the device/API level and steps in your pull request.

### GitHub Actions

Pull requests targeting `main` run the **Build APK** workflow. It uses Temurin JDK 17 and Gradle 8.2, runs unit tests and builds the debug APK, then generates a dependency licence report. The workflow uploads the debug APK and licence report as downloadable artifacts.

To inspect a run, open the repository's **Actions** tab, select **Build APK**, and open the run for your commit. If a check fails, include the failing task and relevant log excerpt in your pull request discussion.

## AI-assisted development

AI coding assistants are permitted. You may use tools such as ChatGPT, GitHub Copilot or Claude for research, explanations, debugging, test ideas, documentation and code suggestions. AI use is neither required nor a reason to reject a contribution.

The contributor remains responsible for every submitted change:

- Understand the code you submit and be able to explain its purpose and behavior.
- Review generated suggestions instead of accepting them blindly.
- Verify that APIs, dependencies, licenses and technical claims are appropriate for this project.
- Run relevant tests and describe what you actually verified.
- Check for security issues, exposed secrets, personal data and unrelated changes.
- Respond to review feedback and make corrections when needed.

Do not submit large, unreviewed generated changes or claim that tests passed when they were not run. Do not use AI tools to fabricate test results, commit history, authorship or contribution activity. Never paste credentials, private keys, personal data or other secrets into an AI service.

You do not need to disclose routine AI assistance. If a maintainer asks how a complex change was produced, explain the assistance used and, more importantly, demonstrate that you understand and take responsibility for the implementation.

## Pull request guidelines

- Keep each pull request focused on one issue or improvement.
- Explain the problem, the approach and any relevant trade-offs.
- Link the related issue using `Closes #123` when appropriate.
- Include clear verification steps and screenshots for visible UI changes.
- Add or update tests where practical.
- Run the relevant Gradle checks before submitting; state clearly if a check could not be run.
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
