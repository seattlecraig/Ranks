# Ranks Plugin - End User Guide

## What is Ranks?

Ranks is a progression system that lets you advance through different ranks by spending in-game currency. As you earn money on the server, you can purchase higher ranks that come with new permissions, perks, and prestige.

## Getting Started

When you first join the server, you'll automatically be assigned to the first rank in the progression system, Mortal. From there, you can work your way up through the ranks by earning money and purchasing upgrades.

## Commands

### Viewing Ranks

**Command:** `/ranks`

This command displays all available ranks in the progression system along with:
- Your current rank (marked with a green arrow: `<----`)
- Which ranks you've completed (marked with ✓)
- Which ranks you haven't reached yet (marked with ✗)
- The cost of each rank
- Your current balance
- How much more money you need for ranks you can't afford yet

**Example output:**
```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Ranks for YourName | Balance: $1,234,567
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
✓ [Mortal] - $0
✓ [Adept] - $40,000 <----
✗ [Hero] - $200,000 (Need $5,000 more)
✗ [Paragon] - $800,000 (Need $605,000 more)
...
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

### Getting Help

**Command:** `/ranks help`

Shows a list of all available commands with descriptions.

### Upgrading to the Next Rank

**Command:** `/ranks next`

This command attempts to upgrade you to the next rank in the progression. The plugin will:
1. Check if you're already at the highest rank
2. Check if the next rank can be purchased with currency
3. Check if you have enough money
4. If everything checks out, withdraw the cost and upgrade you

**What happens when you rank up:**
- Money is deducted from your account
- A broadcast message announces your achievement to all online players
- Your permissions are automatically updated via LuckPerms
- You gain access to any perks or abilities associated with the new rank

**Example:**
```
/ranks next
```

If successful, all players will see something like:
```
─────────────────
PlayerName has been promoted to [Hero]!
─────────────────
```

**Common errors:**
- `"You don't have enough money!"` - You need to earn more money before you can afford this rank
- `"This rank cannot be purchased with currency."` - This rank must be obtained through other means (like donations)
- `"You are already at the highest rank!"` - Congratulations, you've reached the top!

### Skipping Ranks

**Command:** `/ranks upgrade <rankname>`

This command lets you skip multiple ranks at once and jump directly to a higher rank. When you do this:
- You pay the **cumulative cost** of all ranks between your current rank and the target rank
- All intermediate ranks are processed (you receive their broadcasts and permissions)
- This is faster than upgrading one rank at a time

**Example:**
```
/ranks upgrade hero
```

If you're currently [Adept] and upgrade to [Hero], you'll pay the cost of [Hero]. 

If you're currently [Mortal] and upgrade to [Hero], you'll pay the cost of both [Adept] and [Hero] combined.

**Important notes:**
- The rank name is case-insensitive (`hero`, `Hero`, and `HERO` all work)
- You cannot skip to a rank you already have or a lower rank
- If any rank in the path cannot be purchased with currency, the entire upgrade will fail
- Make sure you type the rank name correctly - the plugin won't guess what you meant

**Common errors:**
- `"Rank 'xyz' not found!"` - You typed the rank name incorrectly
- `"You already have this rank or a higher one!"` - You're trying to downgrade or stay at your current rank
- `"Cannot upgrade to [X] because [Y] is in the path and cannot be purchased with currency."` - you need to buy that rank @ https://pixels.supafloof.com

## Understanding Rank Costs

There are three types of ranks:

### Free Ranks (Cost: $0)
These are usually the starting rank. Everyone gets these automatically when they join.

### Purchasable Ranks (Cost: $X)
These ranks have a specific money cost. You can buy them using in-game currency that you've earned through playing.

### Non-Purchasable Ranks 
These ranks cannot be bought with in-game currency. You need to buy them on the web store, https://pixels.supafloof.com. 

## Tips and Strategies

### Planning Your Upgrades

Use `/ranks` regularly to:
- Track your progress toward the next rank
- See how much money you need to save
- Plan whether to upgrade one rank at a time or save up for a multi-rank jump

### When to Use /ranks next vs /ranks upgrade

**Use `/ranks next` when:**
- You want to upgrade one rank at a time
- You want to celebrate each rank achievement separately
- You're close to affording the next rank

**Use `/ranks upgrade <rank>` when:**
- You've saved up a lot of money and want to skip ahead
- You want to reach a specific rank quickly
- You don't mind paying the cumulative cost upfront

### Money Management

- Check your balance before attempting an upgrade
- The plugin will tell you exactly how much more money you need if you can't afford a rank
- When using `/ranks upgrade`, make sure you have enough for the cumulative cost of all intermediate ranks

## Frequently Asked Questions

**Q: Do I lose my old rank's permissions when I upgrade?**
A: No! All previous permissions are preserved or replaced with better ones.

**Q: Can I downgrade to a lower rank?**
A: No, the progression only goes upward. Once you've achieved a rank, you cannot go back to a lower one.

**Q: What happens if I don't have enough money?**
A: The plugin will tell you exactly how much you need and how much you currently have. No money is deducted unless the upgrade succeeds.

**Q: Can I see other players' ranks?**
A: Yes! When someone ranks up, a broadcast message is sent to all online players. You can also often see ranks in chat prefixes or player name tags (this depends on your server's configuration).

**Q: Why can't I purchase a certain rank?**
A: The highest ranks can only be bought at https://pixels.supafloof.com

## Need More Help?

If you encounter any issues or have questions:
1. Use `/ranks help` to see all available commands
2. Contact a server administrator
3. Check if the rank you're trying to upgrade to is purchasable with in-game currency

---

**Made with ❤️ by SupaFloof Games, LLC**