# Ranks Plugin - Server Administrator Guide

## Overview

Ranks is a comprehensive rank progression system that integrates with LuckPerms and Vault to provide players with a currency-based rank advancement system. This guide covers installation, configuration, and administration.

## Dependencies

### Required
- **Vault** - Provides economy integration
- **An Economy Plugin** - Such as EssentialsX, CMI, or any other Vault-compatible economy plugin
- **LuckPerms** - For permission group management

The plugin will **not enable** if any of these dependencies are missing.

## Installation

1. Ensure Vault, an economy plugin, and LuckPerms are installed and running
2. Download `Ranks.jar` and place it in your server's `plugins/` folder
3. Start or restart your server
4. The plugin will create a default `config.yml` in `plugins/Ranks/`
5. Edit the configuration to match your server's rank structure
6. Run `/ranks reload` in-game or restart the server to apply changes

## Configuration

### Main Configuration File (`config.yml`)

The configuration file defines your rank progression system.

#### Structure Overview

```yaml
defaultpath: default

Ranks:
  default:
    'rankname':
      display: "&7[Rank Display Name]"
      nextrank: nextrankname
      cost: 0
      broadcast:
      - "Broadcast message line 1"
      - "Broadcast message line 2"
      executecmds:
      - "[console] command to execute"
```

#### Configuration Fields

**`defaultpath`**
- Type: String
- Description: The rank path to use (supports multiple rank trees)
- Default: `default`
- Usage: Allows you to have different rank progressions (e.g., "default", "donor", "staff")

**Rank Configuration**

Each rank must have the following fields:

**`display`**
- Type: String
- Description: The display name shown to players with color codes
- Supports: Minecraft legacy color codes (`&7`, `&a`, `&l`, `&m`, etc.)
- Example: `"&7[Mortal]"`, `"&a[Adept]"`, `"&4&l[Ancient God]"`

**`nextrank`**
- Type: String
- Description: The name of the next rank in progression
- Special Value: `LASTRANK` - Indicates this is the final rank
- Must match: The key name of another rank in the config (case-sensitive)

**`cost`**
- Type: Number
- Description: The cost in currency to purchase this rank
- Special Values:
  - `0` - Free rank (typically the starting rank)
  - `-1` - Cannot be purchased with currency (donor/premium ranks)
  - Any positive number - Currency cost
- Example: `40000`, `200000`, `1000000000`

**`cost-message`** (optional, only used when cost is -1)
- Type: String
- Description: Custom message shown when players try to purchase a non-purchasable rank
- Example: `"Purchase at www.yourserver.com/store"`

