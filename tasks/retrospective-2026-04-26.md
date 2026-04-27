# 開発振り返り — DJ App（2026-04-26）

---

## ① 開発で参照した全資材の整理

### 資材マップ（カテゴリ別）

```
dj-app/
├── Constitution.md                        プロジェクト憲章
├── design/
│   ├── adr/
│   │   ├── ADR-001-audio-api.md           音声API選定
│   │   ├── ADR-002-ui-framework.md        UIフレームワーク選定
│   │   ├── ADR-003-architecture.md        アーキテクチャ選定
│   │   └── ADR-004-bpm-detection.md       BPM検出ライブラリ選定
│   ├── architecture.md                    アーキテクチャ設計書
│   ├── non-functional.md                  非機能要件
│   └── tech-stack.md                      技術スタック
├── specs/
│   ├── README.md                          仕様書インデックス
│   ├── glossary.md                        用語集
│   ├── usecases/ (UC-001〜008)            ユースケース仕様
│   └── acceptance/ (AC-001〜007)          受入条件
├── tasks/
│   └── task-list.md                       タスク一覧・進捗管理
└── docs/
    └── knowledge-base.md                  開発中の知見・トラブルシュート記録

memory/ (AI側 / C:\Users\surre\.claude\projects\D--AI-Projects\memory\)
├── project_dj_app.md                      プロジェクト状況・技術決定記録
├── feedback_spec_driven_dev.md            仕様駆動開発プロセス知見
├── feedback_sprint_verification.md        実機検証プロセス
├── feedback_android_build.md             ビルド環境既知問題
├── feedback_change_process.md            修正前比較検討ルール
├── feedback_run_tests.md                 テスト実行ルール
├── feedback_audio_bugs.md               音声・ジェスチャーバグパターン
├── feedback_ui_testability.md           UI testabilityルール
└── MEMORY.md                             インデックス
```

---

### A. プロジェクト方針文書

#### Constitution.md
- **役割**: スコープ・品質制約・開発原則の基準。プロジェクト期間中に変更されない「憲法」
- **主要内容**:
  - IN/OUTスコープの定義
  - 音声レイテンシ20ms以下・クラッシュ率1%以下の品質制約
  - 「仕様駆動」「ヒューマンインザループ」「小さなコミット」の開発原則
- **活用実績**: 全Sprint通じてスコープ判断・技術選定の根拠として参照
- **現状**: 最新・正確（v1.1）

#### non-functional.md
- **役割**: レイテンシ・クラッシュ率・APKサイズなど数値目標の定義
- **主要内容**: 音声レイテンシ20ms以下、ライブラリ1000曲を1秒以内表示、連続30分再生でゼロ途切れ等
- **活用実績**: T-802（レイテンシ計測）の目標値根拠として参照。T-803/T-804の検証基準
- **現状**: 最新。T-803/T-804が未実施のため一部未検証のまま

#### tech-stack.md
- **役割**: 言語・ライブラリ・ビルド設定の技術選定記録
- **活用実績**: Sprint 0の基盤構築時に参照
- **現状**: 以下2点で実態と乖離あり（深刻度: 中）
  - テストフレームワークが「JUnit 5」と記載されているが実際はJUnit 4を使用
  - Roomを依存に追加したがBPMキャッシュとしては実質未使用

---

### B. 設計意思決定記録（ADR）

#### ADR-001: 音声API選定（Oboe採用）
- **役割**: 音声APIをOboe/AAudioに決定した根拠の記録
- **決定内容**: Option A（Oboe）採用。レイテンシ20ms以下をOboe/AAudioで達成
- **活用実績**: Sprint 2でOboe add_subdirectory方式採用時の判断根拠
- **現状**: 確定・正確

#### ADR-002: UIフレームワーク選定（Jetpack Compose採用）
- **役割**: UIフレームワーク選定の根拠
- **決定内容**: Option A（Jetpack Compose）採用。Canvas + PointerInputでカスタムUI実装
- **活用実績**: Sprint 1〜のUI実装全般の判断根拠
- **現状**: 確定・正確

#### ADR-003: アーキテクチャ選定（Clean Architecture + MVI採用）
- **役割**: レイヤー構成・状態管理方式の選定根拠
- **決定内容**: Clean Architecture（4層）+ MVI（State/Intent/SideEffect）採用
- **活用実績**: 全Sprintの設計判断の基準。UseCase = UC-IDの1対1対応を実現
- **現状**: 確定・正確

