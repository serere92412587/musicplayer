package com.example.musicplayer.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.musicplayer.model.TimerConfig
import com.example.musicplayer.viewmodel.PlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistSettingScreen(
    playlistId: Long,
    viewModel: PlayerViewModel,
    onBack: () -> Unit
) {
    // 全プレイリストから、対象のプレイリストを探す
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val targetPlaylist = playlists.find { it.id == playlistId }

    // プレイリストが見つからない場合（ロード中など）は真っ白にするかローディングを出す
    if (targetPlaylist == null) {
        return
    }

    // 編集用のローカルステート（初期値はDBに保存されている現在の設定）
    var isEnabled by remember(targetPlaylist) { mutableStateOf(targetPlaylist.timerConfig.enabled) }
    var durationText by remember(targetPlaylist) { mutableStateOf(targetPlaylist.timerConfig.durationMin.toString()) }
    var fadeOutText by remember(targetPlaylist) { mutableStateOf(targetPlaylist.timerConfig.fadeOutSec.toString()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${targetPlaylist.name} の設定") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // ── 1. タイマーの ON/OFF スイッチ ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("スリープタイマーを有効にする", style = MaterialTheme.typography.titleMedium)
                Switch(
                    checked = isEnabled,
                    onCheckedChange = { isEnabled = it }
                )
            }

            // ── 2. タイマーの詳細設定（ONのときだけ表示される） ──
            AnimatedVisibility(visible = isEnabled) {
                Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {

                    // 停止までの時間入力（120分まで）
                    OutlinedTextField(
                        value = durationText,
                        onValueChange = { newValue ->
                            // ① 数字以外の入力を弾く（コピペ対策）
                            val filtered = newValue.filter { it.isDigit() }
                            // ② 数字に変換して上限をチェックする
                            val intValue = filtered.toIntOrNull() ?: 0

                            if (filtered.isEmpty()) {
                                durationText = "" // 空欄は許可する（入力中のため）
                            } else if (intValue <= 120) {
                                durationText = intValue.toString() // 120以下ならそのまま更新（0埋め防止）
                            } else {
                                durationText = "120" // 120を超えたら強制的に120にする
                            }
                        },
                        label = { Text("停止までの時間") },
                        suffix = { Text("分") },
                        // ③ ここで「数字だけのキーボード」を強制！
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // フェードアウト時間入力（60秒まで）
                    OutlinedTextField(
                        value = fadeOutText,
                        onValueChange = { newValue ->
                            val filtered = newValue.filter { it.isDigit() }
                            val intValue = filtered.toIntOrNull() ?: 0

                            if (filtered.isEmpty()) {
                                fadeOutText = ""
                            } else if (intValue <= 60) {
                                fadeOutText = intValue.toString()
                            } else {
                                fadeOutText = "60"
                            }
                        },
                        label = { Text("フェードアウト時間") },
                        suffix = { Text("秒") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // ── 3. 保存ボタン ──
            Button(
                onClick = {
                    // 新しい設定を持ったプレイリストのコピーを作成して更新
                    val updatedConfig = TimerConfig(
                        enabled = isEnabled,
                        // 空欄やエラーの場合はデフォルトで 60分 / 30秒 にする安全設計
                        durationMin = durationText.toIntOrNull() ?: 60,
                        fadeOutSec = fadeOutText.toIntOrNull() ?: 30
                    )
                    val updatedPlaylist = targetPlaylist.copy(timerConfig = updatedConfig)

                    viewModel.updatePlaylist(updatedPlaylist)
                    onBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("設定を保存")
            }
        }
    }
}