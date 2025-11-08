package com.supafloof;

// Bukkit/Spigot/Paper API imports
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.Bukkit;

// Adventure API for modern text components
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

// LuckPerms API for permission management
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;

// Vault API for economy integration
import net.milkbowl.vault.economy.Economy;

// Java standard library imports
import java.text.DecimalFormat;
import java.util.*;

/**
 * Main plugin class for Ranks
 * 
 * This plugin provides a comprehensive rank progression system with economy integration.
 * Players can upgrade through ranks using in-game currency, with automatic LuckPerms
 * group management and support for both single-step and multi-step rank upgrades.
 * 
 * Features:
 * - Command: /ranks - Display all ranks with progress and costs
 * - Command: /ranks help - Show formatted help information
 * - Command: /ranks reload - Reload configuration (OP only)
 * - Command: /ranks next - Upgrade to the next rank
 * - Command: /ranks upgrade <rank> - Jump to a specific rank (cumulative cost)
 * - Automatic migration from "default" group to first rank on player join
 * - Permission-based access: ranks.use for basic commands, ranks.reload for config reload
 * - Visual rank display with completion status, costs, and progress indicators
 * - Economy integration via Vault for rank purchase transactions
 * - LuckPerms integration for automatic group permission management
 * - Configurable rank hierarchy with customizable costs, broadcasts, and commands
 * - Broadcast messages on rank-up with variable substitution
 * 
 * @author SupaFloof Games, LLC
 * @version 1.0.0
 */
public class Ranks extends JavaPlugin implements Listener {

    /**
     * Vault Economy instance for handling currency transactions.
     * Used to check player balances and withdraw money for rank purchases.
     */
    private Economy economy = null;
    
    /**
     * LuckPerms API instance for managing player groups and permissions.
     * Used to automatically add/remove group permissions when players rank up.
     */
    private LuckPerms luckPerms = null;
    
    /**
     * Decimal formatter for displaying currency amounts.
     * Formats numbers with comma separators (e.g., 1,000,000).
     */
    private final DecimalFormat moneyFormat = new DecimalFormat("#,###.##");

