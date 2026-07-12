---
name: ui-components
description: Use when building or modifying Compose UI in this app — reuse the shared components, theme tokens, and list patterns instead of reinventing them.
---

# UI Components & Theming

All shared UI lives in `composeApp/src/commonMain/kotlin/org/example/project/ui/` (`component/` and `theme/`). Screens live under `features/<feature>/ui/`. Before writing any new composable, check the catalog below — most rows, images, and buttons already exist.

## Component catalog (`ui/component/`)

### SongItem.kt — the universal song row
Used by QueueScreen, PlaylistScreen, search results. Signature (real names):

```kotlin
fun SongItem(
    modifier: Modifier = Modifier,
    song: Song,
    state: SongItemState = SongItemState.Default,
    isEditMode: Boolean = false,
    dragHandleModifier: Modifier = Modifier,
    onMenuClicked: () -> Unit = {},
    onRemoveClicked: () -> Unit = {},
    onClick: () -> Unit
)
```

Visual states via the sealed interface `SongItemState`:
- `Default` — plain row.
- `Current(val isPlaying: Boolean)` — accent background (`appColors.accentContainer.copy(alpha = 0.3f)`), accent title color, vertical accent bar, marquee title, and an animated `EqualizerBars` overlay on the cover (bars animate only when `isPlaying`).
- `Previous` — history row, dimmed to `alpha = 0.45f`.
- `Manual` — manually queued song, rose-tinted background (`appColors.rose.copy(alpha = 0.1f)`).

`isEditMode = true` swaps the trailing `MoreVert` menu button for a `Close` remove button (`onRemoveClicked`) and shows a leading `Reorder` icon that you make draggable by passing `dragHandleModifier = Modifier.draggableHandle()` inside a `ReorderableItem` (see Lists below). The preview at the bottom of the file renders all five combinations — extend it when you add a state.

### PlaylistItem.kt
Simple playlist row: `PlaylistItem(modifier, playlist: Playlist, onClick)`. Cover thumb (`Dimens.Size.coverThumb`) + name + "Playlist • N songs" subtitle. Used by LibraryScreen.

### CoverImage.kt — ALL artwork goes through this
```kotlin
fun CoverImage(
    data: Any?,
    modifier: Modifier = Modifier,
    size: Dp = Dp.Unspecified,
    shape: Shape = MaterialTheme.shapes.small,
    icon: VectorPainter = rememberVectorPainter(Icons.Sharp.Refresh),
    onClick: (() -> Unit)? = null
)
```
Coil 3 `AsyncImage` with a `remember(data)`-cached `ImageRequest` and `.crossfade(true)`, `ContentScale.Crop`, clipped to `shape`. The same `icon` vector is used for BOTH `placeholder` and `error` (default: a Refresh icon). Callers usually pass `shape = RoundedCornerShape(Dimens.radiusM)`. Never call `AsyncImage` directly in screens.

### PlayPauseButton.kt
`PlayPauseButton(modifier, onPressed, isPlaying, isBuffering = false, willPlayWhenReady = false)`. A `FilledIconButton` sized `Dimens.Size.playButton` (64.dp) colored `appColors.iconActive`/`onAccent`. When `isBuffering`, shows a `CircularProgressIndicator` ring; if additionally `willPlayWhenReady`, a small play icon sits inside the ring ("queued to play once loaded"). Used on PlaylistScreen header and the player.

### PressableCard.kt — press-shrink animation
Not a composable; a modifier extension:
```kotlin
fun Modifier.pressableCard(onClick: () -> Unit) = composed { ... }
```
Shrinks to 0.95f over 90ms on press, springs back (`Spring.DampingRatioMediumBouncy`) on release, and only fires `onClick` if the press wasn't cancelled. It consumes tap input itself — do NOT combine with `clickable`. Used on LibraryScreen's `LikedSongBanner` and HomeScreen's `RecentlyPlayedItem` cards. Use it for any card-like tappable surface.

### SearchBar.kt
`SearchBar(modifier, query, onSuggestionPressed, onQueryChange, onVoiceSearch, isListening = false, onVoiceSearchCancel = {}, openKeyboardOnLaunch = false, onTextCleared)`. A `BasicTextField` with custom `decorationBox` (rounded `Dimens.radiusL`, `appColors.backgroundElevated` background, animated border color when focused), a state-machine trailing icon (`Stop` while listening / `Clear` when text present / `Mic` when focused+empty), and an animated "Cancel" text that slides in on focus. It syncs an internal `TextFieldValue` with the external `query` so voice transcripts move the cursor to the end. Search-specific; reuse as-is rather than forking.

