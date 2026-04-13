package com.example.musicplayer.service

import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * MusicService
 *
 * アプリがバックグラウンドに回っても音楽を再生し続けるためのサービス。
 *
 * ─ なぜ Service が必要なのか？ ─
 * Androidのアプリは画面（Activity）が非表示になると、OSがメモリ節約のために
 * プロセスを終了しようとする。Serviceを使うことで「まだ動いてます」とOSに伝え、
 * バックグラウンド再生を維持できる。
 *
 * ─ MediaSessionService を継承している理由 ─
 * Media3が提供する MediaSessionService を使うと、以下が自動的に処理される：
 *  - 通知（ノーティフィケーション）の表示と管理
 *  - イヤホンボタンや Bluetooth の再生/停止ボタンへの対応
 *  - Google Assistant などの外部アプリからの操作への対応
 *  - Android のメディアコントロール（ロック画面のコントローラー等）
 */
class MusicService : MediaSessionService() {

    // ─────────────────────────────────────────
    // プロパティ
    // ─────────────────────────────────────────

    /**
     * ExoPlayer
     *
     * 実際に音楽ファイルを読み込んで再生するエンジン。
     * Google製のオープンソース動画・音楽プレーヤーライブラリで、
     * MP3, FLAC, AAC, OGG など多くの形式に対応している。
     *
     * nullable（?付き）にしているのは、onCreate() で初期化して
     * onDestroy() で解放するライフサイクル管理のため。
     */
    private var player: ExoPlayer? = null

    /**
     * MediaSession
     *
     * ExoPlayer（実際の再生処理）と外部（通知、Bluetooth機器、他アプリ）を
     * 橋渡しするセッション管理オブジェクト。
     *
     * 「いま何の曲を再生中か」「再生状態はどうか」などの情報を
     * システム全体で共有するための仕組み。
     */
    private var mediaSession: MediaSession? = null

    // ─────────────────────────────────────────
    // ライフサイクル
    // ─────────────────────────────────────────

    /**
     * onCreate()
     *
     * Serviceが最初に起動したときに1回だけ呼ばれる。
     * ここでプレーヤーとセッションを初期化する。
     */
    override fun onCreate() {
        super.onCreate()

        // ExoPlayer を組み立てる
        player = buildPlayer()

        // MediaSession を作成し、ExoPlayer と紐付ける
        // これにより、通知やBluetooth経由での操作が ExoPlayer に届くようになる
        mediaSession = MediaSession.Builder(this, player!!)
            .build()
    }

    /**
     * onDestroy()
     *
     * Serviceが終了するときに呼ばれる（ユーザーがアプリを完全終了した場合など）。
     * ここでリソースを解放しないと、メモリリークの原因になる。
     */
    override fun onDestroy() {
        // MediaSession を解放（nullにして GC に任せる）
        mediaSession?.run {
            release()
            mediaSession = null
        }

        // ExoPlayer を停止して解放
        player?.run {
            stop()
            release()
            player = null
        }

        super.onDestroy()
    }

    // ─────────────────────────────────────────
    // MediaSessionService の必須オーバーライド
    // ─────────────────────────────────────────

    /**
     * onGetSession()
     *
     * MediaSessionService が「どの MediaSession を使うか」を
     * 外部（通知、他アプリ）に教えるためのメソッド。
     * 必ずオーバーライドが必要。
     *
     * @param controllerInfo 接続してきたコントローラーの情報（通知パネル等）
     * @return 現在のMediaSession（nullを返すと接続を拒否する）
     */
    override fun onGetSession(
        controllerInfo: MediaSession.ControllerInfo
    ): MediaSession? = mediaSession

    // ─────────────────────────────────────────
    // プレーヤー構築
    // ─────────────────────────────────────────

