# CLAUDE.md — PocketDJ 開発ガイド

このファイルは Claude Code がセッション開始時に自動読み込みする開発ルール集です。
memory/ の feedback_*.md から安定したルールをここに集約しています。

---

## プロジェクト概要

- **場所:** `D:/AI_Projects/dj-app/`
- **開発端末:** Pixel 8 Pro（Android 16 / API 36）
- **パッケージ:** `com.djapp`
- **開発プロセス:** 仕様駆動開発（Constitution → Specify → Plan → Tasks → Implement）

## 主要ドキュメントへのポインタ

| ドキュメント | 役割 |
|---|---|
| `Constitution.md` | スコープ・品質制約・開発原則（変更しない） |
| `design/adr/` | ADR-001〜004: 音声API・UI・アーキテクチャ・BPM選定根拠 |
| `design/architecture.md` | 4層構成・データフロー・JNI設計図 |
| `specs/usecases/` | UC-001〜008: ユースケース仕様 |
| `specs/acceptance/` | AC-001〜007: Given-When-Then 受入条件 |
| `tasks/task-list.md` | Sprint/タスク進捗（唯一の信頼できる進捗記録） |
| `docs/knowledge-base.md` | ビルドトラブル・設計パターン・未解決事項 |

---

## 開発ルール

### 実装前の確認

- **実装・ドキュメント変更・ルール更新を問わず、必ず「検討結果の報告 → ユーザー確認 → 実作業」の順を守る**
  - 分析・提案の依頼を受けた場合も同様。検討結果のみを先に報告し、承認を得てからファイル操作に入る
  - ユーザーに確認するのは「方針・アプローチの選択」のみ
  - バージョン番号・ファイル構成・設定値などの細部はこちらで調査して決める
- C++ で新規 API を使う前に対象ライブラリのヘッダーをGrep/Readで確認し、メソッド名・引数・戻り値を確認してから実装する

### コード変更後の確認

- Kotlin/Java 変更後: `./gradlew test` を実行しテスト全件 PASS を確認してから提示する
- C++ 変更後: `./gradlew buildCMakeDebug` でビルドが通ることを確認する
- `assembleDebug` と Firebase 配布はユーザーから依頼された時のみ実行する

### UI 編集

- UI変更前に、変更対象 Composable を含む**親コンポーネントのファイル全体**を読む
- `weight(1f)` / `fillMaxSize()` / `Modifier.height()` を変更する際は、変更前後のレイアウト構造をテキストで明示してから確認を求める
- UI変更後は `assembleDebug` でビルドが通ることを確認してから提示する
- 各スプリントで実機動作可能な状態を維持する。フルデザイン変更は独立タスクで行う

### 往復数の計測

開発速度の改善に使うため、タスクごとの往復数を記録する。

- **定義:** ユーザーメッセージ → Claude応答 = 1往復
- **計測範囲:** タスク着手から ✅ になるまでの全往復数。Sprint レビューで指摘を受けて追加対応した往復もそのタスクに加算する
- **記録場所:** `tasks/task-list.md` の `往復数` 列に記入する
- **振り返り:** Sprint 振り返りに往復数サマリーを含める。5往復以上のタスクは原因を分析する

### Sprint 完了時の必須手順

1. `tasks/task-list.md` の当該スプリント全タスクを ✅ に更新し、`往復数` 列を記入する
2. `memory/project_dj_app.md` の進捗表を更新
3. 仕様変更があった場合: 対応する UC/AC ファイルを更新
4. 解決済みの `knowledge-base.md` 項目を更新
5. **スプリント振り返りを実施し `tasks/retrospective-YYYY-MM-DD-SprintX.md` に保存**
   - ① 良かった点、② 問題になった点と再発防止策、③ ルール変更提案
   - ④ 往復数サマリー（スプリント平均・最多タスクと原因）
6. 実機（Pixel 8 Pro）で受入条件を手動検証し OK を確認してから「Sprint X 完了です」と報告

### 仕様変更の管理

- 仕様変更は既存文書を黙って上書きせず、変更内容を会話で確認してから反映する
- 変更が発生したら `task-list.md` のコメント記入と UC 文書更新を同時に行う

---

## ビルド環境の既知問題

- **Java 24 + Prefab 非互換**: Oboe は `add_subdirectory` 方式で組み込む（Prefab AAR は使わない）
- **TarsosDSP**: Maven Central 未配布 → `app/libs/` にローカル AAR を配置
- **API 36**: `gradle.properties` に `android.suppressUnsupportedCompileSdk=36` が必要
- **Gradle/AGP**: 8.10.2 / 8.8.0 がキャッシュ済みで動作確認済み
- **Oboe API**: `getLatencyMillis()` は存在しない。正しくは `calculateLatencyMillis()`

## Windows 環境での CLI ツール実行

- JDK ツール（keytool 等）は文字化けを防ぐため非対話モード（全パラメーターを CLI に指定）を優先する
- bash では `/c/Program Files/...` 形式でパスを指定する（`& "C:\..."` は PowerShell 構文）
- 対話入力が必要なコマンドは PowerShell ツールを使用する

---

## 応答言語

すべての応答は日本語で行う。
