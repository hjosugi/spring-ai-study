# fork を最新に保つ

2026-11-21 時点の `~/ghq/github.com/hjosugi/spring-ai` の設定を前提にした手順。

## リモートとブランチの役割

| 名前 | 中身 | 決まり |
| --- | --- | --- |
| `upstream` | `spring-projects/spring-ai` | fetch 専用。push URL は `DISABLED_no_push_to_upstream` にしてあるので、間違えて push しても失敗する |
| `origin` | `hjosugi/spring-ai`（fork） | push してよいのは `main` の fast-forward と PR 用ブランチ |
| `main` | `upstream/main` の鏡 | **ここには自分でコミットしない** |
| PR 用ブランチ | 1 PR につき 1 本 | 必ず `upstream/main` から切る |

## main を最新にする

```bash
git fetch upstream
git switch main
git merge --ff-only upstream/main
git push origin main
```

`--ff-only` は、`main` に upstream に無いコミットがあると失敗して止まる。
止まったら「鏡ではなくなっている」という合図なので、merge で先に進めずに原因を見る。

```bash
git log --oneline upstream/main..main     # main にだけあるコミット
```

残す価値のあるものが無ければ、鏡に戻す。

```bash
git reset --hard upstream/main
git push --force-with-lease origin main
```

毎回打つのが面倒なら alias にできる。

```bash
git config alias.sync '!git fetch upstream && git switch main && git merge --ff-only upstream/main && git push origin main'
git sync
```

## GitHub の「Sync fork」ボタン

`main` が鏡のままなら fast-forward されるだけなので使ってよい（その後ローカルで `git pull --ff-only`）。
`main` が分岐していると、ボタンは merge commit を作る。
2026-11-20 にできた `c98d3e845 Merge branch 'spring-projects:main' into main` がそれで、原因は upstream の `4cce4caa9` と同じ内容で SHA だけ違うコミット `0ec089f70`（committer が自分）が fork の `main` に入っていたこと。
2026-11-21 に `main` を `upstream/main` に reset して解消した。

## PR 用ブランチ

新しく作る。

```bash
git fetch upstream
git switch -c <branch> --no-track upstream/main
# ... 作業、コミット（-s で Signed-off-by を付ける）...
git push -u origin <branch>
```

`--no-track` を付けるのは、付けないと追跡先が `upstream/main` になり、引数なしの `git push` / `git pull` の相手が upstream になるため。

出してある PR を最新の `main` に追従させる。

```bash
git fetch upstream
git switch <branch>
git rebase upstream/main
git push --force-with-lease origin <branch>
```

`--force` ではなく `--force-with-lease` を使う。
リモートに自分の知らないコミットが入っていたら push を拒否してくれる。

## upstream に影響しないことの確認

- upstream に何かが届くのは、PR を作ったときと、issue や PR にコメントしたときだけ。fork への push では届かない。
- `gh pr create` は fork から実行すると upstream 宛ての PR を作る。実行前にブランチと差分を確認する（`git log --oneline upstream/main..HEAD`）。
- fork の GitHub Actions は一度も実行されていない（2026-11-21 に `gh run list` で確認）。fork の Actions タブでワークフローを有効化しない限りそのまま。
