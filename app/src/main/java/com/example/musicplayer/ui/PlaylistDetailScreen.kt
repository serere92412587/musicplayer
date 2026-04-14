package com.example.musicplayer.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.musicplayer.model.Playlist
import com.example.musicplayer.model.Song
import com.example.musicplayer.viewmodel.PlayerViewModel

/**
 * PlaylistDetailScreen
 *
 * プレイリストの詳細画面。
 * 選択したプレイリストに含まれる曲の一覧を表示し、
 * 再生・曲の追加・削除ができる。
 *
 * @param viewModel       PlayerViewModel のインスタンス
 * @param playlistId      表示するプレイリストのID
 * @param onBack          戻るボタンのコールバック
 * @param onSettingsClick タイマー設定ボタンのコールバック
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    viewModel: PlayerViewModel,
    playlistId: Long,
    onBack: () -> Unit,
    onSettingsClick: () -> Unit
) {
    // ─────────────────────────────────────────
    // 状態の収集
    // ─────────────────────────────────────────

    // 型を明示することで collectAsStateWithLifecycle の型推論エラーを防ぐ
    val playlistSongs: List<Song> by viewModel.playlistSongs.collectAsStateWithLifecycle()
    val isLoadingSongs: Boolean by viewModel.isLoadingSongs.collectAsStateWithLifecycle()
    val allSongs: List<Song> by viewModel.allSongs.collectAsStateWithLifecycle()
    val playlists: List<Playlist> by viewModel.playlists.collectAsStateWithLifecycle()

    // 現在表示しているプレイリストの情報（名前の表示に使う）
    val currentPlaylist: Playlist? = playlists.find { it.id == playlistId }

    // ─────────────────────────────────────────
    // ローカルの UI 状態
    // ─────────────────────────────────────────

    var showAddSongsDialog by remember { mutableStateOf(false) }
    var songToRemove by remember { mutableStateOf<Song?>(null) }

    // ─────────────────────────────────────────
    // 副作用：画面表示時にプレイリストの曲を読み込む
    // ─────────────────────────────────────────

    /**
     * LaunchedEffect
     *
     * Composable が最初に表示されたとき（または key が変化したとき）に
     * 一度だけ実行されるコルーチンブロック。
     * key に playlistId を渡しているので、
     * 別のプレイリストに遷移したときも再実行される。
     */
    LaunchedEffect(key1 = playlistId) {
        viewModel.loadPlaylistSongs(playlistId)
    }

    // ─────────────────────────────────────────
    // ダイアログ
    // ─────────────────────────────────────────

    if (showAddSongsDialog) {
        AddSongsDialog(
            allSongs = allSongs,
            currentSongIds = playlistSongs.map { song: Song -> song.id }.toSet(),
            onConfirm = { selectedSongIds: List<Long> ->
                viewModel.addSongsToPlaylist(playlistId, selectedSongIds)
                showAddSongsDialog = false
            },
            onDismiss = { showAddSongsDialog = false }
        )
    }

    songToRemove?.let { song: Song ->
        AlertDialog(
            onDismissRequest = { songToRemove = null },
            title = { Text("曲を削除") },
            text = { Text("「${song.title}」をこのプレイリストから削除しますか？") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.removeSongFromPlaylist(playlistId, song.id)
                    songToRemove = null
                }) {
                    Text("削除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { songToRemove = null }) {
                    Text("キャンセル")
                }
            }
        )
    }

    // ─────────────────────────────────────────
    // レイアウト
    // ─────────────────────────────────────────

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = currentPlaylist?.name ?: "プレイリスト",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "戻る"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "タイマー設定"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddSongsDialog = true }) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "曲を追加"
                )
            }
        }
    ) { innerPadding ->

        when {
            // ─ 読み込み中 ─
            isLoadingSongs -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    /**
                     * CircularProgressIndicator
                     * くるくる回るローディングインジケーター。
                     * 読み込み中であることをユーザーに伝える。
                     */
                    CircularProgressIndicator()
                }
            }

            // ─ 曲が0件 ─
            playlistSongs.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.MusicNote,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "曲が追加されていません",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "右下の「+」ボタンで追加できます",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ─ 曲がある場合 ─
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    PlaybackButtonsRow(
                        onPlayAll = {
                            viewModel.playPlaylist(playlistId, shuffle = false)
                        },
                        onShuffle = {
                            viewModel.playPlaylist(playlistId, shuffle = true)
                        }
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        /**
                         * itemsIndexed
                         *
                         * items() と同じだが、インデックス（番号）も一緒に受け取れる。
                         * 曲番号の表示に使う。
                         * ラムダのパラメータ型を明示して型推論エラーを防ぐ。
                         */
                        itemsIndexed(
                            items = playlistSongs,
                            key = { _: Int, song: Song -> song.id }
                        ) { index: Int, song: Song ->
                            SongItem(
                                song = song,
                                index = index + 1,
                                onClick = {
                                    viewModel.playPlaylist(
                                        playlistId = playlistId,
                                        startIndex = index
                                    )
                                },
                                onDeleteClick = { songToRemove = song }
                            )
                        }

                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// サブ Composable
// ─────────────────────────────────────────────────────────────

/**
 * PlaybackButtonsRow
 *
 * 「全曲再生」「シャッフル再生」ボタンを横並びに表示する。
 */
@Composable
private fun PlaybackButtonsRow(
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier
                .weight(1f)
                .clickable { onPlayAll() },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "全曲再生",
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        Card(
            modifier = Modifier
                .weight(1f)
                .clickable { onShuffle() },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Shuffle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "シャッフル",
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

/**
 * SongItem
 *
 * プレイリスト内の曲を1行で表示する。
 *
 * @param song          表示する曲
 * @param index         プレイリスト内での番号（表示用・1始まり）
 * @param onClick       タップしたときのコールバック
 * @param onDeleteClick 削除ボタンのコールバック
 */
@Composable
private fun SongItem(
    song: Song,
    index: Int,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 曲番号
        Text(
            text = "$index",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(28.dp)
        )

        // 曲名・アーティスト名
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // 再生時間
        Text(
            text = song.duration.toTimeString(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // 削除ボタン
        IconButton(onClick = onDeleteClick) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = "プレイリストから削除",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * AddSongsDialog
 *
 * 端末内の全曲からプレイリストに追加する曲を選ぶダイアログ。
 * チェックボックスで複数選択できる。
 *
 * @param allSongs       端末内の全曲リスト
 * @param currentSongIds 既にプレイリストに含まれている曲のIDセット
 * @param onConfirm      「追加」ボタンが押されたときのコールバック
 * @param onDismiss      ダイアログを閉じるコールバック
 */
@Composable
private fun AddSongsDialog(
    allSongs: List<Song>,
    currentSongIds: Set<Long>,
    onConfirm: (List<Long>) -> Unit,
    onDismiss: () -> Unit
) {
    /**
     * mutableStateListOf
     *
     * Compose が変化を検知できるミュータブルなリスト。
     * 通常の MutableList では Compose が変化に気づかないため、
     * こちらを使う必要がある。
     */
    val selectedIds = remember { mutableStateListOf<Long>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("曲を追加") },
        text = {
            LazyColumn {
                // count と key を使う形式で型推論エラーを回避する
                items(
                    count = allSongs.size,
                    key = { index: Int -> allSongs[index].id }
                ) { index: Int ->
                    val song: Song = allSongs[index]
                    val isAlreadyAdded: Boolean = song.id in currentSongIds
                    val isSelected: Boolean = song.id in selectedIds

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isAlreadyAdded) {
                                if (isSelected) {
                                    selectedIds.remove(song.id)
                                } else {
                                    selectedIds.add(song.id)
                                }
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isSelected || isAlreadyAdded,
                            onCheckedChange = null,
                            enabled = !isAlreadyAdded
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = song.title,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = if (isAlreadyAdded) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                            Text(
                                text = song.artist,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selectedIds.toList()) },
                enabled = selectedIds.isNotEmpty()
            ) {
                Text("追加（${selectedIds.size}曲）")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("キャンセル")
            }
        }
    )
}

/**
 * Long.toTimeString()
 * ミリ秒を「分:秒」形式に変換する拡張関数。
 */
private fun Long.toTimeString(): String {
    val totalSeconds = this / 1_000L
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:%02d".format(seconds)
}