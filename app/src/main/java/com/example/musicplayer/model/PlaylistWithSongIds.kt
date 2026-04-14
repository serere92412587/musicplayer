package com.example.musicplayer.model

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

/**
 * PlaylistWithSongIds
 *
 * プレイリストと、そこに含まれる曲IDのリストをまとめたデータクラス。
 * Room のリレーション機能を使って、2つのテーブルを結合した結果を受け取る。
 *
 * ─ このクラスが必要な理由 ─
 * Playlist テーブルには曲IDが直接入っていない。
 * 曲IDは playlist_song_cross_ref テーブルに別途入っている。
 * このクラスを使うことで、Room が自動的に2つのテーブルを結合して
 * 「プレイリスト + 曲IDリスト」をひとまとめに返してくれる。
 *
 * ─ @Embedded ─
 * Playlist の全プロパティをこのクラスのプロパティとして展開する。
 * playlist.name ではなく、直接 name でアクセスできるようになる。
 *
 * ─ @Relation ─
 * Room に「このプロパティは別テーブルから取得する」と伝えるアノテーション。
 *
 * ─ @Junction ─
 * 多対多の関係を表すための中間テーブルを指定する。
 * PlaylistSongCrossRef テーブルを経由して曲IDを取得することを表す。
 */
data class PlaylistWithSongIds(

    @Embedded
    val playlist: Playlist,

    /**
     * songIds
     *
     * このプレイリストに含まれる曲のIDリスト。
     *
     * parentColumn  = playlists テーブルの結合キー（id）
     * entity        = 取得元のエンティティ（PlaylistSongCrossRef）
     * entityColumn  = PlaylistSongCrossRef 内で parentColumn と対応するカラム（playlistId）
     * projection    = 取得したいカラム（songId）
     *
     * Room はこの定義を見て以下のような SQL を自動生成する：
     * SELECT songId FROM playlist_song_cross_ref
     * WHERE playlistId = [Playlist.id]
     */
    @Relation(
        parentColumn = "id",
        entityColumn = "playlistId",
        entity = PlaylistSongCrossRef::class,
        projection = ["songId"]
    )
    val songIds: List<Long>
)