# Companions: item-registration framework + first sweaters

Small reusable item-registration helper in `rkcore`, proven by one real feature: wolf-worn
"sweater" items coexisting with vanilla `minecraft:wolf_armor`, shipped from new standalone mod at
`rkc/mods/companions`. API claims here checked against `libs/decompiledjava/minecraft-26.2/` and
Polymer checkout at `libs/polymer/`, not memory; citations are `File.java:line` for recheck.

Two requirements shape whole build order:

- **`rkcore` keeps one canonical registration helper, `companions` reuses it** — not
  hand-duplicated copy. Real code sharing via build tooling, chosen over duplicating ~10-line
  helper or optional-dependency-with-inline-fallback split.
- **`companions` installs completely standalone** — dropped alone into mods folder with no rkcore
  jar, and even with rkcore installed alongside, none of rkcore's other features (hover
  labels, its mixins, its own entrypoint) may activate as side effect.

Stage order: S1 (rkcore-side helper) unblocks everything. S2 and S3 (bare scaffold, then shaded
dependency) both must land before any real item code compiles against shared class.
S4–S6 (item, stats, recipe), S8 (freeze immunity) and S9 (per-colour variants) buildable
in any order once S3 lands; S9 reverses S4's single-item decision. S7 (standalone-install
proof plus docs) is last, makes feature complete. Stage numbers record order stages were
*asked for*, not order they run.

---

## Findings that reshape the work

**Normal Fabric mod dependency or Loom's `include()` both fail standalone requirement.**
`modImplementation` + `fabric.mod.json` `"depends"` entry forces rkcore's jar present
wherever `companions` installed. Loom's Jar-in-Jar `include()` embeds whole nested jar,
`fabric.mod.json` and all, and Fabric Loader loads it as second full mod (entrypoint, mixins,
everything). Fix: Gradle Shadow-plugin relocation (S3): `companions` copies just rkcore's
`ItemRegistration` bytecode into its own jar under private package name, so logic has one
canonical source with no runtime dependency on second jar. `com.gradleup.shadow:9.6.1` can
relocate and merge specific classes without pulling in dependency's Fabric metadata, which
`include()` cannot. Relocating also means if rkcore *is* separately installed, no two jars both
claim to define `club.rainbowkitty.rkcore.common.item.ItemRegistration` in shared classloader.

**Vanilla wolf armor needs no custom `Item` behaviour, and its convenience builder doesn't fit our
stats.** `Item.Properties.wolfArmor(ArmorMaterial)` (`Item.java:558-574`) hardcodes
`.durability(...)` (finite) and `.repairable(...)` (declares repair tag) — wrong for item that
must be unbreakable and non-repairable. S5 reproduces its body by hand, dropping those two calls.

**Infinite durability is absence of `MAX_DAMAGE`; `DataComponents.UNBREAKABLE` is display-only.**
Omitting `.durability(...)` makes item unbreakable — with no `MAX_DAMAGE`,
`ItemStack.isDamageableItem` is false and `hurtAndBreak` no-ops. `DataComponents.UNBREAKABLE`
(`DataComponents.java:119`, a `Unit` marker) matters only because Polymer builds client stack
from `Items.WOLF_ARMOR`, whose `MAX_DAMAGE` would otherwise draw durability bar. Polymer's
`COMPONENTS_TO_COPY` excludes `UNBREAKABLE`, so S5 also calls
`PolymerItemUtils.addCopiedComponent(DataComponents.UNBREAKABLE)` once — server-wide and additive.

**`EquipmentAsset` needs no server registration.** `EquipmentAssets.createId` is just
`ResourceKey.create(ROOT_ID, id)`, absent from `BuiltInRegistries` — mod-owned asset key is purely
client resource-pack JSON reference.

**No per-layer constant tint (shapes S9).** `EquipmentLayerRenderer.getColorForLayer:111`
reads `DyedItemColor.getOrDefault(stack, 0)` **once per stack**; every layer is then untinted or
follows that one value. Two regions cannot get two colours from data, so a variant's body
colour must be baked into its texture while trim stays live-dyeable.

