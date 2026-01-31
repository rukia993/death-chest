package org.example.managers;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.example.DeathChest;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ChestManager {

    private static final BlockFace[] ADJACENT_FACES = {BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};

    private final DeathChest plugin;
    private final Map<Location, UUID> chestOwners;
    private final Map<Location, BukkitRunnable> chestTasks;

    public ChestManager(DeathChest plugin) {
        this.plugin = plugin;
        this.chestOwners = new HashMap<>();
        this.chestTasks = new HashMap<>();
    }

    public Location createDeathChest(Player player, ItemStack[] items) {
        Location deathLocation = findSafeLocation(player.getLocation());
        Block block = deathLocation.getBlock();

        if (hasAdjacentChest(block)) {
            block.setType(Material.TRAPPED_CHEST);
        } else {
            block.setType(Material.CHEST);
        }

        Chest chest = (Chest) block.getState();
        for (ItemStack item : items) {
            if (item != null) {
                chest.getInventory().addItem(item);
            }
        }

        Location normalizedLocation = block.getLocation();
        chestOwners.put(normalizedLocation, player.getUniqueId());
        startRemovalTask(normalizedLocation);

        return normalizedLocation;
    }

    private boolean hasAdjacentChest(Block block) {
        for (BlockFace face : ADJACENT_FACES) {
            Material type = block.getRelative(face).getType();
            if (type == Material.CHEST || type == Material.TRAPPED_CHEST) {
                return true;
            }
        }
        return false;
    }

    private Location findSafeLocation(Location location) {
        Location safe = location.getBlock().getLocation();
        safe.setY(Math.max(safe.getY(), safe.getWorld().getMinHeight() + 1));

        for (int y = (int) safe.getY(); y <= safe.getWorld().getMaxHeight(); y++) {
            safe.setY(y);
            Block block = safe.getBlock();
            Material type = block.getType();

            if (type == Material.AIR || type == Material.CAVE_AIR || type == Material.VOID_AIR) {
                if (!isNearLiquid(block)) {
                    return safe;
                }
            }
        }

        return location.getBlock().getLocation();
    }

    private boolean isNearLiquid(Block block) {
        if (block.isLiquid()) {
            return true;
        }
        for (BlockFace face : BlockFace.values()) {
            if (face == BlockFace.SELF) continue;
            if (block.getRelative(face).isLiquid()) {
                return true;
            }
        }
        return false;
    }

    private void startRemovalTask(Location location) {
        int seconds = plugin.getConfig().getInt("chest-duration", 300);

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                removeChest(location);
            }
        };

        task.runTaskLater(plugin, seconds * 20L);
        chestTasks.put(location, task);
    }

    public void removeChest(Location location) {
        Location normalized = location.getBlock().getLocation();
        if (chestOwners.containsKey(normalized)) {
            Block block = normalized.getBlock();
            if (block.getType() == Material.CHEST || block.getType() == Material.TRAPPED_CHEST) {
                Chest chest = (Chest) block.getState();
                chest.getInventory().clear();
                block.setType(Material.AIR);
            }
            chestOwners.remove(normalized);

            BukkitRunnable task = chestTasks.remove(normalized);
            if (task != null) {
                task.cancel();
            }
        }
    }

    public void removeAllChests() {
        for (Location location : chestOwners.keySet()) {
            Block block = location.getBlock();
            if (block.getType() == Material.CHEST || block.getType() == Material.TRAPPED_CHEST) {
                Chest chest = (Chest) block.getState();
                chest.getInventory().clear();
                block.setType(Material.AIR);
            }
        }
        chestTasks.values().forEach(BukkitRunnable::cancel);
        chestOwners.clear();
        chestTasks.clear();
    }

    public boolean isOwner(Location location, UUID playerId) {
        Location normalized = location.getBlock().getLocation();
        return playerId.equals(chestOwners.get(normalized));
    }

    public boolean isDeathChest(Location location) {
        Location normalized = location.getBlock().getLocation();
        return chestOwners.containsKey(normalized);
    }
}
