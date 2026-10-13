# Better Anvils 26.2 Implementation Report

**Branch:** `26.2-better-anvils`  
**Locally verified source tree:** `ffe6b57bda7f4783844fd7e53c2021499c7b0a59`  
**Runtime:** Minecraft 26.2, Java 25, Fabric Loader 0.19.3, Fabric API 0.161.0+26.2, Gradle wrapper 9.5.1.

## Delivered

- Vanilla anvil operations remain responsible for validity, output items, materials, and wear. Valid XP prices use a validated configurable discount and minimum; the configured maximum controls the high-cost threshold and rename-only exception.
- Server configuration is authoritative and synchronized to clients. The optional Mod Menu and Cloth Config integration edits and saves settings; connected clients see server values as read-only.
- Defaults are 100 maximum levels, 50% discount, 1 minimum level, and preserving the prior-work penalty. Configuration is stored in `config/better-anvils.json`.
- The repository includes the mod icon and real client screenshots for a 100-level anvil result, the default settings screen, and invalid-setting validation. The README links those assets and documents installation, settings, compatibility, and local builds.
- MIT licensing, third-party notices, and a Java 25 GitHub Actions workflow are included. The jar does not bundle HigherEnchants or optional GUI mods.

## Verification

The clean build and client test tree is committed as `ffe6b57bda7f4783844fd7e53c2021499c7b0a59`. On 2026-10-13:

