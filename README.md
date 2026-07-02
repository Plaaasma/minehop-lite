# minehop-lite (Antropov31 fork)

A fork of [Plaaasma/minehop-lite](https://github.com/Plaaasma/minehop-lite), a Fabric mod that brings Source-engine style movement (bunny hopping / strafe jumping / surf) to Minecraft.

**Maintainer of this fork:** [Antropov31](https://github.com/Antropov31)

## What this fork changes

- **Targets Minecraft 26.2 "Chaos Cubed"** on a dedicated `26.2` branch (based on the upstream `1.21.4` branch).
- **Automated releases:** pushing a tag matching `v*` triggers a GitHub Actions workflow that builds the mod with Gradle and publishes the resulting `.jar` straight to [Releases](https://github.com/Antropov31/minehop-lite/releases).

## Branches

| Branch    | Minecraft | Status |
|-----------|-----------|--------|
| `26.2`    | 26.2      | Port in progress |
| `1.21.4`  | 1.21.4    | Inherited from upstream |
| `1.20.4`  | 1.20.4    | Inherited from upstream |

> Note on 26.2: starting with the 26.x series Minecraft moved to a year-based version scheme and switched from obfuscated builds + Yarn mappings to official Mojang mappings. Porting from `1.21.4` therefore involves migrating mappings and updating mixin targets, not just bumping version numbers. This branch tracks that work.

## Building

```bash
./gradlew build
```

The built jar lands in `build/libs/`.

## Releasing

```bash
git tag v<version>
git push origin v<version>
```

CI takes it from there: builds the jar and attaches it to a new GitHub Release.

## Credits

- Original mod: [Plaaasma](https://github.com/Plaaasma)
- Fork & 26.2 work: [Antropov31](https://github.com/Antropov31)
