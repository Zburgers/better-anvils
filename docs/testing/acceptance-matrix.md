# Better Anvils 26.2 — Acceptance & Verification Matrix

**Status:** PLANNED / NOT RUN. This matrix defines *future tests*. Change a row to PASS only after running it and recording its proof (logs, CI run URL, screenshot, or reproducible steps), tester, environment, and commit SHA.

**Target:** Minecraft 26.2 / Java 25 / Fabric Loader >=0.19.3 / Fabric API 0.161.0+26.2.
**Development branch:** `26.2-better-anvils`.
**Test code locations:** `src/test/java/io/github/zburgers/betteranvils/`, `src/gametest/java/io/github/zburgers/betteranvils/test/`.
**Plan:** [Master implementation plan](../plans/2026-10-13-better-anvils-26.2.md).

## Required machine setups

- **A: Clean singleplayer:** Fabric 26.2, Fabric API, Better Anvils jar; no Mod Menu, no Cloth Config, no datapack. Client launches, game operations work and no optional GUI class causes a crash.
- **B: Menu singleplayer:** same as A plus Mod Menu 20.0.3 and Cloth Config Fabric 26.2.155; test menu Save/Cancel/Reset and integrated server policy update.
- **C: Dedicated multiplayer:** 26.2 Fabric server + Better Anvils + Fabric API; two matching 26.2 Fabric clients with Better Anvils (one with optional GUI dependencies). Server settings distinct from local client preferences.
- **D: HigherEnchants:** B and C variants with HigherEnchants 1.4 26.2 loaded as an *externally acquired* datapack. No redistribution or vanilla enchant JSON overrides.
- **E: Conflict detection:** known conflicting Too Cheap mod installed alongside Better Anvils; expected clear incompatibility error or documented preventive failure, not silent double discounts.

## Unit checks (JUnit / Fabric Loader JUnit)

| ID | Requirement | Setup / Action | Expected result | Status |
|---|---|---|---|---|
| U01 | Defaults | No JSON present | Generated config: 100,50,1,true | NOT RUN |
| U02 | Discount rounding | Input pre-cost 1 at 50% | 1 | NOT RUN |
| U03 | 30 level result | Pre-cost 59 at 50% | 30 | NOT RUN |
| U04 | 40 level result | Pre-cost 79 at 50% | 40 | NOT RUN |
| U05 | 60 level result | Pre-cost 119 at 50% | 60 | NOT RUN |
| U06 | 99 level result | Pre-cost 197 at 50% | 99 | NOT RUN |
| U07 | 100 level result | Pre-cost 199 at 50% | 100 allowed | NOT RUN |
| U08 | 101 level result | Pre-cost 201 at 50% | 101 rejected | NOT RUN |
| U09 | Full-discount floor | Pre-cost 60 at 100% | 1 | NOT RUN |
| U10 | No discount | Pre-cost 60 at 0% | 60 | NOT RUN |
| U11 | Alternate discount | Pre-cost 60 at 75% | 15 | NOT RUN |
| U12 | No operation | Original 0 / invalid output | Not promoted to a valid operation | NOT RUN |
| U13 | Overflow | Integer.MAX_VALUE pre-cost at 0,50,100% | no arithmetic overflow; bounded result | NOT RUN |
| U14 | Validate bounds | discount -1,101; max 0,501; min 0,11; min>max | Save rejected; descriptive error | NOT RUN |
| U15 | Safe file persistence | Save, close/reopen | Exact values round-trip | NOT RUN |
| U16 | Malformed config recovery | Invalid JSON on startup | Warn, backup invalid file, use defaults/last good, no crash | NOT RUN |
| U17 | Failed write | Read-only config directory | No partial/corrupt replaced config; error reported | NOT RUN |
| U18 | Payload codec | Encode/decode configured values | Exact values restored; invalid protocol version rejected | NOT RUN |

## Game logic tests (actual 26.2 runtime)

