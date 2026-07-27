---
name: ui-components
description: Use when building or modifying Compose UI in this app — reuse the shared components, theme tokens, and list patterns instead of reinventing them.
---

# UI Components & Theming

All shared UI lives in `composeApp/src/commonMain/kotlin/org/example/project/ui/` (`component/` and `theme/`). Screens live under `features/<feature>/ui/`. Before writing any new composable, check the catalog below — most rows, images, and buttons already exist.

## Component catalog (`ui/component/`)

### SongItem.kt — the universal song row
Used by QueueScreen, search results, and (via `SongCollectionScaffold`) PlaylistScreen + LikedSongsScreen. Signature (real names):

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

### SongCollectionScaffold.kt — the shell for "one collection of songs" screens
Used by `PlaylistScreen` and `LikedSongsScreen`. Takes plain `List<Song>` — both screens' rows are just songs:

```kotlin
fun SongCollectionScaffold(
    title: String,
    songs: List<Song>,
    onBackPressed: () -> Unit,
    onSongClicked: (song: Song, index: Int) -> Unit,
    onSongMenuClicked: (Song) -> Unit,
    isLoading: Boolean = false,
    emptyMessage: String = "No songs",
    currentlyPlayingSongId: String? = null,
    isContextActive: Boolean = false,
    isPlaying: Boolean = false,
    topBarActions: @Composable RowScope.() -> Unit = {},
    header: (@Composable () -> Unit)? = null
)
```

It owns the `Scaffold` + back-arrow top bar (title `titleLarge`, single-line ellipsized, `topBarActions` trailing), the loading/empty/list switch, and the `SongItem` rows — including the currently-playing highlight, computed once as `if (currentlyPlayingSongId == song.uniqueId && isContextActive) SongItemState.Current(isPlaying) else Default`.

Rows are keyed on `Song.uniqueId`, which both callers make unique per row: playlist songs carry their `playlist_songs` row ID (set in `PlaylistMapper`), liked songs their URL. That is why PlaylistScreen needs no wrapper type — it passes `song.uniqueId` straight through as `playlistSongId` for `RemoveFromPlaylist`. **Anything else feeding this scaffold must guarantee unique `uniqueId`s**, or the LazyColumn keys collide.

`header` renders as the first scrolling item followed by a `HorizontalDivider`; pass `null` for a bare list. When `songs` is empty the header still renders and `emptyMessage` takes the list's place, so an empty playlist keeps its cover art. PlaylistScreen also folds its "playlist not found" case in here by passing `header = null` + `emptyMessage = "Playlist not found"` when `state.playlist == null`.

**Call `rememberSongMenuController()` at the top of the screen composable, not inside a branch** — gating it on a non-empty list tears the bottom sheets down whenever the list empties or loading flips.

Does NOT support drag-to-reorder. If PlaylistScreen ever needs it, add it here behind a flag rather than forking the scaffold.

### SongCollectionHeader.kt — the header for those screens
`SongCollectionHeader(songCount, isPlaying, onShufflePressed, onMenuPressed: (() -> Unit)?, onPlayPressed, modifier, title: String? = null, artwork: (@Composable () -> Unit)? = null)`. Song count + shuffle/menu icon row + `PlayPauseButton`, built for `SongCollectionScaffold`'s `header` slot.

`onMenuPressed` is nullable and **hides the ⋮ when null** — pass null when the collection has no menu-worthy actions (LikedSongsScreen does this while empty; a ⋮ opening an empty sheet is worse than no ⋮). Wire the non-null case straight to the controller: `onMenuPressed = { collectionMenu.show(playlist) }`.

Both extras are optional slots: PlaylistScreen passes `artwork = { CoverImage(...) }` and `title = playlist.name`; LikedSongsScreen passes neither (no cover art exists, and the scaffold top bar already names it), so it renders as count + controls only.

**Pass `isPlaying = state.isPlaying && isContextActive`** — bare `state.isPlaying` makes the FAB show "pause" while a *different* collection is what's playing.

### MenuBottomSheet.kt — every ⋮ menu in the app
The sheet chrome (`ModalBottomSheet` + drag handle + `appColors.backgroundElevated`) plus the row renderer, shared by the per-song menu and the collection menu:

