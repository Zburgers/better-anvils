# Better Anvils 26.2 Implementation Plan

> **Shipyard writing-plans format.** Execute tasks in order with test-first, build, review, and commit gates. **Planning only:** this document does not assert the mod has been implemented or tested.

**Goal:** Ship one reliable Minecraft Java 26.2 Fabric mod that permits 100-level post-discount anvil work, makes valid operations 50% cheaper by default, and provides an in-game settings menu that automatically writes a JSON configuration.

**Architecture:** Keep Minecraft's own `AnvilMenu` responsible for validating ingredients, applying enchantments, item repair, naming, XP deduction, and anvil wear. Apply narrowly targeted Mixin transformations to calculated XP cost and the vanilla expense gate, synchronize the authoritative server policy to clients, and expose the same validated config through an optional Mod Menu + Cloth Config client screen.

**Tech stack:** Minecraft 26.2; Java 25; Fabric Loader >=0.19.3; Fabric Loom 1.17.x / non-obfuscated plugin `net.fabricmc.fabric-loom`; Fabric API `0.161.0+26.2`; Gradle 9.5.1 as official baseline (verify final supported wrapper); Mod Menu 20.0.3; Cloth Config Fabric 26.2.155; JUnit 5 + Fabric Loader JUnit + Fabric GameTest.

**Repository:** https://github.com/Zburgers/better-anvils  
**Planning branch:** `26.2-better-anvils`  
**Plan date:** 2026-10-13  
**Status:** PLAN READY FOR REVIEW; implementation NOT STARTED.  
**Initial baseline:** an almost empty `main` branch containing `README.md`; separate work branch already exists.  
**Reference:** MIT-licensed Too Cheap https://github.com/the-sh4d0w/too-cheap (older constraints; NOT a ready-to-use 26.2 implementation).  
**Compatibility fixture:** https://modrinth.com/datapack/higherenchants (external, not redistributed).

---

## 1. Signed-off product contract

1. User flow: **Minecraft → Mods → Better Anvils → Configure → adjust → Save**. Mod Menu provides the Mods entry; Cloth Config provides controls; Save immediately serializes validated data to `config/better-anvils.json`, with no manual JSON editing needed in singleplayer.
2. Default policy: `maximumAnvilCost=100`, `anvilDiscountPercent=50`, `minimumAnvilCost=1`, `preservePriorWorkPenalty=true`.
3. User-editable bounds: discount **0..100** integer percent, maximum **1..500** levels, minimum **1..10** levels and `minimum <= maximum`; prior-work toggle boolean. If range/UX limits change later, update tests, config schema, and README together.
4. For a *valid* operation with pre-discount cost `C > 0`, compute `discounted = max(minimumAnvilCost, ceil(C * (100 - percent) / 100))`. Use 64-bit intermediates; no integer overflow. If original cost is zero, input invalid, incompatible, or output empty, do NOT fabricate a result or cost.
5. A normal result is available if and only if its **discounted** cost is `<= maximumAnvilCost` and all vanilla validation rules pass. In survival, 100 is valid and 101 rejected (at default cap); no changed creative bypass semantics.
6. **Rename-only exception:** vanilla allows an otherwise too-expensive pure rename by capping its cost at 39. Preserve that *specific* vanilla exception with the cap rebased to the configured maximum: a valid rename-only operation above the normal threshold is displayed/charged at that maximum. This is intentionally the sole exception to the formula; document and test it explicitly. A regular rename combined with repair/enchant is NOT exempt.
7. With `preservePriorWorkPenalty=true`, tax/REPAIR_COST metadata calculations remain vanilla. With `false`, exclude accumulated prior-work tax only from the **XP price calculation** while retaining the input/output REPAIR_COST components and vanilla prior-work growth. This flag must not erase or reset item metadata.
8. Vanilla `AnvilMenu.onTake` remains the sole XP removal source, using the same synchronized DataSlot cost displayed on screen. No replacement item/enchantment algorithms. Never modify anvil break chance (12% vanilla), book compatibility logic, enchantment max levels, custom names, durability, or outputs.
9. Servers are authoritative. Clients require Better Anvils for accurate high-cost GUI and pickup behavior. The server sends its current cost policy on login/reload. Dedicated server config is administered on server disk; client menu is read-only/clearly marked 'server controlled' when connected to a remote server. Singleplayer Save updates the integrated server policy safely.
10. The core jar works without optional client GUI libraries. To access the in-game menu, users install matching Mod Menu + Cloth Config builds on the client. Fabric API remains required for networking; no extra datapack.
11. HigherEnchants 26.2 test profile uses the user's legally obtained compatible datapack; never bundle its assets or overwrite `data/minecraft/enchantment/*.json`.
12. No Modrinth publishing or merging to `main` without separate approval.