---

## How these stages get tested

Each stage states **Build** and **Done when**. Stage not finished until its "Done when"
observed in real build or dev-server session, so failure at stage N implicates stage N, not
ones before it. In-world checks run on mod's own dev server
(`scripts/devserver.sh rkc/mods/companions ...`); S7 additionally assembles standalone scratch
server, since relocated bytecode never runs under `runServer`.

## S1 — rkcore: the reusable registration helper — **done**

**Build.** New `common/item/ItemRegistration.java` in rkcore (package mirrors `common/label/`,
`common/rule/`). One static method `register(String namespace, String path,
Function<ResourceKey<Item>, T> factory)` — namespace is explicit parameter because real
caller lives in different mod. Publish locally: `cd rkc/mods/rkcore && ./gradlew
publishToMavenLocal` (writes `club.rainbowkitty.rkcore:rkcore:0.1.0` to `~/.m2`, regardless of
commented `mavenLocal()` line under `publishing.repositories`).

**Done when.** `~/.m2/repository/club/rainbowkitty/rkcore/rkcore/0.1.0/` contains jar and POM
after `./gradlew build && ./gradlew publishToMavenLocal`.

### As built

Built as planned. Three registry signatures re-checked against decompiled tree, all
held — `Registry.register` (`Registry.java:117`), `ResourceKey.create` (`ResourceKey.java:26`),
`Identifier.fromNamespaceAndPath` (`Identifier.java:42`); `BuiltInRegistries.ITEM` is
`DefaultedRegistry<Item>` (`BuiltInRegistries.java:189`).

Publication is four artifacts, not two — jar, `-sources.jar` (rkcore sets `withSourcesJar()`),
`.pom`, `.module`. `unzip -l` on jar shows it carries rkcore's `fabric.mod.json` alongside its
classes — concrete form of problem S3 exists to solve. Linted clean.

## S2 — Scaffold `companions` as a bare, buildable mod — **done**

**Build.** Copy `fabric_mod_skeleton/` into `rkc/mods/companions`, then replace every
`CHANGEME`/`changeme` placeholder per its README:

| File | Change |
|---|---|
| `settings.gradle` | `rootProject.name = 'companions'` |
| `gradle.properties` | mod_name/description/authors; `maven_group = club.rainbowkitty.companions`; `mod_id = companions`; `entrypoint = club.rainbowkitty.companions.Main`; drop `bil_version` |
| `CHANGEME.java` | → `club.rainbowkitty.companions.Main` |
| `changeme.mixins.json` | delete — no mixins needed |
| `fabric.mod.json` | drop `"mixins"` array; no `"rkcore"` entry in `"depends"` |
| `build.gradle` | drop bil's `mavenLocal()`/Modrinth/Sponge repos, bil dependency, `fabric-permissions-api` JiJ line |

No rkcore wiring yet — that is S3.

**Done when.** `cd rkc/mods/companions && ./gradlew build` succeeds, and `scripts/devserver.sh
rkc/mods/companions restart` boots with 0 `Mixin apply failed`/`InvalidInjectionException`.

### As built

Rename table applied as written. Three things plan missed:

- **`run/` had to be set up by hand, port choice not free.** Sibling checkouts already
  take `server-port` 25577/25599/25601 and `rcon.port` 25575/25576, so `companions` took
  **25602 / rcon 25578**, with `pause-when-empty-seconds=0` so headless world keeps ticking.
  `run/eula.txt` and seven-line `run/server.properties` written directly — server fills
  every unspecified property with its default.
- **`polymer-virtual-entity` also dropped from `depends`** — this mod renders through
  `PolymerItem` and resource pack, not virtual entities. `polymer-core` and `polymer-resource-pack`
  stay.
- **Benign boot-log false alarm:** `Done!` → RCON listener → `Saving chunks` / `All dimensions are
  saved` looks like stdin-EOF implicit-stop trap, but is initial world save. Distinguishing
  check: 25602/25578 stay `LISTEN` and `rcon list` answers.

Boot clean — `companions 0.1.0` in mod list, `Companions loaded!`, 0 mixin failures.

