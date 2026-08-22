# スクリーンショット

`src/app/src/androidTest/` のスクリーンショットテストで取得した画面。
撮影方法と制約は[スクリーンショットテスト](../spec/品質管理/スクリーンショットテスト.md)を参照。

## 構成

```
screenshots/<versionCode>/api<SDK>/<画面>-<ロケール>-<ナビゲーション>-<テーマ>.png
```

`<versionCode>` はコミットメッセージの `vc<versionCode>` 接頭辞（`CLAUDE.md`）と同じ値。
どの時点の見た目かをこれで特定する。

見た目を変える作業は versionCode を分けて行う。同一の versionCode に変更前後の 2 つの
状態が混ざると、後からどちらの画像か判別できなくなるため。

| 部分 | 値 |
| --- | --- |
| 画面 | `disable` / `disable-options` / `setting` / `diagnostics` |
| ロケール | `ja` / `en`（API 33 未満は AVD のシステムロケール） |
| ナビゲーション | `gestural` / `threebutton`（API 29 未満は `default`） |
| テーマ | `light` / `dark`（API 29 未満は `default`） |

ナビゲーションとテーマの全組み合わせを撮るのは `ja` のみ。`en` は端末の初期状態である
`gestural-light` だけ撮る。理由は[スクリーンショットテスト](../spec/品質管理/スクリーンショットテスト.md)を参照。

API 33 未満（api26 / api21）はアプリ単位のロケール切替ができないため、AVD 自体を
日本語に設定して撮っている。設定手順は[スクリーンショットテスト](../spec/品質管理/スクリーンショットテスト.md)を参照。

## 用途

リリース間で見た目の変化を比較する。差分が出た場合、意図した変更か事故かを人が判断する。

## 取得手順

```sh
cd src
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=io.github.diontools.donotspeak.ScreenshotTest

# Windows の Git Bash では MSYS_NO_PATHCONV=1 が必要
MSYS_NO_PATHCONV=1 adb -s <serial> pull /sdcard/Download/shots/api36 ../screenshots/vc<N>/
```

エミュレータを複数起動しておけば 1 コマンドで並列に撮影される。

## 最適化

`screencap` が出力する PNG は圧縮されていないため、コミット前に可逆最適化する。

```sh
oxipng -o 4 --strip safe -a -r screenshots
```

`-o max` は `-o 4` と比べて改善がなく、時間だけかかる。実績で 32.5% 削減（15.4MB → 10.4MB）。

## 履歴

| versionCode | 内容 | 枚数 |
| --- | --- | --- |
| 90 | ダイアログ改善後・Android 16 対応前（`targetSdk = 35`） | 47 (api36:20 / api35:20 / api26:4 / api21:3) |
| 91 | Android 16 対応後（`targetSdk = 36`・エッジツーエッジ） | 47 (api36:20 / api35:20 / api26:4 / api21:3) |
