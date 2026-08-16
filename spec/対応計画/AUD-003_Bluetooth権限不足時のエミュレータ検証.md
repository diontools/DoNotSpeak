---
title: Bluetooth権限不足時のエミュレータ検証
type: 対応計画
status: 検証待ち
created: 2026-08-16
last-reviewed: 2026-08-16
applies-to: 7e9f386
related-findings:
  - AUD-003
  - AUD-008
---

# Bluetooth権限不足時のエミュレータ検証

## 背景

`1f74d3a`で、Bluetooth利用設定と`BLUETOOTH_CONNECT`の実権限をサービス利用時に
組み合わせ、権限不足時はBluetoothプロファイルの初期化を待たずに安全側の音量制御へ進むよう
修正した。計画作成時点ではDebug本体とandroidTestのJavaコンパイルまで成功していたが、
Android 12以降の実OS権限、`MainActivity`のキャンセル経路、`DNSService`の診断ログと
音量制御を通した自動検証は未実施だった。

計画作成時のandroidTestは`SmokeTest`と`ScreenshotTest`だけであり、サービスの権限拒否経路を検証しない。
今回の回帰はBluetooth実機の接続再現ではなく、権限不足というOS状態に対するサービスの判断が中心なので、
API 31およびAPI 36のエミュレータを正本の検証環境とする。

## 実施状況

2026-08-16にandroidTest、ADB実行スクリプト、API 31・36のGitHub Actions matrixを実装した。

- API 36の`Pixel_9_API_36_Android_16`で、拒否、設定無効、ダイアログキャンセル、許可の4試験を3回連続実行し、すべて成功した
- API 31の`DoNotSpeak_API_31`で同じ4試験を3回連続実行し、すべて成功した
- 最終コードへ戻した後にも4試験を再実行し、すべて成功した
- `isBluetoothInitialized()`を設定値だけで判定する修正前条件へ一時的に戻すと、拒否試験が`BLUETOOTH_CONNECT permission: denied`の欠落を検出して失敗した
- 一時的な本番コード変更は元へ戻し、`DNSService.java`にテスト由来の差分を残していない
- GitHub ActionsのエミュレータActionはv2.38.0のrelease commit `a421e43855164a8197daf9d8d40fe71c6996bb0d`へ固定した

ローカルのAPI 31・36検証は完了した。GitHub Actionsの初回実行結果が未確認なので、
計画とAUD-003の状態は`検証待ち`を維持する。

## 目標

- API 36エミュレータで、権限不足時にサービスがBluetooth初期化を待たないことを自動検証する
- API 31エミュレータで、`BLUETOOTH_CONNECT`導入境界の同じ動作を確認する
- 権限ダイアログのキャンセル後も`USE_BLUETOOTH=true`が維持され、サービス側で実権限を評価することを確認する
- 設定無効、権限不足、プロファイル初期化待ちの診断ログを区別して検証する
- テスト前後の音量、設定、サービス、Logger、権限状態を復元し、反復実行できるようにする
- ローカル実行とGitHub Actionsで同じテスト本体を使用する

## 対象外

- 実Bluetooth機器のペアリング、A2DP再生、SCO通話音声の検証
- メーカー独自の権限画面やBluetoothスタック差異の検証
- AUD-008で管理するサービス全体のテスト不足の解消
- AUD-007で管理する既存Lintエラーの解消

## 対応方針

### 1. エミュレータ上でサービスを観測する

androidTestと`DNSService`は同一プロセスで動作するため、テスト開始前に
`DNSService.Logger`へ記録用の`DiagnosticsLogger`を設定する。テスト用Loggerはスレッドセーフな
メッセージ一覧と待機機構を持ち、固定時間のsleepではなく、目的のログまたはタイムアウトを待つ。

本番コードへBluetooth状態を偽装するフックは追加しない。次の既存境界をそのまま通す。

- `DNSSetting`による`USE_BLUETOOTH`の保存と`applySettings()`による読込
- `PermissionUtility.isBluetoothGranted()`による実OS権限の確認
- `DNSService.canUseBluetooth()`と`isBluetoothInitialized()`による実効判定
- `DNSService.mute()`から`setVolumeTo(0, 0)`までの安全側動作

テスト用Loggerを別ファイルにする場合も`src/androidTest`内だけに置き、release APKへ含めない。

### 2. 権限拒否時のサービス試験

`BluetoothPermissionServiceTest.permissionDenied_doesNotWaitForProfiles()`をAPI 31とAPI 36で実行する。

事前条件:

