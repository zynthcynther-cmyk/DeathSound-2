# DeathSound (Paper 26.3)

Plays a sound of your choice when a player dies: Warden death, Warden spawn, Wither, or ANY sound in the game.
Pitch and volume are adjustable.

## Build with GitHub (no install needed)

1. Create a GitHub repo and upload everything in this folder.
   - `.github` is a hidden folder. On Mac press Cmd+Shift+. in Finder to show it, then drag it in.
   - If it still won't upload: in the repo click Add file > Create new file, type `.github/workflows/build.yml`
     as the name and paste in the contents of `.github/workflows/build.yml` from this zip.
   - Updating your existing DeathSound repo? Only these files changed: `src/main/java/.../DeathSoundPlugin.java`,
     `src/main/resources/config.yml`, `src/main/resources/plugin.yml`. The workflow stays the same.
2. Open the Actions tab. The build starts by itself (or click Run workflow).
3. When it shows a green check, open the run and download **DeathSound** under Artifacts.
4. Unzip it and put only `DeathSound.jar` in your server's `plugins` folder, then restart.

Needs Paper 26.3+ and Java 25.

## Commands (op only, permission `deathsound.admin`)
- `/deathsound sound <name>`: pick the sound. Use an easy name like `warden_death` or `warden_spawn`,
  or any game sound like `entity.creeper.primed`
- `/deathsound search <word>`: find any sound in the game (click a result to use it)
- `/deathsound list`: show the easy names
- `/deathsound pitch <0.5-2.0>`: pitch (1.0 = normal, lower = deeper)
- `/deathsound volume <0.1-10>`: volume
- `/deathsound test [name]`: preview the current sound, or any sound without changing anything
- `/deathsound toggle`: turn the sound on/off
- `/deathsound reload`: reload config.yml
- `/deathsound`: show current settings

Changes made with commands are saved to `plugins/DeathSound/config.yml`.

Note: if your server already has a `plugins/DeathSound/config.yml` from the first version, it keeps working
(`warden` and `wither` still map to their death sounds). New options use their defaults.
