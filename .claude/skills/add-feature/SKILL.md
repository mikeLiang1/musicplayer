---
name: add-feature
description: Use when adding a new feature/screen (or platform-specific capability) to this KMP music player so the new code follows the project's exact MVVM State/Action/Effect conventions.
---

# Adding a Feature to MusicPlayer

All shared code lives in `composeApp/src/commonMain/kotlin/org/example/project/`. Paths below are relative to that root unless they start with `composeApp/` or `androidMain`. Read `CLAUDE.md` at the repo root for the full file map.

**Best template to copy: the Library feature** (`features/library/`). It is mid-size, has the complete UiState/Action/Effect trio, a `model/` subpackage, its own navigation file, and the derived state style (`combine().stateIn()`; ignore its leftover unused `_uiState` — the imperative exposure is commented out). Search (`features/search/`) is the best template for imperative state updates, error effects, and platform-optional dependencies.

## The pattern as actually used

One file per ViewModel contains **everything**: the ViewModel class, then `XxxUiState`, `XxxAction`, `XxxEffect` declared at the bottom of the same file (see `features/library/ui/LibraryViewModel.kt`, `features/search/ui/SearchViewModel.kt`, `features/home/ui/HomeViewModel.kt`).

### UiState
```kotlin
data class LibraryUiState(
    val allItems: List<LibraryItem> = listOf(),
    val selectedFilter: LibraryItemFilter = LibraryItemFilter.All,
    ...
)
```
Plain data class, every field has a default so `XxxUiState()` is a valid empty/loading state.

Two ways to produce it — both exist in the codebase:
1. **Imperative** (Home, Search): `private val _uiState = MutableStateFlow(SearchUiState())`, exposed via `val uiState = _uiState.asStateFlow()`, mutated with `_uiState.update { it.copy(...) }`.
2. **Derived** (Library): build `uiState` from source flows:
```kotlin
val uiState: StateFlow<LibraryUiState> = combine(
    playlistRepository.getPlaylists(),
    _selectedFilter
) { playlists, filter -> LibraryUiState(...) }
    .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = LibraryUiState())
```
Prefer style 2 when state is a pure function of Room/repository flows; style 1 otherwise.

### Actions
```kotlin
sealed interface SearchAction {
    data object OnBackPressed : SearchAction
    data class OnQueryChanged(val query: String) : SearchAction
    data class OnSongClicked(val song: Song) : SearchAction
}
```
Sealed **interface** (not class), members named `OnXxx` (`data object` for no-arg, `data class` for payload). Dispatched through a single entry point:
```kotlin
fun handleAction(searchAction: SearchAction) { when (searchAction) { ... } }
```
Inconsistency to know about: `HomeViewModel` names it `onHomeAction(action: HomeAction)` instead of `handleAction`. **Use `handleAction` for new code** — Search, Library and Playlist all use it.

### Effects (one-shot events: navigation, snackbars)
```kotlin
private val _effect = MutableSharedFlow<LibraryEffect>()
val effect: SharedFlow<LibraryEffect> = _effect.asSharedFlow()
// emit inside a coroutine:
viewModelScope.launch { _effect.emit(LibraryEffect.NavigateToPlaylist(id)) }
```
```kotlin
sealed interface LibraryEffect {
    data class NavigateToPlaylist(val playlistId: String) : LibraryEffect
}
```
Effects are collected **outside the screen**, in the navigation composable that hosts it (see below). ViewModels never touch `Navigator` — navigation always goes ViewModel → Effect → navigation composable → `Navigator`.

### Screen
Screens are stateless and take exactly `(state, onAction)`:
```kotlin
@Composable
fun LibraryScreen(state: LibraryUiState, onAction: (LibraryAction) -> Unit)
```
The screen never sees the ViewModel. This keeps it previewable — every screen ends with a preview:
```kotlin
@DevicePreviews
@Composable
private fun LibraryPreview() {
    AppPreview { LibraryScreen(state = LibraryUiState(), onAction = {}) }
}
```
(`@DevicePreviews` and `AppPreview` come from `ui/theme/`.)

### Wiring: who creates the ViewModel and collects state/effects