- `BLUETOOTH_CONNECT`は拒否状態
- `USE_BLUETOOTH=true`
- `USE_NOTIFICATION=false`として通知権限ダイアログを試験対象から除外する
- 音楽ストリーム音量を0より大きい検証用値へ設定する

Foreground Serviceのバックグラウンド起動制限を避けるため、テストは`MainActivity`を
`DNSService.ACTION_SWITCH`で前景起動し、既存のユーザー操作経路からサービスを開始する。

確認項目:

- `PermissionUtility.isBluetoothGranted()`が`false`
- サービス起動後も`DNSSetting.getUseBluetooth()`が`true`
- `BLUETOOTH_CONNECT permission: denied`が記録される
- 同じ更新処理で`Bluetooth initialized: false`と`Bluetooth not initialized`が記録されない
- `set volume: 0`が記録され、最終的な音楽ストリーム音量が0になる
- サービスが異常終了せず、明示停止まで稼働する

これにより、エミュレータがBluetoothアダプターを提供しない場合でも、権限不足をアダプター状態より先に
判定して処理を継続したことを確認できる。

### 3. 権限ダイアログキャンセルのE2E試験

`BluetoothPermissionFlowTest.canceledDialog_preservesSettingAndStartsService()`をAPI 36で必須実行し、
API 31でも実行する。

手順:

1. アプリデータを初期化し、`BLUETOOTH_CONNECT`を拒否状態にする
2. `USE_BLUETOOTH=true`、`USE_NOTIFICATION=false`を保存する
3. actionなしで`MainActivity`を起動する
4. アプリのBluetooth権限説明ダイアログが表示されたことをUiAutomatorで確認する
5. Back操作で説明ダイアログをキャンセルする
6. Activity完了後にサービスのログと状態を待つ

確認項目:

- `PermissionDialogFragment.onCancel()`の経路を通る
- `USE_BLUETOOTH=true`が書き換えられない
- サービスが開始される
- `BLUETOOTH_CONNECT permission: denied`が記録される
- Bluetooth初期化待ちへ入らず、安全側の音量制御まで到達する

文字列翻訳への依存を減らすため、ダイアログの検出には表示文言ではなく対象パッケージ、window、
Android標準button IDを優先する。Back操作後の判定は画面表示ではなくサービスの記録Loggerで行う。

### 4. 設定無効と権限許可の対照試験

誤った理由で拒否試験が成功していないことを確認するため、次の対照試験を別のinstrumentation実行で行う。

| 試験 | 設定 | 権限 | 必須の結果 |
| --- | --- | --- | --- |
| `settingDisabled_logsSettingState()` | 無効 | 拒否 | `useBluetooth: false`があり、権限拒否ログがない |
| `permissionGranted_doesNotLogDenial()` | 有効 | 許可 | 権限拒否ログがない |

権限許可時のエミュレータはBluetoothアダプターを提供しないか、無効状態の場合がある。そのため
`Bluetooth not supported`、`Bluetooth disabled`、`Bluetooth initialized: false/true`のどれになるかは
対照試験の成否に使用しない。実機Bluetooth接続の代用にはしない。

### 5. 権限状態をinstrumentationの外側で設定する

実行中の対象アプリから`BLUETOOTH_CONNECT`をrevokeすると、OSが対象プロセスを終了してテスト自体が
中断する可能性がある。権限状態はホスト側のADB実行スクリプトで各instrumentation起動前に設定する。

スクリプトは明示されたemulator serialだけを対象に、次の順で実行する。

1. Debug APKとandroidTest APKをビルド・インストールする
2. 対象パッケージを停止し、データを初期化する
3. `pm revoke`後に拒否系テストだけを実行する
4. 対象パッケージを停止し、データを再初期化する
5. `pm grant`後に許可系テストだけを実行する
6. テスト終了後にサービス停止、音量復元、権限revokeを行う

実行対象をclassまたはmethodで明示し、JUnitの実行順には依存しない。スクリプトはADB結果だけでなく
instrumentation出力の`OK`または失敗件数を確認し、テスト失敗時に非0で終了する。

### 6. API 31・36エミュレータの実行経路

ローカルでは既存AVDを起動し、serialを明示して同じスクリプトを実行する。複数端末接続時に
暗黙の対象選択を行わない。

GitHub Actionsにはエミュレータ専用workflowを追加し、API 31とAPI 36をmatrix実行する。

