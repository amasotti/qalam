# Nix dev shell & CI

Nix is a package manager that builds tools in isolation and pins exact versions via a lock file (`flake.lock`).
`flake.nix` at the repo root defines a **devShell** — a reproducible shell containing every system-level tool this project needs.

## Local usage

```bash
# One-time: add to ~/.zshrc
eval "$(direnv hook zsh)"

# One-time per checkout: trust the .envrc
direnv allow          # activates automatically on cd from now on

# Or manually, without direnv
nix develop
```

## Troubleshooting direnv

| Command             | Purpose                                                    |
|---------------------|------------------------------------------------------------|
| `direnv allow`      | Trust `.envrc` for the first time (or after edits)         |
| `direnv reload`     | Re-evaluate `.envrc` and refresh exported env vars         |
| `direnv status`     | Show which `.envrc` is active and what vars it exports     |
| `direnv deny`       | Stop direnv from auto-loading in this directory            |
| `echo $VIRTUAL_ENV` | Check if a stale venv path is leaking into the shell       |
| `deactivate`        | Clear a manually-activated venv from the current session   |
| `unset VIRTUAL_ENV` | Force-clear `VIRTUAL_ENV` if `deactivate` is not available |

```bash
deactivate        # or: unset VIRTUAL_ENV
direnv reload
```

## How CI uses it

Both GitHub Actions workflows (`.github/workflows/ci.yml` and `release.yml`) install Nix via
`DeterminateSystems/nix-installer-action`, then wrap every command with `nix develop --command`.