The **navigation composable** (the `entry<Route>` block), not the screen. Real example from `features/library/navigation/LibraryNavigation.kt`:
```kotlin
entry<Route.DashboardRoutes.LibraryRoutes.Library> {
    val libraryViewModel = koinViewModel<LibraryViewModel>()
    val state by libraryViewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        libraryViewModel.effect.collect { effect ->
            when (effect) {
                is LibraryEffect.NavigateToPlaylist -> navigateToPlaylist(effect.playlistId)
            }
        }
    }
    LibraryScreen(state = state, onAction = libraryViewModel::handleAction)
}
```
- ViewModel: `koinViewModel<XxxViewModel>()` from `org.koin.compose.viewmodel.koinViewModel`.
- State: `collectAsStateWithLifecycle()` from `androidx.lifecycle.compose` (always this, never plain `collectAsState`).
- Effects: `LaunchedEffect(Unit) { vm.effect.collect { ... } }`.
- Snackbars: `SnackbarHostState` is created in `features/dashboard/navigation/DashboardNavigation.kt` and passed down as a parameter (see `SearchNavigation(snackbarHostState)`); error effects call `snackbarHostState.showSnackbar(effect.message)`.

### How screens get Navigator and SongMenuController
- `Navigator` is **not injected via Koin**. Each navigation composable builds its own: `val navigator = remember { Navigator(navigationState) }` (see `DashboardNavigation.kt`, `LibraryNavigation.kt`). Cross-level navigation is done with lambdas passed down, e.g. `LibraryNavigation(navigateToPlaylist = { navigator.navigate(Route.DashboardRoutes.Playlist(it)) })`.
- Song context menu: call `rememberSongMenuController()` (from `features/songMenu/ui/SongMenuController.kt`) inside a screen. It obtains `SongMenuViewModel` via Koin, composes the two bottom sheets itself, and returns a controller with `show(song, options, playlistSongId)`. Called in `PlaylistScreen.kt` and `MusicPlayerScreen.kt` (which passes the controller down to `QueueScreen` as a parameter — don't call it twice in nested composables or you compose duplicate sheets).

## Recipe: new feature screen

1. **Create the package** `features/<name>/ui/` under `commonMain/kotlin/org/example/project/`. Add `model/` for feature-local models (like `features/library/model/LibraryItem.kt`) and `repository/` only if the feature owns data access (like `features/playlist/repository/PlaylistRepository.kt`). Add `navigation/` if the feature is a bottom-nav tab with its own stack.
2. **Write `<Name>ViewModel.kt`** containing, in order: `class <Name>ViewModel(deps...) : ViewModel()` with `uiState`, `effect`, `handleAction`; then `data class <Name>UiState(...)`, `sealed interface <Name>Action`, `sealed interface <Name>Effect` at the bottom. Copy `LibraryViewModel.kt` and rename.
3. **Write `<Name>Screen.kt`**: `@Composable fun <Name>Screen(state: <Name>UiState, onAction: (<Name>Action) -> Unit)` with private section composables below it and a `@DevicePreviews` preview at the bottom. Root is usually a `Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0), ...)` — the outer dashboard scaffold already handles insets.
4. **Register in Koin** — add to `viewModelModule` in `core/di/AppModule.kt` (import `org.koin.core.module.dsl.viewModel`):
   ```kotlin
   viewModel { LibraryViewModel(get(), get(), get()) }
   ```
   Runtime parameter variant (used by `PlaylistViewModel`):
   ```kotlin
   viewModel { params -> PlaylistViewModel(params.get(), get(), get(), get(), get()) }
   ```
   consumed with `koinViewModel(parameters = { parametersOf(key.playlistId) })`.
   Repositories/use cases go in `repositoryModule` / `useCaseModule` in the same file as `single { ... }`.
5. **Wire into navigation**: define a route in `navigation/Route.kt` and add an `entry<YourRoute> { ... }` block (as in "Wiring" above) in the correct entryProvider. Full details, including which file owns which entryProvider and the tab back-stack rules, are in the `navigation` skill — read it before this step.
6. **Verify**: `./gradlew :composeApp:assembleDebug` compiles; the preview renders; navigating to the screen doesn't crash with a Koin `NoDefinitionFoundException` (means step 4 was missed).

## Recipe: platform-specific functionality

This project uses **two different mechanisms** — pick by whether the code is composable:

### Non-composable code: interface in commonMain + Koin binding in androidMain
(NOT expect/actual.) Worked example — voice search:
1. Interface in `commonMain`: `core/SpeechRecognizer.kt`
   ```kotlin
   interface SpeechRecognizer {
       fun startListening(): Flow<String>
       fun cancel()
   }
   ```
2. Implementation in `androidMain`: `composeApp/src/androidMain/kotlin/org/example/project/core/SpeechRecognizerImpl.kt` — wraps `android.speech.SpeechRecognizer` in a `callbackFlow` (partials then final result, `awaitClose { recognizer.destroy() }`).
3. Bind in the **Android** Koin module, `composeApp/src/androidMain/kotlin/org/example/project/core/di/AndroidModule.kt`:
   ```kotlin
   single<SpeechRecognizer> { SpeechRecognizerImpl(androidContext()) }
   ```
   (`androidModule` is included in `startKoin` in `androidMain/.../MainApplication.kt`.)
4. Consume it **nullable** in commonMain so unbound platforms still work — `viewModelModule` in `AppModule.kt`:
   ```kotlin
   viewModel { SearchViewModel(get(), get(), get(), get(), getOrNull<SpeechRecognizer>()) }
   ```
   and the ViewModel guards: `val recognizer = speechRecognizer ?: return`. `MusicPlayerManager` follows the same interface-in-commonMain / `single<MusicPlayerManager> { MusicPlayerManagerImpl(get()) }` pattern (non-nullable, because the app can't run without it).

### Composable code: `@Composable expect/actual`
Worked example — mic permission. Same package and file name in both source sets:
- `commonMain/.../features/search/ui/VoiceSearchPermission.kt`:
  ```kotlin
  @Composable
  expect fun RequestVoicePermissionEffect(onGranted: () -> Unit)
  ```
- `androidMain/.../features/search/ui/VoiceSearchPermission.kt`: `actual fun RequestVoicePermissionEffect(...)` — checks `RECORD_AUDIO` via `ContextCompat.checkSelfPermission`, otherwise launches `rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission())` from a `LaunchedEffect(Unit)`.
- Call site (`SearchScreen.kt`): gated by a `showPermissionDialog` boolean so the effect only composes on demand.

Note: iOS targets are commented out in `composeApp/build.gradle.kts`, so today only the Android `actual`/binding must exist — but keep the split anyway.

## Conventions checklist

- [ ] Package: `org.example.project.features.<name>.ui` (+ optional `model`, `repository`, `navigation`)
- [ ] Names: `<Name>ViewModel`, `<Name>Screen`, `<Name>UiState`, `<Name>Action` (members `OnXxx`), `<Name>Effect` — all in the ViewModel file
- [ ] Single dispatcher `fun handleAction(action: <Name>Action)`; screens call `viewModel::handleAction`
- [ ] Effects via `MutableSharedFlow` + `asSharedFlow()`; collected in the navigation composable, never in the screen
- [ ] Screen signature `(state, onAction)`; stateless; `@DevicePreviews` + `AppPreview` preview at the bottom
- [ ] Theming: use `appColors.*`, `Dimens.*`, `MaterialTheme.typography.*` from `ui/theme/` — never hardcoded dp/colors
- [ ] Lists: give `items(...)` a stable `key` (see `stableKey()` in `features/library/model/LibraryItem.kt`)
- [ ] Koin: `viewModel { ... }` in `AppModule.kt`'s `viewModelModule`; platform impls in `AndroidModule.kt`

## Common failures

| Symptom | Cause | Fix |
|---|---|---|
| `NoDefinitionFoundException` for your ViewModel at runtime | Forgot step 4 | Add `viewModel { ... }` to `viewModelModule` in `core/di/AppModule.kt` |
| `NoDefinitionFoundException` for a platform interface | Bound in `AppModule.kt` where the impl class doesn't exist, or forgot `AndroidModule.kt` | Bind `single<Interface> { Impl(...) }` in `androidMain/.../core/di/AndroidModule.kt` |
| Effect (snackbar/navigation) fires never or only once | Collected inside the screen, or collected without `LaunchedEffect` | Collect in the navigation composable: `LaunchedEffect(Unit) { vm.effect.collect { ... } }` |
| Compile error "Suspend function 'emit' should be called only from a coroutine..." | `_effect.emit` called directly in `handleAction` | Wrap in `viewModelScope.launch { _effect.emit(...) }` |
| UI stops updating when app is backgrounded then resumed | Used `collectAsState()` | Use `collectAsStateWithLifecycle()` (androidx.lifecycle.compose) |
| Derived `stateIn` state resets/refetches on every navigation | Wrong `started` value | Use `SharingStarted.WhileSubscribed(5000)` like `LibraryViewModel` |
| Preview won't render | Screen takes a ViewModel instead of `(state, onAction)` | Keep screens stateless; construct `XxxUiState()` in the preview |
| Unresolved reference `viewModel` in AppModule | Wrong import | `import org.koin.core.module.dsl.viewModel` |
