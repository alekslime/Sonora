package com.sonora.browse

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sonora.data.Album
import com.sonora.data.rememberAlbumArtwork
import com.sonora.theme.LocalSonoraColors
import com.sonora.theme.LocalSonoraTypography
import com.sonora.theme.SonoraShapes

private enum class SortMode(val label: String) {
    TITLE("Title"),
    ARTIST("Artist"),
    YEAR("Year"),
}

/**
 * M1 compact browsing: search, sort and list/grid views over the library,
 * so large collections stay navigable without the spatial shelf.
 */
@Composable
fun BrowseScreen(
    albums: List<Album>,
    onOpenAlbum: (Album) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalSonoraColors.current
    val type = LocalSonoraTypography.current
    var query by rememberSaveable { mutableStateOf("") }
    var sort by rememberSaveable { mutableStateOf(SortMode.TITLE) }
    var grid by rememberSaveable { mutableStateOf(false) }
    BackHandler(onBack = onBack)

    val visible = albums
        .filter {
            query.isBlank() ||
                it.title.contains(query, ignoreCase = true) ||
                it.artist.contains(query, ignoreCase = true)
        }
        .let { list ->
            when (sort) {
                SortMode.TITLE -> list.sortedBy { it.title }
                SortMode.ARTIST -> list.sortedBy { it.artist }
                SortMode.YEAR -> list.sortedByDescending { it.year }
            }
        }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(colors.obsidian, colors.abyss))),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 20.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back to shelf", tint = colors.ivory)
            }
            Spacer(Modifier.width(4.dp))
            Text("Browse".uppercase(), style = type.labelCaps, color = colors.ivory)
            Spacer(Modifier.weight(1f))
            Text("${visible.size} records", style = type.caption, color = colors.ivoryFaint)
        }

        TextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            placeholder = { Text("Search title or artist", style = type.body) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            shape = SonoraShapes.small,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = colors.charcoal,
                unfocusedContainerColor = colors.charcoal,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = colors.ivory,
                unfocusedTextColor = colors.ivory,
                cursorColor = colors.amber,
            ),
        )

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SortMode.entries.forEach { mode ->
                FilterChip(
                    selected = sort == mode,
                    onClick = { sort = mode },
                    label = { Text(mode.label, style = type.caption) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color.Transparent,
                        labelColor = colors.ivoryDim,
                        selectedContainerColor = colors.amber.copy(alpha = 0.22f),
                        selectedLabelColor = colors.ivory,
                    ),
                )
            }
            Spacer(Modifier.weight(1f))
            ViewToggle(grid = grid, onToggle = { grid = it })
        }

        if (visible.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No records match.", style = type.body, color = colors.ivoryDim)
            }
            return
        }
        if (grid) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(20.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                items(visible, key = { it.id }) { album ->
                    GridCell(album = album, onOpen = { onOpenAlbum(album) })
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(visible, key = { it.id }) { album ->
                    ListRow(album = album, onOpen = { onOpenAlbum(album) })
                }
            }
        }
    }
}

@Composable
private fun ViewToggle(grid: Boolean, onToggle: (Boolean) -> Unit) {
    val colors = LocalSonoraColors.current
    val type = LocalSonoraTypography.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        listOf("List" to false, "Grid" to true).forEach { (label, value) ->
            val active = grid == value
            Text(
                label.uppercase(),
                style = type.labelCaps,
                color = if (active) colors.amber else colors.ivoryFaint,
                modifier = Modifier
                    .clip(SonoraShapes.extraSmall)
                    .clickable(role = Role.Tab) { onToggle(value) }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun ListRow(album: Album, onOpen: () -> Unit) {
    val colors = LocalSonoraColors.current
    val type = LocalSonoraTypography.current
    val art = rememberAlbumArtwork(album)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(SonoraShapes.small)
            .clickable(role = Role.Button, onClickLabel = "Open ${album.title}", onClick = onOpen)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(56.dp)
                .clip(SonoraShapes.extraSmall)
                .background(colors.graphite),
        ) {
            if (art != null) {
                Image(art, contentDescription = null, contentScale = ContentScale.Crop)
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                album.title,
                style = type.title,
                color = colors.ivory,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${album.artist}  •  ${album.year}",
                style = type.caption,
                color = colors.ivoryDim,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun GridCell(album: Album, onOpen: () -> Unit) {
    val colors = LocalSonoraColors.current
    val type = LocalSonoraTypography.current
    val art = rememberAlbumArtwork(album)
    Column(
        Modifier
            .clip(SonoraShapes.small)
            .clickable(role = Role.Button, onClickLabel = "Open ${album.title}", onClick = onOpen),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(SonoraShapes.small)
                .background(colors.graphite),
        ) {
            if (art != null) {
                Image(
                    art,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            album.title,
            style = type.title,
            color = colors.ivory,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            "${album.artist}  •  ${album.year}",
            style = type.caption,
            color = colors.ivoryDim,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