**`broadcast`**
- Type: List of Strings
- Description: Messages sent to all online players when someone achieves this rank
- Supports: 
  - Legacy color codes
  - Variable: `%player%` (replaced with player's name)
  - Decorative characters: `&m` for strikethrough
- Can be empty: `broadcast: []` for silent rank-ups

**`executecmds`**
- Type: List of Strings
- Description: Commands executed when a player achieves this rank
- Format:
  - `"[console] command"` - Executes from console (recommended for LuckPerms commands)
  - `"command"` - Executes as the player
- Supports: Variable `%player%` (replaced with player's name)
- Typical Usage: LuckPerms group add/remove commands

### Example Configuration

```yaml
defaultpath: default

Ranks:
  default:
    'mortal':
      display: "&7[Mortal]"
      nextrank: adept
      cost: 0
      broadcast: []  # Silent for starting rank
      executecmds: []
      
    'adept':
      display: "&a[Adept]"
      nextrank: hero
      cost: 40000
      broadcast:
      - "&7&m-----------------"
      - "&e%player% &fhas been promoted to &a&l[Adept]!"
      - "&7&m-----------------"
      executecmds:
      - "[console] lp user %player% parent add adept"
      - "[console] lp user %player% parent remove mortal"
      
    'hero':
      display: "&2[Hero]"
      nextrank: legend
      cost: 200000
      broadcast:
      - "&7&m-----------------"
      - "&e%player% &fhas been promoted to &2&l[Hero]!"
      - "&7&m-----------------"
      executecmds:
      - "[console] lp user %player% parent add hero"
      - "[console] lp user %player% parent remove adept"
      
    'legend':
      display: "&6[Legend]"
      nextrank: LASTRANK
      cost: -1
      cost-message: "Purchase at www.yourserver.com/store"
      broadcast:
      - "&7&m-----------------"
      - "&c&lAll hail &e%player%&c&l, who has achieved &6&l[Legend]!"
      - "&7&m-----------------"
      executecmds:
      - "[console] lp user %player% parent add legend"
      - "[console] lp user %player% parent remove hero"
```

## LuckPerms Integration

### Group Setup

For the plugin to work correctly, you need to create LuckPerms groups that match your rank names.

**Example setup for a rank called "adept":**

```bash
/lp creategroup adept
/lp group adept permission set ranks.use true
/lp group adept parent add default
# Add any other permissions you want adept players to have
```

**Important Notes:**
- Group names in LuckPerms must match the rank keys in config.yml (case-sensitive)
- The plugin automatically adds/removes parent groups when players rank up
- Make sure your LuckPerms inheritance is set up correctly
- Each rank should inherit from the previous rank or have all necessary permissions

### Recommended LuckPerms Command Pattern

For each rank, your `executecmds` should typically:
1. Add the new group
2. Remove the old group

```yaml
executecmds:
- "[console] lp user %player% parent add newrank"
- "[console] lp user %player% parent remove oldrank"
```

### Handling the Default Group

The plugin automatically migrates players from the "default" LuckPerms group to your first rank (cost: 0) when they join. This ensures players start in your rank progression system rather than the generic default group.

**Setup:**
1. Create your first rank with `cost: 0`
2. The plugin will automatically move players from "default" to this rank on join
3. Your `executecmds` for the first rank should remove the default group

## Permissions

The plugin uses two permissions:

**`ranks.use`**
- Default: `true` (all players)
- Grants: Access to `/ranks`, `/ranks help`, `/ranks next`, `/ranks upgrade`
- Recommended: Keep default for all players

**`ranks.reload`**
- Default: `op` (operators only)
- Grants: Access to `/ranks reload`
- Recommended: Restrict to staff/admins only

## Commands

### Player Commands

**`/ranks`**
- Permission: `ranks.use`
- Description: Displays all ranks with progress, costs, and player's current rank
- Usage: Players use this to see what ranks are available

**`/ranks help`**
- Permission: `ranks.use`
- Description: Shows command help
- Usage: Help for players

**`/ranks next`**
- Permission: `ranks.use`
- Description: Upgrades player to the next rank in progression
- Process:
  1. Checks if next rank exists
  2. Checks if next rank is purchasable
  3. Checks player's balance
  4. Deducts cost
  5. Executes rank-up

**`/ranks upgrade <rank>`**
- Permission: `ranks.use`
- Description: Upgrades player to a specific rank (skipping intermediate ranks)
- Process:
  1. Validates target rank exists
  2. Validates target rank is higher than current
  3. Calculates cumulative cost of all intermediate ranks
  4. Checks for unpurchasable ranks in path
  5. Checks player's balance
  6. Deducts cumulative cost
  7. Executes rank-up for each intermediate rank

### Admin Commands

**`/ranks reload`**
- Permission: `ranks.reload` (OP)
- Description: Reloads the configuration from disk
- Usage: After editing config.yml, run this command to apply changes without restarting
- Note: Online players' ranks are not affected; only new upgrades use the new configuration

## Console Messages

The plugin follows standard console message color patterns:

**Startup (Green):**
```
[Ranks] Ranks Started!
```

**Author Credit (Magenta):**
```
[Ranks] By SupaFloof Games, LLC
```

**Shutdown (Red):**
```
[Ranks] Ranks Disabled!
```

**Errors (Severe):**
```
Vault not found! This plugin requires Vault and an economy plugin.
LuckPerms not found! This plugin requires LuckPerms.
```

## Troubleshooting

### Plugin Won't Enable

**Issue:** Plugin disables on startup

**Possible Causes:**
1. **Vault not found**
   - Solution: Install Vault plugin
   - Verify: Check `plugins/` folder for Vault.jar

2. **Economy plugin not found**
   - Solution: Install an economy plugin (EssentialsX, CMI, etc.)
   - Verify: Run `/plugins` and check for your economy plugin

3. **LuckPerms not found**
   - Solution: Install LuckPerms
   - Verify: Run `/lp` to confirm it's working

### Players Can't Upgrade

**Issue:** Players get error when trying to upgrade

**Possible Causes:**
1. **"You don't have enough money!"**
   - Cause: Player's balance is less than the rank cost
   - Solution: Player needs to earn more money
   - Verify: Check player's balance with your economy plugin

2. **"This rank cannot be purchased with currency"**
   - Cause: Rank has `cost: -1`
   - Solution: This is intentional for donor ranks
   - Verify: Check if the rank should be purchasable

3. **"You are already at the highest rank!"**
   - Cause: Player's current rank has `nextrank: LASTRANK`
   - Solution: This is correct if they're at max rank
   - Verify: Check player's current rank with `/lp user <player> info`

4. **"Error: Could not find your current rank!"**
   - Cause: Player's LuckPerms group doesn't match any rank in config
   - Solution: Manually set player's group to a valid rank
   - Verify: Run `/lp user <player> parent set <rankname>`

### Players Not Migrating from Default Group

**Issue:** New players stay in "default" group instead of being moved to first rank

**Possible Causes:**
1. **First rank doesn't have cost: 0**
   - Solution: Make sure your starting rank has `cost: 0`
   - Example: 
     ```yaml
     'mortal':
       cost: 0
     ```

2. **executecmds not removing default group**
   - Solution: Add command to remove default group:
     ```yaml
     executecmds:
     - "[console] lp user %player% parent add mortal"
     - "[console] lp user %player% parent remove default"
     ```

3. **Plugin loaded after player joined**
   - Solution: Have player rejoin or reload the plugin
   - Note: The plugin processes online players during enable

### Rank-Up Not Working Properly

**Issue:** Money deducted but permissions not updated

**Possible Causes:**
1. **LuckPerms commands incorrect**
   - Solution: Verify executecmds syntax:
     ```yaml
     executecmds:
     - "[console] lp user %player% parent add newrank"
     - "[console] lp user %player% parent remove oldrank"
     ```
   
2. **LuckPerms group doesn't exist**
   - Solution: Create the group in LuckPerms first
   - Command: `/lp creategroup <rankname>`

3. **Commands missing [console] prefix**
   - Solution: LuckPerms commands need console permissions:
     ```yaml
     - "[console] lp user %player% parent add newrank"  # Correct
     - "lp user %player% parent add newrank"            # Wrong - may fail
     ```

### Multi-Rank Upgrade Issues

**Issue:** `/ranks upgrade <rank>` fails or behaves unexpectedly

**Possible Causes:**
1. **Unpurchasable rank in path**
   - Cause: One or more ranks between current and target have `cost: -1`
   - Solution: This is intended behavior; player must obtain the unpurchasable rank first
   - Message: "Cannot upgrade to [X] because [Y] is in the path..."

2. **Insufficient funds for cumulative cost**
   - Cause: Player has enough for target rank alone but not cumulative cost
   - Solution: Player needs to earn more money
   - Example: Upgrading from Mortal to Hero requires Adept cost + Hero cost

3. **Rank name typo**
   - Cause: Player misspelled the rank name
   - Solution: Rank names are case-insensitive, but must match config keys
   - Verify: Check config.yml for exact rank names

### Configuration Issues

**Issue:** Changes to config.yml not taking effect

**Solution:**
1. Save your config.yml file
2. Run `/ranks reload` in-game
3. OR restart the server
4. Verify changes took effect by using `/ranks` command

**Issue:** Broadcast messages not showing correctly

**Possible Causes:**
1. **Color codes not working**
   - Solution: Use `&` not `§` for color codes
   - Example: `&a` for green, `&c` for red

2. **%player% not replaced**
   - Solution: Make sure you're using `%player%` not `{player}` or `<player>`
   - Example: `"&e%player% &fhas been promoted!"`

## Best Practices

### Configuration Design

1. **Start with cost: 0**
   - Always have your first rank cost 0
   - This ensures all players can enter the system

2. **Progressive cost scaling**
   - Make each rank significantly more expensive than the last
   - Example: 40k, 200k, 800k, 4M, 20M

3. **Clear progression chain**
   - Make sure each rank's `nextrank` points to a valid rank
   - End the chain with `nextrank: LASTRANK`

4. **Meaningful broadcasts**
   - Use broadcasts to celebrate player achievements
   - Include decorative elements for impact

5. **Test your executecmds**
   - Verify LuckPerms commands work before going live
   - Test with a dummy account first

### LuckPerms Setup

1. **Create all groups in advance**
   - Don't wait for players to rank up to create groups
   - Pre-create all rank groups in LuckPerms

2. **Set up inheritance properly**
   - Each rank should inherit previous rank's permissions
   - OR give each rank all permissions it needs

3. **Use parent add/remove pattern**
   - Always add new group AND remove old group
   - This prevents players from having multiple rank groups

4. **Test permission inheritance**
   - Verify players actually get new permissions
   - Test on a dummy account before going live

### Economy Integration

1. **Balance your costs**
   - Consider your economy's money generation rate
   - Make ranks achievable but challenging

2. **Test transactions**
   - Verify money is actually deducted
   - Check for edge cases (exactly enough money, not enough, etc.)

3. **Monitor player progression**
   - Track how long it takes players to reach ranks
   - Adjust costs if progression is too fast or too slow

### Donor Rank Integration

1. **Use cost: -1 for donor ranks**
   - This prevents players from buying them with in-game money

2. **Provide clear cost-message**
   - Tell players where to donate
   - Example: `"Purchase at yourserver.com/store"`

3. **Place donor ranks appropriately**
   - Can be at the end of progression
   - OR interspersed throughout (blocks multi-rank jumps)

## Maintenance

### Regular Tasks

**Weekly:**
- Monitor console for errors
- Check if players are reporting issues
- Verify rank-ups are working correctly

**Monthly:**
- Review player progression rates
- Consider adjusting costs if needed
- Update donor rank offerings

**After Updates:**
- Test rank commands
- Verify LuckPerms integration still works
- Check economy integration

### Backup Recommendations

Always backup before making changes:
1. **LuckPerms data** - `plugins/LuckPerms/`
2. **Ranks config** - `plugins/Ranks/config.yml`
3. **Economy data** - Your economy plugin's data folder

## Common Customizations

### Multiple Rank Paths

You can create multiple rank progressions:

```yaml
defaultpath: survival  # Choose which path is default

Ranks:
  survival:
    'mortal':
      # ... survival ranks
      
  creative:
    'builder':
      # ... creative ranks
```

### Rank Skipping

Allow players to skip ranks by:
1. Setting intermediate ranks to `cost: 0`
2. OR using only `/ranks upgrade <rank>` and hiding `/ranks next`

### Silent Rank-Ups

Remove broadcasts for subtle progression:
```yaml
broadcast: []
```

### Custom Rank Actions

Execute any commands on rank-up:
```yaml
executecmds:
- "[console] lp user %player% parent add newrank"
- "[console] give %player% diamond 10"
- "[console] title %player% title {\"text\":\"Ranked Up!\",\"color\":\"gold\"}"
```

## Support

For issues or questions:
1. Check this documentation
2. Verify dependencies are installed
3. Check console for error messages
4. Test with a clean configuration
5. Contact SupaFloof Games, LLC for support

---

*Plugin by SupaFloof Games, LLC*