### Cost examples at defaults

| Vanilla pre-discount XP cost | Effective cost | Normal survival output |
|---:|---:|---|
| 0 | invalid/no cost | No new output created |
| 1 | 1 | Allowed |
| 59 | 30 | Allowed |
| 79 | 40 | Allowed |
| 119 | 60 | Allowed |
| 197 | 99 | Allowed |
| 199 | 100 | Allowed |
| 200 | 100 | Allowed |
| 201 | 101 | Rejected |
| 60 with 0% discount | 60 | Allowed |
| 60 with 75% discount | 15 | Allowed |
| 60 with 100% discount | 1 | Allowed |

**Technical caveat:** the fixture pre-discount prices must be constructed using actual 26.2 enchantments/repair components or explicit test seams; do not assume an arbitrary pair of items can generate every integer.

## 2. Official API and upstream evidence to re-check at implementation start

- Fabric 26.2 migration advisory (Loom 1.17, Loader 0.19.3, Java 25): https://fabricmc.net/2026/06/15/262.html
- Loom no-remap plugin for 26.x: https://docs.fabricmc.net/develop/loom/
- Fabric API (26.2 release): https://www.curseforge.com/minecraft/mc-mods/fabric-api/files/all
- Mod Menu 20.0.3: https://www.curseforge.com/minecraft/mc-mods/modmenu/files/8966634
- Cloth Config 26.2.155: https://www.curseforge.com/minecraft/mc-mods/cloth-config/files/8269699
- GameTest and JUnit: https://docs.fabricmc.net/develop/automatic-testing
- Networking payloads: https://docs.fabricmc.net/develop/networking
- HigherEnchants 1.4 for 26.2: https://modrinth.com/datapack/higherenchants/version/AE9dkSbO
- Too Cheap source/mixins and MIT license: https://github.com/the-sh4d0w/too-cheap

**Verified behavior in reviewed Minecraft 26.2 source:** `AnvilMenu.createResult` computes `tax`, `price` and `finalPrice`, then uses 40 for a stacked-enchantment penalty, 40 for rename-only cap check, 39 for rename-only replacement, and 40 for the normal survival threshold. `AnvilScreen.extractLabels` has a separate 40 check. **Never blindly replace every 40:** the stacked-enchantment penalty must remain 40; only the two threshold checks and the 39 clamp are targets. Before implementation, inspect bytecode in the actual Loom build and pin accurate Mixin injection points; decompiled code and injection ordinals are not a substitute for a real launch test.

## 3. Planned filesystem layout (exact paths)

