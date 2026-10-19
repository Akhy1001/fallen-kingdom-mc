package fr.anas.serveurFallenKingdom.base;

import fr.anas.serveurFallenKingdom.config.Messages;
import fr.anas.serveurFallenKingdom.team.Team;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * Gère les zones de bases et l'outil de sélection (F05, F06, F07).
 */
public class BaseManager {

    public static final String WAND_NAME = Messages.colorize("&6&lBaguette de Base FK");

    private final Map<String, Base> bases = new HashMap<>();
    private final Map<UUID, Location> pos1Map = new HashMap<>();
    private final Map<UUID, Location> pos2Map = new HashMap<>();

    public void setPos1(UUID playerUuid, Location loc) {
        pos1Map.put(playerUuid, loc);
    }

    public void setPos2(UUID playerUuid, Location loc) {
        pos2Map.put(playerUuid, loc);
    }

    public boolean hasCompleteSelection(UUID playerUuid) {
        Location p1 = pos1Map.get(playerUuid);
        Location p2 = pos2Map.get(playerUuid);
        return p1 != null && p2 != null && p1.getWorld() != null && p1.getWorld().equals(p2.getWorld());
    }

    /**
     * Crée une région à partir de la sélection d'un admin, étendue sur toute la hauteur du monde
     * pour éviter qu'un joueur soit hors base en sautant ou en minant sous sa base.
     */
    public Region createRegionFromSelection(UUID playerUuid) {
        if (!hasCompleteSelection(playerUuid)) return null;
        Location p1 = pos1Map.get(playerUuid);
        Location p2 = pos2Map.get(playerUuid);
        World world = p1.getWorld();
        int minY = world != null ? world.getMinHeight() : -64;
        int maxY = world != null ? world.getMaxHeight() : 320;

        return new Region(
                world.getName(),
                p1.getBlockX(), minY, p1.getBlockZ(),
                p2.getBlockX(), maxY, p2.getBlockZ()
        );
    }

    /**
     * Crée une région carrée autour d'un centre avec un rayon donné (Section 3.3).
     */
    public Region createRegionFromRadius(Location center, int radius) {
        World world = center.getWorld();
        int minY = world != null ? world.getMinHeight() : -64;
        int maxY = world != null ? world.getMaxHeight() : 320;
        int cx = center.getBlockX();
        int cz = center.getBlockZ();

        return new Region(
                world.getName(),
                cx - radius, minY, cz - radius,
                cx + radius, maxY, cz + radius
        );
    }

    public Base setBase(Team team, Region region, Location spawnLocation) {
        Base base = new Base(team.getName(), region, spawnLocation);
        bases.put(team.getName().toLowerCase(), base);
        team.setSpawnLocation(spawnLocation);
        return base;
    }

    public Base getBase(Team team) {
        if (team == null) return null;
        return bases.get(team.getName().toLowerCase());
    }

    public Base getBase(String teamName) {
        return bases.get(teamName.toLowerCase());
    }

    public Base getBaseAt(Location loc) {
        for (Base base : bases.values()) {
            if (base.contains(loc)) {
                return base;
            }
        }
        return null;
    }

    public Collection<Base> getAllBases() {
        return Collections.unmodifiableCollection(bases.values());
    }

    public static ItemStack createWand() {
        ItemStack item = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(WAND_NAME);
            meta.setLore(List.of(
                    Messages.colorize("&7Clic gauche sur un bloc : &eCoin 1"),
                    Messages.colorize("&7Clic droit sur un bloc : &eCoin 2"),
                    Messages.colorize("&7Puis tapez : &a/fk base set <équipe>")
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    public static boolean isWand(ItemStack item) {
        if (item == null || item.getType() != Material.BLAZE_ROD) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && WAND_NAME.equals(meta.getDisplayName());
    }
}
