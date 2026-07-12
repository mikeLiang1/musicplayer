---
name: navigation
description: Use when adding routes or bottom-nav tabs, changing back behavior, or debugging tab back stacks / the full-screen player overlay in this app's AndroidX Navigation3 setup.
---

# Navigation in MusicPlayer

All paths relative to `composeApp/src/commonMain/kotlin/org/example/project/` unless noted. This project uses **AndroidX Navigation3** (`NavDisplay` + `entryProvider`), not classic NavHost. There is no `NavController`.

**Spelling warning:** the Search navigation package directory is genuinely misspelled: `features/search/navigtion/SearchNavigation.kt` (package `org.example.project.features.search.navigtion`). It is not a typo in this doc — match it exactly or imports fail.

## File map

| File | Owns |
|---|---|
| `navigation/Route.kt` | All route definitions + the route sets (`appTopLevelRoutes`, `dashboardAllRoutes`, `searchAllRoutes`, `libraryAllRoutes`) |
| `navigation/NavigationState.kt` | `rememberNavigationState`, `NavigationState` (topLevelRoute + per-tab back stacks), `toEntries()` |
| `navigation/Navigator.kt` | `navigate` / `goBack` / `navigateToTopLevelRoute` / `replaceRoot` |
| `navigation/AppNavigation.kt` | Root `NavDisplay`; only live entry is `DashboardRoutes` |
| `features/dashboard/navigation/DashboardNavigation.kt` | Main scaffold: bottom bar, mini player, snackbar host, dashboard entryProvider (Home / Search / Library / shared Playlist), player overlay |
| `features/dashboard/navigation/BottomNavItems.kt` | `dashboardTopLevelDestinations: Map<Route, BottomNavItem>` — drives tab buttons and bar visibility |
| `features/dashboard/navigation/BottomNavigationBar.kt` | Renders the map; calls `navigate(route)` per tab |
| `features/library/navigation/LibraryNavigation.kt` | Nested NavDisplay for the Library tab |
| `features/search/navigtion/SearchNavigation.kt` | Nested NavDisplay for the Search tab (misspelled dir) |
| `core/manager/PlayerNavigator.kt` | Channel bridging platform events → open full-screen player |
| `features/musicPlayer/ui/DismissiblePlayerOverlay.kt` | Full-screen player sheet drawn over the scaffold |

## Mental model

### Routes — `navigation/Route.kt`
Every destination is a `@Serializable` member of one sealed interface:
```kotlin
@Serializable
sealed interface Route : NavKey {
    @Serializable
    data object DashboardRoutes : Route {
        @Serializable data object Home : Route
        @Serializable data object SearchRoutes : Route { ... }
        @Serializable data object LibraryRoutes : Route { ... }
        @Serializable data class Playlist(val playlistId: String) : Route
    }
}
```
Route arguments are just constructor properties (`Playlist(val playlistId: String)`). The same file declares the route **sets** that feed each navigation level: `appTopLevelRoutes`, `dashboardAllRoutes` (Home/SearchRoutes/LibraryRoutes), `searchAllRoutes`, `libraryAllRoutes`.

### NavigationState — `navigation/NavigationState.kt`
The actual data structures:
- `topLevelRoute: NavKey` — the active tab, backed by `rememberSerializable` + `MutableStateSerializer(NavKeySerializer())` so it survives process death.
- `backStacks: Map<NavKey, NavBackStack<NavKey>>` — **one back stack per top-level route**, each created with `rememberNavBackStack(key)` (so each stack starts containing its own key).
- `stacksInUse` — `[startRoute]` when on the start tab, else `[startRoute, topLevelRoute]`. `toEntries()` flattens those stacks (in that order) into the entry list handed to `NavDisplay`, decorating each entry with `rememberSaveableStateHolderNavEntryDecorator` + `rememberViewModelStoreNavEntryDecorator` — this is why each destination gets its own ViewModel store and saveable state.

