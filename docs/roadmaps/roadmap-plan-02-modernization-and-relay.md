# Roadmap Plan 02 Modernization And Relay

## Objective
Bring Global Intercom up to current plugin standards and prepare it for a new lightweight relay backend owned by `rw-girs`.

## Ownership
Primary repository: `rw-plugin-oz-global-intercom`.

Supporting repositories:
- `rw-plugin-oz-tools` provides shared settings, logging, UI, WebSocket helpers, and info/status panel support.
- `rw-girs` owns the new relay server.

## Dependencies
- Tools remains a hard runtime dependency.
- Relay contract changes must be coordinated with `rw-girs`.

## Work Packages
- [x] Package 1: Audit Global Intercom against current plugin standards used by Tools, Wallet, GPS, Shop, and Marketplace.
- [x] Package 2: Update logger usage to one main `OZ.GlobalIntercom` logger.
- [x] Package 3: Complete admin `PluginSettings` metadata coverage, grouped settings labels, numeric input behavior, and i18n labels.
- [x] Package 4: Add Global Intercom info/status panel content and redirect existing info/status commands to the shared Tools panel.
- [x] Package 5: Review current WebSocket client contract and document required relay messages, channel state, player state, and reconnect behavior.
- [x] Package 6: Coordinate compatibility with `rw-girs` and avoid backend-specific business logic leaking into Tools.
- [ ] Package 7: Validate runtime behavior against the new relay server once `rw-girs` has a testable implementation.

## Step 6 Notes
- Current standards gaps addressed: grouped admin settings metadata, full safe settings coverage, i18n labels/descriptions, shared Tools info/status provider registration, and `/gi info` plus `/gi status` redirection to the shared panel.
- Logger usage already used the single main `OZ.GlobalIntercom` logger path; no local logger wrapper with independent behavior was removed.
- Relay-contract constraints are documented in [../relay-contract.md](../relay-contract.md). Step 6 does not change WebSocket event names, payload classes, JSON field names, reconnect behavior, or relay state ownership.
- `rw-girs` compatibility remains contract-first only. Runtime validation is deferred to Package 7 after the relay server has a testable implementation.
- No `rw-plugin-maven-template` update is needed because this step modernizes one existing plugin and introduces no new cross-plugin structural rule.

## Risks
- Updating plugin standards and relay behavior in the same step can hide protocol regressions.
- WebSocket contract changes can break multi-server chat if old and new relay versions are mixed.

## Validation Strategy
- Run Maven package and tests after plugin standardization.
- Verify connection, channel join/leave, channel preference persistence, and message relay against the selected relay server.
- Test disconnect/reconnect and server restart behavior.

## Affected Repositories/Plugins
- `rw-plugin-oz-global-intercom`
- `rw-plugin-oz-tools`
- `rw-girs`

## Rollback Considerations
Keep the existing relay connection behavior until the `rw-girs` implementation is validated.