#### ADR-004: BPM検出ライブラリ選定（段階的アプローチ）
- **役割**: BPM検出ライブラリの選定根拠
- **決定内容**: Phase 1はTarsosDSP（Apache 2.0）、精度未達なら Phase 2でaubioへ切替
- **活用実績**: Sprint 4でTarsosDSP採用時の判断根拠
- **現状**: 確定。Phase 2（aubio切替）は未実施。精度の定量検証も未実施

---

### C. 設計書

#### architecture.md
- **役割**: 4層アーキテクチャの構成図・データフロー・JNI境界・モジュール構成の設計図
- **主要内容**: Presentation/Domain/Data/Audio Engineの4層構成図、クロスフェーダー操作のデータフロー例、JNI関数の設計原則
- **活用実績**: 全Sprint通じてアーキテクチャ判断の参照元
- **現状**: ステータスが「提案中（ADRレビュー後に確定）」のまま更新されていない（深刻度: 低。内容は正確）

---

### D. 仕様書

#### specs/README.md（仕様書インデックス）
- **役割**: UC/ACファイルへのナビゲーション
- **現状**: UC-001〜008すべて「完了」マーク済み

#### glossary.md（用語集）
- **役割**: ユビキタス言語の定義（デッキ・クロスフェーダー・BPM等の用語統一）
- **活用実績**: 仕様書・コード命名の共通言語として参照
- **現状**: 安定。特段の乖離なし

#### UC-001〜008（ユースケース仕様）
- **役割**: 各機能の基本フロー・代替フロー・例外フロー・事前・事後条件の定義
- **活用実績**: 各Sprint実装時の設計根拠として参照
- **現状**: 以下の仕様変更が文書に未反映（深刻度: 中）
  - UC-006（ループ）: ビートボタン方式→手動IN/OUT方式に仕様変更。task-list.mdのコメントのみに記録され、UC本体が未更新の可能性が高い
  - UC-005（EQ）: スライダー→RotaryKnob（Canvas実装）に仕様変更。同様に未反映の可能性あり

#### AC-001〜007（受入条件）
- **役割**: Given-When-Then形式の受入条件。Sprint完了時の検証基準
- **活用実績**: 各Sprint完了時の手動検証チェックリストとして参照
- **現状**: AC-005〜007が1ファイルにまとまっており粒度が粗い。仕様変更の反映状況も確認が必要

---

### E. タスク管理

#### task-list.md
- **役割**: Sprint/タスクの進捗管理。唯一の信頼できる進捗記録
- **活用実績**: 毎Sprint参照・更新。スプリント完了の定義に直結
- **現状**: 71/73完了（T-803/T-804が残）。Sprint 5〜7の更新漏れが一時発生し、Sprint 8で発覚した

---

### F. 開発知見

#### knowledge-base.md
- **役割**: ビルドトラブル・設計パターン・未解決事項の記録。次タスク着手前に参照する資材
- **主要内容**: Java 24+Prefab非互換、TarsosDSP取得方法、API 36固有設定、UseCase設計パターン等
- **活用実績**: Sprint 0〜4でトラブルシュート時に参照
- **現状**: 「未解決・要調査」セクションが陳腐化（深刻度: 低）
  - T-802（レイテンシ実測）→ 実装済みだが文書は未更新
  - TarsosDSP BPM精度→ 定量検証未実施のまま
  - Spotify SDKライセンス→ 将来検討のまま（これは正しい）

---

### G. AI側メモリ（memory/）

#### project_dj_app.md
- **役割**: プロジェクト状況・技術決定の累積記録。セッションをまたいでコンテキストを維持
- **活用実績**: 毎セッション冒頭で参照し、続きから開発を再開できるよう機能
- **現状**: Sprint 8の技術決定まで反映済み

#### feedback_*.md（7ファイル）
- **役割**: 開発ルール・既知バグ・プロセスの知見。「二度同じ指摘をしない」ための記録
- **ファイル一覧と内容**:
  - `feedback_spec_driven_dev.md`: 仕様駆動開発各工程のパターン、Sprint完了チェックリスト
  - `feedback_sprint_verification.md`: 実機検証必須ルール、インストール方法
  - `feedback_android_build.md`: Java 24/Prefab非互換、TarsosDSP配布問題、API 36設定
  - `feedback_change_process.md`: 実装前に複数案比較→確認→実装の順を守るルール
  - `feedback_run_tests.md`: コード変更後のテスト実行ルール
  - `feedback_audio_bugs.md`: DeckPlayer memsetバグ、Compose Sliderダブルタップ競合
  - `feedback_ui_testability.md`: 各Sprintで動作可能なUI状態を維持するルール
- **現状**: 最新。今回のセッションで3件追加（Sprint完了チェックリスト、T-802実装知見等）