```text
.github/workflows/build.yml
.gitignore
LICENSE
THIRD_PARTY_NOTICES.md
README.md
build.gradle
gradle.properties
settings.gradle
gradlew
gradlew.bat
gradle/wrapper/gradle-wrapper.jar
gradle/wrapper/gradle-wrapper.properties
src/main/java/io/github/zburgers/betteranvils/BetterAnvils.java
src/main/java/io/github/zburgers/betteranvils/AnvilCostPolicy.java
src/main/java/io/github/zburgers/betteranvils/config/BetterAnvilsConfig.java
src/main/java/io/github/zburgers/betteranvils/config/ConfigService.java
src/main/java/io/github/zburgers/betteranvils/net/AnvilPolicyPayload.java
src/main/java/io/github/zburgers/betteranvils/net/PolicyNetworking.java
src/main/java/io/github/zburgers/betteranvils/mixin/AnvilMenuMixin.java
src/client/java/io/github/zburgers/betteranvils/client/BetterAnvilsClient.java
src/client/java/io/github/zburgers/betteranvils/client/ClientPolicyState.java
src/client/java/io/github/zburgers/betteranvils/client/BetterAnvilsConfigScreen.java
src/client/java/io/github/zburgers/betteranvils/client/ModMenuIntegration.java
src/client/java/io/github/zburgers/betteranvils/mixin/client/AnvilScreenMixin.java
src/main/resources/fabric.mod.json
src/main/resources/better-anvils.mixins.json
src/main/resources/assets/better-anvils/lang/en_us.json
src/main/resources/assets/better-anvils/icon.png
src/test/java/io/github/zburgers/betteranvils/AnvilCostPolicyTest.java
src/test/java/io/github/zburgers/betteranvils/config/ConfigServiceTest.java
src/gametest/java/io/github/zburgers/betteranvils/test/AnvilGameTests.java
src/gametest/java/io/github/zburgers/betteranvils/test/AnvilClientTests.java
src/gametest/resources/fabric.mod.json
docs/testing/acceptance-matrix.md
docs/plans/2026-10-13-better-anvils-26.2.md
```

`gradle-wrapper.jar` is the official Gradle wrapper binary, not hand-written text. Derive it from a trusted generated wrapper and verify its provenance. `icon.png` must be an original asset or license-cleared artwork, and nonessential to core mod logic. Test source sets must not leak into the distributed jar.

## 4. Architecture/decisions

### Flow A: save config from menu

```text
Mod Menu -> registered Better Anvils config factory -> Cloth Config Screen
  -> bind fields to a detached mutable draft
  -> Save -> validate whole draft -> write temp JSON -> atomic replace config file
  -> update local effective policy -> notify integrated server on server thread
  -> close/reopen anvil to force recalculation
Cancel/Esc -> discard draft; Reset Defaults -> update draft only until Save
```

Config service should recover from missing file by generating defaults and should NOT overwrite malformed/out-of-range files silently. Report the error, back up the bad file, and use safe defaults or last-known-good values with a visible warning. No unrestricted config sync from clients to remote servers.

### Flow B: server-calculated anvil operation

```text
Server AnvilMenu.createResult
  -> vanilla validates items, names, enchants and combination compatibility
  -> optional tax adjustment (only prior-work XP component)
  -> apply discount to the computed pre-discount cost, preserving nonzero/invalid guards
  -> vanilla special-case rename cap and normal Too Expensive gate
  -> DataSlot cost + result output synced through vanilla menu packets
  -> vanilla mayPickup checks XP; onTake consumes exactly DataSlot cost, items and anvil wear
```

Use focused Mixin injections: potentially a MixinExtras `@ModifyExpressionValue` around the clamped final price expression, targeted changes to *only* vanilla threshold constants, and (for the prior-work toggle) a dedicated injection at the tax-accumulation seam. Do not overwrite the full `createResult` method. Confirm every injection actually applies with a production game launch; if a stable injection point is unavailable, stop and revise the plan rather than silently allowing invalid or wrong-cost operations.

### Flow C: client/server policy synchronization

Register a versioned clientbound policy payload with Fabric API and send on server join and after server policy refresh. Clients cache the policy for GUI thresholds and local display/prediction; the canonical cost remains the server menu's synced DataSlot. Clear the remote policy on disconnect. Reject unsupported payload/schema versions gracefully; do not accept a client-origin 'change server policy' packet. Ensure no client-only classes load on a dedicated server.

