# sbt 2 incremental test selection skips suites after an external dependency change

A minimal, self-contained reproduction of a false-green CI scenario on sbt 2: a change to the
runtime classpath brought by dependencies does not re-run test suites, because the incremental
test selection's cache key does not change. Three shapes are demonstrated; the third is
structurally distinct in what it rules out for a fix.

- sbt **2.0.12** (latest stable) — all three shapes; sbt **2.1.0-RC3** — also reproduced
- Scala **2.13.18**
- Every consumer suite asserts the old behavior of the dependency and never imports it in test
  code (the test reaches it through main code only).

## Layout

```
libs/
  duration-formatter/v1   DurationFormat.render(d) == "90s"      (version 0.1.0)
  duration-formatter/v2   DurationFormat.render(d) == "1m 30s"   (version 0.2.0, same API)
  duration-report/v1      a published intermediate that calls the formatter
case1-direct/            failure mode F1
case2-internal-module/   failure mode F2
case3-eviction/          failure mode F3
```

The dependency under test is published in two versions with the **same API** and different
behavior — mirroring real-world dependency bumps where a library changes its output behind an
unchanged API (formatters, parsers, serializers). Consumer classes compile byte-identically
against either version — references are symbolic — so nothing about consumer *code* changes in
any scenario. Each `case*` directory is an independent sbt build (the consumer under test).

## Failure modes

| # | directory | shape | the one-line change |
|---|---|---|---|
| F1 | `case1-direct/` | library is a direct dependency | bump `duration-formatter` 0.1.0 → 0.2.0 |
| F2 | `case2-internal-module/` | library consumed by an internal module; tests in the depending module | bump the library version in `core`'s `build.sbt` |
| F3 | `case3-eviction/` | a **security-fix-style pin** (`dependencyOverrides`) evicts the transitive dependency | uncomment `dependencyOverrides += "example" %% "duration-formatter" % "0.2.0"` (referenced by no app code; evicts the transitive 0.1.0) |

## Reproduce

Publish all library versions once:

```bash
cd libs/duration-formatter/v1 && sbt publishLocal
cd ../v2 && sbt publishLocal
cd ../../duration-report/v1 && sbt publishLocal
cd ../..
```

Per case (shown for F1; identical recipe for the others, with the change noted above):

```bash
cd case1-direct
rm -rf .sbt-local-cache target project/target  # project-level selection state
rm -rf ~/Library/Caches/sbt/v2                 # machine-wide action cache (macOS;
                                               # Linux: ~/.cache/sbt/v2) — affects other
                                               # projects on this machine, see below
sbt test                            # baseline: suite runs and passes
sbt 'show Test/definedTestDigests'  # note the digest
# apply the one-line change in build.sbt
sbt 'show Test/definedTestDigests'  # same digest
sbt test                            # skipped
sbt testFull                        # runs, and fails
git checkout -- build.sbt
```

The machine-wide wipe is what makes the run genuinely cold: sbt 2 caches test results
**machine-wide** (`~/Library/Caches/sbt/v2`, `ac`/`cas`/`proc`), in addition to the
project-local `.sbt-local-cache`. Without it, a previously-run suite may report `cached` instead
of running, even after the project-level wipe. It also clears cached results for other projects
on the machine — they will simply re-run. This recipe was verified from a fresh clone of this
repository.

Note that the digest values below are **specific to our machine and directory layout** — the
same repository cloned to a different path produced a different digest (e.g. F1 shows
`sha256-3f2d9b6f…/64` from a fresh clone in another directory). What matters, and what the
reproduction is about, is that the value is **identical before and after the dependency change**.

## Observed output (sbt 2.0.12)

In **all three shapes** the digest is identical before and after the change, the incremental test
skips the suite, and `testFull` fails:

