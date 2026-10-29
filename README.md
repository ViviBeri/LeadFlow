# LeadFlow

**Discover the right businesses. Build a better pipeline.**

LeadFlow is an Android business lead discovery and management project designed to help sales teams, agencies, consultants and entrepreneurs find relevant business prospects using targeted search and practical filters.

<p align="center">
  <img src="https://raw.githubusercontent.com/ViviBeri/LeadFlow/main/readme-hero.svg" alt="LeadFlow project banner" width="100%" />
</p>

## What is LeadFlow?

Instead of searching for potential customers one business at a time, LeadFlow aims to bring discovery, qualification and follow-up into one streamlined workflow.

**Example:** A supplier looking for restaurant customers can search for **Restaurants** in **Pondicherry**, narrow the results by rating or contact availability, save promising businesses and export a shortlist for outreach.

LeadFlow can support many industries: marketing agencies, distributors, service providers, business consultants, contractors and local sales teams.

## Planned lead discovery

| Filter | Example | Why it matters |
|---|---|---|
| **Category** | Restaurants, hotels, bakeries, salons | Target the right industry |
| **Location** | Pondicherry, Cuddalore, Villupuram | Focus on a service territory |
| **Rating range** | 3.0–4.0, 4.0+ | Segment businesses by public rating |
| **Review count** | Fewer than 50, 50–200, 200+ | Compare visibility and activity |
| **Contact availability** | Has phone, has website | Prioritize reachable prospects |
| **Website presence** | With or without website | Identify digital-service opportunities |
| **Subcategory** | Cafés, fine dining, budget hotels | Refine broad industry searches |
| **Distance / area** | Within a chosen radius | Plan territory-based outreach |
| **Sort order** | Rating, reviews, distance, name | Organize the results |

Filter availability will depend on the selected data source and which fields it provides. A low rating does not by itself mean a business needs a particular service.

## Product vision

1. **Find businesses** — enter a category and location to discover relevant public business listings.
2. **Refine results** — apply available filters such as rating, review count, website presence and business subcategory.
3. **Evaluate prospects** — review names, addresses, contact information, listing links and other available details.
4. **Save leads** — select prospects, detect potential duplicates and organize them into a local lead collection.
5. **Manage follow-ups** — track lead status, notes and next-contact dates.
6. **Export and connect** — export selected leads as CSV, Excel or PDF and open contact channels for manual outreach.

## Current implementation

LeadFlow currently contains a native Android lead-management foundation built with:

- **Kotlin** and **Jetpack Compose** for the Android interface
- **Room** for local lead storage
- Screens for Home, Add Lead, Leads, Export and Settings
- Local search, basic lead management and export utilities
- CSV, XLSX and PDF generation
- WhatsApp click-to-open support

**Development status:** Online business discovery and advanced search filters are planned features, not yet implemented. Existing screens and exports still require end-to-end device verification.

## Development roadmap

- [ ] Verify launch, navigation, lead persistence and exports on Android
- [ ] Create the business discovery screen with category and location inputs
- [ ] Integrate an appropriate business-listing data source
- [ ] Display results with available contact details and source links
- [ ] Add rating, review-count, category and contact-availability filters
- [ ] Add sorting, pagination and useful empty/error states
- [ ] Save selected search results to Room with duplicate detection
- [ ] Add lead statuses, notes and follow-up scheduling
- [ ] Verify the full discovery-to-export workflow on a device

## Build the Android app

1. Install Android Studio with JDK 17.
2. Open the repository and allow Gradle to sync.
3. Select **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
4. Locate the debug APK, normally at `app/build/outputs/apk/debug/app-debug.apk`.

To install on an Android device, open the APK from the phone's file manager and approve installation from that source if prompted. Alternatively, use `adb install app-debug.apk` with USB debugging enabled.

## Project structure

| File | Responsibility |
|---|---|
| `MainActivity.kt` | Navigation |
| `LeadViewModel.kt` | Lead state and operations |
| `data/Lead.kt` | Lead model |
| `data/LeadDao.kt` | Database queries |
| `data/AppDatabase.kt` | Room database |
| `ui/HomeScreen.kt` | Dashboard |
| `ui/AddLeadScreen.kt` | Manual lead entry |
| `ui/LeadsScreen.kt` | Saved leads |
| `ui/ExportScreen.kt` | Export interface |
| `ui/SettingsScreen.kt` | Preferences |
| `util/Exporter.kt` | CSV, XLSX and PDF generation |
| `util/FileStore.kt` | Export storage and sharing |

Source files are under `app/src/main/java/com/leadflow/app/` unless otherwise noted.

## Data and responsible outreach

Business data availability, search limits and permitted use depend on the data provider. Integrations should respect provider terms, applicable privacy laws and marketing rules. LeadFlow is intended to help users research and manage prospects, not to send unsolicited automated messages.

## Contributing

Contributions and suggestions are welcome, particularly around Android usability, search/filter design, data-source integration, duplicate detection and reliable exports. Features should be tested and documented before being marked complete.