## 5. Shipyard execution tasks

**Convention for every task:** create a focused failing test/check first (RED), implement only its scoped change, verify it (GREEN), inspect the diff, and create one descriptive commit. No parallel work on overlapping files. Record evidence (command, result, commit) in an execution summary only when actually run.

### Task 01: Bootstrap 26.2 Fabric project

**Files to create**

- `build.gradle`
- `gradle.properties`
- `settings.gradle`
- `gradlew`
- `gradlew.bat`
- `gradle/wrapper/gradle-wrapper.properties`
- `gradle/wrapper/gradle-wrapper.jar`
- `src/main/resources/fabric.mod.json`
- `src/main/java/io/github/zburgers/betteranvils/BetterAnvils.java`
- `.gitignore`

**Implementation steps**

1. Record exact Gradle/Loom/Loader/API resolved versions and capture a baseline `./gradlew --version`.
2. Write metadata assertions/verification task that expects mod id `better-anvils`, exact Minecraft 26.2, Java 25, and client/common source sets.
3. Scaffold project using `net.fabricmc.fabric-loom` (NO Yarn remap dependency for 26.2), source-set separation and a no-op initializer.
4. Run `./gradlew clean build --stacktrace` and correct real build errors.
5. Commit `chore: bootstrap Fabric 26.2 project`.

**Verification command**

```bash
./gradlew clean build --stacktrace
```

**Expected result:** exit 0; source jar and main mod jar emitted; no unresolved dependencies


### Task 02: Define validated configuration schema

**Files to create**

- `src/main/java/io/github/zburgers/betteranvils/config/BetterAnvilsConfig.java`
- `src/test/java/io/github/zburgers/betteranvils/config/ConfigServiceTest.java`

**Implementation steps**

1. Write tests asserting exact defaults, upper/lower bounds, `minimum <= maximum`, immutable snapshot/copy behavior, and rejection of NaN/non-integer or JSON type mismatches.
2. Run `./gradlew test --tests '*ConfigServiceTest'` (RED).
3. Implement a typed config DTO and pure validation with field-specific errors.
4. Run the same targeted test (GREEN) and commit `feat: define validated anvil settings`.

**Verification command**

```bash
./gradlew test --tests '*ConfigServiceTest'
```

**Expected result:** exit 0; defaults 100/50/1/true; bad values rejected


### Task 03: Persist JSON safely

**Files to create**

- `src/main/java/io/github/zburgers/betteranvils/config/ConfigService.java`

**Files to modify**

- `src/test/java/io/github/zburgers/betteranvils/config/ConfigServiceTest.java`

**Implementation steps**

1. Write round-trip tests using temporary directories for file-absent defaults, Save, reload, Reset-then-Save, Cancel-no-write, invalid JSON, invalid ranges, and simulated failed writes.
2. Run focused tests (RED); implement Gson-based persistence with UTF-8, temporary-file write and atomic replacement where supported.
3. Keep last valid settings on I/O failure and provide explicit logger/UI warning; never silently corrupt config.
4. Run test (GREEN) and commit `feat: persist anvil settings atomically`.

**Verification command**

```bash
./gradlew test --tests '*ConfigServiceTest'
```

**Expected result:** exit 0; JSON file round-trips; malformed config is handled without data loss


### Task 04: Add pure cost policy with overflow-safe math

**Files to create**

- `src/main/java/io/github/zburgers/betteranvils/AnvilCostPolicy.java`
- `src/test/java/io/github/zburgers/betteranvils/AnvilCostPolicyTest.java`

**Implementation steps**

