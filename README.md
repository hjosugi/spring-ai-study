# spring-ai-study

Spring AI を理解するための個人用ノートと実験コード。
[spring-projects/spring-ai](https://github.com/spring-projects/spring-ai) とは別のリポジトリで、fork（`hjosugi/spring-ai`）とも git 上のつながりはない。

## 置き場所のルール

| 場所 | 入れるもの |
| --- | --- |
| `../spring-ai`（fork） | upstream の鏡である `main` と、upstream に出す PR 用ブランチだけ |
| このリポジトリ | ノート、図、実験コード、読みかけのメモ |

ノートや実験コードを fork 側に置かない。
fork に無ければ、PR に混ざることも upstream に届くこともない。

## 構成

```
notes/   読んで分かったことのメモ（参照したコミットを各ファイルの先頭に書く）
labs/    リリース済みの Spring AI に依存する Maven プロジェクト。API キー不要で動くものだけ置く
```

| ノート | 内容 |
| --- | --- |
| [notes/00-fork-workflow.md](notes/00-fork-workflow.md) | fork を最新に保つ手順と、PR ブランチの扱い |
| [notes/01-module-map.md](notes/01-module-map.md) | リポジトリのモジュール地図と依存の向き |
| [notes/02-chatclient-call-path.md](notes/02-chatclient-call-path.md) | `ChatClient.call()` が `ChatModel` に届くまで |
| [notes/roadmap.md](notes/roadmap.md) | 次に読むもの・作るもの |

| ラボ | 内容 |
| --- | --- |
| `labs/src/*/java/study/lab01` | 台本どおりに返す `ChatModel` を本物の `ChatClient` に差し込み、Advisor の順序とツール呼び出しループを観察する |
| `labs/src/*/java/study/lab02` | Advisor チェーンとツールループを自分で小さく再実装し、lab01 と同じ動きになることを確かめる |

## ラボの実行

JDK 17 以上が必要。

```bash
cd labs
./mvnw test                               # 全部
./mvnw test -Dtest=CallPathTest           # 1 クラスだけ
```

依存する Spring AI のバージョンは `labs/pom.xml` の `spring-ai.version`（今は Maven Central の 2.1.0）。
fork の作業ツリーの変更を試したいときは、fork 側で `./mvnw -DskipTests install` してから、その SNAPSHOT バージョンを `spring-ai.version` に指定する。
