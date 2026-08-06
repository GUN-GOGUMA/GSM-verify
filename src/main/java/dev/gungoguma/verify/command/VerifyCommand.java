package dev.gungoguma.verify.command;

import dev.gungoguma.verify.config.VerifyConfig;
import dev.gungoguma.verify.oauth.DiscordOAuthUrlBuilder;
import dev.gungoguma.verify.oauth.PendingVerification;
import dev.gungoguma.verify.oauth.PendingVerificationStore;
import dev.gungoguma.verify.storage.VerificationStore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class VerifyCommand implements CommandExecutor {
    private final VerifyConfig config;
    private final VerificationStore verificationStore;
    private final PendingVerificationStore pendingStore;
    private final DiscordOAuthUrlBuilder oauthUrlBuilder;

    public VerifyCommand(
        VerifyConfig config,
        VerificationStore verificationStore,
        PendingVerificationStore pendingStore,
        DiscordOAuthUrlBuilder oauthUrlBuilder
    ) {
        this.config = config;
        this.verificationStore = verificationStore;
        this.pendingStore = pendingStore;
        this.oauthUrlBuilder = oauthUrlBuilder;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        if (verificationStore.isVerified(player.getUniqueId())) {
            player.sendMessage(Component.text(config.messagePrefix() + " 이미 인증되어 있습니다.", NamedTextColor.GREEN));
            return true;
        }

        PendingVerification pending = pendingStore.create(player.getUniqueId(), player.getName());
        String url = oauthUrlBuilder.build(pending.state());

        player.sendMessage(Component.text(config.messagePrefix() + " Discord 인증을 진행해 주세요.", NamedTextColor.YELLOW));
        player.sendMessage(
            Component.text("[인증 링크 열기]", NamedTextColor.AQUA)
                .clickEvent(ClickEvent.openUrl(url))
                .hoverEvent(HoverEvent.showText(Component.text("클릭하여 Discord 인증 페이지를 엽니다.")))
        );
        player.sendMessage(Component.text("인증 링크는 " + config.stateExpireSeconds() + "초 동안 유효합니다.", NamedTextColor.GRAY));
        return true;
    }
}
