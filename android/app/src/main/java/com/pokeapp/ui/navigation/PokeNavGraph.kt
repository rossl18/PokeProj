package com.pokeapp.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.pokeapp.ui.collection.CollectionScreen
import com.pokeapp.ui.detail.CardDetailScreen
import com.pokeapp.ui.search.SearchScreen

private object Routes {
    const val COLLECTION = "collection"
    const val SEARCH = "search"
    const val CARD_DETAIL = "cardDetail/{cardId}?variant={variant}"

    fun cardDetail(cardId: String, variant: String) = "cardDetail/$cardId?variant=$variant"
}

private data class BottomTab(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val bottomTabs = listOf(
    BottomTab(Routes.COLLECTION, "Collection", Icons.Filled.Style),
    BottomTab(Routes.SEARCH, "Search & Scan", Icons.Filled.Search),
)

@androidx.camera.core.ExperimentalGetImage
@Composable
fun PokeNavGraph(navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination

    Scaffold(
        bottomBar = {
            val showBottomBar = bottomTabs.any { it.route == currentRoute?.route }
            if (showBottomBar) {
                NavigationBar {
                    bottomTabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute?.hierarchy?.any { it.route == tab.route } == true,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.COLLECTION,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.COLLECTION) {
                CollectionScreen(
                    onCardClick = { cardId, variant ->
                        navController.navigate(Routes.cardDetail(cardId, variant))
                    },
                )
            }
            composable(Routes.SEARCH) {
                SearchScreen(
                    onCardClick = { cardId, variant ->
                        navController.navigate(Routes.cardDetail(cardId, variant))
                    },
                )
            }
            composable(
                route = Routes.CARD_DETAIL,
                arguments = listOf(
                    navArgument("cardId") { type = NavType.StringType },
                    navArgument("variant") { type = NavType.StringType; nullable = true },
                ),
            ) {
                CardDetailScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
