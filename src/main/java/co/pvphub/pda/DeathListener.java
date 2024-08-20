package co.pvphub.pda;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DeathListener implements Listener {
    private final @NotNull Map<Integer, DeathCache> deaths = new ConcurrentHashMap<>();
    private final @NotNull DeathAnimationsPlugin plugin;

    public DeathListener(@NotNull DeathAnimationsPlugin plugin) {
        this.plugin = plugin;
    }

    public @Nullable DeathCache getCachedDeath(int entityId) {
        return deaths.get(entityId);
    }

    @EventHandler
    public void onDeath(@NotNull PlayerDeathEvent event) {
        if (Compatibility.isNPC(event.getEntity())) {
            return;
        }

        int entityId = event.getPlayer().getEntityId();

        DeathCache cache = new DeathCache(event.getPlayer(), System.currentTimeMillis(), entityId);
        deaths.put(entityId, cache);

        // Forcefully remove as a timeout to prevent memory leak
        Bukkit.getScheduler()
            .runTaskLater(plugin, () -> {
                if (deaths.get(entityId) == cache) {
                    deaths.remove(entityId);
                }
            }, plugin.getDeathCacheTimeout());
    }

    @EventHandler
    public void onQuit(@NotNull PlayerQuitEvent event) {
        deaths.remove(event.getPlayer().getEntityId());
    }

    @EventHandler
    public void onJoin(@NotNull PlayerJoinEvent event) {
        // We should make sure that this is removed from any currently processing packets
        boolean removed = plugin.getOutgoingPacketListener().cancelAwaitingRemoval(event.getPlayer().getUniqueId());

        if (plugin.isVerbose() && removed) {
            plugin.getLogger().info(String.format("Cancelled delay removal packet of %s due to login", event.getPlayer().getName()));
        }
    }

    public record DeathCache(@NotNull Player player, long timeOfDeath, int entityId) {

        public long getMillisSinceDeath() {
            return System.currentTimeMillis() - timeOfDeath;
        }
    }
}
