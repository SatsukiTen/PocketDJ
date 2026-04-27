# ADR-002: UIフレームワークの選定

| 項目 | 内容 |
|---|---|
| **ID** | ADR-002 |
| **状態** | 確定 |
| **決定日** | 2026-04-25 |
| **関連制約** | Constitution §2 直感操作優先、Android 16対応 |

---

## 背景

DJアプリのUIは波形表示・スクラッチゾーン・複数のノブ・スライダーなど
カスタムUIコンポーネントが多く、タッチ応答性が重要。
AndroidのUIフレームワークとして現在2つの選択肢がある。

---

## 選択肢の比較

### Option A: **Jetpack Compose（推奨）**

> Googleが推進するKotlinファーストの宣言的UIフレームワーク。

| 項目 | 評価 |
|---|---|
| Googleの推奨度 | ◎ 新規Androidアプリの標準 |
| カスタムUI | ◎ Canvas APIで波形・ノブ・スクラッチゾーン等を自由に描画 |
| タッチ処理 | ◎ PointerInput APIで多点タッチ・ジェスチャーを柔軟に処理 |
| Android 16対応 | ◎ 最新APIとの親和性が高い |
| 学習コスト | △ XML Viewsより新しい概念（ただし長期的には効率的） |
| パフォーマンス | ○ 適切に実装すればスムーズ（不要な再コンポーズを避ける設計が必要） |

### Option B: XML Views（従来方式）

> 長年のAndroid標準。XMLでレイアウトを定義し、ViewBindingで操作する。

| 項目 | 評価 |
|---|---|
| Googleの推奨度 | △ 新規開発では非推奨（既存コードのメンテナンス向け） |
| カスタムUI | ○ CustomView + Canvas で実装可能（ただしボイラープレートが多い） |
| タッチ処理 | ○ onTouchEvent で実装可能 |
| Android 16対応 | △ 動作はするが新APIとの統合が煩雑 |
| 学習コスト | ◎ 既存知識が活かしやすい |
| パフォーマンス | ○ 安定しているが最適化が手動 |

---

## 推奨: **Option A — Jetpack Compose**

### 理由
- Android 16をターゲットとする新規プロジェクトで、Googleが明確に推奨
- カスタム波形・スクラッチゾーン・ノブUIをCanvas + PointerInputで実装しやすい
- 状態管理（再生状態・フェーダー位置）とUIの連携がCompose Stateで自然に書ける

### トレードオフ
- 波形描画など重いカスタム描画はパフォーマンスに注意が必要
- 対策: `drawBehind` / `Canvas` をremember + derivedStateOf で最適化する

---

## 決定

**Option A — Jetpack Compose を採用する。**
Android 16対応の新規プロジェクトとして最適。Canvas + PointerInputでカスタムUIを実装する。
