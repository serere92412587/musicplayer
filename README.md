# アプリ名:musicplayer

## アプリ概要

本アプリは、Jetpack ComposeとMedia3を使って作成した音楽プレーヤーです。
シャッフル・リピート再生、プレイリスト作成などの基本的な機能に加え、
プレイリストごとに一定時間が経つとフェードアウトしながら自動的に再生を停止するスリープタイマー機能、
jaudiotaggerを用いてファイルに埋め込んだリプレイゲインのゲイン値を読み取り、
音量を補正するリプレイゲイン機能を搭載しています。

## 使用技術
- Kotlin 2.0.21
- Media3 1.4.1
- Jetpack Compose
- Room (SQLite ※プレイリスト保存用)
- jaudiotagger (メタデータ解析用)

## 主な機能
- 音源を再生
- プレイリストの新規作成・編集・削除(削除は確認ダイアログあり)
- プレイリストごとのスリープタイマー(音量が低下するまでの時間を指定、時間になったら段階的に音量を下げる)
- 音源再生・プレイリストに追加する際のフォルダ名・曲名・アーティスト名による絞り込み

## 使用環境・セットアップ手順
- Android Studio Panda3 2025.3.3 Patch1
- JDK 21
- 最小SDK 26(Android 8.0)
- ターゲットSDK 36(Android 16 ※最新OSのpixel 9aでデバッグしています)

### リポジトリのクローン
git clone https://github.com/serere92412587/musicplayer.git

## UIのスクリーンショット
### プレーヤー画面
<img width="400" height="700" alt="image" src="https://github.com/user-attachments/assets/be797ed9-367b-4d88-9e97-f43a3daababf" />

### プレイリスト管理・表示画面
<img width="400" height="700" alt="image" src="https://github.com/user-attachments/assets/dbc67a6c-2fe1-4c47-a8b2-542e3234fa97" />

### ファイル選択画面
<img width="416" height="700" alt="スクリーンショット 2026-06-25 145609" src="https://github.com/user-attachments/assets/9e224134-0088-48e6-9734-22b81fc0be8e" />

### 曲追加・選択画面
<img width="417" height="700" alt="スクリーンショット 2026-06-25 145555" src="https://github.com/user-attachments/assets/64b3b1e3-7953-4788-aa82-8e0ec4115ed8" />

<img width="400" height="700" alt="image" src="https://github.com/user-attachments/assets/4442a777-5361-43b2-9c23-686f1e0318f7" />

<img width="416" height="700" alt="スクリーンショット 2026-06-25 145643" src="https://github.com/user-attachments/assets/b6333c46-4077-44f2-930b-0da6729a835d" />


