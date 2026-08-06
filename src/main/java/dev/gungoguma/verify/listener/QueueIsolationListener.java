package dev.gungoguma.verify.listener;

import dev.gungoguma.verify.config.VerifyConfig;
import java.util.function.Supplier;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupArrowEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class QueueIsolationListener implements Listener {
    private final JavaPlugin plugin;
    private final Supplier<VerifyConfig> configSupplier;

    public QueueIsolationListener(JavaPlugin plugin, Supplier<VerifyConfig> configSupplier) {
        this.plugin = plugin;
        this.configSupplier = configSupplier;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.joinMessage(null);
        isolatePlayer(event.getPlayer());
        plugin.getServer().getScheduler().runTask(plugin, () -> isolatePlayer(event.getPlayer()));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        event.quitMessage(null);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!hasPositionChanged(event)) {
            return;
        }

        event.setCancelled(true);
        event.getPlayer().teleport(queueLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.PLUGIN) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        String command = event.getMessage().split("\\s+", 2)[0].toLowerCase();
        if (command.equals("/verify")) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPickup(PlayerAttemptPickupItemEvent event) {
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onArrowPickup(PlayerPickupArrowEvent event) {
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player player) {
            event.setCancelled(true);
            player.setFoodLevel(20);
            player.setSaturation(20.0F);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onToggleFlight(PlayerToggleFlightEvent event) {
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onToggleSneak(PlayerToggleSneakEvent event) {
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onToggleSprint(PlayerToggleSprintEvent event) {
        event.setCancelled(true);
    }

    private void isolatePlayer(Player player) {
        player.teleport(queueLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);
        player.setGameMode(GameMode.ADVENTURE);
        player.setInvulnerable(true);
        player.setCollidable(false);
        player.setFoodLevel(20);
        player.setSaturation(20.0F);
        player.getInventory().clear();
        hideOtherPlayersFrom(player);
    }

    private void hideOtherPlayersFrom(Player player) {
        for (Player other : plugin.getServer().getOnlinePlayers()) {
            if (other.equals(player)) {
                continue;
            }

            player.hidePlayer(plugin, other);
            other.hidePlayer(plugin, player);
        }
    }

    private Location queueLocation() {
        VerifyConfig config = configSupplier.get();
        World world = plugin.getServer().getWorld(config.queueWorldName());
        if (world == null) {
            world = plugin.getServer().getWorlds().getFirst();
        }

        return new Location(
            world,
            config.queueX(),
            config.queueY(),
            config.queueZ(),
            config.queueYaw(),
            config.queuePitch()
        );
    }

    private boolean hasPositionChanged(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) {
            return false;
        }

        return from.getX() != to.getX()
            || from.getY() != to.getY()
            || from.getZ() != to.getZ();
    }
}
