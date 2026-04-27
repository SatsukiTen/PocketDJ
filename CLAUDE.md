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
| **`DESIGN.md`** | **デザインシステム（カラー・タイポグラフィ・コンポーネント仕様）** |
| `design/adr/` | ADR-001〜004: 音声API・UI・アーキテクチャ・BPM選定根拠 |
| `design/architecture.md` | 4層構成・データフロー・JNI設計図 |
| `specs/usecases/` | UC-001〜008: ユースケース仕様 |
| `specs/acceptance/` | AC-001〜007: Given-When-Then 受入条件 |
| `tasks/task-list.md` | Sprint/タスク進捗（唯一の信頼できる進捗記録） |
| `docs/knowledge-base.md` | ビルドトラブル・設計パターン・未解決事項 |

---

## 開発ルール

### 実装前の確認

- **UI改善・リファクタリング・デザイン変更を含むあらゆる開発作業は、Constitution → Specify → Plan → Tasks → Implement の順を踏む**
  - 「小さな変更だから」という理由でプロセスを省略しない
  - 「始めてください」「進めてください」という指示は **「Specify フェーズから始めてください」** と解釈する。実装着手の許可ではない
  - ユーザーから作業依頼を受けたら、まず `tasks/task-list.md` を Read し、次に Specify（仕様・受入条件）と Tasks（タスク一覧）を作成して確認を取る
  - **タスク番号を割り当てる前に必ず `tasks/task-list.md` を読んで既存番号を確認する**
  - 確認が取れた後に初めて実装に入る
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
- **カラー・タイポグラフィ・サイズは必ず `DESIGN.md` を参照**して適用する。デザインシステム外の値を使わない
- `DESIGN.md` の「MainScreen レイアウト」「DeckPanel 内部構造」に記載されたゾーン配置・重み配分は変更しない
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
6. 実機（Pixel 8 Pro）で受入条件を手動検証し OK を確認する
7. **作業ブランチを master にマージしてコミット**（後述「Git 管理」参照）
8. 「Sprint X 完了です」と報告

### 仕様変更の管理

- 仕様変更は既存文書を黙って上書きせず、変更内容を会話で確認してから反映する
- 変更が発生したら `task-list.md` のコメント記入と UC 文書更新を同時に行う

---

## Git 管理

### ブランチ戦略

| ブランチ名 | 用途 | ルール |
|---|---|---|
| `master` | リリース基準点 | 常にビルド可能・テスト全PASS・実機動作確認済みを保つ |
| `feature/sprint-N-xxx` | Sprint作業 | Sprint開始時に master から切る。完了・検証後に master へマージ |
| `ui/xxx` | UI改善・デザイン変更 | フルデザイン変更など独立タスク用。master へのマージは実機検証後のみ |
| `experiment/xxx` | 試験的変更 | マージしない可能性あり。検証後に明示的に判断する |

```bash
# Sprint 開始時
git checkout -b feature/sprint-8-ui-redesign

# Sprint 完了・実機検証後
git checkout master
git merge --no-ff feature/sprint-8-ui-redesign
git branch -d feature/sprint-8-ui-redesign
```

### コミットのタイミング

- **Sprint 完了時**: 実機検証 OK の直後に必ずコミット（master へのマージコミット）
- **UI 改善作業中**: 各作業単位（コンポーネント1つ分など）でこまめにコミット。「戻せる粒度」を意識する
- **ドキュメント変更**: コード変更と分けてコミットしてよい
- 作業途中の壊れた状態でコミットしない（`gradlew test` 通過後にコミット）

### コミットメッセージ規則

```
<種別>: <概要（日本語・50文字以内）>

<背景・理由（任意）>

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>
```

種別: `feat`（機能追加）/ `fix`（バグ修正）/ `ui`（UI変更）/ `refactor` / `docs` / `chore`

### .gitignore の原則（全プロジェクト共通）

必ず除外するもの:

| 種別 | パターン例 |
|---|---|
| センシティブ情報 | `*.jks` `*.keystore` `keystore.properties` `local.properties` `*.env` |
| ビルド成果物 | `build/` `app/build/` `.gradle/` `*.apk` `*.aab` |
| IDE 設定 | `.idea/` `*.iml` |
| 外部ライブラリ（独自 .git あり） | 個別に除外（例: `app/src/main/cpp/oboe/`） |
| OS 生成ファイル | `.DS_Store` `Thumbs.db` `desktop.ini` |

センシティブファイルをコミットしてしまった場合は `git filter-branch` または `git filter-repo` で履歴から除去が必要。**コミット前に必ず `git status` で除外確認する。**

### ロールバック手順

```bash
# UI 変更を特定ファイルだけ master 時点に戻す
git checkout master -- app/src/main/kotlin/com/djapp/presentation/

# 特定コミット時点の1ファイルを復元
git checkout <commit-hash> -- <ファイルパス>

# 作業ブランチごと破棄して master に戻る
git checkout master
git branch -D ui/failed-experiment

# 直前のコミットを取り消す（変更内容は残す）
git reset --soft HEAD~1

# コミット履歴を確認
git log --oneline --graph
```

### このプロジェクト固有の注意

- `app/src/main/cpp/oboe/` は独自 `.git` を持つため追跡対象外。ビルドには手動配置が必要
- `dj-app-release.jks` は `.gitignore` で除外済み。絶対にコミットしない

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
