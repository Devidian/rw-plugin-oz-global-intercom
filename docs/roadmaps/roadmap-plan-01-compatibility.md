# Roadmap Plan 01 Compatibility

## Objective
Record that Roadmap Plan 01 introduces no direct Global Intercom feature work.

## Ownership
Primary repository: `rw-plugin-oz-global-intercom`.

Supporting repositories:
- `rw-plugin-oz-tools` for shared settings reload/admin settings tab adoption if rolled out portfolio-wide.

## Dependencies
- Hard dependency: `rw-plugin-oz-tools`.
- Existing relay/WebSocket backend assumptions remain unchanged.

## Work Packages
- [ ] Package 1: Adopt shared settings reload/admin settings tab metadata if the portfolio-wide prework is applied to all plugins.
- [ ] Package 2: Verify no Shop, Marketplace, Wallet, GPS, LandClaim, or Admin Utils work changes Intercom runtime dependency expectations.

## Risks
- None of the Roadmap Plan 01 features should alter WebSocket channel contracts.
- Portfolio-wide Tools changes still require compatibility validation.

## Validation Strategy
- Run existing Maven validation after shared settings/admin-tab adoption.
- Verify Intercom still handles settings reload and relay connection settings as before.

## Affected Repositories/Plugins
- `rw-plugin-oz-global-intercom`
- `rw-plugin-oz-tools`

## Rollback Considerations
No feature behavior changes are planned. Rollback only applies to shared settings/admin-tab adoption if implemented.

## Open Questions
- None.
- None for current Roadmap Plan 01.
