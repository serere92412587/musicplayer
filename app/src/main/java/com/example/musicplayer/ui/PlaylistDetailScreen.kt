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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.FilterChip
import org.checkerframework.checker.units.qual.UnknownUnits

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: Long,
    viewModel: PlayerViewModel,
    onBack: () -> Unit,
    onPlay: () -> Unit //再生時のコールバック
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

    var showAddDialog by remember { mutableStateOf(false) }

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
            // 曲追加ボタン
            FloatingActionButton(onClick = { showAddDialog = true }) {
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

    // ── 曲追加ダイアログ ──
    if (showAddDialog) {
        AddSongsDialog(
            allSongs = allSongs,
            // 既に追加されている曲は最初からチェックを入れておく
            initialSelectedSongIds = playlistSongs.map { it.id }.toSet(),
            onConfirm = { selectedIds ->
                viewModel.addSongsToPlaylist(playlistId, selectedIds.toList())
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
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

/**
 * 複数曲を選択できるダイアログ（フォルダ絞り込み機能付き）
 */
@Composable
private fun AddSongsDialog(
    allSongs: List<Song>,
    initialSelectedSongIds: Set<Long>,
    onConfirm: (Set<Long>) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedIds by remember { mutableStateOf(initialSelectedSongIds) }

    // 💡 フォルダ選択用の状態を追加
    var selectedFolder by remember { mutableStateOf("すべて") }

    // 💡 フォルダのリストを作成（"すべて" を先頭に）
    val availableFolders = remember(allSongs) {
        listOf("すべて") + allSongs.map { it.folderName }.distinct().sorted()
    }

    // 💡 選択したフォルダで曲を絞り込み
    val displayedSongs = remember(allSongs, selectedFolder) {
        if (selectedFolder == "すべて") {
            allSongs
        } else {
            allSongs.filter { it.folderName == selectedFolder }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("曲を追加") },
        text = {
            Column {
                // ── ① フォルダ選択タブ（横スクロール） ──
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(availableFolders) { folder ->
                        FilterChip(
                            selected = (folder == selectedFolder),
                            onClick = { selectedFolder = folder },
                            label = { Text(folder) }
                        )
                    }
                }

                // ── ② 曲リスト ──
                LazyColumn(
                    // 💡 weight(1f, fill=false) を指定し、曲が多くてもダイアログが画面外にはみ出さないようにする
                    modifier = Modifier.fillMaxWidth().weight(1f, fill = false)
                ) {
                    items(displayedSongs, key = { it.id }) { song ->
                        val isSelected = selectedIds.contains(song.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    // チェックのON/OFFを切り替え
                                    selectedIds = if (isSelected) {
                                        selectedIds - song.id
                                    } else {
                                        selectedIds + song.id
                                    }
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = null // 行全体のクリックで制御
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = song.title,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedIds) }) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("キャンセル")
            }
        }
    )
}