---
title: 定期確認Alarm初回時刻修正
type: 対応計画
status: 完了
created: 2026-08-16
last-reviewed: 2026-08-17
applies-to: 28cfb8b
related-findings:
  - AUD-005
  - AUD-008
---

# 定期確認Alarm初回時刻修正

## 背景

`DNSService.setRebootTimer()`は、サービスが停止した場合に`DNSReceiver`から復旧通知を表示できるよう、
1時間間隔の定期確認Alarmを設定する。サービスが稼働中なら`DNSService.IsLive`を確認して何も表示せず、
サービスが稼働していなければ復旧通知を表示する。

現行実装は`AlarmManager.ELAPSED_REALTIME`を使用しながら、`triggerAtMillis`へ
`60 * 60 * 1000`を直接渡している。この値は待ち時間ではなく端末起動後の絶対時刻なので、
端末の稼働時間が1時間を超えている場合は過去時刻となり、Alarmが設定直後に発火する。
`onStartCommand()`の各コマンド後に同じ`PendingIntent`で再設定するため、通常操作のたびに不要な
Broadcastが発生し得る。

Androidの`AlarmManager`仕様では、経過時間基準のAlarmは`SystemClock.elapsedRealtime()`を基準にする。
過去の開始時刻を指定した繰り返しAlarmは即時発火し、不正確なAlarmは指定時刻より前には発火しないが、
DozeやOSの一括処理によって後へ遅れる可能性がある。

## 実施結果

`28cfb8b`で、初回時刻を`SystemClock.elapsedRealtime() + AlarmManager.INTERVAL_HOUR`へ修正した。
`ELAPSED_REALTIME`、非Wakeup、`setInexactRepeating()`、1時間間隔は変更していない。

2026-08-17にAPI 36の`Pixel_9_API_36_Android_16`で、製品と同じ1時間設定を使用して確認した。

- 修正前APKを端末起動1時間前から稼働させ、`origWhen=3600000`、`repeatInterval=3600000`として
  OSへ登録されることを確認した
- 端末稼働時間3698秒でサービスコマンドを再実行すると初回時刻が過去になり、実際の
  `PendingIntent`が`DNSReceiver.ACTION_REBOOT`まで配信された
- 修正後は端末稼働時間3909秒の確認時点で`origWhen=7499085`、`repeatInterval=3600000`となり、
  現在から約59分49秒先に登録されていた
- 修正後の登録から60秒間、`DNSReceiver.ACTION_REBOOT`が発生しないことを確認した
- サービスコマンドの再実行により、初回絶対時刻が`7385007`から`7499085`へ更新された
- UIの停止確認から「停止する」を実行すると`clearRebootTimer`と`onDestroy`が記録され、
  対象Alarmと稼働サービスが残らなかった
- `assembleDebug`と`compileDebugAndroidTestJavaWithJavac`が成功した

自動テストは追加していない。本番と異なる時間値、テスト専用の値注入、Alarm境界の抽象化を追加すると、
今回確認したい製品設定そのものとは異なる動作になり、低重要度の1箇所修正に対してコード量も増えるためである。
サービス全体のテスト可能性は引き続きAUD-008で扱う。

## 目標

- サービスへの最新コマンドから少なくとも1時間は定期確認Alarmを発火させない
- 端末の壁時計、タイムゾーン、手動時刻変更の影響を受けない経過時間基準を維持する
- 定期確認のために端末を起こさず、OSが許容する範囲でAlarmをまとめられる省電力特性を維持する
- 端末稼働時間が1時間を超える条件で、設定直後に`DNSReceiver.ACTION_REBOOT`が届かないことを確認する

## 対象外

- 定期確認間隔である1時間の変更
- Alarmの正確な時刻での発火保証
- Doze中に端末を起こして定期確認すること
- `DNSService.IsLive`によるサービス稼働判定方式の変更
- AUD-001で扱うForeground Service起動失敗と状態復旧の変更
- AUD-008で扱うサービス全体の自動テスト基盤整備

## 対応方針

