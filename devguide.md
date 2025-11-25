# Ranks Plugin - Developer Guide

## Overview

Ranks is a Minecraft Paper/Spigot plugin that provides a currency-based rank progression system. This guide covers the technical architecture, code structure, and implementation details for developers who want to understand, modify, or extend the plugin.

## Technical Stack

### Core Technologies
- **Java** - Programming language
- **Paper/Spigot API** - Minecraft server API (Bukkit-compatible)
- **Maven** - Build tool and dependency management

### External Dependencies
- **Vault API** - Economy integration layer
- **LuckPerms API** - Permission management system
- **Adventure API** - Modern text component system (bundled with Paper)

### Build Information
- **Minimum Java Version:** Java 17+
- **Target Minecraft Version:** 1.19+ (Paper/Spigot)
- **API Version:** 1.19

## Architecture

### Single-File Design

The plugin follows a single-file architecture pattern with all code contained in `Ranks.java`. This design choice provides:
- **Simplicity** - Easy to understand and maintain
- **Portability** - Single file to modify and deploy
- **Minimal overhead** - No complex package structures

### Class Structure

```
Ranks (extends JavaPlugin, implements Listener)
├── Fields
│   ├── Economy economy (Vault)
│   ├── LuckPerms luckPerms (LuckPerms API)
│   └── DecimalFormat moneyFormat (for currency display)
│
├── Lifecycle Methods
│   ├── onEnable()
│   ├── onDisable()
│   ├── setupEconomy()
│   └── setupLuckPerms()
│
├── Event Handlers
│   ├── onPlayerJoin(PlayerJoinEvent)
│   └── processPlayer(Player)
│
├── Core Logic
│   ├── getCurrentRank(Player, ConfigurationSection)
│   ├── getRankOrder(ConfigurationSection)
│   ├── getFirstRank(ConfigurationSection)
│   └── executeRankUp(Player, ConfigurationSection, String)
│
└── Inner Classes
    └── RanksCommand (implements CommandExecutor)
        ├── onCommand(...)
        ├── showHelp(Player)
        ├── showRanks(Player)
        ├── upgradeNext(Player)
        └── upgradeToRank(Player, String)
```

## Detailed Component Analysis

### Main Plugin Class: Ranks

#### Purpose
The main class handles plugin lifecycle, dependency setup, event handling, and core rank management logic.

#### Key Responsibilities
1. **Dependency Management** - Setting up Vault and LuckPerms
2. **Configuration Loading** - Loading and validating config.yml
3. **Event Handling** - Processing player join events
4. **Rank Logic** - Determining current ranks, rank order, and executing rank-ups

#### Critical Fields

**`Economy economy`**
- Type: `net.milkbowl.vault.economy.Economy`
- Purpose: Interface for all currency transactions
- Initialization: `setupEconomy()` method during `onEnable()`
- Usage: Checking balances, withdrawing money for rank purchases

**`LuckPerms luckPerms`**
- Type: `net.luckperms.api.LuckPerms`
- Purpose: Interface for permission group management
- Initialization: `setupLuckPerms()` method during `onEnable()`
- Usage: Querying player groups, checking default group membership

**`DecimalFormat moneyFormat`**
- Pattern: `"#,###.##"`
- Purpose: Consistent currency formatting throughout the plugin
- Usage: All money display in messages (e.g., "$1,000,000")

### Lifecycle Flow

#### Plugin Enable Sequence

```
onEnable()
  ├─> saveDefaultConfig()                    # Save config.yml if not exists
  ├─> setupEconomy()                         # Hook into Vault
  │     └─> IF FAIL: disable plugin
  ├─> setupLuckPerms()                       # Hook into LuckPerms API
  │     └─> IF FAIL: disable plugin
  ├─> Register RanksCommand                  # Set command executor
  ├─> Register Event Listeners               # Register PlayerJoinEvent
  ├─> Process Online Players                 # Handle hot reload case
  │     └─> For each online player: processPlayer()
  └─> Console Messages                       # Startup + author credit
```

**Important Design Note:**
The plugin processes all online players during enable to handle hot reloads. This ensures that if the plugin is loaded while players are already on the server, they still get processed for default group migration.

