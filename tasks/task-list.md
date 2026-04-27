# タスク一覧 — PocketDJ

> ガイドライン: 1タスク = レビューが30分以内に完了できる粒度
> 凡例: ⬜ 未着手 / 🔄 進行中 / ✅ 完了
> **ルール: 実機検証タスク（T-x09 / T-x10 等）が ✅ になるまで Sprint 完了としない**

---

## Sprint 0: プロジェクト基盤

| ID | タイトル | 仕様ID | 依存 | 状態 |
|---|---|---|---|---|
| T-001 | Androidプロジェクト新規作成（Kotlin + Compose + CMake） | — | — | ✅ |
| T-002 | Clean Architectureのパッケージ構成を作成 | ADR-003 | T-001 | ✅ |
| T-003 | Hilt DIセットアップ | ADR-003 | T-002 | ✅ |
| T-004 | Oboeライブラリをプロジェクトに組み込む（CMakeLists.txt設定） | ADR-001 | T-001 | ✅ |
| T-005 | JNIブリッジの骨格を実装（Kotlin ↔ C++の接続確認） | ADR-001 | T-004 | ✅ |
| T-006 | TarsosDSPをプロジェクトに組み込む | ADR-004 | T-001 | ✅ |
| T-007 | DataStore（設定値永続化）のセットアップ | ADR-003 | T-003 | ✅ |
| T-008 | CIパイプライン構築（GitHub Actions: ビルド + テスト） | — | T-001 | ✅ |

---

## Sprint 1: 音楽ライブラリ（UC-008）

| ID | タイトル | 仕様ID | 依存 | 状態 |
|---|---|---|---|---|
| T-101 | TrackドメインモデルとTrackRepositoryインターフェースを定義 | UC-008 | T-002 | ✅ |
| T-102 | LibraryUseCaseを実装（一覧取得・検索・ソート） | UC-008 | T-101 | ✅ |
| T-103 | TrackRepositoryImpl（MediaStore連携）を実装 | UC-008 | T-101 | ✅ |
| T-104 | READ_MEDIA_AUDIO権限リクエスト処理を実装 | UC-008 | T-103 | ✅ |
| T-105 | LibraryViewModelを実装（MVI State/Intent） | UC-008 | T-102 | ✅ |
| T-106 | LibraryBrowser UIを実装（一覧・検索バー・ソート） | UC-008 | T-105 | ✅ |
| T-107 | LibraryUseCase単体テスト（AC-001-02, AC-008系） | UC-008 | T-102 | ✅ |

---

## Sprint 2: デッキ基本再生（UC-001 / UC-002）

| ID | タイトル | 仕様ID | 依存 | 状態 |
|---|---|---|---|---|
| T-201 | DeckStateドメインモデルを定義 | UC-001/002 | T-002 | ✅ |
| T-202 | AudioEngineRepositoryインターフェースを定義 | ADR-003 | T-201 | ✅ |
| T-203 | C++ DeckPlayerを実装（ロード・再生・停止・位置管理） | UC-001/002 | T-005 | ✅ |
| T-204 | JNIブリッジにデッキ操作関数を追加 | UC-001/002 | T-203 | ✅ |
| T-205 | LoadTrackUseCaseを実装 | UC-001 | T-202, T-204 | ✅ |
| T-206 | PlaybackControlUseCaseを実装 | UC-002 | T-202, T-204 | ✅ |
| T-207 | AudioEngineRepositoryImplを実装（JNI呼び出し） | ADR-003 | T-204 | ✅ |
| T-208 | DeckViewModelを実装（MVI）| UC-001/002 | T-205, T-206 | ✅ |
| T-209 | デッキUI基本レイアウトを実装（再生・停止ボタン、曲名表示） | UC-002 | T-208 | ✅ |
| T-210 | 波形表示UIを実装（Canvasで描画・再生位置インジケーター） | UC-001 | T-209 | ✅ |
| T-211 | LoadTrackUseCase / PlaybackControlUseCase単体テスト | UC-001/002 | T-205, T-206 | ✅ |

---

## Sprint 3: ミックス基盤（UC-003）

| ID | タイトル | 仕様ID | 依存 | 状態 |
|---|---|---|---|---|
| T-301 | C++ Crossfaderを実装（Linear / Equal Powerカーブ） | UC-003 | T-203 | ✅ |
| T-302 | JNIブリッジにクロスフェーダー操作関数を追加 | UC-003 | T-301 | ✅ |
| T-303 | CrossfaderUseCaseを実装 | UC-003 | T-202, T-302 | ✅ |
| T-304 | SettingsRepositoryにクロスフェーダーカーブ設定を追加 | UC-003 | T-007 | ✅ |
| T-305 | MixerViewModelを実装（クロスフェーダー位置・カーブ管理） | UC-003 | T-303, T-304 | ✅ |
| T-306 | クロスフェーダーUIを実装（スライダー・ダブルタップリセット） | UC-003 | T-305 | ✅ |
| T-307 | 設定画面UIを実装（カーブ切替: Linear / Equal Power） | UC-003 | T-305 | ✅ |
| T-308 | CrossfaderUseCase単体テスト（AC-003系） | UC-003 | T-303 | ✅ |
| T-309 | **実機検証**（Pixel 8 Pro / AC-003-01〜09 手動確認） | UC-003 | T-308 | ✅ |

