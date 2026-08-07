package dev.gungoguma.verify.bungee;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import dev.gungoguma.verify.config.VerifyConfig;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class BungeeConnector {
    public static final String CHANNEL = "BungeeCord";
    private static final String FORWARD_SUBCHANNEL = "Forward";

    private final JavaPlugin plugin;
    private VerifyConfig config;

    public BungeeConnector(JavaPlugin plugin, VerifyConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void registerChannel() {
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, CHANNEL);
    }

    public void unregisterChannel() {
        plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin, CHANNEL);
    }

    public void updateConfig(VerifyConfig config) {
        this.config = config;
    }

    public void connectToSmp(Player player) {
        connect(player, config.smpServerName());
    }

    public void connect(Player player, String serverName) {
        ByteArrayDataOutput output = ByteStreams.newDataOutput();
        output.writeUTF("Connect");
        output.writeUTF(serverName);
        player.sendPluginMessage(plugin, CHANNEL, output.toByteArray());
    }

    public void forwardToSmp(Player player, String channel, byte[] payload) {
        ByteArrayDataOutput output = ByteStreams.newDataOutput();
        output.writeUTF(FORWARD_SUBCHANNEL);
        output.writeUTF(config.smpServerName());
        output.writeUTF(channel);
        output.writeShort(payload.length);
        output.write(payload);
        player.sendPluginMessage(plugin, CHANNEL, output.toByteArray());
    }
}
