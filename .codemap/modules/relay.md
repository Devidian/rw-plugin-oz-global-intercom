# WebSocket connection and relay message protocol
<!-- codemap-id: rw-plugin-oz-global-intercom/relay -->

## Responsibility

WebSocket connection and relay message protocol. The relay and client share a compatibility-sensitive wire protocol. Check both endpoints and privacy consent when changing messages or connection behavior.

## Source and tests

- [`GlobalIntercomRuntime`](../../src/de/omegazirkel/risingworld/GlobalIntercomRuntime.java)
- [`WebSocketHandler`](../../src/de/omegazirkel/risingworld/globalintercom/WebSocketHandler.java)
- [`IncomingRelayMessage`](../../src/de/omegazirkel/risingworld/globalintercom/IncomingRelayMessage.java)
- [`WSMessage`](../../src/de/omegazirkel/risingworld/globalintercom/entities/WSMessage.java)
- [`IncomingRelayMessageTest`](../../src-test/de/omegazirkel/risingworld/globalintercom/IncomingRelayMessageTest.java)

See the [repository index](../README.md) for lifecycle, build and cross-repository dependencies.