1. Write parameterized JUnit inputs `(1,1), (59,30), (79,40), (119,60), (197,99), (199,100), (200,100), (201,101)` under 50%; add 0/25/75/100 percent and Integer.MAX_VALUE safety.
2. Run `./gradlew test --tests '*AnvilCostPolicyTest'` (RED).
3. Implement precondition `originalCost > 0` and `max(minCost, (originalCost * (100L - discount) + 99L) / 100L)`, then clamp to integer only after safe arithmetic.
4. Make permit decision `cost <= maximum`; ensure pure math never invents an item/result.
5. Run test (GREEN) and commit `feat: calculate discounted costs and final cap`.

**Verification command**

```bash
./gradlew test --tests '*AnvilCostPolicyTest'
```

**Expected result:** exit 0; exact ceil rounding and inclusive max; no overflow


### Task 05: Hook computed cost without altering vanilla enchant logic

**Files to create**

- `src/main/java/io/github/zburgers/betteranvils/mixin/AnvilMenuMixin.java`
- `src/main/resources/better-anvils.mixins.json`
- `src/gametest/java/io/github/zburgers/betteranvils/test/AnvilGameTests.java`
- `src/gametest/resources/fabric.mod.json`

**Files to modify**

- `src/main/resources/fabric.mod.json`

**Implementation steps**

1. Configure Fabric Loom GameTest source set and a fixture whose vanilla result/cost is known.
2. Write RED GameTests for unchanged valid output components and halved computed XP cost.
3. Examine 26.2 `AnvilMenu.createResult` compiled bytecode; choose stable cost expression injection **before** vanilla 40 check and after vanilla validity determination.
4. Pass the policy snapshot to pure cost helper and alter only its final XP cost; leave `price<=0`, empty result paths and item mutation intact.
5. Run GameTests, inspect Mixin apply logs and commit `feat: discount valid anvil results`.

**Verification command**

```bash
./gradlew tasks --all
```

**Expected result:** exit 0; exposes Loom GameTest tasks; record exact selected task name before running it


### Task 06: Raise survival threshold and preserve rename exception

**Files to modify**

- `src/main/java/io/github/zburgers/betteranvils/mixin/AnvilMenuMixin.java`
- `src/gametest/java/io/github/zburgers/betteranvils/test/AnvilGameTests.java`

**Implementation steps**

1. Write RED GameTests for final cost 99/100 permitted and 101 rejected, creative mode unchanged, and rename-only exemption at configurable maximum.
2. Inspect and target the **two** threshold 40 constants and the rename-only 39 assignment; **do not modify** the separate `price=40` stacked-item penalty.
3. Make threshold `maximum + 1` (safe at supported max 500), and rename-only replacement `maximum`; use constrained injectors with fail-fast apply assertions.
4. Run GameTests and commit `feat: enforce configurable post-discount anvil limit`.

**Verification command**

```bash
./gradlew build
```

**Expected result:** exit 0; GameTests independently prove 100 valid, 101 invalid, rename-only semantics and unmodified penalty


### Task 07: Support optional prior-work XP-tax exclusion

**Files to modify**

- `src/main/java/io/github/zburgers/betteranvils/mixin/AnvilMenuMixin.java`
- `src/gametest/java/io/github/zburgers/betteranvils/test/AnvilGameTests.java`

**Implementation steps**

1. Write tests for default prior-work true = vanilla cost before discount; false = ignore only accumulated XP tax; REPAIR_COST output component and its growth unchanged in both cases.
2. Inspect injection seam specifically around the two initial REPAIR_COST reads used for `tax`, not later reads used for result metadata.
3. Implement a narrow tax expression/local adjustment; if no safe injection exists, block and revise plan rather than rewriting vanilla anvil code.
4. Run GameTests and commit `feat: optionally exclude prior-work XP tax`.

**Verification command**

```bash
./gradlew build
```

**Expected result:** exit 0 plus confirmed GameTests: difference only in XP cost, never in item prior-work metadata


### Task 08: Prove XP charge and vanilla invariants

**Files to modify**

- `src/gametest/java/io/github/zburgers/betteranvils/test/AnvilGameTests.java`

**Implementation steps**

