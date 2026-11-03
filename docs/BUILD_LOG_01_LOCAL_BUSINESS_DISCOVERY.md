# Build Log 01: Turning Local Business Discovery into an Android Lead Workflow

**Project:** [LeadFlow](https://github.com/ViviBeri/LeadFlow)  
**Platform:** Android  
**Stack:** Kotlin, Jetpack Compose, Room, Kotlin Coroutines, OpenStreetMap  
**Status:** Active development

## The problem

Finding a business is only the first step in local prospecting. A useful workflow also needs a way to save a prospect, avoid duplicate records, remember conversations, schedule follow-ups, and understand the state of a sales pipeline.

LeadFlow is being built around that complete workflow rather than treating place search as the entire product.

## Why a native Android application?

LeadFlow uses Kotlin and Jetpack Compose for its Android interface. Room provides local persistence for saved leads, while Kotlin coroutines keep network-backed discovery work off the main UI thread.

This gives the project a practical foundation for a mobile-first tool: discovery can retrieve public place listings, while lead notes, statuses, tags, priorities, and interaction history remain part of the local lead-management workflow.

## Why OpenStreetMap for the current discovery implementation?

The current place-discovery flow uses:

- **Nominatim** for location lookup.
- **Overpass API** for nearby place listings.
- **OpenStreetMap** data and attribution for the discovery experience.

This is an explicit implementation choice, not a claim that every desired business-data field is available. The current result model does not provide Google ratings, review counts, customer review text, or verified business rankings. Those features require a suitable permitted data source and must not be simulated.

The project also needs to respect the usage policies and availability limits of community geocoding and map-data services. A production deployment should review request pacing and whether a dedicated provider is more appropriate.

## From discovery to a usable lead record

The current Android implementation includes a number of workflow details beyond searching:

1. Search by category, typed location, and configurable radius.
2. Filter fetched results by name, address, or category.
3. Sort results by name or category.
4. Open a result in Google Maps.
5. Save a discovered place into the local lead database.
6. Check for an existing lead using normalized business name and address before saving.
7. Track pipeline status, priority, tags, notes, and follow-up dates.
8. Record timestamped calls, WhatsApp conversations, meetings, and email interactions.
9. Review dashboard summaries for pipeline counts, conversion rate, and follow-ups.
10. Export lead information to CSV, XLSX, and PDF.

These are documented implementation capabilities. They should still be validated on a physical Android device before being presented as verified end-to-end behavior.

## Engineering trade-offs and boundaries

A few deliberate boundaries matter:

- A public place listing is not proof that a business is a qualified sales prospect.
- Ratings and review counts are not currently part of the OpenStreetMap result model used by this implementation.
- Dish-level insights depend on review text from a source that explicitly permits access and analysis.
- Duplicate detection uses business name and address; it is a practical safeguard, not a universal identity-resolution system.
- A successful CI build verifies compilation and packaging, not every network interaction on a physical phone.

These limitations are part of the product design discussion, not details to hide in marketing copy.

## What comes next

- Verify search, saving, duplicate handling, and follow-up flows on a physical Android device.
- Improve error handling, request pacing, and resilient data-source behavior.
- Add pagination or result limits for large searches.
- Evaluate a permitted source for ratings and review counts.
- Test discovery, lead management, and export workflows end-to-end.

## Follow the work

- [Source code](https://github.com/ViviBeri/LeadFlow)
- [Open issues and planned work](https://github.com/ViviBeri/LeadFlow/issues)
- [CI workflow](https://github.com/ViviBeri/LeadFlow/actions)

LeadFlow is being developed incrementally: ship useful workflow foundations, state data limitations clearly, and verify behavior before making stronger claims.
