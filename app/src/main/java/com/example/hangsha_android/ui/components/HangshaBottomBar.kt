package com.example.hangsha_android.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import com.example.hangsha_android.ui.navigation.BottomTab
import com.example.hangsha_android.ui.navigation.HangshaDestinations

@Composable
fun HangshaBottomBar(
    currentDestination: NavDestination?,
    onNavigateToDestination: (BottomTab) -> Unit
) {
    val currentRoute = currentDestination?.route

    NavigationBar {
        BottomTab.entries.forEach { tab ->
            val selected = when {
                tab == BottomTab.Calendar &&
                    currentRoute?.startsWith(HangshaDestinations.DailyEvents.baseRoute) == true -> true
                else -> currentDestination?.hierarchy?.any { it.route == tab.route } == true
            }
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigateToDestination(tab) },
                alwaysShowLabel = false,
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color.Transparent
                ),
                icon = {
                    Icon(
                        painter = painterResource(
                            id = if (selected) tab.activeIconResId else tab.iconResId
                        ),
                        contentDescription = tab.label,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(30.dp)
                    )
                }
            )
        }
    }
}
