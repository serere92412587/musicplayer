package com.example.musicplayer.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.musicplayer.model.Song
import com.example.musicplayer.viewmodel.PlayerViewModel
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.FilterChip
import org.checkerframework.checker.units.qual.UnknownUnits

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: Long,
    viewModel: PlayerViewModel,
    onBack: () -> Unit,
    onPlay: () -> Unit, //再生時のコールバック
    onAddSongsClick: () -> Unit
    ) {
    // 画面が開かれたときに、このプレイリストの曲を読み込む
    LaunchedEffect(playlistId) {
        viewModel.loadPlaylistSongs(playlistId)
    }

    // プレイリスト本体の情報
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val playlist = playlists.find { it.id == playlistId }

    // プレイリスト内の曲リスト
    val playlistSongs: List<Song> by viewModel.playlistSongs.collectAsStateWithLifecycle()
    // 全曲リスト（追加ダイアログ用）
    val allSongs: List<Song> by viewModel.allSongs.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(playlist?.name ?: "プレイリスト") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                // 💡 ダイアログを出す代わりに、フォルダ選択画面へ Go!
                viewModel.clearSelectionForAdd(playlistSongs.map { it.id }.toSet())
                onAddSongsClick()
            }) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "曲を追加")
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (playlistSongs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("曲がありません。右下のボタンから追加してください。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(playlistSongs, key = { it.id }) { song ->
                        PlaylistSongItem(
                            song = song,
                            onClick = {
                                // 💡 ② 再生処理のあとに onPlay() を呼ぶ
                                viewModel.playPlaylist(
                                    playlistId,
                                    shuffle = false,
                                    startIndex = playlistSongs.indexOf(song)
                                )
                                onPlay()
                            },
                            onRemoveClick = {
                                // プレイリストから削除
                                viewModel.removeSongFromPlaylist(playlistId, song.id)
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * プレイリスト内の1曲を表示するUI
 */
@Composable
private fun PlaylistSongItem(song: Song, onClick: () -> Unit, onRemoveClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = Icons.Filled.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = song.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text = song.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
        // リストから外すボタン
        IconButton(onClick = onRemoveClick) {
            Icon(imageVector = Icons.Filled.Delete, contentDescription = "プレイリストから外す", tint = MaterialTheme.colorScheme.error)
        }
    }
}
