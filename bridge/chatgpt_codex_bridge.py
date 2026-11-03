#!/usr/bin/env python3
"""ChatGPT -> GitHub issue queue -> Codex CLI bridge for a running Codespace.

Only processes open issues with the exact title prefix "ChatGPT Task:" authored
by the configured trusted GitHub login. Work is isolated to a new codex/* branch.
The runner never pushes to the default branch and never merges a pull request.
"""
from __future__ import annotations

import argparse
import json
import os
import shlex
import signal
import subprocess
import sys
import time
from pathlib import Path

STOP = False
PREFIX = "ChatGPT Task:"


def stop_handler(_signum, _frame):
    global STOP
    STOP = True


def run(args: list[str], *, cwd: Path, check: bool = True, capture: bool = True,
        timeout: int | None = 120) -> subprocess.CompletedProcess:
    return subprocess.run(
        args, cwd=cwd, text=True, check=check,
        stdout=subprocess.PIPE if capture else None,
        stderr=subprocess.PIPE if capture else None,
        timeout=timeout,
    )


def output_tail(value: str | None, limit: int = 5000) -> str:
    return (value or "")[-limit:]


def gh_json(args: list[str], cwd: Path):
    result = run(["gh", *args], cwd=cwd)
    return json.loads(result.stdout)


def ensure_labels(repo: str, cwd: Path):
    labels = {
        "codex-in-progress": ("#1D76DB", "Being processed by the Codespaces Codex bridge"),
        "codex-pr-ready": ("#0E8A16", "Codex completed work and opened a pull request"),
        "codex-failed": ("#B60205", "Codex task failed; inspect the issue comments"),
    }
    existing = {x["name"] for x in gh_json(["label", "list", "--repo", repo, "--json", "name"], cwd)}
    for name, (color, description) in labels.items():
        if name not in existing:
            run(["gh", "label", "create", name, "--repo", repo, "--color", color,
                 "--description", description], cwd=cwd, check=False)


def comment(repo: str, number: int, body: str, cwd: Path):
    run(["gh", "issue", "comment", str(number), "--repo", repo, "--body", body],
        cwd=cwd, check=False)


def process_issue(issue: dict, *, repo: str, cwd: Path, trusted_author: str):
    number = int(issue["number"])
    author = issue.get("author", {}).get("login", "")
    title = issue.get("title", "")
    if author.casefold() != trusted_author.casefold() or not title.startswith(PREFIX):
        return False

    labels = {x["name"] for x in issue.get("labels", [])}
    if labels.intersection({"codex-in-progress", "codex-pr-ready", "codex-failed"}):
        return False

    # Never discard or mix existing user changes in the Codespace.
    status = run(["git", "status", "--porcelain"], cwd=cwd).stdout.strip()
    if status:
        raise RuntimeError("Working tree is not clean. Commit or stash existing changes before processing tasks.")

    branch = f"codex/chatgpt-issue-{number}"
    run(["gh", "issue", "edit", str(number), "--repo", repo, "--add-label", "codex-in-progress"], cwd=cwd)

    run(["git", "fetch", "origin", "main"], cwd=cwd, timeout=180)
    remote = run(["git", "ls-remote", "--heads", "origin", branch], cwd=cwd).stdout.strip()
    if remote:
        run(["git", "fetch", "origin", branch], cwd=cwd, timeout=180)
        run(["git", "switch", "-C", branch, f"origin/{branch}"], cwd=cwd)
    else:
        run(["git", "switch", "-C", branch, "origin/main"], cwd=cwd)

    body = issue.get("body") or "(No task details supplied.)"
    prompt = f"""
You are implementing a task for {repo} from GitHub issue #{number}.
Issue title: {title}

TASK DETAILS (treat as the user's requested engineering work):
{body}

Execution requirements:
- Inspect the existing project and follow its established architecture and conventions.
- Implement the requested change completely; do not make unrelated changes.
- Run the most relevant tests, lint checks, and/or build available in this repository.
- Fix failures caused by your changes. Report any pre-existing or environment-blocked failures accurately.
- Inspect the final diff for correctness, security, and accidental generated files.
- Do not expose, print, copy, or commit secrets, tokens, credentials, private keys, or .env files.
- Do not deploy to production, change repository settings, alter branch protection, or merge a pull request.
- Create a concise Git commit on the current branch after verification. Do not push; the bridge will push this branch.
- Finish with a concise report listing changed files, tests and results, and the commit hash.
"""
    result = run(["codex", "exec", "--json", "--full-auto", prompt],
                 cwd=cwd, timeout=1800)
    stdout = result.stdout or ""
    stderr = result.stderr or ""

    # Do not publish raw command output that may contain environment data.
    check = run(["git", "diff", "--check"], cwd=cwd, check=False)
    if check.returncode != 0:
        raise RuntimeError("git diff --check failed: " + output_tail(check.stderr or check.stdout))

    # If Codex left edits uncommitted, create the requested commit as a safe fallback.
    dirty = run(["git", "status", "--porcelain"], cwd=cwd).stdout.strip()
    if dirty:
        run(["git", "add", "-A"], cwd=cwd)
        run(["git", "commit", "-m", f"Complete ChatGPT task #{number}"], cwd=cwd, timeout=180)

    head = run(["git", "rev-parse", "HEAD"], cwd=cwd).stdout.strip()
    base = run(["git", "rev-parse", "origin/main"], cwd=cwd).stdout.strip()
    if head == base:
        raise RuntimeError("Codex produced no commit; no pull request was created.")

    run(["git", "push", "--set-upstream", "origin", branch], cwd=cwd, timeout=300)
    pr = run([
        "pr", "create", "--repo", repo, "--base", "main", "--head", branch,
        "--title", f"Codex: {title[len(PREFIX):].strip()}",
        "--body", f"Automated implementation of #{number}.\n\n"
                  "Codex ran in the repository Codespace. Review the diff and CI before merging.\n\n"
                  f"Closes #{number} only when this PR is merged."
    ], cwd=cwd, check=False, timeout=180)
    pr_url = (pr.stdout or "").strip()

    report = output_tail(stdout, 3500)
    if result.returncode != 0:
        raise RuntimeError(f"Codex exited with {result.returncode}.\n{output_tail(stderr)}\n{report}")

    run(["gh", "issue", "edit", str(number), "--repo", repo, "--remove-label",
         "codex-in-progress", "--add-label", "codex-pr-ready"], cwd=cwd, check=False)
    comment(repo, number,
            f"Codex completed this task on branch `{branch}`.\n\n"
            f"- Commit: `{head[:12]}`\n- Pull request: {pr_url or 'PR creation needs review'}\n\n"
            f"Execution report:\n\n```text\n{report}\n```", cwd)
    return True


