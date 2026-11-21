# `ChatClient.call()` が `ChatModel` に届くまで

参照コミット: `51ce70ecfe`（upstream/main、2026-11-20）。
行番号はこのコミットのもの。
パスは `spring-ai-client-chat/src/main/java/org/springframework/ai/chat/client/` からの相対（`ChatModel.java` だけ `spring-ai-model`）。

ここに書いた動きは `labs` の lab01（`CallPathTest`）で、リリース版 2.1.0 に対して確かめてある。

## 登場するもの

| 型 | 役割 |
| --- | --- |
| `ChatModel` | プロバイダの抽象。実装が必須なのは `ChatResponse call(Prompt)` だけ（`ChatModel.java:45`） |
| `ChatClient` | アプリが使う fluent API。`ChatModel` を 1 つ包む |
| `CallAdvisor` | モデル呼び出しを包むインターセプタ。`adviseCall(request, chain)` の中で `chain.nextCall(request)` を呼ぶ |
| `ChatClientRequest` / `ChatClientResponse` | Advisor の間を流れる入れ物。`Prompt`（または `ChatResponse`）と、Advisor 同士が値を渡すための `context` マップ |

## 流れ

`chatClient.prompt().user("...").call().content()` を例にする。

1. **`.call()` でチェーンを組む。**
   `DefaultChatClient.java:1230` の `call()` が `buildAdvisorChain()`（`:1243`）を呼ぶ。
   ここで登録済みの Advisor の後ろに `ChatModelCallAdvisor` を足す（`:1250`）。
   つまり「モデルを呼ぶ」こと自体が、チェーンの最後の Advisor として実装されている。

2. **ツール用の Advisor が自動で入る。**
   同じ `buildAdvisorChain()` の先頭で `autoRegisterToolCallingAdvisor()`（`:1271`）が呼ばれ、`ToolAdvisor` が 1 つも登録されていなければ `ToolCallingAdvisor` を足す。
   自動登録を無効にしていない限り、ツールを 1 つも渡していない呼び出しでも入る。

3. **`Prompt` のオプションはモデルのものから作られる。**
   `DefaultChatClientUtils.java:103` で `chatModel.getOptions().mutate()` を出発点にし、それが `ToolCallingChatOptions` のビルダーなら、リクエストで渡したツールをそこに載せる（`:108`）。

4. **並び順は `getOrder()` で決まる。**
   `DefaultAroundAdvisorChain` が `OrderComparator` で並べ替える（`advisor/DefaultAroundAdvisorChain.java:277`）。
   値が小さいほど外側で、リクエストを先に見て、レスポンスを最後に見る。
   登録した順番は関係ない。

5. **`.content()` で実際に走る。**
   `DefaultChatClient.java:703` の `advisorChain.nextCall(request)` が入口。
   `nextCall` は deque から Advisor を 1 つ pop して `adviseCall` を呼ぶ（`advisor/DefaultAroundAdvisorChain.java:98`、`:105`）。
   各 Advisor が `chain.nextCall(...)` を呼ぶことで次へ進む。

6. **最後に `ChatModelCallAdvisor` がモデルを呼ぶ。**
   `advisor/ChatModelCallAdvisor.java:57` の `this.chatModel.call(prompt)`。
   構造化出力のフォーマット指示をユーザーメッセージに足すのもここ。

## ツール呼び出しループ

`ToolCallingAdvisor.adviseCall`（`advisor/ToolCallingAdvisor.java:144`）がやること。

```
オプションが ToolCallingChatOptions でなければ、何もせず次へ          (:149)
do {
    チェーンの「自分より後ろ」をコピーして実行                        (:188)
    レスポンスにツール呼び出しがあれば ToolCallingManager で実行       (:203)
    会話履歴（ツール要求とツール結果を足したもの）を次の入力にする
} while (ツール呼び出しがあった)                                       (:244)
```

押さえておく点が 3 つある。

- **チェーンは使い捨て。**
  `nextCall` が pop していくので、同じチェーンを 2 回は走らせられない。
  ループの各周で `callAdvisorChain.copy(this)`（`advisor/DefaultAroundAdvisorChain.java:169`）を使うのはそのため。
  lab02 の `MiniChain` で同じ構造を自分で書いた。

- **ループの内側にいる Advisor は周回ごとに走る。**
  `ToolCallingAdvisor.DEFAULT_ORDER` は `Ordered.HIGHEST_PRECEDENCE + 300`（`:96`）で、かなり外側。
  order がそれより大きい Advisor（たとえば order 0）はループの内側に入り、ツール呼び出しが 1 回あればモデル呼び出しと同じく 2 回走る。
  lab01 の `toolCallMakesTheAdvisorLoopCallTheModelTwice` がこれを見ている。
  `DEFAULT_ORDER` が `+ 300` なのは、既定 order が `HIGHEST_PRECEDENCE + 200` のメモリ用 Advisor をループの外側に置くため。

- **自作の `ChatModel` でツールを使うには `getOptions()` を上書きする。**
  `ChatModel.getOptions()` の既定実装（`ChatModel.java:52`）は素の `ChatOptions` を返す。
  そのままだと上の 3 でツールが載らず、`:149` の判定でループも素通りする。
  lab01 の `ScriptedChatModel` は `ToolCallingChatOptions.builder().build()` を返している。

## まだ読んでいないところ

- ストリーミング側（`adviseStream`、`ChatModelStreamAdvisor`、チャンクの集約）
- `ToolCallingManager` の中身（ツールの解決、実行、回数制限、確認待ち）
- 実プロバイダの `ChatModel` が `Prompt` を HTTP リクエストに変換するところ