---

## Sprint 4: BPM検出・Sync（UC-004）

| ID | タイトル | 仕様ID | 依存 | 状態 |
|---|---|---|---|---|
| T-401 | BpmDetectorインターフェースを定義（AudioEngineRepository に getBpm() 追加） | UC-004 | T-002 | ✅ |
| T-402 | C++ 自己相関BPM検出を DeckPlayer に実装（detectBpmLocked） | UC-004 | T-401, T-006 | ✅ |
| T-403 | BpmSyncUseCaseを実装（検出・Sync ON/OFF・ピッチ調整±16%） | UC-004 | T-402, T-202 | ✅ |
| T-404 | C++ DeckPlayerにピッチ変更機能を追加（線形補間・fractional playhead） | UC-004 | T-203 | ✅ |
| T-405 | JNIブリッジにBPM/ピッチ操作関数を追加（nativeGetBpm） | UC-004 | T-404 | ✅ |
| T-406 | DeckViewModelにBPM表示・SYNCボタン状態管理を追加 | UC-004 | T-403, T-405 | ✅ |
| T-407 | BPM表示・SYNCボタン・ピッチスライダーUIを実装 | UC-004 | T-406 | ✅ |
| T-408 | BpmSyncUseCase単体テスト（AC-004系）10テスト PASS | UC-004 | T-403 | ✅ |
| T-409 | **実機検証**（Pixel 8 Pro / AC-004-01〜xx 手動確認） | UC-004 | T-408 | ✅ |

---

## Sprint 5: イコライザー（UC-005）

> 仕様変更: EQスライダー → RotaryKnob（Canvas実装）に変更。ダブルタップで 0dB リセット。

| ID | タイトル | 仕様ID | 依存 | 状態 |
|---|---|---|---|---|
| T-501 | C++ EqProcessor（3バンド LR4 クロスオーバーフィルター）を実装 | UC-005 | T-203 | ✅ |
| T-502 | JNIブリッジにEQ操作関数を追加 | UC-005 | T-501 | ✅ |
| T-503 | EqualizerUseCaseを実装 | UC-005 | T-202, T-502 | ✅ |
| T-504 | DeckViewModelにEQ状態管理を追加 | UC-005 | T-503 | ✅ |
| T-505 | EQロータリーノブUI（High/Mid/Low・ダブルタップリセット）を実装 | UC-005 | T-504 | ✅ |
| T-506 | EqualizerUseCase単体テスト（AC-005系） | UC-005 | T-503 | ✅ |
| T-507 | **実機検証**（Pixel 8 Pro / AC-005-01〜03 手動確認） | UC-005 | T-506 | ✅ |

---

## Sprint 6: ループ・サンプリング（UC-006）

> 仕様変更: ビートボタン方式 → 手動 IN/OUT マーキング方式に変更。
> 追加実装: サンプルパッド（4スロット）、EQ+Pitch をサンプルに焼込み。

| ID | タイトル | 仕様ID | 依存 | 状態 |
|---|---|---|---|---|
| T-601 | C++ ループ処理を DeckPlayer に実装（loopIn/Out・ビート同期） | UC-006 | T-203 | ✅ |
| T-602 | JNIブリッジにループ操作関数を追加 | UC-006 | T-601 | ✅ |
| T-603 | LoopUseCaseを実装（手動IN/OUT・倍半・ナッジ） | UC-006 | T-202, T-602 | ✅ |
| T-604 | DeckViewModelにループ状態管理を追加 | UC-006 | T-603 | ✅ |
| T-605 | ループUIを実装（IN→/→OUT/÷½・尺・×2・ナッジ・波形ハイライト） | UC-006 | T-604 | ✅ |
| T-606 | LoopUseCase単体テスト（AC-006系） | UC-006 | T-603 | ✅ |
| T-607 | **実機検証**（Pixel 8 Pro / AC-006-01〜02 手動確認） | UC-006 | T-606 | ✅ |
| T-608 | C++ SamplePlayer実装（4スロット PCM キャプチャ・再生） | UC-006 | T-601 | ✅ |
| T-609 | サンプルパッドUI実装（キャプチャ・トグル再生・クリア） | UC-006 | T-608 | ✅ |
| T-610 | サンプルキャプチャ時に EQ + Pitch を焼込み | UC-006 | T-608 | ✅ |

---

## Sprint 7: スクラッチ（UC-007）

> 追加実装: CHOP_PAD / VELOCITY_WHEEL / FULL_STRIP の3方式を選択可能。DataStore永続化。

