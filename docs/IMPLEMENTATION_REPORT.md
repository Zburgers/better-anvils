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

The step-by-step local results and outstanding acceptance cases are in the [acceptance matrix](testing/acceptance-matrix.md). Local logs live under `/tmp` and are not committed. Remote GitHub Actions and remote SHA verification require the branch push and remain pending.

## Remaining qualification

The implementation has not been exercised with two networked clients, a dedicated server/client configuration split, or Too Cheap installed. Several edge-case GameTests and menu navigation checks also remain marked NOT RUN in the matrix. No merge or release publication was performed.
