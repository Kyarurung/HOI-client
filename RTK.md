# RTK for HOI repositories

Installed tool: [rtk-ai/rtk](https://github.com/rtk-ai/rtk), Windows release 1.2.1. This is a project adaptation of its generated Codex guidance.

Use `rtk git status`, `rtk git diff` and other filters supported by `rtk --help` when compressed output is sufficient. Search source with `rg` first; use native output for exact matches, complete diffs, JSON, hashes and failure details. Do not blindly prefix PowerShell cmdlets or unsupported commands. Keep the actual process exit status when judging success. RTK filters output; it does not replace the repository's validation commands or grant permission to execute them.

Check `rtk --version` and `rtk gain` for installation and local usage. If the current shell predates the PATH update, this Windows install is at `C:/Users/Kiruy/.local/bin/rtk.exe`. Global hooks rewrite the Bash tool's commands for both agents: `rtk hook claude` in `~/.claude/settings.json` (`rtk init -g --hook-only`) and `rtk hook codex` in `~/.codex/hooks.json` (`rtk init -g --codex`; Codex runs it only after it is trusted in `/hooks`). PowerShell commands are not rewritten; use `rtk bypass <command>`, PowerShell or the file tools when exact output matters.


## Repository workflow

Read the applicable repository skill linked below before using its workflow. Sibling repositories have their own AGENTS.md and skills; inspect those before cross-repository edits. Keep unrelated working-tree changes out of task commits.

Use `rg` for targeted source searches. If RTK is installed, check `rtk --version` and `rtk --help` before using supported output filters; use native commands when exact output or failure details matter. For CodeGraph, first identify the installed implementation and its CLI/MCP schema, then verify the repository root, index freshness and Java/Python support. Confirm graph results against source. Do not invent tool commands or claim an unavailable tool is configured. Index source only; exclude generated builds, runtime data, credentials and asset archives. Tool setup does not authorize starting the operational server.

Keep durable behavior, reproduction commands and verification limits in README or the relevant feature document. Use task responses and Git commits for completion/progress/review summaries instead of creating dated report Markdown. Before deleting an old report, preserve unique operational knowledge or benchmark evidence in the maintained documentation and update its references. Keep functional references, operating preferences, licenses and asset provenance.

Repository skill: [hoi-client-development](.agents/skills/hoi-client-development/SKILL.md).


## Tools and applicable skills

Use `minecraft-modding`, `minecraft-testing` and `minecraft-resource-pack` for resource interface changes from the installed Jahrome907/minecraft-agent-skills collection when applicable. Legacy mapping, launcher, dependency, license and release examples do not override this repository's Java 25 / Minecraft 26.3 configuration or operating rules.

Apply Ponytail's existing-code/stdlib-first approach to coding while completing the user's requested scope and required validation. Use the requested i-have-adhd output preferences: state the result/action first, keep steps short and omit tangents; this is a writing preference, not a medical inference. `humanize-korean` from im-not-ai is available for Korean prose editing: preserve facts, identifiers, commands and links, and use its Codex references. Installing these skills does not authorize unrelated code audits or generating progress reports.

Read [RTK.md](RTK.md) for supported shell-output filtering. RTK hooks rewrite Bash tool commands only; do not claim PowerShell commands are rewritten. CodeGraph is colbymchenry/codegraph; its `codegraph` MCP server (`codegraph serve --mcp` in the session's working directory) is configured for Claude in the user MCP config and for Codex in `~/.codex/config.toml`, and the untracked `.codex/config.toml` points it at this repository for Codex. The server exposes one tool, `codegraph_explore`; its `projectPath` argument queries a sibling repository's index. Run `codegraph status` from this repository; use `codegraph sync` when needed, then `codegraph query <symbol>` or `codegraph explore <question>` for relationships. Follow [codegraph.json](codegraph.json) exclusions, and verify relevant source and native command failures. The `.codegraph/` index is local generated data. The operational server folder indexes reviewed scripts only, not worlds, mods or configuration secrets.
