package com.example.musicplayer.timer

import androidx.media3.common.Player
import kotlinx.coroutines.*

/**
 * SleepTimerManager
 *
 * スリープタイマーのカウントダウンと、フェードアウト処理を担当するクラス。
 */
class SleepTimerManager(private val coroutineScope: CoroutineScope) {

    private var timerJob: Job? = null
    private val defaultVolume = 1.0f // ExoPlayerの最大音量（1.0 = 100%）

    /**
     * タイマーを開始する
     *
     * @param player      操作対象のプレイヤー（MediaController）
     * @param durationMin 停止までの時間（分）
     * @param fadeOutSec  フェードアウトにかける時間（秒）
     */
    fun startTimer(player: Player, durationMin: Int, fadeOutSec: Int) {
        // 既に動いているタイマーがあればキャンセルしてリセット
        cancelTimer(player)

        timerJob = coroutineScope.launch(Dispatchers.Main) {
            val durationMs = durationMin * 60 * 1000L
            val fadeOutMs = fadeOutSec * 1000L

            // フェードアウトを開始するまでの待機時間
            val delayBeforeFade = durationMs - fadeOutMs

            // ① 指定時間まで待機
            if (delayBeforeFade > 0) {
                delay(delayBeforeFade)
            }

            // ② ── フェードアウト処理 ──
            val initialVolume = player.volume
            val steps = 20 // 20段階で滑らかに音量を下げる
            val stepDelay = fadeOutMs / steps
            val volumeStep = initialVolume / steps

            for (i in 1..steps) {
                if (!isActive) return@launch // 途中でキャンセルされたらループを抜ける

                // 音量を段階的に下げる（0.0未満にならないように coerceAtLeast でガード）
                player.volume = (initialVolume - (volumeStep * i)).coerceAtLeast(0f)
                delay(stepDelay)
            }

            // ③ フェードアウト完了：一時停止して、次回の再生のために音量を元に戻す
            player.pause()
            player.volume = defaultVolume
        }
    }

    /**
     * タイマーをキャンセル（手動で止めた場合や、別の曲を再生した場合）
     */
    fun cancelTimer(player: Player?) {
        timerJob?.cancel()
        timerJob = null
        // 音量が下がっている途中でキャンセルされた場合に備えて、音量をリセット
        player?.volume = defaultVolume
    }
}