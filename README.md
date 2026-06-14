# SubCan

[![Quality](https://github.com/kohannnnn/SubCan/actions/workflows/quality.yml/badge.svg)](https://github.com/kohannnnn/SubCan/actions/workflows/quality.yml)

サブスクリプションの支払日と支出を、シンプルに把握するためのAndroidアプリです。
「いつ、何に、いくら支払うか」をカレンダーと分析画面で見える化します。

## 主な機能

- 契約中・解約予定・停止済みを含むサブスクリプション管理
- 月表示・週表示で確認できる更新カレンダー
- 月額・年額合計とカテゴリ別の支出分析
- 更新日前のリマインダー通知
- 解約日、最終利用日、削減できた月額・年額を残せる解約済み履歴
- よく使われるサービスから選べるプリセット登録

データは端末内のRoomデータベースに保存されます。

## 技術構成

- Kotlin
- Jetpack Compose / Material 3
- Room
- WorkManager
- Navigation Compose
- Gradle Version Catalog

## 動作環境

- Android 7.0（API 24）以上
- JDK 17

## ビルド

```bash
git clone https://github.com/kohannnnn/SubCan.git
cd SubCan
./gradlew assembleDebug
```

Android Studioで開く場合は、プロジェクトを開いてGradle Syncを実行してください。

## 品質チェック

```bash
./gradlew spotlessCheck lintDebug testDebugUnitTest
```

GitHub Actionsでもフォーマット、Android Lint、ユニットテスト、Gitleaksによる秘密情報の検査を実行しています。

## 開発状況

現在開発中です。仕様や画面構成は変更される場合があります。
