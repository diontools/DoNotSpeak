---
title: 対応Androidバージョン
type: 互換性
status: 有効
created: 2026-08-16
last-reviewed: 2026-08-16
applies-to: d66b892
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
- API 24以降: Quick Settings Tileを提供する
- API 31以降: `BLUETOOTH_CONNECT`を実行時に確認する
- API 33以降: `POST_NOTIFICATIONS`を実行時に確認する
- API 34以降: `specialUse` Foreground Service typeを使用する
- API 36: エッジツーエッジ強制へ対応する

## 回帰確認対象

| API | 主目的 |
| --- | --- |
| 21 | インストール、D8 API modeling、最低対応API |
| 26 | Foreground Service、通知チャンネル、中間API |
| 35 | Android 15退行確認 |
| 36 | targetSdk 36、エッジツーエッジ、最新主要対象 |

詳細なビルド制約は[ビルド設定](../品質管理/ビルド.md)、API 36対応履歴は
[Android 16対応](Android16対応.md)を参照する。