1. Write tests for inadequate XP pickup denial; successful pickup deducts exact displayed level count; rename/repair/book merging; incompatible enchants; no-op; and anvil onTake uses vanilla behavior.
2. Build fixture for survival player; test cost before/after pickup and inventory, output enchantments, item component equality and durability.
3. Only fix focused regressions in existing mixins; do not override `onTake`.
4. Commit `test: verify XP and vanilla anvil invariants`.

**Verification command**

```bash
./gradlew build
```

**Expected result:** exit 0 plus passing game tests for exact XP and output invariants


### Task 09: Synchronize server policy to connected clients

**Files to create**

- `src/main/java/io/github/zburgers/betteranvils/net/AnvilPolicyPayload.java`
- `src/main/java/io/github/zburgers/betteranvils/net/PolicyNetworking.java`
- `src/client/java/io/github/zburgers/betteranvils/client/BetterAnvilsClient.java`
- `src/client/java/io/github/zburgers/betteranvils/client/ClientPolicyState.java`

**Files to modify**

- `src/main/java/io/github/zburgers/betteranvils/BetterAnvils.java`

**Implementation steps**

1. Write codec round-trip tests for values and version/schema rejection. Arrange client/server integration test for login, reconnect, and server refresh.
2. Register Fabric API clientbound payload; send authoritative snapshot on player JOIN and on server policy refresh; register client receiver only in client entrypoint.
3. Update cached session policy on client render/game thread; clear on disconnect; never accept a remote client's settings for server pricing.
4. Test dedicated vs integrated server and commit `feat: synchronize authoritative anvil policy`.

**Verification command**

```bash
./gradlew build
```

**Expected result:** exit 0; client policy equals server after JOIN/reconnect; server jar launches headless without client classes


### Task 10: Fix anvil UI thresholds and client prediction

**Files to create**

- `src/client/java/io/github/zburgers/betteranvils/mixin/client/AnvilScreenMixin.java`
- `src/gametest/java/io/github/zburgers/betteranvils/test/AnvilClientTests.java`

**Files to modify**

- `src/main/resources/better-anvils.mixins.json`

**Implementation steps**

1. Write RED client tests for cost 40 and 100 showing level cost rather than 'Too Expensive'; 101 showing unavailable; insufficient XP text color; client/server config mismatch.
2. Patch only GUI `AnvilScreen.extractLabels` threshold 40 to session server maximum+1; rely on server-authoritative DataSlot for displayed cost.
3. Ensure local client anvil recomputation cannot leave ghost result/pickup after server rejects; if necessary align client createResult with synced policy.
4. Run client GameTests and commit `fix: display authoritative high anvil costs`.

**Verification command**

```bash
./gradlew build
```

**Expected result:** exit 0; client screenshot/assertion shows 100 cost, rejects 101, and matches server during mismatch


### Task 11: Build Mod Menu / Cloth Config settings screen

**Files to create**

- `src/client/java/io/github/zburgers/betteranvils/client/BetterAnvilsConfigScreen.java`
- `src/client/java/io/github/zburgers/betteranvils/client/ModMenuIntegration.java`
- `src/main/resources/assets/better-anvils/lang/en_us.json`

**Files to modify**

- `src/main/resources/fabric.mod.json`
- `src/gametest/java/io/github/zburgers/betteranvils/test/AnvilClientTests.java`

**Implementation steps**

1. Write RED GUI flows: Mod Menu config button exists; controls show saved defaults; slider 50→75; Save writes JSON; Esc/Cancel never saves; Reset only saves on Save; invalid minimum > maximum blocked.
2. Use Mod Menu `ModMenuApi#getModConfigScreenFactory` and Cloth Config `ConfigBuilder`; state is a separate draft until Save. Local singleplayer Save persists and updates integrated server policy; UI tells player to close/reopen anvil.
3. When connected to dedicated server, show server-controlled read-only explanation; no misleading local settings that appear to modify server rules. Gracefully handle missing optional GUI mods without crashing core gameplay.
4. Run GUI tests, manually review 26.2 screen appearance/accessibility and commit `feat: add automatic in-game config editor`.

