# Ranks

A comprehensive rank progression system for Minecraft Paper/Spigot servers with economy integration and automatic permission management.

![Minecraft Version](https://img.shields.io/badge/Minecraft-1.19+-green.svg)
![Java](https://img.shields.io/badge/Java-17+-blue.svg)
![License](https://img.shields.io/badge/License-MIT-yellow.svg)

## 🌟 Features

- **💰 Economy Integration** - Players purchase ranks using in-game currency via Vault
- **🎯 Smart Progression** - Single-step upgrades or multi-rank jumps with automatic cost calculation
- **⚙️ Automatic Permission Management** - Seamless LuckPerms integration for group assignments
- **📊 Visual Progress Display** - See all ranks, costs, and your progress at a glance
- **🎉 Broadcast System** - Celebrate rank-ups with configurable server-wide messages
- **🔒 Non-Purchasable Ranks** - Support for donor/premium ranks that can't be bought with currency
- **🎨 Fully Customizable** - Configure rank names, colors, costs, commands, and messages
- **🔄 Hot Reload** - Update configuration without restarting the server
- **⚡ Lightweight** - Single-file architecture with minimal performance impact

## 📋 Requirements

- **Minecraft Server:** Paper/Spigot 1.19+
- **Java:** 17 or higher
- **Dependencies:**
  - [Vault](https://www.spigotmc.org/resources/vault.34315/) - Economy API
  - An economy plugin (e.g., [EssentialsX](https://essentialsx.net/), CMI)
  - [LuckPerms](https://luckperms.net/) - Permission management

## 🚀 Quick Start

1. **Install Dependencies**
   - Download and install Vault, an economy plugin, and LuckPerms
   - Ensure all plugins are running properly

2. **Install Ranks**
   - Download `Ranks.jar` from the [releases page](../../releases)
   - Place it in your server's `plugins/` folder
   - Start or restart your server

3. **Configure**
   - Edit `plugins/Ranks/config.yml` to define your rank structure
   - Set up matching LuckPerms groups for each rank
   - Run `/ranks reload` or restart the server

4. **Test**
   - Join your server and type `/ranks`
   - Try upgrading: `/ranks next`

## 📖 Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/ranks` | View all ranks and your progress | `ranks.use` |
| `/ranks help` | Show command help | `ranks.use` |
| `/ranks next` | Upgrade to the next rank | `ranks.use` |
| `/ranks upgrade <rank>` | Jump to a specific rank | `ranks.use` |
| `/ranks reload` | Reload configuration | `ranks.reload` (OP) |

## 🎮 How It Works

### For Players

Players earn money through gameplay and use it to purchase rank upgrades:

```
/ranks                    # See all available ranks
/ranks next              # Upgrade to next rank
/ranks upgrade hero      # Skip ahead to Hero rank
```

### Progression Example

```
[Mortal] (Free) → [Adept] ($40,000) → [Hero] ($200,000) → [Paragon] ($800,000) → ...
```

Players can either:
- **Step-by-step:** Use `/ranks next` to go one rank at a time
- **Skip ahead:** Use `/ranks upgrade <rank>` to jump multiple ranks (pays cumulative cost)

### Visual Display

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Ranks for PlayerName | Balance: $1,234,567
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
✓ [Mortal] - $0
✓ [Adept] - $40,000 <----
✗ [Hero] - $200,000 (Need $5,000 more)
✗ [Paragon] - $800,000 (Can afford)
...
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

## ⚙️ Configuration

### Basic Structure

```yaml
defaultpath: default

Ranks:
  default:
    'mortal':
      display: "&7[Mortal]"
      nextrank: adept
      cost: 0
      broadcast:
      - "&7&m-----------------"
      - "&e%player% &fhas been promoted to &7[Mortal]!"
      - "&7&m-----------------"
      executecmds:
      - "[console] lp user %player% parent add mortal"
      - "[console] lp user %player% parent remove default"
```

### Rank Properties

- **`display`** - How the rank appears to players (supports color codes)
- **`nextrank`** - The next rank in progression (use `LASTRANK` for the final rank)
- **`cost`** - Price in currency (`0` = free, `-1` = non-purchasable)
- **`cost-message`** - Custom message for non-purchasable ranks
- **`broadcast`** - Messages sent to all players on rank-up (supports `%player%` variable)
- **`executecmds`** - Commands run on rank-up (use `[console]` prefix for elevated permissions)

### Non-Purchasable Ranks

Perfect for donor/premium ranks:

```yaml
'vip':
  display: "&d[VIP]"
  nextrank: vipplus
  cost: -1
  cost-message: "Purchase at www.yourserver.com/store"
  broadcast:
  - "&d%player% is now VIP!"
  executecmds:
  - "[console] lp user %player% parent add vip"
```

## 🔗 LuckPerms Integration

The plugin automatically manages LuckPerms groups when players rank up:

1. **Create Groups**
   ```bash
   /lp creategroup mortal
   /lp creategroup adept
   /lp creategroup hero
   ```

2. **Set Permissions**
   ```bash
   /lp group adept permission set some.cool.permission true
   ```

3. **Configure executecmds**
   ```yaml
   executecmds:
   - "[console] lp user %player% parent add adept"
   - "[console] lp user %player% parent remove mortal"
   ```

The plugin handles the rest automatically!

## 📚 Documentation

Comprehensive guides for all user types:

- **[End User Guide](enduser.md)** - For players using the ranks system
- **[Server Admin Guide](serveradmin.md)** - For server administrators setting up the plugin
- **[Developer Guide](devguide.md)** - For developers wanting to understand or modify the code

## 🎯 Use Cases

### Survival Servers
Progressive ranks based on playtime and achievement:
- Gather resources → earn money → buy ranks → unlock perks

### Economy Servers
Money-driven progression with escalating costs:
- Jobs, shops, and trading → accumulate wealth → prestige ranks

### Donor Integration
Mix purchasable and donor ranks:
- Free ranks → Purchasable ranks → Donor ranks (non-purchasable)

### Multiple Paths
Different progression tracks (requires minor config changes):
- Survival path, Creative path, Minigame path, etc.

## 💡 Why Ranks?

### Easy to Set Up
- Single JAR file
- Intuitive configuration
- Automatic player migration from default group

### Flexible Design
- Any number of ranks
- Any cost structure
- Fully customizable messages and commands

### Performance Optimized
- Lightweight single-file plugin
- Asynchronous player data loading
- Minimal memory footprint

### Developer Friendly
- Comprehensive inline documentation
- Clear code structure
- Easy to extend and modify

## 🛠️ Building from Source

```bash
git clone https://github.com/yourusername/Ranks.git
cd Ranks
mvn clean package
```

The compiled JAR will be in `target/Ranks.jar`.

## 🐛 Troubleshooting

### Plugin Won't Enable
- **Error:** "Vault not found!"
  - Solution: Install Vault and an economy plugin
- **Error:** "LuckPerms not found!"
  - Solution: Install LuckPerms

### Players Can't Upgrade
- **"You don't have enough money!"**
  - Player needs more currency
- **"This rank cannot be purchased with currency"**
  - Rank has `cost: -1` (donor rank)
- **"Error: Could not find your current rank!"**
  - Player's LuckPerms group doesn't match any rank
  - Set player to a valid rank: `/lp user <player> parent set <rank>`

### Permissions Not Working
- Ensure LuckPerms groups match rank names exactly (case-sensitive)
- Verify executecmds use `[console]` prefix
- Check that groups exist: `/lp listgroups`

See the [Server Admin Guide](serveradmin.md) for detailed troubleshooting.

## 📝 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🤝 Contributing

Contributions are welcome! Please feel free to submit pull requests or open issues for bugs and feature requests.

## 💬 Support

- **Issues:** [GitHub Issues](../../issues)
- **Discussions:** [GitHub Discussions](../../discussions)

## 🏆 Credits

**Plugin by [SupaFloof Games, LLC](https://supafloof.com)**

---

## ⭐ Show Your Support

If you like this plugin, please consider:
- ⭐ Starring this repository
- 🐛 Reporting bugs
- 💡 Suggesting features
- 📖 Contributing to documentation

---

*Made with ❤️ for the Minecraft community*
