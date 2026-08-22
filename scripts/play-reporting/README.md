# play-reporting

Google Play Developer Reporting API からクラッシュ・ANR レポートを取得し、
スタックトレースをローカルで確認するスクリプト。

API が返す `reportText` は書式が固定されていないため解析せず、そのまま表示・保存する。
結果は既定では標準出力だけに書き、`--output` を指定した場合だけ JSON ファイルを作る。

## 事前準備

1. Google Cloud プロジェクトで Google Play Developer Reporting API を有効にする
2. 実行する Google アカウントを Play Console のユーザーとして登録する
3. 対象アプリの「アプリ情報の表示（読み取り専用）」権限を付与する
4. ローカルの Application Default Credentials を設定する

認証スコープ:

```text
https://www.googleapis.com/auth/playdeveloperreporting
```

ローカル手動実行ではユーザー認証を推奨する。サービスアカウントを使う場合も、
鍵はリポジトリ外に置き、`GOOGLE_APPLICATION_CREDENTIALS` で明示する。
このスクリプトは `publisher/credentials.json` を読み込まない。

## 実行

最初に共通の依存関係をインストールする。

```sh
cd scripts
npm ci
```

直近24時間のクラッシュ:

```sh
npm run fetch-crashes
```

直近7日間のクラッシュとANRを versionCode 92 に限定:

```sh
npm run fetch-crashes -- --hours 168 --type CRASH,ANR --version-code 92
```

JSONにも保存:

```sh
npm run fetch-crashes -- --output play-reporting/output/reports.json
```

既存ファイルは上書きしない。`play-reporting/output/` は Git の除外対象だが、
レポートには内部のクラス名、メソッド名、例外メッセージなどが含まれ得るため、
社内限定情報として扱う。

全オプション:

```sh
npm run fetch-crashes -- --help
```

## Windowsでダブルクリック実行

事前にWindowsの環境変数 `GOOGLE_APPLICATION_CREDENTIALS` に、クラッシュ取得専用
サービスアカウントのJSON鍵のWindows形式の絶対パス（例: `C:\Credentials\key.json`）
を設定する。WSL形式の `/mnt/c/...` は使用しない。

その後、`fetch-crashes.bat` をダブルクリックする。取得期間と任意のversionCodeを入力すると、
次の形式で結果を保存する。

```text
play-reporting/output/reports-YYYYMMDD-HHMMSS.json
```

環境変数、JSON鍵、npm、`scripts/node_modules` のいずれかが見つからない場合は、
警告を表示してAPIを呼び出さずに終了する。
