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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.musicplayer.model.Playlist
import com.example.musicplayer.viewmodel.PlayerViewModel

/**
 * PlaylistListScreen
 *
 * プレイリストの一覧を表示するメイン画面。
 *
 * ─ この画面でできること ─
 *  - プレイリストの一覧表示
 *  - プレイリストをタップして再生開始
 *  - 右下の「+」ボタンで新規プレイリスト作成
 *  - プレイリストを長押しして削除
 *  - プレイリストの設定アイコンから設定画面へ遷移
 *
 * @param viewModel        PlayerViewModel のインスタンス
 * @param onPlaylistClick  プレイリストをタップしたときのコールバック（詳細画面へ遷移）
 * @param onSettingsClick  設定アイコンをタップしたときのコールバック（設定画面へ遷移）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistListScreen(
    viewModel: PlayerViewModel,
    onPlaylistClick: (Long) -> Unit,
    onSettingsClick: (Long) -> Unit
) {
    // ─────────────────────────────────────────
    // 状態の収集
    // ─────────────────────────────────────────

    // DBから流れてくるプレイリスト一覧を Compose の State として受け取る
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()

    // ─────────────────────────────────────────
    // ローカルの UI 状態
    // ─────────────────────────────────────────

    // 新規作成ダイアログの表示フラグ
    var showCreateDialog by remember { mutableStateOf(false) }

    // 削除確認ダイアログの対象プレイリスト（null = ダイアログ非表示）
    var playlistToDelete by remember { mutableStateOf<Playlist?>(null) }

    // ─────────────────────────────────────────
    // ダイアログ
    // ─────────────────────────────────────────

    // 新規プレイリスト作成ダイアログ
    if (showCreateDialog) {
        CreatePlaylistDialog(
            onConfirm = { name ->
                // ViewModel に作成を依頼して、ダイアログを閉じる
                viewModel.createPlaylist(name)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false }
        )
    }

    // 削除確認ダイアログ
    playlistToDelete?.let { playlist ->
        DeleteConfirmDialog(
            playlistName = playlist.name,
            onConfirm = {
                viewModel.deletePlaylist(playlist)
                playlistToDelete = null
            },
            onDismiss = { playlistToDelete = null }
        )
    }

    // ─────────────────────────────────────────
    // レイアウト
    // ─────────────────────────────────────────

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("プレイリスト") }
            )
        },
        // 右下に「+」ボタン（FAB = Floating Action Button）を配置
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true }
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "プレイリストを作成"
                )
            }
        }
    ) { innerPadding ->

        if (playlists.isEmpty()) {
            // ─ プレイリストが0件の場合 ─
            // 空状態の案内メッセージを画面中央に表示する
            EmptyPlaylistMessage(
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            // ─ プレイリストがある場合 ─
            // LazyColumn でリストを表示する
            /**
             * LazyColumn
             *
             * スクロール可能な縦リスト。
             * 通常の Column と違い、画面に表示される部分だけを描画するため
             * 大量のアイテムがあってもパフォーマンスが落ちない。
             * RecyclerView の Compose 版にあたる。
             */
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp) // アイテム間の余白
            ) {
                // items() にリストを渡すと、各要素に対してブロック内が呼ばれる
                // key を指定することで、リストが更新されたときに
                // 変化した部分だけ再コンポーズされるようになる（パフォーマンス向上）
                items(playlists, key = { it.id }) { playlist ->
                    PlaylistItem(
                        playlist = playlist,
                        onClick = { onPlaylistClick(playlist.id) },
                        onDeleteClick = { playlistToDelete = playlist },
                        onSettingsClick = { onSettingsClick(playlist.id) }
                    )
                }

                // FAB が最後のアイテムに被らないよう、下に余白を追加
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// サブ Composable
// ─────────────────────────────────────────────────────────────

/**
 * PlaylistItem
 *
 * プレイリスト一覧の1行分の UI。
 *
 * @param playlist       表示するプレイリスト
 * @param onClick        行をタップしたときのコールバック
 * @param onDeleteClick  削除ボタンをタップしたときのコールバック
 * @param onSettingsClick 設定ボタンをタップしたときのコールバック
 */
@Composable
private fun PlaylistItem(
    playlist: Playlist,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    /**
     * Card
     *
     * Material3 のカードコンポーネント。
     * 影（elevation）と角丸で囲まれた見た目のコンテナ。
     */
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }, // カード全体をタップ可能にする
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左側：音楽アイコン
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )

            // 中央：プレイリスト名とタイマー情報
            Column(
                modifier = Modifier
                    .weight(1f) // 残りのスペースを全て使う
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                // タイマー設定の状態を小さく表示する
                Text(
                    text = if (playlist.timerConfig.enabled) {
                        "タイマー: ${playlist.timerConfig.durationMin}分後に停止"
                    } else {
                        "タイマーなし"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (playlist.timerConfig.enabled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            // 右側：削除ボタン
            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "削除",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/**
 * EmptyPlaylistMessage
 *
 * プレイリストが1件もないときに表示するメッセージ。
 * 画面中央に案内文を出して、ユーザーが何をすればいいかわかるようにする。
 */
@Composable
private fun EmptyPlaylistMessage(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
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
                text = "プレイリストがありません",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "右下の「+」ボタンで作成できます",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * CreatePlaylistDialog
 *
 * 新規プレイリスト作成ダイアログ。
 * プレイリスト名を入力して「作成」を押すと onConfirm が呼ばれる。
 *
 * @param onConfirm 「作成」ボタンが押されたときのコールバック（入力された名前を渡す）
 * @param onDismiss ダイアログを閉じるコールバック
 */
@Composable
private fun CreatePlaylistDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    // テキストフィールドの入力内容を保持する State
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新しいプレイリスト") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("プレイリスト名") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                // 名前が空欄のときは「作成」ボタンを無効化する
                onClick = { if (name.isNotBlank()) onConfirm(name.trim()) },
                enabled = name.isNotBlank()
            ) {
                Text("作成")
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
 * DeleteConfirmDialog
 *
 * プレイリスト削除の確認ダイアログ。
 * 誤操作で削除してしまわないように、確認ステップを挟む。
 *
 * @param playlistName 削除対象のプレイリスト名（ダイアログに表示する）
 * @param onConfirm    「削除」ボタンが押されたときのコールバック
 * @param onDismiss    「キャンセル」が押されたときのコールバック
 */
@Composable
private fun DeleteConfirmDialog(
    playlistName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("プレイリストを削除") },
        text = { Text("「$playlistName」を削除しますか？\nこの操作は元に戻せません。") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = "削除",
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("キャンセル")
            }
        }
    )
}