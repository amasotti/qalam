# Backlog — Post-MVP

Features deferred until qalam is in daily use with real data migrated from an-na7wi.
See `ROADMAP.md` for the full picture and ordering rationale.

## Deferred milestones

- **M8 Audio attachments** — no audio in an-na7wi, zero existing data, purely additive
- **M9 Training / SRS backend** — can study from texts without flashcards; build after data is in
- **M11 Global search + analytics** — vocabulary search (M4) covers immediate needs
- **M12 Backend hardening** (all except M12.4 backup which is MVP) — cycle guard, request validation, CI release
- **M17 Frontend: Training** — blocked on M9
- **M18 Frontend: Analytics** — blocked on M11
- **M19 Migration Phase 2** — sentences, annotations, training sessions (Phase 1: roots/words/texts is MVP)
- **M20 E2E tests** — post-MVP polish

## Infrastructure / ops backlog

- Monitoring stack (Prometheus + Grafana + cAdvisor + postgres-exporter via Docker Compose profiles)
- Backblaze B2 / S3 sync for backups via rclone
- `github-release.yml` with git-cliff changelog
- Mobile-optimised flashcard view
