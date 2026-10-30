# LeadFlow

**Find the right places. Discover the right prospects. Make better choices.**

LeadFlow is a business discovery and local exploration concept for Android. It is designed to help people discover relevant businesses using location, category, public ratings and review information — whether they are finding prospects for work or choosing somewhere to eat or stay.

<p align="center">
  <img src="https://raw.githubusercontent.com/ViviBeri/LeadFlow/main/readme-hero.svg" alt="LeadFlow project banner" width="100%" />
</p>

## One platform, two ways to discover

### LeadFlow for Business — find relevant leads

Help sales teams, agencies, consultants, suppliers and local service providers find businesses that match their target market.

**Example:** Search for **Restaurants** in **Pondicherry**, filter by rating, review count or website availability, sort the results and save promising prospects for follow-up.

Planned filters include:

| Filter | Examples | Purpose |
|---|---|---|
| Business category | Restaurants, hotels, bakeries, salons | Target the right industry |
| Location | Pondicherry, Cuddalore, Villupuram | Focus on a city or service area |
| Rating | 3.5+, 4.0+, 4.5+ | Narrow results by public rating |
| Review count | 10+, 50+, 200+ | Compare the amount of available feedback |
| Website | With / without website | Find businesses with different online-presence needs |
| Phone availability | Has a listed phone number | Prioritize prospects with public contact details |
| Subcategory / keywords | Café, family restaurant, boutique hotel | Refine the search |
| Distance / radius | Nearby, within a selected radius | Focus on a local area |
| Sort order | Rating, review count, distance, relevance | Put useful matches first |

LeadFlow can help users identify prospects, but ratings alone do not prove a business needs a particular service. All filters will depend on the fields available from the selected data source.

### LeadFlow Explore — find places to visit

A tourist or local resident can search for **restaurants, cafés, hotels and other places nearby**, filter by rating or review count, and see the strongest matches in a clear ranked list.

Planned capabilities:
- Search by current/selected location, category and radius
- Filter by minimum rating and review count
- Sort by rating, review volume, distance or relevance
- Show available address, hours, phone, website and map/listing link
- Make the reason for a recommendation clear, such as rating threshold or proximity
- Link users to the original listing so they can check current details before visiting

A high rating is only one signal, not a guarantee of quality. Where possible, ranking should account for both rating and review volume rather than treating a 5.0 rating from very few reviews as automatically better than a well-reviewed alternative. Results should identify the source and when information was last retrieved when that information is available.

### LeadFlow Dish Insights — discover dishes customers actually mention

This planned companion feature focuses on a different question:

**“What dish should I try at this restaurant?”**

Rather than promoting paid placements or repeating influencer recommendations, Dish Insights aims to surface dishes that real customers explicitly mention in public reviews of a restaurant or hotel.

Planned approach:
1. Retrieve review information only through a permitted data source.
2. Prioritize the newest available reviews and show the review date when supplied.
3. Identify specific dish names mentioned by reviewers.
4. Show a concise explanation based on the review, with a link or reference to the original review/listing where permitted.
5. Distinguish positive, mixed and negative customer comments; do not label a dish “best” simply because it appears once.
6. Indicate how many recent reviews mention a dish and how recent that evidence is, when enough data is available.
7. Clearly label cases where evidence is limited or no dish-specific feedback was found.

**Authenticity principle:** Dish Insights is intended to be based on genuine customer experiences, not paid promotion by restaurants, owners or influencers. The product should not sell ranking placement or present sponsored content as organic customer feedback. It should also avoid claiming that a dish is a proven customer favorite unless the available review evidence supports that conclusion.

Review access, display, quotation and analysis must follow the data provider's terms and applicable rules. If a provider does not permit access to review text or dish-level analysis, the feature must use another permitted source or clearly explain the limitation rather than bypassing access controls. Google Maps/Places data availability and API terms must be checked before implementation; not all reviews or review history are necessarily available.

## Typical workflows

**Business lead discovery**
1. Choose a category and location.
2. Apply filters such as rating, review count, website and phone availability.
3. Review and sort matching business listings.
4. Save relevant prospects and detect possible duplicates.
5. Track statuses, notes and follow-up dates.
6. Export a shortlist to CSV, Excel or PDF and contact businesses manually.

**Tourist and local discovery**
1. Select a place category and location or nearby search.
2. Set a rating threshold, review-count preference and radius.
3. Compare ranked results and open the original listing for details.

