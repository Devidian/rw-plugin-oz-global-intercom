# Roadmap Plan 03 Menu Info Button

## Objective
Add a Global Intercom radial-main-menu button that opens the existing shared Tools Info/Status panel.

## Ownership
Primary repository: `rw-plugin-oz-global-intercom`

Supporting repository:
- `rw-plugin-oz-tools` for the shared Info/Status panel contract.

## Dependencies
- Hard runtime dependency: `rw-plugin-oz-tools`.
- No WebSocket relay payload change is planned for this package.

## Phases
- [x] Phase 1: Add the Info/Status action to the plugin's main radial menu.
- [x] Phase 2: Reuse the existing Info/Status provider and command behavior.
- [x] Phase 3: Update README/HISTORY and validate.

## Risks
- Menu changes must not affect relay connection state, channel membership, or message routing.

## Validation Strategy
- Run `mvn -B -DskipTests package`.
- Run `mvn -B test`.
- Runtime-smoke the radial button and existing `/gi info` or `/gi status` behavior.

## Affected Repositories/Plugins
- `rw-plugin-oz-global-intercom`
- `rw-plugin-oz-tools`

## Rollback Considerations
The radial button can be removed without changing relay behavior.

## Progress Notes
- Phase 1 complete: Global Intercom registers a radial menu entry with the Tools-provided `info-status` icon.
- Phase 2 complete: the radial entry opens the existing shared Tools Info/Status provider; `/gi info` and `/gi status` are unchanged.
- Phase 3 complete: README/HISTORY were updated.
- Validation passed with `mvn -B test` and `mvn -B -DskipTests package`.
