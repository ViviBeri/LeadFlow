# Place Discovery Manual QA Checklist

This checklist covers the Android **Discover Places** flow. Run it on a physical Android device with an internet connection. Record the device model, Android version, date, and outcome for each check.

## Test setup

- Install the latest debug APK from a successful **Build APK** workflow run.
- Allow the app to reach the internet.
- Open **Discover Places** from the home screen.
- Use a real location name such as `Pondicherry, India` and a category such as `Restaurant`.
- Keep the selected category and location visible when testing the Google Maps fallback.

## Manual device checks

| ID | Steps | Expected result | Status |
|---|---|---|---|
| PD-01 | Enter category `Restaurant`, location `Pondicherry, India`, radius `5`; tap **Find Places**. | Search enters a loading state, then displays named results when the providers return matching data. Each card shows its available category/address and distance; phone and website appear only when supplied. | Not device-verified |
| PD-02 | Search a valid location/category combination that has no named matching listings, or use a deliberately narrow radius. | A clear **No named places found** message appears with guidance to broaden the category or location. | Not device-verified |
| PD-03 | Enter a location that cannot be resolved, such as a deliberately invalid place name; tap **Find Places**. | A location-specific message explains that no matching location was found. | Not device-verified |
| PD-04 | With internet disabled, or while the discovery providers are unavailable, run a search. | Search stops loading and shows the service-unavailable message with the **Search in Google Maps** fallback. The app remains usable. | Not device-verified |
| PD-05 | From the error state, tap **Search in Google Maps**. | Google Maps (or the device's app chooser/browser) opens a search for the same category and location entered in LeadFlow. | Not device-verified |
| PD-06 | Complete a successful search and tap **Open in Maps** on a result. | Google Maps opens the selected place using its coordinates. Return to LeadFlow and confirm the result list remains available. | Not device-verified |
| PD-07 | On a result card, tap **Save lead** for a business not already saved. | A success toast appears. The lead is available in the saved Leads screen with its name/address and available website; its notes identify OpenStreetMap as the source and include the map link/category. | Not device-verified |
| PD-08 | Tap **Save lead** again for the same result. | A duplicate-warning toast appears; a second copy is not added. | Not device-verified |
| PD-09 | Search successfully, then enter a term in the result filter that matches no card. | The result count becomes zero and **No matching places. Try a different filter.** appears. Clear or change the filter to restore matching cards. | Not device-verified |
| PD-10 | Tap **View data attribution**. | The OpenStreetMap copyright/attribution page opens externally. The screen visibly credits OpenStreetMap contributors and identifies the ODbL. | Not device-verified |
| PD-11 | Enter radius values below 0.5 km and above 50 km, then search. | The search uses the supported 0.5–50 km range (values are clamped by the screen logic); the UI remains responsive. | Not device-verified |
| PD-12 | Search with a broad radius/category that returns many places. Scroll through the form and results. | The complete screen scrolls as one list. If the result cap is reached, the app explains that only the first 300 places are shown. | Not device-verified |

## Automated coverage currently present

The JVM test file `app/src/test/java/com/leadflow/app/OpenPlaceSearchTest.kt` covers:

- Parsing named places while dropping unnamed entries and duplicate name/address pairs.
- Reading node coordinates and way centre coordinates.
- Computing distance from the search centre.
- Keeping the Overpass query bounded by `MAX_RESULTS`.
- Avoiding a catch-all regex for an unknown category.
- Sanitizing category text used in Overpass regex filters.

These tests do **not** exercise live Nominatim/Overpass requests, Android intents, Google Maps handoff, Compose rendering, Room persistence/toasts, or real-device scrolling. A successful CI build is not proof that these device flows work.

## Data-source and fallback boundaries

- Discovery names, categories, coordinates and available contact fields come from OpenStreetMap data through Nominatim and Overpass.
- OpenStreetMap coverage and optional tags vary by place. Missing phone, website or address details are not necessarily app errors.
- The current discovery result model does not supply Google ratings, review counts, customer review text or opening hours. Do not use these checks to claim that LeadFlow ranks places by Google reviews or ratings.
- Google Maps is an external handoff only. The fallback opens a search URL; it does **not** import Google Maps results into LeadFlow or make them saveable as discovered results.
- Public community endpoints can be rate-limited, slow or unavailable. A provider failure should be recorded as such, not described as proof that no businesses exist.

## Test record

| Field | Value |
|---|---|
| Device / model | |
| Android version | |
| APK workflow run / commit | |
| Test date | |
| Tester | |

For each PD check, replace **Not device-verified** with **Pass** or **Fail** and add a short note for any failure. Do not mark a check passed based only on unit tests or CI.
