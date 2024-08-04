package co.pvphub.pda;

import com.github.retrooper.packetevents.PacketEvents;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class DeathAnimationsPlugin extends JavaPlugin {
    private DeathListener deathListener;

    @Override
    public void onEnable() {
        deathListener = new DeathListener(this);

        Bukkit.getPluginManager().registerEvents(deathListener, this);
        PacketEvents.getAPI().getEventManager().registerListener(new OutgoingPlayerDeathListener(this));
    }

    public DeathListener getDeathListener() {
        return deathListener;
    }

}