# DESIGN.md — PocketDJ デザインシステム

Claude Code がUI実装・変更時に参照するデザイン仕様書。
カラー・タイポグラフィ・コンポーネント仕様は Stitch 生成モックアップを部分採用している。
レイアウト構造（ゾーン配置・コンポーネント位置）は**変更しない**。

## 参照ビジュアル

| 画面 | ファイル |
|---|---|
| メイン画面 | `design-reference/stitch-export/stitch_pocketdj_pro_ui_main/screen.png` |
| ライブラリピッカー | `design-reference/stitch-export/stitch_pocketdj_pro_ui_LibraryPicker/screen.png` |

UI変更時は上記 screen.png を Read ツールで読んでビジュアルを確認してから実装する。

---

## デザイン原則

1. **シングル画面完結**: 全機能（デッキ・ループ・EQ・スクラッチ・サンプルパッド・クロスフェーダー）をスクロールなしで1画面に収める
2. **レイアウト安定性**: コントロールの位置は状態（再生中/停止中/ループ中など）によって変わらない
3. **視認性優先**: 暗い環境でも BPM・ピッチ・再生状態を瞬時に読み取れる。技術値は大きく・明るく
4. **デッキ色の一貫性**: Deck A = Blue 系、Deck B = Teal 系。この色割当てをあらゆる要素に適用する

---

## Stitch デザイン 採用方針

### 採用する（ビジュアル品質の向上）
- カラートークン全色
- タイポグラフィ（Space Grotesk + Inter）
- デッキカードの枠線・背景階層
- アクティブ状態のグローエフェクト
- サンプルパッドのビジュアル（大きさ・LED 感）
- 波形の配色（より深いダーク背景・鮮明なアクセント）
- ライブラリピッカーのデザイン（ジャンルチップ・アルバムアート・BPM表示）

### 採用しない（レイアウト構造の変更にあたるもの）
- ボトムナビゲーションバー（DECKS/LIBRARY/SAMPLES/MIXER）
- スクラッチホイールをデッキ中央の主役にするレイアウト
- EQ の縦スライダー化（ロータリーノブを維持）
- ピッチフェーダーの縦配置（横スライダーを維持）
- トップアプリバーのブランドロゴ（縦スペースを消費するため省略）

---

## カラートークン

Jetpack Compose の `MaterialTheme.colorScheme` に対応する値。
`Theme.kt` でこれらの値を使って MaterialTheme を定義する。

```
background:                #131313
surface:                   #131313
surface-dim:               #131313
surface-container-lowest:  #0e0e0e   ← 波形背景
surface-container-low:     #1c1b1b   ← デッキカード背景
surface-container:         #201f1f
surface-container-high:    #2a2a2a   ← チップ・ボタン非選択
surface-container-highest: #353534
surface-variant:           #353534
on-surface:                #e5e2e1
on-surface-variant:        #bdc8d0
outline:                   #889299
outline-variant:           #3e484f

primary:                   #9adbff   ← Deck A ブルー（テキスト・アイコン）
primary-container:         #4fc3f7   ← Deck A ブルー（塗りつぶし・arc）
on-primary:                #003548
on-primary-container:      #004e69

secondary:                 #71d7cd   ← Deck B ティール（テキスト・アイコン）
secondary-container:       #32a097   ← Deck B ティール（塗りつぶし・arc）
on-secondary:              #003733
on-secondary-container:    #00302c

tertiary:                  #f8c0ff   ← サンプルパッド パープル
tertiary-container:        #dfa2e8
on-tertiary:               #4b1b58
on-tertiary-container:     #663471

error:                     #ffb4ab   ← EQ Kill / xRun 警告
error-container:           #93000a
```

Compose での使い方イメージ:
```kotlin
// Deck A
val deckColor = MaterialTheme.colorScheme.primary          // テキスト・アイコン
val deckFill  = MaterialTheme.colorScheme.primaryContainer  // ボタン塗り・arc

// Deck B
val deckColor = MaterialTheme.colorScheme.secondary
val deckFill  = MaterialTheme.colorScheme.secondaryContainer
```

---

## タイポグラフィ

### フォントファミリー
| 用途 | フォント | 備考 |
|---|---|---|
| BPM・時間・ピッチ等の技術値 | **Space Grotesk** | geometric で数値の視認性が高い |
| ラベル・チップテキスト | **Space Grotesk** | uppercase + letterSpacing で明瞭に |
| ライブラリのトラック名・アーティスト | **Inter** | 長い文字列に適した可読性 |

