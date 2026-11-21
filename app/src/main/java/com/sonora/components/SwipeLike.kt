package com.sonora.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.sonora.theme.LocalSonoraColors
import com.sonora.theme.content

/**
 * M15 swipe-to-like wrapper. Either direction fires [onToggle] and
 * snaps back; the row never dismisses.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeLike(
    liked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val colors = LocalSonoraColors.current
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it != SwipeToDismissBoxValue.Settled) onToggle()
            false
        },
    )
    SwipeToDismissBox(
        state = state,
        backgroundContent = {
            // Only mid-swipe: at rest the background stays empty so no
            // wash leaks through around the row.
            val direction = state.dismissDirection
            if (direction != null) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(colors.content.copy(alpha = 0.10f))
                        .padding(horizontal = 20.dp),
                    contentAlignment = when (direction) {
                        SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                        else -> Alignment.CenterStart
                    },
                ) {
                    Icon(
                        if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = null,
                        tint = colors.content,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        },
        modifier = modifier,
    ) {
        content()
    }
}