| ID | タイトル | 仕様ID | 依存 | 状態 |
|---|---|---|---|---|
| T-701 | C++ スクラッチ処理を DeckPlayer に実装（速度・逆再生） | UC-007 | T-203 | ✅ |
| T-702 | JNIブリッジにスクラッチ操作関数を追加 | UC-007 | T-701 | ✅ |
| T-703 | ScratchUseCaseを実装 | UC-007 | T-202, T-702 | ✅ |
| T-704 | DeckViewModelにスクラッチ状態管理を追加 | UC-007 | T-703 | ✅ |
| T-705 | スクラッチUIを実装（CHOP_PAD / VELOCITY_WHEEL / FULL_STRIP） | UC-007 | T-704 | ✅ |
| T-706 | ScratchUseCase単体テスト（AC-007系） | UC-007 | T-703 | ✅ |
| T-707 | **実機検証**（Pixel 8 Pro / AC-007-01〜03 手動確認） | UC-007 | T-706 | ✅ |

---

## Sprint 8: 統合・品質

> T-801 は完了済み。スクラッチモード選択を DeckPanel 内に移動、波形ストリップ横並び化等の UI 統合を実施。

| ID | タイトル | 仕様ID | 依存 | 状態 |
|---|---|---|---|---|
| T-801 | メイン画面レイアウトの統合（全コンポーネント結合・UI調整） | — | Sprint 1〜7 | ✅ |
| T-802 | レイテンシ実測（Pixel 8 Pro / Oboe計測API / 目標 20ms 以下） | Constitution §4 | T-801 | ✅ |
| T-803 | 1000曲ライブラリのパフォーマンス確認 | non-functional.md | T-801 | ✅ |
| T-804 | 30分連続再生の安定性テスト | non-functional.md | T-801 | ✅ |
| T-805 | Google Playストア向けリリースビルド設定（ProGuard・署名・AAB） | — | T-801 | ✅ |

---

## Sprint 9: UI デザイン改善

> Stitch 生成モックアップを部分採用し、レイアウト構造を維持したままビジュアル品質を向上させる。
> 仕様詳細: `DESIGN.md` 参照。参照ビジュアル: `design-reference/stitch-export/*/screen.png`

| ID | タイトル | 仕様 | 依存 | 状態 | 往復数 |
|---|---|---|---|---|---|
| T-901 | Google Fonts 依存追加（`libs.versions.toml` + `build.gradle.kts`） | DESIGN.md §タイポグラフィ | — | ⬜ | — |
| T-902 | `Theme.kt`：Stitch カラーパレット全色を適用 | DESIGN.md §カラートークン | — | ⬜ | — |
| T-903 | `Typography.kt`：Space Grotesk 導入・スタイル定義 | DESIGN.md §タイポグラフィ | T-901, T-902 | ⬜ | — |
| T-904 | `DeckScreen.kt`：カード枠線・EQ ノブ・Play ボタングローのビジュアル更新 | DESIGN.md §S8-03〜05 | T-902, T-903 | ⬜ | — |
| T-905 | `DeckScreen.kt`：スクラッチ・波形・ループ・ピッチチップのビジュアル更新 | DESIGN.md §S8-06〜08 | T-904 | ⬜ | — |
| T-906 | `MainScreen.kt`：WaveformStrip アクセントカラーを `primaryContainer`/`secondaryContainer` に変更 | DESIGN.md §S8-07 | T-902 | ⬜ | — |
| T-907 | `SamplePadRow.kt`：高さ 36dp→44dp、再生中グローエフェクト追加 | DESIGN.md §S8-09 | T-902 | ⬜ | — |
| T-908 | `LibraryBrowserScreen.kt`：「Select Track」タイトル・アルバムアート円形プレースホルダー追加 | DESIGN.md §S8-10 | T-902 | ⬜ | — |
| T-909 | `./gradlew test` 全件 PASS 確認 + `assembleDebug` ビルド通過確認 | — | T-901〜T-908 | ⬜ | — |
| T-910 | **実機検証**（Pixel 8 Pro / UI 受入条件を目視確認） | DESIGN.md §変更スコープ | T-909 | ⬜ | — |

---

## タスク数サマリー

| Sprint | タスク数 | 完了 | 主な仕様 |
|---|---|---|---|
| Sprint 0（基盤） | 8 | 8 | プロジェクト設定 |
| Sprint 1（ライブラリ） | 7 | 7 | UC-008 |
| Sprint 2（再生） | 11 | 11 | UC-001/002 |
| Sprint 3（ミックス） | 9 | 9 | UC-003 |
| Sprint 4（BPM/Sync） | 9 | 9 | UC-004 |
| Sprint 5（EQ） | 7 | 7 | UC-005 |
| Sprint 6（ループ・サンプリング） | 10 | 10 | UC-006 |
| Sprint 7（スクラッチ） | 7 | 7 | UC-007 |
| Sprint 8（統合・品質） | 5 | 5 | — |
| Sprint 9（UI デザイン改善） | 10 | 0 | DESIGN.md |
| **合計** | **83** | **73** | |
