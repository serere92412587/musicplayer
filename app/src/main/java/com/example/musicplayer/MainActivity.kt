package com.example.musicplayer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.musicplayer.ui.PlayerScreen
import com.example.musicplayer.ui.theme.MusicplayerTheme
import com.example.musicplayer.viewmodel.PlayerViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.musicplayer.ui.AllSongsScreen
import com.example.musicplayer.ui.PlaylistListScreen
import com.example.musicplayer.ui.PlaylistSettingScreen

/**
 * MainActivity
 *
 * アプリのエントリーポイント（最初に起動される画面）。
 * ここで行う主な仕事は3つ：
 *
 *  1. ストレージのパーミッション（権限）をユーザーに要求する
 *  2. ViewModel を取得して PlayerScreen に渡す
 *  3. Compose の UI をセットアップする
 *
 * ─ ComponentActivity を継承している理由 ─
 * Jetpack Compose を使う場合は ComponentActivity（または AppCompatActivity）を
 * 継承する必要がある。setContent { } で Composable を画面にセットできる。
 */
class MainActivity : ComponentActivity() {

    // ─────────────────────────────────────────
    // ViewModel の取得
    // ─────────────────────────────────────────

    /**
     * viewModels() デリゲート
     *
     * Activity に紐付いた ViewModel を取得する。
     * by キーワードで委譲することで、初回アクセス時に自動で生成される。
     *
     * ViewModel は画面回転などの設定変更でも破棄されないため、
     * 再生状態がリセットされることがない。
     */
    private val viewModel: PlayerViewModel by viewModels()

    // ─────────────────────────────────────────
    // パーミッションリクエストの準備
    // ─────────────────────────────────────────

