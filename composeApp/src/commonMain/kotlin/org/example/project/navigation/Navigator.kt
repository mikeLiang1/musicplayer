package org.example.project.navigation

import androidx.navigation3.runtime.NavKey
import org.example.project.navigation.NavigationState

/**
 * Handles navigation events (forward and back) by updating the navigation state.
 */
class Navigator(val state: NavigationState) {
    fun navigate(route: NavKey) {
        if (route in state.backStacks.keys) {
            // This is a top level route, just switch to it.
            state.topLevelRoute = route
        } else {
            state.backStacks[state.topLevelRoute]?.add(route)
        }
    }

    fun goBack() {
        val currentStack = state.backStacks[state.topLevelRoute]
            ?: error("Stack for ${state.topLevelRoute} not found")
        val currentRoute = currentStack.last()

        // If we're at the base of the current route, go back to the start route stack.
        if (currentRoute == state.topLevelRoute) {
            state.topLevelRoute = state.startRoute
        } else {
            currentStack.removeLastOrNull()
        }
    }

    fun navigateToTopLevelRoute(route: NavKey) {
        if (route == state.topLevelRoute) return
        val stack = state.backStacks[route]
        stack?.clear()
        stack?.add(route)
        state.topLevelRoute = route
    }

    fun replaceRoot(newRoot: NavKey) {
        // 1. Logic for "PopUpTo(0) inclusive" on the NEW route
        // We want the new stack to be fresh: [newRoot]
        val newStack = state.backStacks[newRoot]
            ?: error("Stack for $newRoot not found")

        newStack.clear()
        newStack.add(newRoot)

        // 2. Logic for "Clearing the old task"
        // Clean up all OTHER stacks to free memory (Optional but recommended)
        state.backStacks.forEach { (key, stack) ->
            if (key != newRoot) {
                stack.clear()
            }
        }

        // 3. Finally, switch the channel
        state.topLevelRoute = newRoot
    }
}