| ID | Requirement | Setup / Action | Expected result | Status |
|---|---|---|---|---|
| G01 | Inclusive max | Valid normal operation after discount costs 100 | Output selectable with sufficient XP | NOT RUN |
| G02 | Above max | Same kind of normal operation costs 101 | Output unavailable and no XP taken | NOT RUN |
| G03 | XP shortage | Player has 99 levels, cost is 100 | Pickup denied, input unaffected | NOT RUN |
| G04 | Exact charge | Player has 125 levels, cost is 100 | Remaining 25, items consumed exactly once | NOT RUN |
| G05 | Creative unaffected | Creative player + expensive valid operation | Vanilla creative permission intact | NOT RUN |
| G06 | Invalid incompatible pair | Only incompatible enchants, no valid combined output | No result | NOT RUN |
| G07 | Mixed enchantments | Some compatible and some incompatible | Matches vanilla accepted subset and price rules | NOT RUN |
| G08 | Unchanged item metadata | Same valid operation vs vanilla control | Same enchantments/name/durability/REPAIR_COST output | NOT RUN |
| G09 | Repair material | Damageable tool + allowed material | Vanilla repair amount and material consumption | NOT RUN |
| G10 | Item merge | Two compatible damageable items | Vanilla combined durability behavior | NOT RUN |
| G11 | Rename only | High-tax rename, above normal cap | Vanilla rename exemption re-based to configured max | NOT RUN |
| G12 | Rename with another change | Rename + enchant or repair above cap | No rename-only exemption | NOT RUN |
| G13 | Prior-work ON | Existing repair penalty, default flag true | Vanilla pre-cost tax, discounted afterward | NOT RUN |
| G14 | Prior-work OFF | Same input, false | Prior-work XP tax ignored; metadata preserved/increases normally | NOT RUN |
| G15 | Anvil break behavior | Repeated vanilla pickup operations | Same vanilla 12% RNG event condition; no modded break hook | NOT RUN |
| G16 | Vanilla stacked-item penalty | Enchanted stack handling where vanilla writes price 40 | Remains 40 before discounts; not replaced by 101 | NOT RUN |
| G17 | No-op/empty | Missing inputs, unchanged rename | No forged result from discount mixin | NOT RUN |

## UI and file persistence tests

| ID | Requirement | Action | Expected result | Status |
|---|---|---|---|---|
| C01 | Menu discoverability | Main menu → Mods → Better Anvils → Configure | Screen opens with current settings | NOT RUN |
| C02 | In-game access | Esc/pause → Mods → Better Anvils → Configure | Screen opens from gameplay if Mod Menu exposes Mods | NOT RUN |
| C03 | Default presentation | Open config after first install | 50%, 100, 1, true visible | NOT RUN |
| C04 | Save updates JSON | Change to 75%, Save | `config/better-anvils.json` contains 75, no manual editing | NOT RUN |
| C05 | Apply local settings | Save, close/reopen anvil | New anvil cost uses 75% on integrated server | NOT RUN |
| C06 | Cancel preserves JSON | Change 50→75 then Esc/Cancel | File and active cost remain 50% | NOT RUN |
| C07 | Reset defaults | Change entries, Reset, Save | 100/50/1/true restored in JSON and UI | NOT RUN |
| C08 | Invalid combo | Set min larger than max | Save disabled/validation error; file untouched | NOT RUN |
| C09 | Optional dependencies missing | Setup A without Mod Menu/Cloth | Game loads; anvil modification works | NOT RUN |
| C10 | Dedicated UI protection | Open client menu connected to setup C | Server-controlled values read-only; no local Save impersonates server config | NOT RUN |
| C11 | Reopen persistence | Restart Minecraft after Save | Same configuration shown | NOT RUN |
| C12 | GUI max cost | Cost 100, sufficient levels | Screen displays 100 and not 'Too Expensive' | NOT RUN |
| C13 | GUI over max | Cost 101 | Screen shows too-expensive/unavailable condition correctly | NOT RUN |

## Network and headless server tests

