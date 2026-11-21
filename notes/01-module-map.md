# モジュール地図

参照コミット: `51ce70ecfe`（upstream/main、2026-11-20、バージョン 2.1.1-SNAPSHOT）

ルートの `pom.xml` には 180 個ほどのモジュールが並ぶが、ほとんどは「プロバイダごとの実装」と「それを Spring Boot に載せるための箱」。
仕組みを理解するために読む必要があるのは、最初の表の数個だけ。

## 中心（ここを読む）

依存の向きは上から下。下のモジュールは上のモジュールを知らない。

```
spring-ai-commons
   └─ spring-ai-template-st
        └─ spring-ai-model
             ├─ spring-ai-client-chat
             ├─ spring-ai-vector-store
             │    └─ spring-ai-rag   （spring-ai-client-chat にも依存）
             └─ spring-ai-session
```

| モジュール | 主なパッケージ（`org.springframework.ai.` 以下） | 役割 |
| --- | --- | --- |
| `spring-ai-commons` | `document` `reader` `transformer` `writer` `tokenizer` `observation` | `Document` と、その読み込み・分割・書き出し（ETL）。モデルに依存しない土台 |
| `spring-ai-template-st` | `template.st` | プロンプトテンプレートの StringTemplate 実装 |
| `spring-ai-model` | `chat.model` `chat.messages` `chat.prompt` `chat.memory` `tool` `model.tool` `embedding` `image` `audio` `converter` | モデルの抽象（`ChatModel`、`EmbeddingModel` など）、メッセージ、`Prompt`、ツール定義と実行、チャットメモリ、構造化出力の変換 |
| `spring-ai-client-chat` | `chat.client` `chat.client.advisor` | `ChatClient`（fluent API）と Advisor チェーン |
| `spring-ai-vector-store` | `vectorstore` `vectorstore.filter` | `VectorStore` の抽象とフィルタ式 |
| `spring-ai-rag` | `rag.preretrieval` `rag.retrieval` `rag.postretrieval` `rag.generation` `rag.advisor` | RAG の各段階の部品と、それを Advisor として組むもの |
| `spring-ai-session` | `session` `session.compaction` | セッションメモリ。追記のみの会話ログと、その圧縮 |

## 実装（必要になったものだけ読む）

| ディレクトリ | 中身 |
| --- | --- |
| `models/` | プロバイダごとの `ChatModel` などの実装。`spring-ai-anthropic`、`spring-ai-openai`、`spring-ai-ollama` など 18 個 |
| `vector-stores/` | `VectorStore` の実装。22 個 |
| `advisors/` | 中心モジュールの外にある Advisor（session、tool-search、vector-store） |
| `memory-repositories/` | チャットメモリの保存先（JDBC、Redis、MongoDB など） |
| `document-readers/` | PDF、Markdown、Tika、jsoup の `DocumentReader` |
| `mcp/` | Model Context Protocol 連携（`common`、`mcp-annotations`、`transport`） |

## Spring Boot に載せる箱（仕組みの理解には後回しでよい）

| ディレクトリ | 中身 |
| --- | --- |
| `auto-configurations/` | 上の実装それぞれの auto-configuration と `@ConfigurationProperties`。59 個 |
| `starters/` | 依存をまとめるだけの starter。52 個 |
| `spring-ai-bom` | バージョンをそろえる BOM |
| `spring-ai-spring-boot-testcontainers`、`spring-ai-spring-boot-docker-compose` | 開発・テスト用のサービス接続 |

## その他

| ディレクトリ | 中身 |
| --- | --- |
| `spring-ai-docs` | リファレンス（Antora、`.adoc`） |
| `spring-ai-test`、`spring-ai-integration-tests` | テスト用の共通部品と結合テスト |
| `spring-ai-retry` | プロバイダ呼び出しのリトライ設定 |
| `spring-ai-tool-search-tool` | Tool Search Tool の API 型（`ToolIndex` など） |

## 1 つのプロバイダを縦に追うとき

Anthropic を例にすると、次の 3 つが 1 組になっている。

```
models/spring-ai-anthropic                                           実装（AnthropicChatModel）
auto-configurations/models/spring-ai-autoconfigure-model-anthropic   Bean 定義とプロパティ
starters/spring-ai-starter-model-anthropic                           依存をまとめる pom
```
