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
            case "resetall" -> resetAll(sender);
            default -> {
                sender.sendMessage("사용법: /verify [reload|status|reset|resetall]");
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

    private boolean reload(CommandSender sender) {
        if (!sender.hasPermission("gsmverify.reload")) {
            sender.sendMessage("권한이 없습니다.");
            return true;
        }

        plugin.reloadVerifyConfig();
        config = plugin.verifyConfig();
        oauthUrlBuilder = new DiscordOAuthUrlBuilder(config);
        sender.sendMessage("GSM-Verify 설정을 다시 불러왔습니다.");
        return true;
    }

    private boolean status(CommandSender sender, String[] args) {
        if (!sender.hasPermission("gsmverify.status")) {
            sender.sendMessage("권한이 없습니다.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("사용법: /verify status <player>");
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        Optional<VerifiedUser> user = verificationStore.findByUuid(target.getUniqueId());
        if (user.isEmpty()) {
            sender.sendMessage(target.getName() + "님은 인증되어 있지 않습니다.");
            return true;
        }

        sender.sendMessage(formatStatus(target.getUniqueId(), user.get()));
        return true;
    }

    private boolean reset(CommandSender sender, String[] args) {
        if (!sender.hasPermission("gsmverify.reset")) {
            sender.sendMessage("권한이 없습니다.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("사용법: /verify reset <player>");
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        try {
            if (verificationStore.deleteByUuid(target.getUniqueId())) {
                sender.sendMessage(target.getName() + "님의 인증 정보를 초기화했습니다.");
            } else {
                sender.sendMessage(target.getName() + "님의 인증 정보가 없습니다.");
            }
        } catch (IOException exception) {
            sender.sendMessage("인증 정보 초기화 중 오류가 발생했습니다.");
            plugin.getLogger().warning("Failed to reset verification for " + target.getUniqueId() + ": " + exception.getMessage());
        }
        return true;
    }

    private boolean resetAll(CommandSender sender) {
        if (!sender.hasPermission("gsmverify.resetall")) {
            sender.sendMessage("권한이 없습니다.");
            return true;
        }

        try {
            verificationStore.deleteAll();
            sender.sendMessage("모든 인증 정보를 초기화했습니다.");
        } catch (IOException exception) {
            sender.sendMessage("전체 인증 정보 초기화 중 오류가 발생했습니다.");
            plugin.getLogger().warning("Failed to reset all verification data: " + exception.getMessage());
        }
        return true;
    }

    private String formatStatus(UUID uuid, VerifiedUser user) {
        String identity = user.flag() != null ? user.flag() + "기" : user.studentId();
        return "UUID: " + uuid
            + ", Discord ID: " + user.discordId()
            + ", 이름: " + user.name()
            + ", 구분: " + user.roleType()
            + ", 식별: " + identity
            + ", 인증일: " + TIME_FORMATTER.format(user.verifiedAt());
    }
}
