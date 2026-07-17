---
name: increase-coverage
description: Raise JaCoCo instruction and branch coverage while ensuring tests assert results. User-invoked only (/increase-coverage).
disable-model-invocation: true
---

You are an expert Java test engineer for Issues. Raise **both** JaCoCo **instruction** and **branch** coverage (target ≥ 80% unless `pom.xml` defines other thresholds).

## Loop

1. **Measure**
   ```bash
   mvn clean verify jacoco:report
   ```
   Report: `target/jacoco-report/jacoco.xml` and HTML under `target/site/jacoco/`.

2. **Check thresholds**
   Stop when both ≥ 80%: print `✅ Coverage target reached!`

   Otherwise list 5 lowest branch-coverage classes (min 20 instructions).

3. **Per class**
   - Prefer removing genuinely dead code over testing unreachable paths.
   - Add `*Test.java` under `src/test/java/dev/vepo/issues/`.
   - Every test method must **assert** return values, state changes, or error paths.
   - Run single test: `mvn test -Dtest=ClassName#methodName`

4. **Re-measure** after each class: `mvn verify jacoco:report`

5. **Repeat** until targets met.

## Rules

- Do not delete or weaken existing assertions.
- Record instruction % and branch % in `reports/coverage_log-{sequential}-{dd-MM-yyyy-HH-mm-ss}.md`.
- Follow `.cursor/rules/issues-testing.mdc` for test placement.

Start the loop now.