    /**
     * buildPlayer()
     *
     * ExoPlayer を設定込みで組み立てて返すプライベート関数。
     * Builder パターンで各種オプションを積み上げていく。
     */
    private fun buildPlayer(): ExoPlayer {
        // AudioAttributes: 「このアプリはどんな音を出すか」をOSに伝える設定
        // OSはこの情報をもとに、電話着信時のダッキング（音量自動調整）や
        // オーディオフォーカス（他のアプリが音を出したときの調整）を制御する
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)            // 用途：メディア再生（音楽・動画）
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC) // コンテンツ種別：音楽
            .build()

        return ExoPlayer.Builder(this)
            // オーディオフォーカスの自動管理を有効化
            // 例：電話がかかってきたら自動で一時停止し、通話後に再開する
            .setAudioAttributes(audioAttributes, /* handleAudioFocus = */ true)

            // バックグラウンドでも再生を続ける設定
            // false にするとアプリが画面から消えたときに再生が止まってしまう
            .setHandleAudioBecomingNoisy(true)
            // ↑ イヤホンを抜いたときに自動で一時停止する機能（trueで有効）

            .build()
    }

    // ─────────────────────────────────────────
    // 公開API（ViewModelから呼ばれる操作）
    // ─────────────────────────────────────────

    /**
     * setPlaylist()
     *
     * 再生するトラックリストをプレーヤーにセットして、再生を開始する。
     *
     * @param items       再生したい曲のリスト（MediaItem = 1曲分の情報）
     * @param startIndex  最初に再生する曲のインデックス（何番目の曲から始めるか）
     */
    fun setPlaylist(items: List<MediaItem>, startIndex: Int = 0) {
        player?.run {
            // 既存のプレイリストをクリアしてから新しいリストをセット
            setMediaItems(items, startIndex, /* startPositionMs = */ 0L)

            // prepare() を呼ぶことでプレーヤーがファイルを読み込み始める
            // prepare() を忘れると play() を呼んでも無音になるので注意
            prepare()

            // 再生開始
            play()
        }
    }

    /**
     * togglePlayPause()
     *
     * 再生中なら一時停止、停止中なら再生する。トグル関数。
     */
    fun togglePlayPause() {
        player?.run {
            if (isPlaying) pause() else play()
        }
    }

    /**
     * skipToNext()
     *
     * 次の曲にスキップする。
     * シャッフルがONの場合、ExoPlayerが自動でランダムな次の曲を選ぶ。
     */
    fun skipToNext() {
        player?.seekToNextMediaItem()
    }

    /**
     * skipToPrevious()
     *
     * 前の曲に戻る。
     * 再生位置が3秒以上進んでいる場合は曲の先頭に戻り、
     * 3秒未満なら前の曲に戻る（一般的な音楽プレーヤーの挙動）。
     */
    fun skipToPrevious() {
        player?.run {
            if (currentPosition > 3_000L) {
                // 3秒以上再生済み → 曲の先頭（0ms）に戻る
                seekTo(0L)
            } else {
                // 3秒未満 → 前の曲へ
                seekToPreviousMediaItem()
            }
        }
    }

    /**
     * seekTo()
     *
     * 曲の任意の位置にジャンプする（シークバー操作時に使う）。
     *
     * @param positionMs ジャンプ先の位置（ミリ秒単位）
     *                   例：30秒 = 30_000L
     */
    fun seekTo(positionMs: Long) {
        player?.seekTo(positionMs)
    }

    // ─────────────────────────────────────────
    // シャッフル / リピート 制御
    // ─────────────────────────────────────────

    /**
     * setShuffleModeEnabled()
     *
     * シャッフル再生のON/OFFを切り替える。
     *
     * ExoPlayerはシャッフルを内部で管理しており、ONにすると
     * seekToNextMediaItem() が自動でランダムな曲を選ぶようになる。
     *
     * @param enabled true でシャッフルON、false でOFF
     */
    fun setShuffleModeEnabled(enabled: Boolean) {
        player?.shuffleModeEnabled = enabled
    }

    /**
     * setRepeatMode()
     *
     * リピートモードを設定する。
     * ExoPlayer には以下の3つのモードがある：
     *
     *  - Player.REPEAT_MODE_OFF  : リピートなし（最後の曲で停止）
     *  - Player.REPEAT_MODE_ONE  : 1曲リピート（同じ曲をループ）
     *  - Player.REPEAT_MODE_ALL  : 全曲リピート（最後の曲の次は最初の曲）
     *
     * @param mode 上記の Player.REPEAT_MODE_* 定数を渡す
     */
    fun setRepeatMode(@Player.RepeatMode mode: Int) {
        player?.repeatMode = mode
    }

    /**
     * getRepeatMode()
     *
     * 現在のリピートモードを取得する。
     * UIの表示更新に使う。
     *
     * @return Player.REPEAT_MODE_OFF / ONE / ALL のいずれか
     *         プレーヤーが未初期化なら REPEAT_MODE_OFF を返す
     */
    fun getRepeatMode(): Int {
        return player?.repeatMode ?: Player.REPEAT_MODE_OFF
    }

    /**
     * isShuffleModeEnabled()
     *
     * 現在シャッフルがONかどうかを返す。
     *
     * @return true ならシャッフルON
     */
    fun isShuffleModeEnabled(): Boolean {
        return player?.shuffleModeEnabled ?: false
    }

    // ─────────────────────────────────────────
    // プレーヤー状態の取得
    // ─────────────────────────────────────────

    /**
     * getCurrentPosition()
     *
     * 現在の再生位置をミリ秒で返す。
     * シークバーの表示更新などに使う。
     */
    fun getCurrentPosition(): Long {
        return player?.currentPosition ?: 0L
    }

    /**
     * getDuration()
     *
     * 現在再生中の曲の長さ（総再生時間）をミリ秒で返す。
     * まだ読み込み中の場合は C.TIME_UNSET が返ることがある。
     */
    fun getDuration(): Long {
        return player?.duration ?: C.TIME_UNSET
    }

    /**
     * isPlaying()
     *
     * 現在再生中かどうかを返す。
     */
    fun isPlaying(): Boolean {
        return player?.isPlaying ?: false
    }

    /**
     * addListener()
     *
     * プレーヤーの状態変化（再生/停止/曲送り/エラー等）を
     * 外部（ViewModel）から監視するためのリスナーを登録する。
     *
     * 使い方の例：
     *   service.addListener(object : Player.Listener {
     *       override fun onIsPlayingChanged(isPlaying: Boolean) {
     *           // UIを更新する
     *       }
     *   })
     *
     * @param listener 状態変化を受け取る Player.Listener
     */
    fun addListener(listener: Player.Listener) {
        player?.addListener(listener)
    }

    /**
     * removeListener()
     *
     * 登録したリスナーを解除する。
     * ViewModelが破棄されるときに必ず呼ぶこと（メモリリーク防止）。
     *
     * @param listener 解除したい Player.Listener
     */
    fun removeListener(listener: Player.Listener) {
        player?.removeListener(listener)
    }
}
