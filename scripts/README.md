# scripts

ローカル検証とCI、Google Playに関するスクリプト。

`package.json` と `node_modules` はこのディレクトリで共有し、処理ごとのソースと設定は
サブディレクトリに分離する。

| ディレクトリ | 用途 | 認証 |
| --- | --- | --- |
| `android/` | Androidエミュレータの権限回帰テスト | 不要 |
| `publisher/` | AAB のアップロード | `publisher/credentials.json` |
| `play-reporting/` | クラッシュ・ANR レポートの取得 | Application Default Credentials |

## Androidエミュレータテスト

API 31またはAPI 36のエミュレータを起動し、serialを指定する。

```sh
android/run-bluetooth-permission-tests.sh emulator-5554
```

ADBが`PATH`にない場合は`ADB_COMMAND`、Gradle wrapperを別経路で実行する場合は
`GRADLE_COMMAND`を指定する。APKを事前にインストール済みなら`DNS_SKIP_BUILD=1`を指定できる。

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