#### Plugin Disable Sequence

```
onDisable()
  └─> Console Message                        # Shutdown message
```

**Note:** No explicit cleanup needed for economy or LuckPerms as these are managed by their respective plugins.

### Dependency Setup

#### Vault Economy Setup

```java
private boolean setupEconomy() {
    if (getServer().getPluginManager().getPlugin("Vault") == null) {
        return false;
    }
    RegisteredServiceProvider<Economy> rsp = 
        getServer().getServicesManager().getRegistration(Economy.class);
    if (rsp == null) {
        return false;
    }
    economy = rsp.getProvider();
    return economy != null;
}
```

**Process:**
1. Check if Vault plugin exists
2. Query Bukkit's services manager for Economy registration
3. Extract the Economy provider (e.g., EssentialsX economy)
4. Store in `economy` field

**Failure Handling:**
- Returns `false` if any step fails
- Causes plugin to disable in `onEnable()`

#### LuckPerms Setup

```java
private boolean setupLuckPerms() {
    RegisteredServiceProvider<LuckPerms> provider = 
        Bukkit.getServicesManager().getRegistration(LuckPerms.class);
    if (provider != null) {
        luckPerms = provider.getProvider();
    }
    return luckPerms != null;
}
```

**Process:**
1. Query services manager for LuckPerms registration
2. Extract LuckPerms API instance
3. Store in `luckPerms` field

**Failure Handling:**
- Returns `false` if provider is null
- Causes plugin to disable in `onEnable()`

### Event Handling System

#### Player Join Event

```java
@EventHandler
public void onPlayerJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();
    processPlayer(player);
}
```

**Triggers:** Every time a player joins the server
**Action:** Delegates to `processPlayer()` for default group migration check

#### Player Processing

```java
private void processPlayer(Player player) {
    luckPerms.getUserManager().loadUser(player.getUniqueId())
        .thenAcceptAsync(user -> {
            boolean hasDefaultGroup = user.getNodes().stream()
                .anyMatch(node -> node.getKey().startsWith("group.default"));
            
            if (hasDefaultGroup) {
                // Migrate player to first rank
            }
        });
}
```

**Process:**
1. Load player's LuckPerms User data (asynchronous)
2. Check if player has "group.default" permission node
3. If yes, execute rank-up to first rank (cost: 0)
4. First rank's `executecmds` handle LuckPerms group changes

**Asynchronous Design:**
- Uses CompletableFuture pattern
- Prevents blocking the main thread during LuckPerms data loading
- Ensures smooth server performance

**Why This Matters:**
New players start with LuckPerms' "default" group. This automatic migration ensures they immediately enter the rank progression system with proper permissions.

### Core Rank Logic

#### Getting Current Rank

```java
protected String getCurrentRank(Player player, ConfigurationSection ranksSection) {
    User user = luckPerms.getUserManager().getUser(player.getUniqueId());
    if (user == null) {
        return getFirstRank(ranksSection);
    }

    Set<String> ranks = ranksSection.getKeys(false);
    for (String rank : ranks) {
        String groupNode = "group." + rank;
        boolean hasRank = user.getNodes().stream()
            .anyMatch(node -> node.getKey().equals(groupNode));
        if (hasRank) {
            return rank;
        }
    }
    return getFirstRank(ranksSection);
}
```

**Algorithm:**
1. Load player's LuckPerms User data (synchronous)
2. Iterate through all ranks in config
3. Check if player has the corresponding LuckPerms group node
4. Return first matching rank
5. Default to first rank if no match found

**Implementation Notes:**
- Uses streams for efficient node checking
- Fallback to first rank ensures method always returns valid rank
- Synchronous User loading is safe here (called from main thread)

#### Building Rank Order

```java
protected List<String> getRankOrder(ConfigurationSection ranksSection) {
    List<String> order = new ArrayList<>();
    String current = getFirstRank(ranksSection);
    
    while (current != null) {
        order.add(current);
        ConfigurationSection section = ranksSection.getConfigurationSection(current);
        if (section == null) break;
        
        String next = section.getString("nextrank");
        if (next == null || next.equalsIgnoreCase("LASTRANK")) {
            break;
        }
        current = next;
    }
    return order;
}
```