- API 36の拒否試験とキャンセルE2E試験を必須ゲートとする
- API 31でも同じ試験を実行し、権限モデル導入境界を確認する
- AVDは毎回クリーンな状態から作成する
- GradleとAndroid SDKのキャッシュは利用しても、アプリデータやsnapshotは試験間で共有しない
- emulator起動、APKインストール、各試験には個別のタイムアウトを設定する
- 失敗時はinstrumentation出力と対象タグのLogcatをartifactとして保存する

外部GitHub Actionを採用する場合はバージョンタグだけでなくcommit SHAへ固定し、権限と保守状況を
実装時に確認する。

### 7. 後始末と再現性

各テストは`finally`または`@After`で次を実行する。

- 起動した`DNSService`を停止する
- 変更前の音楽ストリーム音量を復元する
- 変更前のBluetooth・通知設定を復元する
- `DNSService.Logger`を解除する
- callback、待機スレッド、Activityを残さない

テスト開始時にはAPI level、対象パッケージ、権限、設定、音量を記録し、事前条件が成立しない場合は
skipではなく失敗とする。エミュレータ依存の待機は上限10秒とし、失敗時に最後に得たログをassertionへ含める。

## 対象ファイル

| ファイル | 変更内容 |
| --- | --- |
| `src/app/src/androidTest/java/io/github/diontools/donotspeak/BluetoothPermissionServiceTest.java` | 拒否、設定無効、許可のサービス試験 |
| `src/app/src/androidTest/java/io/github/diontools/donotspeak/BluetoothPermissionFlowTest.java` | 権限説明ダイアログキャンセルのE2E試験 |
| `src/app/src/androidTest/java/io/github/diontools/donotspeak/RecordingDiagnosticsLogger.java` | サービス診断ログの待機・検証補助 |
| `scripts/android/run-bluetooth-permission-tests.sh` | serial、権限状態、対象testを制御するADB実行スクリプト |
| `.github/workflows/android-emulator-tests.yml` | API 31・36のエミュレータmatrix |
| `spec/品質管理/テスト方針.md` | 実装後の自動テストと実行コマンドを現行方針へ反映 |
| `spec/問題管理/問題一覧.md` | 検証結果、テストコミット、AUD-003の状態を更新 |

原則として`src/main`は変更しない。安定した待機や状態確認に本番コードの変更が必要になった場合は、
debug限定かつpackage-privateの最小境界とし、release APKへのテスト用操作混入をレビューする。

## 実装順序

1. 記録Loggerと共通の待機・後始末処理をandroidTestへ追加する
2. API 36で権限拒否時のサービス試験を成立させる
3. API 36で権限説明ダイアログキャンセルのE2E試験を成立させる
4. 設定無効と権限許可の対照試験を追加する
5. API 31で同じ試験を実行し、API差異を吸収する
6. serialと権限状態を制御するADB実行スクリプトを追加する
7. API 31・36のGitHub Actions matrixを追加する
8. 3回連続でクリーン実行し、flakyでないことを確認する
9. テスト方針とAUD-003へ実行結果・コミットを反映する

## 検証計画

- `compileDebugJavaWithJavac`と`compileDebugAndroidTestJavaWithJavac`を成功させる
- API 36のクリーンAVDで拒否、キャンセル、設定無効、許可の全試験を3回連続成功させる
- API 31のクリーンAVDで同じ全試験を3回連続成功させる
- 拒否試験が修正前の条件を検出することを、`isBluetoothInitialized()`が設定値だけを見る状態へ一時的に戻した作業ツリーで確認する。確認用変更はコミットしない
- 試験後にサービス、設定、音量、権限状態が残っていないことを確認する
- `python3 .agents/skills/manage-project-specs/scripts/validate_specs.py .`を成功させる
- `git diff --check`を成功させる

## 完了条件

- API 31とAPI 36で全エミュレータ試験が安定して成功する
- 権限拒否ログ、初期化非待機、設定値維持、安全側の音量0を自動判定できる
- キャンセル経路がActivityからServiceまでE2Eで検証される
- 許可／拒否のテストが実行順や以前のインストール状態へ依存しない
- GitHub ActionsでAPI 36を必須ゲートとして実行できる
- 失敗時にinstrumentation出力とLogcatから原因を判断できる
- AUD-003を`対応済み`へ更新し、テスト実装コミットと検証結果を記録する
- AUD-008には今回追加した検証範囲だけを記録し、問題全体は未対応のまま維持する

## 参考

- 修正コミット: `1f74d3a`
- [問題一覧](../問題管理/問題一覧.md#aud-003-bluetooth権限不足時に初期化を待ち続ける)
- [権限管理](../アーキテクチャ/権限管理.md)
- [テスト方針](../品質管理/テスト方針.md)