- `./gradlew clean test build --stacktrace --console plain` passed: 32 JUnit tests and 16 vanilla GameTests.
- `ALSOFT_DRIVERS=null xvfb-run -a ./gradlew runClientGameTest --console plain` passed. It exercised UI defaults, draft/cancel, save and integrated-server update, resets, persistence, and validation. Screenshots were reviewed and copied into `docs/images/`.
- A dedicated server startup reached `Done` with optional GUI dependencies absent. A client smoke without Mod Menu and Cloth Config reached the main menu and loaded Better Anvils resources.
- HigherEnchants 1.4 for 26.2 passed the compatibility profile: dynamic Sharpness VIII and pack-defined Sharpness/Smite compatibility were preserved. The externally acquired datapack was used only as a temporary local fixture and is not included in this repository.
- `actionlint .github/workflows/build.yml`, jar inspection, and `git diff --check` passed.
- [GitHub Actions run 38410238219](https://github.com/Zburgers/better-anvils/actions/runs/38410238219) passed all steps on implementation SHA `e298a20067c3854ed40912fe5786242da3fc10b3`, including the unit/GameTest build, client GameTests, dedicated-server startup, client startup without optional GUI mods, and artifact upload.

The step-by-step local results and outstanding acceptance cases are in the [acceptance matrix](testing/acceptance-matrix.md). Local logs live under `/tmp` and are not committed. The implementation SHA above was pushed and matched the remote branch; this later report-only update does not change source or workflow code.

## Remaining qualification

The implementation has not been exercised with two networked clients, a dedicated server/client configuration split, or Too Cheap installed. Several edge-case GameTests and menu navigation checks also remain marked NOT RUN in the matrix. No merge or release publication was performed.

## A. Implementation summary and deviations

Minecraft's vanilla `AnvilMenu` still computes and applies the result. Narrow Mixin hooks discount the valid calculated price, adjust only the two intended high-cost thresholds and rename-only clamp, and optionally remove the prior-work XP tax without replacing vanilla item metadata handling. Validated settings load from and save atomically to JSON. The client settings screen is draft-based; an integrated server receives saved policy, while remote server policy is read-only and server-controlled.

The 26.2 Loom plugin uses the game's Mojang names in its non-obfuscated environment, so the build omits a separate mappings dependency. This was documented in the plan after confirming the actual plugin behavior. The project wrapper is pinned at Gradle 9.5.1 for the selected Loom setup; it was used instead of the host's Gradle 9.8.1. Mod Menu and Cloth Config are runtime-optional, and `-PwithoutOptionalGui` provides the core client smoke profile.

## B. Task completion matrix

| Task | Status | Commit(s) | Evidence and remaining checks |
| --- | --- | --- | --- |
| 01 Bootstrap Fabric 26.2 | Complete | `30ff92f` | Metadata/build setup; clean build passes. |
| 02 Validated config schema | Complete | `d5bb0ca` | Defaults, ranges, invalid values tested. |
| 03 Safe JSON persistence | Complete | `e6b9d8a` | Round-trip, malformed-file backup, and failed-write behavior tested. |
| 04 Discount policy | Complete | `02ccacf` | Rounding, bounds, alternate discounts, and integer overflow covered. |
| 05 Discount valid anvil result | Complete | `48209f6` | Actual 26.2 GameTests exercise vanilla result path. |
| 06 Configurable cap and rename exception | Complete | `ebdf798` | 100 allowed, 101 rejected, rename-only exception, stacked penalty preserved. |
| 07 Prior-work tax option | Complete | `5bd47cf` | XP tax toggle and repair-cost metadata checks pass. |
| 08 XP and vanilla invariants | Partial qualification | `00eab9f` | XP shortage/charge, creative, repair, merge, invalid/no-op covered; mixed enchant case and repeated anvil-wear test not run. |
| 09 Server policy sync | Partial qualification | `76a4421` | Payload validation and integrated-server policy update pass; reconnect, two-client, dedicated policy split, and race tests not run. |
| 10 Client cost labels | Partial qualification | `db8c66e` | 100-level display passes; GUI over-cap display case not run. |
| 11 Config screen | Partial qualification | `eb5849f`, `7b2691c` | Save, cancel, reset, invalid draft, JSON write and integrated update tested; Mod Menu navigation and restart persistence not run. |
| 12 HigherEnchants profile | Partial qualification | `c1aca75` | Dynamic Sharpness VIII and pack-defined compatible pair pass; a forbidden pair and repair of high-level gear not run. |
| 13 Packaging, licensing, assets, docs | Complete | `20ec1e6`, `9873ddf` | Icon, screenshots, license, notices, install docs, and README instructions are present. |
| 14 CI and artifact | Complete | `ffe6b57` | Java 25 workflow passed on `e298a20067c3854ed40912fe5786242da3fc10b3`; uploads jar, reports, logs and screenshots. |
| 15 Final review and guarded handoff | Partial qualification | `ee184f9`, `9873ddf`, `e298a20` | Source review, acceptance record, normal push, remote SHA verification and green CI completed; manual multiplayer, restart, and conflict gates remain open. |

## C. Build evidence

| Check | Command/profile | Result |
| --- | --- | --- |
| Clean build, JUnit, vanilla GameTests | `./gradlew clean test build --stacktrace --console plain` | PASS; 32 JUnit and 16 GameTests. |
| Client GameTests | `ALSOFT_DRIVERS=null xvfb-run -a ./gradlew runClientGameTest --console plain` | PASS; settings and anvil display assertions. |
| HigherEnchants | `JAVA_TOOL_OPTIONS=-DbetterAnvils.requireHigherEnchants=true ./gradlew runGameTest` with external HigherEnchants 1.4 for 26.2 | PASS; 16 required tests. The datapack fixture was removed after the test and is not tracked. |
| Dedicated server | `./gradlew runServer --console plain` with temporary local EULA acceptance | PASS; Better Anvils initialized and server reached `Done`. The local EULA file was restored to `eula=false`. |
| Core client without optional GUI mods | `ALSOFT_DRIVERS=null xvfb-run -a ./gradlew -PwithoutOptionalGui runClient` | PASS; initialized, loaded resources and reached the main menu. |
| Workflow lint and diff hygiene | `actionlint .github/workflows/build.yml`; `git diff --check` | PASS. |

Build runtime versions: Java 25; Minecraft 26.2; Fabric Loader 0.19.3; Fabric API 0.161.0+26.2; Gradle wrapper 9.5.1; Mod Menu 20.0.3 and Cloth Config 26.2.155 for UI runs.

The local distributable is `build/libs/better-anvils-1.0.0.jar` (1,142,550 bytes; SHA-256 `699e3ee735570920e8f6e7f69ff5fd1176407e2f11565be2ce7d5d197fc12718`). It contains the mod metadata, common/client classes, icon and English strings; it does not bundle HigherEnchants data, Mod Menu or Cloth Config classes, or Minecraft client classes. The sources jar is separate. The GitHub Actions artifact is available from the [green workflow run](https://github.com/Zburgers/better-anvils/actions/runs/38410238219) under artifact `better-anvils-e298a20067c3854ed40912fe5786242da3fc10b3`.

## D. Test evidence and distinctions

The [acceptance matrix](testing/acceptance-matrix.md) is the detailed per-case record. Unit tests, vanilla GameTests, client GUI tests, the local server/client startup checks, the HigherEnchants profile, jar inspection and CI have PASS evidence. Integrated-server policy update passed; this is not a dedicated multiplayer test. The GitHub workflow run above passed build/tests, optional-mod client GameTests, dedicated-server startup, core client startup and artifact upload.

NOT RUN includes real two-client multiplayer; reconnect and server restart synchronization; remote-server read-only GUI interaction; Save persistence across a client restart; client display at 101; mixed compatible/incompatible enchantments; rename plus another operation; repeated anvil wear; HigherEnchants forbidden-pair and high-level repair cases; and Too Cheap installed beside the mod. Those outcomes are not inferred from unit tests or a successful singleplayer run.

## E. Outstanding defects and release blockers

No launch-blocking defect was found in the local source review. The tests above do not establish the multiplayer and compatibility behavior still marked NOT RUN. The release-readiness assessment is **NOT RELEASE-READY** until the multiplayer/server-authority, reconnect/restart, Too Cheap conflict, and remaining gameplay/UI acceptance cases are exercised and recorded. The mod is declared incompatible with Too Cheap in metadata, but that failure path has not been tested with Too Cheap installed.

## F. Final delivery information

- Repository: [Zburgers/better-anvils](https://github.com/Zburgers/better-anvils)
- Development branch: [`26.2-better-anvils`](https://github.com/Zburgers/better-anvils/tree/26.2-better-anvils)
- Full implementation, README, and green-CI SHA recorded here: `e298a20067c3854ed40912fe5786242da3fc10b3`. This handoff report is committed as a following documentation-only update.
- Jar: `better-anvils-1.0.0.jar`; local path `build/libs/better-anvils-1.0.0.jar`; CI artifact linked above.
- GitHub Actions: [run 38410238219 — success](https://github.com/Zburgers/better-anvils/actions/runs/38410238219).
- Required: Minecraft Java 26.2, Fabric Loader 0.19.3+, Fabric API 0.161.0+26.2, Java 25. Mod Menu 20.0.3 and Cloth Config 26.2.155 are optional client UI dependencies.
- Install: build with `./gradlew build`, place the non-sources jar in `mods`, and install Better Anvils plus Fabric API on the server and each multiplayer client. See [README](../README.md) for settings and dedicated-server instructions.
- Merge/release: not performed. Release readiness remains blocked by the qualification gaps above.

## G. Reviewer handoff

Review these files first:

1. `src/main/java/io/github/zburgers/betteranvils/mixin/AnvilMenuMixin.java` — price expression, targeted 40/39 constant ordinals, prior-work tax seam.
2. `src/main/java/io/github/zburgers/betteranvils/AnvilCostPolicy.java` and `config/BetterAnvilsConfig.java` — ceiling math, bounds and inclusive maximum.
3. `src/main/java/io/github/zburgers/betteranvils/net/PolicyNetworking.java`, `net/AnvilPolicyPayload.java`, and `src/client/java/io/github/zburgers/betteranvils/client/ClientPolicyState.java` — versioned server policy and disconnect behavior.
4. `src/client/java/io/github/zburgers/betteranvils/client/BetterAnvilsConfigScreen.java` and `ModMenuIntegration.java` — draft/save/cancel and optional dependency behavior.
5. `src/gametest/java/io/github/zburgers/betteranvils/test/AnvilGameTests.java` and `AnvilClientTests.java` — actual game invariants and the exact limits of current coverage.
6. `build.gradle`, `.github/workflows/build.yml`, `fabric.mod.json`, and `THIRD_PARTY_NOTICES.md` — source-set isolation, CI, compatibility declaration and packaging provenance.

Key decisions: preserve vanilla result generation; change only the calculated price and verified threshold expressions; make server policy authoritative; keep UI libraries optional; do not bundle HigherEnchants or overwrite vanilla enchantment data. The self-review did not replace the outstanding independent multiplayer/conflict qualification.