**Algorithm:**
1. Start with first rank (cost: 0)
2. Add to list
3. Get next rank from "nextrank" field
4. Continue until reaching LASTRANK or null
5. Return complete ordered list

**Use Cases:**
- Displaying ranks in correct order
- Calculating rank indices for comparison
- Multi-rank upgrade path calculation

**Complexity:** O(n) where n is number of ranks

#### Finding First Rank

```java
private String getFirstRank(ConfigurationSection ranksSection) {
    for (String rank : ranksSection.getKeys(false)) {
        ConfigurationSection section = ranksSection.getConfigurationSection(rank);
        if (section != null && section.getDouble("cost", -1) == 0) {
            return rank;
        }
    }
    return ranksSection.getKeys(false).iterator().next();
}
```

**Algorithm:**
1. Iterate through all ranks
2. Find rank with cost: 0
3. Return that rank
4. Fallback to first rank key if none found

**Design Assumption:**
- Exactly one rank should have cost: 0 (the starting rank)
- If misconfigured, falls back to first rank in config

#### Executing Rank-Up

```java
protected void executeRankUp(Player player, ConfigurationSection rankSection, String rankName) {
    // Send broadcasts
    List<String> broadcasts = rankSection.getStringList("broadcast");
    for (String broadcast : broadcasts) {
        String message = broadcast.replace("%player%", player.getName());
        Component component = LegacyComponentSerializer.legacyAmpersand()
            .deserialize(message);
        Bukkit.broadcast(component);
    }

    // Execute commands
    List<String> commands = rankSection.getStringList("executecmds");
    for (String cmd : commands) {
        String command = cmd.replace("%player%", player.getName());
        if (command.startsWith("[console] ")) {
            command = command.substring("[console] ".length());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        } else {
            player.performCommand(command);
        }
    }
}
```

**Process:**
1. **Broadcast Phase**
   - Load broadcast messages from config
   - Replace %player% variable
   - Parse legacy color codes
   - Send to all online players
   
2. **Command Execution Phase**
   - Load commands from config
   - Replace %player% variable
   - Execute as console or player based on [console] prefix
   - Console execution provides elevated permissions

**Variable Substitution:**
- `%player%` → Player's display name
- Applied to both broadcasts and commands

**Command Execution Context:**
- `[console]` prefix → Console sender (for LuckPerms commands)
- No prefix → Player sender (for player-specific commands)

**Design Note:**
This method is called AFTER payment has been processed and validated. It assumes all checks have passed.

### Command System

#### Inner Class: RanksCommand

**Purpose:** Handles all /ranks command execution and subcommands

**Design Pattern:** Separate executor class for clean separation of concerns

**Access:** Has reference to main plugin instance via constructor

#### Command Routing

```java
public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    // Validate sender is player
    if (!(sender instanceof Player)) {
        sender.sendMessage(...);
        return true;
    }
    
    Player player = (Player) sender;
    
    // Check permission
    if (!player.hasPermission("ranks.use")) {
        player.sendMessage(...);
        return true;
    }
    
    // Route based on args
    if (args.length == 0) {
        showRanks(player);
        return true;
    }
    
    switch (args[0].toLowerCase()) {
        case "help": showHelp(player); return true;
        case "reload": /* ... */ return true;
        case "next": upgradeNext(player); return true;
        case "upgrade": upgradeToRank(player, args[1]); return true;
        default: /* Unknown command */ return true;
    }
}
```

**Validation Flow:**
1. Check sender is Player (not console)
2. Check player has `ranks.use` permission
3. Route to appropriate handler based on subcommand

**Return Value:**
- Always returns `true` (we handle all messages internally)
- Returning `false` would show usage from plugin.yml

#### Show Ranks Display

**Purpose:** Visual display of all ranks with progress indicators

**Key Features:**
- Balance display
- Completion checkmarks (✓/✗)
- Color-coded rank names
- Cost display with affordability check
- Current rank indicator (<----)

