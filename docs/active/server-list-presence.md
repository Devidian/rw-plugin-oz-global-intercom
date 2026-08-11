# Relay Server Presence

## Objective

Register every Global Intercom plugin connection with its configured short server
name so the relay can identify it in channel-presence updates.

## Ownership and compatibility

- Global Intercom owns the server registration frame and its local setting.
- The relay retains an IP fallback when a registration has no short name.
- Existing relay versions ignore the additive event, so rollout is safe in either
  order; older plugins simply do not contribute a named server entry.

## Risks and rollback

The setting is optional and falls back to the Rising World server name. Removing
the new frame restores the previous relay-only behavior without persistence work.

## Validation

- [ ] Add the optional short-name setting and send registration after connect.
- [ ] Build and deploy the affected plugin to Development.