## Theming — the honest rules

Theme composable: **`BudgetTheme`** in `ui/theme/Theme.kt` (name is a template leftover; it IS the app theme, applied in `App.kt`).

- **Colors**: the primary system is the custom `AppColors` data class (`ui/theme/AppColor.kt`) with `DarkAppColors` / `LightAppColors` palettes (violet accent + rose tertiary), exposed via `val appColors: AppColors @Composable get() = LocalAppColors.current`. `BudgetTheme` also maps these into a Material3 `ColorScheme`, so `MaterialTheme.colorScheme.*` works too — but screens overwhelmingly use `appColors.*` directly (`appColors.textPrimary`, `appColors.accentPrimary`, `appColors.divider`, ...). **New code: use `appColors`.**
- **Dimensions**: `ui/theme/Dimens.kt` — spacing (`spaceXxs`..`space3xl`, 4pt grid), radii (`radiusS`..`radius3xl`), icons (`iconXs`..`iconXl`), strokes, elevations, and `Dimens.Size.*` for fixed component sizes (`coverThumb`, `playButton`, `miniPlayerHeight`, ...). **Verified honest verdict: the codebase actually follows this** — a repo-wide search for raw `N.dp` in commonMain finds hits ONLY inside `ui/theme/` itself. Do not hardcode dp in new code; if no token fits, add one to `Dimens` with a comment.
- **Typography**: screens use `MaterialTheme.typography.*` (bodySmall/bodyMedium/titleLarge/headlineLarge/labelSmall...). CAVEAT: `ui/theme/Type.kt` defines a custom `Typography` and `ui/theme/Shape.kt` defines custom `Shapes`, but `BudgetTheme` does **not** pass either into `MaterialTheme(...)` — so you're getting Material defaults. Both files are effectively dead until someone wires them in. Don't assume Type.kt values apply.
- Small honest exceptions exist: raw `sp` (`letterSpacing = 0.sp` in LibraryScreen), `Color.Transparent`, and `Color.Black.copy(alpha = 0.45f)` (SongItem overlay scrim). Transparent/scrim colors are fine; anything with hue belongs in `AppColors`.

## Lists

- Plain lists: `LazyColumn` with stable `key`s — `items(state.libraryItems, key = { it.stableKey() })` (LibraryScreen), `itemsIndexed(state.playlist.songs, key = { _, item -> item.id })` (PlaylistScreen), `key = { "${it.contentType}:${it.contentId}" }` (HomeScreen LazyRow).
- Screens use `Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0), topBar = {...})` — insets are handled by the dashboard scaffold, so feature screens zero them out.
- **Drag-to-reorder** (`sh.calvin.reorderable` 3.0.0) is used only in `features/musicPlayer/ui/QueueScreen.kt`. The exact pattern:

```kotlin
val listState = rememberLazyListState()
val reorderableState = rememberReorderableLazyListState(listState) { from, to ->
    viewModel.onMove(from.key as String, to.key as String)  // keys are song.uniqueId
}
LaunchedEffect(reorderableState.isAnyItemDragging) {
    if (!reorderableState.isAnyItemDragging) viewModel.onDragEnd()  // commit once on drop
}
```
Then inside `LazyColumn(state = listState)`, each row is wrapped:
```kotlin
items(displayQueue.upcoming, key = { it.uniqueId }) { song ->
    ReorderableItem(reorderableState, key = song.uniqueId) {
        SongItem(
            modifier = Modifier.animateItem(),
            song = song,
            isEditMode = uiState.isEditingQueue,
            dragHandleModifier = Modifier.draggableHandle(),
            ...
        )
    }
}
```
`Modifier.draggableHandle()` is only available inside the `ReorderableItem` scope. The move callback fires per position change; the actual persistence happens in `onDragEnd()`. PlaylistScreen does NOT currently use reorderable (its `reorderSongs` repository method exists, but the screen renders a plain `itemsIndexed`).

## Previews

`ui/theme/AppPreview.kt` provides:
- `AppPreview(darkTheme = isSystemInDarkTheme()) { content }` — wraps content in `BudgetTheme` + a full-size `Surface(color = appColors.backgroundPrimary)`. Always wrap preview content in this, never bare.
- `@DevicePreviews` — a multipreview annotation emitting "Phone Dark" (`UI_MODE_NIGHT_YES`) and "Phone Light" (`UI_MODE_NIGHT_NO`), both `showSystemUi = true`.