**Algorithm:**
```
1. Load configuration
2. Get player's current rank
3. Get rank order
4. Get player's balance
5. Display header with name and balance
6. For each rank:
   a. Check if completed (index <= current index)
   b. Display checkmark (green ✓ or red ✗)
   c. Display rank name (parsed colors)
   d. Display cost or special message
   e. Show affordability for incomplete ranks
   f. Show arrow for current rank
7. Display footer
```

**Text Component Construction:**
Uses Adventure API's Component system for rich text formatting with colors and decorations.

#### Upgrade Next

**Purpose:** Single-step rank upgrade

**Algorithm:**
```
1. Load configuration
2. Get current rank
3. Get next rank from current.nextrank
4. Validate next rank exists and isn't LASTRANK
5. Get cost of next rank
6. Check if cost is -1 (unpurchasable)
7. Get player balance
8. Check if player can afford
9. Withdraw money
10. Execute rank-up
```

**Error Handling:**
- No ranks configured
- Current rank not found
- At max rank (LASTRANK)
- Next rank not in config
- Rank unpurchasable (cost -1)
- Insufficient funds

**Transaction Safety:**
Money is only withdrawn AFTER all validation passes. If any check fails, no money is taken.

#### Upgrade To Rank

**Purpose:** Multi-rank upgrade with cumulative cost calculation

**Algorithm:**
```
1. Load configuration
2. Validate target rank exists
3. Get current rank and target rank
4. Get rank order list
5. Get indices of current and target
6. Validate target > current
7. Calculate cumulative cost:
   a. Sum costs of ranks from (current+1) to target (inclusive)
   b. Check for any cost=-1 ranks in path
   c. If found, abort with message
8. Get player balance
9. Check if player can afford cumulative cost
10. Withdraw cumulative cost
11. For each rank from (current+1) to target:
    a. Execute rank-up (broadcasts + commands)
```

**Key Difference from upgradeNext:**
- Calculates cumulative cost
- Checks ALL ranks in path for unpurchasable ranks
- Executes rank-up for EACH intermediate rank

**Why Execute Each Rank-Up:**
Ensures all broadcasts are sent and all commands execute for each rank, so player receives all permissions and announcements for intermediate ranks.

**Unpurchasable Rank Blocking:**
If any rank in the path has cost: -1, the entire upgrade fails. This prevents players from bypassing donor ranks.

## Configuration System

### Config Structure

The plugin uses Bukkit's YAML configuration system:

```yaml
defaultpath: default

Ranks:
  default:          # Rank path
    'mortal':       # Rank key (must match LuckPerms group name)
      display: "&7[Mortal]"
      nextrank: adept
      cost: 0
      broadcast: []
      executecmds: []
```

### Configuration Access

```java
FileConfiguration config = plugin.getConfig();
String defaultPath = config.getString("defaultpath", "default");
ConfigurationSection ranksSection = config.getConfigurationSection("Ranks." + defaultPath);
```

**Loading Process:**
1. Get FileConfiguration from plugin
2. Access nested sections using dot notation
3. Provide defaults for safety

### Configuration Validation

**What's Validated:**
- Ranks section exists
- Individual rank sections exist
- Current rank exists in player's data

**What's NOT Validated:**
- Nextrank references valid ranks
- Cost values are reasonable
- LuckPerms groups exist
- Command syntax

**Developer Consideration:**
Could add configuration validation on load to catch errors early. Currently relies on runtime checking.

## Text Component System

### Adventure API Integration

The plugin uses Adventure API (bundled with Paper) for modern text components:

```java
Component text = Component.text("Message", NamedTextColor.GREEN);
player.sendMessage(text);
```

### Legacy Color Code Support

For configuration strings, legacy codes (&7, &a, &l, etc.) are parsed:

```java
Component component = LegacyComponentSerializer.legacyAmpersand()
    .deserialize("&7[&aMortal&7]");
```

**Supported Codes:**
- Colors: &0-9, &a-f
- Formatting: &l (bold), &m (strikethrough), &n (underline), &o (italic)
- Reset: &r

### Text Building Pattern

```java
Component line = Component.text("✓ ", NamedTextColor.GREEN)
    .append(Component.text("[Mortal]", NamedTextColor.GRAY))
    .append(Component.text(" - $40,000", NamedTextColor.GRAY));
player.sendMessage(line);
```