    /**
     * Called when the plugin is enabled during server startup or plugin load.
     * 
     * This method:
     * 1. Saves the default config.yml if it doesn't exist
     * 2. Sets up Vault economy integration
     * 3. Sets up LuckPerms API integration
     * 4. Registers the /ranks command and its executor
     * 5. Registers event listeners for player join events
     * 6. Processes all online players for default group migration (hot reload case)
     * 7. Displays startup messages to console
     */
    @Override
    public void onEnable() {
        // Save default config.yml from resources if it doesn't exist
        saveDefaultConfig();
        
        // Setup Vault economy - required dependency
        if (!setupEconomy()) {
            getLogger().severe("Vault not found! This plugin requires Vault and an economy plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Setup LuckPerms API - required dependency
        if (!setupLuckPerms()) {
            getLogger().severe("LuckPerms not found! This plugin requires LuckPerms.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Register the command executor for /ranks
        RanksCommand commandExecutor = new RanksCommand(this);
        this.getCommand("ranks").setExecutor(commandExecutor);
        
        // Register this class as an event listener for PlayerJoinEvent
        getServer().getPluginManager().registerEvents(this, this);
        
        // Handle the case where the plugin is loaded while players are already online
        // (e.g., during a hot reload). Process them for default group migration.
        for (Player player : Bukkit.getOnlinePlayers()) {
            processPlayer(player);
        }
        
        // Display startup messages to console in the specified colors
        // Green message: Plugin started successfully
        getServer().getConsoleSender().sendMessage(
            Component.text("[Ranks] Ranks Started!")
                .color(NamedTextColor.GREEN)
        );
        // Magenta message: Plugin author credit
        getServer().getConsoleSender().sendMessage(
            Component.text("[Ranks] By SupaFloof Games, LLC")
                .color(NamedTextColor.LIGHT_PURPLE)
        );
    }

    /**
     * Called when the plugin is disabled during server shutdown or plugin unload.
     * 
     * This method displays a shutdown message to console.
     */
    @Override
    public void onDisable() {
        // Display shutdown message to console
        getServer().getConsoleSender().sendMessage(
            Component.text("[Ranks] Ranks Disabled!")
                .color(NamedTextColor.RED)
        );
    }

    /**
     * Sets up Vault economy integration.
     * 
     * This method checks if Vault is installed and retrieves the Economy service provider.
     * The economy provider is used for all currency transactions (checking balance, withdrawing money).
     * 
     * @return true if economy was successfully set up, false otherwise
     */
    private boolean setupEconomy() {
        // Check if Vault plugin is present
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        
        // Get the Economy service provider from Vault
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        
        economy = rsp.getProvider();
        return economy != null;
    }

    /**
     * Sets up LuckPerms API integration.
     * 
     * This method retrieves the LuckPerms API instance which is used for managing
     * player groups and permissions. When players rank up, this API is used to
     * add the new group and remove the old group.
     * 
     * @return true if LuckPerms was successfully set up, false otherwise
     */
    private boolean setupLuckPerms() {
        // Get the LuckPerms service provider
        RegisteredServiceProvider<LuckPerms> provider = getServer().getServicesManager().getRegistration(LuckPerms.class);
        if (provider != null) {
            luckPerms = provider.getProvider();
            getLogger().info("LuckPerms API hooked successfully!");
            return true;
        }
        return false;
    }

    /**
     * Event handler for player join events.
     * 
     * This method is called automatically when a player joins the server.
     * It processes the player asynchronously to check if they need to be
     * migrated from the "default" group to the first rank.
     * 
     * @param event The PlayerJoinEvent containing the joining player
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Run async to avoid blocking the main thread
        // Player group checking and modification can take time, especially with network latency
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> processPlayer(player));
    }

    /**
     * Processes a player for default group migration.
     * 
     * This method loads the player's LuckPerms data and checks if they are in the
     * "default" group. If they are, it automatically migrates them to the first rank
     * defined in the config (the rank with cost: 0).
     * 
     * This ensures that all players start in the rank progression system rather than
     * staying in the generic "default" group.
     * 
     * @param player The player to process
     */
    private void processPlayer(Player player) {
        // Load the user from LuckPerms asynchronously
        luckPerms.getUserManager().loadUser(player.getUniqueId()).thenAcceptAsync(user -> {
            if (user == null) {
                getLogger().warning("Could not load user data for " + player.getName());
                return;
            }

            // Check if player is in the "default" group by looking for "group.default" permission
            boolean isInDefaultGroup = user.getNodes().stream()
                    .anyMatch(node -> node.getKey().equals("group.default"));

            if (isInDefaultGroup) {
                // Get the first rank from config (the starting rank with cost: 0)
                String firstRank = getFirstRank();
                if (firstRank == null) {
                    getLogger().warning("Could not determine first rank from config!");
                    return;
                }

                getLogger().info("Player " + player.getName() + " is in 'default' group. Changing to '" + firstRank + "'...");

                // Remove from default group
                user.data().remove(Node.builder("group.default").build());

                // Add to first rank group
                user.data().add(Node.builder("group." + firstRank).build());

                // Save the user data back to LuckPerms
                luckPerms.getUserManager().saveUser(user).thenRun(() -> {
                    getLogger().info("Successfully changed " + player.getName() + " from 'default' to '" + firstRank + "' group!");
                });
            }
        });
    }

    /**
     * Determines the first rank in the rank progression system.
     * 
     * This method looks through the ranks in the config to find the starting rank.
     * It first checks for a rank with cost: 0, which indicates the free starting rank.
     * If no rank has cost 0, it returns the first rank in the progression chain.
     * 
     * @return The name of the first rank, or null if no ranks are configured
     */
    private String getFirstRank() {
        FileConfiguration config = getConfig();
        String defaultPath = config.getString("defaultpath", "default");
        ConfigurationSection ranksSection = config.getConfigurationSection("Ranks." + defaultPath);
        
        if (ranksSection == null) {
            return null;
        }

        // Find the rank with cost 0 (starting rank)
        for (String key : ranksSection.getKeys(false)) {
            ConfigurationSection section = ranksSection.getConfigurationSection(key);
            if (section != null && section.getDouble("cost", -1) == 0) {
                return key;
            }
        }
        
        // If no rank with cost 0, return the first rank in the order
        List<String> rankOrder = getRankOrder(ranksSection);
        return rankOrder.isEmpty() ? null : rankOrder.get(0);
    }

    /**
     * Gets the current rank of a player.
     * 
     * This method checks the player's LuckPerms permissions to determine which rank
     * group they belong to. It checks from highest to lowest rank to find the first
     * match, since players should only be in one rank group at a time.
     * 
     * @param player The player to check
     * @param ranksSection The configuration section containing rank definitions
     * @return The name of the player's current rank
     */
    private String getCurrentRank(Player player, ConfigurationSection ranksSection) {
        List<String> rankOrder = getRankOrder(ranksSection);
        
        // Check from highest to lowest rank to find which group the player has
        for (int i = rankOrder.size() - 1; i >= 0; i--) {
            String rank = rankOrder.get(i);
            if (player.hasPermission("group." + rank)) {
                return rank;
            }
        }
        
        // Default to first rank if no rank found
        return rankOrder.get(0);
    }

    /**
     * Builds the ordered list of ranks from the configuration.
     * 
     * This method follows the rank chain by starting with the rank that has cost: 0,
     * then following the "nextrank" field to build the complete progression chain.
     * 
     * The result is a list of rank names in order from first to last.
     * 
     * @param ranksSection The configuration section containing rank definitions
     * @return List of rank names in progression order
     */
    private List<String> getRankOrder(ConfigurationSection ranksSection) {
        List<String> order = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        
        // Find the starting rank (cost: 0)
        String current = null;
        for (String key : ranksSection.getKeys(false)) {
            ConfigurationSection section = ranksSection.getConfigurationSection(key);
            if (section != null && section.getDouble("cost", -1) == 0) {
                current = key;
                break;
            }
        }
        
        // If no rank with cost 0, start with the first key
        if (current == null && !ranksSection.getKeys(false).isEmpty()) {
            current = ranksSection.getKeys(false).iterator().next();
        }
        
        // Follow the chain of nextrank fields to build the complete order
        while (current != null && !visited.contains(current)) {
            order.add(current);
            visited.add(current);
            
            ConfigurationSection section = ranksSection.getConfigurationSection(current);
            if (section == null) break;
            
            String next = section.getString("nextrank");
            // LASTRANK indicates the final rank in the progression
            if (next == null || next.equalsIgnoreCase("LASTRANK")) {
                break;
            }
            current = next;
        }
        
        return order;
    }

    /**
     * Executes the rank-up process for a player.
     * 
     * This method:
     * 1. Runs all commands defined in the rank's executecmds list (with [console] prefix support)
     * 2. Broadcasts the rank-up message to all players (if configured)
     * 3. Or sends a simple confirmation message to the player (if no broadcast configured)
     * 
     * Commands and broadcasts support %player% variable substitution.
     * 
     * @param player The player being promoted
     * @param rankSection The configuration section of the rank being promoted to
     * @param rankName The name of the rank being promoted to
     */
    private void executeRankUp(Player player, ConfigurationSection rankSection, String rankName) {
        // Execute all commands defined for this rank
        List<String> executeCmds = rankSection.getStringList("executecmds");
        
        for (String cmd : executeCmds) {
            // Replace %player% with the actual player name
            String processedCmd = cmd.replace("%player%", player.getName());
            
            // Check for [console] prefix - indicates command should be run from console
            if (processedCmd.startsWith("[console] ")) {
                processedCmd = processedCmd.substring(10); // Remove "[console] " prefix
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processedCmd);
            } else {
                // Run command from console by default
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processedCmd);
            }
        }

        // Handle broadcast messages
        List<String> broadcast = rankSection.getStringList("broadcast");
        
        if (!broadcast.isEmpty()) {
            // Broadcast rank-up message to all players
            for (String line : broadcast) {
                // Replace %player% with the actual player name
                String processedLine = line.replace("%player%", player.getName());
                // Convert legacy & color codes to Adventure components
                Component component = LegacyComponentSerializer.legacyAmpersand().deserialize(processedLine);
                Bukkit.broadcast(component);
            }
        } else {
            // No broadcast configured - send simple confirmation to player
            String display = rankSection.getString("display", rankName);
            Component displayComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(display);
            player.sendMessage(Component.text("You have been promoted to ", NamedTextColor.GREEN)
                    .append(displayComponent)
                    .append(Component.text("!", NamedTextColor.GREEN)));
        }
    }

    /**
     * Inner class that handles the /ranks command execution.
     * 
     * This class implements CommandExecutor to process commands sent by players.
     * It handles:
     * - Permission checks
     * - Command syntax validation
     * - Displaying all ranks with progress (/ranks)
     * - Showing help information (/ranks help)
     * - Reloading configuration (/ranks reload)
     * - Upgrading to next rank (/ranks next)
     * - Upgrading to specific rank (/ranks upgrade <rank>)
     */
    public class RanksCommand implements CommandExecutor {

        /**
         * Reference to the main plugin instance.
         * Used to access plugin methods and configuration.
         */
        private final Ranks plugin;

        /**
         * Constructor for the command executor.
         * 
         * @param plugin The main Ranks plugin instance
         */
        public RanksCommand(Ranks plugin) {
            this.plugin = plugin;
        }

        /**
         * Main command handler for /ranks command.
         * 
         * Command syntax:
         * - /ranks              → Display all ranks with progress and costs
         * - /ranks help         → Show help information
         * - /ranks reload       → Reload configuration (OP only)
         * - /ranks next         → Upgrade to next rank
         * - /ranks upgrade <rank> → Upgrade to specific rank
         * 
         * @param sender The entity that sent the command
         * @param command The command object
         * @param label The command alias used
         * @param args Command arguments
         * @return true if command was handled, false otherwise
         */
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            // Validate command name
            if (!command.getName().equalsIgnoreCase("ranks")) {
                return false;
            }

            // No arguments - show ranks list (must be a player)
            if (args.length == 0) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
                    return true;
                }
                showRanksList((Player) sender);
                return true;
            }

            // Handle subcommands
            switch (args[0].toLowerCase()) {
                case "help":
                    showHelp(sender);
                    return true;
                    
                case "reload":
                    // Permission check for reload command
                    if (!sender.hasPermission("ranks.reload")) {
                        sender.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
                        return true;
                    }
                    reloadConfig();
                    sender.sendMessage(Component.text("Configuration reloaded!", NamedTextColor.GREEN));
                    return true;
                    
                case "next":
                    // Must be a player
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
                        return true;
                    }
                    upgradeNext((Player) sender);
                    return true;
                    
                case "upgrade":
                    // Must be a player
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
                        return true;
                    }
                    // Require rank argument
                    if (args.length < 2) {
                        sender.sendMessage(Component.text("Usage: /ranks upgrade <rank>", NamedTextColor.RED));
                        return true;
                    }
                    upgradeToRank((Player) sender, args[1]);
                    return true;
                    
