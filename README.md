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
