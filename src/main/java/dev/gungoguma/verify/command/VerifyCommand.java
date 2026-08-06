package dev.gungoguma.verify.command;

import dev.gungoguma.verify.GsmVerify;
import dev.gungoguma.verify.config.VerifyConfig;
import dev.gungoguma.verify.model.VerifiedUser;
import dev.gungoguma.verify.oauth.DiscordOAuthUrlBuilder;
import dev.gungoguma.verify.oauth.PendingVerification;
import dev.gungoguma.verify.oauth.PendingVerificationStore;
import dev.gungoguma.verify.storage.VerificationStore;
import java.io.IOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class VerifyCommand implements CommandExecutor {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter
        .ofPattern("yyyy-MM-dd HH:mm:ss")
        .withLocale(Locale.KOREA)
        .withZone(ZoneId.systemDefault());

    private final GsmVerify plugin;
    private VerifyConfig config;
    private final VerificationStore verificationStore;
    private final PendingVerificationStore pendingStore;
    private DiscordOAuthUrlBuilder oauthUrlBuilder;

    public VerifyCommand(
        GsmVerify plugin,
        VerifyConfig config,
        VerificationStore verificationStore,
        PendingVerificationStore pendingStore,
        DiscordOAuthUrlBuilder oauthUrlBuilder
    ) {
        this.plugin = plugin;
        this.config = config;
        this.verificationStore = verificationStore;
        this.pendingStore = pendingStore;
        this.oauthUrlBuilder = oauthUrlBuilder;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            return startVerification(sender);
        }

        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> reload(sender);
            case "status" -> status(sender, args);
            case "reset" -> reset(sender, args);
            case "resetall" -> resetAll(sender, args);
            default -> {
                sender.sendMessage("Usage: /verify [reload|status|reset|resetall]");
                yield true;
            }
        };
    }

    private boolean startVerification(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        if (verificationStore.isVerified(player.getUniqueId())) {
            player.sendMessage(Component.text(config.messagePrefix() + " Already verified.", NamedTextColor.GREEN));
            return true;
        }

        PendingVerification pending = pendingStore.create(player.getUniqueId(), player.getName());
        String url = oauthUrlBuilder.build(pending.state());

        player.sendMessage(Component.text(config.messagePrefix() + " Please complete Discord verification.", NamedTextColor.YELLOW));
        player.sendMessage(
            Component.text("[Open verification link]", NamedTextColor.AQUA)
                .clickEvent(ClickEvent.openUrl(url))
                .hoverEvent(HoverEvent.showText(Component.text("Open the Discord verification page.")))
        );
        player.sendMessage(Component.text("This link expires in " + config.stateExpireSeconds() + " seconds.", NamedTextColor.GRAY));
        return true;
    }

    private boolean reload(CommandSender sender) {
        if (!sender.hasPermission("gsmverify.reload")) {
            sender.sendMessage("You do not have permission.");
            return true;
        }

        plugin.reloadVerifyConfig();
        config = plugin.verifyConfig();
        oauthUrlBuilder = new DiscordOAuthUrlBuilder(config);
        sender.sendMessage("GSM-Verify config reloaded.");
        return true;
    }

    private boolean status(CommandSender sender, String[] args) {
        if (!sender.hasPermission("gsmverify.status")) {
            sender.sendMessage("You do not have permission.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("Usage: /verify status <player>");
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        Optional<VerifiedUser> user = verificationStore.findByUuid(target.getUniqueId());
        if (user.isEmpty()) {
            sender.sendMessage(displayName(target) + " is not verified.");
            return true;
        }

        sender.sendMessage(formatStatus(target.getUniqueId(), user.get()));
        return true;
    }

    private boolean reset(CommandSender sender, String[] args) {
        if (!sender.hasPermission("gsmverify.reset")) {
            sender.sendMessage("You do not have permission.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("Usage: /verify reset <player>");
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        try {
            if (verificationStore.deleteByUuid(target.getUniqueId())) {
                sender.sendMessage("Reset verification for " + displayName(target) + ".");
            } else {
                sender.sendMessage(displayName(target) + " has no verification data.");
            }
        } catch (IOException exception) {
            sender.sendMessage("Failed to reset verification data.");
            plugin.getLogger().warning("Failed to reset verification for " + target.getUniqueId() + ": " + exception.getMessage());
        }
        return true;
    }

    private boolean resetAll(CommandSender sender, String[] args) {
        if (!sender.hasPermission("gsmverify.resetall")) {
            sender.sendMessage("You do not have permission.");
            return true;
        }

        if (args.length < 2 || !"confirm".equalsIgnoreCase(args[1])) {
            sender.sendMessage("This resets every verification record. Run /verify resetall confirm to continue.");
            return true;
        }

        try {
            verificationStore.deleteAll();
            sender.sendMessage("Reset all verification data.");
        } catch (IOException exception) {
            sender.sendMessage("Failed to reset all verification data.");
            plugin.getLogger().warning("Failed to reset all verification data: " + exception.getMessage());
        }
        return true;
    }

    private String formatStatus(UUID uuid, VerifiedUser user) {
        String identity = user.flag() != null ? user.flag() + "gi" : user.studentId();
        return "UUID: " + uuid
            + ", Discord ID: " + user.discordId()
            + ", name: " + user.name()
            + ", role: " + user.roleType()
            + ", identity: " + identity
            + ", verifiedAt: " + TIME_FORMATTER.format(user.verifiedAt());
    }

    private String displayName(OfflinePlayer player) {
        return player.getName() != null ? player.getName() : player.getUniqueId().toString();
    }
}
