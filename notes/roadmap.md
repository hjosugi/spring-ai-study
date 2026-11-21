# ロードマップ

中心から外へ向かって読む。
各項目は「読む場所」と「labs に作るもの」の組で、作るものは API キー無しで動く範囲にする。

## 済み

- [x] `ChatClient` → Advisor チェーン → `ChatModel`（[02-chatclient-call-path.md](02-chatclient-call-path.md)、lab01、lab02）

## 次

- [ ] **ストリーミング**
  - 読む: `ChatModelStreamAdvisor`、`ToolCallingAdvisor.adviseStream`
  - 作る: `ScriptedChatModel` に `stream(Prompt)` を足し、チャンクがどこで 1 つの応答にまとめられるかをテストで見る
  - PR #7424（Anthropic のストリーム中断）と同じ領域
- [ ] **ツール実行の中身**
  - 読む: `spring-ai-model` の `model/tool/DefaultToolCallingManager`、`tool/method/MethodToolCallback`、`tool/annotation/Tool`
  - 作る: `@Tool` を付けたメソッドを `.tools(...)` で渡す。ツールが例外を投げたとき、モデルに何が返るかを見る
- [ ] **構造化出力**
  - 読む: `converter/BeanOutputConverter`、`ChatClient` の `entity(...)`
  - 作る: 台本モデルに JSON を返させて record に変換する。プロンプトに足されるフォーマット指示を `receivedPrompts()` で見る
- [ ] **チャットメモリ**
  - 読む: `chat/memory/MessageWindowChatMemory`、`InMemoryChatMemoryRepository`、`MessageChatMemoryAdvisor`
  - 作る: 2 ターンの会話で、2 回目の `Prompt` に 1 回目のやり取りが入ることを見る
- [ ] **RAG**
  - 読む: `spring-ai-rag` の `advisor/RetrievalAugmentationAdvisor`、`spring-ai-vector-store` の `SimpleVectorStore`
  - 作る: 決め打ちのベクトルを返す `EmbeddingModel` を書き、検索結果がプロンプトに差し込まれる様子を見る
- [ ] **プロバイダ実装を 1 つ縦に読む**
  - 読む: `models/spring-ai-anthropic` の `AnthropicChatModel`（`Prompt` をリクエストに変換し、レスポンスを `ChatResponse` に戻すところ）
  - 作る: ノートだけでよい
- [ ] **Spring Boot への載せ方**
  - 読む: `auto-configurations/models/spring-ai-autoconfigure-model-anthropic`
  - 作る: `ScriptedChatModel` を Bean にする最小の auto-configuration
- [ ] **MCP**、**observability** は上が終わってから

## ノートの書き方

- 先頭に参照したコミットを書く。行番号はそのコミットのもの
- コードから読み取ったことと、テストで確かめたことを区別して書く
- 「まだ読んでいないところ」を最後に残す
