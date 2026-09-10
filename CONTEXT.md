# PlanIt — Domain Glossary

## Core Entities

| Term | Definition |
|------|------------|
| **User** | Registered person. Has email, password, role. Can create plans. |
| **Place** | A location in Buenos Aires. Has name, description, category, address, coordinates, image. |
| **Plan** | A curated collection of places with schedule. Owned by a User. Can be PUBLIC or PRIVATE. |
| **PlanPlace** | A place within a plan, with visit date, time, and order in the itinerary. |

## Categories

| Term | Definition |
|------|------------|
| **RESTAURANT** | Dining establishment |
| **BAR** | Bar or pub |
| **CAFE** | Coffee shop |
| **MUSEUM** | Museum or gallery |
| **PARK** | Green space or plaza |
| **SHOPPING** | Shopping center or market |
| **NIGHTLIFE** | Club or late-night venue |
| **CULTURE** | Theater, cinema, cultural center |
| **SPORT** | Sports venue or activity |
| **OTHER** | Uncategorized |

## Visibility

| Term | Definition |
|------|------------|
| **PUBLIC** | Plan accessible via shared URL without login |
| **PRIVATE** | Plan visible only to its owner |

## Key Concepts

| Term | Definition |
|------|------------|
| **Short Code** | Unique URL-friendly identifier for sharing plans (e.g., `a1b2c3`) |
| **Itinerary** | Ordered list of places in a plan, with dates and times |
| **Marker** | Visual pin on the map representing a place |
| **Cluster** | Group of nearby markers shown as a single count |
| **Popup** | Info window that opens when clicking a marker |
