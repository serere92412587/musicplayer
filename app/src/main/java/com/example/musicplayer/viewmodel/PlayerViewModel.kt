package com.example.musicplayer.viewmodel

import com.example.musicplayer.repository.MusicRepository
import com.example.musicplayer.model.Playlist
import com.example.musicplayer.repository.PlaylistRepository
import com.example.musicplayer.model.Song
import android.app.Application
import android.content.ComponentName
import android.util.Log
import androidx.media3.common.Player
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.musicplayer.db.AppDatabase
import com.example.musicplayer.service.MusicService
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

/**
 * PlayerViewModel
 *
 * UIと MusicService の橋渡し役。
 *
 * ─ ViewModel を使う理由 ─
 * Activity や Composable（画面）は画面回転などで再生成される。
 * ViewModel はそのサイクルとは独立して生き続けるので、
 * 「画面が回転しても再生状態が消えない」を実現できる。
 *
 * ─ AndroidViewModel を継承している理由 ─
 * 通常の ViewModel は Application コンテキストを持たないが、
 * MediaController の接続に Application が必要なため
 * AndroidViewModel（Application付きViewModel）を使う。
 */
class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    // ─────────────────────────────────────────
    // MediaController（Serviceとの接続）
    // ─────────────────────────────────────────

    /**
     * MediaController
     *
     * ViewModel から MusicService を操作するためのリモコン。
     * MediaSession を通じて Service に命令を送る。
     *
     * Service が別プロセスにあっても動作するように設計されており、
     * play() / pause() / seekTo() などのメソッドが使える。
     */

