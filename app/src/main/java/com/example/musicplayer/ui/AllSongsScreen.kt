package com.example.musicplayer.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.musicplayer.model.Song
import com.example.musicplayer.ui.components.SongSearchBar
import com.example.musicplayer.viewmodel.PlayerViewModel
import com.example.musicplayer.viewmodel.SortType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllSongsScreen(
    viewModel: PlayerViewModel,
    onBack: () -> Unit
) {
    // ViewModel から全曲のリストを受け取る
    // allSongs の代わりに、加工済みの displayedSongs を使う
    val displayedSongs by viewModel.displayedSongs.collectAsStateWithLifecycle()
    // 💡 選択中のフォルダ名を取得（タイトルに使うため）
    val selectedFolder by viewModel.selectedFolder.collectAsStateWithLifecycle()
    // ソートの状態
    val sortType by viewModel.sortType.collectAsStateWithLifecycle()
    // 💡 ① ViewModel から検索ワードの状態を取得する
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    // 💡 現在再生中の曲と再生状態を取得
    val currentMediaItem by viewModel.currentMediaItem.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val currentSongId = currentMediaItem?.mediaId?.toLongOrNull()

    // ソートメニューの開閉状態
    var showSortMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                // 💡 タイトルを「すべての曲」から「選択したフォルダ名」に変更
                title = { Text(selectedFolder) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "戻る"
                        )
                    }
                },
                actions = {
                    // 💡 ② 全く同じソートメニューを配置！
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(imageVector = Icons.Filled.Sort, contentDescription = "並び替え")
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            SortType.entries.forEach { type ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = type.displayName,
                                            color = if (sortType == type) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    onClick = {
                                        viewModel.updateSortType(type)
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 💡 ② リストの直上に検索バーを配置！
            SongSearchBar(
                query = searchQuery,
                onQueryChange = { newQuery ->
                    viewModel.updateSearchQuery(newQuery)
                }
            )

            // ── 曲リスト ──
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(displayedSongs, key = { it.id }) { song ->
                    val isCurrentTrack = (song.id == currentSongId)
                    SongListItem(
                        song = song,
                        isCurrentTrack = isCurrentTrack,
                        isPlaying = isPlaying,
                        onClick = {
                            // 💡 画面遷移せず、その場で再生を開始する
                            viewModel.playSongFromLibrary(song.id)
                        }
                    )
                }
            }
        }
    }
}

/**
 * 1曲分の表示UI
 */
@Composable
private fun SongListItem(
    song: Song,
    isCurrentTrack: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isCurrentTrack) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
    } else {
        Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isCurrentTrack) {
            // 💡 再生中の曲はイコライザーアニメーションアイコンを表示
            EqualizerAnimation(
                isPlaying = isPlaying,
                modifier = Modifier.size(40.dp),
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleMedium,
                color = if (isCurrentTrack) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${song.artist} • ${song.album}",
                style = MaterialTheme.typography.bodySmall,
                color = if (isCurrentTrack) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * EqualizerAnimation
 *
 * 再生中の曲を示すイコライザー風のアニメーションアイコン。
 * 4本のバーがリズミカルに上下する。
 */
@Composable
fun EqualizerAnimation(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val transition = rememberInfiniteTransition(label = "equalizer")

    val bar1Height by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 420, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar1"
    )
    val bar2Height by transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 320, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar2"
    )
    val bar3Height by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar3"
    )
    val bar4Height by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 380, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar4"
    )

    val heights = if (isPlaying) {
        listOf(bar1Height, bar2Height, bar3Height, bar4Height)
    } else {
        listOf(0.4f, 0.7f, 0.5f, 0.3f)
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.size(24.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            heights.forEach { heightRatio ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(heightRatio)
                        .background(color = color, shape = RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                )
            }
        }
    }
}