# DigitalNote

Jetpack Composeで作成した手書きノートアプリのUIプロトタイプです。添付モックをもとに、以下の画面を実装しています。

- ノート一覧
- ノート検索（フィルタ付き）
- キャンバス導線
- 設定
- ノート編集
- 手書きキャンバス（描画、色変更、太さ調整、Undo/Redo、クリア）
- Room永続化（ノート単位でCanvasストローク保存）
- Navigation Compose遷移（`editor/{noteId}` / `canvas/{noteId}`）

## 技術構成

- Kotlin
- Jetpack Compose (Material 3)
- Navigation Compose
- Room + KSP
- Gradle Version Catalog

## 画面仕様（現状）

- ノート一覧はグリッド表示
- ノートタップで `canvas/{noteId}` へ直接遷移
- トップページはFABで新規ノートを作成し、そのままキャンバスへ遷移
- ノートカードはダミーデータで表示
- `SEARCH` はクエリ + チップフィルタで絞り込み
- `SETTINGS` は同期、ペン感度、表示モードなどをUI上で変更可能
- `CANVAS` はポインタイベントで実際に描画可能
- `CANVAS` はペン/消しゴム切替・筆圧カーブ・Undo/Redo・クリア対応
- `CANVAS` は上部1行ツールバーで操作し、詳細調整はポップアップで変更
- ツールバーは描画系のみ（ペン/消しゴム/レーザー/色/Undo/Redo/Clear）
- 非描画機能（背景選択、入力モード、ページ管理、表示設定）はサイドバーに集約
- サイドバーは左側配置で開閉可能
- サイドバーはボタンで開閉
- サイドバーは右側に表示し、ページを縦一覧で選択可能
- ページ一覧には描画内容のサムネイルプレビューを表示
- 背景は `マス目` / `罫線` を切替可能
- 入力モードは `ペン入力` / `指書き` を切替可能
- ストロークは丸い線端・丸い接続で描画
- 2本指スライドでページ送り、ピンチで拡大縮小（上下限あり）
- 拡大縮小の範囲は `80%` 〜 `120%`
- 罫線背景は100%時に大学ノート相当の行間になるよう調整
- `PAGE`モードはA4固定制限を設けず、表示領域全体に描画可能
- 罫線背景は初期表示で約6mm間隔
- ページ送りは2本指ドラッグで実行
- ページモード（複数ページ）と無限ホワイトボードモードを切替可能
- ピンチイン/アウトとパンで拡大縮小・移動が可能
- レーザーポインタモードを利用可能
- 描画データはノート単位でDBに保存され、再表示時に復元

## 構成（機能分割）

- `app/src/main/java/com/waju/factory/digitalnote/ui/DigitalNoteApp.kt`
- `app/src/main/java/com/waju/factory/digitalnote/ui/screens/*`
- `app/src/main/java/com/waju/factory/digitalnote/ui/components/*`
- `app/src/main/java/com/waju/factory/digitalnote/ui/viewmodel/*`
- `app/src/main/java/com/waju/factory/digitalnote/ui/canvas/*`
- `app/src/main/java/com/waju/factory/digitalnote/data/local/*`
- `app/src/main/java/com/waju/factory/digitalnote/data/repository/NoteRepository.kt`
- `app/src/main/java/com/waju/factory/digitalnote/navigation/Routes.kt`
- `app/src/main/java/com/waju/factory/digitalnote/model/NoteItem.kt`
- `app/src/main/java/com/waju/factory/digitalnote/data/SampleNotes.kt`
- `app/src/main/java/com/waju/factory/digitalnote/domain/FilterNotes.kt`

## テスト

`filterNotes` の単体テストを実装済みです。

## 実行方法

```powershell
./gradlew.bat testDebugUnitTest
./gradlew.bat installDebug
```

> Android Studio からは `app` モジュールを通常実行できます。

