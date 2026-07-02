---
name: Fix Sonar Issues
description: Run local static analysis and fix findings with conservative, behavior-preserving changes.
---

You are an expert Java developer working on Issues. Fix **all issues surfaced by local static analysis** — the same class of problems SonarCloud flags, without calling SonarCloud or using any token.

Align with [static-analysis.mdc](../rules/static-analysis.mdc): **local checks only**.

Follow this loop — **do not ask for confirmation** before editing.

## 1. Discover issues (local)

```bash
mvn -B compile -Dmaven.compiler.showWarnings=true -Dmaven.compiler.showDeprecation=true
```

Scan IDE diagnostics: `ReadLints` on changed `src/main/java` and `src/test/java`.

Do **not** call SonarCloud API.

## 2. Prioritize

1. Compile errors, failing `verify`
2. Reliability smells: empty catches, `System.out`, `printStackTrace`
3. Compiler + linter warnings: unused code, deprecations
4. Maintainability: cognitive complexity, duplicated literals

## 3. Fix strategies

| Theme | Safe approach | Avoid |
|-------|---------------|-------|
| Unused imports / dead code | Remove after confirming zero references | Deleting public API |
| Exception handling | SLF4J + rethrow or `IssuesException` | Empty catch |
| Test smells | Fix assertion or setup | `@Disabled`, weakened assertions |
| HTTP contract | Use `*Request`/`*Response` records | VO/DTO suffix |

**Never:** weaken workflow validation; delete tests to green CI.

## 4. Verify each batch

```bash
mvn formatter:format
mvn -B verify
```

## 5. Log every change

Append to `reports/sonar_fix_log-{sequential}-{dd-MM-yyyy-HH-mm-ss}.md`.

## 6. Stop when

- `mvn verify` passes
- `ReadLints` clean on touched files

Print `✅ Local static analysis clean!` and summarize.

Start the loop now.
