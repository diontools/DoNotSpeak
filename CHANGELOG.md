# CHANGELOG

## v1.6.0 2021/03/xx

### 新機能

* Bluetooth設定を追加
  * ペアリングされたBluetoothデバイスのリストからイヤホンとして扱うデバイスを選択
* `android.bluetooth.a2dp.profile.action.PLAYING_STATE_CHANGED`ブロードキャストに対応
* `android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED`ブロードキャストに対応
* `android.bluetooth.headset.profile.action.AUDIO_STATE_CHANGED`ブロードキャストに対応
* `android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED`ブロードキャストに対応
* `android.media.AudioDeviceCallback`に対応（Android 6.0以降）
* `android.media.AudioManager.AudioPlaybackCallback`に対応（Android 9.0以降）

### 変更

* Bluetooth設定で選択したデバイスのMACアドレスを使用して、厳密にBluetoothイヤホンを検出

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

