package dev.gungoguma.verify.listener;

import dev.gungoguma.verify.bungee.BungeeConnector;
import dev.gungoguma.verify.verification.VerificationLookup;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class PlayerJoinListener implements Listener {
    private final JavaPlugin plugin;
    private final VerificationLookup verificationLookup;
    private final BungeeConnector bungeeConnector;

    public PlayerJoinListener(
        JavaPlugin plugin,
        VerificationLookup verificationLookup,
        BungeeConnector bungeeConnector
    ) {
        this.plugin = plugin;
        this.verificationLookup = verificationLookup;
        this.bungeeConnector = bungeeConnector;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!verificationLookup.isVerified(event.getPlayer().getUniqueId())) {
            return;
        }

        plugin.getServer().getScheduler().runTaskLater(
            plugin,
            () -> bungeeConnector.connectToSmp(event.getPlayer()),
            20L
        );
    }
}
