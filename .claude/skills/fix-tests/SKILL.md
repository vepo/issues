---
name: fix-tests
description: Automatically fix all failing Maven tests by iterating until they pass. User-invoked only (/fix-tests).
disable-model-invocation: true
---

You are an expert Java developer. Fix **all failing tests** in the Issues project.

Follow `.cursor/rules/issues-testing.mdc` and `.cursor/rules/issues-test-failure-diagnosis.mdc`.

Follow this exact loop — **do not ask for confirmation** and **do not invent workarounds**.

1. **Discover failures**
   ```bash
   mvn test
   ```
   If frontend may be affected, also run:
   ```bash
   cd src/main/webui && npm test -- --no-watch --browsers=ChromeHeadless
   ```

2. **Check for failures**
   - If **no test failures**, run `mvn verify`. If green, print `✅ All tests pass!` and stop.
   - Otherwise proceed to step 3.

3. **List each failing test with the reason**
   Parse `target/surefire-reports/*.txt`. Group failures by root cause. For each:
   - Test class & method name
   - Exception type and stack trace
   - Assertion details (expected vs actual)

4. **Fix each root cause** (one at a time)
   - Read `ARCHITECTURE.md` and the test + production code.
   - **Apply a direct fix** — never `Thread.sleep()`, `@Disabled`, or swallowed exceptions.
   - Re-run: `mvn test -Dtest=ClassName#methodName`

5. **Repeat steps 3–4** until tests pass, then run **`mvn verify` once**.

**After fix is green:** write one report under `reports/` per `.cursor/rules/issues-test-failure-diagnosis.mdc`.

Start the loop now.
