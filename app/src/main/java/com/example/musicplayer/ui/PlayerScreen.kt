package com.example.musicplayer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import com.example.musicplayer.viewmodel.PlayerViewModel

/**
 * PlayerScreen
 *
 * 音楽プレーヤーのメイン画面全体を構成する Composable。
 *
 * ─ Composable とは ─
 * Jetpack Compose における UI の構成単位。
 * @Composable アノテーションを付けた関数が UI のパーツになる。
 * 状態（State）が変化すると、その関数が自動で再実行されて画面が更新される。
 * （これを「再コンポーズ」という）
 *
 * ─ この画面の構成 ─
 * ┌─────────────────────┐
 * │   曲名・アーティスト名   │
 * │   ─────────────────  │
 * │   シークバー            │
 * │   再生時間 / 総時間     │
 * │   ─────────────────  │
 * │  |◀  ▶/‖  ▶|        │
 * │   ─────────────────  │
 * │  シャッフル  リピート    │
 * └─────────────────────┘
 *
 * @param viewModel PlayerViewModel のインスタンス。
 *                  画面に表示するデータと操作メソッドを持つ。
 */
@Composable
fun PlayerScreen(viewModel: PlayerViewModel) {

    // ─────────────────────────────────────────
    // 状態の収集（StateFlow → Compose State への変換）
    // ─────────────────────────────────────────

    /**
     * collectAsStateWithLifecycle()
     *
     * ViewModel の StateFlow を Compose の State に変換する。
     * ライフサイクルを考慮した収集で、画面が非表示のときは収集を止めて
     * バッテリーを節約してくれる（collectAsState() より推奨）。
     *
     * by キーワードで委譲することで、.value を省略して変数として使える。
     */
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val isShuffleOn by viewModel.isShuffleOn.collectAsStateWithLifecycle()
    val repeatMode by viewModel.repeatMode.collectAsStateWithLifecycle()
    val currentPositionMs by viewModel.currentPositionMs.collectAsStateWithLifecycle()
    val durationMs by viewModel.durationMs.collectAsStateWithLifecycle()
    val currentMediaItem by viewModel.currentMediaItem.collectAsStateWithLifecycle()

    // ─────────────────────────────────────────
    // レイアウト
    // ─────────────────────────────────────────

    /**
     * Column
     *
     * 子要素を縦に並べるレイアウト。
     * fillMaxSize() で画面全体を使い、padding で余白を確保する。
     */
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {

        // ── 曲情報エリア ──────────────────────
        TrackInfoSection(
            title = currentMediaItem?.mediaMetadata?.title?.toString() ?: "曲を選択してください",
            artist = currentMediaItem?.mediaMetadata?.artist?.toString() ?: "アーティスト不明"
        )

        // ── シークバーエリア ──────────────────
        SeekBarSection(
            currentPositionMs = currentPositionMs,
            durationMs = durationMs,
            onSeek = { positionMs -> viewModel.seekTo(positionMs) }
        )

        // ── 再生コントロールエリア ────────────
        PlaybackControlsSection(
            isPlaying = isPlaying,
            onPlayPause = { viewModel.togglePlayPause() },
            onSkipNext = { viewModel.skipToNext() },
            onSkipPrevious = { viewModel.skipToPrevious() }
        )

        // ── シャッフル・リピートエリア ────────
        ShuffleRepeatSection(
            isShuffleOn = isShuffleOn,
            repeatMode = repeatMode,
            onShuffleClick = { viewModel.toggleShuffle() },
            onRepeatClick = { viewModel.cycleRepeatMode() }
        )
    }
}

// ─────────────────────────────────────────────────────────────
// サブ Composable（各セクションを独立した関数に分割）
// ─────────────────────────────────────────────────────────────

/**
 * TrackInfoSection
 *
 * 曲名とアーティスト名を表示するセクション。
 *
 * ─ なぜ別関数に分けるのか ─
 * Compose では UI を小さな Composable に分割することで、
 * 状態が変わったときに変化した部分だけを再コンポーズできる（効率的）。
 * また、コードの見通しも良くなる。
 *
 * @param title  曲名
 * @param artist アーティスト名
 */