| shape | `Test / definedTestDigests` (same before and after) | `sbt test` | `sbt testFull` |
|---|---|---|---|
| F1 `case1-direct` | `sha256-da5b6dc3178e9b3e9f1a3babcdf8ff118e007c69b3832371992b4050e6fece2f/64` | `No tests to run for Test / testQuick` | `Failed: Total 1, Failed 1` |
| F2 `case2-internal-module` | `sha256-50f92d5cc3133955cdd943a380f09bf60d936a320f157050353cb4ea5c57cf79/80` | `No tests to run for Test / testQuick` | `Failed: Total 1, Failed 1` |
| F3 `case3-eviction` | `sha256-b2e25e3ae97f19f477b090a2a53431f42702f3b22ac1ca34618a33a537876972/64` | `No tests to run for Test / testQuick` | `Failed: Total 1, Failed 1` |

The F3 failure message shows the pin took effect — the suite now observes the evicting
version's output:

```
==> X app.ReportSpec.renders a duration in seconds  0.014s munit.ComparisonFailException: …/case3-eviction/src/test/scala/app/ReportSpec.scala:7
values are not the same
=> Obtained
1m 30s
=> Diff (- obtained, + expected)
-1m 30s
+90s
```

## Observed output (sbt 2.1.0-RC3, shape F1)

The digest is also unchanged on 2.1.0-RC3 (different digest computation, same omission):

```
[info] Map(app.ReportSpec -> sha256-c2e915e83f2ec57d3b587a28003ed0ce9009a3fdd48017d867cb502368d8bbe2/96)
```

and the skip is presented by the new test summary as a **pass** — the stale cached result
laundered into a green line that `testFull` immediately refutes:

```
$ sbt test
[info] passed: total 1, failed 0, errors 0, passed 1, cached 1

$ sbt testFull
[error]         app.ReportSpec     FAIL
[error] failed: total 1, failed 1, errors 0, passed 0, cached 0
```

Switching `sbt.version` requires restarting the sbt server first (`pkill -f sbt-launch.jar`):
a running server does not pick up a `build.properties` change and keeps running the old version.

## Why, and what it rules out for a fix

In sbt 2, `Test / test` is the incremental test (`test == testQuick` in 2.0.x): every suite
success is cached in the action cache under a Merkle-tree SHA-256 digest computed by
`Test / definedTestDigests` (see `sbt.internal.IncrementalTest` and `ClassStamper`). A suite is
skipped when the action cache holds a success for the suite's current digest.

The digest covers the suite's transitive **internal class** bytecode, resources, and test
options — external dependency jars contribute nothing. F1 makes that visible for a directly
referenced jar: the jar is imported by the consumer's own analyzed code, its bytes change across
the bump, and the digest does not move. F2 extends it to the monorepo shape where the dependency
travels through an internal module — the shape where sbt's documentation claims coverage
("tests that have one or more transitive dependencies, maybe in a different project,
recompiled").

F3 is the structurally distinct mode: the version change enters the classpath purely through
**resolution** — a dependency pin (the security-fix workflow: force the CVE-patched version of a
transitive you never import) evicts the old one — and the changing jar is referenced by no
analyzed code at all, only by the published bytecode of `duration-report`, which carries no zinc
analysis. No fix keyed on analysis-reachable artifacts can see it, which rules out the tempting
half-fix ("stamp the jars that `externalDeps` reaches") and forces the digest to incorporate the
**resolved transitive runtime classpath** (or the resolved dependency graph). Zinc's *compile*
layer behaves correctly in every shape shown here — it recompiles affected sources against the
new jars (byte-identically) — which is what isolates the defect to the test-selection layer.

## Related

- sbt/sbt#8072 — the issue that introduced the digest-based incremental test selection (sbt 2.0).
- sbt/sbt#10037 / sbt/sbt#10038 — a sibling of this bug (main *resources* missing from the same
  digest), fixed for 2.1.0; the dependency dimensions demonstrated here are still open (verified
  on 2.1.0-RC3).
- `sbt testFull` runs every suite unconditionally and is the workaround.
