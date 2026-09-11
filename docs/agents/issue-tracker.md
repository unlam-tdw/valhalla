# Issue tracker: Trello

Issues and specs for this repo live on Trello. Use the Trello MCP tools for all operations.

## Conventions

- **Read a card**: `trelloReadCard(action: "get", cardId: "...")`
- **List cards in a list**: `trelloReadCard(action: "list", boardId: "...", listId: "...")`
- **Create a card**: `trelloWriteCard(action: "create", boardId: "...", listId: "...", name: "...", desc: "...")`
- **Comment on a card**: `trelloWriteCard(action: "comment", cardId: "...", text: "...")`
- **Add/update labels**: Use Trello labels to track triage state and card status.

## Board structure

- Board: PlanIt (TDW)
- Lists follow the sprint structure defined in `docs/sprint-planner.md`
- Cards map 1:1 to the specs in `docs/specs/`

## When a skill says "publish to the issue tracker"

Create a Trello card in the appropriate list.

## When a skill says "fetch the relevant ticket"

Use Trello MCP tools to read the card by ID or search by name.

## PRs as a request surface

**No.** This project does not treat external PRs as a triage surface.

## Wayfinding operations

Used by `/wayfinder`. Adapted for Trello:

- **Map**: a single card labelled `wayfinder:map`, holding the Notes / Decisions-so-far / Fog body.
- **Child ticket**: a card linked to the map. Labels: `wayfinder:<type>` (`research`/`prototype`/`grilling`/`task`). Once claimed, the ticket is assigned to the driving dev.
- **Blocking**: Trello's due dates or custom fields, or a `Blocked by:` line in the card description.
- **Frontier**: open, unblocked, unclaimed child cards; first in map order wins.
- **Claim**: assign the card to yourself.
- **Resolve**: add a comment with the answer, then move the card to the Done list.
