package com.example.deathsound;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DeathSoundPlugin extends JavaPlugin implements Listener, TabExecutor {

    private static final float MIN_PITCH = 0.5f;
    private static final float MAX_PITCH = 2.0f;
    private static final int MAX_SEARCH_RESULTS = 20;
    private static final int MAX_TAB_RESULTS = 50;

    /** Easy names for popular sounds. Any other sound in the game can be used by its full name. */
    private static final Map<String, String> PRESETS = new LinkedHashMap<>();

    static {
        PRESETS.put("warden_death", "entity.warden.death");
        PRESETS.put("warden_spawn", "entity.warden.emerge");
        PRESETS.put("warden_roar", "entity.warden.roar");
        PRESETS.put("warden_sonic_boom", "entity.warden.sonic_boom");
        PRESETS.put("warden_heartbeat", "entity.warden.heartbeat");
        PRESETS.put("warden_angry", "entity.warden.angry");
        PRESETS.put("wither_death", "entity.wither.death");
        PRESETS.put("wither_spawn", "entity.wither.spawn");
        PRESETS.put("wither_shoot", "entity.wither.shoot");
        PRESETS.put("dragon_death", "entity.ender_dragon.death");
        PRESETS.put("dragon_growl", "entity.ender_dragon.growl");
        PRESETS.put("elder_guardian_curse", "entity.elder_guardian.curse");
        PRESETS.put("ghast_scream", "entity.ghast.scream");
        PRESETS.put("thunder", "entity.lightning_bolt.thunder");
        PRESETS.put("totem", "item.totem.use");
        // Old config values from the first version still work
        PRESETS.put("warden", "entity.warden.death");
        PRESETS.put("wither", "entity.wither.death");
    }

    private List<String> soundNames;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        if (getCommand("deathsound") != null) {
            getCommand("deathsound").setExecutor(this);
            getCommand("deathsound").setTabCompleter(this);
        }
        getLogger().info("DeathSound enabled. Sound: " + configuredSound() + ", pitch: " + pitch());
    }

    // ------------------------------------------------------------------ events

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        if (!getConfig().getBoolean("enabled", true)) {
            return;
        }
        Player dead = event.getEntity();
        Location loc = dead.getLocation();
        World world = loc.getWorld();
        if (world == null) {
            return;
        }

        NamespacedKey key = resolve(configuredSound());
        if (key == null) {
            getLogger().warning("The sound '" + configuredSound() + "' in config.yml is not a valid sound name.");
            return;
        }
        Sound sound = buildSound(key);

        if (getConfig().getBoolean("play-for-everyone", true)) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                online.playSound(sound, Sound.Emitter.self());
            }
        } else {
            world.playSound(sound, loc.getX(), loc.getY(), loc.getZ());
        }
    }

    // ---------------------------------------------------------------- commands

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String sub = args.length == 0 ? "info" : args[0].toLowerCase(Locale.ROOT);

        switch (sub) {
            case "sound" -> {
                if (args.length < 2) {
                    send(sender, "Usage: /deathsound sound <name>   (see /deathsound list or /deathsound search)", NamedTextColor.RED);
                    return true;
                }
                NamespacedKey key = resolve(args[1]);
                if (key == null) {
                    send(sender, "That isn't a valid sound name. Try /deathsound search <word>.", NamedTextColor.RED);
                    return true;
                }
                getConfig().set("sound", args[1].toLowerCase(Locale.ROOT));
                saveConfig();
                send(sender, "Death sound set to " + key.asString() + ".", NamedTextColor.GREEN);
                if (Registry.SOUNDS.get(key) == null) {
                    send(sender, "Note: that isn't a vanilla sound. It only plays if a resource pack adds it.", NamedTextColor.YELLOW);
                }
            }
            case "pitch" -> {
                if (args.length < 2) {
                    send(sender, "Usage: /deathsound pitch <" + MIN_PITCH + "-" + MAX_PITCH + ">", NamedTextColor.RED);
                    return true;
                }
                Double value = parseNumber(args[1]);
                if (value == null) {
                    send(sender, "That isn't a number. Example: /deathsound pitch 0.8", NamedTextColor.RED);
                    return true;
                }
                if (value < MIN_PITCH || value > MAX_PITCH) {
                    send(sender, "Pitch must be between " + MIN_PITCH + " and " + MAX_PITCH + ".", NamedTextColor.RED);
                    return true;
                }
                getConfig().set("pitch", value);
                saveConfig();
                send(sender, "Pitch set to " + value + ".", NamedTextColor.GREEN);
            }
            case "volume" -> {
                if (args.length < 2) {
                    send(sender, "Usage: /deathsound volume <0.1-10>", NamedTextColor.RED);
                    return true;
                }
                Double value = parseNumber(args[1]);
                if (value == null || value < 0.1 || value > 10) {
                    send(sender, "Volume must be a number between 0.1 and 10.", NamedTextColor.RED);
                    return true;
                }
                getConfig().set("volume", value);
                saveConfig();
                send(sender, "Volume set to " + value + ".", NamedTextColor.GREEN);
            }
            case "test" -> {
                if (!(sender instanceof Player player)) {
                    send(sender, "Only players can use /deathsound test.", NamedTextColor.RED);
                    return true;
                }
                String which = args.length >= 2 ? args[1] : configuredSound();
                NamespacedKey key = resolve(which);
                if (key == null) {
                    send(sender, "That isn't a valid sound name.", NamedTextColor.RED);
                    return true;
                }
                player.playSound(buildSound(key), Sound.Emitter.self());
                send(sender, "Playing " + key.asString() + " at pitch " + pitch() + ".", NamedTextColor.GRAY);
            }
            case "list" -> {
                send(sender, "Easy names (you can also use any sound in the game, see /deathsound search):", NamedTextColor.GOLD);
                for (Map.Entry<String, String> e : PRESETS.entrySet()) {
                    if (e.getKey().equals("warden") || e.getKey().equals("wither")) {
                        continue;
                    }
                    send(sender, " " + e.getKey() + "  ->  " + e.getValue(), NamedTextColor.GRAY);
                }
            }
            case "search" -> {
                if (args.length < 2) {
                    send(sender, "Usage: /deathsound search <word>   e.g. /deathsound search creeper", NamedTextColor.RED);
                    return true;
                }
                String needle = args[1].toLowerCase(Locale.ROOT);
                List<String> hits = new ArrayList<>();
                for (String name : soundNames()) {
                    if (name.contains(needle)) {
                        hits.add(name);
                    }
                }
                if (hits.isEmpty()) {
                    send(sender, "No sounds contain '" + needle + "'.", NamedTextColor.RED);
                    return true;
                }
                send(sender, hits.size() + " match(es)" + (hits.size() > MAX_SEARCH_RESULTS
                        ? ", showing the first " + MAX_SEARCH_RESULTS + ". Search a longer word to narrow it."
                        : ". Click one to use it:"), NamedTextColor.GOLD);
                for (int i = 0; i < Math.min(hits.size(), MAX_SEARCH_RESULTS); i++) {
                    String name = hits.get(i);
                    sender.sendMessage(Component.text(" " + name, NamedTextColor.AQUA)
                            .clickEvent(ClickEvent.suggestCommand("/deathsound sound " + name)));
                }
            }
            case "toggle" -> {
                boolean now = !getConfig().getBoolean("enabled", true);
                getConfig().set("enabled", now);
                saveConfig();
                send(sender, "DeathSound is now " + (now ? "ON" : "OFF") + ".", NamedTextColor.GREEN);
            }
            case "reload" -> {
                reloadConfig();
                send(sender, "DeathSound config reloaded.", NamedTextColor.GREEN);
            }
            default -> {
                NamespacedKey key = resolve(configuredSound());
                send(sender, "DeathSound: " + (key == null ? configuredSound() + " (INVALID)" : key.asString())
                        + "  pitch " + pitch() + "  volume " + volume()
                        + (getConfig().getBoolean("enabled", true) ? "" : "  [OFF]"), NamedTextColor.GOLD);
                send(sender, "/deathsound sound <name>   - pick a sound (warden_death, warden_spawn, or any sound)", NamedTextColor.GRAY);
                send(sender, "/deathsound search <word>  - find any sound in the game", NamedTextColor.GRAY);
                send(sender, "/deathsound list           - easy names", NamedTextColor.GRAY);
                send(sender, "/deathsound pitch <" + MIN_PITCH + "-" + MAX_PITCH + ">  |  /deathsound volume <0.1-10>", NamedTextColor.GRAY);
                send(sender, "/deathsound test [name]    - preview (without changing anything)", NamedTextColor.GRAY);
                send(sender, "/deathsound toggle  |  /deathsound reload", NamedTextColor.GRAY);
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (String s : new String[]{"sound", "search", "list", "pitch", "volume", "test", "toggle", "reload", "info"}) {
                if (s.startsWith(args[0].toLowerCase(Locale.ROOT))) out.add(s);
            }
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("sound") || args[0].equalsIgnoreCase("test"))) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            if (prefix.startsWith("minecraft:")) {
                prefix = prefix.substring("minecraft:".length());
            }
            for (String preset : PRESETS.keySet()) {
                if (preset.startsWith(prefix) && !preset.equals("warden") && !preset.equals("wither")) {
                    out.add(preset);
                }
            }
            for (String name : soundNames()) {
                if (out.size() >= MAX_TAB_RESULTS) break;
                if (name.startsWith(prefix)) out.add(name);
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("pitch")) {
            for (String s : new String[]{"0.5", "0.75", "1.0", "1.5", "2.0"}) {
                if (s.startsWith(args[1])) out.add(s);
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("volume")) {
            for (String s : new String[]{"0.5", "1.0", "2.0", "5.0"}) {
                if (s.startsWith(args[1])) out.add(s);
            }
        }
        return out;
    }

    // ----------------------------------------------------------------- helpers

    private String configuredSound() {
        return getConfig().getString("sound", "warden_death");
    }

    /** Turns an easy name ("warden_spawn") or a sound name ("entity.creeper.primed") into a key, or null if invalid. */
    private NamespacedKey resolve(String input) {
        if (input == null) {
            return null;
        }
        String s = input.toLowerCase(Locale.ROOT).trim();
        String preset = PRESETS.get(s);
        if (preset != null) {
            s = preset;
        }
        return NamespacedKey.fromString(s);
    }

    private float pitch() {
        double p = getConfig().getDouble("pitch", 1.0);
        return (float) Math.max(MIN_PITCH, Math.min(MAX_PITCH, p));
    }

    private float volume() {
        double v = getConfig().getDouble("volume", 1.0);
        return (float) Math.max(0.1, v);
    }

    private Sound buildSound(NamespacedKey key) {
        return Sound.sound(Key.key(key.asString()), Sound.Source.MASTER, volume(), pitch());
    }

    /** All vanilla sound names (without "minecraft:"), sorted. Built once, on first use. */
    private List<String> soundNames() {
        if (soundNames == null) {
            try {
                soundNames = Registry.SOUNDS.keyStream()
                        .filter(k -> k.getNamespace().equals("minecraft"))
                        .map(NamespacedKey::getKey)
                        .sorted()
                        .toList();
            } catch (Throwable t) {
                getLogger().warning("Could not load the sound list for tab completion: " + t.getMessage());
                soundNames = List.of();
            }
        }
        return soundNames;
    }

    private static Double parseNumber(String s) {
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static void send(CommandSender sender, String text, NamedTextColor color) {
        sender.sendMessage(Component.text(text, color));
    }
}
