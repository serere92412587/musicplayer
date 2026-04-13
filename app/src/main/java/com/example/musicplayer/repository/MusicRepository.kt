package com.example.musicplayer.repository

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.example.musicplayer.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * MusicRepository
 *
 * 端末のストレージから音楽ファイルの情報を取得する責務を持つクラス。
 *
 * ─ Repository パターンとは ─
 * データの取得元（ローカルDB、ネットワーク、端末ストレージ等）を
 * ViewModel から隠蔽するための設計パターン。
 *
 * ViewModel は「どこからデータを取得するか」を知らなくていい。
 * Repository に「曲のリストをくれ」と頼むだけでよく、
 * 取得元が変わっても ViewModel のコードを変える必要がない。
 *
 * ─ データの流れ ─
 * 端末ストレージ（MediaStore）
 *     ↓
 * MusicRepository（ここで取得・加工）
 *     ↓
 * PlayerViewModel（受け取って MusicService に渡す）
 *     ↓
 * MusicService → ExoPlayer（実際に再生）
 *
 * @param context アプリのコンテキスト。MediaStore へのアクセスに必要。
 */
class MusicRepository(private val context: Context) {

    /**
     * getSongs()
     *
     * 端末内の全音楽ファイルを取得して Song のリストで返す。
     *
     * ─ MediaStore とは ─
     * Android が端末内のメディアファイル（音楽・動画・画像）を
     * データベースのように管理する仕組み。
     * SQL に似たクエリで「タイトル順に音楽ファイルを全部くれ」などと
     * 問い合わせることができる。
     *
     * ─ suspend 関数とは ─
     * コルーチンの中でしか呼べない関数。
     * ファイルの読み込みは時間がかかるため、メインスレッド（UIスレッド）で
     * 実行するとアプリがフリーズする。
     * suspend にすることで、バックグラウンドスレッドで実行できる。
     *
     * @return 端末内の音楽ファイルを Song のリストで返す
     */
    suspend fun getSongs(): List<Song> = withContext(Dispatchers.IO) {
        // Dispatchers.IO：ファイルIO・ネットワーク等の重い処理に適したスレッド
        // withContext で処理をこのスレッドに切り替える

        val songs = mutableListOf<Song>()

        // ─ クエリの設定 ─

        /**
         * projection（取得するカラムの指定）
         *
         * SQLでいう「SELECT カラム名」の部分。
         * 必要な情報だけ取得することでパフォーマンスを上げる。
         */
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,           // ファイルの一意ID（URIの生成に使う）
            MediaStore.Audio.Media.TITLE,         // 曲名
            MediaStore.Audio.Media.ARTIST,        // アーティスト名
            MediaStore.Audio.Media.ALBUM,         // アルバム名
            MediaStore.Audio.Media.DURATION,      // 再生時間（ミリ秒）
            MediaStore.Audio.Media.ALBUM_ID,      // アルバムアートの取得に使うID
            MediaStore.Audio.Media.DATE_ADDED,    // 追加日時（ソートに使う）
        )

        /**
         * selection（絞り込み条件）
         *
         * SQLでいう「WHERE」の部分。
         * IS_MUSIC = 1 で「音楽ファイルだけ」に絞り込む。
         * （着信音や通知音などは除外される）
         *
         * DURATION > ? で短すぎるファイル（効果音等）を除外する。
         * ? の値は selectionArgs で指定する（SQLインジェクション対策）。
         */
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} = 1 " +
                "AND ${MediaStore.Audio.Media.DURATION} > ?"

        // 30秒（30000ミリ秒）未満のファイルは除外する
        val selectionArgs = arrayOf("30000")

        /**
         * sortOrder（並び順）
         *
         * SQLでいう「ORDER BY」の部分。
         * ここではタイトルのアルファベット順（ASC = 昇順）で並べる。
         */
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        // ─ MediaStore にクエリを実行 ─

        /**
         * contentResolver.query()
         *
         * Android の ContentResolver を通じて MediaStore に問い合わせる。
         * 結果は Cursor（データベースのカーソル）として返ってくる。
         *
         * use { } ブロックを使うことで、処理が終わったら自動で Cursor を閉じる
         * （閉じ忘れによるメモリリークを防ぐ）。
         */
        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, // 外部ストレージの音楽
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->

            // 各カラムのインデックスを事前に取得しておく（ループ内で毎回取得するより高速）
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

            // Cursor を1行ずつ読み進める
            while (cursor.moveToNext()) {

                val id = cursor.getLong(idColumn)
                val title = cursor.getString(titleColumn) ?: "不明なタイトル"
                val artist = cursor.getString(artistColumn) ?: "不明なアーティスト"
                val album = cursor.getString(albumColumn) ?: "不明なアルバム"
                val duration = cursor.getLong(durationColumn)
                val albumId = cursor.getLong(albumIdColumn)

                /**
                 * 音楽ファイルの URI を生成する
                 *
                 * URI（Uniform Resource Identifier）はファイルの「住所」。
                 * content://media/external/audio/media/123 のような形式になる。
                 * ExoPlayer はこの URI を使って実際のファイルを読み込む。
                 */
                val contentUri: Uri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id
                )

                /**
                 * アルバムアートの URI を生成する
                 *
                 * アルバムアートは MediaStore.Audio.Albums から取得できる。
                 * アルバムID を使って対応するアートワーク画像の URI を組み立てる。
                 */
                val albumArtUri: Uri = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"),
                    albumId
                )

                // Song データクラスに詰めてリストに追加
                songs.add(
                    Song(
                        id = id,
                        title = title,
                        artist = artist,
                        album = album,
                        duration = duration,
                        contentUri = contentUri,
                        albumArtUri = albumArtUri
                    )
                )
            }
        }

        // 取得した曲リストを返す
        songs
    }

    /**
     * toMediaItems()
     *
     * Song のリストを ExoPlayer が扱える MediaItem のリストに変換する
     * 拡張関数。
     *
     * ─ MediaItem とは ─
     * ExoPlayer に「この曲を再生してくれ」と渡すためのオブジェクト。
     * 再生するファイルの URI と、曲名・アーティスト名などのメタデータを持つ。
     *
     * ─ 拡張関数にする理由 ─
     * List<Song> に直接メソッドを追加する形にすることで、
     * 呼び出し側で `songs.toMediaItems()` と自然に書ける。
     */
    fun List<Song>.toMediaItems(): List<MediaItem> {
        return this.map { song ->
            MediaItem.Builder()
                .setUri(song.contentUri)           // 再生するファイルのURI
                .setMediaId(song.id.toString())    // 曲の一意ID（プレイリスト管理に使う）
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(song.title)               // 曲名（通知やロック画面に表示）
                        .setArtist(song.artist)             // アーティスト名
                        .setAlbumTitle(song.album)          // アルバム名
                        .setArtworkUri(song.albumArtUri)    // アルバムアート画像のURI
                        .build()
                )
                .build()
        }
    }
}
