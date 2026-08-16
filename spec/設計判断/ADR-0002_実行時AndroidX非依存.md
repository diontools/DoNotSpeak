---
title: 実行時AndroidX非依存
type: 設計判断
status: 有効
created: 2026-08-16
last-reviewed: 2026-08-16
applies-to: d66b892
---

# ADR-0002 実行時AndroidX非依存

## 状況

アプリ本体は小さく、Android framework APIだけで主要機能を実装している。AndroidXを実行時依存へ
追加すると、APKサイズとAPI 21互換性の検証対象が増える。

## 決定

release APKへAndroidX実行時ライブラリを追加しない。互換処理は小規模な`Compat`ヘルパーで実装する。
instrumentationテスト専用の`androidTestImplementation`ではAndroidX Testを使用してよい。

## 理由

- release APKを小さく保つ
- 現在の機能はframework APIで実現できる
- androidTest依存はrelease APKへ含まれない

## 影響

- Insets、通知、権限などのAPI差分を自前で隔離する
- AndroidX導入が合理的になった場合は、APKサイズとAPI 21回帰を測定して新しいADRで判断する
- AGP組み込みKotlinによる意図しないkotlin-stdlib混入も監視する

## 関連資料

- [ビルド設定](../品質管理/ビルド.md)
- [スクリーンショットテスト](../品質管理/スクリーンショットテスト.md)

