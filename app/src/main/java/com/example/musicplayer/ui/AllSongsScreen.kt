package com.example.musicplayer.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.musicplayer.model.Song
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
    val availableFolders by viewModel.availableFolders.collectAsStateWithLifecycle()
    // 💡 選択中のフォルダ名を取得（タイトルに使うため）
    val selectedFolder by viewModel.selectedFolder.collectAsStateWithLifecycle()
    val sortType by viewModel.sortType.collectAsStateWithLifecycle()


    // ソートメニューの開閉状態
    var showSortMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                // 💡 タイトルを「すべての曲」から「選択したフォルダ名」に変更
                title = { Text(selectedFolder) },
                navigationIcon = { /* ... 既存の戻るボタン ... */ },
                actions = { /* ... 既存のソートメニュー ... */ }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // ✂️ ここにあった LazyRow（横スクロールのタブ）をまるごと削除します！
            /**
             * 横タブ
            LazyRow(
                modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(availableFolders) { folder ->
                    FilterChip(
                        selected = (folder == selectedFolder),
                        onClick = { viewModel.updateSelectedFolder(folder) },
                        label = { Text(folder) }
                    )
                }
            }
             */

            // ── 曲リスト ──
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(displayedSongs, key = { it.id }) { song ->
                    SongListItem(
                        song = song,
                        onClick = {
                            viewModel.playSongFromLibrary(song.id)
                            // 💡 曲を選んだら、プレーヤー画面まで一気に戻るようにする
                            // （今回は少し特殊な戻り方をするので、ここはonBack()のままで、MainActivity側で制御します）
                            onBack()
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
private fun SongListItem(song: Song, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.MusicNote,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(40.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${song.artist} • ${song.album}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}