// ─── 1. Repository ───────────────────────
    private val playlistRepository = PlaylistRepository(
        application,
        AppDatabase.getInstance(getApplication()).playlistDao()
    )

    // ─── 2. MediaController関連 ──────────────
    private var controller: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var pendingMediaItems: List<MediaItem>? = null

    // ─── 3. 再生状態の StateFlow ─────────────
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isShuffleOn = MutableStateFlow(false)
    val isShuffleOn: StateFlow<Boolean> = _isShuffleOn.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _currentMediaItem = MutableStateFlow<MediaItem?>(null)
    val currentMediaItem: StateFlow<MediaItem?> = _currentMediaItem.asStateFlow()

    // ─── 4. プレイリスト関連のStateFlow ──────
    val playlists: StateFlow<List<Playlist>> = playlistRepository
        .getAllPlaylists()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val _playlistSongs = MutableStateFlow<List<Song>>(emptyList())
    val playlistSongs: StateFlow<List<Song>> = _playlistSongs.asStateFlow()

    private val _isLoadingSongs = MutableStateFlow(false)
    val isLoadingSongs: StateFlow<Boolean> = _isLoadingSongs.asStateFlow()

    // 端末内のすべての曲を保持するリスト
    private val _allSongs = MutableStateFlow<List<Song>>(emptyList())
    val allSongs: StateFlow<List<Song>> = _allSongs.asStateFlow()


    // ─────────────────────────────────────────
    // Player.Listener（Serviceからの状態変化を受け取る）
    // ─────────────────────────────────────────

    /**
     * playerListener
     *
     * ExoPlayer の状態が変化したときに呼ばれるコールバック群。
     * Service 側の変化（再生/停止、曲送りなど）を
     * ViewModel の StateFlow に反映して UI を更新する。
     *
     * Player.Listener はインターフェースだが、全メソッドにデフォルト実装があるため
     * 必要なものだけオーバーライドすればOK。
     */
    private val playerListener = object : Player.Listener {

        /**
         * 再生状態が変わったときに呼ばれる
         * （再生開始・一時停止・バッファリング中・再生完了）
         */
        override fun onPlaybackStateChanged(playbackState: Int) {
            // 再生中かどうかの状態を更新
            _isPlaying.value = controller?.isPlaying ?: false
            // 曲の長さが確定するのも STATE_READY のタイミングなので、ここで取得
            _durationMs.value = controller?.duration?.coerceAtLeast(0L) ?: 0L
        }

        /**
         * isPlaying の値が変わったときに呼ばれる
         * （バッファリング完了後の自動再生開始なども検知できる）
         */
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
        }

        /**
         * 再生中の曲が変わったときに呼ばれる（曲送り・曲戻し）
         */
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            _currentMediaItem.value = mediaItem
            _durationMs.value = controller?.duration?.coerceAtLeast(0L) ?: 0L
        }

        /**
         * シャッフルモードが変わったときに呼ばれる
         */
        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            _isShuffleOn.value = shuffleModeEnabled
        }

        /**
         * リピートモードが変わったときに呼ばれる
         */
        override fun onRepeatModeChanged(repeatMode: Int) {
            _repeatMode.value = repeatMode
        }
    }

    // ─────────────────────────────────────────
    // 初期化
    // ─────────────────────────────────────────

    init {
        // ViewModel 生成時に Service への接続を開始する
        connectToService()
        // 再生位置を定期更新するループを開始する
        startPositionUpdateLoop()
    }

    /**
     * connectToService()
     *
     * MediaController を通じて MusicService に接続する。
     * 接続は非同期なので、完了したらリスナーを登録して状態を同期する。
     */

    private fun connectToService() {
        // SessionToken：「どのServiceのMediaSessionに接続するか」を示す識別子
        val sessionToken = SessionToken(
            getApplication(),
            ComponentName(getApplication(), MusicService::class.java)
        )

        // MediaController.Builder で非同期接続を開始
        // MainExecutor を指定することで、接続完了コールバックがメインスレッドで呼ばれる
        controllerFuture = MediaController.Builder(getApplication(), sessionToken)
            .buildAsync()

        controllerFuture?.addListener(
            {
                // 接続完了！ controller を取得する
                controller = controllerFuture?.get()

                // リスナーを登録して、以降の状態変化を受け取れるようにする
                controller?.addListener(playerListener)

                // 接続時点での現在状態を StateFlow に反映する（初期値の同期）
                syncStateFromController()

                // 接続完了後に pending の曲があればここで実行する
                pendingMediaItems?.let { items ->
                    controller?.run {
                        setMediaItems(items)
                        prepare() // 曲を読み込んでいつでも再生できる状態にする
                        // play() をここで呼ばないことが重要
                    }
                    pendingMediaItems = null
                }
            },
            // コールバックをメインスレッド（UIスレッド）で実行する指定
            MoreExecutors.directExecutor()
        )
    }

    /**
     * syncStateFromController()
     *
     * Controller 接続直後に、現在の再生状態を StateFlow に反映する。
     * アプリ再起動後などに、Service 側の状態と UI を同期するために必要。
     */
    private fun syncStateFromController() {
        controller?.let { c ->
            _isPlaying.value = c.isPlaying
            _isShuffleOn.value = c.shuffleModeEnabled
            _repeatMode.value = c.repeatMode
            _currentMediaItem.value = c.currentMediaItem
            _durationMs.value = c.duration.coerceAtLeast(0L)
        }
    }

    /**
     * startPositionUpdateLoop()
     *
     * シークバーを滑らかに動かすために、再生位置を定期的に更新するループ。
     *
     * viewModelScope は ViewModel が生きている間だけ動くコルーチンスコープ。
     * ViewModel が破棄されると自動でキャンセルされるので、手動で止める必要がない。
     */
    private fun startPositionUpdateLoop() {
        viewModelScope.launch {
            while (true) {
                // 再生中のときだけ位置を更新する（停止中は更新不要）
                if (_isPlaying.value) {
                    _currentPositionMs.value = controller?.currentPosition ?: 0L
                }
                // 500ms ごとに更新（シークバーが滑らかに見える最低限の間隔）
                delay(500L)
            }
        }
    }

    // ─────────────────────────────────────────
    // UI から呼ばれる操作メソッド
    // ─────────────────────────────────────────

    /**
     * setPlaylist()
     *
     * 再生するプレイリストをセットして再生開始する。
     * Repository から取得した曲リストを渡す。
     *
     * @param items      再生する曲のリスト
     * @param startIndex 最初に再生する曲の番号（デフォルトは先頭）
     */
    fun setPlaylist(items: List<MediaItem>, startIndex: Int = 0) {
        controller?.run {
            setMediaItems(items, startIndex, 0L)
            prepare()
            play()
        }
    }

    /**
     * togglePlayPause()
     *
     * 再生↔一時停止を切り替える。プレーヤー画面の中央ボタンに使う。
     */
    fun togglePlayPause() {
        controller?.run {
            if (isPlaying) pause() else play()
        }
    }

    /**
     * skipToNext() / skipToPrevious()
     *
     * 曲送り・曲戻し。シャッフルON時は ExoPlayer が自動でランダムに選ぶ。
     */
    fun skipToNext() { controller?.seekToNextMediaItem() }
    fun skipToPrevious() {
        controller?.run {
            // 3秒以上再生済みなら先頭に戻る、それ未満なら前の曲へ
            if (currentPosition > 3_000L) seekTo(0L) else seekToPreviousMediaItem()
        }
    }

    /**
     * seekTo()
     *
     * シークバーをドラッグしたときに呼ぶ。
     *
     * @param positionMs ジャンプ先（ミリ秒）
     */
    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
        // シーク直後に位置を即反映（次のループ更新を待たずに表示を更新）
        _currentPositionMs.value = positionMs
    }

    // ─────────────────────────────────────────
    // シャッフル / リピート 切り替え
    // ─────────────────────────────────────────

    /**
     * toggleShuffle()
     *
     * シャッフルのON/OFFを切り替える。
     * ワンタップで切り替わるようにトグル実装にしている。
     */
    fun toggleShuffle() {
        val newValue = !(_isShuffleOn.value)
        controller?.shuffleModeEnabled = newValue
        // リスナーの onShuffleModeEnabledChanged() が呼ばれて StateFlow も更新されるが、
        // 即時反映のためにここでも更新しておく
        _isShuffleOn.value = newValue
    }

    /**
     * cycleRepeatMode()
     *
     * リピートモードを順番に切り替える。
     *
     * 切り替え順：
     *   リピートなし → 1曲リピート → 全曲リピート → リピートなし → ...
     *
     * foobar2000 のようにボタンを何度も押す操作を、
     * このメソッドのワンタップで済ませられる。
     */
    fun cycleRepeatMode() {
        val nextMode = when (_repeatMode.value) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ONE  // なし → 1曲
            Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ALL  // 1曲 → 全曲
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_OFF  // 全曲 → なし
            else -> Player.REPEAT_MODE_OFF
        }
        controller?.repeatMode = nextMode
        _repeatMode.value = nextMode
    }

    // ─────────────────────────────────────────
    // ViewModel のクリーンアップ
    // ─────────────────────────────────────────

    /**
     * onCleared()
     *
     * ViewModel が破棄されるときに呼ばれる。
     * MediaController の接続を解放しないとメモリリークになるので必ず実装する。
     */
    override fun onCleared() {
        // リスナーを解除してから接続を切る
        controller?.removeListener(playerListener)
        controllerFuture?.let {
            MediaController.releaseFuture(it)
        }
        controller = null
        super.onCleared()
    }

    //プレイリストの関数
    fun createPlaylist(name: String) {
        viewModelScope.launch {
            playlistRepository.createPlaylist(name)
        }
    }

    fun deletePlaylist(playlist: Playlist) {
        viewModelScope.launch {
            playlistRepository.deletePlaylist(playlist)
        }
    }

    fun loadPlaylistSongs(playlistId: Long) {
        viewModelScope.launch {
            _isLoadingSongs.value = true
            _playlistSongs.value = playlistRepository.getSongsInPlaylist(playlistId)
            _isLoadingSongs.value = false
        }
    }

    fun addSongsToPlaylist(playlistId: Long, songIds: List<Long>) {
        viewModelScope.launch {
            playlistRepository.addSongsToPlaylist(playlistId, songIds)
            // 追加後に曲リストを再読み込みして画面を更新する
            loadPlaylistSongs(playlistId)
        }
    }

    fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch {
            playlistRepository.removeSongFromPlaylist(playlistId, songId)
            loadPlaylistSongs(playlistId)
        }
    }

    fun playPlaylist(playlistId: Long, shuffle: Boolean = false, startIndex: Int = 0) {
        viewModelScope.launch {
            val songs = playlistRepository.getSongsInPlaylist(playlistId)
            val mediaItems = with(MusicRepository(getApplication())) { songs.toMediaItems() }
            if (mediaItems.isNotEmpty()) {
                controller?.shuffleModeEnabled = shuffle
                controller?.setMediaItems(mediaItems, startIndex, 0L)
                controller?.prepare()
                controller?.play()
            }
        }
    }

    /**
     * プレイリストの更新（タイマー設定の変更などに使う）
     */
    fun updatePlaylist(playlist: Playlist) {
        viewModelScope.launch(Dispatchers.IO) {
            // ※ repository に updatePlaylist がある前提です
            val repository = PlaylistRepository(getApplication(), AppDatabase.getInstance(getApplication()).playlistDao())
            repository.updatePlaylist(playlist)
        }
    }

    // ─────────────────────────────────────────
    // 音楽ファイルの読み込み
    // ─────────────────────────────────────────

    /**
     * loadMusicFromStorage()
     *
     * MainActivity から呼ばれる。
     * Repository を通じて端末の音楽ファイルを取得し、
     * プレイリストとして Service にセットする。
     */
    fun loadMusicFromStorage() {
        viewModelScope.launch {
            // Repository のインスタンスを生成
            val repository = MusicRepository(getApplication())

            // 曲リストを取得（suspend 関数なので launch の中で呼べる）
            val songs = repository.getSongs()

            // 全曲リストを保持しておく（AddSongsDialog で使う）
            _allSongs.value = songs

            // Song のリストを MediaItem のリストに変換
            val mediaItems = with(repository) { songs.toMediaItems() }

            Log.d("PlayerViewModel", "Loaded ${mediaItems.size} songs")
            // プレイリストをセットして再生開始
            if (mediaItems.isNotEmpty()) {
                // 起動直後の自動再生を防ぐため、setPlaylistではなく直接セットしてprepareだけにする
                controller?.setMediaItems(mediaItems, 0, 0L)
                controller?.prepare()
            }
        }
    }

    // ── ③ 新しいメソッドを追加 ──
    /**
     * 全曲リストの中から、指定した曲を再生する
     */
    fun playSongFromLibrary(songId: Long) {
        val songs = _allSongs.value
        // タップされた曲が、リストの何番目にあるかを探す
        val index = songs.indexOfFirst { it.id == songId }

        if (index != -1) {
            val repository = MusicRepository(getApplication())
            val mediaItems = with(repository) { songs.toMediaItems() }
            // そのインデックスから再生を開始する（これで「次の曲」もそのまま機能する）
            setPlaylist(mediaItems, startIndex = index)
        }
    }
}