## S3 — Shadow-relocated rkcore dependency — **done**

**Build.** Add `com.gradleup.shadow` 9.6.1 and private `shade` configuration
(`implementation.extendsFrom shade`) to `companions/build.gradle`. `shadowJar` takes plain
archive name and relocates `club.rainbowkitty.rkcore.common.item` into private package.

**Flagged unknown:** whether source imports `ItemRegistration` under original rkcore package
name or relocated name. Resolve by building and reading compiler/runtime behaviour.

**Done when.** `./gradlew build` succeeds, and `unzip -l build/libs/companions-*.jar` shows
`companions`' own classes plus exactly one relocated `ItemRegistration` class, and **nothing** from
rkcore's `Main`, mixins, or `fabric.mod.json`.

### As built

`com.gradleup.shadow` 9.6.1 runs against Gradle 9.5.1 + Loom 1.17.19 with no warnings. Three things
had to change:

- **Dependency must be non-transitive.** `shade "…:rkcore:0.1.0"` produced 10 MB, 5332-entry
  jar carrying all of fabric-api and Polymer, because rkcore publishes `from components.java` and
  its `implementation` deps land in POM as runtime scope. Fixed with `{ transitive = false }`.
- **Shadow's `include`/`exclude` cannot express "keep one class"** — patterns match
  pre-relocation path and also govern this mod's own classes. Instead `rkcoreSharedJar` task
  filters resolved artifact down to `club/rainbowkitty/rkcore/common/item/**` before Shadow sees
  it — stricter than pattern list, needs its own `rkcore` configuration registered *above*
  `dependencies` block.
- **`jar` and `shadowJar` both defaulted to `companions-0.1.0.jar`** with no overlap warning. `jar`
  now carries `unshaded` classifier, so plain name is unambiguously the shaded, installable
  artifact.

**Flagged unknown, resolved:** source imports **original** rkcore package name. Probe class
importing `club.rainbowkitty.rkcore.common.item.ItemRegistration` compiled (unrelocated class on
compile classpath via `implementation.extendsFrom shade`), and `javap` on shaded jar showed
Shadow rewrites references in this mod's own bytecode too. Corollary: because `shade` feeds
`implementation`, `runServer` runs against **unrelocated** class — only real jar install
exercises relocation.

**Amended after S4:** shared package now holds `PolymerModelItem` too, so relocation target
renamed to `club.rainbowkitty.companions.shaded.rkcore.item` (old
`…shaded.itemregistration` had become misnomer). `rkcoreSharedJar` filter needed no change —
payoff of filtering by package. `javap` on `WolfSweater` confirms Shadow rewrites
`new`/`invokespecial` for relocated superclass and synthetic lambda's return type.

## S4 — The wolf sweater item, registered and rendered — **done**

**Revised mid-stage.** Originally one item per colour from `WolfArmorVariant` record. After
first in-world round design changed to single `Wolf Sweater` carrying `minecraft:dyed_color`
component, way vanilla armour does it — less work, because `minecraft:wolf_armor` already solves
problem and every piece copies from it. (S9 later restored per-colour variants for different
reason.)

**Build.** One item, `companions:wolf_sweater`, "Wolf Sweater". Two Java files: a
`PolymerItem` shim returning `Items.WOLF_ARMOR` as client fallback and custom model id, and
`WolfSweater` holding `NAME`/`DEFENSE`, the `ArmorMaterial`, and registration call through the
shaded `ItemRegistration.register(Main.MOD_ID, NAME, key -> …)`. `Main.onInitialize()` adds
`PolymerResourcePackUtils.addModAssets(MOD_ID)` + `markAsRequired()` before `WolfSweater.register()`.

Resources under `src/main/resources/assets/companions/`, mirroring vanilla's
`wolf_armor`/`armadillo_scute` pair: two item textures (icon base + overlay), two body textures,
`items/wolf_sweater.json` (item **definition** — `minecraft:condition` on
`has_component: minecraft:dyed_color`, copied structurally from
`assets/minecraft/items/wolf_armor.json`), plain and dyed item models, `equipment/wolf_sweater.json`
(two `wolf_body` layers, second marked `"dyeable": {}`), and `lang/en_us.json`.

