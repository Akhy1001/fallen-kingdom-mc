package fr.anas.serveurFallenKingdom.command;

import fr.anas.serveurFallenKingdom.base.BaseManager;
import fr.anas.serveurFallenKingdom.base.Region;
import fr.anas.serveurFallenKingdom.config.ConfigService;
import fr.anas.serveurFallenKingdom.config.Messages;
import fr.anas.serveurFallenKingdom.game.GameManager;
import fr.anas.serveurFallenKingdom.game.GameState;
import fr.anas.serveurFallenKingdom.listener.ChatListener;
import fr.anas.serveurFallenKingdom.team.Team;
import fr.anas.serveurFallenKingdom.team.TeamManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Commande principale /fk et auto-complétion (Section 5).
 */
public class FkCommand implements CommandExecutor, TabCompleter {

    private final ConfigService configService;
    private final Messages messages;
    private final GameManager gameManager;
    private final TeamManager teamManager;
    private final BaseManager baseManager;

    private static final List<String> ALLOWED_COLORS = List.of(
            "RED", "BLUE", "GREEN", "YELLOW", "AQUA", "LIGHT_PURPLE", "GOLD", "DARK_GREEN", "DARK_AQUA", "WHITE"
    );

    public FkCommand(ConfigService configService, Messages messages, GameManager gameManager,
                     TeamManager teamManager, BaseManager baseManager) {
        this.configService = configService;
        this.messages = messages;
        this.gameManager = gameManager;
        this.teamManager = teamManager;
        this.baseManager = baseManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sendInfo(sender);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "info" -> {
                if (!sender.hasPermission("fk.player")) {
                    sender.sendMessage(messages.get("no-permission"));
                    return true;
                }
                sendInfo(sender);
            }
            case "reload" -> {
                if (!sender.hasPermission("fk.admin")) {
                    sender.sendMessage(messages.get("no-permission"));
                    return true;
                }
                if (gameManager.getState() == GameState.RUNNING) {
                    sender.sendMessage(messages.get("command-locked-running"));
                    return true;
                }
                configService.reload();
                messages.load();
                sender.sendMessage(messages.get("config-reloaded"));
            }
            case "join" -> handleJoin(sender, args);
            case "team" -> handleTeam(sender, args);
            case "base" -> handleBase(sender, args);
            default -> sender.sendMessage(messages.get("unknown-command"));
        }
        return true;
    }

    private void handleBase(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messages.get("only-players"));
            return;
        }
        if (!player.hasPermission("fk.admin")) {
            player.sendMessage(messages.get("no-permission"));
            return;
        }
        if (gameManager.getState() == GameState.RUNNING) {
            player.sendMessage(messages.get("command-locked-running"));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(Messages.colorize("&cUsage: /fk base <wand|set> [équipe] [rayon]"));
            return;
        }

        String action = args[1].toLowerCase();
        if (action.equals("wand")) {
            player.getInventory().addItem(BaseManager.createWand());
            player.sendMessage(messages.get("wand-given"));
            return;
        }

        if (action.equals("set")) {
            if (args.length < 3) {
                player.sendMessage(Messages.colorize("&cUsage: /fk base set <équipe> [rayon]"));
                return;
            }
            Team team = teamManager.getTeam(args[2]);
            if (team == null) {
                player.sendMessage(messages.get("team-not-found"));
                return;
            }

            Region region;
            // Option 1 : /fk base set <équipe> <rayon> (autour de l'admin)
            if (args.length >= 4) {
                try {
                    int radius = Integer.parseInt(args[3]);
                    if (radius <= 0) throw new NumberFormatException();
                    region = baseManager.createRegionFromRadius(player.getLocation(), radius);
                } catch (NumberFormatException e) {
                    player.sendMessage(Messages.colorize("&cLe rayon doit être un nombre entier positif."));
                    return;
                }
            } else {
                // Option 2 : /fk base set <équipe> (avec les 2 coins de la baguette)
                if (!baseManager.hasCompleteSelection(player.getUniqueId())) {
                    player.sendMessage(messages.get("selection-incomplete"));
                    return;
                }
                region = baseManager.createRegionFromSelection(player.getUniqueId());
            }

            baseManager.setBase(team, region, player.getLocation());
            player.sendMessage(messages.get("base-set").replace("{team}", team.getColoredName()));
            return;
        }

        player.sendMessage(Messages.colorize("&cUsage: /fk base <wand|set> [équipe] [rayon]"));
    }

    private void handleJoin(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messages.get("only-players"));
            return;
        }
        if (!player.hasPermission("fk.player")) {
            player.sendMessage(messages.get("no-permission"));
            return;
        }
        if (gameManager.getState() == GameState.RUNNING) {
            player.sendMessage(messages.get("command-locked-running"));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(Messages.colorize("&cUsage: /fk join <équipe>"));
            return;
        }
        Team team = teamManager.getTeam(args[1]);
        if (team == null) {
            player.sendMessage(messages.get("team-not-found"));
            return;
        }
        if (teamManager.isTeamFull(team)) {
            player.sendMessage(messages.get("team-full"));
            return;
        }
        teamManager.addPlayerToTeam(player, team);
        player.sendMessage(messages.get("team-joined")
                .replace("{player}", player.getName())
                .replace("{team}", team.getColoredName()));
    }

    private void handleTeam(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Messages.colorize("&cUsage: /fk team <create|delete|add|random|chat>"));
            return;
        }

        String action = args[1].toLowerCase();

        if (action.equals("chat")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(messages.get("only-players"));
                return;
            }
            Team team = teamManager.getPlayerTeam(player.getUniqueId());
            if (team == null) {
                player.sendMessage(messages.get("no-team"));
                return;
            }
            if (args.length < 3) {
                player.sendMessage(Messages.colorize("&cUsage: /fk team chat <message>"));
                return;
            }
            String message = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
            ChatListener.sendTeamMessage(player, team, message);
            return;
        }

        if (!sender.hasPermission("fk.admin")) {
            sender.sendMessage(messages.get("no-permission"));
            return;
        }
        if (gameManager.getState() == GameState.RUNNING) {
            sender.sendMessage(messages.get("command-locked-running"));
            return;
        }

        switch (action) {
            case "create" -> {
                if (args.length < 4) {
                    sender.sendMessage(Messages.colorize("&cUsage: /fk team create <nom> <couleur>"));
                    return;
                }
                if (!teamManager.canCreateMoreTeams()) {
                    sender.sendMessage(messages.get("team-max-reached"));
                    return;
                }
                String name = args[2];
                if (teamManager.getTeam(name) != null) {
                    sender.sendMessage(messages.get("team-exists"));
                    return;
                }
                ChatColor color;
                try {
                    color = ChatColor.valueOf(args[3].toUpperCase());
                    if (!color.isColor()) throw new IllegalArgumentException();
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(messages.get("invalid-color"));
                    return;
                }
                Team created = teamManager.createTeam(name, color);
                sender.sendMessage(messages.get("team-created").replace("{team}", created.getColoredName()));
            }
            case "delete" -> {
                if (args.length < 3) {
                    sender.sendMessage(Messages.colorize("&cUsage: /fk team delete <nom>"));
                    return;
                }
                if (!teamManager.deleteTeam(args[2])) {
                    sender.sendMessage(messages.get("team-not-found"));
                    return;
                }
                sender.sendMessage(messages.get("team-deleted").replace("{team}", args[2]));
            }
            case "add" -> {
                if (args.length < 4) {
                    sender.sendMessage(Messages.colorize("&cUsage: /fk team add <joueur> <équipe>"));
                    return;
                }
                Player target = Bukkit.getPlayerExact(args[2]);
                if (target == null) {
                    sender.sendMessage(Messages.colorize("&cJoueur introuvable ou hors-ligne."));
                    return;
                }
                Team team = teamManager.getTeam(args[3]);
                if (team == null) {
                    sender.sendMessage(messages.get("team-not-found"));
                    return;
                }
                if (teamManager.isTeamFull(team)) {
                    sender.sendMessage(messages.get("team-full"));
                    return;
                }
                teamManager.addPlayerToTeam(target, team);
                sender.sendMessage(messages.get("team-joined")
                        .replace("{player}", target.getName())
                        .replace("{team}", team.getColoredName()));
            }
            case "random" -> {
                if (teamManager.getAllTeams().isEmpty()) {
                    sender.sendMessage(Messages.colorize("&cCréez d'abord des équipes avant de lancer la répartition."));
                    return;
                }
                teamManager.assignRandomly();
                sender.sendMessage(messages.get("team-random-done"));
            }
            default -> sender.sendMessage(Messages.colorize("&cUsage: /fk team <create|delete|add|random|chat>"));
        }
    }

    private void sendInfo(CommandSender sender) {
        sender.sendMessage(Messages.colorize("&6&m---&r &6&lFallen Kingdom - Infos &6&m---"));
        sender.sendMessage(Messages.colorize("&eÉtat : &f" + gameManager.getState().name()));
        sender.sendMessage(Messages.colorize("&eJour actuel : &fJ" + gameManager.getCurrentDay()
                + " &7(" + configService.getDayDurationMinutes() + " min/jour)"));
        sender.sendMessage(Messages.colorize("&eDéblocages : "
                + formatUnlock("PvP (J" + configService.getPvpDay() + ")", gameManager.isPvpUnlocked()) + " &7| "
                + formatUnlock("Nether (J" + configService.getNetherDay() + ")", gameManager.isNetherUnlocked()) + " &7| "
                + formatUnlock("Assauts (J" + configService.getAssaultDay() + ")", gameManager.isAssaultUnlocked()) + " &7| "
                + formatUnlock("End (J" + configService.getEndDay() + ")", gameManager.isEndUnlocked())));

        if (teamManager.getAllTeams().isEmpty()) {
            sender.sendMessage(Messages.colorize("&eÉquipes : &7Aucune équipe créée."));
        } else {
            sender.sendMessage(Messages.colorize("&eÉquipes (" + teamManager.getAllTeams().size() + ") :"));
            for (Team t : teamManager.getAllTeams()) {
                boolean hasBase = baseManager.getBase(t) != null;
                sender.sendMessage(Messages.colorize(" &7- " + t.getColoredName()
                        + " &7(" + t.getSize() + "/" + configService.getMaxPlayersPerTeam() + " joueurs) "
                        + (hasBase ? "&a[Base OK]" : "&c[Sans Base]")));
            }
        }
    }

    private String formatUnlock(String name, boolean unlocked) {
        return (unlocked ? "&a✔ " : "&c✘ ") + name;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subs = new ArrayList<>(List.of("info", "join", "team"));
            if (sender.hasPermission("fk.admin")) {
                subs.addAll(List.of("reload", "base", "altar", "start", "pause", "resume", "stop", "day"));
            }
            return filter(subs, args[0]);
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("join")) {
                return filter(teamManager.getAllTeams().stream().map(Team::getName).toList(), args[1]);
            }
            if (args[0].equalsIgnoreCase("team")) {
                List<String> actions = new ArrayList<>(List.of("chat"));
                if (sender.hasPermission("fk.admin")) {
                    actions.addAll(List.of("create", "delete", "add", "random"));
                }
                return filter(actions, args[1]);
            }
            if (args[0].equalsIgnoreCase("base") && sender.hasPermission("fk.admin")) {
                return filter(List.of("wand", "set"), args[1]);
            }
        }

        if (args.length == 3 && sender.hasPermission("fk.admin")) {
            if (args[0].equalsIgnoreCase("team") && args[1].equalsIgnoreCase("delete")) {
                return filter(teamManager.getAllTeams().stream().map(Team::getName).toList(), args[2]);
            }
            if (args[0].equalsIgnoreCase("team") && args[1].equalsIgnoreCase("add")) {
                return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), args[2]);
            }
            if (args[0].equalsIgnoreCase("base") && args[1].equalsIgnoreCase("set")) {
                return filter(teamManager.getAllTeams().stream().map(Team::getName).toList(), args[2]);
            }
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("team") && sender.hasPermission("fk.admin")) {
            if (args[1].equalsIgnoreCase("create")) {
                return filter(ALLOWED_COLORS, args[3]);
            }
            if (args[1].equalsIgnoreCase("add")) {
                return filter(teamManager.getAllTeams().stream().map(Team::getName).toList(), args[3]);
            }
        }

        return completions;
    }

    private List<String> filter(List<String> list, String input) {
        String lower = input.toLowerCase();
        List<String> result = new ArrayList<>();
        for (String s : list) {
            if (s.toLowerCase().startsWith(lower)) {
                result.add(s);
            }
        }
        return result;
    }
}
