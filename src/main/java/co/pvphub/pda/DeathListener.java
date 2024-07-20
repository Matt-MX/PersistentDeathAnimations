package co.pvphub.pda;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

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
        return deaths.remove(entityId);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        // possible memory leak? shouldn't be a problem
        deaths.put(event.getPlayer().getEntityId(), new DeathCache(event.getPlayer(), System.currentTimeMillis()));
    }

    public record DeathCache(Player player, Long timeOfDeath) {
        public long getMillisSinceDeath() {
            return System.currentTimeMillis() - timeOfDeath;
        }
    }
}
