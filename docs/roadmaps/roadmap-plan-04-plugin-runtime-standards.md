# Roadmap Plan 04 Plugin Runtime Standards

## Objective
Apply Plan 04 portfolio runtime standards to Global Intercom without changing relay protocol behavior.

## Ownership
Primary repository: `rw-plugin-oz-global-intercom`

Supporting repositories:
- `rw-plugin-oz-tools` for shared settings, i18n, persistence, WebSocket helpers, and overlay behavior.
- `rw-girs` only for compatibility smoke if relay-facing behavior is touched.

## Dependencies
- Hard runtime dependency: `rw-plugin-oz-tools`.
- Relay message contracts should remain unchanged for Plan 04 unless a runtime standards audit finds a direct issue.

## Phases
- [x] Phase 1: Audit for deprecated Tools `SQLite` usage and migrate to `SQLiteConnectionFactory` if needed.
- [x] Phase 2: Verify i18n files are loaded only once during `onEnable`.
- [x] Phase 3: Add PlayerPluginSettings shortcut visibility for `/ozt open` and inventory entry, defaulting to visible.
- [x] Phase 4: Document the Escape-close API limitation for open Global Intercom panels.
- [x] Phase 5: Verify persisted channel/player preferences use SQLite/world-safe storage where plugin-local persistence is required.
- [x] Phase 6: Update README/HISTORY and validate.

## Implementation Notes
- Global Intercom had no deprecated Tools `SQLite` usage and no plugin-owned persistent overlay to register for escape-close.
- Global Intercom now loads i18n once through `I18n.getInstance(this)` during enable.
- The player settings panel now includes a default-visible Global Intercom shortcut setting persisted through shared `PlayerSettings`.
- Relay channel/player preferences remain backend/WebSocket-owned; plugin-local persistence is limited to runtime standards player settings.

## Risks
- Runtime standard cleanup should not alter WebSocket event names, payload fields, or relay state expectations.

## Validation Strategy
- Run `mvn -B test` and `mvn -B -DskipTests package`.
- If relay behavior is touched, run the matching `rw-girs` contract tests.

## Affected Repositories/Plugins
- `rw-plugin-oz-global-intercom`
- `rw-plugin-oz-tools`
- `rw-girs` for compatibility validation only if needed.

## Rollback Considerations
Keep changes local to UI/settings/persistence standards. Do not change relay protocol as part of this plan.
