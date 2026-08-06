package dev.gungoguma.verify.listener;

import dev.gungoguma.verify.bungee.BungeeConnector;
import dev.gungoguma.verify.config.VerifyConfig;
import dev.gungoguma.verify.oauth.PendingVerificationStore;
import dev.gungoguma.verify.storage.VerificationStore;
import java.util.function.Supplier;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class PlayerJoinListener implements Listener {
    private final JavaPlugin plugin;
    private final Supplier<VerifyConfig> configSupplier;
    private final VerificationStore verificationStore;
    private final PendingVerificationStore pendingStore;
    private final BungeeConnector bungeeConnector;

    public PlayerJoinListener(
        JavaPlugin plugin,
        Supplier<VerifyConfig> configSupplier,
        VerificationStore verificationStore,
        PendingVerificationStore pendingStore,
        BungeeConnector bungeeConnector
    ) {
        this.plugin = plugin;
        this.configSupplier = configSupplier;
        this.verificationStore = verificationStore;
        this.pendingStore = pendingStore;
        this.bungeeConnector = bungeeConnector;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> handleJoin(event), 20L);
    }

    private void handleJoin(PlayerJoinEvent event) {
        if (!event.getPlayer().isOnline()) {
            return;
        }

        if (verificationStore.isVerified(event.getPlayer().getUniqueId())) {
            VerifyConfig config = configSupplier.get();
            event.getPlayer().sendMessage(
                Component.text(config.messagePrefix() + " Verification found. Moving to SMP.", NamedTextColor.GREEN)
            );
            bungeeConnector.connectToSmp(event.getPlayer());
            return;
        }

        if (pendingStore.findByUuid(event.getPlayer().getUniqueId()).isPresent()) {
            VerifyConfig config = configSupplier.get();
            event.getPlayer().sendMessage(
                Component.text(config.messagePrefix() + " Discord verification is in progress. Complete the browser page.", NamedTextColor.YELLOW)
            );
            return;
        }

        VerifyConfig config = configSupplier.get();
        event.getPlayer().sendMessage(
            Component.text(config.messagePrefix() + " Run /verify in Queue to start Discord verification.", NamedTextColor.YELLOW)
        );
    }
}