### Navigator — `navigation/Navigator.kt`
- `navigate(route)` — if `route` is a key of `backStacks` (i.e. a top-level tab), just switches `topLevelRoute`. Otherwise **pushes onto the current tab's stack**: `state.backStacks[state.topLevelRoute]?.add(route)`.
- `goBack()` — pops the current tab's stack; if already at the tab's base entry, jumps to `state.startRoute` (the Home tab) instead.
- `navigateToTopLevelRoute(route)` — used by the bottom bar. **Clears the target tab's stack** and re-adds its root before switching. Tabs therefore do NOT keep their pushed sub-screens across tab switches (see Gotchas).
- `replaceRoot(newRoot)` — clears the new root's stack to `[newRoot]`, clears **every other stack too**, then switches. `error(...)` if `newRoot` has no stack in the map. Intended for flows like login → dashboard (see the commented-out login entry in `AppNavigation.kt` lines 27-33).

`Navigator` is never injected via Koin. Each navigation composable creates its own: `val navigator = remember { Navigator(navigationState) }`.

Key `navigate` logic, verbatim from `Navigator.kt`:
```kotlin
fun navigate(route: NavKey) {
    if (route in state.backStacks.keys) {
        // This is a top level route, just switch to it.
        state.topLevelRoute = route
    } else {
        state.backStacks[state.topLevelRoute]?.add(route)
    }
}
```
So whether a route "switches tab" or "pushes a screen" is decided purely by membership in the `topLevelRoutes` set that built the `NavigationState` — there is no other flag.

Every level follows the same skeleton (verbatim shape from `LibraryNavigation.kt`):
```kotlin
val navigationState = rememberNavigationState(
    startRoute = Route.DashboardRoutes.LibraryRoutes.Library,
    topLevelRoutes = libraryAllRoutes
)
val navigator = remember { Navigator(navigationState) }
val entryProvider = entryProvider<NavKey> { entry<...> { ... } }
NavDisplay(
    entries = navigationState.toEntries(entryProvider),
    onBack = { navigator.goBack() }
)
```

### The tree (three nested NavDisplays)
1. `navigation/AppNavigation.kt` — top level. One live entry: `entry<Route.DashboardRoutes> { DashboardNavigation() }`.
2. `features/dashboard/navigation/DashboardNavigation.kt` — the main scaffold. Own `rememberNavigationState(startRoute = Route.DashboardRoutes.Home, topLevelRoutes = dashboardAllRoutes)`. Its entryProvider has: `Home` (inline HomeScreen wiring), `SearchRoutes` → `SearchNavigation(snackbarHostState)`, `LibraryRoutes` → `LibraryNavigation(...)`, and the shared `Playlist` entry. Its `Scaffold` `bottomBar` stacks `MusicPlayerBar` + `BottomNavigationBar`.
3. `features/search/navigtion/SearchNavigation.kt` and `features/library/navigation/LibraryNavigation.kt` — per-tab nested NavDisplays with their own `NavigationState` (currently each holds a single route: `Suggestions` / `Library`).

### How the shared Playlist detail works (trace)
`Route.DashboardRoutes.Playlist` is in **no** top-level route set — it exists only as an `entry` in **DashboardNavigation's** entryProvider:
```kotlin
entry<Route.DashboardRoutes.Playlist> { key ->
    val playlistViewModel: PlaylistViewModel = koinViewModel(
        parameters = { parametersOf(key.playlistId) }
    )
    ...
    PlaylistScreen(state, onBackPressed = { navigator.goBack() }, onAction = playlistViewModel::handleAction)
}
```
Because it isn't a top-level key, `navigator.navigate(Route.DashboardRoutes.Playlist(id))` pushes it onto **whichever tab stack is currently active** in the dashboard state. Both entry paths funnel to that same dashboard-level navigator:
- From Home: `HomeEffect.NavigateToPlaylist` is collected inside the `Home` entry in `DashboardNavigation.kt` → `navigator.navigate(Route.DashboardRoutes.Playlist(effect.playlistId))`.
- From Library: `DashboardNavigation` passes a lambda down — `LibraryNavigation(navigateToPlaylist = { navigator.navigate(Route.DashboardRoutes.Playlist(it)) })` — and `LibraryNavigation` invokes it when it collects `LibraryEffect.NavigateToPlaylist`. The nested library navigator is NOT used for this; the push happens on the dashboard's Library tab stack.

That is the pattern for any screen reachable from multiple tabs: register the entry once at dashboard level, hand child navigations a lambda.

## Recipe: add a new route (pushed detail screen)

