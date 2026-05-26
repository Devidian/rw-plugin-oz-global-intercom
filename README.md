# Installation (prebuild)

Download the latest zip files from [here (OZTools)](https://github.com/Devidian/rw-plugin-oz-tools/releases) and [here (Global Intercom)](https://github.com/Devidian/rw-plugin-oz-global-intercom/releases) and unpack them into your plugins folder.

## Filetree

Should look like this:

```css
    ── RisingWorld
        ├── Plugins
        │    ├── GlobalIntercom
        │    │    ├── i18n...
        │    │    ├── HISTORY.de.md
        │    │    ├── HISTORY.md
        │    │    ├── OZGlobalIntercom.jar
        │    │    ├── README.de.md
        │    │    ├── README.md
        │    │    └── settings.properties
        │    ├── Tools
        │    │    ├── assets...
        │    │    │── lib
        │    │    │    ├── gson-2.8.5.jar
        │    │    │    ├── javax.websocket-api-1.1.jar
        │    │    │    ├── tyrus-standalone-client-1.15.jar
        │    │    │    :
        │    │    ├── HISTORY.md
        │    │    ├── README.md
        │    │    └── OZTools.jar
        :    :
```

## Contributor Workflow

- Review `AGENTS.md`, `PLANS.md`, `.codex/agents.toml`, and `.codex/skills/` before making structural changes.
- Verify Rising World API usage with `scripts/verify-plugin-api.sh` when adding or changing API calls.
- Run `mvn -B -DskipTests package` and `mvn -B test` before release-facing changes are merged.
- Use `RUNTIME_TESTING.md` and `scripts/docker-runtime-smoke.sh <PluginFolderName>` for runtime smoke tests when behavior changes need server validation.
- Keep `README.md` and `HISTORY.md` current and use Conventional Commit titles for commits and PRs.

## Admin Settings And Status

Global Intercom registers safe admin `PluginSettings` metadata through OZTools.
Admins can edit grouped logging, runtime, relay, chat color, player-message, and
screenshot settings from the shared Tools settings UI. Integer filtering for
`maxScreenWidth` is provided by OZTools.

The `/gi info` and `/gi status` commands open the shared OZTools plugin
Info/Status panel. The plugin radial menu entry opens the same panel using the
portfolio-wide Info/Status icon. The panel reports relay connection state,
player channel state, language data, and selected runtime settings.

## Relay Contract

The current WebSocket relay contract is documented in
[docs/relay-contract.md](docs/relay-contract.md). Modernization keeps existing
event names, payload shapes, and relay behavior unchanged so the Java plugin can
remain compatible while `rw-girs` relay work is prepared.