@Composable
private fun TrackInfoSection(title: String, artist: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // 曲名
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            // 長い曲名は末尾を「...」で省略する
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(4.dp))

        // アーティスト名（曲名より小さく、色も薄く）
        Text(
            text = artist,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * SeekBarSection
 *
 * シークバーと再生時間を表示するセクション。
 *
 * @param currentPositionMs 現在の再生位置（ミリ秒）
 * @param durationMs        曲の総時間（ミリ秒）
 * @param onSeek            シークバーを動かしたときのコールバック
 */
@Composable
private fun SeekBarSection(
    currentPositionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {

        /**
         * Slider（シークバー）
         *
         * value: 0f〜1f の範囲で現在位置を表す比率
         * durationMs が 0 のとき（曲未選択）は 0f にする
         */
        val progress = if (durationMs > 0L) {
            (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

        Slider(
            value = progress,
            onValueChange = { newProgress ->
                // スライダーの値（0f〜1f）をミリ秒に変換して ViewModel に渡す
                val newPositionMs = (newProgress * durationMs).toLong()
                onSeek(newPositionMs)
            },
            modifier = Modifier.fillMaxWidth()
        )

        // 再生時間の表示（左：現在位置、右：総時間）
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = currentPositionMs.toTimeString(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = durationMs.toTimeString(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * PlaybackControlsSection
 *
 * 前の曲・再生/停止・次の曲ボタンを横並びに配置するセクション。
 *
 * @param isPlaying      現在再生中かどうか（ボタンのアイコン切り替えに使う）
 * @param onPlayPause    再生/停止ボタンが押されたときのコールバック
 * @param onSkipNext     次の曲ボタンが押されたときのコールバック
 * @param onSkipPrevious 前の曲ボタンが押されたときのコールバック
 */
@Composable
private fun PlaybackControlsSection(
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 前の曲ボタン
        IconButton(
            onClick = onSkipPrevious,
            modifier = Modifier.size(56.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.SkipPrevious,
                contentDescription = "前の曲",
                modifier = Modifier.size(36.dp)
            )
        }

        // 再生 / 一時停止ボタン（isPlaying の状態でアイコンが切り替わる）
        IconButton(
            onClick = onPlayPause,
            modifier = Modifier.size(72.dp)
        ) {
            Icon(
                // 再生中なら一時停止アイコン、停止中なら再生アイコン
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "一時停止" else "再生",
                modifier = Modifier.size(48.dp)
            )
        }

        // 次の曲ボタン
        IconButton(
            onClick = onSkipNext,
            modifier = Modifier.size(56.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.SkipNext,
                contentDescription = "次の曲",
                modifier = Modifier.size(36.dp)
            )
        }
    }
}

/**
 * ShuffleRepeatSection
 *
 * シャッフルボタンとリピートボタンを横並びに配置するセクション。
 *
 * foobar2000 で不満だった「切り替えが面倒」を解決する核心部分。
 * 各ボタンは1タップで状態が切り替わり、現在の状態が色で一目でわかる。
 *
 * @param isShuffleOn  シャッフルがONかどうか
 * @param repeatMode   現在のリピートモード（OFF / ONE / ALL）
 * @param onShuffleClick シャッフルボタンが押されたときのコールバック
 * @param onRepeatClick  リピートボタンが押されたときのコールバック
 */
@Composable
private fun ShuffleRepeatSection(
    isShuffleOn: Boolean,
    repeatMode: Int,
    onShuffleClick: () -> Unit,
    onRepeatClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {

        // ── シャッフルボタン ──────────────────

        /**
         * シャッフルボタンの色
         *
         * ONのとき  → テーマのプライマリカラー（目立つ色）
         * OFFのとき → グレーアウト（薄い色）
         *
         * これにより、ボタンを見ただけで現在の状態がわかる。
         */
        val shuffleColor = if (isShuffleOn) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(onClick = onShuffleClick) {
                Icon(
                    imageVector = Icons.Filled.Shuffle,
                    contentDescription = "シャッフル",
                    tint = shuffleColor,
                    modifier = Modifier.size(28.dp)
                )
            }
            // ボタンの下にラベルを表示
            Text(
                text = if (isShuffleOn) "シャッフルON" else "シャッフル",
                style = MaterialTheme.typography.labelSmall,
                color = shuffleColor
            )
        }

        // ── リピートボタン ────────────────────

        /**
         * リピートモードに応じてアイコンと色とラベルを切り替える
         *
         * REPEAT_MODE_OFF → グレー・Repeatアイコン・「リピートなし」
         * REPEAT_MODE_ONE → プライマリカラー・RepeatOneアイコン・「1曲リピート」
         * REPEAT_MODE_ALL → プライマリカラー・Repeatアイコン・「全曲リピート」
         */
        val (repeatIcon, repeatColor, repeatLabel) = when (repeatMode) {
            Player.REPEAT_MODE_ONE -> Triple(
                Icons.Filled.RepeatOne,
                MaterialTheme.colorScheme.primary,
                "1曲リピート"
            )
            Player.REPEAT_MODE_ALL -> Triple(
                Icons.Filled.Repeat,
                MaterialTheme.colorScheme.primary,
                "全曲リピート"
            )
            else -> Triple( // REPEAT_MODE_OFF
                Icons.Filled.Repeat,
                MaterialTheme.colorScheme.onSurfaceVariant,
                "リピートなし"
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(onClick = onRepeatClick) {
                Icon(
                    imageVector = repeatIcon,
                    contentDescription = "リピート切り替え",
                    tint = repeatColor,
                    modifier = Modifier.size(28.dp)
                )
            }
            Text(
                text = repeatLabel,
                style = MaterialTheme.typography.labelSmall,
                color = repeatColor
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 拡張関数
// ─────────────────────────────────────────────────────────────

/**
 * Long.toTimeString()
 *
 * ミリ秒を「分:秒」形式の文字列に変換する拡張関数。
 *
 * 拡張関数とは：既存のクラスにメソッドを追加できるKotlinの機能。
 * Long クラスを変更せずに toTimeString() メソッドを追加している。
 *
 * 例：
 *   90_000L.toTimeString() → "1:30"
 *   3_661_000L.toTimeString() → "61:01"（1時間超えも分で表示）
 */
private fun Long.toTimeString(): String {
    // ミリ秒 → 秒に変換（1000で割る）
    val totalSeconds = this / 1_000L
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    // %02d は「2桁ゼロ埋め」のフォーマット指定子（例：5秒 → "05"）
    return "$minutes:%02d".format(seconds)
}