Worked example: pretend `Playlist` didn't exist and you're adding it.
1. **Define the route** in `navigation/Route.kt` as a nested `@Serializable` member of `Route.DashboardRoutes` (data class if it has args). Do NOT add it to `dashboardAllRoutes` — only tabs go there.
2. **Pick the owning entryProvider**:
   - reachable from multiple tabs → `DashboardNavigation.kt`'s entryProvider.
   - internal to one tab's flow → that tab's file (`LibraryNavigation.kt` / `SearchNavigation.kt`) **and** add the route to that tab's set in `Route.kt` (`libraryAllRoutes` / `searchAllRoutes`)... **caution**: routes in those sets become top-level roots of the *nested* state with their own parallel stack, not pushed entries. For a plain push inside a tab, keep the route out of every set and add the `entry` to the nested entryProvider; `navigator.navigate` on the nested navigator will push it.
3. **Add the entry**: `entry<Route.DashboardRoutes.YourRoute> { key -> ... }` — `key` gives you the route args. Wire ViewModel/state/effects per the `add-feature` skill.
4. **Navigate**: from a ViewModel emit an Effect; in the navigation composable collect it and call `navigator.navigate(Route.DashboardRoutes.YourRoute(...))` (or invoke a passed-down lambda if the navigator lives a level up).
5. **Verify**: navigate in → system back returns to the pushed-from screen (NavDisplay's `onBack = { navigator.goBack() }`); switching tabs and returning behaves as you expect (see Gotchas — the pushed screen will be gone after a tab switch).

## Recipe: add a new bottom-nav tab

1. `navigation/Route.kt` — add a nested route object inside `Route.DashboardRoutes` (e.g. `data object ProfileRoutes : Route`) and add it to `dashboardAllRoutes`. Without this, `rememberNavigationState` creates no back stack for it — `navigateToTopLevelRoute` still switches `topLevelRoute` (only `stack?.clear()`/`add` are null-guarded), so the tab highlights but shows no content (`toEntries` finds no stack), and the next `goBack()` crashes with `error("Stack for ... not found")`.
2. `features/dashboard/navigation/BottomNavItems.kt` — add an entry to `dashboardTopLevelDestinations`:
   ```kotlin
   Route.DashboardRoutes.LibraryRoutes to BottomNavItem(label = "Library", icon = Icons.Filled.LibraryMusic)
   ```
   `BottomNavigationBar.kt` iterates this map to render buttons and calls `navigate(route)` → `navigator.navigateToTopLevelRoute(it)`; nothing else to touch there. This map also drives `isBottomBarVisible` in `DashboardNavigation.kt` (`navigationState.topLevelRoute in dashboardTopLevelDestinations.keys`).
3. `DashboardNavigation.kt` — add `entry<Route.DashboardRoutes.ProfileRoutes> { ... }`. Either an inline screen (like Home) or a nested `XxxNavigation(...)` composable (like Search/Library) if the tab will own sub-screens.
4. If nested: create `features/<name>/navigation/<Name>Navigation.kt` copying `LibraryNavigation.kt` — own `rememberNavigationState(startRoute = ..., topLevelRoutes = <name>AllRoutes)` + `Navigator` + `NavDisplay`, and a new `<name>AllRoutes` set in `Route.kt`.
5. **Verify**: the tab button appears and highlights via `navigationState.topLevelRoute == route`; back from the new tab's root lands on Home (that's `goBack`'s jump-to-`startRoute` rule); the mini player bar still shows above the bottom bar.

## The player is NOT a route

Verified in `DashboardNavigation.kt` and `features/musicPlayer/ui/DismissiblePlayerOverlay.kt`:
- `MusicPlayerBar` (mini player) sits in the dashboard `Scaffold`'s `bottomBar`, above `BottomNavigationBar`. Tapping it calls `viewModel.setFullScreen(true)`.
- The full-screen player is `DismissiblePlayerOverlay(visible = isFullScreenVisible, ...)` drawn in a `Box` **on top of the whole Scaffold** — it never enters any back stack. Visibility is ViewModel state: `MusicPlayerViewModel.uiState.isFullScreenVisible`, flipped only via `setFullScreen(Boolean)`.
- It's a swipe-down-to-dismiss sheet (`PlayerSheetState` + nested-scroll handoff from the queue's LazyColumn). Every dismiss path just flips the flag; a `LaunchedEffect(visible)` animates to match. When hidden it composes nothing, so the mini bar stays tappable.
- Back press while open: `MusicPlayerScreen.kt` line 68 — `BackHandler { onDismissRequest() }` → `setFullScreen(false)`. So back closes the overlay without touching navigation state.
- External open (media notification tap): `core/manager/PlayerNavigator.kt` — a Koin singleton wrapping a `Channel<Unit>(Channel.CONFLATED)`. Platform code calls `requestOpenPlayer()`; `DashboardNavigation` collects `openPlayerEvents` in a `LaunchedEffect` and calls `setFullScreen(true)`. Use this pattern for any "open UI from outside Compose" need.