                default:
                    sender.sendMessage(Component.text("Unknown command. Use /ranks help for help.", NamedTextColor.RED));
                    return true;
            }
        }

        /**
         * Displays formatted help information to the command sender.
         * 
         * Shows all available commands with descriptions.
         * The reload command is only shown if the sender has the ranks.reload permission.
         * 
         * @param sender The entity to show help to
         */
        private void showHelp(CommandSender sender) {
            sender.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GRAY, TextDecoration.STRIKETHROUGH));
            sender.sendMessage(Component.text("Ranks Help", NamedTextColor.GOLD, TextDecoration.BOLD));
            sender.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GRAY, TextDecoration.STRIKETHROUGH));
            sender.sendMessage(Component.text("/ranks", NamedTextColor.YELLOW)
                    .append(Component.text(" - Show all ranks and costs", NamedTextColor.WHITE)));
            sender.sendMessage(Component.text("/ranks next", NamedTextColor.YELLOW)
                    .append(Component.text(" - Upgrade to next rank", NamedTextColor.WHITE)));
            sender.sendMessage(Component.text("/ranks upgrade <rank>", NamedTextColor.YELLOW)
                    .append(Component.text(" - Upgrade to specified rank", NamedTextColor.WHITE)));
            // Only show reload command if sender has permission
            if (sender.hasPermission("ranks.reload")) {
                sender.sendMessage(Component.text("/ranks reload", NamedTextColor.YELLOW)
                        .append(Component.text(" - Reload configuration", NamedTextColor.WHITE)));
            }
            sender.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GRAY, TextDecoration.STRIKETHROUGH));
        }

        /**
         * Displays the list of all ranks with progress indicators and costs.
         * 
         * This method creates a visual representation showing:
         * - Completed ranks (strikethrough gray with "COMPLETED" badge in green)
         * - Current rank (with green arrow indicator <----)
         * - Future ranks (with individual cost and cumulative cost from current rank)
         * 
         * Format example (player is at Adept):
         * mortal > adept | COMPLETED
         * adept > hero | $50,000 <----
         * hero > paragon | $200,000 | $250,000
         * 
         * @param player The player to show the ranks list to
         */
        private void showRanksList(Player player) {
            FileConfiguration config = plugin.getConfig();
            String defaultPath = config.getString("defaultpath", "default");
            ConfigurationSection ranksSection = config.getConfigurationSection("Ranks." + defaultPath);
            
            if (ranksSection == null) {
                player.sendMessage(Component.text("No ranks configured!", NamedTextColor.RED));
                return;
            }

            String currentRank = plugin.getCurrentRank(player, ranksSection);
            List<String> rankOrder = plugin.getRankOrder(ranksSection);
            
            // Header
            player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GRAY, TextDecoration.STRIKETHROUGH));
            player.sendMessage(Component.text("Server Ranks", NamedTextColor.GOLD, TextDecoration.BOLD));
            player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GRAY, TextDecoration.STRIKETHROUGH));
            
            int currentRankIndex = rankOrder.indexOf(currentRank);
            
            // Display each rank progression
            for (int i = 0; i < rankOrder.size() - 1; i++) {
                String rank = rankOrder.get(i);
                String nextRank = rankOrder.get(i + 1);
                ConfigurationSection rankSection = ranksSection.getConfigurationSection(rank);
                ConfigurationSection nextRankSection = ranksSection.getConfigurationSection(nextRank);
                
                if (rankSection == null || nextRankSection == null) continue;
                
                String display = rankSection.getString("display", rank);
                double cost = nextRankSection.getDouble("cost", 0);
                
                Component displayComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(display);
                Component nextDisplayComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(
                        nextRankSection.getString("display", nextRank));
                
                boolean isCompleted = i < currentRankIndex;
                boolean isCurrent = i == currentRankIndex;
                
                Component line = Component.empty();
                
                if (isCompleted) {
                    // Completed rank - show strikethrough gray with COMPLETED badge
                    // Strip color codes from display names for completed ranks
                    String plainDisplay = display.replaceAll("&[0-9a-fk-or]", "").replaceAll("§[0-9a-fk-or]", "");
                    String plainNextDisplay = nextRankSection.getString("display", nextRank)
                            .replaceAll("&[0-9a-fk-or]", "").replaceAll("§[0-9a-fk-or]", "");
                    
                    line = line.append(Component.text(plainDisplay, NamedTextColor.GRAY, TextDecoration.STRIKETHROUGH))
                            .append(Component.text(" > ", NamedTextColor.GRAY, TextDecoration.STRIKETHROUGH))
                            .append(Component.text(plainNextDisplay, NamedTextColor.GRAY, TextDecoration.STRIKETHROUGH))
                            .append(Component.text(" | ", NamedTextColor.GRAY))
                            .append(Component.text("COMPLETED", NamedTextColor.GREEN, TextDecoration.BOLD));
                } else {
                    // Calculate cumulative cost from current rank to this rank
                    double cumulativeFromCurrent = 0;
                    boolean hasUnpurchasableRank = false;
                    for (int j = currentRankIndex + 1; j <= i + 1; j++) {
                        if (j < rankOrder.size()) {
                            ConfigurationSection tempSection = ranksSection.getConfigurationSection(rankOrder.get(j));
                            if (tempSection != null) {
                                double tempCost = tempSection.getDouble("cost", 0);
                                if (tempCost == -1) {
                                    hasUnpurchasableRank = true;
                                } else {
                                    cumulativeFromCurrent += tempCost;
                                }
                            }
                        }
                    }
                    
                    // Future rank - show with costs or cost-message
                    line = line.append(displayComponent)
                            .append(Component.text(" > ", NamedTextColor.WHITE))
                            .append(nextDisplayComponent)
                            .append(Component.text(" | ", NamedTextColor.GRAY));
                    
                    // Check if this rank has cost -1 (cannot be purchased with currency)
                    if (cost == -1) {
                        String costMessage = nextRankSection.getString("cost-message", "Not available for purchase");
                        line = line.append(Component.text(costMessage, NamedTextColor.RED));
                    } else {
                        line = line.append(Component.text("$" + moneyFormat.format(cost), NamedTextColor.YELLOW));
                        
                        // Show cumulative cost if not current rank and no unpurchasable ranks in between
                        if (!isCurrent && cumulativeFromCurrent > 0 && !hasUnpurchasableRank) {
                            line = line.append(Component.text(" | ", NamedTextColor.GRAY))
                                    .append(Component.text("$" + moneyFormat.format(cumulativeFromCurrent), NamedTextColor.GOLD));
                        }
                    }
                    
                    // Add green arrow for current rank
                    if (isCurrent) {
                        line = line.append(Component.text(" <----", NamedTextColor.GREEN));
                    }
                }
                
                player.sendMessage(line);
            }
            
            // Footer
            player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GRAY, TextDecoration.STRIKETHROUGH));
        }

        /**
         * Upgrades a player to the next rank in the progression.
         * 
         * This method:
         * 1. Determines the player's current rank
         * 2. Gets the next rank from the configuration
         * 3. Checks if the player has enough money
         * 4. Withdraws the cost from the player's balance
         * 5. Executes the rank-up process (commands and broadcasts)
         * 
         * @param player The player attempting to upgrade
         */
        private void upgradeNext(Player player) {
            FileConfiguration config = plugin.getConfig();
            String defaultPath = config.getString("defaultpath", "default");
            ConfigurationSection ranksSection = config.getConfigurationSection("Ranks." + defaultPath);
            
            if (ranksSection == null) {
                player.sendMessage(Component.text("No ranks configured!", NamedTextColor.RED));
                return;
            }

            String currentRank = plugin.getCurrentRank(player, ranksSection);
            ConfigurationSection currentSection = ranksSection.getConfigurationSection(currentRank);
            
            if (currentSection == null) {
                player.sendMessage(Component.text("Error: Could not find your current rank!", NamedTextColor.RED));
                return;
            }

            String nextRank = currentSection.getString("nextrank");
            
            // Check if player is already at max rank
            if (nextRank == null || nextRank.equalsIgnoreCase("LASTRANK")) {
                player.sendMessage(Component.text("You are already at the highest rank!", NamedTextColor.RED));
                return;
            }

            ConfigurationSection nextSection = ranksSection.getConfigurationSection(nextRank);
            
            if (nextSection == null) {
                player.sendMessage(Component.text("Error: Next rank not found in configuration!", NamedTextColor.RED));
                return;
            }

            double cost = nextSection.getDouble("cost", 0);
            
            // Check if rank cannot be purchased with currency
            if (cost == -1) {
                String costMessage = nextSection.getString("cost-message", "This rank cannot be purchased with currency.");
                player.sendMessage(Component.text(costMessage, NamedTextColor.RED));
                return;
            }
            
            double balance = economy.getBalance(player);
            
            // Check if player can afford the rank
            if (balance < cost) {
                player.sendMessage(Component.text("You don't have enough money! Cost: $" + moneyFormat.format(cost) + 
                        " | Your balance: $" + moneyFormat.format(balance), NamedTextColor.RED));
                return;
            }

            // Withdraw money and execute rank-up
            economy.withdrawPlayer(player, cost);
            plugin.executeRankUp(player, nextSection, nextRank);
        }

        /**
         * Upgrades a player to a specific target rank.
         * 
         * This method allows players to skip multiple ranks at once by paying the
         * cumulative cost of all ranks between their current rank and the target rank.
         * 
         * This method:
         * 1. Validates the target rank exists
         * 2. Checks the target rank is higher than current rank
         * 3. Calculates cumulative cost of all intermediate ranks
         * 4. Checks if the player has enough money
         * 5. Withdraws the total cost from the player's balance
         * 6. Executes the rank-up process for each intermediate rank
         * 
         * @param player The player attempting to upgrade
         * @param targetRank The name of the rank to upgrade to
         */
        private void upgradeToRank(Player player, String targetRank) {
            FileConfiguration config = plugin.getConfig();
            String defaultPath = config.getString("defaultpath", "default");
            ConfigurationSection ranksSection = config.getConfigurationSection("Ranks." + defaultPath);
            
            if (ranksSection == null) {
                player.sendMessage(Component.text("No ranks configured!", NamedTextColor.RED));
                return;
            }

            ConfigurationSection targetSection = ranksSection.getConfigurationSection(targetRank);
            
            // Validate target rank exists
            if (targetSection == null) {
                player.sendMessage(Component.text("Rank '" + targetRank + "' not found!", NamedTextColor.RED));
                return;
            }

            String currentRank = plugin.getCurrentRank(player, ranksSection);
            List<String> rankOrder = plugin.getRankOrder(ranksSection);
            
            int currentIndex = rankOrder.indexOf(currentRank);
            int targetIndex = rankOrder.indexOf(targetRank);
            
            // Check if target rank is actually higher than current rank
            if (targetIndex <= currentIndex) {
                player.sendMessage(Component.text("You already have this rank or a higher one!", NamedTextColor.RED));
                return;
            }

            // Calculate total cumulative cost and check for unpurchasable ranks
            double totalCost = 0;
            boolean hasUnpurchasableRank = false;
            String unpurchasableRankName = "";
            
            for (int i = currentIndex + 1; i <= targetIndex; i++) {
                ConfigurationSection section = ranksSection.getConfigurationSection(rankOrder.get(i));
                if (section != null) {
                    double rankCost = section.getDouble("cost", 0);
                    if (rankCost == -1) {
                        hasUnpurchasableRank = true;
                        unpurchasableRankName = rankOrder.get(i);
                        break;
                    }
                    totalCost += rankCost;
                }
            }
            
            // Check if there's an unpurchasable rank in the path
            if (hasUnpurchasableRank) {
                ConfigurationSection unpurchasableSection = ranksSection.getConfigurationSection(unpurchasableRankName);
                String costMessage = unpurchasableSection != null ? 
                    unpurchasableSection.getString("cost-message", "This rank cannot be purchased with currency.") : 
                    "This rank cannot be purchased with currency.";
                player.sendMessage(Component.text("Cannot upgrade to " + targetRank + " because '" + unpurchasableRankName + 
                        "' is in the path and cannot be purchased with currency.", NamedTextColor.RED));
                player.sendMessage(Component.text(costMessage, NamedTextColor.RED));
                return;
            }

            double balance = economy.getBalance(player);
            
            // Check if player can afford the cumulative cost
            if (balance < totalCost) {
                player.sendMessage(Component.text("You don't have enough money! Cost: $" + moneyFormat.format(totalCost) + 
                        " | Your balance: $" + moneyFormat.format(balance), NamedTextColor.RED));
                return;
            }

            // Withdraw total cost
            economy.withdrawPlayer(player, totalCost);
            
            // Execute rank-up for each intermediate rank
            for (int i = currentIndex + 1; i <= targetIndex; i++) {
                String rank = rankOrder.get(i);
                ConfigurationSection section = ranksSection.getConfigurationSection(rank);
                if (section != null) {
                    plugin.executeRankUp(player, section, rank);
                }
            }
        }
    }
}