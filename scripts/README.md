# scripts

Google Play に関するローカルスクリプトと CI スクリプト。

`package.json` と `node_modules` はこのディレクトリで共有し、処理ごとのソースと設定は
サブディレクトリに分離する。

| ディレクトリ | 用途 | 認証 |
| --- | --- | --- |
| `publisher/` | AAB のアップロード | `publisher/credentials.json` |
| `play-reporting/` | クラッシュ・ANR レポートの取得 | Application Default Credentials |

## セットアップ

```sh
cd scripts
npm ci
```

型チェック:

```sh
npm run typecheck
```

各スクリプトの使い方と権限設定は、それぞれの README を参照する。