---

### 現時点で乖離・陳腐化している資材のまとめ

| 資材 | 問題 | 深刻度 | 対応方針 |
|---|---|---|---|
| architecture.md | ステータスが「提案中」のまま | 低 | 「確定」に更新 |
| tech-stack.md | JUnit 4と記載の乖離、Room未使用 | 中 | 実態に合わせて修正 |
| knowledge-base.md | 「未解決・要調査」の内容が古い | 低 | T-802を解決済みに更新 |
| UC-006 | 手動IN/OUT方式への仕様変更が未反映 | 中 | UC文書を実装に合わせて更新 |
| UC-005 | RotaryKnobへの仕様変更が未反映 | 中 | UC文書を実装に合わせて更新 |

---

## ② 本日（2026-04-26）の振り返り

### 実施内容

| タスク | 内容 | 結果 |
|---|---|---|
| T-802 | レイテンシ計測（C++→JNI→Kotlin→UI全レイヤー） | 完了 |
| T-805 | リリースビルド設定（ProGuard・署名・AAB） | 完了 |
| アイコン作成 | Vector DrawableでDJターンテーブルデザイン | 完了 |
| キーストア生成 | keytoolでdj-app-release.jks生成 | 完了 |
| AABビルド | bundleReleaseで9.4MB AAB生成 | 完了 |

### 良かった点

- T-802・T-805とも1セッションで全レイヤーを完走できた
- ProGuard・署名・AABの設定を一度に整備できた
- keystore.properties.templateというパターンで秘密情報をコードから分離できた
- Vector Drawableアイコンにより全解像度でスケーラブルなDJデザインを実現

### 問題になった点

**問題1: Oboe API名を事前確認せずに実装**
- `getLatencyMillis()` という存在しないメソッドを実装し、ビルドエラーで発覚
- `calculateLatencyMillis()` が正しい名前
- 原因: C++実装前にヘッダーを確認するルールが存在しなかった
- 既存ルール（C++変更後のbuildCMakeDebug確認）は「実装後」の検証であり「実装前」のAPI確認ルールが欠けていた

**問題2: keytoolの文字化け・対話入力問題**
- Windows + bash環境でkeytoolの日本語プロンプトが文字化けし3回失敗
- `-dname`を含む非対話形式コマンドを最初から案内すれば防げた
- PowerShellではなくbashセッションからの実行という環境への配慮が不足していた

---

## ③ 見直すべき開発方針・ルールの提案

### 提案A: C++実装前のAPI事前確認ルール

現状のルール（feedback_run_tests.md）は「C++変更後にbuildCMakeDebug」という事後検証。  
実装前のAPI確認ルールが欠けている。

**提案するルール:**
```
C++で新規APIを使用する前に:
1. 対象ライブラリのヘッダーファイルをGrepまたはReadで確認
2. メソッド名・引数・戻り値の型を確認してから実装
3. 実装後にbuildCMakeDebugで検証
```

### 提案B: Windows環境でのCLIツール実行ガイドライン

現状はルールなし。

**提案するルール:**
```
Windows + bash環境でのCLIツール実行:
- JDKツール（keytool等）は文字化けを防ぐため非対話モード（全パラメーターをCLIに指定）を優先
- & "path with spaces" はPowerShell構文。bashでは "/c/path/to/cmd" 形式を使う
- 対話入力が必要なコマンドはPowerShellツールを使用する
```

### 提案C: 資材の鮮度管理ルール

現状は資材の更新タイミングが定義されていない。仕様変更がUC文書に反映されない問題が発生。

**提案するルール:**
```
仕様変更が発生したとき:
1. task-list.mdの該当Sprintにコメントを追記（現状）
2. 対応するUCファイルも同時に更新する（追加）
3. knowledge-base.mdの「未解決・要調査」は解決したら即座に更新する
```

### 提案D: Sprint完了チェックリストの強化

現状のチェックリスト（feedback_spec_driven_devに記載）に資材更新の観点を追加。

**現状:**
```
1. task-list.mdの当該Sprint全タスクを✅に更新
2. memory/project_*.mdの進捗表を更新
3. 更新後に「Sprint X完了です」と報告
```

**提案（追加項目）:**
```
1. task-list.mdの当該Sprint全タスクを✅に更新
2. memory/project_*.mdの進捗表を更新
3. 仕様変更があった場合: 対応するUC/ACファイルを更新
4. 解決済みのknowledge-base.md項目を更新
5. 更新後に「Sprint X完了です」と報告
```

---

## ④ ユーザーフィードバックへの対応（2026-04-26）