**Dish discovery**
1. Select a restaurant.
2. Review dish names found in the newest available customer reviews.
3. Compare the supporting feedback and dates where available.
4. Open the source listing/review to check context before deciding what to order.

## Current Android implementation

LeadFlow now includes an initial working place-discovery flow alongside its local lead-management foundation.

**Built with:**
- **Kotlin** and **Jetpack Compose** for the native Android interface
- **Room** for local lead storage
- **OpenStreetMap Nominatim** for location lookup
- **OpenStreetMap Overpass API** for nearby place listings
- Kotlin coroutines for background network requests

**Implemented in the app:**
- Home, Discover Places, Add Lead, Leads, Export and Settings screens
- Search places by category, typed location and configurable radius (500–25,000 metres)
- Initial category coverage for restaurants, hotels/guest houses, cafés, bakeries, bars, pharmacies, supermarkets, salons and hospitals
- Filter fetched results by name, address or category
- Sort results by name or category
- View available address, phone number and website details
- Open a discovered place in Google Maps
- Save a discovered place into the local lead database
- Check for an existing lead with the same normalized business name and address before saving
- Show save feedback for new and duplicate leads
- Display OpenStreetMap attribution and a link to its copyright page
- Local lead search and management with pipeline statuses (New, Contacted, Interested, Converted, Not Interested)
- Set, change or clear a follow-up date for each saved lead
- Edit and review notes on saved leads
- Add comma-separated tags such as GST, Restaurant or High Priority; search also matches tags
- Follow-up dashboard summaries for overdue, due-today and upcoming active leads, with tap-to-filter cards
- Existing export utilities and WhatsApp click-to-open support

**Data-source limitations:** The current discovery implementation uses OpenStreetMap listings. It does not provide Google ratings, review counts, customer review text, opening hours in the current result model, or verified business rankings. The result filter searches already-fetched listings; it is not a Google review/rating filter. Nominatim and Overpass are community services with usage policies and availability limits, so requests should be paced and a production deployment should assess service policies and a suitable hosted data provider. A successful CI build confirms compilation and APK packaging, not end-to-end network behavior on a physical phone.

## Development roadmap

- [x] Build the initial category-and-location place discovery screen
- [x] Integrate OpenStreetMap geocoding and nearby place lookup
- [x] Add result filtering by name, address and category
- [x] Add sorting by name and category
- [x] Save discovered places to the local lead database
- [x] Prevent duplicate discovered leads by business name and address
- [x] Add OpenStreetMap attribution in the discovery interface
- [ ] Verify search behavior and lead saving on a physical Android device
- [ ] Improve error handling, request pacing and resilient data-source behavior
- [ ] Add pagination or result limits for large searches
- [x] Add lead pipeline statuses and status updates
- [x] Add per-lead follow-up date management with database migration
- [x] Add overdue, due-today and upcoming follow-up dashboard summaries with selectable filters
- [x] Add editable notes to saved leads
- [x] Add editable lead tags and include them in lead search
- [ ] Add note history and richer tag management
- [ ] Integrate a permitted data source that supplies ratings and review counts
- [ ] Add transparent rating/review-count-aware ranking when reliable fields are available
- [ ] Explore review-source permissions and feasibility for Dish Insights
- [ ] Prototype dish-name extraction only from permitted, available review data
- [ ] Show recent review dates, supporting evidence and uncertainty
- [ ] Test discovery, ranking and export workflows end-to-end on Android

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
| `LeadViewModel.kt` | Lead state, operations and duplicate checks |
| `data/Lead.kt` | Lead model |
| `data/OpenPlaceSearch.kt` | OpenStreetMap geocoding and nearby place search |
| `ui/DiscoveryScreen.kt` | Category/location discovery, result filtering and sorting |
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

## Responsible data use

LeadFlow is intended for useful discovery and informed decisions, not automated unsolicited messaging or deceptive rankings. Data availability, review access and permitted use depend on each provider. Integrations must respect source terms, privacy requirements and applicable law. Recommendations should be transparent about their criteria, and customer reviews should not be fabricated, manipulated or misrepresented.

## Contributing

Suggestions and contributions are welcome, especially around Android usability, search/filter design, fair ranking, permitted data-source integration, review analysis, duplicate detection and reliable exports. Features should be tested and documented before being marked complete.
