---
title: Lint品質ゲート整備
type: 対応計画
status: 完了
created: 2026-08-22
last-reviewed: 2026-08-22
applies-to: 9914846
related-findings:
  - AUD-007
---

# Lint品質ゲート整備

## 背景

Androidコード変更の基本確認にはLintを含めているが、CIはBundleとBluetooth権限試験だけを実行し、
Lintを品質ゲートとしていない。2026-08-22にコミット`ca84f8b`で`lintDebug`を実行した結果は
8 errors / 33 warningsであり、タスクは失敗した。

エラーの内訳は次のとおりである。

| Lint ID | 件数 | 対象 | 現行の保護 |
| --- | ---: | --- | --- |
| `MissingPermission` | 6 | `DNSService.isBluetoothHeadsetConnected()`のBluetooth API呼出し | `canUseBluetooth()`が設定値と実OS権限を確認するが、確認後の`SecurityException`は処理していない |
| `NewApi` | 1 | Manifestの`DNSTileService`宣言 | OSはAPI 24未満でQuick Settings Tile Serviceを起動せず、Javaクラスにも`@RequiresApi(24)`がある |
| `StartActivityAndCollapseDeprecated` | 1 | API 24–33用の`startActivityAndCollapse(Intent)` | API 34以上では`PendingIntent`版を使用する実行時分岐がある |

APIレベルの2件には実行時分岐が存在するが、Lintへ意図が明示されていない。Bluetooth権限は
`canUseBluetooth()`で事前確認しているものの、確認とAPI呼出しの間に権限状態が変化した場合の
`SecurityException`を処理していない。
警告33件にはAPI 37、依存ライブラリ更新、API 25以降のshortcut属性、未使用リソース、
monochrome icon、Locale変更追従が含まれ、AUD-007ではエラーとして扱わない。

## 目標

- 実行時の権限確認とAPI分岐を維持したまま、`lintDebug`をエラー0件で成功させる
- Bluetooth権限が事前確認後に利用不能になっても、サービスを終了せず非接続へ安全側縮退する
- Lint抑止は理由を説明できる最小範囲に限定し、新しいエラーを隠さない
- pull request、`master`、`dev`へのpushでDebugビルドとLintを自動実行し、エラーを品質ゲートにする

## 対象外

- AUD-009で扱う既存警告33件の整理
- Lint baselineによる既存エラーの固定
- `warningsAsErrors`による警告の品質ゲート化
- API 37への`compileSdk`・`targetSdk`更新
- AndroidXテスト依存関係の更新
- Unit TestとTypeScript型チェックのCI導入
- Bluetooth権限不足時の機能動作変更と再検証（AUD-003で完了済み）

## 対応方針

### Bluetooth権限

`DNSService.isBluetoothHeadsetConnected()`は、入口の`canUseBluetooth()`によるBluetooth設定と
`BLUETOOTH_CONNECT`の実OS権限確認を維持する。接続機器一覧、再生状態、機器名、アドレスを取得する
Bluetooth API境界を`try`で囲み、`SecurityException`を捕捉した場合は権限拒否を診断ログへ記録して
`false`を返す。これにより、権限不足時はBluetooth機器を非接続として扱うAUD-003の安全側動作へ
統一する。`MissingPermission`の`@SuppressLint`は使用しない。

### APIレベル

Manifestの`DNSTileService`宣言へ`tools:targetApi="24"`を追加し、このserviceがAPI 24以降専用で
あることを明示する。`minSdk = 21`とJava側の`@RequiresApi(24)`は維持する。

`Compat.startActivityAndCollapse()`では、API 34以上の`PendingIntent`版とAPI 24–33の`Intent`版という
現行分岐を維持する。API 24–33の呼出しを専用メソッドへ分離し、その1行だけへ
`StartActivityAndCollapseDeprecated`の限定抑止を付ける。

### CI

署名不要のDebugビルドworkflowを追加し、pull requestと`master`・`dev`へのpushで
`lintDebug assembleDebug`を実行する。
署名シークレットを使う既存のrelease workflowから分離し、PRやforkでも署名情報なしで実行できるよう、
`local.properties`にはSDKパスと未使用の署名設定だけを生成する。LintエラーはGradleの終了コードで
Debugビルド検証全体を失敗させ、レポートartifactは保存しない。

警告は可視化したまま成功を許可し、エラーだけを品質ゲートとする。baselineや全体抑止は追加しない。

## 対象ファイル