**Benefits:**
- Type-safe color handling
- Composable text components
- Modern Minecraft text features

## Console Message Patterns

### Standard Color Scheme

The plugin follows established console message conventions:

```java
// Startup - Green
Component.text("[Ranks] Ranks Started!").color(NamedTextColor.GREEN)

// Author Credit - Magenta
Component.text("[Ranks] By SupaFloof Games, LLC").color(NamedTextColor.LIGHT_PURPLE)

// Shutdown - Red
Component.text("[Ranks] Ranks Disabled!").color(NamedTextColor.RED)
```

### Error Messages

```java
getLogger().severe("Vault not found! This plugin requires Vault and an economy plugin.");
```

Uses Bukkit's logging system for errors.

## Extension Points

### Adding New Subcommands

**Location:** `RanksCommand.onCommand()` method

**Pattern:**
```java
case "newcommand":
    if (!player.hasPermission("ranks.newcommand")) {
        player.sendMessage(...);
        return true;
    }
    executeNewCommand(player, args);
    return true;
```

**Steps:**
1. Add case to switch statement
2. Check permissions if needed
3. Delegate to new method
4. Add method to RanksCommand class

### Adding New Rank Properties

**Steps:**
1. Add field to config.yml
2. Read value in appropriate method
3. Use value in logic

**Example - Adding rank icon:**
```yaml
'mortal':
  display: "&7[Mortal]"
  icon: "DIAMOND"  # New field
  # ...
```

```java
// In showRanks() method:
String icon = section.getString("icon", "PAPER");
// Display icon in GUI
```

### Custom Variable Support

**Current Variable:** `%player%` (replaced with player name)

**Adding New Variables:**
```java
protected void executeRankUp(Player player, ConfigurationSection rankSection, String rankName) {
    // ... existing code ...
    
    for (String broadcast : broadcasts) {
        String message = broadcast
            .replace("%player%", player.getName())
            .replace("%rank%", rankName)  // New variable
            .replace("%displayname%", player.getDisplayName());
        // ...
    }
}
```

### Event Hooks

**Adding Custom Events:**
```java
// Create custom event
public class PlayerRankUpEvent extends PlayerEvent implements Cancellable {
    private String oldRank;
    private String newRank;
    private boolean cancelled;
    
    // Constructor, getters, setters, etc.
}

// Call in executeRankUp():
PlayerRankUpEvent event = new PlayerRankUpEvent(player, oldRank, newRank);
Bukkit.getPluginManager().callEvent(event);
if (event.isCancelled()) {
    return;
}
```

**Use Cases:**
- Allow other plugins to react to rank-ups
- Enable cancellation of rank-ups
- Provide rank-up data to external systems

## Testing Considerations

### Unit Testing Challenges

**Difficulties:**
- Bukkit/Paper API requires server environment
- LuckPerms and Vault dependencies
- Configuration loading requires file system

**Solutions:**
- Mock frameworks (Mockito, PowerMock)
- MockBukkit for Bukkit environment simulation
- Test configuration files in resources

### Integration Testing

**Recommended Approach:**
1. Set up test server with Paper
2. Install Vault, economy plugin, LuckPerms
3. Load plugin
4. Test commands with real players
5. Verify economy transactions
6. Verify permission changes

### Manual Testing Checklist

- [ ] Plugin enables without errors
- [ ] New players migrate from default group
- [ ] `/ranks` displays correctly
- [ ] `/ranks next` upgrades correctly
- [ ] `/ranks upgrade` calculates cumulative cost
- [ ] Money is withdrawn correctly
- [ ] Broadcasts are sent
- [ ] LuckPerms groups are updated
- [ ] Permissions apply correctly
- [ ] Non-purchasable ranks block correctly
- [ ] Error messages display correctly
- [ ] `/ranks reload` works
- [ ] Plugin disables without errors

## Performance Considerations

### Asynchronous Operations

**Player Data Loading:**
```java
luckPerms.getUserManager().loadUser(player.getUniqueId())
    .thenAcceptAsync(user -> {
        // Process user data
    });
```

Benefits:
- Doesn't block main thread
- Smooth server performance
- Handles slow LuckPerms database queries

