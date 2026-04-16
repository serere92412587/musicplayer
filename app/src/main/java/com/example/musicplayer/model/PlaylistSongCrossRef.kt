package com.example.musicplayer.model

import androidx.room.Entity

/**
 * PlaylistSongCrossRef
 *
 * プレイリストと曲の「多対多」の関係を表す中間テーブル。
 *
 * ─ なぜ中間テーブルが必要なのか ─
 * 1つのプレイリストには複数の曲が含まれる。
 * 1つの曲は複数のプレイリストに含まれる可能性がある。
 * これを「多対多（Many-to-Many）」の関係という。
 *
 * 例：
 *   プレイリスト「healing夜用」→ 曲A, 曲B, 曲C
 *   プレイリスト「通勤用」    → 曲B, 曲D
 *   （曲Bは両方のプレイリストに含まれている）
 *
 * この関係をデータベースで表現するには中間テーブルが必要になる。
 * Playlist テーブルに曲IDのリストを直接持たせることはできないため。
 *
 * ─ データベース上のイメージ ─
 *
 * playlists テーブル     playlist_song_cross_ref テーブル    （曲はMediaStoreで管理）
 * | id | name     |      | playlistId | songId | sortOrder |
 * |----|----------|      |------------|--------|-----------|
 * | 1  | healing  |      | 1          | 100    | 0         |
 * | 2  | 通勤用   |      | 1          | 200    | 1         |
 *                        | 1          | 300    | 2         |
 *                        | 2          | 200    | 0         |
 *                        | 2          | 400    | 1         |
 *
 * playlistId=1 の行を集めると「healing」の曲リストになる。
 *
 * ─ Song テーブルを作らない理由 ─
 * 曲の情報（タイトル・アーティスト等）は Android の MediaStore が管理している。
 * アプリ側でも同じ情報をDBに保存すると、MediaStore と二重管理になり
 * 曲名変更などの同期が面倒になる。
 * そのため、アプリのDBには曲の「ID（MediaStore上のID）」だけを保存し、
 * 曲の詳細情報が必要なときは MediaStore に問い合わせる設計にする。
 *
 * @param playlistId どのプレイリストか（Playlist.id と対応）
 * @param songId     どの曲か（MediaStore の Audio.Media._ID と対応）
 * @param sortOrder  プレイリスト内での曲の並び順（0始まり）
 *                   ユーザーが曲を並び替えたときにこの値を更新する
 */
@Entity(
    tableName = "playlist_song_cross_ref",
    // 複合主キー：playlistId と songId の組み合わせで一意になる
    // 同じプレイリストに同じ曲を2回追加できないようにする制約
    primaryKeys = ["playlistId", "songId"]
)
data class PlaylistSongCrossRef(
    val playlistId: Long,
    val songId: Long,
    val sortOrder: Int = 0
)