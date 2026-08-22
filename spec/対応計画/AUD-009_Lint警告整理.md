---
title: Lint警告整理
type: 対応計画
status: 計画済み
created: 2026-08-22
last-reviewed: 2026-08-22
applies-to: ca84f8b
related-findings:
  - AUD-009
---

# Lint警告整理

## 背景

2026-08-22にコミット`ca84f8b`で`lintDebug`を実行し、8 errors / 33 warningsで失敗することを
確認した。AUD-007は8件のエラー解消とCIへのLint導入へ限定し、既存警告33件は本計画へ分離する。

| Lint ID | 件数 | 内容 |
| --- | ---: | --- |
| `UnusedAttribute` | 24 | main・debugのshortcut定義にあるAPI 25以降の3属性を各4項目で検出 |
| `GradleDependency` | 4 | `compileSdk`とAndroidXテスト依存関係3件の更新通知 |
| `ConstantLocale` | 2 | `DNSService`と`DiagnosticsLogger`が起動時のLocaleをstatic formatterへ保持 |
| `OldTargetApi` | 1 | `targetSdk = 36`に対するAPI 37への更新通知 |
| `UnusedResources` | 1 | `disable_alert_untilScreenOffButton`が未使用 |
| `MonochromeLauncherIcon` | 1 | adaptive iconに`monochrome`レイヤーがない |

## 目標

- 33件をカテゴリごとに確認し、実装修正、限定的な抑止、別問題への分割のいずれかを決定する
- 修正する警告は影響範囲に応じたビルド、端末、画面検証を定義する
- 抑止する警告は対象と理由を最小範囲に記録し、新しい警告を不必要に隠さない
- 各判断と検証結果を問題一覧、品質管理、互換性または製品仕様へ反映する

## 対象外

- AUD-007で扱う8件のLintエラー解消
- AUD-007で扱うLintのCI品質ゲート導入
- 警告を確認せず一括抑止すること
- Lint baselineへ警告を固定すること

## 対応方針

次の順序で1カテゴリずつ内容と影響を確認し、対応方針を確定してから次へ進む。

1. `UnusedAttribute` 24件
2. `GradleDependency` 4件と関連する`OldTargetApi` 1件
3. `ConstantLocale` 2件
4. `UnusedResources` 1件
5. `MonochromeLauncherIcon` 1件

各カテゴリでは次を記録する。

- 警告が示す実害または将来リスク
- 現行仕様として意図した状態か
- 修正、限定的な抑止、別問題への分割の選択
- 対象ファイルと必要な検証

個別方針は確認後に本計画へ追記する。全カテゴリの判断が終わるまでは、警告を一括で無効化しない。

## 対象ファイル

現時点で警告が報告されている対象は次のとおり。変更対象はカテゴリごとの確認後に確定する。

- `src/app/build.gradle`
- `src/app/src/main/java/io/github/diontools/donotspeak/DNSService.java`
- `src/app/src/main/java/io/github/diontools/donotspeak/DiagnosticsLogger.java`
- `src/app/src/main/res/xml/shortcuts.xml`
- `src/app/src/debug/res/xml/shortcuts.xml`
- `src/app/src/main/res/values/strings.xml`
- `src/app/src/main/res/values-ja/string.xml`
- `src/app/src/main/res/drawable-anydpi-v26/ic_launcher.xml`
- `spec/品質管理/テスト方針.md`
- `spec/問題管理/問題一覧.md`

## 検証計画

1. 各カテゴリの変更前後で`lintDebug`のLint ID、件数、対象箇所を比較する。
2. Androidコードまたはリソース変更時は`assembleDebug`とandroidTestコンパイルを実行する。
3. API対応範囲に関わる変更はAPI 21と影響対象APIで確認する。
4. shortcut変更はAPI 21とAPI 25以上でランチャーおよびショートカット動作を確認する。
5. Locale変更は実行中の言語切替後に日時表示を確認する。
6. ランチャーアイコン変更は対応ランチャーで通常表示とmonochrome表示を比較する。
7. 最終的な警告件数と、残す警告がある場合はその理由を記録する。

## 完了条件

- 33件すべてについて対応方針と理由が記録されている
- 修正対象について必要なビルド・端末・画面検証が完了している
- 抑止が必要な場合は対象が限定され、理由がコードまたは資料に記録されている
- baselineや全体抑止を使用していない
- 問題一覧と関連する現行資料へ実装結果、検証結果、修正コミットを反映している
- AUD-009と本計画の状態が同期されている

## 参考

- [テスト方針](../品質管理/テスト方針.md)
- [AUD-007 Lint品質ゲート整備](AUD-007_Lint品質ゲート整備.md)
- [対応Androidバージョン](../互換性/対応Androidバージョン.md)