**Trade-off:**
- More complex code (CompletableFuture)
- Requires careful thread safety

### Synchronous Operations

**Rank Queries:**
```java
User user = luckPerms.getUserManager().getUser(player.getUniqueId());
```

**When Used:**
- During command execution (already on main thread)
- User data typically cached by LuckPerms

**Safety:**
- Safe because commands execute on main thread
- LuckPerms caches user data in memory

### Memory Usage

**Minimal Memory Footprint:**
- No data caching in plugin
- Configuration loaded into memory once
- Single Economy/LuckPerms instances

**Scalability:**
- Performance independent of player count
- O(n) operations for rank listing (n = number of ranks)
- O(1) operations for current rank lookup

## Security Considerations

### Permission Checks

**All Commands:**
```java
if (!player.hasPermission("ranks.use")) {
    player.sendMessage(...);
    return true;
}
```

**Reload Command:**
```java
if (!player.isOp()) {
    player.sendMessage(...);
    return true;
}
```

### Console Command Execution

**Why [console] Prefix:**
LuckPerms commands require elevated permissions. Executing from console bypasses permission checks.

**Security Implications:**
- Commands are predefined in config (admin-controlled)
- No player input directly executed as console
- Variable substitution limited to %player%

**Potential Vulnerability:**
If rank names contain malicious characters, could theoretically inject commands. Mitigated by:
- Config is admin-controlled
- LuckPerms sanitizes input
- No user-provided rank names

### Transaction Safety

**Money Withdrawal:**
All validation occurs BEFORE withdrawal:
```java
if (balance < cost) {
    // Error message - NO withdrawal
    return;
}
economy.withdrawPlayer(player, cost);  // Only reached if affordable
```

**Benefits:**
- No money lost on failed upgrades
- No partial transactions
- Clear error messages

## Code Style and Conventions

### Documentation

**JavaDoc Standards:**
- All public methods documented
- All protected methods documented
- All fields documented
- Complex logic explained with inline comments

**Comment Style:**
```java
/**
 * Method description
 * 
 * Longer explanation if needed
 * 
 * @param player Description
 * @return Description
 */
```

### Naming Conventions

**Methods:**
- `getCurrentRank()` - Getters prefix with "get"
- `setupEconomy()` - Setup methods prefix with "setup"
- `executeRankUp()` - Action methods use verbs
- `showRanks()` - Display methods prefix with "show"

**Variables:**
- `economy` - Lowercase for fields
- `LASTRANK` - Uppercase for constants (in config)
- `currentRank` - CamelCase for locals

### Code Organization

**Method Order:**
1. Lifecycle methods (onEnable, onDisable)
2. Setup methods (setupEconomy, setupLuckPerms)
3. Event handlers
4. Core logic methods
5. Inner classes

**Readability:**
- Single responsibility per method
- Clear variable names
- Consistent formatting
- Logical grouping of related code

## Common Development Tasks

### Adding a New Subcommand

1. Add case to `RanksCommand.onCommand()`:
```java
case "mynewcommand":
    if (args.length < 2) {
        player.sendMessage(...);
        return true;
    }
    myNewCommand(player, args[1]);
    return true;
```

2. Implement handler method:
```java
private void myNewCommand(Player player, String arg) {
    // Implementation
}
```

3. Add to help display in `showHelp()`:
```java
player.sendMessage(
    Component.text("/ranks mynewcommand <arg>", NamedTextColor.AQUA)
        .append(Component.text(" - Description", NamedTextColor.WHITE))
);
```

4. Document in README and user guides

### Modifying Rank Display

**Location:** `RanksCommand.showRanks()` method

**Customization Points:**
- Header/footer styling
- Rank line format
- Color scheme
- Additional information display

**Example - Adding rank number:**
```java
for (int i = 0; i < rankOrder.size(); i++) {
    String rank = rankOrder.get(i);
    // ... existing code ...
    
    // Add rank number
    line = Component.text((i + 1) + ". ", NamedTextColor.GRAY)
        .append(line);
    
    player.sendMessage(line);
}
```

### Adding Configuration Options

1. Add to config.yml:
```yaml
settings:
  silent-mode: false
```