`DNSService.setRebootTimer()`で、基準時刻と間隔を同じ経過時間軸で指定する。

```java
long interval = AlarmManager.INTERVAL_HOUR;
long firstTrigger = SystemClock.elapsedRealtime() + interval;
alarmManager.setInexactRepeating(
        AlarmManager.ELAPSED_REALTIME,
        firstTrigger,
        interval,
        rebootIntent);
```

次の現行判断は変更しない。

- `ELAPSED_REALTIME`を使い、壁時計の変更による影響を避ける
- Wakeup型へ変更せず、端末のスリープ中は次に起きた時点まで配信を待つ
- `setInexactRepeating()`を使い、OSによるAlarmの一括処理を許容する
- 同じ`PendingIntent`による再設定で、最新コマンドから1時間後を新しい初回基準とする
- 定期確認は時刻厳守のユーザー向け機能ではないため、Exact Alarm権限を追加しない

「1時間後」は最短の発火時刻を表す。不正確なAlarmなので、実際の初回発火は1時間より後になる場合がある。
Android 12以降でも、バッテリーセーバーやDozeの影響があれば通常の配信範囲を超えて遅延し得る。

## 対象ファイル

| ファイル | 変更内容 |
| --- | --- |
| `src/app/src/main/java/io/github/diontools/donotspeak/DNSService.java` | `SystemClock`を使用して初回発火時刻を算出し、間隔を`AlarmManager.INTERVAL_HOUR`で表す |
| `spec/問題管理/問題一覧.md` | 実装・検証結果、修正コミット、AUD-005の状態を更新する |
| `spec/アーキテクチャ/状態保存と復旧.md` | 完了後に定期確認の正常な時刻基準と遅延許容を現行設計へ反映する |

## 検証計画

1. 端末稼働時間が1時間を超えたAPI 36エミュレータでサービスへコマンドを送り、
   直後に`DNSReceiver.ACTION_REBOOT`が記録されないことを確認する。
2. `adb shell dumpsys alarm`で対象`PendingIntent`の次回経過時間が現在より約1時間先に設定され、
   過去時刻になっていないことを確認する。OSによる不正確化後の実配信時刻との完全一致は要求しない。
3. 同じ条件の修正前コードでは、開始直後に`DNSReceiver.ACTION_REBOOT`が記録されるか、
   次回時刻が過去として扱われることを確認し、試験が回帰を検出できることを示す。
4. サービスへ別のコマンドを送り、同じ`PendingIntent`の初回基準がそのコマンド時点から
   約1時間先へ更新されることを確認する。
5. 明示停止時に定期確認Alarmが取り消され、復旧通知が残らないことを確認する。
6. Debug APKビルドとandroidTestのJavaコンパイルを成功させる。

`dumpsys alarm`の出力形式はAndroidバージョン間で安定した公開APIではないため、この確認は永続的な
自動テストにしていない。Alarm設定境界を差し替えるサービス全体の回帰テストはAUD-008で扱う。

## 完了条件

- 初回発火時刻が`SystemClock.elapsedRealtime() + AlarmManager.INTERVAL_HOUR`で設定されている
- `ELAPSED_REALTIME`、非Wakeup、`setInexactRepeating()`が維持されている
- 端末稼働時間が1時間を超える修正前条件で問題を再現し、修正後に即時発火しないことを確認している
- Alarmの再設定と明示停止時の取消を確認している
- ビルドとandroidTestコンパイルが成功している
- 問題一覧と状態保存・復旧の現行資料に実装結果を反映している
- 修正コミットと確認日を問題一覧へ記録し、AUD-005と本計画を完了へ更新している

## 参考

- [Android Developers: AlarmManager](https://developer.android.com/reference/android/app/AlarmManager)
- [Android Developers: Alarmをスケジュールする](https://developer.android.com/develop/background-work/services/alarms)
- [2026-07-26 総合監査](../監査記録/2026-07-26_総合監査.md#5-定期アラームの開始時刻が絶対値になっている)
- [状態保存と復旧](../アーキテクチャ/状態保存と復旧.md#定期確認)
