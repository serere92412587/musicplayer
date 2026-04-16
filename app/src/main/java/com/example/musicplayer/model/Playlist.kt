package com.example.musicplayer.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Playlist
 *
 * プレイリスト1件分の情報を表すデータクラス。
 * Room のテーブル定義も兼ねている。
 *
 * ─ @Entity とは ─
 * Room に「このクラスをデータベースのテーブルとして扱う」と伝えるアノテーション。
 * クラスのプロパティが各カラム（列）に対応する。
 *
 * tableName を指定することでテーブル名を明示的に決められる。
 * 指定しない場合はクラス名がそのままテーブル名になる。
 *
 * ─ データベース上のイメージ ─
 * playlists テーブル：
 * | id | name      | enabled | durationMin | fadeOutSec | createdAt      |
 * |----|-----------|---------|-------------|------------|----------------|
 * | 1  | healing夜用 | 1      | 60          | 30         | 1700000000000  |
 * | 2  | 通勤用     | 0       | 30          | 0          | 1700000001000  |
 *
 * @param id         自動採番される一意ID。
 *                   @PrimaryKey(autoGenerate = true) で Room が自動で採番する。
 *                   新規作成時は 0 を渡すと Room が自動でIDを割り当てる。
 * @param name       プレイリストの名前。ユーザーが自由に設定する。
 * @param timerConfig スリープタイマーの設定。@Embedded で同じテーブルに埋め込む。
 * @param createdAt  作成日時（ミリ秒）。一覧の並び順に使う。
 *                   System.currentTimeMillis() で現在時刻を入れる。
 */
@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val name: String,

    /**
     * @Embedded
     *
     * 別のデータクラスのプロパティを、このテーブルのカラムとして展開する。
     * TimerConfig の enabled / durationMin / fadeOutSec が
     * playlists テーブルのカラムとして直接保存される。
     *
     * @Embedded を使わない場合は別テーブルにするか、
     * JSON文字列に変換して1カラムに保存する方法になる。
     * 今回はシンプルに @Embedded を選択。
     */
    @Embedded
    val timerConfig: TimerConfig = TimerConfig(),

    val createdAt: Long = System.currentTimeMillis()
)