```kotlin
fun <T : MenuAction> MenuBottomSheet(
    isVisible: Boolean,
    actions: List<T>,
    onActionSelected: (T) -> Unit,
    onDismissRequest: () -> Unit
)
fun MenuBottomSheetItem(modifier: Modifier = Modifier, action: MenuAction, onClick: () -> Unit)
```

Rows are described by the `MenuAction` interface (`label`, `icon`, `accent: MenuAccent = Neutral`). Implement it with a **feature-local sealed class**, never by adding rows to someone else's: `SongMenuAction` (per-song) and `CollectionMenuAction` (per-collection) both do this. `MenuAccent` (`Neutral` / `Like` / `Destructive`) picks the icon-chip and label colors — `Destructive` is `appColors.error`.

Selecting a row hides the sheet, then fires `onDismissRequest` followed by `onActionSelected`, so an action that opens *another* sheet (add-to-playlist, rename) doesn't fight this one's exit animation. Don't re-close the sheet from your handler.

### PlaylistNameBottomSheet.kt — naming a playlist
`PlaylistNameBottomSheet(isVisible, name, isSaving, onNameChange, onConfirm, onDismissRequest, title = "New playlist", confirmLabel = "Create")`. `BasicTextField` in a bordered rounded box, keyboard auto-shown, and the incoming `name` arrives fully selected so the first keystroke replaces it. Library's "+" uses the defaults; the collection menu's rename passes `title = "Rename playlist", confirmLabel = "Save"`. Gate `onConfirm` on `name.isNotBlank() && !isSaving` in the caller too — the sheet only dims the button.

### ConfirmDialog.kt — stop sign for irreversible actions
`ConfirmDialog(isVisible, title, message, confirmLabel, onConfirm, onDismissRequest, isDestructive = false, dismissLabel = "Cancel")`. Material3 `AlertDialog` in app colors; `isDestructive` tints the confirm label with `appColors.error`. A dialog rather than a sheet on purpose — it can appear over a sheet. Used by the delete-playlist flow.

### PlaylistItem.kt
Simple playlist row: `PlaylistItem(modifier, playlist: Playlist, onClick, trailing: @Composable (() -> Unit)? = null)`. Cover thumb (`Dimens.Size.coverThumb`) + name + "Playlist • N songs" subtitle. Used by LibraryScreen (no trailing) and `AddToPlaylistBottomSheet`, which passes a `Checkbox` as `trailing` to show whether the song is already in that playlist. The trailing slot renders after the weighted text `Column`, separated by `Dimens.spaceS`; omit it and the row lays out exactly as before.

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
`PlayPauseButton(modifier, onPressed, isPlaying, isBuffering = false, willPlayWhenReady = false)`. A `FilledIconButton` sized `Dimens.Size.playButton` (64.dp) colored `appColors.iconActive`/`onAccent`. When `isBuffering`, shows a `CircularProgressIndicator` ring; if additionally `willPlayWhenReady`, a small play icon sits inside the ring ("queued to play once loaded"). Used by `SongCollectionHeader` (playlist + liked songs) and the player.

### PressableCard.kt — press-shrink animation
Not a composable; a modifier extension:
```kotlin
fun Modifier.pressableCard(onClick: () -> Unit) = composed { ... }
```
Shrinks to 0.95f over 90ms on press, springs back (`Spring.DampingRatioMediumBouncy`) on release, and only fires `onClick` if the press wasn't cancelled. It consumes tap input itself — do NOT combine with `clickable`. Used on LibraryScreen's `LikedSongBanner` and HomeScreen's `RecentlyPlayedItem` cards. Use it for any card-like tappable surface.

### SearchBar.kt
`SearchBar(modifier, query, onSuggestionPressed, onQueryChange, onVoiceSearch, isListening = false, onVoiceSearchCancel = {}, openKeyboardOnLaunch = false, onTextCleared)`. A `BasicTextField` with custom `decorationBox` (rounded `Dimens.radiusL`, `appColors.backgroundElevated` background, animated border color when focused), a state-machine trailing icon (`Stop` while listening / `Clear` when text present / `Mic` when focused+empty), and an animated "Cancel" text that slides in on focus. It syncs an internal `TextFieldValue` with the external `query` so voice transcripts move the cursor to the end. Search-specific; reuse as-is rather than forking.

