# Bugs 05 WebSocket Threading (Closed 2026-07-15)

## Objective
Complete WebSocket message-boundary hardening and runtime validation after
moving all relay-driven game operations onto the server thread.

## Ownership
Owning repository/plugin: `rw-plugin-oz-global-intercom`
Supporting repositories/plugins: `rw-plugin-oz-tools`

## Dependencies
- Runtime: Rising World development server and `rw-girs` relay
- Build: OZTools `0.21.1`, Java 20, and Maven
- Optional integrations: Discord Connect

## Risks
- Relay payload changes are compatibility-sensitive and require multi-server
  validation.

## Validation Strategy
- [x] `mvn -B test`
- [x] `mvn -B -DskipTests package`
- [x] Runtime-test one game-to-relay message
- [x] Runtime-test relay messages, channel responses, screenshots, reconnect,
  reload, and shutdown
- [x] Run sustained multi-server relay activity during the native-crash soak

## Affected Repositories/Plugins
- `rw-plugin-oz-global-intercom`
- `rw-plugin-oz-tools`

## Rollback Considerations
Do not restore direct WebSocket-thread game API calls. Preserve the existing
wire contract when replacing mutable parsed entities.

## Implementation Checklist
- [x] Parse WebSocket JSON before server-thread dispatch
- [x] Dispatch relay-driven game operations
- [x] Separate transport-only sends from player-aware handling
- [x] Remove retained Player access from screenshot callbacks
- [x] Replace mutable parsed entities with immutable message values
- [x] Add focused message-boundary regression tests
- [x] Complete multi-server runtime validation