Consequence: navigation changes can't break the player, and the player can't be "navigated to" — don't try to add it to `Route.kt`.

## Gotchas

- **Tab switches reset the tab's stack.** `navigateToTopLevelRoute` does `stack?.clear(); stack?.add(route)`. Push Playlist from Library → switch to Home → back to Library: you're on the Library root, Playlist is gone. This is current intended behavior; if you want stack retention, change `navigateToTopLevelRoute`, not the bottom bar.
- **Back at a tab root goes to Home, not out of the app.** `goBack()`: `if (currentRoute == state.topLevelRoute) state.topLevelRoute = state.startRoute`. Only from the Home tab does back propagate up (Home's stack base) and eventually exit.
- **`replaceRoot` wipes every other stack** and `error()`s if the new root isn't in `backStacks` (i.e. wasn't in the `topLevelRoutes` set). Only currently referenced by the commented-out login flow in `AppNavigation.kt`.
- **A route pushed but not registered** in the active entryProvider crashes NavDisplay when it tries to build the entry (unverified exact exception; the entry simply has no provider). Every `navigate` target needs a matching `entry<...>`.
- **`BackHandler` in commonMain**: `androidx.activity.compose.BackHandler` is used directly in common code (`SearchScreen.kt:47`, `MusicPlayerScreen.kt:68`). Search uses it for an in-screen state pop (`SearchAction.OnBackPressed` returns from results to suggestions) — a screen-level back layered above the NavDisplay `onBack`. Enabled `BackHandler`s win over `NavDisplay.onBack`, which is why searching results → back doesn't leave the Search tab.
- **Process death**: `topLevelRoute` and the back stacks are serialized (`rememberSerializable` / `rememberNavBackStack`), which is why every route must be `@Serializable` and implement `NavKey`. Forgetting `@Serializable` on a new route fails at runtime state-save, not compile time.
- **Three navigator instances exist** (app, dashboard, per-tab). When adding navigation calls, make sure you're holding the one whose `NavigationState` owns the target stack — the wrong one either pushes onto the wrong stack or no-ops. When in doubt, pass a lambda down from the level that owns the entry (the `LibraryNavigation(navigateToPlaylist = ...)` pattern).

## Common failures

| Symptom | Cause | Fix |
|---|---|---|
| New bottom-nav tab highlights but shows no content; back then crashes `error("Stack ... not found")` | Route missing from `dashboardAllRoutes`, so it has no back stack | Add it to the set in `Route.kt` |
| Crash on navigating to a new route | No `entry<...>` for it in the active entryProvider | Register the entry in the navigation file that owns that level |
| Detail screen opens from one tab but not another | Entry registered in a nested (per-tab) entryProvider instead of `DashboardNavigation` | Move the entry to dashboard level; pass navigate lambdas down |
| State save crash mentioning serialization | New route not `@Serializable` | Annotate the route (and any arg types) |
| Back exits the tab instead of closing search results / player | Screen-level back state not handled | Add an enabled-gated `BackHandler` like `SearchScreen.kt:47` |
| Pushed screen vanishes after switching tabs and returning | `navigateToTopLevelRoute` clears the tab stack by design | Expected; change `Navigator.navigateToTopLevelRoute` if retention is required |
| `error("Stack for X not found")` | `replaceRoot`/`goBack` on a route with no stack in `backStacks` | Ensure the route was in the `topLevelRoutes` set passed to `rememberNavigationState` |
| Unresolved reference importing SearchNavigation | Assumed package spelled `navigation` | It is `features.search.navigtion` (sic) |
