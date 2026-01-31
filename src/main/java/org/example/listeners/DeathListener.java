package org.example.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.example.DeathChest;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DeathListener implements Listener {

    private static final String BYPASS_PERMISSION = "deathchest.bypass";

    private final DeathChest plugin;
    private final Map<UUID, Location> pendingMessages;

    public DeathListener(DeathChest plugin) {
        this.plugin = plugin;
        this.pendingMessages = new HashMap<>();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent e) {
        Player player = e.getEntity();
        List<ItemStack> drops = e.getDrops();

        if (drops.isEmpty()) {
            return;
        }

        ItemStack[] items = drops.toArray(new ItemStack[0]);
        Location chestLocation = plugin.getChestManager().createDeathChest(player, items);
        drops.clear();

        pendingMessages.put(player.getUniqueId(), chestLocation);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent e) {
        Player player = e.getPlayer();
        UUID playerId = player.getUniqueId();

        if (pendingMessages.containsKey(playerId)) {
            Location loc = pendingMessages.remove(playerId);

            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                player.sendMessage(Component.text("Ваши вещи находятся в сундуке: ", NamedTextColor.GOLD)
                        .append(Component.text("X: " + loc.getBlockX() + " Y: " + loc.getBlockY() + " Z: " + loc.getBlockZ(), NamedTextColor.YELLOW)));

                int duration = plugin.getConfig().getInt("chest-duration", 300);
                player.sendMessage(Component.text("Сундук исчезнет через " + (duration / 60) + " минут.", NamedTextColor.GRAY));
            }, 20L);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryOpen(InventoryOpenEvent e) {
        if (!(e.getPlayer() instanceof Player player)) {
            return;
        }

        Location location = e.getInventory().getLocation();
        if (location == null) {
            return;
        }

        if (plugin.getChestManager().isDeathChest(location)) {
            if (!plugin.getChestManager().isOwner(location, player.getUniqueId()) && !player.hasPermission(BYPASS_PERMISSION)) {
                e.setCancelled(true);
                player.sendMessage(Component.text("Это не ваш сундук смерти!", NamedTextColor.RED));
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockBreak(BlockBreakEvent e) {
        Location location = e.getBlock().getLocation();

        if (plugin.getChestManager().isDeathChest(location)) {
            Player player = e.getPlayer();

            if (!plugin.getChestManager().isOwner(location, player.getUniqueId()) && !player.hasPermission(BYPASS_PERMISSION)) {
                e.setCancelled(true);
                player.sendMessage(Component.text("Это не ваш сундук смерти!", NamedTextColor.RED));
            } else {
                plugin.getChestManager().removeChest(location);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityExplode(EntityExplodeEvent e) {
        Iterator<Block> iterator = e.blockList().iterator();
        while (iterator.hasNext()) {
            Block block = iterator.next();
            if (plugin.getChestManager().isDeathChest(block.getLocation())) {
                iterator.remove();
            }
        }
    }
}
