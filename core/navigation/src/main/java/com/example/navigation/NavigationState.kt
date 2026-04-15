package com.example.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator

class NavigationState(
    val backStack: NavBackStack<NavKey>,
) {
    fun navigateTo(route: NavKey) {
        backStack.add(route)
    }

    fun navigateBack(): Boolean {
        if (backStack.size > 1) {
            backStack.removeLastOrNull()
            return true
        }
        return false
    }

    fun replace(route: NavKey) {
        if (backStack.isNotEmpty()) backStack.removeLastOrNull()
        backStack.add(route)
    }

    fun navigateAndClear(route: NavKey) {
        backStack.clear()
        backStack.add(route)
    }
}

@Composable
fun rememberNavigationState(
    startKey: NavKey
): NavigationState {
    val backStack = rememberNavBackStack(startKey)
    return remember {
        NavigationState(backStack)
    }
}
