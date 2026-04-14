package com.example.musicplayer.model

/**
 * TimerConfig
 *
 * プレイリストに紐付くスリープタイマーの設定を表すデータクラス。
 *
 * ─ なぜ Playlist と別クラスにするのか ─
 * タイマーの設定項目は今後増える可能性がある（例：曜日指定、繰り返し等）。
 * Playlist クラスに直接書くと肥大化するため、責務を分けて別クラスにする。
 * Playlist クラスはこのクラスをプロパティとして持つ形になる。
 *
 * ─ Room での扱い ─
 * このクラスは Room のテーブルにはならず、Playlist テーブルの中に
 * 埋め込まれる形（@Embedded）で保存される。
 * （詳しくは AppDatabase.kt のコメントを参照）
 *
 * @param enabled      タイマーが有効かどうか。false なら他の設定は無視される。
 * @param durationMin  タイマーの時間（分）。再生開始からこの分数後に停止する。
 *                     例：60 なら60分後に停止。
 * @param fadeOutSec   停止前にフェードアウトする秒数。
 *                     例：30 なら30秒かけて音量をゼロに下げてから停止する。
 *                     0 なら即座に停止。
 */
data class TimerConfig(
    val enabled: Boolean = false,
    val durationMin: Int = 60,
    val fadeOutSec: Int = 30
)
