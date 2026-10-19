# Building

```
./gradlew generateAssets    # once after cloning, and whenever datagen or its inputs change
./gradlew build             # versions/<v>/build/libs/companions-<version>+<v>.jar, one per version
```

The house build is documented once for every first-party mod in the workspace's `docs/STYLE.md`.
This file covers only where this mod departs from it.

- **Multi-version build** (Stonecutter): 26.2 and 26.3, with 26.2 committed. Each version's keys
  are in `versions/<v>/gradle.properties`.
  - 26.3 builds against Polymer 0.18.4 from Maven and Fabric Loader 0.19.5. 0.19.3's MixinExtras
    crashes the datagen client on Polymer 0.18's palette mixin.
  - Dev servers are on ports 25602/25578 (26.2) and 25812/25814 (26.3).
- **Datagen is per version.** `generateAssets` boots a client per version and writes every
  per-colour model, item definition, equipment asset, tinted texture, recipe, tag and lang entry
  into that version's gitignored `versions/<v>/src/main/generated/`.
  - Run it as its own invocation, never `generateAssets build` in one: `processResources` runs
    first and the jar would ship without the new output.
  - `build` fails with a pointed message when a version's output is missing.
  - A `src/main/generated/` at the mod root is left over from a single-version build and is
    ignored.
- **`mavenLocal()` dependency.** kalic0re's `common/armor`, `common/item` and `common/datagen`
  packages are relocated into the jar from `club.rainbowkitty.kalic0re:kalic0re:<kalic0re_version>`
  in `~/.m2`: `0.13.0` for 26.2 and `0.31.0+26.3` for 26.3.
  - 26.3's comes from kalic0re's multi-version build. 0.13.0 predates that; `0.31.0+26.2` builds
    the same assets if it has to stand in.
  - A stale copy shows up as a compile error naming a missing kalic0re class or method.
- **The jar to install** is `companions-<version>+<v>.jar` for the server's version. `-unshaded`
  and `-sources` beside it are not.
