---
title: 対応Androidバージョン
type: 互換性
status: 有効
created: 2026-08-16
last-reviewed: 2026-08-22
applies-to: ca84f8b
---

# 対応Androidバージョン

## 対応範囲

| 項目 | 値 |
| --- | --- |
| minSdk | 21（Android 5.0） |
| compileSdk | 36 |
| targetSdk | 36 |
| 実装言語 | Java |
| 実行時AndroidX | 使用しない |

API 21対応を維持する理由と影響は[ADR-0001](../設計判断/ADR-0001_API21対応の維持.md)、
実行時AndroidX非依存は[ADR-0002](../設計判断/ADR-0002_実行時AndroidX非依存.md)を参照する。

## 主なAPI差分

- API 21～25: `startService()`を使用する
- API 26以降: `startForegroundService()`と通知チャンネルを使用する
- API 28以降: アプリ通知と通知チャンネルのブロック状態変更Broadcastを受信する
- API 24以降: Quick Settings Tileを提供する。Manifestのservice宣言も`tools:targetApi="24"`で
  API 24以降専用であることを明示する
- API 24～33: Quick Settings Tileからの画面起動に`startActivityAndCollapse(Intent)`を使用する
- API 31以降: `BLUETOOTH_CONNECT`を実行時に確認する
- API 33以降: `POST_NOTIFICATIONS`を実行時に確認する
- API 34以降: Quick Settings Tileからの画面起動に`startActivityAndCollapse(PendingIntent)`を使用し、
  `specialUse` Foreground Service typeを使用する
- API 36: エッジツーエッジ強制へ対応する

## 回帰確認対象

| API | 主目的 |
| --- | --- |
| 21 | インストール、D8 API modeling、最低対応API |
| 26 | Foreground Service、通知チャンネル、中間API |
| 35 | Android 15退行確認 |
| 36 | targetSdk 36、エッジツーエッジ、最新主要対象 |

2026-08-22にAUD-007のDebug APKをAPI 21 AVDへインストールし、パッケージが登録されることを
確認した。同日、API 26 AVDのQuick Settings Tile操作から`DNSTileService.onClick()`を経て
`MainActivity`が起動することを確認した。API 36 AVDでもQuick Settings Tile操作からAPI 34以上の
`PendingIntent`版を経て`MainActivity`が最前面で起動し、クラッシュしないことを確認した。
同日、API 31 AVDでBluetooth権限の拒否・設定無効・ダイアログキャンセル・許可の4試験が成功した。
API 36 AVDでも同じ4試験が成功し、両APIで試験後のプロセス停止と権限revokeを確認した。

詳細なビルド制約は[ビルド設定](../品質管理/ビルド.md)、API 36対応履歴は
[Android 16対応](Android16対応.md)を参照する。
