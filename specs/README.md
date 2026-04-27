# 仕様書インデックス — DJ App

## 用語集
- [glossary.md](glossary.md) — ユビキタス言語（全工程共通）

## ユースケース一覧

| ID | タイトル | 状態 |
|---|---|---|
| [UC-001](usecases/UC-001-load-track.md) | トラックをデッキにロードする | 完了 |
| [UC-002](usecases/UC-002-playback-control.md) | デッキを再生・一時停止・停止する | 完了 |
| [UC-003](usecases/UC-003-crossfader.md) | クロスフェーダーで音量バランスを調整する | 完了 |
| [UC-004](usecases/UC-004-bpm-sync.md) | BPMを検出し、デッキ間でSyncする | 完了 |
| [UC-005](usecases/UC-005-equalizer.md) | イコライザーで音質を調整する | 完了 |
| [UC-006](usecases/UC-006-loop.md) | ループ再生（サンプリング）を設定する | 完了 |
| [UC-007](usecases/UC-007-scratch.md) | スクラッチ操作をする | 完了 |
| [UC-008](usecases/UC-008-library.md) | 端末内の音楽ライブラリを閲覧・検索する | 完了 |

## 受入条件一覧

| ID | タイトル | 対応UC |
|---|---|---|
| [AC-001](acceptance/AC-001-load-track.md) | トラックをデッキにロードする | UC-001 |
| [AC-002](acceptance/AC-002-playback.md) | デッキを再生・一時停止・停止する | UC-002 |
| [AC-003](acceptance/AC-003-crossfader.md) | クロスフェーダーで音量バランスを調整する | UC-003 |
| [AC-004](acceptance/AC-004-bpm-sync.md) | BPM検出・Sync | UC-004 |
| [AC-005〜007](acceptance/AC-005-007-eq-loop-scratch.md) | EQ・ループ・スクラッチ | UC-005〜007 |