### フィードバック1: CLAUDE.md の管理方針

**ユーザーの問題意識:** CLAUDE.md は未作成。MD ファイルは多数あるが分散している。現状形式の利益・不利益を分析してほしい。

**分析結果:**
- 現状の `memory/feedback_*.md` はリポジトリ外（`C:\Users\surre\.claude\...`）にあり、git 管理外・バックアップなし。memory がリセットされるとルールが全滅するリスクがある
- CLAUDE.md はセッション開始時に自動読み込みされ、プロジェクトリポジトリ内に存在するためバージョン管理・他者参照が可能
- memory の役割は「セッション継続性（Sprint進捗・技術決定）」に特化し、安定したルールは CLAUDE.md に移行するのが望ましい

**対応:** `CLAUDE.md` を `D:/AI_Projects/dj-app/` に新設。既存 feedback_*.md のルールを統合。今後は安定したルールを CLAUDE.md へ昇格し、memory は進捗・技術決定の記録に使う運用に移行。

---

### フィードバック2: UI編集でコンポーネントが隠れる問題

**ユーザーの問題意識:** 改善案を適用すると項目が隠れることが多々発生し時間がかかった。原因調査と再発防止を。JSON UI フレームワークについても考慮を。

**根本原因分析:**
1. AI に視覚フィードバックがない — コードだけで Compose レイアウトを編集するため、`weight(1f)` 変更の連鎖影響を確認できない
2. 部分読み込みによる親コンテキスト喪失 — 大きなファイルを一部だけ読んで編集すると親コンポーネントの制約を見落とす
3. Compose のサイレント overflow — レイアウト問題がコンパイル時に発見されず実機検証まで発覚しない
4. 変更影響範囲の未提示 — 変更前にどのコンポーネントへ影響するかを明示していなかった

**JSON UI フレームワーク考察:** 完全 SDUI は小規模個人開発では過剰。ただし「UI コンポーネントツリーをテキストで先に定義 → レビュー → Compose 実装」のプロセスは有効（AI がテキスト設計を生成し人間がレビューすることで、コード化前にレイアウト構造の問題を発見できる）。

**対応:** `memory/feedback_ui_editing.md` を新設。CLAUDE.md に UI 編集ルール（親コンポーネント全読み・weight変更の事前明示・assembleDebug確認）を追記。

---

### フィードバック3: スプリント完了時の振り返り常態化

**ユーザーの要望:** スプリント完了の都度レトロスペクティブを実施してほしい。

**対応:** `feedback_spec_driven_dev.md` の Sprint 完了チェックリストに振り返り実施（`tasks/retrospective-YYYY-MM-DD-SprintX.md` への保存）を追加。CLAUDE.md にも同手順を記載。

---

## ⑤ 新規プロジェクトへの適用可否チェック

| ルール | dj-app | 新規PJ | 備考 |
|---|---|---|---|
| CLAUDE.md を PJ リポジトリに置く | 今回から適用 | ✅ 全PJ | memory の代替ではなく補完として導入 |
| 実装前に複数案比較→確認→実装 | ✅ 適用済み | ✅ 全PJ | プロジェクト種別問わず有効 |
| C++ API はヘッダーを事前確認 | ✅ 適用済み | C++使用PJのみ | C++不使用なら不要 |
| テスト実行後に提示 | ✅ 適用済み | ✅ 全PJ | テストがない場合は lint / type check |
| UI変更前に親コンポーネント全読み | ✅ 適用済み | ✅ UI PJ全般 | React/Flutter/Compose 問わず有効 |
| Sprint完了時に振り返りを実施 | 今回から適用 | ✅ 全PJ | Sprint がある開発なら必須 |
| Sprint完了時に UC/AC を更新 | 今回から適用 | ✅ 仕様駆動PJ | 仕様書がある場合のみ |
| keytool は非対話モードで実行 | ✅ | Windows+bash PJ | Windows 環境固有 |
| Java 24 + Prefab 非互換 | ✅ | Android+Oboe PJ | 環境固有 |
| TarsosDSP はローカル AAR | ✅ | BPM検出PJのみ | ライブラリ固有 |

**新規プロジェクト向けの推奨事項:**
- Sprint 0 で `CLAUDE.md` を作成し、Constitution のスコープ・品質制約・開発ルールを記載する
- UI を含むプロジェクトでは Sprint の UI タスク開始前にコンポーネントツリーをテキストで設計する工程を設ける
- Sprint 完了チェックリスト（task-list 更新 / memory 更新 / 振り返り保存 / 実機検証）をテンプレートとして各 PJ の Constitution またはCLAUDE.md に記載する
