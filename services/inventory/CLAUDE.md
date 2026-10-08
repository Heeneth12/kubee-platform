## Code navigation (llm-index) — MANDATORY, follow in order, every task

This repo is auto-indexed by llm-index on every app boot (see `llm-index.startup.*` in
`application.properties`). This sequence is not optional and does not get skipped for
"simple" or "obvious" questions — divergence is exactly what causes missed context.

1. **Query first.** Before opening or grepping anything:

       java -jar /Users/venkatheenethsai/Documents/kubee/indexer/target/llm-index-exec.jar query <ClassName or keyword>

   Never use the `llm-index` shell alias — it only exists in the user's interactive terminal,
   not in this non-interactive session. Always use the full `java -jar ...` command above.
   Add `--hops 2` or `--hops 3` if you need to trace further outward (what calls this, what it
   calls).

2. **Read the index docs, not the source, first.** In `.llm-index/`, in this order:
   - `01-tree.md` — project layout
   - `02-skeleton.md` — fields/methods/annotations for the classes in scope
   - `03-dependencies.md` — what each class touches

   These alone are enough to decide *which* files actually need opening. Do not skip to
   step 3 "to be safe" — that's the exact habit this rule exists to stop.

3. **Open only what's left unresolved.** Read only the specific file:line references from
   step 1, or the full file only when you must edit it (e.g. exact import text for a diff).
   Do not read files unrelated to the task, and do not grep speculatively.

4. **Rebuild after structural changes.** Any new/renamed/deleted class or method:

       java -jar /Users/venkatheenethsai/Documents/kubee/indexer/target/llm-index-exec.jar build .

   Do this yourself before your next query — don't wait to be asked, and don't restart the
   app just to refresh it.

If a task seems to obviously not need this (e.g. a pure question with no file lookup), say so
explicitly rather than silently skipping the sequence.