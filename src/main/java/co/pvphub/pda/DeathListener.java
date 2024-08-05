package co.pvphub.pda;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class DeathListener implements Listener {
    private final Map<Integer, DeathCache> deaths = Collections.synchronizedMap(new HashMap<>());
    private final DeathAnimationsPlugin plugin;

    public DeathListener(DeathAnimationsPlugin plugin) {
        this.plugin = plugin;
    }

    public DeathCache getCachedDeath(int entityId) {
        return deaths.get(entityId);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        // possible memory leak? shouldn't be a problem
        int entityId = event.getPlayer().getEntityId();

        DeathCache cache = new DeathCache(event.getPlayer(), System.currentTimeMillis(), entityId);
        deaths.put(entityId, cache);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (deaths.get(entityId) == cache) {
                deaths.remove(entityId);
            }
        }, 25L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        deaths.remove(event.getPlayer().getEntityId());
    }

    public record DeathCache(Player player, long timeOfDeath, int entityId) {
        public long getMillisSinceDeath() {
            return System.currentTimeMillis() - timeOfDeath;
        }
    }
}
