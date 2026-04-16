package com.example.musicplayer.ui

import androidx.benchmark.traceprocessor.Row
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.musicplayer.viewmodel.PlayerViewModel
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import com.example.musicplayer.viewmodel.SortType
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSongsListScreen(
    playlistId: Long,
    viewModel: PlayerViewModel,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    val displayedSongs by viewModel.displayedSongs.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedIdsForAdd.collectAsStateWithLifecycle()
    val selectedFolder by viewModel.selectedFolder.collectAsStateWithLifecycle()

    // 💡 ① ソートメニューの状態を取得
    val sortType by viewModel.sortType.collectAsStateWithLifecycle()
    var showSortMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$selectedFolder の曲") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                },
                actions = {
                    // 💡 ② ソートボタンとメニューを追加！
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
                                            // 選択中のものは色を変える
                                            color = if (sortType == type) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    onClick = {
                                        // ViewModelのソート状態を更新
                                        viewModel.updateSortType(type)
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // 右上の保存ボタン（既存）
                    TextButton(onClick = {
                        viewModel.addSongsToPlaylist(playlistId, selectedIds.toList())
                        onSave()
                    }) {
                        Text("保存", style = MaterialTheme.typography.titleMedium)
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            items(displayedSongs, key = { it.id }) { song ->
                val isSelected = selectedIds.contains(song.id)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.toggleSongSelectionForAdd(song.id) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = isSelected, onCheckedChange = null)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(text = song.title, style = MaterialTheme.typography.titleMedium)
                        Text(text = song.artist, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}