**Verification command**

```bash
./gradlew build
```

**Expected result:** exit 0; GUI Save persists, Cancel discards; singleplayer behavior changes after reopening anvil


### Task 12: HigherEnchants and clean-profile compatibility

**Files to create**

- `docs/testing/acceptance-matrix.md`

**Files to modify**

- `src/gametest/java/io/github/zburgers/betteranvils/test/AnvilGameTests.java`

**Implementation steps**

1. Use independent 26.2 profiles: vanilla/no datapack and 26.2 HigherEnchants 1.4 enabled and reloaded from its separate public distribution.
2. Test enchanted books combinations, extra levels, newly compatible enchantment pairs, armor/weapons/tools, repaired high-enchant equipment and denied incompatible pairs.
3. Inspect produced jar contents for absence of `data/minecraft/enchantment/` and HigherEnchants assets; do not alter registry/data-driven max levels.
4. Record actual PASS/FAIL/SKIPPED evidence for both profiles; commit `test: verify HigherEnchants compatibility`.

**Verification command**

```bash
jar tf build/libs/better-anvils-*.jar
```

**Expected result:** no bundled HigherEnchants files, no overwritten enchantment JSON; actual play tests recorded separately


### Task 13: Packaging, assets, licensing and installation docs

**Files to create**

- `LICENSE`
- `THIRD_PARTY_NOTICES.md`
- `src/main/resources/assets/better-anvils/icon.png`

**Files to modify**

- `README.md`
- `src/main/resources/fabric.mod.json`

**Implementation steps**

