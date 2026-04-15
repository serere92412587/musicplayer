package com.example.musicplayer.repository

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.example.musicplayer.db.AppDatabase
import com.example.musicplayer.db.PlaylistDao
import com.example.musicplayer.model.Playlist
import com.example.musicplayer.model.PlaylistSongCrossRef
import com.example.musicplayer.model.Song
import com.example.musicplayer.model.TimerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * PlaylistRepository
 *
 * プレイリストに関するデータ操作をまとめるクラス。
 *
 * ─ 責務の分担 ─
 * ViewModel  → 「プレイリストを作って」と命令するだけ
 * Repository → 実際にDBに保存する処理を担当
 * DAO        → SQLを実行する最小単位
 *
 * ViewModel はDBの存在を知らなくていい。
 * Repository に頼むだけで済む。
 *
 * @param context アプリのコンテキスト。
 *                DBの取得と MediaStore へのアクセスに使う。
 */
class PlaylistRepository(private val context: Context, playlistDao: PlaylistDao) {

    /**
     * dao
     *
     * AppDatabase のシングルトンインスタンスから DAO を取得する。
     * Repository が生成されるたびに同じDBインスタンスが使われる。
     */
    private val dao = AppDatabase.getInstance(context).playlistDao()

    // ─────────────────────────────────────────
    // プレイリストの CRUD
    // ─────────────────────────────────────────

    /**
     * getAllPlaylists()
     *
     * 全プレイリストを Flow で返す。
     * DBが更新されるたびに自動で最新のリストが流れてくる。
     *
     * ViewModel でこの Flow を collect することで、
     * プレイリストを追加・削除したら即座に一覧画面が更新される。
     */
    fun getAllPlaylists(): Flow<List<Playlist>> {
        return dao.getAllPlaylists()
    }

    /**
     * createPlaylist()
     *
     * 新しいプレイリストを作成する。
     * 作成後のIDを返すので、すぐに詳細画面に遷移できる。
     *
     * @param name        プレイリスト名
     * @param timerConfig タイマー設定（省略時はタイマーOFF）
     * @return 作成されたプレイリストのID
     */
    suspend fun createPlaylist(
        name: String,
        timerConfig: TimerConfig = TimerConfig()
    ): Long {
        val playlist = Playlist(
            name = name,
            timerConfig = timerConfig
            // id は 0 のまま渡すと Room が自動採番する
            // createdAt は Playlist のデフォルト値で現在時刻が入る
        )
        return dao.insertPlaylist(playlist)
    }

    /**
     * updatePlaylist()
     *
     * プレイリスト名やタイマー設定を更新する。
     * プレイリスト設定画面の「保存」ボタンで呼ばれる。
     *
     * @param playlist 更新後のデータを持つ Playlist
     */
    suspend fun updatePlaylist(playlist: Playlist) {
        dao.updatePlaylist(playlist)
    }

    /**
     * deletePlaylist()
     *
     * プレイリストと、そこに紐付く曲の関連を全て削除する。
     *
     * ─ なぜ2つの操作が必要か ─
     * プレイリストを削除しても playlist_song_cross_ref テーブルの行は
     * 自動では消えない（Room は外部キー制約による CASCADE を
     * デフォルトでは設定しないため）。
     * 先に CrossRef を消してからプレイリストを消す必要がある。
     *
     * @param playlist 削除するプレイリスト
     */
    suspend fun deletePlaylist(playlist: Playlist) {
        // 先に紐付けを全削除してから
        dao.removeAllSongsFromPlaylist(playlist.id)
        // プレイリスト本体を削除する
        dao.deletePlaylist(playlist)
    }

    // ─────────────────────────────────────────
    // プレイリストへの曲の追加・削除
    // ─────────────────────────────────────────

    /**
     * addSongsToPlaylist()
     *
     * 複数の曲をプレイリストに追加する。
     * 曲選択画面で複数曲を選んで「追加」ボタンを押したときに呼ばれる。
     *
     * sortOrder は既存の曲の末尾に続く番号を振る。
     * 例：既に3曲あれば 3, 4, 5... と番号を振る。
     *
     * @param playlistId 追加先のプレイリストID
     * @param songIds    追加する曲のIDリスト
     */
    suspend fun addSongsToPlaylist(playlistId: Long, songIds: List<Long>) {
        // 既存の曲数を取得して、末尾から sortOrder を振る
        val existingSongIds = dao.getSongIdsForPlaylist(playlistId)
        val startOrder = existingSongIds.size

        val crossRefs = songIds.mapIndexed { index, songId ->
            PlaylistSongCrossRef(
                playlistId = playlistId,
                songId = songId,
                sortOrder = startOrder + index
            )
        }
        dao.addSongsToPlaylist(crossRefs)
    }

