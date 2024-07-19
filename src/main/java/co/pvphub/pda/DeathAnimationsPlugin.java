package co.pvphub.pda;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public class DeathAnimationsPlugin extends JavaPlugin {

    private ProtocolManager manager;
    private DeathListener deathListener;

    @Override
    public void onEnable() {
        manager = ProtocolLibrary.getProtocolManager();
        deathListener = new DeathListener(this);

        Bukkit.getPluginManager().registerEvents(deathListener, this);
        manager.addPacketListener(new OutgoingPlayerDeathListener(this, manager));
    }

    public ProtocolManager getProtocolManager() {
        return manager;
    }

    public DeathListener getDeathListener() {
        return deathListener;
    }

}