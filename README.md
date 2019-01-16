# DoNotSpeak

絶対にスピーカーで音楽を再生させないアプリ



## 資料

起動時チェック
ボリューム変更時チェック
ACTION_AUDIO_BECOMING_NOISYを受信時チェック
通知領域でオンオフ


https://developer.android.com/reference/android/media/AudioManager

ACTION_AUDIO_BECOMING_NOISYを受信
https://blog.ch3cooh.jp/entry/20151001/1443661200

https://stackoverflow.com/questions/13610258/how-to-detect-when-a-user-plugs-headset-on-android-device-opposite-of-action-a/13610712

ボリューム変更検出
https://stackoverflow.com/questions/6896746/is-there-a-broadcast-action-for-volume-changes

ヘッドホン接続検出
isWiredHeadsetOnは廃止
getDevices(GET_DEVICES_OUTPUTS)でTypeを確認

ミュート処理
adjustStreamVolume(STREAM_MUSIC, ADJUST_MUTE, flags)
setStreamVolume
getStreamMinVolume
http://blog.livedoor.jp/stock_club/archives/52109540.html

通知領域に常駐
ServiceをstartForegroundする
https://stackoverflow.com/questions/11292993/always-show-service-in-notification-bar
OreoからstartForegroundServiceしないといけない
https://qiita.com/naoi/items/03e76d10948fe0d45597

起動時サービス開始
BOOT_COMPLETED

とりあえずAndroid5で実装する 

