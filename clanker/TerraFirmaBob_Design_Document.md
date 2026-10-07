# TerraFirmaBob - Design Document

**A bridge mod unifying TerraFirmaCraft/TFC+ and HBM's Nuclear Tech Mod / NTM: Space on Minecraft 1.7.10**

Version 1.0 - compiled 2026-10-08
Status: fact-checked design proposal

---

## Part I - Executive Summary

TerraFirmaCraft (TFC/TFC+) is a total-conversion geology-and-survival simulator that replaces vanilla Minecraft's world, resources, and survival systems. HBM's Nuclear Tech Mod (NTM) is an industrial-military sandbox that silently assumes vanilla's world, resources, and progression exist. Installed together without a bridge, whole gameplay loops on both sides become non-functional. Config alone cannot fix this; a dedicated bridge mod is the only path from "two mods in a folder" to one coherent game.

This document:

1. **Verifies** the claims in the underlying research against primary sources (Part II), including two corrections and one newly discovered piece of prior art.
2. **Catalogs** all 30 direct incompatibilities in six tiers, with corrections applied inline (Part III).
3. **Specifies** the bridge mod's architecture and per-system design decisions (Part IV).
4. **Lays out** a phased implementation roadmap with milestones, test plan, and risk register (Part V).

One architectural fact governs everything else: **NTM: Space is a standalone fork of NTM, not an addon** - both register the same mod ID (`hbm`) and cannot coexist - so the bridge must treat NTM and NTM: Space as mutually exclusive dependency targets.

---

## Part II - Verification Report

Every load-bearing claim in the source analysis was checked against primary sources (official READMEs, wikis, GitHub issues, mod pages). Results:

### Verified as stated

| # | Claim | Source |
|---|-------|--------|
| V1 | NTM: Space is a **fork** of 1.7.10 NTM (by JameH2 and Mellow), not an addon; kept within ~24h of upstream | NTM wiki "Versions & Forks"; Modrinth/CurseForge pages |
| V2 | NTM README ships kill-switch configs `1.31_enableSkyboxes` (skybox chainloader) and `1.32_enableImpactWorldProvider` (Tom-impact world provider) | NTM GitHub README, "Compatibility notice" |
| V3 | NTM deliberately crashes on Thermos-base servers; README also documents shader breakage with guns | NTM GitHub README |
| V4 | Hamster-Systems issue #37 (TFC integration): *"they do not cover the entire list of ores, and most importantly, there is no oil in the world at all"* - and the same reply notes *"fresh water is also water and boilers could use it"* | github.com/Hamster-Systems/Hbm-s-Nuclear-Tech-GIT/issues/37 |
| V5 | EndlessIDs issue #199: NTM: Space nuclear-dust/fallout (`EntityFalloutRain` → `WorldUtil.setBiome`) accessed the vanilla byte biome array and crashed under extended-biome-ID mods | github.com/GTMEGA/EndlessIDs/issues/199 |
| V6 | TheVoidedOnes' **TFC Nuclear Tech Addon** (1.12.2) bridges NTM CE + TFC: recipe transfer to TFC rocks/ores, TFC-style metallurgy, fully custom ore generation, optional NTM Space support, MixinBooter dependency | Addon README on GitHub/CurseForge/Modrinth |
| V7 | **KirbCraft** (Modrinth, 1.7.10): *"Combines TFC+ with NTM space along with performance boosts"* - seasons, thirst, radiation, new ore generation, oil processing, custom rockets | modrinth.com/modpack/kirbcraft |
| V8 | NTM: Space has a **Martian world type** for "Mark Watney style survival"; other planets are custom dimensions reached by rockets | nucleartech.wiki/wiki/NTM:_Space |
| V9 | NTM: Space teleporters require **N-MASS(II) Driver Fuel** for cross-dimension operation (16,000 mB buffer) | nucleartech.wiki/wiki/NTM:_Space and /Teleporter |
| V10 | TNFC_Mod (AnodeCathode) is the glue mod for TechNodeFirmaCraft; TerraFirmaPunk is a shipped 1.7.10 TFC+tech pack | GitHub; CurseForge/FTB forum |
| V11 | TFC's fresh/salt water split breaks tech-mod boilers in practice: TerraFirmaPunk's dev log - *"the steam boilers and steam engines do not recognize TFC water in any form"* | FTB forum TerraFirmaPunk thread |

### Corrections to the source document

