package co.pvphub.pda;

import co.pvphub.pda.listener.DeathListener;
import co.pvphub.pda.listener.OutgoingPlayerDeathListener;
import com.github.retrooper.packetevents.PacketEvents;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class DeathAnimationsPlugin extends JavaPlugin {
    private DeathListener deathListener;
    private OutgoingPlayerDeathListener outgoingPacketListener;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reload();

        Objects.requireNonNull(Bukkit.getPluginCommand("death-animations-reload"))
            .setExecutor(new DeathAnimationsCommand(this));
    }

    public void reload() {
        if (deathListener != null) {
            HandlerList.unregisterAll(deathListener);
            getLogger().info("Unregistered DeathListener");
        }

        if (outgoingPacketListener != null) {
            PacketEvents.getAPI()
                .getEventManager()
                .unregisterListeners(outgoingPacketListener);
            getLogger().info("Unregistered OutgoingPlayerDeathListener");
        }

        deathListener = new DeathListener(this);
        outgoingPacketListener = new OutgoingPlayerDeathListener(this);

        Bukkit.getPluginManager().registerEvents(deathListener, this);
        PacketEvents.getAPI().getEventManager().registerListener(outgoingPacketListener);
    }

    public @NotNull DeathListener getDeathListener() {
        return Objects.requireNonNull(deathListener, "Plugin is not initialized yet.");
    }

    public @NotNull OutgoingPlayerDeathListener getOutgoingPacketListener() {
        return Objects.requireNonNull(outgoingPacketListener, "Plugin is not initialized yet.");
    }

    public boolean isVerbose() {
        return this.getConfig().getBoolean("verbose");
    }

    public long getEntityRemoveDelay() {
        return this.getConfig().getLong("remove-delay", 750L);
    }

    public double getMaximumFakeDeathPlayDistance() {
        String stringValue = this.getConfig().getString("play-distance-max", "default");

        if (stringValue.equalsIgnoreCase("default")) {
            return Math.pow(Bukkit.getSimulationDistance() * 16.0, 2.0);
        }

        return DoubleUtil.parseDouble(stringValue)
            .orElseThrow(() -> new NumberFormatException("Config option 'play-distance-max' is an invalid double. Please refer to config.yml comments."));
    }

    public long getDeathCacheTimeout() {
        return this.getConfig().getLong("debug.death-cache-timeout", 25L);
    }

    public long getDeathCacheIgnore() {
        return this.getConfig().getLong("debug.death-cache-ignore", 1000L);
    }

}