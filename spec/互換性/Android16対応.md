---
title: Android 16対応
type: 互換性
status: 完了
created: 2026-07-26
last-reviewed: 2026-08-16
applies-to: d66b892
---

# Android 16 (API 36) 対応

targetSdk を 35 → 36 に上げるための調査結果と作業計画。

## 前提

- **この対応は `versionCode = 91` として行う。** ダイアログ改善までを含む vc90 とは
  versionCode を分ける。同一 versionCode に見た目の変更前後が混ざると、
  スクリーンショットがどちらの状態か判別できなくなるため
- 現状: `compileSdk = 35` / `targetSdk = 36` ではなく 35、`minSdk = 21`
- SDK は `android-36` プラットフォームと build-tools 36.0.0 が導入済み
- AVD `Pixel_9_API_36_Android_16` が利用可能
- 純 Java・AndroidX 依存ゼロ・ネイティブコードなし

## 対応が必要な変更

### エッジツーエッジの強制（唯一の破壊的変更）

`values-v29/styles.xml` の `ActivityTheme` にある

```xml
<item name="android:windowOptOutEdgeToEdgeEnforcement" tools:targetApi="35">true</item>
```

は targetSdk 36 では **Android 16 端末で無視される**（属性自体が deprecated）。
Android 15 端末では依然として効くため、残すと 15 と 16 で表示が食い違う。削除して
inset を自前で処理する。

対象は `ActivityTheme` を使う 2 画面:

- `SettingActivity`（`activity_setting.xml`、末尾が `ListView`）
- `DiagnosticsActivity`（`activity_diagnostics.xml`、末尾が `ListView`）

`MainActivity` は `AlertDialog` ベース（ダイアログウィンドウ）のため強制対象外。目視確認のみ。

実装方針:

- `androidx.core` は導入しない（APK サイズを抑えるため AndroidX 依存ゼロを維持する）
- `Compat.java` に inset 適用ヘルパーを追加する。API 30+ は
  `WindowInsets.getInsets(WindowInsets.Type.systemBars())`、それ未満は
  `getSystemWindowInsetTop()` / `getSystemWindowInsetBottom()`（API 21+）にフォールバック
- `ListView` は `clipToPadding=false` + bottom padding とし、ナビゲーションバー下まで
  スクロールできるようにする

## 影響がないことを確認済みの変更

| 変更 | 判定理由 |
| --- | --- |
| Predictive Back（`onBackPressed` が呼ばれない） | `onBackPressed` / `KEYCODE_BACK` のオーバーライドなし |
| 大画面での orientation / resizability 無視 (sw≥600dp) | `screenOrientation`・`setRequestedOrientation` 未使用 |
| `elegantTextHeight` の無効化 | 対象言語（アラビア語・タイ語等）なし。`en` / `ja` のみ |
| Safer Intents | `intentMatchingFlags` による opt-in のため既定では無効 |
| ローカルネットワーク権限 | ネットワーク通信なし |
| 16KB ページサイズ | ネイティブコードなし |
| FGS `specialUse` | targetSdk 36 起因の変更なし |
| `scheduleAtFixedRate` の間引き | 未使用 |

## 作業手順

1. `versionCode` を 91 に上げ、`compileSdk` / `targetSdk` を 36 に変更して
   ビルドが通ることを確認する
2. エッジツーエッジ対応（テーマ修正・`Compat` に inset ヘルパー追加・レイアウト適用）
3. **API 21 エミュレータでの回帰確認**。[ビルド設定](../品質管理/ビルド.md)の手順で `classes4.dex` のサイズと
   `adb install` の成否を必ず見る。compileSdk を上げると D8 の API modeling 抑止が
   効かなくなる可能性があるため、この確認を省略しない
4. Android 16 (`Pixel_9_API_36_Android_16`) で 3 画面（メインダイアログ / 設定 / 診断）を
   確認。あわせて API 26・35 でも退行がないか確認する
5. `screenshots/vc91/` に撮り直し、`screenshots/vc90/` と比較する
6. CHANGELOG / release-notes を更新する

## 対応結果（vc91）

- インセットは `Compat.applySystemBarInsets(root, listView)` で適用する。ルートに
  left / top / right、`ListView` に bottom（`clipToPadding=false`）
- **エッジツーエッジ時、ルートに届く top インセットはステータスバー高 + アクションバー高**
  （API 36 の Pixel 9 で 289px）。デコアがアクションバー分を差し引いた位置に
  コンテンツを置くのではなく、自身のコンテンツインセットを下位に流すため。
  これをそのまま padding にすると vc90 と同じ位置に収まる
- API 36 / 35 は下端がナビゲーションバー下まで描画されるようになった。
  API 26 / 21 は vc90 と完全に同一（二重 padding なし）
- API 21 での `adb install` は成功。`classes4.dex` は 13KB で `com.android.tools.r8.*` のみ。
  compileSdk 36 でも API modeling の抑止は効いている

## 検証方法

対応前後の画面比較と今後の出荷前テストのため、エミュレータ起動・画面遷移・
スクリーンショット取得を自動化する。詳細は[スクリーンショットテスト](../品質管理/スクリーンショットテスト.md)を参照。
