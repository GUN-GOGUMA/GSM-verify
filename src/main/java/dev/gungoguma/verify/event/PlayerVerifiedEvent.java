package dev.gungoguma.verify.event;

import dev.gungoguma.verify.model.VerifiedUser;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public final class PlayerVerifiedEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final VerifiedUser verifiedUser;

    public PlayerVerifiedEvent(Player player, VerifiedUser verifiedUser) {
        this.player = player;
        this.verifiedUser = verifiedUser;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    public Player player() {
        return player;
    }

    public VerifiedUser verifiedUser() {
        return verifiedUser;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }
}