**Done when.** `/give` yields item with no missing-texture/model error; dyed and undyed stacks
render distinguishably; tamed wolf equips it into body slot; `/data get entity` confirms
equipped stack is `companions:wolf_sweater` with `dyed_color` intact, not coerced to vanilla wolf
armor.

### As built

Confirmed in-world: icons render, dyed body layer tints, item equips like wolf armor.
`/data get entity` returns `{body: {components: {"minecraft:dyed_color": 16711680}, count: 1, id:
"companions:wolf_sweater"}}`.

- **One real bug: item-definition file.** First build shipped only
  `models/item/<name>.json` and icon rendered as missing-texture cube. Since 1.21.4 client
  reaches item model through *item definition* at `assets/<ns>/items/<name>.json`; Polymer's
  `getPolymerItemModel` names that definition, not model. Both files required.
- **Dyeable redesign deleted more than it added.** `WolfArmorVariant` and its `VARIANTS` loop
  gone. `DataComponents.DYED_COLOR` already in `PolymerItemUtils.COMPONENTS_TO_COPY`, so
  colour reaches vanilla clients with no `addCopiedComponent` call.
- **Lang file needed, unplanned** — without it item showed raw key. Polymer ships
  mod's `lang/` in generated pack and server resolves it too.

Two dev-environment traps, neither a mod bug: entity `Owner` must be UUID **int-array**, not
string (string logs `Failed to decode value ... Not a list` and wolf silently discarded);
and with **no players online spawn chunks not loaded**, so summoned entities unload same
tick — `forceload add` around spawn fixes it.

**Amended before S5: `PolymerItem` shim generalized into rkcore.** Its whole body was
stock Polymer pattern — vanilla fallback, custom model id — so it became
`rkcore/common/item/PolymerModelItem`, instantiated with fallback and model as constructor
arguments. companions is down to two Java files. Still one-caller abstraction today; taken on
expectation that more items are coming workspace-wide.

## S5 — Stats: defense 17, infinite durability, not repairable — **done**

**Revised twice.** Verified at `defense = 3`, raised to `10`, then to `17` after S9 once damage
formula worked through — see As built. Only constant changed.

**Build.** Replace `WolfSweater.buildProperties`' delegation to vanilla `.wolfArmor(material)` with
hand-rolled equivalent: `.setId`, `.attributes`, the `EQUIPPABLE` component (body slot, wolf-only,
shearable), `.component(DataComponents.UNBREAKABLE, Unit.INSTANCE)`, `.stacksTo(1)` — dropping
`.durability(...)` and `.repairable(...)`. Skipping `.repairable(...)` means no `REPAIRABLE`
component, so anvil has nothing to look up. Add
`PolymerItemUtils.addCopiedComponent(DataComponents.UNBREAKABLE)` to `Main.onInitialize()`.

**Done when.** Repeated damage to equipped wolf never decreases sweater's durability and it
never breaks (gameplay requirement); separately, client shows no durability bar at all
(proving `addCopiedComponent` call took effect).

### As built

Three-wolf differential, three hits of 1 `minecraft:mob_attack` damage from health 8.0:

| Wolf | Health after | Body slot after |
|---|---|---|
| vanilla `wolf_armor` | 8.0 (unchanged) | gained `minecraft:damage: 1` |
| sweater | 5.3 | no damage component |

- **Plan's premise about how wolf armor protects was wrong, and that is why defense kept
  rising.** `Wolf.canArmorAbsorb` (`Wolf.java:427`) is hardcoded `is(Items.WOLF_ARMOR)` identity
  check; in that branch `Wolf.actuallyHurt` (`Wolf.java:409`) never calls `super`, so vanilla wolf
  armor is **100% negation plus durability loss**, not armor points. Modded body item can never
  enter that branch, so sweater falls through to ordinary `LivingEntity` armor-points path
  for everything.
