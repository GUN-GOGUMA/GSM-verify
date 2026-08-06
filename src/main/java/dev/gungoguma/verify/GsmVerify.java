package dev.gungoguma.verify;

import org.bukkit.plugin.java.JavaPlugin;

public final class GsmVerify extends JavaPlugin {
    @Override
    public void onEnable() {
        saveDefaultConfig();
        getLogger().info("GSM-verify enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("GSM-verify disabled.");
    }
}