### MenuBottomSheet.kt — the shared ⋮ sheet
`MenuBottomSheet(isVisible, actions: List<T : MenuAction>, onActionSelected, onDismissRequest)` renders a column of rows; `SongMenuAction` and `CollectionMenuAction` both implement `MenuAction` (label + icon + `MenuAccent`). Selecting a row **hides the sheet first, then fires `onDismissRequest` and `onActionSelected`**, so a row may open another sheet without fighting this one's exit animation.

A row that the owning ViewModel can't service itself reports back via a one-shot effect rather than doing cross-feature work: `SongMenuAction.SleepTimer` (passed only by the full-screen player, deliberately excluded from `SongMenuAction.all`) makes `SongMenuViewModel` emit `SongMenuEffect.OpenSleepTimer`, which `MusicPlayerScreen` collects off `SongMenuController.effect` to show its own `SleepTimerBottomSheet`. Follow that shape for any future player-scoped menu row.

## Theming — the honest rules

Theme composable: **`BudgetTheme`** in `ui/theme/Theme.kt` (name is a template leftover; it IS the app theme, applied in `App.kt`).

- **Colors**: the primary system is the custom `AppColors` data class (`ui/theme/AppColor.kt`) with `DarkAppColors` / `LightAppColors` palettes (violet accent + rose tertiary), exposed via `val appColors: AppColors @Composable get() = LocalAppColors.current`. `BudgetTheme` also maps these into a Material3 `ColorScheme`, so `MaterialTheme.colorScheme.*` works too — but screens overwhelmingly use `appColors.*` directly (`appColors.textPrimary`, `appColors.accentPrimary`, `appColors.divider`, ...). **New code: use `appColors`.**
- **Dimensions**: `ui/theme/Dimens.kt` — spacing (`spaceXxs`..`space3xl`, 4pt grid), radii (`radiusS`..`radius3xl`), icons (`iconXs`..`iconXl`), strokes, elevations, and `Dimens.Size.*` for fixed component sizes (`coverThumb`, `playButton`, `miniPlayerHeight`, ...). **Verified honest verdict: the codebase actually follows this** — a repo-wide search for raw `N.dp` in commonMain finds hits ONLY inside `ui/theme/` itself. Do not hardcode dp in new code; if no token fits, add one to `Dimens` with a comment.
- **Typography**: screens use `MaterialTheme.typography.*` (bodySmall/bodyMedium/titleLarge/headlineLarge/labelSmall...). CAVEAT: `ui/theme/Type.kt` defines a custom `Typography` and `ui/theme/Shape.kt` defines custom `Shapes`, but `BudgetTheme` does **not** pass either into `MaterialTheme(...)` — so you're getting Material defaults. Both files are effectively dead until someone wires them in. Don't assume Type.kt values apply.
- Small honest exceptions exist: raw `sp` (`letterSpacing = 0.sp` in LibraryScreen), `Color.Transparent`, and `Color.Black.copy(alpha = 0.45f)` (SongItem overlay scrim). Transparent/scrim colors are fine; anything with hue belongs in `AppColors`.

## Lists

- Plain lists: `LazyColumn` with stable `key`s — `items(state.libraryItems, key = { it.stableKey() })` (LibraryScreen), `itemsIndexed(songs, key = { _, song -> song.uniqueId })` (SongCollectionScaffold), `key = { "${it.contentType}:${it.contentId}" }` (HomeScreen LazyRow).
- Screens use `Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0), topBar = {...})` — insets are handled by the dashboard scaffold, so feature screens zero them out. Song-list screens get this for free from `SongCollectionScaffold`.
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
`Modifier.draggableHandle()` is only available inside the `ReorderableItem` scope. The move callback fires per position change; the actual persistence happens in `onDragEnd()`. PlaylistScreen does NOT currently use reorderable (its `reorderSongs` repository method exists, but it renders through `SongCollectionScaffold`, which uses a plain `itemsIndexed`).

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