2. Read in code:
```java
boolean silentMode = config.getBoolean("settings.silent-mode", false);
```

3. Use in logic:
```java
if (!silentMode) {
    // Send broadcasts
}
```

### Debugging Tips

**Enable Debug Logging:**
```java
getLogger().info("Current rank: " + currentRank);
getLogger().info("Balance: " + balance + ", Cost: " + cost);
```

**Check Plugin State:**
```java
getLogger().info("Economy: " + (economy != null ? "Connected" : "NULL"));
getLogger().info("LuckPerms: " + (luckPerms != null ? "Connected" : "NULL"));
```

**Verify Configuration:**
```java
if (ranksSection == null) {
    getLogger().severe("Ranks section is null!");
    return;
}
getLogger().info("Loaded " + ranksSection.getKeys(false).size() + " ranks");
```

## Build and Deployment

### Maven Configuration

**pom.xml essentials:**
```xml
<dependencies>
    <!-- Paper API -->
    <dependency>
        <groupId>io.papermc.paper</groupId>
        <artifactId>paper-api</artifactId>
        <version>1.19.4-R0.1-SNAPSHOT</version>
        <scope>provided</scope>
    </dependency>
    
    <!-- Vault API -->
    <dependency>
        <groupId>com.github.MilkBowl</groupId>
        <artifactId>VaultAPI</artifactId>
        <version>1.7</version>
        <scope>provided</scope>
    </dependency>
    
    <!-- LuckPerms API -->
    <dependency>
        <groupId>net.luckperms</groupId>
        <artifactId>api</artifactId>
        <version>5.4</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

**Note:** All dependencies are `provided` scope because they're available on the server.

### Plugin.yml

```yaml
name: Ranks
version: 1.0.0
main: com.supafloof.Ranks
api-version: 1.19
depend: [Vault, LuckPerms]
author: SupaFloof Games, LLC

commands:
  ranks:
    description: Rank progression system
    usage: /ranks [help|next|upgrade|reload]
    permission: ranks.use

permissions:
  ranks.use:
    description: Access to ranks commands
    default: true
  ranks.reload:
    description: Reload configuration
    default: op
```

### Build Process

```bash
mvn clean package
```

Output: `target/Ranks.jar`

### Deployment

1. Stop server
2. Place `Ranks.jar` in `plugins/` folder
3. Ensure Vault, economy plugin, and LuckPerms are present
4. Start server
5. Check console for green startup message
6. Configure `plugins/Ranks/config.yml`
7. Run `/ranks reload` or restart

## Future Enhancement Ideas

### Potential Features

1. **GUI Interface**
   - Replace text display with inventory GUI
   - Click ranks to upgrade
   - Visual progress bars

2. **Rank Requirements**
   - Level requirements
   - Permission requirements
   - Time-played requirements

3. **Rank Rewards**
   - Item rewards on rank-up
   - Command rewards beyond LuckPerms
   - Title/effect rewards

4. **Statistics Tracking**
   - Track rank-up history
   - Time spent in each rank
   - Money spent on ranks

5. **Multiple Rank Paths**
   - Implement path selection
   - Path switching
   - Path-specific rewards

6. **Prestige System**
   - Reset ranks and start over
   - Prestige-specific benefits
   - Prestige level display

### Technical Improvements

1. **Database Storage**
   - Store rank history
   - Track upgrades
   - Enable statistics

2. **PlaceholderAPI Integration**
   - Expose rank data to other plugins
   - Custom placeholders for ranks
   - Integration with chat plugins

3. **Bossbar Progress Display**
   - Show progress to next rank
   - Visual money/rank tracking

4. **Economy Hooks**
   - Support multiple economies
   - Per-rank currency types
   - Alternative payment methods

## Conclusion

The Ranks plugin provides a solid foundation for rank progression systems. Its single-file architecture, comprehensive comments, and clear structure make it easy to understand and modify. The plugin demonstrates best practices for:
- Dependency management
- Event handling
- Command processing
- Configuration usage
- Text component formatting

Developers can use this as a reference for creating similar progression systems or extend it to add custom features for their servers.

---

*Plugin by SupaFloof Games, LLC*