- **C1 - TFC sea level is y=145, not "~y144".** The TFC wiki's Altitude page states sea level moved "from y=62 to y=145." The original text's "~y144 seas / ~y130 ground" is directionally right (roughly double vanilla height) but the canonical figure is **145**. (Applied to incompatibility #9.)
- **C2 - The mixin quote is misattributed.** *"If your addon doesn't clash mixin functionality with CE:Space, it should be perfectly compatible"* comes from the **1.12.2 NTM CE: Space addon** page (Th3_Sl1ze / Warfactory), not from the 1.7.10 NTM: Space fork. The underlying point survives - on 1.7.10, TFC/TFC+ itself does not use Mixins (the MixinBooter dependency belongs to the 1.12.2 TFC Nuclear Tech Addon), so mixin-clash risk is a property of the bridge's own toolchain (e.g., UniMixins/Cleanroom), not of TFC. (Applied to incompatibility #28.)
- **C3 - TNFC_Mod's current target is 1.12.2.** TechNodeFirmaCraft originated on 1.7.10 but the maintained pack and its glue mod are 1.12.2-era; it remains a valid design pattern reference, but its code is not directly reusable on 1.7.10.

### Newly discovered prior art (strengthens the core thesis)

- **HBM2TFC (Bufka2011)** - *"A mod that adds compatibility bridge between HBM's NTM (+ Space) and TerraFirmaCraft+"*, a 1.7.10 **Cleanroom** mod. Current state: "Fixes worldgen Crash; Fixes space suits." This is someone already building exactly the bridge this document specifies - in early alpha, but its existence both validates feasibility and offers a collaboration/fork target instead of a green-field start.
- **bobfluidtranslator** - a small 1.7.10 mod seen in NTM: Space crash logs (HBM issue #2650), evidence that fluid-translation glue for this ecosystem is already being attempted independently.

### Not independently verifiable (kept as design assumptions)

The following are internally consistent with TFC/NTM mechanics but were not confirmed against source code in this pass; each is flagged "verify in code" in the roadmap: exact NTM ore/biome spawn tables, NTM machine ore-dictionary acceptance behavior, NTM tool harvest-level registration, NTM: Space splashdown altitude logic, and NTM structure placement Y-math.

---

## Part III - The 30 Incompatibilities (corrected)

*Framing: this is not a list of bugs; it is one structural conflict expressed many ways. TFC replaces vanilla's world; NTM assumes vanilla's world exists.*

### Tier 1 - Blockers (survival impossible until solved)

**1. World provider collision.** NTM installs a custom world provider to render Tom-impact nuke effects (sky color, lighting); TFC installs its own provider for tectonic terrain, rivers, and layered geology. Two mods cannot co-own the overworld provider. NTM's README ships the kill switch `1.32_enableImpactWorldProvider` - disabling it saves TFC's world but sacrifices NTM's post-detonation visual feedback. *(Verified V2.)*

**2. No oil, ever.** NTM's oil fields are keyed to vanilla biome IDs; a TFC world contains none of those biomes. Primary source: *"most importantly, there is no oil in the world at all"* (Hamster-Systems #37). Oil gates NTM's entire mid-game - fuel, plastic, and rocket construction, which per the NTM: Space wiki assumes the oil stage is reached. The thematically correct fix, proven by the 1.12.2 addon: bitumen/oil seeps embedded in TFC's sedimentary rock strata. *(Verified V4, V6.)*

**3. Geology mismatch - ores in the wrong universe.** TFC generates ~20 rock types in geological layers, with ores locked to rock type in large sparse veins found by prospecting, at roughly double vanilla terrain height (sea level y=145 vs vanilla ~y63). NTM ores (uranium, thorium, tungsten, bauxite, oil shale, rare earths, schrabidium) spawn in vanilla stone keyed to vanilla biomes - in a TFC world they either never generate or sit in vanilla stone that shouldn't exist. Requires fully custom ore generation tied to TFC rock layers - the 1.12.2 addon's proven spec. *(Verified V1-terrain, V6.)*

**4. Vanilla resource assumptions.** NTM's recipe load references vanilla items TFC removes or replaces: vanilla iron ingot (TFC's is wrought iron from a bloomery), redstone, gems, vanilla coal, stone, glass. Every such recipe must be audited and re-based onto TFC equivalents.

### Tier 2 - Progression and economy breakers

**5. The steel exploit.** TFC steel = tier-4 anvil plus flux welding of wrought iron, atop an 8-tier smithing ladder from stone knapping to blue/red steel. NTM steel = alloy furnace, iron plus coal dust. Any conversion path NTM-steel → TFC-steel guts TFC's identity. The bridge must interlock the trees (e.g., NTM machine frames requiring TFC wrought iron), not merely convert items.

**6. Ore grading vs ore items.** TFC ores carry poor/normal/rich grades yielding units of metal; NTM's shredder/centrifuge/acidizer assume one item = one ore with fixed output. Rich TFC ores through NTM machines = metal multiplication. Compounding it, TFC registers almost no ore-dictionary entries, so NTM machines won't even accept TFC ores without bridge registration.

**7. Fresh vs salt water.** TFC distinguishes drinkable fresh water from seawater as separate fluids (jugs fill only from fresh water). NTM boilers, cooling, and chemical plants expect vanilla water. Machines reject TFC fresh water unless the bridge declares fluid equivalence - a documented conflict class in the TFC ecosystem, including this exact pairing's issue tracker and TerraFirmaPunk's dev log. *(Verified V4, V11.)*

**8. Tool tiering.** TFC blocks check tool class and metal tier against rock hardness. NTM tools carry neither registration, so an NTM titanium pickaxe literally cannot mine TFC rock - and TFC tools aren't recognized by NTM blocks either. Tool classes must be registered in both directions.

**9. Structures and splashdown at the wrong Y.** TFC seas sit at y=145; NTM: Space splashdown and structure placement assume ~y63. Returning rockets miss the ocean math; structures embed at wrong elevations. *(Corrected per C1.)*

**10. TFC container size/weight.** TFC chests, vessels, and bags only accept items within size/weight classes. NTM items carry no TFC data - many can't be stored in TFC containers at all, or default wrongly.

**11. Coal/charcoal variants.** TFC coal (bituminous/lignite) and TFC charcoal are not vanilla coal; NTM coke ovens and solid-fuel machines need the bridge to accept TFC fuels.

### Tier 3 - Survival systems that collide (design decisions)

**12. Wounds vs guns.** TFC+ inflicts typed wounds - piercing causes wounds, slashing causes cuts, crushing/falls cause fractures, each needing specific medicine. NTM guns and blades use untyped custom damage sources, so TFC+'s wound system never fires. Mapped properly this is a selling point: gunshot wounds requiring real treatment.

**13. Max-HP tug-of-war.** NTM's digamma radiation works by *removing* max HP; TFC+ *raises* max HP via XP levels, nutrition, and wounds. Two writers on one attribute demand a defined precedence, or TFC+ leveling accidentally cures digamma and radiation corrupts nutrition scaling.

**14. Hunger model.** TFC+ replaces hunger with a 24-ounce stomach, thirst, and nutrient groups; food decays. NTM's few edibles (pills, vodka) feed vanilla hunger values that TFC remaps - no-ops or exploits. (Fun bridge: NTM's Fabulous Vodka, a digamma cure, belongs in TFC+'s alcohol system.)

**15. Fallout vs a survival economy.** NTM fallout irradiates chunks permanently; TFC settlements run on finite local fresh water and farmland. An irradiated pond is a permanent settlement-killer - far harsher than in vanilla. Best content opportunity in the crossover: decontamination as a core mechanic.

**16. Hazmat vs body temperature.** TFC+ natively tracks body temperature via clothing, dampness, exertion, roofs, and hats - heat stroke strikes when thirst is empty above 35 °C. Sealed NTM hazmat and power armor has no thermal model; the bridge must decide whether suits insulate, cook the wearer, or respect TFC+ clothing slots.

### Tier 4 - World physics interactions

**17. Cave-ins meet missiles.** TFC cave-in physics (unsupported rock collapses; support beams prevent it) plus NTM craters, missiles, and mining explosives = collapse cascades and lag. Policy needed: do support beams survive schrabidium?

**18. Falling-block cascades.** TFC dirt/gravel fall; craters trigger endless gravel slides. Cosmetic but noisy.

**19. Firestorms.** TFC+ torches ignite foliage, and TFC trees are custom structures; NTM incendiaries/napalm turn this into biome-scale fire. Bug or feature - decide.

**20. Missiles and chunkloading.** NTM missiles rely on chunkloading to reach targets; NTM's own README documents optimization mods freezing them mid-air. TFC's larger travel distances amplify the risk.

**21. Spawn protection.** TFC+'s protection meter vs NTM automated turrets - edge-case griefing rules on servers.

### Tier 5 - NTM: Space layer

**22. World type exclusivity.** NTM: Space's Martian world type (its Mark-Watney survival mode) is mutually exclusive with TFC's world type at world creation - a per-world decision. NTM: Space's other planets are ordinary custom dimensions and work fine alongside TFC's world; the "no other dimensions" line in TFC's FAQ refers only to vanilla Nether/End portals, which TFC disables. *(Verified V8.)*

**23. Calendar off-world.** TFC's seasonal calendar is overworld logic; space bodies run their own day/night cycles. Sane default: freeze the TFC calendar outside the overworld.

**24. Off-world resource bypass.** NTM: Space's gallium/niobium/lanthanum/hafnium/neon chain lets players skip the entire TFC metallurgical ladder. Defensible only if space is positioned as post-TFC endgame.

**25. Soil → farmland.** NTM: Space's nitrogen-distillation soil creates vanilla farmland; vanilla crops don't exist under TFC. Must create TFC farmland or terraforming is dead content.

**26. Teleporters become the only dimensional transport.** Cross-dimension teleporters now require N-MASS(II) driver fuel, and TFC removes vanilla portals - so teleporters are the sole interdimensional route. Their arrival behavior vs TFC+ spawn protection must be defined. *(Verified V9.)*

### Tier 6 - Technical / loader level

**27. Biome-array crash (documented).** EndlessIDs issue #199: NTM: Space's nuclear dust/fallout *"tried to access the biome array of a chunk like in vanilla"* and crashed under extended-biome-ID mods. NTM code assumes the vanilla byte biome array; TFC brings its own biome set, and 1.7.10 players commonly stack biome extenders. Audit NTM's biome-array accesses (`WorldUtil.setBiome` and callers). *(Verified V5.)*

**28. Mixin contract.** The 1.12.2 CE: Space addon's stated rule: *"If your addon doesn't clash mixin functionality with CE:Space, it should be perfectly compatible."* TFC/TFC+ on 1.7.10 does **not** use Mixins - the MixinBooter dependency belongs to the 1.12.2 TFC Nuclear Tech Addon, not to TFC itself. The mixin risk is NTM-vs-bridge (whichever ASM/Mixin toolchain the bridge adopts, e.g., UniMixins or Cleanroom on 1.7.10), not NTM-vs-TFC. *(Corrected per C2.)*

**29. NTM's other self-documented invasive behaviors** (its README "compatibility notice"): skybox chainloader (config `1.31_enableSkyboxes`) colliding with TFC's own sky; stat re-registering for all modded items; global keybind-overlap handling; render-distance capping at 16 (hurts tall TFC vistas); sound-limit extension; and a deliberate hard crash on Thermos-type servers. Each needs a TFC test. *(Verified V2, V3.)*

**30. Fluid and dimension registration order.** TFC registers fresh/salt water and its own fluids; NTM registers crude oil and a large chemical fluid set. Dimension and fluid IDs must be sequenced so NTM: Space dimensions never inherit TFC's overworld generation logic.

### Verified non-issues (checked, low risk)

- **Power:** NTM's HE is self-contained; TFC has no power system - no unit clash, pure bridge opportunity.
- **XP/enchanting:** neither mod uses vanilla enchanting; TFC XP is survival stats.
- **Sleeping:** TFC straw beds and NTM have no conflict.
- **Mob loot:** TFC mobs drop TFC items, NTM mobs drop NTM items; cross-needs are minor.
- **Other dimensions existing at all:** works (see #22) - space travel is fully viable in a TFC world once splashdown Y and calendar rules are fixed.

### Proof and prior art

- **TheVoidedOnes' TFC Nuclear Tech Addon (1.12.2)** proves the feature set: recipe transfer to TFC rocks/ores, TFC-style metallurgy, fully custom ore generation, NTM Space compat.
- **Hamster-Systems issue #37** is the primary-source statement of the ore-coverage and oil problems.
- **TechNodeFirmaCraft's TNFC_Mod** is the flagship "TFC + heavy industry" glue-mod pattern; **TerraFirmaPunk** proves 1.7.10 TFC+tech coexistence and documents the water conflict in the wild.
- **KirbCraft** (Modrinth, 1.7.10) - *"combines TFC+ with NTM space"* - is a published pack on the exact target combination, advertising seasons, thirst, radiation, new ore generation, and oil processing: someone already hand-built the bridge as pack configuration; TerraFirmaBob replaces that with a real mod.
- **HBM2TFC** (Bufka2011) - an early-stage 1.7.10 Cleanroom mod attempting this exact bridge ("fixes worldgen crash; fixes space suits") - validates feasibility and offers a collaboration or fork target.

**Bottom line.** TFC and NTM/NTM: Space are two complete visions of Minecraft sharing one engine. Config cannot fix Tiers 1–2; a dedicated bridge mod - rewriting recipes, generating NTM ores in TFC geology, creating oil, declaring fluid/tool/metal equivalences, and interlocking the two tech trees - is the only path from "two mods in a folder" to one coherent game.

---

## Part IV - Bridge Mod Architecture and Design Decisions

### IV.1 Mod identity and loading strategy

- **Name:** TerraFirmaBob (TFC's "TerraFirma" × NTM's "Bob"/HBM). Mod ID suggestion: `tfbob`.
- **Loader:** Forge 1.7.10 coremod. TFC/TFC+ does not use Mixins; the bridge should adopt **one** patching toolchain and hold to it - either legacy Forge ASM coremod transformers or the modern 1.7.10 **UniMixins** stack (HBM2TFC uses Cleanroom). Decision rule: if the bridge ever targets GTNH-adjacent packs, UniMixins; otherwise plain ASM keeps the dependency footprint to zero.
- **Dependency model (mutually exclusive targets):**
  - `required-after:terrafirmacraft` (or TFC+; detect and adapt - TFC+ is the primary target given its survival systems)
  - `after:hbm` - satisfied by **either** NTM **or** NTM: Space, never both; detect at runtime which fork is present and enable the Space module only for the fork.
  - Hard-fail with a readable error if both `hbm` jars (base + Space) are detected - they share the mod ID and the game would crash anyway; the bridge should say why.
- **Configuration philosophy:** every behavioral decision below ships with a config default; anything touching worldgen or progression interlock also ships a pack-maker API hook (CraftTweaker/ZenScript exposure in a later phase).

### IV.2 Module map

Each module owns a set of incompatibility numbers from Part III.

| Module | Owns | Responsibility |
|--------|------|----------------|
| **Core** | 1, 28, 29, 30 | Loader contract, fork detection, config sequencing, NTM invasive-behavior taming |
| **WorldGen** | 2, 3, 27 | TFC-strata ore generation, oil seeps, biome-array audit patches |
| **Materials** | 4, 5, 6, 11 | Recipe re-basing, ore-dict registration, metal interlock, fuels |
| **Fluids** | 7, 30 | Fresh/salt water equivalence, oil fluid registration order |
| **Tools & Storage** | 8, 10 | Tool-class registration both ways, size/weight data for NTM items |
| **Survival** | 12, 13, 14, 15, 16 | Wound mapping, HP precedence, hunger/thirst, fallout economy, hazmat thermals |
| **Physics** | 17, 18, 19, 20, 21 | Cave-in/explosion policy, falling blocks, fire, chunkloading, spawn protection |
| **Space** | 9, 22, 23, 24, 25, 26 | Splashdown Y, world-type gate, calendar freeze, endgame gating, farmland, teleporters |

### IV.3 Core module (1, 28, 29, 30)

- **World provider (1):** default the bridge to *forcing* `1.32_enableImpactWorldProvider=false` at first run (with a loud log line), then offer an optional "provider weave" that re-implements NTM's Tom-impact sky/lighting effects as an event-driven overlay on TFC's provider instead of a provider replacement. Phase 1 ships the force-off; the weave is Phase 4 polish.
- **Skybox (29):** NTM's chainloader is designed to defer to other mods' skyboxes; TFC's sky rendering is part of its provider, so the safe default is `1.31_enableSkyboxes=false`. Verify whether TFC's seasonal/day-length sky still renders; document the trade-off (no NTM skybox effects).
- **Render distance cap (29):** NTM caps render distance at 16. The bridge patches the cap out when TFC is present (TFC vistas are a core feature), guarded by a config for low-end machines.
- **Thermos crash (29):** leave NTM's deliberate Thermos crash intact but append bridge-specific guidance to its crash message if TFC is present (TFC+ servers often run Thermos-family bases). Long-term: document a supported server base.
- **Registration order (30):** bridge loads `after:*` in preInit of both parents; asserts fluid-ID and dimension-ID allocation happen after both parents register, and stamps NTM: Space dimensions with a "not TFC" marker so no TFC gen logic ever runs there.
- **Stat/keybind behaviors (29):** test matrix entries only; no code unless a concrete conflict is observed.

### IV.4 WorldGen module (2, 3, 27)

The heart of the bridge. Spec follows the 1.12.2 addon's proven model, adapted to 1.7.10 TFC/TFC+ APIs.

- **Ore generation in TFC geology (3):** disable all NTM overworld ore spawning; register bridge generators that place NTM ores as TFC-style deposits:
  - Uranium/pitchblende → large sparse veins in granite/metamorphic layers, prospecting-detectable via TFC prospector's pick.
  - Thorium → rare veins in igneous extrusive layers.
  - Tungsten (wolframite/scheelite skin) → metamorphic contact zones.
  - Bauxite → surface-near sedimentary (laterite logic: wet/hot climates only - uses TFC climate data, a genuine improvement over vanilla-keyed spawn).
  - Oil shale, lignite-adjacent → sedimentary.
  - Rare earths → small veins keyed to specific intrusive rocks.
  - Schrabidium: **never worldgen** - keep it crafted/transmute-gated as in NTM.
  - Every deposit uses TFC's poor/normal/rich grade distribution so prospecting skill matters for NTM ores exactly as for TFC ores.
- **Oil (2):** two-tier solution.
  - *Bitumen seeps:* rare surface features in sedimentary biomes (oil-sand blocks + crude seep fluid), mineable early - small, finite, flavor-first.
  - *Deep oil deposits:* TFC-strata-keyed reservoirs detectable with NTM's own survey tools, extracted with NTM pumps/derricks - this preserves NTM's intended oil gameplay while making the deposits exist in a TFC world.
- **Biome-array audit (27):** patch `WorldUtil.setBiome` and all direct chunk biome-array writes to go through a bridge facade that is EndlessIDs-aware (use the extended-ID API when present, vanilla array otherwise). This fixes the documented #199 crash class for the whole ecosystem, not just TFC.
- **Vanilla-stone containment:** audit NTM decorators/structures that place vanilla stone/cobble in the overworld; substitute the local TFC rock type (TFC exposes per-chunk rock layer data) so no foreign vanilla stone ever appears in-world.

### IV.5 Materials module (4, 5, 6, 11)

- **Recipe audit (4):** machine-generated diff of every NTM recipe against "items that exist in a TFC world." Re-base rulebook:
  - vanilla iron ingot → TFC wrought iron ingot;
  - vanilla redstone dust → TFC's redstone-bearing mineral (or a bridge-added "refined redstone" from TFC cinnabar);
  - vanilla coal → TFC bituminous coal; charcoal → TFC charcoal;
  - vanilla stone/cobble/glass → TFC equivalents (TFC glass is made from sand in a fire pit - keep NTM glass recipes but re-base the input).
- **Metal interlock, not conversion (5):** the single most important design decision.
  - **Rule 1:** no recipe converts NTM steel → TFC steel or vice versa. They are different materials in-fiction (industrial mild steel vs hand-forged crucible steel).
  - **Rule 2:** interlock by *dependency*: NTM machine frames, anvils-as-machines, and structural blocks require TFC wrought iron / steel *components* (plates, rods - crafted on TFC anvils or in NTM machines from TFC ingots). Consequence: you cannot start NTM without progressing TFC smithing to at least wrought iron; NTM then accelerates TFC (powered bellows equivalents, machine-driven quench/grind) without replacing it.
  - **Rule 3:** NTM's alloy furnace accepts TFC metals for NTM alloys only (e.g., NTM's own steel from TFC iron + TFC coal dust is fine because NTM-steel never flows back into the TFC smithing tree).
- **Ore grading (6):** register TFC ores into the ore dictionary **with grade-aware NTM machine recipes**: NTM shredder/centrifuge/acidizer recipes for TFC ores scale output by grade (poor = 0.5×, normal = 1×, rich = 1.5× of a normalized unit), implemented as custom machine-recipe handlers rather than 1:1 item mapping. No free multiplication.
- **Fuels (11):** register TFC bituminous coal, lignite, and charcoal as valid NTM solid fuels / coke oven inputs with TFC-accurate burn values; NTM coke recipes accept TFC coal.

### IV.6 Fluids module (7, 30)

- **Water equivalence (7):** declare NTM's `water` expectations satisfiable by TFC fresh water via a bridge fluid-alias layer (do NOT make salt water machine-usable - chemistry consequences). Boiler feed, cooling, and chemical plant recipes accept fresh water. This matches the community consensus fix direction noted in Hamster-Systems #37 ("fresh water is also water and boilers could use it") and the pain documented in TerraFirmaPunk.
- **Oil registration (30/2):** crude oil fluid registered by NTM is preserved; bridge only adds worldgen sources. Ensure TFC barrel/vessel interactions with NTM fluids are either fully supported (small-volume transport - nice feature) or explicitly blacklisted (no infinite-fluid exploits via barrels).

### IV.7 Tools & Storage module (8, 10)

- **Bidirectional tool registration (8):** register NTM tools with TFC's tool-class system (pickaxe class + metal tier mapped from NTM material: titanium ≈ steel tier; schrabidium ≈ blue/red steel tier); register TFC tools with NTM blocks' harvest requirements via harvest-level mapping. Atomic, config-tweakable mapping table.
- **Size/weight (10):** assign TFC size/weight data to all NTM items via a generated default table (machine parts = medium/heavy; ingots/plates = small; dusts = tiny) with a config override file for pack makers. Test pass: every NTM item must be storable in *some* TFC container.

### IV.8 Survival module (12, 13, 14, 15, 16)

- **Damage-type mapping (12):** map NTM damage sources onto TFC+ wound types: bullets/shrapnel → piercing wounds; blades → cuts; explosions/falls → fractures; radiation/digamma stay NTM-side (they are already typed status systems). Result: gunshot wounds need real TFC+ medicine - the crossover's signature feature.
- **HP precedence (13):** single-writer rule. TFC+ owns the *base* max-HP attribute; digamma/radiation apply as a multiplicative *modifier* computed after TFC+'s value, never by rewriting the base attribute. Leveling up can no longer cure digamma; radiation can no longer corrupt nutrition scaling.
- **Hunger (14):** NTM edibles get TFC+ food data (stomach ounces, nutrient group, decay); Fabulous Vodka registers in TFC+'s alcohol system and keeps its digamma-cure effect with a TFC+-scaled dose. NTM pills become TFC+ medicine-adjacent items.
- **Fallout economy (15):** irradiated fresh-water sources stay irradiated (do not soften this); instead, add **decontamination gameplay**: NTM's existing decon tools re-balanced around TFC resources, plus a late-game "soil washing" multiblock recipe line. Fallout permanently kills a settlement's water unless the player invests - harsh, memorable, on-theme for both mods.
- **Hazmat thermals (16):** sealed suits get a thermal model: full insulation against environment (no cooling in heat, no warming in cold) + internal heat build-up under exertion, vented at TFC+ fresh water or NTM cooling infrastructure. Suits occupy TFC+ clothing slots (no double-dipping clothing + hazmat). Power armor draws on its power supply for active cooling - energy-for-comfort trade.

### IV.9 Physics module (17, 18, 19, 20, 21)

- **Explosion/cave-in policy (17, 18):** NTM explosions trigger TFC collapse checks once per affected region (batched, not per-block) with a per-tick collapse budget; support beams reduce collapse probability from explosions within their radius but do not grant immunity to schrabidium-class blasts (flavor: some energies laugh at timber). Gravel-slide cascades capped per tick.
- **Fire policy (19):** NTM napalm/incendiaries ignite TFC foliage using TFC+'s own spread rules; add config `napalmBiomeBurn` (default false on servers) because biome-scale fire × TFC forests is a genuine content question, not a bug.
- **Chunkloading (20):** document that NTM missiles need a chunkloading solution for TFC-scale distances; ship a recommended-config file for the common 1.7.10 chunkloaders rather than inventing a new one. Add a bridge watchdog that logs missiles frozen mid-air (diagnosing the README-documented optimization-mod freeze).
- **Spawn protection (21):** turrets treat TFC+ spawn-protected players/land as no-target zones; teleporter arrivals inside protected zones respect the protection meter.

### IV.10 Space module (9, 22, 23, 24, 25, 26)

- **World-type gate (22):** at world creation, if the Martian world type is selected, the bridge warns that TFC worldgen is disabled there (mutually exclusive). Overworld = TFC; Mars-survival = NTM: Space; never both in one dimension. Other planets (ordinary custom dimensions) are unaffected.
- **Splashdown & structure Y (9):** read TFC's actual sea level (y=145) at runtime - do not hardcode - and feed it to NTM: Space splashdown math; same for structure placement ground-detection (query TFC's surface height map).
- **Calendar freeze (23):** outside the overworld, TFC calendar/season/temperature queries return a frozen snapshot; TFC crops/food decay off-world use NTM: Space's local day length mapped to calendar ticks. (Per-body atmosphere already handled by NTM: Space.)
- **Endgame gating (24):** space resources (gallium/niobium/lanthanum/hafnium/neon) gate behind TFC blue/red steel *and* NTM oil-stage progression - the rocket's structural parts require TFC high-tier metals, making space the joint endgame of both trees rather than a bypass of either.
- **Farmland (25):** NTM: Space nitrogen-distillation soil converts to TFC farmland (with nutrients initialized from the distillation recipe's inputs); TFC crops grow off-world under NTM: Space atmosphere rules - terraforming becomes real content instead of dead content.
- **Teleporters (26):** as the sole interdimensional route, teleporter arrival integrates with TFC+ spawn protection (arrival pads count as the owner's claim); N-MASS(II) fuel chain stays as NTM: Space designed it.

---

## Part V - Implementation Roadmap

### Guiding principles

1. **Playable early.** Phase 1 alone must produce a survival-playable world (Tiers 1–2 solved). Everything after is depth.
2. **No forks of either parent.** The bridge never ships modified NTM/TFC jars - only runtime patches and added content. NTM: Space updates within ~24 h of upstream NTM, so the bridge must tolerate a fast-moving target: prefer event/API hooks over bytecode patches; keep every ASM patch small, named, and unit-tested.
3. **Everything configurable.** Every design decision in Part IV has a config key; worldgen and interlock decisions additionally get ZenScript hooks in Phase 3.
4. **Don't repeat KirbCraft by hand.** KirbCraft's config-level fixes are the requirements document for what must become code.

### Phase 0 - Foundation (2–3 weeks)

- Repo, build (Gradle + Forge 1.7.10), CI with both NTM and NTM: Space as test targets; runtime fork detection; hard-fail-on-both logic.
- Toolchain decision: ASM vs UniMixins (evaluate HBM2TFC's Cleanroom approach; contact its author - collaboration beats duplication).
- Code-verification sprint for the unverified assumptions from Part II: NTM ore/biome spawn tables, machine ore-dict acceptance, tool harvest registration, splashdown math, structure Y-math. Output: confirmed spec deltas.
- **Milestone M0:** dev environment boots TFC+ + NTM: Space + bridge; world loads without crash; worldgen-crash class fixed (HBM2TFC parity).

### Phase 1 - Survival floor (Tier 1 + critical Tier 2) (4–6 weeks)

- WorldGen: disable NTM overworld ores; TFC-strata deposits for uranium, thorium, tungsten, bauxite; bitumen seeps + deep oil deposits; biome-array facade (#27 fix).
- Core: world-provider force-off; skybox off; render-cap patch; registration ordering.
- Fluids: fresh-water equivalence.
- Materials: recipe audit pipeline (automated diff tool → re-base table); wrought-iron machine frames (interlock rule 2); grade-aware machine inputs; TFC fuels.
- Tools & Storage: bidirectional tool registration; NTM item size/weight table.
- **Milestone M1:** a fresh world supports stone-age → bloomery → first NTM machines → oil extraction, entirely in TFC geology, survival-legit. This is the public alpha.

### Phase 2 - One economy (remaining Tier 2 + Tier 3) (4–6 weeks)

- Full metal interlock tables; barrel/fluid policy; wound-type mapping; HP precedence; hunger integration (Vodka!); hazmat thermals v1.
- **Milestone M2:** beta - both tech trees progressible to their mid-games with no exploits (steel exploit closed, grade multiplication closed, no-ops closed).

### Phase 3 - The world fights back (Tier 4 + polish) (3–4 weeks)

- Explosion/collapse batching; fire policy; chunkloading watchdog + recommended configs; turret/protection rules.
- ZenScript/pack-maker API surface.
- **Milestone M3:** release candidate for overworld play; server-hardening pass (Thermos guidance, turret edge cases).

### Phase 4 - Space (Tier 5) (4–6 weeks)

- Splashdown Y from live TFC data; calendar freeze; farmland conversion; endgame gating (blue/red-steel rocket structure); teleporter/protection integration; Martian world-type warning UX.
- **Milestone M4:** full-loop release: TFC stone age → NTM industry → NTM: Space interplanetary endgame as one continuous progression.

### Phase 5 - Community hardening (ongoing)

- Test matrix: NTM vs NTM: Space × TFC vs TFC+ × with/without EndlessIDs, UniMixins, common optimization mods (the missile-freeze class), dedicated server vs integrated.
- Track upstream: NTM: Space's ~24 h upstream cadence means a per-release smoke suite is mandatory, not optional.

### Risk register

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| Upstream NTM: Space change breaks a patch | High | Medium | Small named patches + smoke suite; prefer hooks |
| TFC+ API gaps (it's a small-team fork) | Medium | High | Early contact with Dunk's team; TFC 1.7.10 API policy explicitly welcomes addon-driven requests |
| Oil balance: seeps too generous → skips NTM mid-game | Medium | Medium | Seep scarcity configs; playtest vs KirbCraft baseline |
| Legal/licensing: TFC is GPLv3, NTM is GPLv3 - bridge can be GPLv3 cleanly | Low | - | Keep it one license, credit both |
| Scope creep (every mod wants a bridge) | High | Medium | This document is the scope; other mods via the Phase 3 API only |

---

## Appendix A - Sources

- NTM GitHub README (compatibility notice: Thermos, skybox `1.31_enableSkyboxes`, world provider `1.32_enableImpactWorldProvider`, shaders) - github.com/HbmMods/Hbm-s-Nuclear-Tech-GIT
- NTM wiki: Versions & Forks; NTM: Space; Teleporter - nucleartech.wiki
- Hamster-Systems/Hbm-s-Nuclear-Tech-GIT issue #37 - TFC integration ("there is no oil in the world at all")
- GTMEGA/EndlessIDs issue #199 - NTM: Space biome-array crash
- TheVoidedOnes/TFC-Nuclear-Tech-Addon - 1.12.2 bridge (README: custom ore gen, TFC metallurgy, MixinBooter, optional NTM Space)
- HBM's NTM CE: Space (Th3_Sl1ze/Warfactory) - mixin-compatibility statement, 1.12.2 addon
- KirbCraft modpack - modrinth.com/modpack/kirbcraft ("Combines TFC+ with NTM space")
- Bufka2011/HBM2TFC - existing 1.7.10 Cleanroom bridge attempt
- AnodeCathode/TNFC_Mod - TechNodeFirmaCraft glue mod
- TerraFirmaPunk 2.0 - CurseForge + FTB forum thread (water-compatibility dev log)
- TerraFirmaCraft 1.7.10 wiki - Altitude (sea level y=145), Main Page (TFC+ = Dunk's 1.7.10 fork)