- **No defense value reaches parity.** `CombatRules` clamps post-penetration `realArmor` to
  `MAX_ARMOR = 20.0`, and against unpenetrating damage reduction is `armor/25`: 3 → 12%,
  10 → 40%, 17 → 68%, 20 → 80%. **17** is meaningfully protective without approaching cap;
  parity would need `canArmorAbsorb` mixin, which combined with unbreakability would make wolf
  near-invulnerable, so not pursued.
- **`UNBREAKABLE` is display half, not mechanism** — see Findings.
- Two deletions from plan's code: `DataComponents.BREAK_SOUND` (item that can't break can't
  play it) and `EntityTypes.WOLF.builtInRegistryHolder()` (`@Deprecated`; uses
  `BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(EntityTypes.WOLF)` instead).

**Dev-world trap:** S4 test wolves carry `Invulnerable: 1b`, which silently swallows every
`/damage` command. `TicksFrozen` still accumulates under it, so it stays valid probe where health
does not. (Bites S8 too.)

## S6 — Crafting recipe: wool in the wolf-armor shape — **done**

**Build.** Two recipe types, pure data (mod's own `data/` is part of default data pack).
Shaped recipes reuse vanilla's wolf-armor recipe shape
(`decompiledjava/minecraft-26.2/data/minecraft/recipe/wolf_armor.json`) with wool as key
material — 16 colour-specific recipes rather than one `#minecraft:wool` recipe, so each wool colour
crafts sweater already dyed to match:

```json
{ "type": "minecraft:crafting_shaped", "category": "equipment",
  "key": { "X": "minecraft:red_wool" },
  "pattern": [ "X  ", "XXX", "X X" ],
  "result": { "id": "companions:wolf_sweater",
              "components": { "minecraft:dyed_color": 11546150 } } }
```

The 16 `dyed_color` values are `DyeColor.textureDiffuseColor`, read from `DyeColor.java:30-45`, so
wool-crafted and dye-crafted sweater of a colour are identical. Plus one `minecraft:crafting_dye`
recipe (`target`/`dye`/`result`) that writes `DYED_COLOR` onto result for free-form recolouring.

**Done when.** Crafting 6 wool of a colour yields `companions:wolf_sweater` whose `dyed_color`
matches, verified for two colours; and that sweater plus different dye yields recoloured one.

### As built

Shipped as 17 hand-written files (16 colours + dye recipe). "Identical" claim checked in
source: `DyedItemColor.applyDyes` with one dye and no existing colour has `colorCount = 1`, so every
channel passes through unchanged and single-dye craft yields exactly enum literal.

**White-wool question resolved opposite to guess.** White's `16383998` cannot
collide with undyed item, because "undyed" is *absence* of component, not white. The
real finding was narrower: with all 16 wools spoken for, undyed sweater had no recipe and became
uncraftable — which prompted per-colour redesign.

**All 17 files then deleted.** S9 replaced single dyeable item with 16 registered variants,
so recipes now emitted per variant by datagen — same two shapes, but `result.id` names the
variant and carries no component. The `group` fields (`wolf_sweater` / `dyed_armor`) survived so
recipe book still collapses them.

## S8 — Freeze immunity for the wearer — **done**

**Build.** Sweatered wolf immune to cold — which is exactly `DamageTypes.FREEZE` (powder snow)
in 26.2. No Java. `LivingEntity.canFreeze()` (`LivingEntity.java:3913`) walks
`EquipmentSlotGroup.ARMOR` and returns false if any item there is in
`ItemTags.FREEZE_IMMUNE_WEARABLES`; that group accepts `Type.ANIMAL_ARMOR`, i.e. wolf's `BODY`
slot (vanilla relies on this for `leather_horse_armor`). One file, merged into vanilla tag (no
`"replace": true`):

`src/main/resources/data/minecraft/tags/item/freeze_immune_wearables.json`
```json
{ "values": [ "companions:wolf_sweater" ] }
```

**Done when.** Sweatered wolf in powder snow never accumulates `TicksFrozen`, while comparable
wolves without sweater do and go on to take freeze damage.

### As built

Three-wolf test in powder snow: sweatered wolf's `TicksFrozen` stayed at 0; vanilla `wolf_armor` and
bare wolves both hit 140 cap. Vanilla wolf armor confers no freeze protection (only leather gear
in tag), which makes it good control.

- **Freeze damage lags visual by wide margin.** Shaking starts at `TicksFrozen > 0`, but
  damage only begins at 140 ticks (7 s) and then applies 1 HP per 40 ticks. Wolves also flee powder
  snow after first damage tick, so expect one tick per voluntary entry, not steady drain.
- **`@e` selector output order not stable between commands** — reading `Health` across three
  wolves in one query and comparing to earlier query misattributes values. Select by
  distinguishing NBT predicate per wolf.

**Amended by S9:** tag now holds `#companions:wolf_sweaters`, so it follows generated
variant list without further edits.

## S9 — Per-colour variants from datagen — **done**

Reverses S4's central decision. Each of 16 dye colours gets `<Colour> Wolf Sweater` item,
**undyed by default but still dye-able on top** — two independently-coloured regions per sweater: a
body whose colour is variant's identity, and trim player dyes. Because equipment tint
resolved once per stack (see Findings), body colour has to be baked into texture; a
zero-texture data-only solution is impossible.

**Build.**

- **Java** — `WolfSweater.register()` loops `DyeColor.values()` and registers
  `<colour>_wolf_sweater` for each, with `name(colour)`/`modelId(colour)`/`assetId(colour)` deriving
  every id from `getSerializedName()`. Single `companions:wolf_sweater` item removed;
  `PolymerModelItem` still backs every variant.
- **Assets** — `./gradlew generateAssets` (thin alias for Fabric `runDatagen`, from client-only
  datagen source set), **not wired into `build`**: run after clone or when base art or
  colour list changes. Output lands in gitignored `src/main/generated/`, picked up as resource
  dir by Loom. Six providers under
  `src/datagen/java/club/rainbowkitty/companions/datagen/`:
  - `SweaterTextureProvider` — per-colour tinted icon and body PNGs. Reads shared base art from
    the classpath (datagen's working dir is the client run folder, but `runDatagen` depends on
    `:classes` → `:processResources`, which copies `src/main/resources` onto the classpath), and
    tints with `ARGB.multiply(pixel, colour)` — the same per-channel multiply the client applies to
    a dyeable layer, so a baked base and a live-tinted trim agree pixel-for-pixel. Colours come
    straight from `DyeColor.getTextureDiffuseColor()`, no duplicated constant list.
  - `SweaterModelProvider` — item definitions (the `has_component: minecraft:dyed_color` split) and
    plain/dyed item models.
  - `SweaterEquipmentProvider` — equipment assets: always-shown base layer plus overlay that
    only renders once dyed (`EquipmentClientInfo.Layer.onlyIfDyed`).
  - `SweaterRecipeProvider` — the two S6 shapes per variant.
  - `SweaterTagProvider` — `companions:wolf_sweaters`.
  - `SweaterLanguageProvider` — one shared `lang/en_us.json`.
- **Freeze tag** — S8's file points at `#companions:wolf_sweaters`.
- **Build guard** — `shadowJar` has `doFirst` that fails with `Run: ./gradlew generateAssets` if
  `src/main/generated/assets/companions/` missing its white variant. Cannot live in
  `processResources`: `runDatagen` depends on `processResources`, so that would be circular.

**Done when.** `./gradlew generateAssets && ./gradlew build` succeeds from fresh clone; guard
fires when `src/main/generated` moved aside; and in-world, undyed variant shows its own baked
colour while applying dye recolours only its trim.

### As built

Built as described. Generator reimplemented as six Fabric datagen providers above,
replacing earlier hand-rolled `build.gradle` imageio task — same output shape, but on datagen
classpath and using `ARGB.multiply` rather than Groovy channel multiply. `generateAssets` writes
~160 files. Tint maths checked against real pixel: red variant's base pixel came out
`(99, 25, 21)` = source `(144,144,144)` × red `(176,46,38)` / 255. Defense 17 confirmed in-world
(sweater `17.0`, `wolf_armor` `11.0`, bare `0.0`), which also proved generated lang file —
item reported as "Red Wolf Sweater".

- **Undyed variants first showed uncoloured overlay burying baked base.** In this art the
  overlay is strict subset sitting on top of base (covering ~63% of icon's opaque pixels,
  ~63% of body's), opposite of vanilla wolf armor where overlay is large dyeable
  region. Fixed by **not drawing overlay until item is dyed**: item definition keeps its
  `minecraft:condition` on `has_component: minecraft:dyed_color`, and equipment overlay uses
  `onlyIfDyed`. Absent dyed colour resolves to `0`, and `EquipmentLayerRenderer` skips any layer
  whose colour is `0`.
- **Task-graph ordering:** `generateAssets build` in one invocation lets `processResources`/`jar`
  run before `generateAssets`, so generated assets miss jar. Run `generateAssets` first,
  then `build`. `sourcesJar mustRunAfter runDatagen` handles task-validation half when both land
  in one graph.
- Base textures under `src/main/resources/assets/companions/textures/` are datagen **input**,
  read from classpath — every shipped variant references generated copy, but providers
  need base art present.

## S7 — Standalone-install proof + docs — **done**

**Build.** Copy only shaded `companions-<version>.jar` into scratch mods folder with no rkcore
jar, boot it, confirm item registers/crafts/equips. Then add rkcore's jar and confirm both run
side by side with no duplicate-class or duplicate-mixin warnings. Add `rkc/mods/companions` row to
`CLAUDE.md`'s mods table, a "Sharing code between mods" note (relocation, why, and that rkcore must
be republished after any `common/item` change), and document `./gradlew generateAssets` as
required post-clone step.

**Done when.** Both boots succeed cleanly and `CLAUDE.md` reflects new mod. Live deploy from
here means shipping only `companions-<version>.jar` unless rkcore's own features also wanted.

### As built

Both boots passed; `CLAUDE.md` updated. This stage is first and only thing that ever ran the
relocated bytecode — `runServer` resolves shared classes from compile classpath under their
original rkcore package names.

- **Scratch server had to be assembled, not just populated.** No Fabric server in this
  workspace outside Loom's dev environment, and sandbox cannot reach `maven.fabricmc.net`. The
  live server's own artifacts supplied every piece — Fabric launcher jar, `libraries/`, and
  `.fabric/server/26.2-server.jar` (without which launcher hangs on network timeout).
  Verification ran over RCON via Python client in `scripts/devserver.sh`.
- **Alone.** 53 mods, `Companions loaded!`, 0 mixin failures, no `duplicate`/`conflict`/
  `LinkageError`. All 16 colour ids resolved (deliberate `companions:nonexistent_wolf_sweater`
  answering `Unknown item` is what makes other 16 mean something). Crafting proved with
  crafter: six `red_wool` → one `companions:red_wolf_sweater`; re-run with `blue_dye` →
  `minecraft:dyed_color: 3949738`, vanilla blue literal.
- **Side by side.** Adding `rkcore-0.1.0.jar`: 54 mods, both entrypoints load, 0 mixin failures,
  0 ERROR lines. The two jars define
  `club/rainbowkitty/rkcore/common/item/ItemRegistration.class` and
  `club/rainbowkitty/companions/shaded/rkcore/item/ItemRegistration.class` respectively — nothing
  for classloader to disagree about.
- **Two scratch-server traps:** `level-type=minecraft:flat` with `generator-settings={}` builds
  world with no layers (`No key layers in MapLike[{}]`), so wolves fell out of world; and
  reading `attribute … armor get` in same RCON batch as `summon` returns `0.0` because
  equipment modifier applies on following tick.

## Open decisions

- ~~Shadow-plugin version vs Gradle 9.5.1 + Loom.~~ Resolved in S3: `9.6.1` works.
- ~~`WolfArmor*` Java naming.~~ Resolved in S4: replaced with `WolfSweater` / `PolymerModelItem`.
- ~~Colour variants as separate items.~~ Resolved twice, opposite directions: S4 collapsed them into
  one `dyed_color` item, S9 restored 16 registered variants because variant colour and dye
  colour must be independent.
- ~~One `#minecraft:wool` recipe vs 16 colour-specific.~~ Resolved as 16; S9 made them per-variant
  and generated.
- Placeholder art. Base textures still vanilla-identical copies; mod not visually
  finished until real sweater art replaces them. Rerun `generateAssets` afterwards.

## Reference: verified facts

- `Item.Properties.wolfArmor(ArmorMaterial)` — `Item.java:558-574`; `.setId(ResourceKey<Item>)` —
  `Item.java:629`.
- `DataComponents.UNBREAKABLE` (`DataComponentType<Unit>`) — `DataComponents.java:119`;
  `Unit.INSTANCE` — `Unit.java:9`.
- `ArmorMaterial` record fields, in order: `durability, defense, enchantmentValue, equipSound,
  toughness, knockbackResistance, repairIngredient, assetId` — `ArmorMaterial.java:15`.
- `EquipmentAssets.createId` is `ResourceKey.create(ROOT_ID, id)`, not a populated registry.
- Vanilla wolf-armor recipe shape/material —
  `decompiledjava/minecraft-26.2/data/minecraft/recipe/wolf_armor.json`.
- Polymer's `COMPONENTS_TO_COPY` includes `EQUIPPABLE`/`DAMAGE`/`MAX_DAMAGE`/`DYED_COLOR`, excludes
  `UNBREAKABLE` — `PolymerItemUtils.java:126-182`. `PolymerResourcePackUtils.addModAssets(modId)`
  delivers a mod's `assets/` tree as a resource pack with no per-file registration.
- An item model is reached through an *item definition* at `assets/<ns>/items/<name>.json`, which
  names a model under `models/item/`. Both files required. Vanilla example, including the
  dyed/undyed `minecraft:condition` split — `assets/minecraft/items/wolf_armor.json`.
- Equipment layers take `"dyeable": {}` to opt into `DYED_COLOR` tinting —
  `assets/minecraft/equipment/armadillo_scute.json`.
- `DyedItemColor` is `record DyedItemColor(int rgb)` — command form is a bare integer
  (`[minecraft:dyed_color=16711680]`) — `DyedItemColor.java:22`. `applyDyes` with one dye and no
  existing colour passes channels through unchanged (`colorCount = 1`).
- Dyeing is the datapack recipe type `minecraft:crafting_dye` (`target`/`dye`/`result`) — sets
  `DYED_COLOR` on the result — `DyeRecipe.java`, `data/minecraft/recipe/wolf_armor_dyed.json`.
- The 16 dye RGB literals — `DyeColor.java:30-45`, field `textureDiffuseColor` (held as
  `ARGB.opaque(...)`).
- Entity `Owner` NBT must be a UUID int-array; a string fails with `Failed to decode value ... Not a
  list` and the entity is discarded even though `/summon` reports success.
- Equipment tint is resolved **once per stack**, not per layer —
  `EquipmentLayerRenderer.getColorForLayer:111` reads `DyedItemColor.getOrDefault(stack, 0)` and
  skips any layer whose resolved colour is `0`. `EquipmentClientInfo.Dyeable`'s only knob is
  `colorWhenUndyed`, a fallback for that same single value (`EquipmentClientInfo.java:75`).
- `Wolf.canArmorAbsorb` (`Wolf.java:427`) hardcodes `is(Items.WOLF_ARMOR)`, and `Wolf.actuallyHurt`
  (`Wolf.java:409`) skips `super` in that branch — vanilla wolf armor is 100% negation plus
  durability loss, not armor points. Its 11 points apply only to `#minecraft:bypasses_wolf_armor`.
- `Attributes.ARMOR` is `RangedAttribute(…, 0.0, 0.0, 30.0)` (`Attributes.java:13`);
  `CombatRules.MAX_ARMOR = 20.0` clamps post-penetration `realArmor`. Reduction against
  unpenetrating damage is `armor / 25`.