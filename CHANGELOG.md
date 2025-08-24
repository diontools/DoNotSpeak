# CHANGELOG

## v1.10.0 2025/08/24

### 新機能

* Android 15 に対応
* このアプリを停止 のショートカットを追加
* 開発者サポートリンクを追加

## v1.9.1 2023/10/08

### 変更

* Android 14 以降で `TileService.startActivityAndCollapse(Intent)` を `PendingIntent` に変更

## v1.9.0 2023/08/27

### 新機能

* Android 14 に対応
  * 定期的な起動確認を通知に変更（正確なアラームが使用できなくなったため）
    * `SCHEDULE_EXACT_ALARM`権限を削除
  * フォアグラウンドサービスタイプを"specialUse"に設定
    * `FOREGROUND_SERVICE_SPECIAL_USE`権限を追加
  * 通知が閉じられた時に通知を再作成（Ongoingな通知が閉じられるようになったため）

### 変更

* 自動バックアップを止め、Key-Valueバックアップに変更（常時起動のアプリと相性が悪いため）

## v1.8.1 2022/12/29

### 変更

* Android 12 以降に導入されたフォアグラウンドサービスの起動制限を回避する (https://developer.android.com/about/versions/12/foreground-services?hl=ja#alarm-manager)
  * `SCHEDULE_EXACT_ALARM`権限を追加
  * RebootTimerで`set`から`setExact`を使用するように変更

## v1.8.0 2022/12/24

### 新機能

* Android 12 / Android 13 に対応
  * `BLUETOOTH_CONNECT`権限を追加（Android 12以降はBluetoothデバイスの情報を得ることが許可制になる）
  * `POST_NOTIFICATIONS`権限を追加（Android 13以降は通知が許可制になる）
  * `AlarmManager`の`setExactAndAllowWhileIdle`と`setExact`の代わりに`setAndAllowWhileIdle`と`set`を使用する。(`SCHEDULE_EXACT_ALARM`権限を回避)
  * 通知タップからの通知トランポリンを回避
  * 通知に`FOREGROUND_SERVICE_IMMEDIATE`を指定
  * `PendingIntent`に`FLAG_IMMUTABLE`を指定
  * `dataExtractionRules`に対応（Android 12以降のクラウドバックアップ・D2D転送）
* 再生停止を要求する機能を追加
* 画面を消灯しない機能を追加

### 変更

* 通知のデザインを標準的なデザインに変更（今後のAndroidの変更に対応しやすくする）
* 通知ドットを無効化（通知を使用するとショートカットを表示できる数が減るため）
* メニュー画面の通知バーの色を透過色に変更

## v1.7.0 2022/01/09

### 新機能

* `AudioManager.adjustStreamVolume` を使用する設定を追加（OPPO / Realme デバイス用）
* 常にログを記録する機能を追加
* `AlarmManager.set(RTC_WAKEUP)`で1時間毎に起動する（OSによる停止を回避）

### 変更

* Bluetoothの初期化処理中にミュート処理が行われないように変更
* サービスの状態を保存し、サービス復帰時に状態を復元するように変更

## v1.6.1 2021/04/02

### 新機能

* USBヘッドセットに対応

## v1.6.0 2021/03/27

### 新機能

* Bluetooth設定を追加 (Android 6.0以降)
  * ペアリングされたBluetoothデバイスのリストからイヤホンとして扱うデバイスを選択
* `android.bluetooth.a2dp.profile.action.PLAYING_STATE_CHANGED`ブロードキャストに対応
* `android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED`ブロードキャストに対応
* `android.bluetooth.headset.profile.action.AUDIO_STATE_CHANGED`ブロードキャストに対応
* `android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED`ブロードキャストに対応
* `android.media.AudioDeviceCallback`に対応（Android 6.0以降）
* `android.media.AudioManager.AudioPlaybackCallback`に対応（Android 9以降）

### 変更

* Bluetooth設定で選択したデバイスのMACアドレスを使用して、厳密にBluetoothイヤホンを検出 (Android 6.0以降)
* `android.permission.BLUETOOTH`パーミッションを追加

## v1.5.0 2021/03/01

### 新機能

* 診断ツールを追加
  * サービスの動作ログの表示と開発者へのメール送信

### 変更

* `android.media.VOLUME_CHANGED_ACTION`がループしてしまう問題を修正 (Android 5.0)

## v1.4.0 2021/02/21

### 変更

* 音量変更の検出方法をシステム設定の監視から`android.media.VOLUME_CHANGED_ACTION`ブロードキャストに変更

## v1.3.0 2021/02/11

### 新機能

* アプリを停止するメニューを追加
* スピーカーを指定時刻まで有効化する機能を追加

### 変更

* デバイスのテーマを使用する（Android 10以降）
* 通知にライトテーマを使用する（Android 9以前）

## v1.2.0 2020/09/11

### 新機能

* 切り替えショートカットを追加（Android 7.1以降）
  * 画面消灯まで有効化と無効化をトグル操作
* スピーカー有効化時に音量を復元するオプションを追加
* Android 11 をサポート

## v1.1.0 2019/09/07

### 新機能

* クイック設定タイルを追加（Android 7.0以降）
  * ロック画面で操作可能
  * 長押しで「画面消灯までスピーカー有効化」を呼び出し（ロック画面ではロック解除必須）
* ショートカットを追加（Android 7.1以降）
  * メニューを表示
  * 画面消灯までスピーカー有効化
* アダプティブアイコンに対応

### 変更

* 通知のチャンネル名を`DoNotSpeak`から`ステータス`に変更
* `startForegroundService`を使用していない問題を修正
* Android 8.0 以降で`ACTION_AUDIO_BECOMING_NOISY`が処理されていない問題を修正
* スピーカー有効状態のとき`ACTION_AUDIO_BECOMING_NOISY`でミュートにしないように修正
* `ACTION_HEADSET_PLUG`でミュート処理をするように変更

## v1.0.2 2019/01/30

* MODIFY_AUDIO_SETTINGSパーミッションを削除

## v1.0.1 2019/01/29

* 初回リリース