    /**
     * permissionLauncher
     *
     * パーミッション（権限）をユーザーに要求して、
     * 結果（許可/拒否）を受け取るためのランチャー。
     *
     * Android 6.0 以降、音楽ファイルの読み込みには
     * ユーザーの明示的な許可が必要になった（実行時パーミッション）。
     * マニフェストに書くだけでは不十分で、コードでも要求する必要がある。
     *
     * registerForActivityResult() は onCreate() より前に登録する必要があるため、
     * プロパティとして定義している。
     */
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // isGranted: true → 許可された、false → 拒否された
        if (isGranted) {
            // 許可されたので音楽ファイルを読み込む
            Log.d("MainActivity", "許可された")
            loadMusicFiles()
        }
        // 拒否された場合はダイアログで説明する（後述の UI 側で処理）
    }

    // ─────────────────────────────────────────
    // ライフサイクル
    // ─────────────────────────────────────────

    /**
     * onCreate()
     *
     * Activity が生成されたときに1回だけ呼ばれる。
     * UI のセットアップとパーミッションの確認をここで行う。
     *
     * @param savedInstanceState 画面回転などで再生成された場合の保存データ
     *                           （今回は特に使わない）
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Compose の UI をセットアップする
        setContent {

            // ─ テーマの適用 ─
            // MusicplayerTheme は ui/theme/Theme.kt に自動生成されているテーマ。
            // Material3 のカラースキームやタイポグラフィを提供する。
            MusicplayerTheme {

                // ─ パーミッション拒否時のダイアログ表示フラグ ─
                // remember { } で Composable のライフサイクルに紐付いた状態を保持する
                // by を使うと .value を省略できる
                var showPermissionRationale by remember { mutableStateOf(false) }

                // ─ パーミッション拒否時の説明ダイアログ ─
                if (showPermissionRationale) {
                    AlertDialog(
                        onDismissRequest = { showPermissionRationale = false },
                        title = { Text("音楽ファイルへのアクセスが必要です") },
                        text = {
                            Text(
                                "このアプリは端末に保存された音楽ファイルを再生するために" +
                                        "ストレージへのアクセス権限が必要です。\n\n" +
                                        "設定アプリから権限を許可してください。"
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = { showPermissionRationale = false }) {
                                Text("わかった")
                            }
                        }
                    )
                }

                /**
                 * Scaffold
                 *
                 * Material3 が提供する画面の基本骨格。
                 * TopAppBar、BottomBar、FAB（フローティングボタン）などを
                 * 決まった位置に配置できる。
                 * 今回はシンプルに使い、主に padding の管理に使っている。
                 */
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            // innerPadding はシステムバー（ステータスバー等）の高さを考慮した余白
                            // これを指定しないとコンテンツがシステムバーの下に隠れる
                            .padding(innerPadding)
                    ) {
                        // 画面遷移を管理するコントローラーを生成
                        val navController = rememberNavController()

                        // NavHost で「どの文字（ルート）の時にどの画面を出すか」を決める
                        NavHost(
                            navController = navController,
                            startDestination = "player_screen" // 最初はプレーヤー画面
                        ){

                            // ① プレーヤー画面
                            composable("player_screen") {
                                // メインの再生画面を表示する
                                // ViewModel を渡すことで、画面が状態を監視・操作できる
                                PlayerScreen(
                                    viewModel = viewModel,
                                    onNavigateToPlaylist = {
                                        // ボタンが押されたらプレイリスト一覧へ移動！
                                        navController.navigate("playlist_list_screen")
                                    },
                                    onNavigateToAllSongs = {
                                        // 全曲一覧ボタンが押されたら全曲一覧へ移動
                                        navController.navigate("all_songs_screen")
                                    }
                                )
                            }

                            // ② プレイリスト一覧画面
                            composable("playlist_list_screen") {
                                PlaylistListScreen(
                                    viewModel = viewModel,
                                    onNavigateToDetail = { playlistId ->
                                        // 詳細画面へ移動（まだ作ってない場合は後回しでOK）
                                        // navController.navigate("playlist_detail/$playlistId")
                                    },
                                    onBack = {
                                        // 戻るボタンが押されたら前の画面に戻る
                                        navController.popBackStack()
                                    },
                                    onPlaylistClick = { playlistId ->
                                        //空
                                    },
                                    onSettingsClick = { playlistId ->
                                        navController.navigate("playlist_setting/$playlistId")
                                    }
                                )
                            }

                            // ③ プレイリスト設定画面（今回追加する部分！）
                            composable("playlist_setting/{playlistId}") { backStackEntry ->
                                // URLから playlistId を Long 型として取り出す
                                val playlistIdStr = backStackEntry.arguments?.getString("playlistId")
                                val playlistId = playlistIdStr?.toLongOrNull()

                                if (playlistId != null) {
                                    PlaylistSettingScreen(
                                        playlistId = playlistId,
                                        viewModel = viewModel,
                                        onBack = { navController.popBackStack() }
                                    )
                                }
                            }

                            // ④ 全曲一覧画面
                            composable("all_songs_screen") {
                                AllSongsScreen(
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                        }
                    }
                }
            }
        }

        // ─ パーミッションの確認と要求 ─
        checkAndRequestPermission(
            onDenied = {
                // 拒否されたらダイアログを表示する
                // ただし setContent {} の外からは直接 State を変更できないため、
                // ViewModel 経由で管理するか、Activity のフィールドで管理する方法もある
                // 今回はシンプルに Activity 側でパーミッション処理を完結させる
            }
        )
    }

    // ─────────────────────────────────────────
    // パーミッション関連
    // ─────────────────────────────────────────

    /**
     * checkAndRequestPermission()
     *
     * ストレージのパーミッションが既に許可されているか確認し、
     * 未許可なら要求する。
     *
     * ─ Android バージョンによる分岐について ─
     * Android 13（API 33）以降：READ_MEDIA_AUDIO（音声ファイル専用のパーミッション）
     * Android 12 以下：       READ_EXTERNAL_STORAGE（外部ストレージ全体のパーミッション）
     *
     * @param onDenied パーミッションが拒否されたときのコールバック
     */
    private fun checkAndRequestPermission(onDenied: () -> Unit = {}) {

        // Android バージョンに応じて必要なパーミッションを選択する
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Log.d("MainActivity", "13以降")
            // Android 13（API 33）以降
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Log.d("MainActivity", "12以下")
            // Android 12（API 32）以下
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            // ケース1：既にパーミッションが許可されている
            ContextCompat.checkSelfPermission(this, permission) ==
                    PackageManager.PERMISSION_GRANTED -> {
                // すぐに音楽ファイルを読み込む
                Log.d("MainActivity", "許可済み")
                loadMusicFiles()
            }

            // ケース2：パーミッションが未許可 → ユーザーに要求する
            else -> {
                Log.d("MainActivity", "未許可")
                permissionLauncher.launch(permission)
            }
        }
    }

    /**
     * loadMusicFiles()
     *
     * パーミッションが許可された後に呼ばれる。
     * ViewModel に音楽ファイルの読み込みを依頼する。
     *
     * 実際の読み込み処理は MusicRepository が行い、
     * ViewModel がそれを受け取って Service に渡す流れになる。
     * （MusicRepository.kt は次のステップで実装予定）
     */
    private fun loadMusicFiles() {
        viewModel.loadMusicFromStorage()
        Log.d("MainActivity", "loadMusicFiles")
    }
}
