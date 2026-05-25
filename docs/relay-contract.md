# Global Intercom Relay Contract

This document records the current Java plugin WebSocket contract for `rw-girs`
relay work. Step 6 modernization does not rename WebSocket events, does not
change JSON payload shapes, and does not change relay behavior.

## Outbound Events

The plugin sends `WSMessage<T>` JSON objects with the event name in `event` and
the typed payload in `payload`.

- `playerOnline`: `PlayerOnlineMessage`
- `playerOffline`: `PlayerOfflineMessage`
- `registerPlayer`: `PlayerRegisterMessage`
- `unregisterPlayer`: `PlayerUnregisterMessage`
- `playerCreateChannel`: `PlayerCreateChannelMessage`
- `playerCloseChannel`: `PlayerCloseChannelMessage`
- `playerJoinChannel`: `PlayerJoinChannelMessage`
- `playerLeaveChannel`: `PlayerLeaveChannelMessage`
- `playerOverrideChange`: `PlayerOverrideChangeMessage`
- `broadcastMessage`: `ChatMessage`

## Inbound Events And Responses

The plugin currently handles these inbound event names:

- `broadcastMessage`: deserializes `payload` as `ChatMessage` and broadcasts to
  local players who are members of `chatChannel`.
- `directContactMessage`: reserved, no current behavior.
- `playerOnline`: deserializes `payload` as `GlobalIntercomPlayer`, stores it in
  the local player map, and may auto-join the default channel when configured.
- `playerOffline`: deserializes `payload` as `GlobalIntercomPlayer` and stores
  it in the local player map; no additional behavior currently runs.
- `playerOverrideChange`: uses `subject` as the new override state and notifies
  the player.
- `playerJoinChannel`: uses `subject` as the channel name and notifies the
  player.
- `playerLeaveChannel`: uses `subject` as the channel name and notifies the
  player.
- `playerCreateChannel`: uses `subject` as the channel name and notifies the
  player.
- `playerResponseError`: uses `errorCode` plus optional `subject` placeholders
  to show localized relay errors.
- `playerResponseSuccess`: uses `successCode` plus optional `subject`
  placeholders to show localized relay success messages.
- `playerResponseInfo`: uses `infoCode` to show localized relay information.

Unknown inbound event names are logged and otherwise ignored.

## Payload Classes

- `WSMessage<T>`: `event`, `payload`, and relay response fields `subject`,
  `infoCode`, `successCode`, `errorCode`.
- `PlayerMessage`: base payload with `playerUID` and `playerName`.
- `PlayerOnlineMessage` / `PlayerOfflineMessage`: player identity only.
- `PlayerRegisterMessage`: player identity plus `register=true`.
- `PlayerUnregisterMessage`: player identity plus `unregister=true`.
- `PlayerCreateChannelMessage`: player identity plus `channel` and optional
  `password`.
- `PlayerCloseChannelMessage`: player identity plus `channel`.
- `PlayerJoinChannelMessage`: player identity plus `channel` and optional
  `password`.
- `PlayerLeaveChannelMessage`: player identity plus `channel`.
- `PlayerOverrideChangeMessage`: player identity plus `override`.
- `ChatMessage`: `createdOn`, `chatVersion`, `chatContent`, `chatChannel`,
  `playerName`, `playerUID`, `sourceName`, `sourceIP`, `sourceVersion`, and
  optional base64 `attachment`.
- `GlobalIntercomPlayer`: relay-owned player state with `_id`, `id64`, `name`,
  `saveSettings`, `channels`, `online`, and `override`.

## Reconnect And Current-State Expectations

- The Java plugin creates one Tools `WSClientEndpoint` using `webSocketURI` on
  plugin enable.
- Player connect and singleplayer spawn send `playerOnline`; player disconnect
  sends `playerOffline`.
- The relay is expected to answer player state events with a
  `GlobalIntercomPlayer` payload so the plugin can refresh its local
  `playerMap`.
- Channel membership, save state, and override state are treated as relay-owned
  current state. The plugin renders and validates local chat behavior from the
  latest `GlobalIntercomPlayer` received for the player.
- If the relay is disconnected, outbound player actions are not queued by the
  plugin; the player receives the existing `MSG_WS_OFFLINE` message.
- Step 6 intentionally leaves reconnect behavior and state resync semantics as
  existing behavior. Runtime validation against `rw-girs` remains deferred until
  a testable relay server exists.
