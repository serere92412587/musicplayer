package com.example.musicplayer.model

import android.net.Uri

/**
 * Song
 *
 * 1曲分の情報を表すデータクラス。
 *
 * ─ data class とは ─
 * データを保持することに特化した Kotlin のクラス。
 * 通常のクラスと違い、以下が自動生成される：
 *   - equals()   : 2つの Song が同じ曲かどうかを比較できる
 *   - hashCode() : Map や Set のキーとして使える
 *   - toString() : デバッグ時に中身を読みやすく表示できる
 *   - copy()     : 一部のプロパティだけ変えたコピーを作れる
 *
 * @param id          MediaStore 上の一意ID。ContentUri の生成にも使う。
 * @param title       曲名。MediaStore から取得した文字列。
 * @param artist      アーティスト名。
 * @param album       アルバム名。
 * @param duration    再生時間（ミリ秒）。シークバーの最大値などに使う。
 * @param contentUri  実際の音楽ファイルの場所を示す URI。ExoPlayer に渡す。
 * @param albumArtUri アルバムアート画像の URI。曲名の横に表示するサムネイル用。
 */
data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val contentUri: Uri,
    val albumArtUri: Uri,
    val folderName: String = "Unknown"
)
