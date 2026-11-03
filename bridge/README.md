# ChatGPT–Codex Codespaces Bridge

This bridge uses GitHub Issues as a task queue:

1. You tell ChatGPT what to change.
2. ChatGPT creates an issue titled `ChatGPT Task: ...` in this repository.
3. A runner that you start inside the LeadFlow Codespace polls for those issues.
4. The runner invokes Codex CLI in the Codespace, where it can read and edit the working tree and run Gradle checks.
5. Codex commits to a dedicated `codex/chatgpt-issue-N` branch. The runner pushes that branch and opens a pull request.
6. ChatGPT can inspect the issue, pull request, diff, and CI results using the connected GitHub integration.

This is an asynchronous queue, not a direct live-control channel: the Codespace must be running with the bridge process active. ChatGPT cannot start or wake a stopped Codespace through this bridge.

## Security boundaries

- Only issues authored by `ViviBeri` with the exact `ChatGPT Task:` title prefix are eligible by default.
- Each task runs on its own `codex/*` branch based on `origin/main`.
- The bridge never pushes to `main`, merges pull requests, changes repository settings, or deploys production.
- Codex runs with workspace write access because code editing is the purpose of this bridge. Treat task text as executable engineering instructions and do not put secrets in issue bodies.
- Do not add API keys, GitHub tokens, or ChatGPT credentials to source files. Use the existing GitHub CLI authentication in Codespaces and Codex's supported sign-in flow.
- Review pull requests and CI before merging.

## One-time Codespace setup

Open the `chore/chatgpt-codex-bridge` branch in GitHub Codespaces, then run:

```bash
git checkout chore/chatgpt-codex-bridge
gh auth status
```

Install Codex CLI if it is not already installed:

```npm install -g @openai/codex
codex --version
codex
```

Complete the interactive sign-in using the supported ChatGPT sign-in option. Do not paste a ChatGPT password or session cookie into the terminal or repository.

From the repository root, start the bridge:

```python3 bridge/chatgpt_codex_bridge.py --repo ViviBeri/LeadFlow
```

Keep this terminal running while you want ChatGPT tasks to execute. Use Ctrl+C to stop it.

## How ChatGPT submits work

Create a GitHub issue with:
- Title beginning exactly `ChatGPT Task:`
- A complete task description, acceptance criteria, and any constraints.
- No secrets, credentials, private customer data, or personal data.

The bridge only accepts issues authored by the configured trusted account. The default trusted account is `ViviBeri`; change it with `BRIDGE_TRUSTED_AUTHOR` only if the repository owner account differs.

## Current limitations

- The Codespace must remain active; Codespaces can stop after their configured idle timeout.
- The bridge polls every 20 seconds by default. It does not provide live streaming into this ChatGPT chat.
- The runner creates a branch and pull request; it does not merge the pull request.
- The first setup and Codex sign-in require a one-time user action in the Codespace.
- This design uses your Codex CLI sign-in. It does not require an OpenAI API key for Codex CLI when ChatGPT-plan sign-in is supported for your account.

## Troubleshooting

- `gh auth status`: verify GitHub CLI authentication.
- `codex`: verify Codex can start and is signed in.
- `python3 bridge/chatgpt_codex_bridge.py --once`: poll once for pending work.
- If a task is marked `codex-failed`, inspect the issue comment and Codespace terminal output.
