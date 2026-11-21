package com.sonora.nowplaying

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sonora.theme.LocalSonoraColors
import com.sonora.theme.LocalSonoraTypography
import com.sonora.theme.content
import com.sonora.theme.contentInverse
import com.sonora.theme.field
import com.sonora.theme.SonoraShapes

/**
 * M12 sheet behind the player's + button: the liked toggle, every
 * playlist with membership state, and inline playlist creation
 * (which adds the song straight away). The heart stays the direct
 * like; this is the save-everywhere panel.
 */
@Composable
fun PlusSheet(
    trackKey: String,
    isLiked: Boolean,
    onToggleLike: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalSonoraColors.current
    val type = LocalSonoraTypography.current
    var naming by rememberSaveable { mutableStateOf(false) }
    var draft by rememberSaveable { mutableStateOf("") }
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(SonoraShapes.small)
                .clickable(
                    role = Role.Switch,
                    onClickLabel = if (isLiked) "Unlike this song" else "Like this song",
                    onClick = onToggleLike,
                )
                .padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = null,
                tint = colors.ivory,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(16.dp))
            Text(
                if (isLiked) "Liked" else "Like this song",
                style = type.body,
                color = colors.ivory,
                modifier = Modifier.weight(1f),
            )
            if (isLiked) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = colors.ivory, modifier = Modifier.size(20.dp))
            }
        }
        Text(
            "Playlists".uppercase(),
            style = type.labelCaps,
            color = colors.ivoryFaint,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
        PlaylistsStore.lists.forEach { playlist ->
            val member = PlaylistsStore.contains(playlist.id, trackKey)
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(SonoraShapes.small)
                    .clickable(
                        role = Role.Checkbox,
                        onClickLabel = if (member) "Remove from ${playlist.name}" else "Add to ${playlist.name}",
                        onClick = { PlaylistsStore.toggleTrack(playlist.id, trackKey) },
                    )
                    .padding(horizontal = 12.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        playlist.name,
                        style = type.body,
                        color = colors.ivory,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        if (playlist.keys.size == 1) "1 song" else "${playlist.keys.size} songs",
                        style = type.caption,
                        color = colors.ivoryDim,
                    )
                }
                if (member) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = colors.ivory, modifier = Modifier.size(20.dp))
                }
            }
        }
        if (naming) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Playlist name", style = type.body) },
                    singleLine = true,
                    shape = SonoraShapes.small,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = colors.field,
                        unfocusedContainerColor = colors.field,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = colors.ivory,
                        unfocusedTextColor = colors.ivory,
                        cursorColor = colors.ivory,
                    ),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Create".uppercase(),
                    style = type.labelCaps,
                    color = if (draft.isBlank()) colors.ivoryFaint else colors.contentInverse,
                    modifier = Modifier
                        .clip(SonoraShapes.small)
                        .background(
                            if (draft.isBlank()) colors.content.copy(alpha = 0.10f) else colors.content,
                        )
                        .clickable(
                            role = Role.Button,
                            onClickLabel = "Create playlist",
                            enabled = draft.isNotBlank(),
                            onClick = {
                                PlaylistsStore.create(draft)?.let {
                                    PlaylistsStore.addTrack(it.id, trackKey)
                                }
                                draft = ""
                                naming = false
                            },
                        )
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                )
            }
        } else {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(SonoraShapes.small)
                    .clickable(role = Role.Button, onClickLabel = "New playlist", onClick = { naming = true })
                    .padding(horizontal = 12.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, tint = colors.ivory, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(16.dp))
                Text("New playlist", style = type.body, color = colors.ivory)
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}
