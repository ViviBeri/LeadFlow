# LeadFlow
Manual WhatsApp lead manager (Kotlin + Jetpack Compose + Room). No scraping, no auto-send, no accessibility service.

## Build the APK
1. Install Android Studio (Iguana or newer). It bundles JDK 17.
2. File > Open > choose this LeadFlow folder. Wait for "Gradle sync" to finish (first run downloads ~500 MB; needs internet).
   If it says the Gradle wrapper is missing, accept the prompt to download/create it.
3. Build > Build Bundle(s) / APK(s) > Build APK(s). Click "locate" in the popup.
   File: app/build/outputs/apk/debug/app-debug.apk

## Fallback if sync fails: paste into a fresh project
1. New Project > Empty Activity > Name LeadFlow, Package com.leadflow.app, Min SDK 24, Language Kotlin.
2. Replace: root build.gradle(.kts), settings.gradle(.kts), gradle.properties, app/build.gradle(.kts), AndroidManifest.xml.
3. Delete the generated MainActivity and ui/theme folder, then copy in app/src/main/java/... and app/src/main/res/... from this zip.
4. File > Sync Project with Gradle Files.

## Install on a phone
- Copy app-debug.apk to the phone (USB, Drive, WhatsApp to self). Open it from the Files app.
- When asked, allow "Install unknown apps" for that app, then tap Install.
- Or: enable Developer options > USB debugging, plug in, press the green Run button in Android Studio (or `adb install app-debug.apk`).

## Files
- build.gradle / settings.gradle / gradle.properties: project setup, plugin versions
- app/build.gradle: SDK 24-34, Compose, Room, navigation libraries
- AndroidManifest.xml: permissions, MainActivity, FileProvider
- res/xml/file_paths.xml: folder FileProvider may share (exports)
- res/values/strings.xml, themes.xml, res/drawable/ic_launcher.xml: app name, theme, icon
- data/Lead.kt: the database table row
- data/LeadDao.kt: queries (search, count, insert, delete)
- data/AppDatabase.kt: Room database
- data/Prefs.kt: SharedPreferences (message template, delay reminder)
- util/Utils.kt: phone validation + wa.me link opener
- util/Exporter.kt: builds CSV, XLSX and PDF bytes
- util/FileStore.kt: saves to Downloads/LeadFlow and to the shareable private copy
- LeadViewModel.kt: holds data/state for all screens
- MainActivity.kt: navigation between screens
- ui/Theme.kt: colours + shared top bar
- ui/HomeScreen.kt, AddLeadScreen.kt, LeadsScreen.kt, ExportScreen.kt, SettingsScreen.kt: the 5 screens
