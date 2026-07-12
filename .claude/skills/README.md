# MusicPlayer Skill Library

Task playbooks for maintaining this Kotlin Multiplatform music player without senior
supervision. Every skill was authored by reading the actual source, then independently
reviewed claim-by-claim against the code. File paths, symbol names, Gradle tasks, and
log tags in these skills are verified facts, not reconstructions — treat a mismatch
between a skill and the code as a signal the code changed, and update the skill.

Claude Code auto-discovers these skills; humans can read them directly. Each skill's
frontmatter `description` says when to reach for it.

## The skills

| Skill | Reach for it when... |
|---|---|
| [build-and-run](build-and-run/SKILL.md) | Building, installing, launching the app; watching logcat; Gradle build failures |
| [validate-changes](validate-changes/SKILL.md) | Proving a change is correct before calling it done — tests, compile checks, manual checklists |
| [add-feature](add-feature/SKILL.md) | Adding a screen/feature or platform-specific capability following the house MVVM conventions |
| [navigation](navigation/SKILL.md) | Adding routes or tabs; tab back stacks; the full-screen player overlay |
| [innertube-api](innertube-api/SKILL.md) | Search/radio/recommendations broken or empty; extending the YouTube Music API integration (see also its [reference.md](innertube-api/reference.md) for exact JSON key-paths) |
| [playback-and-queue](playback-and-queue/SKILL.md) | "Song won't play / skips / wrong song next"; modifying QueueManager, MediaService, or stream resolution |
| [room-database](room-database/SKILL.md) | Entities, DAOs, mappers, database version bumps, DataStore |
| [ui-components](ui-components/SKILL.md) | Building Compose UI — shared components, theme tokens, reorderable lists |

## Onboarding order (for a first session on this codebase)

1. `CLAUDE.md` at the repo root — the map.
2. `build-and-run` — get an APK on a device and logs flowing.
3. `validate-changes` — the definition of done here.
4. Then the skill matching your task area.

## Facts that surprise everyone (cross-cutting, all verified)

- **Stream URLs are resolved lazily** in a `ResolvingDataSource.Factory` lambda in
  `MediaService.onCreate` via NewPipe. The InnerTube `/player` path (`PlayerParser`)
  is fully implemented but **unused**.
- **ExoPlayer auto-advances** at song end; QueueManager is synced *after the fact* via
  `playNext(fromAutoAdvanced = true)` with an echo-loop guard. Repeat modes currently
  only gate `hasNext`/`hasPrevious` (notification buttons) — playback stops at queue
  end in every mode (`STATE_ENDED` handler is a commented-out TODO).
- **Search suggestions come from NewPipe; search results from InnerTube.** A broken
  dropdown and broken results have different root causes.
- **There is no network logging by default.** Repository errors surface as `println`
  (logcat tag `System.out`); `DownloaderImpl`'s logging interceptor exists but its
  `addInterceptor` line is commented out.
- **Room uses destructive migration** — any schema version bump wipes playlists,
  recently played, and the saved queue.
- **Navigator is not in Koin** — three nesting levels each `remember` their own;
  cross-level navigation is lambdas passed down. The full-screen player is a Box
  overlay, not a route.
- The search feature's navigation package is genuinely misspelled `navigtion`.

## Maintaining this library

- When you change behavior a skill documents, **update the skill in the same PR** —
  a stale skill is worse than no skill.
- Keep the style: repo-relative verified paths, verbatim snippets only, numbered
  recipes with verification checkpoints, symptom → cause → fix failure tables,
  frontmatter `description` starting "Use when...".
- If you add a skill, add a row to the table above and cross-reference related skills.
- Unverifiable claims (live-API behavior, device-only steps) must be explicitly
  marked as such in the skill text.
