package dev.gungoguma.verify.listener;

import dev.gungoguma.verify.bungee.BungeeConnector;
import dev.gungoguma.verify.config.VerifyConfig;
import dev.gungoguma.verify.oauth.PendingVerificationStore;
import dev.gungoguma.verify.storage.VerificationStore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class PlayerJoinListener implements Listener {
    private final JavaPlugin plugin;
    private final VerifyConfig config;
    private final VerificationStore verificationStore;
    private final PendingVerificationStore pendingStore;
    private final BungeeConnector bungeeConnector;

    public PlayerJoinListener(
        JavaPlugin plugin,
        VerifyConfig config,
        VerificationStore verificationStore,
        PendingVerificationStore pendingStore,
        BungeeConnector bungeeConnector
    ) {
        this.plugin = plugin;
        this.config = config;
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
            event.getPlayer().sendMessage(
                Component.text(config.messagePrefix() + " 인증 정보가 확인되어 SMP로 이동합니다.", NamedTextColor.GREEN)
            );
            bungeeConnector.connectToSmp(event.getPlayer());
            return;
        }

        if (pendingStore.findByUuid(event.getPlayer().getUniqueId()).isPresent()) {
            event.getPlayer().sendMessage(
                Component.text(config.messagePrefix() + " Discord 인증이 진행 중입니다. 인증 페이지를 완료해 주세요.", NamedTextColor.YELLOW)
            );
            return;
        }

        event.getPlayer().sendMessage(
            Component.text(config.messagePrefix() + " Queue 서버에서 /verify로 Discord 인증을 시작해 주세요.", NamedTextColor.YELLOW)
        );
    }
}
