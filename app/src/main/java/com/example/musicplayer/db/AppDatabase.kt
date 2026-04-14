package com.example.musicplayer.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.musicplayer.model.Playlist
import com.example.musicplayer.model.PlaylistSongCrossRef

/**
 * AppDatabase
 *
 * アプリ全体で使うデータベースの定義クラス。
 *
 * ─ Room とは ─
 * Android 公式の SQLite ラッパーライブラリ。
 * SQLite（端末内に保存されるデータベース）を、
 * Kotlin のクラスとして扱えるようにしてくれる。
 *
 * ─ @Database アノテーション ─
 * Room に「これがデータベースの定義クラスだ」と伝える。
 *
 * entities  = このDBが管理するテーブルのクラスを列挙する。
 *             ここに書いたクラスがテーブルになる。
 *
 * version   = データベースのバージョン番号。
 *             テーブルの構造を変更（カラム追加など）したときに
 *             番号を上げる必要がある。
 *             バージョンが上がると Migration（移行処理）が必要になる。
 *
 * exportSchema = データベースのスキーマ（構造）をJSONファイルに書き出すか。
 *               バージョン管理に便利だが、今回は false にしてシンプルにする。
 *
 * ─ abstract class にする理由 ─
 * Room が実際の実装クラスを自動生成するため、
 * 開発者側は abstract（抽象クラス）として定義するだけでよい。
 */
@Database(
    entities = [
        Playlist::class,             // playlists テーブル
        PlaylistSongCrossRef::class  // playlist_song_cross_ref テーブル
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    /**
     * playlistDao()
     *
     * PlaylistDao のインスタンスを返す抽象メソッド。
     * Room が自動的に実装を生成するので、中身は書かなくていい。
     * Repository からこのメソッドを呼んでDAOを取得する。
     */
    abstract fun playlistDao(): PlaylistDao

    // ─────────────────────────────────────────
    // シングルトンパターン
    // ─────────────────────────────────────────

    /**
     * companion object（シングルトン）
     *
     * データベースのインスタンスはアプリ全体で1つだけにする必要がある。
     * 複数のインスタンスを作ると、データの不整合や
     * パフォーマンス低下の原因になる。
     *
     * シングルトンパターン = クラスのインスタンスが1つだけになるように
     * 制御するデザインパターン。
     */
    companion object {

        /**
         * @Volatile
         *
         * このプロパティへの書き込みが、全スレッドから即座に見えるようにする。
         * マルチスレッド環境でのキャッシュによる不整合を防ぐ。
         *
         * null の場合はまだインスタンスが作られていないことを示す。
         */
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * getInstance()
         *
         * データベースのインスタンスを返す。
         * まだ作られていなければここで作成し、
         * 既に作られていれば同じインスタンスを返す。
         *
         * ─ synchronized とは ─
         * 複数のスレッドが同時にこのブロックに入れないようにする仕組み。
         * 「データベースを作成する処理」が同時に2回走るのを防ぐ。
         *
         * @param context アプリのコンテキスト。DBファイルの場所の特定に使う。
         */
        fun getInstance(context: Context): AppDatabase {
            // INSTANCE が null でなければそのまま返す（インスタンスが既にある）
            return INSTANCE ?: synchronized(this) {
                // synchronized ブロック内でもう一度確認する
                // （2つのスレッドが同時に null を確認した場合への対処）
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        /**
         * buildDatabase()
         *
         * 実際にデータベースを構築して返す。
         * getInstance() からのみ呼ばれる。
         *
         * @param context アプリのコンテキスト
         */
        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext, // ApplicationContext を使う（メモリリーク防止）
                AppDatabase::class.java,
                "music_player.db"           // DBファイルの名前
            )
                /**
                 * fallbackToDestructiveMigration()
                 *
                 * DBのバージョンが上がったときに Migration が未定義の場合、
                 * DBを全削除して新しく作り直す。
                 *
                 * 本番アプリでは Migration をきちんと書くべきだが、
                 * 開発中はこれを設定しておくとバージョンアップ時に
                 * エラーで止まらず開発しやすい。
                 *
                 * ⚠️ ユーザーデータが全部消えるので、
                 * リリース時は Migration に切り替えることを検討する。
                 */
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}