---
name: manage-project-specs
description: Maintain, create, review, validate, and reorganize this repository's Japanese documentation under spec/, including product specifications, architecture, compatibility notes, quality guidance, findings, implementation plans, audits, ADRs, and templates. Use when Codex is asked to update spec files, register investigation results, create or complete plans, synchronize documentation after code changes, migrate the document structure, or check documentation consistency. Do not use for code-only changes that do not require documentation updates.
---

# プロジェクト仕様資料の管理

`spec/`を、現行仕様・調査結果・対応計画・履歴の関係が追跡できる状態に保つ。

## 最初に確認すること

1. リポジトリルートの`spec/README.md`を最後まで読み、分類、メタデータ、命名、状態遷移を正本として扱う。
2. `git status --short`で既存変更を確認し、ユーザーの変更を上書きしない。
3. 依頼が調査・検証だけか、資料更新まで含むかを判定する。調査だけの依頼ではファイルを変更しない。
4. 関係する実装、テスト、コミット、既存資料を確認し、推測と確認済み事実を分ける。

## 資料を更新する

- 正常な現行動作は`製品仕様/`、実現方法は`アーキテクチャ/`へ記録する。
- Androidバージョン差異は`互換性/`、ビルドや検証基準は`品質管理/`へ記録する。
- 問題は最初に`問題管理/問題一覧.md`へ登録し、次の未使用`AUD-NNN`を採番する。
- 複数箇所の変更、互換性判断、復旧設計などを伴う問題だけ`対応計画/`を作成する。
- 方針選択の理由を長期保存する場合は新しいADRを作る。既存ADRを過去にさかのぼって書き換えない。
- 特定時点の調査結果は日付付きの`監査記録/`に保存し、後から判明した現行状態は現行資料か新しい記録へ反映する。
- 新規資料は`テンプレート/`の該当ひな型を基にし、`spec/README.md`の資料一覧にも追加する。

## 問題を完了へ進める

1. 問題一覧と対応計画の状態を同期する。
2. 実装内容、検証方法、検証結果、修正コミットを記録する。
3. 動作や設計が変わった場合は、製品仕様・アーキテクチャ・互換性・品質管理も同じ変更で更新する。
4. 計画や監査記録を現行仕様の代用にしない。完了した計画は移動せず、状態を更新して履歴として残す。

## 構成を変更する

- 追跡可能性を保つため、既存資料の移動には可能な限り`git mv`を使う。
- 移動後はリポジトリ全体で旧パスを検索し、Markdownリンクと文中参照を更新する。
- ファイル名とディレクトリ名は日本語を基本とする。ただし`README.md`、`SKILL.md`、`openai.yaml`などツールが要求する名前は変更しない。
- 生の外部レポート、個人情報、認証情報を追加しない。必要な識別情報と要約だけを記録する。

## 検証する

リポジトリルートから次を実行する。

```sh
python3 .agents/skills/manage-project-specs/scripts/validate_specs.py .
git diff --check
git status --short
```

検証処理はメタデータ、分類と状態、相対リンク、資料一覧、旧パス、問題ID、計画・ADR・監査記録の命名を確認する。エラーを修正して再実行する。依頼が読み取り専用の場合は結果だけを報告する。

最後に、追加・移動・更新した資料、問題状態、検証結果、未確認事項を簡潔に報告する。コード変更は明示的に依頼された場合だけ行う。