| ID | Requirement | Action | Expected result | Status |
|---|---|---|---|---|
| N01 | Dedicated server boot | Launch setup C headlessly | No `net.minecraft.client` linkage, mixin or absent GUI library crash | NOT RUN |
| N02 | On-join sync | Server policy 75%, client JSON 50% | Client display and prediction use server 75%; server deducts 75%-policy cost | NOT RUN |
| N03 | Rejoin refresh | Client leaves server C and joins different policy | No stale policy carries over | NOT RUN |
| N04 | Two clients | Two connected players with unlike local configs | Identical canonical server charge for same work | NOT RUN |
| N05 | Server config restart | Change server JSON then restart | New policy reaches clients; old config not authoritative | NOT RUN |
| N06 | Race safety | Change integrated config while anvil opened | No duping/XP exploit; close/reopen anvil applies settings | NOT RUN |
| N07 | Unauthorized packet | Player attempts local policy override | Server ignores/rejects; no config/XP rule changed | NOT RUN |
| N08 | Protocol mismatch | Mismatched packet schema | Graceful disconnect or explicit compatibility error; no silent desync | NOT RUN |

## HigherEnchants tests

| ID | Requirement | Action | Expected result | Status |
|---|---|---|---|---|
| H01 | Baseline no datapack | Setup A, standard books/tool | Vanilla enchant level caps respected; pricing mod works | NOT RUN |
| H02 | Datapack loaded | Setup D, verify dynamic definitions with `/datapack list` | Correct external pack enabled, no bundled files | NOT RUN |
| H03 | High-level books | Combine HigherEnchants enchanted books | Levels up to pack limit preserved; correct discount | NOT RUN |
| H04 | High-level equipment | Apply books to armor/weapon/tool | Dynamically configured levels apply | NOT RUN |
| H05 | Modified compatibility | Combine pack-compatible enchantments | Datapack compatibility recognized | NOT RUN |
| H06 | Forbidden combinations | Try pair still incompatible with pack | Vanilla/data-driven rejection remains | NOT RUN |
| H07 | Equipment repairs | Repair already highly enchanted gear | Enchant levels, item components and durability preserved | NOT RUN |
| H08 | Packaging | Inspect built jar entries | No HigherEnchants or `data/minecraft/enchantment/` files | NOT RUN |

## CI and release checks

| ID | Requirement | Evidence | Expected result | Status |
|---|---|---|---|---|
| P01 | Clean Gradle build | `./gradlew clean build --stacktrace` | exit 0, correct jars | NOT RUN |
| P02 | JUnit suite | `./gradlew test` | exit 0, all required U tests PASS | NOT RUN |
| P03 | GameTest | Actual task name from `./gradlew tasks --all` | required G tests PASS | NOT RUN |
| P04 | Client GameTest | Headless XVFB/real Minecraft client | required C tests PASS | NOT RUN |
| P05 | GUI review | Screenshots in 26.2 and human confirmation | readable, no overlap/clipping | NOT RUN |
| P06 | Jar inspect | `jar tf build/libs/*.jar` | correct metadata/resources, no unwanted datapack | NOT RUN |
| P07 | Conflict check | With Too Cheap installed | clear incompatible-mod detection, no stacked modification | NOT RUN |
| P08 | GitHub CI | GitHub Actions linked to branch commit | all required jobs green | NOT RUN |
| P09 | Remote SHA | compare local HEAD to `git ls-remote` | identical complete commit SHA | NOT RUN |
| P10 | Release restraint | GitHub/Modrinth audit | no publication/merge without permission | NOT RUN |

## Execution record template

For each test that is run, append or link a record using this format:

```text
Test ID:
Commit SHA:
Minecraft / Java / Loader / API / Mod Menu / Cloth versions:
Profile: A / B / C / D / E
Execution date:
Run command or exact manual steps:
Result: PASS / FAIL / BLOCKED / NOT RUN
Evidence: CI URL, log filepath, screenshot, or short output
Notes and issue/commit resolving failures:
```

**No outcome in this file has been measured as of its creation.** A passing unit suite alone does not prove the entire game runs; real client/server and datapack compatibility testing are mandatory before a release-ready claim.
