# publisher

リリース AAB を Google Play にアップロードするスクリプト。
`master` への push で `.github/workflows/release.yml` から実行される。
`dev` への push では同じ workflow がビルド検証まで行い、このスクリプトは実行されない。

## 構成

| ファイル | 内容 |
| --- | --- |
| `index.ts` | 本体。単一ファイル |
| `settings.json` | アップロード先の設定 |
| `credentials.json` | サービスアカウントの鍵。gitignore 対象 |
| `bundletool.jar` | 実行時にダウンロードされる。gitignore 対象 |

### `settings.json`

| キー | 値 |
| --- | --- |
| `applicationId` | `io.github.diontools.donotspeak` |
| `track` | `internal`。Play Console 側に存在するトラック名と一致する必要がある |
| `releaseFilePath` | AAB のパス（このディレクトリからの相対） |
| `releaseNotesDir` | `../release-notes`。`<言語>-<地域>.txt` のみを読む（`ja-JP.txt` / `en-US.txt`） |

## 処理の流れ

1. `settings.json` と `credentials.json` を読む
2. AAB の sha256 を計算し、`release-notes/` からリリースノートを読む
3. bundletool をダウンロードし、AAB の manifest から `versionCode` / `versionName` を取り出す
4. edit を開始し、トラックの存在を確認する
5. 同じ `versionCode` の bundle が Play に既にあるか調べる
   - **無い**: AAB をアップロードし、トラックを `status: draft` で更新する
   - **有る**: sha256 を照合し、トラックを既存の `status` のまま patch する
6. edit を commit する

`draft` で作成するため、公開は Play Console 上で手動で行う。

## 仕様上の制約

**`versionCode` を上げないコミットを `master` に push すると失敗する。**

その `versionCode` の bundle は Play に既にあるため 5. の「有る」経路に入るが、AAB の
ビルドはバイト単位で再現しないため sha256 が一致せず、`file hash change detected` で
停止する。README の修正だけを push した場合などが該当する。

リリース対象の変更は `versionCode` を上げてから push する（コミットメッセージの
`vc<versionCode>` 接頭辞については `CLAUDE.md` を参照）。

## ローカルでの実行

CI 専用だが、手元でも動かせる。Play に対して実際に書き込むので注意。

```sh
cd publisher
npm ci
# Google Cloud のサービスアカウント鍵を配置する
cp <ダウンロードした鍵>.json credentials.json
# ../src/app/build/outputs/bundle/release/app-release.aab が必要
npm run upload
```

型チェックのみ:

```sh
npm run typecheck
```

## 依存関係

`googleapis` が `node >= 18` を要求する。CI では Node 24 を使う。
HTTP クライアントは Node 組み込みの fetch を使うため、`node-fetch` には依存しない。

- **tsx**: TypeScript のまま実行するために使う。`ts-node --esm` は Node 20 以降の
  ローダー仕様変更に追随できておらず、TypeScript 7 のコンパイラ API にも対応しないため
  置き換えた。`typescript` は型チェック専用。
- **overrides**: `googleapis-common` が `gaxios` と `google-auth-library` をパッチ
  バージョンまで完全固定しており、
  1. `gaxios@7.1.3` 経由で `rimraf → glob → minimatch → brace-expansion` の脆弱性が残る
  2. `google-auth-library` が二重に解決され、`GoogleAuth` の型が別物と判定されて
     `Androidpublisher` の `auth` に渡せなくなる

  の 2 点が起きるため、上位バージョンに寄せている。`googleapis-common` が固定を緩めたら
  削除してよい。
- **bundletool**: バージョンを `index.ts` にハードコードしている。AGP を上げて AAB の
  形式が変わると `dump manifest` に失敗しうるので、その際は追従する。
