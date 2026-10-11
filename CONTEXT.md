# PlanIt, Domain Glossary

## Core Entities

| Term | Definition |
|------|------------|
| **User** | Registered person. Has email, password, role. Can create plans. |
| **Place** | A location in Buenos Aires. Has name, description, category, address, coordinates, image. |
| **Plan** | A curated collection of places with schedule. Owned by a User. Is public or private. |
| **PlanPlace** | A place within a plan, with visit date, time, and order in the itinerary. |
| **Plan participant** | Registered User who joins a Plan using its Short Code. Can read its detail and leave it; cannot edit or delete it unless assigned the Editor role. The administrator owns the Plan and can delete it. |

**Editor**: A Plan participant who can modify the plan and itinerary and change participant roles. Only the administrator (owner) can delete the Plan. Leaving the Plan removes the Editor role.

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

Whether a plan can be read through its shared link. A boolean on the Plan, not a pair of states.

| Term | Definition |
|------|------------|
| **Public plan** | `isPublic` is true: the plan is accessible via shared URL without login |
| **Private plan** | `isPublic` is false: the plan is visible only to its owner and its participants |

## Key Concepts

| Term | Definition |
|------|------------|
| **Short Code** | Unique URL-friendly identifier for sharing plans, 8 characters (e.g., `a1b2c3d4`) |
| **Itinerary** | Ordered list of places in a plan, with dates and times |
| **Marker** | Visual pin on the map representing a place |
| **Popup** | Info window that opens when clicking a marker |