| ファイル | 変更内容 |
| --- | --- |
| `src/app/src/main/java/io/github/diontools/donotspeak/DNSService.java` | Bluetooth API境界で`SecurityException`を捕捉し、権限拒否時は非接続へ縮退する |
| `src/app/src/main/java/io/github/diontools/donotspeak/Compat.java` | API 24–33専用メソッドへレガシー呼出しと限定抑止を隔離する |
| `src/app/src/main/AndroidManifest.xml` | `DNSTileService`へ対応APIを明示する |
| `.github/workflows/android-build-check.yml` | pull requestとpushでDebugビルドとLintを実行する |
| `spec/品質管理/テスト方針.md` | 完了後のLint品質ゲートと警告方針を現行基準へ反映する |
| `spec/問題管理/問題一覧.md` | 実装・検証結果、修正コミット、状態を更新する |

## 実装進捗

- `DNSService.isBluetoothHeadsetConnected()`のBluetooth API境界で`SecurityException`を捕捉し、
  `BLUETOOTH_CONNECT permission: denied during device query`を記録して`false`を返すようにした
- `MissingPermission`の`@SuppressLint`は追加していない
- 変更後の`lintDebug`では`MissingPermission` 6件が解消した
- Lint全体は未対応の`NewApi` 1件と`StartActivityAndCollapseDeprecated` 1件が残り、
  2 errors / 33 warningsで失敗した
- Manifestの`DNSTileService`宣言へ`tools:targetApi="24"`を追加し、API 24以降専用であることを
  Java側の`@RequiresApi(24)`と同様に明示した
- 変更後の`lintDebug`では`NewApi` 1件が解消し、全体は未対応の
  `StartActivityAndCollapseDeprecated` 1件と33 warningsで失敗した
- API 24～33の`startActivityAndCollapse(Intent)`を`startActivityAndCollapseApi24()`へ分離し、
  レガシーAPI呼出しの1行だけへ`StartActivityAndCollapseDeprecated`の限定抑止を追加した
- 変更後の`lintDebug`は0 errors / 33 warningsで成功した。33 warningsはAUD-009で管理する
- `assembleDebug`が成功し、98KBのDebug APKを生成した
- `compileDebugAndroidTestJavaWithJavac`が成功した
- API 21 AVD `API_21_Android_5.0_Lolipop`のSDKレベルが21であることを確認し、生成した
  Debug APKを`adb install -r`でインストールした
- `io.github.diontools.donotspeak.debug`が`/data/app`配下へ登録されたことを確認した
- API 26 AVD `Pixel_6_API_26_Without_GooglePlay`へDebug APKをインストールし、Quick Settingsへ
  `DNSTileService`を追加した
- Quick Settings Tile操作時に`DNSTileService.onClick()`、`MainActivity`のSTART、
  `MainActivity.onCreate()`が順に記録され、API 24～33用の画面起動経路が動作することを確認した
- API 26のTile操作では`AndroidRuntime`のクラッシュを記録しなかった
- API 36 AVD `Pixel_9_API_36_Android_16`へDebug APKをインストールし、Quick Settingsへ
  `DNSTileService`を追加した
- SDKレベル36でQuick Settings Tileを操作し、`DNSTileService.onClick()`の84ms後に
  `MainActivity.onCreate()`が記録され、`MainActivity`が最前面になることを確認した
- API 36ではSDK条件によりAPI 34以上の`PendingIntent`版が選択され、Tile操作時に
  `AndroidRuntime`のクラッシュを記録しなかった
- API 31 AVD `DoNotSpeak_API_31`で`run-bluetooth-permission-tests.sh`を実行し、権限拒否、
  設定無効、権限ダイアログキャンセル、権限許可の4試験がすべて成功した
- API 31の拒否系では`BLUETOOTH_CONNECT permission: denied`と`set volume: 0`、設定無効では
  `useBluetooth: false`を記録し、権限許可試験では権限拒否ログを記録しなかった
- 試験後は対象プロセスが停止し、`BLUETOOTH_CONNECT`が`granted=false`へ戻ったことを確認した
- この試験は事前権限確認による拒否経路を通るため、権限確認後に発生する`SecurityException`の
  捕捉経路は実行していない
- API 36 AVD `Pixel_9_API_36_Android_16`でAPI 31と同じBluetooth権限試験4件がすべて成功し、
  拒否ログ、安全側音量0、設定無効、ダイアログキャンセル、許可時の判定を確認した
- API 31・36の試験後に対象プロセスが停止し、`BLUETOOTH_CONNECT`が`granted=false`へ
  戻ったことを確認した