Convention (see SongItemPreview, LibraryPreview): a `private` composable at the bottom of the component/screen file, annotated `@DevicePreviews` (or plain `@Preview` from `androidx.compose.ui.tooling.preview` for single-variant), body = `AppPreview { ... }` with hand-built mock models. Previews render via Android Studio since Android is the only active target (`compose.uiTooling` is a `debugImplementation`).

## Recipe: add a new list row component

1. Create `ui/component/MyItem.kt`, package `org.example.project.ui.component`.
2. Follow SongItem's shape: `@Composable fun MyItem(modifier: Modifier = Modifier, <model>, <state/flags>, on...: () -> Unit)` — model object in, lambdas out, `modifier` first with default. If the row has visual variants, model them as a `sealed interface` like `SongItemState`, not booleans.
3. Layout skeleton: `Row(modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = Dimens.spaceS).padding(start = Dimens.spaceM), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.spaceM))`; artwork via `CoverImage(data = ..., modifier = Modifier.size(Dimens.Size.coverThumb) /* wrap in Box like SongItem if overlaying */, shape = RoundedCornerShape(Dimens.radiusM))`; texts in `Column(Modifier.weight(1f))` with `maxLines = 1, overflow = TextOverflow.Ellipsis`, title `fontWeight = FontWeight.SemiBold, color = appColors.textPrimary`, subtitle `style = MaterialTheme.typography.bodySmall, color = appColors.textMuted`.
4. Only `appColors.*` and `Dimens.*` tokens — no raw dp/hex.
5. Add a `@DevicePreviews private fun MyItemPreview()` wrapped in `AppPreview`, showing every variant (copy SongItemPreview's structure).
6. Verify: preview renders in both light/dark; then use it from a screen and `./gradlew :composeApp:assembleDebug`.

## Recipe: make a card pressable

1. Apply `.pressableCard(onClick = { ... })` on the card's root modifier — replaces `clickable`, don't stack both.
2. Order matters: clip/size first, then `pressableCard`, e.g. HomeScreen: `Modifier.width(Dimens.Size.coverCardWidth).clip(RoundedCornerShape(Dimens.radiusM)).pressableCard(onClick = onClick)`; or on a `Surface` before padding like LibraryScreen's `LikedSongBanner`: `Modifier.fillMaxWidth().pressableCard(onClick = onClick).padding(Dimens.spaceL).height(Dimens.Size.heroCardHeight)`.
3. Verify on device: card shrinks on press, bounces back, and dragging away cancels the click.

## Common failures

| Symptom | Cause | Fix |
|---|---|---|
| `appColors` unresolved / crashes outside composition | It's a `@Composable` getter reading a CompositionLocal | Only read `appColors` inside composables; import `org.example.project.ui.theme.appColors` |
| Preview renders with wrong (default Material) colors | Content not wrapped in `AppPreview`/`BudgetTheme` | Wrap preview body in `AppPreview { ... }` |
| Custom font/shape "not applying" | `Type.kt` `Typography` and `Shape.kt` `Shapes` are never passed to `MaterialTheme` in `Theme.kt` | Expected today; wire them into `MaterialTheme(colorScheme, typography = Typography, shapes = Shapes, ...)` if you actually need them |
| `draggableHandle()` unresolved | Called outside `ReorderableItem` scope | Wrap the row in `ReorderableItem(reorderableState, key = ...)` and build the handle modifier inside its lambda |
| Reorder callback types crash (`ClassCastException`) | `from.key as String` in `rememberReorderableLazyListState` requires every reorderable item AND sticky header to have a String key | Give all items stable String keys (QueueScreen uses `song.uniqueId`, headers like `"header_manual"`) |
| Card click doesn't fire when the press drags away, or ripple missing | `pressableCard` uses `detectTapGestures` (no ripple, click gated on `tryAwaitRelease()` — a cancelled press never clicks) | That's by design; if you need a ripple use plain `clickable` instead |
| Image shows a Refresh icon instead of artwork | That's `CoverImage`'s shared `placeholder`/`error` painter (loading OR failed) | Check the URL (`thumbnailUrl` may be null); pass a custom `icon` if the fallback should differ |
| List jumps/animations broken on reorder or refresh | Missing/unstable `key` in `items(...)` | Always pass stable keys as the existing screens do |
