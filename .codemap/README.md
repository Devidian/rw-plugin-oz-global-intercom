# rw-plugin-oz-global-intercom
<!-- codemap-id: rw-plugin-oz-global-intercom -->

## Purpose

Cross-server chat channels and WebSocket relay client. The [plugin descriptor](../src/resources/plugin.yml) names [GlobalIntercom](../src/de/omegazirkel/risingworld/GlobalIntercom.java) as its lifecycle entry.

## Modules

- [relay](modules/relay.md) — WebSocket connection and relay message protocol.
- [channels](modules/channels.md) — Player channel operations, commands and settings.

## Build and tests

[Maven](../pom.xml) defines the Java build. Follow [repository validation](../AGENTS.md) for packaging, API checks and runtime validation. Relevant automated tests are under [src-test](../src-test/).

## Dependencies

- `rw-plugin-oz-tools/shared-api` — build and runtime shared infrastructure. [Evidence](../pom.xml)
- `rw-girs/relay` — WebSocket relay protocol peer. [Evidence](../src/de/omegazirkel/risingworld/globalintercom/WebSocketHandler.java)
