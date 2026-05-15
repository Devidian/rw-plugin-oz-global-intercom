# AGENTS.md

## Repository Purpose
This repository owns cross-server chat relay and multi-channel intercom workflows for Rising World Unity.

It must remain usable standalone. Workspace-root orchestration is optional and must never be required for build, release, or local agent operation.

## Ownership
Owns:
- WebSocket-based global relay client behavior
- player channel join, leave, create, close, and default-channel workflows
- intercom-specific settings, commands, persistence, and message routing

Does not own:
- generic shared helpers that belong in `rw-plugin-oz-tools`
- standalone relay server implementation outside this plugin repository
- Discord bridge, land claim, GPS, or admin utility domain logic

## Mandatory Workflow Rules
- Preserve the Java 20 baseline.
- Preserve Maven build and GitHub tag-release behavior.
- Keep dependencies minimal and runtime-safe.
- Use `rw-plugin-oz-tools` for reusable infrastructure.
- Treat WebSocket payload changes as compatibility-sensitive contracts.
- Follow `.codex/agents.toml` for local agent roles, task classes, context loading, and escalation.
- Follow `docs/policies/repository-policy.md` for reusable governance rules.
- Keep `README.md`, `HISTORY.md`, and `PLANS.md` aligned with behavior or structure changes.

## Validation
- Run `mvn -B -DskipTests package` for build-impacting changes.
- Run `mvn -B test` when tests exist.
- Verify new Rising World API usage before relying on it.
- Review WebSocket contract and channel persistence impact for user-visible changes.