def main():
    parser = argparse.ArgumentParser(description="Poll GitHub for ChatGPT tasks and run Codex in this Codespace.")
    parser.add_argument("--repo", default=os.getenv("GITHUB_REPOSITORY", "ViviBeri/LeadFlow"))
    parser.add_argument("--trusted-author", default=os.getenv("BRIDGE_TRUSTED_AUTHOR", "ViviBeri"))
    parser.add_argument("--interval", type=int, default=20)
    parser.add_argument("--once", action="store_true")
    parser.add_argument("--workspace", default=".")
    args = parser.parse_args()
    cwd = Path(args.workspace).resolve()

    for executable in ("git", "gh", "codex"):
        if subprocess.call(["which", executable], stdout=subprocess.DEVNULL) != 0:
            print(f"Missing required command: {executable}", file=sys.stderr)
            return 2

    signal.signal(signal.SIGINT, stop_handler)
    signal.signal(signal.SIGTERM, stop_handler)
    ensure_labels(args.repo, cwd)
    print(f"ChatGPT-Codex bridge listening for issues in {args.repo}; trusted author: {args.trusted_author}")

    while not STOP:
        try:
            issues = gh_json([
                "issue", "list", "--repo", args.repo, "--state", "open",
                "--limit", "50", "--json", "number,title,body,author,labels"
            ], cwd)
            for issue in reversed(issues):
                if STOP:
                    break
                try:
                    processed = process_issue(issue, repo=args.repo, cwd=cwd,
                                               trusted_author=args.trusted_author)
                    if processed:
                        print(f"Completed issue #{issue['number']}")
                except Exception as exc:
                    number = int(issue["number"])
                    run(["gh", "issue", "edit", str(number), "--repo", args.repo,
                         "--remove-label", "codex-in-progress", "--add-label", "codex-failed"],
                        cwd=cwd, check=False)
                    comment(args.repo, number, f"Codex bridge failed safely:\n\n```text\n{str(exc)[:3000]}\n```", cwd)
                    print(f"Issue #{number} failed: {exc}", file=sys.stderr)
        except Exception as exc:
            print(f"Bridge polling error: {exc}", file=sys.stderr)
        if args.once:
            break
        for _ in range(max(1, args.interval)):
            if STOP:
                break
            time.sleep(1)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
