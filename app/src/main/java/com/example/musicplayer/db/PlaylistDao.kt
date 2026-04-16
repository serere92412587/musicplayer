package com.example.musicplayer.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.musicplayer.model.Playlist
import com.example.musicplayer.model.PlaylistSongCrossRef
import com.example.musicplayer.model.PlaylistWithSongIds
import kotlinx.coroutines.flow.Flow

/**
 * PlaylistDao
 *
 * DAO（Data Access Object）= データベースへの操作をまとめたインターフェース。
 *
 * ─ DAO とは ─
 * 「データベースにどんな操作をするか」をメソッドとして定義する場所。
 * Room がこのインターフェースを見て、実際の SQL を自動生成してくれる。
 * 開発者は SQL を直接書かなくていい（書くこともできる）。
 *
 * ─ @Dao アノテーション ─
 * Room に「これは DAO インターフェースだ」と伝えるアノテーション。
 * interface に付ける。
 */
@Dao
interface PlaylistDao {

    // ─────────────────────────────────────────
    // プレイリストの CRUD 操作
    // （Create / Read / Update / Delete）
    // ─────────────────────────────────────────

    /**
     * getAllPlaylists()
     *
     * 全プレイリストを作成日時の降順（新しい順）で取得する。
     *
     * ─ Flow とは ─
     * 「データが変化するたびに自動で新しい値を流してくれるストリーム」。
     * DBのプレイリストが追加・削除・更新されると、
     * この Flow が自動で新しいリストを流してくれる。
     * ViewModel で collectAsState() と組み合わせることで、
     * DBの変化が即座に画面に反映される。
     *
     * ─ @Query とは ─
     * 直接 SQL を書いてデータを取得するアノテーション。
     * Room がこの SQL をコンパイル時にチェックしてくれるので、
     * タイポなどのミスを実行前に発見できる。
     */
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAllPlaylists(): Flow<List<Playlist>>

    /**
     * getPlaylistById()
     *
     * IDを指定して1件のプレイリストを取得する。
     * プレイリスト詳細画面で使う。
     *
     * @param playlistId 取得したいプレイリストのID
     */
    @Query("SELECT * FROM playlists WHERE id = :playlistId")
    suspend fun getPlaylistById(playlistId: Long): Playlist?

    /**
     * insertPlaylist()
     *
     * 新しいプレイリストを追加する。
     * 挿入後のIDを返す（追加直後に詳細画面に遷移するときなどに使う）。
     *
     * ─ @Insert とは ─
     * INSERT 文を自動生成してくれるアノテーション。
     *
     * ─ OnConflictStrategy.REPLACE とは ─
     * 同じ主キーのデータが既に存在した場合の対処法。
     * REPLACE = 既存のデータを新しいデータで上書きする。
     * （プレイリスト名の変更などで UPDATE の代わりに使えることもある）
     *
     * @param playlist 追加するプレイリスト
     * @return 追加されたプレイリストの自動採番ID
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: Playlist): Long

    /**
     * updatePlaylist()
     *
     * 既存のプレイリストを更新する。
     * プレイリスト名の変更やタイマー設定の変更時に使う。
     *
     * ─ @Update とは ─
     * UPDATE 文を自動生成してくれるアノテーション。
     * 主キー（id）が一致する行を更新する。
     *
     * @param playlist 更新後のデータを持つプレイリスト
     */
    @Update
    suspend fun updatePlaylist(playlist: Playlist)

    /**
     * deletePlaylist()
     *
     * プレイリストを削除する。
     * プレイリストを削除しても、紐付いている CrossRef も一緒に消す必要があるため、
     * Repository 側で deletePlaylistWithSongs() としてまとめて呼ぶ。
     *
     * ─ @Delete とは ─
     * DELETE 文を自動生成してくれるアノテーション。
     * 主キーが一致する行を削除する。
     *
     * @param playlist 削除するプレイリスト
     */
    @Delete
    suspend fun deletePlaylist(playlist: Playlist)

    // ─────────────────────────────────────────
    // プレイリストと曲の紐付け操作
    // ─────────────────────────────────────────

    /**
     * getPlaylistWithSongIds()
     *
     * 指定したプレイリストに含まれる曲のIDリストを取得する。
     * 曲の詳細情報（タイトル等）は MediaStore から別途取得する。
     *
     * ─ @Transaction とは ─
     * 複数のクエリをひとまとめにして「全部成功 or 全部失敗」にする仕組み。
     * Room のリレーション取得（@Relation）では必須のアノテーション。
     *
     * @param playlistId 取得したいプレイリストのID
     */
    @Transaction
    @Query("SELECT * FROM playlists WHERE id = :playlistId")
    suspend fun getPlaylistWithSongIds(playlistId: Long): PlaylistWithSongIds?

    /**
     * getSongIdsForPlaylist()
     *
     * 指定したプレイリストの曲IDを並び順で取得する。
     * 曲の並び順（sortOrder）の昇順で返す。
     *
     * @param playlistId プレイリストのID
     */
    @Query(
        "SELECT songId FROM playlist_song_cross_ref " +
                "WHERE playlistId = :playlistId " +
                "ORDER BY sortOrder ASC"
    )
    suspend fun getSongIdsForPlaylist(playlistId: Long): List<Long>

    /**
     * addSongToPlaylist()
     *
     * プレイリストに曲を追加する。
     * 同じ曲が既に追加されている場合は無視する（IGNORE）。
     *
     * OnConflictStrategy.IGNORE = 重複した場合は何もしない。
     * （同じ曲を2回追加しようとしても、2回目は無視される）
     *
     * @param crossRef 追加するプレイリストと曲の組み合わせ
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addSongToPlaylist(crossRef: PlaylistSongCrossRef)

    /**
     * addSongsToPlaylist()
     *
     * 複数の曲をまとめてプレイリストに追加する。
     * 曲選択画面でまとめて追加するときに使う。
     *
     * @param crossRefs 追加する曲とプレイリストの組み合わせのリスト
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addSongsToPlaylist(crossRefs: List<PlaylistSongCrossRef>)

    /**
     * removeSongFromPlaylist()
     *
     * プレイリストから曲を削除する。
     *
     * @param playlistId プレイリストのID
     * @param songId     削除する曲のID
     */
    @Query(
        "DELETE FROM playlist_song_cross_ref " +
                "WHERE playlistId = :playlistId AND songId = :songId"
    )
    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long)

    /**
     * removeAllSongsFromPlaylist()
     *
     * プレイリストから全曲を削除する。
     * プレイリスト自体を削除するときに、先にこれを呼んで紐付けを消す。
     *
     * @param playlistId プレイリストのID
     */
    @Query(
        "DELETE FROM playlist_song_cross_ref " +
                "WHERE playlistId = :playlistId"
    )
    suspend fun removeAllSongsFromPlaylist(playlistId: Long)

    /**
     * updateSongOrder()
     *
     * プレイリスト内の曲の並び順を更新する。
     * ユーザーがドラッグ&ドロップで曲を並び替えたときに呼ぶ。
     *
     * @param playlistId プレイリストのID
     * @param songId     並び順を変更する曲のID
     * @param newOrder   新しい並び順の番号
     */
    @Query(
        "UPDATE playlist_song_cross_ref " +
                "SET sortOrder = :newOrder " +
                "WHERE playlistId = :playlistId AND songId = :songId"
    )
    suspend fun updateSongOrder(playlistId: Long, songId: Long, newOrder: Int)
}