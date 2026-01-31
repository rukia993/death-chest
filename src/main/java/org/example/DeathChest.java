package org.example;

import org.bukkit.plugin.java.JavaPlugin;
import org.example.listeners.DeathListener;
import org.example.managers.ChestManager;

public class DeathChest extends JavaPlugin {

    private static DeathChest instance;
    private ChestManager chestManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        chestManager = new ChestManager(this);
        getServer().getPluginManager().registerEvents(new DeathListener(this), this);
        getLogger().info("DeathChest запущен!");
    }

    @Override
    public void onDisable() {
        chestManager.removeAllChests();
        getLogger().info("DeathChest выключен!");
    }

    public static DeathChest getInstance() {
        return instance;
    }

    public ChestManager getChestManager() {
        return chestManager;
    }
}