Google Fonts から取得。`res/font/` または Compose `FontFamily` で定義する。

### テキストスタイル
```
technical-data: Space Grotesk 18sp / weight 500 / letterSpacing 0.05em
  → BPM表示、再生時間

label-md:       Space Grotesk 12sp / weight 600 / uppercase
  → セクションラベル（LOOP、PITCH、SCRATCH）

label-sm:       Space Grotesk 10sp / weight 500
  → チップ内テキスト、ノブの値表示

body-md:        Inter 14sp / weight 400
  → ライブラリのアーティスト名、説明文
```

---

## スペーシング・角丸

### スペーシング（既存の dp 値に対応）
```
xs:   4dp   ← コンポーネント間の最小余白
base: 8dp   ← 標準の内部余白
sm:   12dp
md:   16dp  ← 画面端マージン
```

### 角丸
```
カード（デッキパネル）:    12dp
サンプルパッド:           6dp
波形コンテナ:             5dp
チップ・小コントロール:    4dp
ループ Nudge チップ:      3dp
円形ボタン（Play等）:     CircleShape
```

---

## コンポーネント別ビジュアル仕様

### DeckPanel カード
```
background:    surface-container-low (#1c1b1b)
border:        1dp, accent-color at alpha 0.20
cornerRadius:  12dp
padding:       horizontal 8dp, vertical 6dp
```

### EQ ロータリーノブ（RotaryKnob）
既存の Canvas 実装を維持。色のみ変更:
```
track arc:          rgba(255,255,255,0.12)  ← 背景弧
active arc (A):     primaryContainer (#4fc3f7)
active arc (B):     secondaryContainer (#32a097)
kill arc:           error (#ffb4ab)
knob body:          surface-container-high (#2a2a2a)
indicator line:     Color.White (kill時は error)
```

### Play/Pause ボタン
```
size:              44dp circle
playing state:     background = deckFill (primaryContainer / secondaryContainer)
                   shadow = [0_0_20dp_deckFill/40]  ← グローエフェクト
stopped state:     background = surface-container-high (#2a2a2a)
                   border = 1dp deckColor at alpha 0.40
icon color:        Color.White（両状態共通）
```

### Stop ボタン
```
size:              36dp IconButton
icon tint:         deckColor（track未選択時は onSurface at alpha 0.30）
```

### スクラッチコントロール — CHOP PAD
```
background (通常):  deckColor at alpha 0.12
background (押下):  deckColor at alpha 0.45
label font size:   22sp（現行 20sp から拡大）
```

### スクラッチコントロール — VELOCITY WHEEL
```
outer background:  #0e0e0e（surface-container-lowest）
rim stroke:        deckColor at alpha 0.85、幅 = radius * 0.10
groove rings:      deckColor at alpha 0.12、3本（0.78/0.56/0.34 倍径）
indicator line:    deckColor at alpha 0.90
indicator dot:     deckColor、radius = outerRadius * 0.09
center hub:        #2a2a2a（surface-container-high）
```

### スクラッチコントロール — FULL STRIP
```
background:        #0e0e0e
groove lines:      deckColor at alpha 0.10（5本）
center line:       deckColor at alpha 0.35
edge markers:      deckColor at alpha 0.45（左右端）
needle (active):   deckColor at alpha 0.90、幅 3dp
needle circle:     deckColor、radius = height * 0.28
```

### ループセクション（ManualLoopSection）
```
IDLE  [IN →]:       background surface-container-high (#2a2a2a)
IN_SET [→ OUT]:     background deckFill（solid）、text White
ACTIVE duration:    background deckColor at alpha 0.20、border deckColor at alpha 0.40
÷½ / ×2 chips:     background surface-container-high
Nudge chips:        background surface-container-high at alpha 0.70
Sample capture Pn:  未ロード = deckColor at alpha 0.20 / ロード済み = tertiary at alpha 0.30
```

### Pitch スライダー
```
ラベル "PITCH":     on-surface-variant (#bdc8d0)、label-sm
値 "%+.1f%%":       ゼロ時 = on-surface-variant、非ゼロ時 = deckColor
Slider thumb:      deckColor（Material Slider の colors パラメーターで指定）
```

### WaveformStrip
```
background:        surface-container-lowest (#0e0e0e)
past bars:         deckColor at alpha 0.85
future bars:       deckColor at alpha 0.22
playhead line:     Color.White、幅 2dp
loop highlight:    deckColor at alpha 0.15（矩形塗り）
loop boundary:     deckColor at alpha 0.75、幅 2dp（縦線）
pending IN line:   deckColor at alpha 0.85（破線）
height:            38dp（変更なし）
```

