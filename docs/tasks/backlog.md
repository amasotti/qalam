# Backlog — Post-MVP

Features deferred until qalam is in daily use with real data.
See `ROADMAP.md` for the full picture and ordering rationale.

## Deferred milestones

- **M8 Audio attachments** — zero existing audio data, purely additive
- ~~**M9 Training / SRS backend**~~ — **DONE** (backend: sessions, mastery promotion, SRS logic)
- **M11 Global search + analytics** — vocabulary search (M4) covers immediate needs
- **M12 Backend hardening** (all except M12.4 backup which is MVP) — cycle guard, request validation, CI release
- ~~**M17 Frontend: Training**~~ — **DONE** (store, FlashCard, SessionSummary, setup + session pages, nav link)
- **M18 Frontend: Analytics** — blocked on M11
- **M19 Migration Phase 2** — sentences, annotations, training sessions (Phase 1: roots/words/texts is MVP)
- **M20 E2E tests** — post-MVP polish

## Deferred from M16 (texts)

- **Annotations** (full feature) — click-to-annotate on Arabic text, annotation panel, annotation CRUD, vocabulary links from annotations, SRS mastery per annotation. Backend API exists (`/texts/{id}/annotations`), types generated, but zero frontend built. Session needed to design UX carefully before implementing.
- **Plain text view** — 3-column Arabic / transliteration / translation layout. Removed from M16 scope; interlinear-only is the target philosophy. Reconsider only if a reading-flow use case emerges.

## Infrastructure / ops backlog

- Monitoring stack (Prometheus + Grafana + cAdvisor + postgres-exporter via Docker Compose profiles)
- Backblaze B2 / S3 sync for backups via rclone
- `github-release.yml` with git-cliff changelog
- Mobile-optimised flashcard view