- `isBluetoothHeadsetConnected()`の接続機器一覧、再生状態、機器名、アドレスの取得がすべて
  `try`内にあり、`catch (SecurityException)`が診断ログを記録して`false`を返すことを確認した
- `.github/workflows/android-build-check.yml`を追加し、pull request、`master`・`dev`へのpush、
  手動実行で`lintDebug assembleDebug`を実行するようにした
- Debugビルドworkflowは署名シークレットとレポートartifactを使用せず、LintエラーをGradleの
  終了コードでビルド検証の失敗として扱う
- workflow追加後にローカルで`lintDebug assembleDebug`を実行し、0 errors / 33 warningsで
  LintとDebug APKビルドが成功することを確認した
- workflowのYAML構文と、PR・push対象、Gradleコマンド、artifact非使用の構造を検証した
- 実装コミット`9914846`の`dev`へのpushでGitHub Actions
  [Android Build Check run 32567063064](https://github.com/diontools/DoNotSpeak/actions/runs/32567063064)が起動し、
  `lintDebug assembleDebug`が成功した
- 同じpushの[Android CI run 32567063043](https://github.com/diontools/DoNotSpeak/actions/runs/32567063043)が
  成功した
- 同じpushの[Android Emulator Tests run 32567063052](https://github.com/diontools/DoNotSpeak/actions/runs/32567063052)は、
  初回のAPI 36ジョブで`pm clear`後の`BOOT_COMPLETED`とテスト開始が競合し、
  `ForegroundServiceDidNotStartInTimeException`により失敗した。失敗ジョブの再実行では
  API 31・36の両ジョブが成功し、最終runは成功した
- 一時PR [#45](https://github.com/diontools/DoNotSpeak/pull/45)で、API 31の
  `Context.getAttributionSource()`を無条件に呼ぶ検証用コミット`13ffa51`を実行した
- [Android Build Check run 32568469636](https://github.com/diontools/DoNotSpeak/actions/runs/32568469636)は
  `NewApi` 1 error / 34 warningsを検出し、`:app:lintDebug`が終了コード1で失敗した
- 負試験後は一時PRをマージせず閉じ、ローカルとリモートの検証用ブランチを削除した

## 検証計画

1. 修正前の`lintDebug`が8 errors / 33 warningsで失敗することを確認する。
2. 修正後に`lintDebug`を実行し、エラー0件で成功することを確認する。
3. Lintレポートを確認し、`MissingPermission`、`NewApi`、
   `StartActivityAndCollapseDeprecated`以外の検査が全体で有効なことを確認する。
4. `assembleDebug`と`compileDebugAndroidTestJavaWithJavac`を成功させる。
5. API 21でDebug APKをインストールし、`DNSTileService`宣言追加による互換性回帰がないことを確認する。
6. API 24–33の代表としてAPI 26でQuick Settings Tileから画面を起動できることを確認する。
7. API 34以上の代表としてAPI 36でQuick Settings Tileから画面を起動できることを確認する。
8. API 31・36でAUD-003のBluetooth権限試験を実行し、拒否時の非接続扱いと許可時の判定を確認する。
9. `isBluetoothHeadsetConnected()`内の権限対象APIが`SecurityException`の捕捉範囲にあり、
   捕捉時に権限拒否ログを記録して`false`を返すことを確認する。
10. pull requestでDebugビルドとLintが実行され、エラーを含む試験コミットではジョブが失敗することを
   確認する。

## 完了条件

- `lintDebug`がエラー0件で成功する
- `MissingPermission`を抑止せず、`SecurityException`をBluetooth API境界で処理している
- APIレベルに関するLint指定が対象メソッドまたはManifest要素に限定され、理由が記録されている
- baseline、全体抑止、`abortOnError = false`を使用していない
- Debug APKビルドとandroidTestコンパイルが成功する
- API 21のインストール、API 26・36のQuick Settings Tile起動、API 31・36のBluetooth権限試験が成功する
- pull request、`master`、`dev`へのpushでDebugビルドとLintが実行され、エラー発生時にCIが失敗する
- 問題一覧とテスト方針へ検証結果と修正コミットを記録し、AUD-007と本計画を完了へ更新する

## 参考

- [テスト方針](../品質管理/テスト方針.md)
- [Android対応範囲](../互換性/対応Androidバージョン.md)
- [AUD-003 Bluetooth権限不足時のエミュレータ検証](AUD-003_Bluetooth権限不足時のエミュレータ検証.md)
- [AUD-009 Lint警告整理](AUD-009_Lint警告整理.md)