### SamplePad（SamplePadButton）
```
height:            44dp（現行 36dp → 拡大）
cornerRadius:      6dp（変更なし）

empty:             background surface-container-high (#2a2a2a)
                   番号テキスト on-surface at alpha 0.30

loaded/stopped:    background tertiary-container (#dfa2e8) at alpha 0.25
                   テキスト tertiary (#f8c0ff)

playing:           background tertiary (#f8c0ff) at alpha 0.88
                   shadow [0_0_12dp_tertiary/40]  ← グロー
                   テキスト Color.White
```

### CrossfaderPanel
```
"A" ラベル:        primary (#9adbff)
"B" ラベル:        secondary (#71d7cd)
Slider:            Material Slider（thumbColor = 現在位置に応じて primary/secondary グラデーション）
LATENCY テキスト:  on-surface-variant（xRun > 0 時は error #ffb4ab）
```

### ライブラリピッカー（LibraryBrowserScreen）
現行のフルスクリーンオーバーレイ方式を維持。以下のビジュアル改善を適用:
```
ヘッダー:          "Select Track" タイトル + 戻るボタン
ジャンルチップ:     横スクロール可能なフィルターチップ行（All / Techno / House 等）
                   選択中 = deckColor 背景
トラック行:         高さ 64dp
                   左: アルバムアート円形プレースホルダー（40dp）
                   中: タイトル（on-surface） + アーティスト（on-surface-variant）
                   右: BPM（deckColor、Space Grotesk） + 時間（on-surface-variant）
選択中行:          左端に 3dp の deckColor vertical bar
```

---

## MainScreen レイアウト（変更しない）

```
┌─────────────────────────────────────────────────────┐
│  ZONE 1: Deck Row  [weight=1f, fillMaxWidth]         │
│  ┌─────────────────┐  ┌─────────────────┐           │
│  │   DeckPanel A   │  │   DeckPanel B   │           │
│  │  [weight=1f]    │  │  [weight=1f]    │           │
│  └─────────────────┘  └─────────────────┘           │
├─────────────────────────────────────────────────────┤
│  ZONE 2: WaveformStrip Row  [height固定, fillMaxWidth]│
│  ┌───────────────┐  ┌───────────────┐               │
│  │ WaveformStrip A│  │ WaveformStrip B│               │
│  └───────────────┘  └───────────────┘               │
├─────────────────────────────────────────────────────┤
│  ZONE 3: SamplePadRow  [height固定, fillMaxWidth]    │
│  [PAD]  [P1]  [P2]  [P3]  [P4]                      │
├─────────────────────────────────────────────────────┤
│  ZONE 4: CrossfaderPanel  [height固定, fillMaxWidth] │
│  A ────────────●──────────── B                       │
└─────────────────────────────────────────────────────┘
```

---

## DeckPanel 内部構造（変更しない）

```
DeckPanel (Column, fillMaxHeight)
├── [Section 1] ヘッダー Row
│   ├── デッキバッジ（28dp circle, deckColor背景）
│   ├── Column: トラック名 / BPM + ÷2/×2 + SYNC(Bのみ)
│   └── ライブラリアイコンボタン
│
├── [Section 2] 中段 Row
│   ├── ManualLoopSection  [weight=0.38f]
│   │   └── IDLE / IN_SET / ACTIVE の3状態チップ
│   └── Column [weight=0.62f]
│       ├── EqSection: HIGH / MID / LOW ロータリーノブ
│       └── Pitch スライダー
│
└── [Section 3] 下段 Row [weight=1f]
    ├── Column [weight=0.40f]
    │   ├── Stop + Play/Pause ボタン行
    │   └── SCRATCH モード選択チップ行（CHOP/WHEEL/STRIP）
    └── Box [weight=0.60f]
        └── ChopPadScratch / VelocityWheelScratch / FullStripScratch
            （scratchMode に応じて切替）
```

---

## Theme.kt への適用方針

上記カラートークンを `lightColorScheme()` / `darkColorScheme()` に設定する。
アプリは常にダークモードのみ使用するため `darkColorScheme` のみ定義すれば良い。

Space Grotesk フォントは `FontFamily` で定義し、BPM 等の技術値表示箇所で `fontFamily` を指定する。
既存の `Typography.kt` を拡張して `technicalData` スタイルを追加する。