    /**
     * removeSongFromPlaylist()
     *
     * プレイリストから1曲を削除する。
     * 曲を長押しして「削除」を選んだときなどに呼ばれる。
     *
     * @param playlistId プレイリストのID
     * @param songId     削除する曲のID
     */
    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        dao.removeSongFromPlaylist(playlistId, songId)
    }

    // ─────────────────────────────────────────
    // プレイリストの曲情報の取得
    // ─────────────────────────────────────────

    /**
     * getSongsInPlaylist()
     *
     * プレイリストに含まれる曲を Song のリストで返す。
     *
     * ─ 処理の流れ ─
     * 1. DBから「このプレイリストに含まれる曲のID」を取得
     * 2. MediaStore に「このIDの曲の詳細情報をくれ」と問い合わせる
     * 3. DBの並び順（sortOrder）に並べ直して返す
     *
     * ─ なぜ2ステップ必要なのか ─
     * 曲の詳細情報（タイトル・アーティスト等）は MediaStore が管理しており、
     * アプリのDB（Room）には曲IDしか入っていないため。
     *
     * @param playlistId プレイリストのID
     * @return 曲のリスト（プレイリストの並び順）
     */
    suspend fun getSongsInPlaylist(playlistId: Long): List<Song> =
        withContext(Dispatchers.IO) {

            // ステップ1：DBから曲IDを並び順で取得
            val songIds = dao.getSongIdsForPlaylist(playlistId)
            if (songIds.isEmpty()) return@withContext emptyList()

            // ステップ2：MediaStore から曲の詳細情報を取得
            val songs = fetchSongsFromMediaStore(songIds)

            // ステップ3：DBの並び順（songIds の順番）に並べ直す
            // MediaStore のクエリ結果は順番が保証されないため、
            // songIds の順番に合わせて並べ直す必要がある
            val songMap = songs.associateBy { it.id } // IDをキーにしたMapを作る
            songIds.mapNotNull { songMap[it] }         // songIds の順番で Song を取り出す
        }

    /**
     * fetchSongsFromMediaStore()
     *
     * 指定した曲IDのリストに対応する曲情報を MediaStore から取得する。
     *
     * ─ IN句を使ったクエリ ─
     * 「WHERE _id IN (100, 200, 300)」のような SQL を組み立てて
     * 複数のIDを一度に取得する。
     * IDごとに1回ずつクエリを投げると遅くなるため、まとめて取得する。
     *
     * @param songIds 取得したい曲のIDリスト
     * @return 取得した Song のリスト
     */
    private fun fetchSongsFromMediaStore(songIds: List<Long>): List<Song> {
        val songs = mutableListOf<Song>()

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID,
        )

        // IN 句のプレースホルダーを作る
        // 例：songIds が [100, 200, 300] なら "?,?,?" という文字列を作る
        val placeholders = songIds.joinToString(",") { "?" }
        val selection = "${MediaStore.Audio.Media._ID} IN ($placeholders)"

        // Long のリストを String の配列に変換（selectionArgs は String[] を要求するため）
        val selectionArgs = songIds.map { it.toString() }.toTypedArray()

        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null // ここでは並び順を指定しない（後でDBの順番に並べ直すため）
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val albumId = cursor.getLong(albumIdColumn)

                songs.add(
                    Song(
                        id = id,
                        title = cursor.getString(titleColumn) ?: "不明なタイトル",
                        artist = cursor.getString(artistColumn) ?: "不明なアーティスト",
                        album = cursor.getString(albumColumn) ?: "不明なアルバム",
                        duration = cursor.getLong(durationColumn),
                        contentUri = ContentUris.withAppendedId(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
                        ),
                        albumArtUri = ContentUris.withAppendedId(
                            Uri.parse("content://media/external/audio/albumart"), albumId
                        )
                    )
                )
            }
        }

        return songs
    }

    /**
     * getPlaylistWithTimerConfig()
     *
     * 指定したプレイリストのタイマー設定を取得する。
     * 再生開始時にスリープタイマーを自動セットするために使う。
     *
     * @param playlistId プレイリストのID
     * @return Playlist（timerConfig を含む）、見つからなければ null
     */
    suspend fun getPlaylistWithTimerConfig(playlistId: Long): Playlist? {
        return dao.getPlaylistById(playlistId)
    }
}