1. Document server+client installation, Java 25/Fabric 26.2/required Fabric API, optional Mod Menu + Cloth Config, exact menu path and JSON path, dedicated-server management/restart, default/valid ranges, conflict caveats.
2. Include MIT attribution/license notice for upstream Too Cheap where code/hooks are adapted. Do not copy upstream branding/icon or redistributable HigherEnchants data.
3. Validate resource paths and translation strings; use original icon asset (or defer icon if artwork isn't licensed), ensure jar and -sources jar are clearly separated in README.
4. Commit `docs: document Better Anvils install and provenance`.

**Verification command**

```bash
./gradlew clean build
```

**Expected result:** exit 0; main jar fabric.mod.json valid; licenses/translation assets included, no datapack


### Task 14: Build CI with executable evidence

**Files to create**

- `.github/workflows/build.yml`

**Files to modify**

- `build.gradle`
- `docs/testing/acceptance-matrix.md`

**Implementation steps**

1. Configure push/PR GitHub Actions on Java 25, Gradle wrapper validation, `./gradlew clean test build`, game test launch tasks (discover names with `./gradlew tasks --all`), server smoke start, and client smoke where CI display runner permits.
2. Upload test reports, logs and the single distributable jar as build artifacts; never report skipped manual tests as passed.
3. Add separate job or matrix smoke with/without Mod Menu/Cloth Config to ensure optional-dependency safety and no server-side client imports.
4. Commit `ci: verify build and in-game compatibility`.

**Verification command**

```bash
./gradlew clean test build --stacktrace
```

**Expected result:** exit 0 locally AND green GitHub Actions for same SHA, with stored reports/artifact


### Task 15: Release-candidate review and guarded handoff

**Files to modify**

- `docs/testing/acceptance-matrix.md`
- `README.md`

**Implementation steps**

1. Inspect entire diff against this plan; recheck requirement traceability, licence, artifact contents, mixin targets, invalid combos, multiplayer sync and no client classes on server.
2. Actually run server startup, singleplayer client, LAN/dedicated client, config Save→restart persistence, HigherEnchants profile and clean profile. Capture versions, game logs, screenshot and results.
3. Perform independent code review and fix all launch-blocking/high-severity findings; rerun full suite and ensure CI's green commit SHA equals reviewed branch HEAD.
4. Push normal fast-forward commits to `26.2-better-anvils`; verify SHA remotely. Produce the jar filename, test matrix and installation instructions. Make a PR if desired, but do not merge/release without approval.

**Verification command**

```bash
git rev-parse HEAD && git ls-remote origin refs/heads/26.2-better-anvils
```

**Expected result:** both full SHA values match; actual executable jar built; all mandatory tests PASS


## 6. Test, review, and release gates

**Unit gate:** 50% integer-ceiling cases, all config bounds, config file crash safety, JSON round-trip, 0% and 100% discounts, overflow near Integer.MAX_VALUE, maximum exactly 100 inclusive.

**Game/server gate:** no-op/incompatible combination still invalid; prior-work penalty respects toggle without changing metadata; insufficient levels cannot take; vanilla `onTake` deducts exactly synchronized cost; repair, naming, durability, anvil wear and creative bypass all stable.

**Client gate:** 'Too Expensive' at 101 and not at 100; client receives server policy; Save/Cancel/Reset semantics; no config writes on client connected to remote server; dedicated server boots without Mod Menu, Cloth Config or any client class.

**Compatibility gate:** vanilla profile plus legally installed HigherEnchants 26.2; dynamic enchantment maxima and compatibility; no overwritten vanilla enchant JSON; no conflict with `too-cheap` (add explicit `breaks`/clear conflict diagnosis for that known competing mod); warn that other anvil-overhaul mods are unsupported unless separately tested.

**CI gate:** deterministic Gradle build from clean checkout, tests, GameTests, server smoke, optional client smoke, jar inspection, exact SHA verification. Manual multiplayer game tests must be marked manual; absence of real test evidence is BLOCKED/NOT RUN, never PASS.

**Release gate:** all critical tests PASS, CI green, no hidden config divergence, license obligations met, correct Java/Loader/API and mod-id metadata, one distributable Better Anvils jar. Explicit owner authorization needed before merge or Modrinth publication.

## 7. Evidence and status ledger

| Gate | At planning time |
|---|---|
| Existing GitHub repository verified | PASS (Zburgers/better-anvils) |
| Branch `26.2-better-anvils` created | PASS |
| Upstream MIT source reviewed | PASS (source reference, not imported code) |
| Current dependency versions researched | PASS for candidate versions, final Gradle resolution pending |
| Source implemented | NOT STARTED |
| Build/jar generated | NOT RUN |
| Unit tests | NOT RUN |
| GameTests | NOT RUN |
| GUI smoke test | NOT RUN |
| Dedicated multiplayer synchronization | NOT RUN |
| HigherEnchants gameplay compatibility | NOT RUN |
| GitHub CI green | NOT RUN |
| Modrinth release | NOT AUTHORIZED |

The plan is intentionally an approval checkpoint. Begin task 01 only when the owner authorizes implementation.

## 8. Execution feedback rules

- Log a failed task's exact error and fix it before continuing. Never skip to release because the code looks right.
- If Loom names/API or target `AnvilMenu` bytecode differ from this document, record the finding and update the plan before a breaking architectural change.
- Keep every commit narrow and reversible. Never force push, touch the Too Cheap upstream repo, overwrite `main`, or publish Modrinth without approval.
- If a phase cannot be exercised in this environment (e.g., graphical client), mark it NOT RUN and provide the exact manual verification procedure. No unverified claims.

## 9. Distinct references

This plan follows Shipyard's publicly documented writing-plans pattern: file-specific tasks, RED/GREEN checks, readable Markdown task sections, explicit expected command results, incremental commits, dependency sequencing and acceptance gates. The Shipyard runner itself is not installed as a native tool in this environment; this file is a self-contained plan for later execution.

Plan background: https://www.claudepluginhub.com/skills/lgbarn-shipyard-2/shipyard-writing-plans
