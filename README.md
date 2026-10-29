# LeadFlow

**Zero-budget local business lead finder for Android** — search for businesses by category and location, review the results, save useful prospects, and export them for follow-up.

LeadFlow is evolving from a manual lead manager into a location-based prospecting tool. Example: enter **Restaurants** + **Pondicherry** to find publicly listed restaurants and hotels in the area.

<p align="center">
  <img src="https://raw.githubusercontent.com/ViviBeri/LeadFlow/main/readme-hero.svg" alt="LeadFlow animated project banner" width="100%" />
</p>

## Project goals

- Search by business category and location (for example, bakeries in Cuddalore or hotels in Pondicherry).
- Display available public business details: name, category, address, phone, website, Maps listing, rating and review count.
- Save selected prospects locally in the Android app and avoid duplicate entries.
- Search, filter, track lead status and add follow-up notes.
- Export saved leads to CSV, Excel and PDF.
- Open a business listing or WhatsApp conversation for manual follow-up. LeadFlow will not automatically send messages.

## Zero-budget principles

LeadFlow is intended to be usable without a paid subscription or paid lead database.

- Prefer free/open-source components and local Android storage.
- Do not add a paid API, hosting service or subscription as a required dependency.
- Check provider quotas, API terms and usage restrictions before integrating any external data source.
- A free tier may have limits, require an account or request a payment method; it is not automatically a permanently free service.
- If a source cannot be used reliably at zero cost, keep it optional and explain the limitation instead of silently introducing a charge.

**Data-source note:** Google Maps data collection is subject to Google's terms, access controls and applicable privacy/data-protection rules. Official Google Places API usage can incur charges and therefore must not be assumed to fit the zero-budget requirement. Before implementation, select a permitted data source that genuinely fits the budget. Do not promise unlimited Google Maps extraction or bypass anti-bot protections.

## Current status

The existing Android project provides the foundation for managing leads locally:

- Kotlin, Jetpack Compose and Room database
- Home, Add Lead, Leads, Export and Settings screens
- Local lead search and basic database operations
- CSV, XLSX and PDF export utilities
- WhatsApp click-to-open support

**Important:** the existing app is primarily a manual lead manager. Online category-and-location discovery is the next major feature; this README does not claim that it is already implemented or runtime-tested.

## Planned roadmap

1. **Stabilize the current app** — verify launch, database save/reload, validation, navigation and export flows.
2. **Build lead search** — category + location inputs, search progress, result counts, empty/error states and cancellation where supported.
3. **Connect a suitable data source** — start with a permitted zero-cost option; document limits and any setup required.
4. **Review and save results** — show available contact details, listing links and source; allow users to select and save leads.
5. **Prevent duplicates** — normalize business names and addresses and identify possible duplicates before saving.
6. **Improve prospect management** — statuses, notes, follow-up dates and category/location filters.
7. **Verify on Android** — test real workflows and exports on a device; a successful build alone does not prove that the app works correctly.

## Build the APK

1. Install Android Studio (Iguana or newer) with JDK 17.
2. Open this repository in Android Studio and wait for Gradle sync to finish. The first sync needs internet to download dependencies.
3. Select **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
4. The debug APK is normally created at `app/build/outputs/apk/debug/app-debug.apk`.

If Gradle sync fails, first check the repository's Gradle wrapper and the error message; avoid creating or replacing project files blindly.

## Install on an Android phone

- Copy `app-debug.apk` to the phone and open it using Files.
- If prompted, allow installation from that source and complete installation.
- Alternatively, connect a phone with USB debugging enabled and install with `adb install app-debug.apk`.

## Main project files

| File or folder | Purpose |
|---|---|
| `app/build.gradle` | Android dependencies and SDK configuration |
| `app/src/main/AndroidManifest.xml` | App declaration and permissions |
| `data/Lead.kt` | Lead database model |
| `data/LeadDao.kt` | Room queries and database operations |
| `data/AppDatabase.kt` | Local Room database |
| `LeadViewModel.kt` | Screen state and lead operations |
| `MainActivity.kt` | App navigation |
| `ui/HomeScreen.kt` | Home screen |
| `ui/AddLeadScreen.kt` | Manual lead entry |
| `ui/LeadsScreen.kt` | Saved leads |
| `ui/ExportScreen.kt` | Export interface |
| `ui/SettingsScreen.kt` | Preferences |
| `util/Exporter.kt` | CSV, XLSX and PDF export generation |
| `util/FileStore.kt` | Export file storage and sharing |

## Responsible use

Use lead data for legitimate business outreach. Respect provider terms, applicable privacy and marketing rules, opt-out requests, and reasonable contact frequency. Publicly visible information is not automatically unrestricted for every use.

## Contributing / development approach

Work incrementally: make one focused change, build, test the affected workflow, and document what was actually verified. Do not describe a feature as complete until it has been tested beyond compilation where practical.
