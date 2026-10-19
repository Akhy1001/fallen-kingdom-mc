package fr.anas.serveurFallenKingdom.config;

import fr.anas.serveurFallenKingdom.ServeurFallenKingdom;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

/**
 * Gestionnaire des messages traduisibles depuis messages.yml.
 */
public class Messages {

    private final ServeurFallenKingdom plugin;
    private File file;
    private FileConfiguration config;

    public Messages(ServeurFallenKingdom plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    public String get(String key) {
        String prefix = config.getString("prefix", "&6[FK] &r");
        String msg = config.getString(key, "&cMessage introuvable: " + key);
        return colorize(prefix + msg);
    }

    public String getRaw(String key) {
        return colorize(config.getString(key, key));
    }

    public static String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
