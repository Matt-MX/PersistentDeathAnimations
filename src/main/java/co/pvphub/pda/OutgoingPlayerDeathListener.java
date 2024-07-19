package co.pvphub.pda;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class OutgoingPlayerDeathListener extends PacketAdapter {
    private static final byte ENTITY_DEATH_EVENT_ID = 3;
    private final ProtocolManager manager;
    private final DeathAnimationsPlugin deathAnimations;
    private final double cancelDistanceSquared;

    public OutgoingPlayerDeathListener(DeathAnimationsPlugin deathAnimationsPlugin, ProtocolManager manager) {
        super(deathAnimationsPlugin, PacketType.Play.Server.ENTITY_DESTROY);
        this.manager = manager;
        this.deathAnimations = deathAnimationsPlugin;
        this.cancelDistanceSquared = Bukkit.getSimulationDistance() * 16.0;
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        Player playerSendingTo = event.getPlayer();
        PacketContainer packet = event.getPacket();

        IntArrayList entityIds = (IntArrayList) packet.getModifier().read(0);

        List<Integer> remainingToSend = new ArrayList<>(Arrays.stream(entityIds.toArray(new int[]{})).boxed().toList());

        for (int entityId : entityIds) {
            DeathListener.DeathCache cached = deathAnimations.getDeathListener().getCachedDeath(entityId);

            if (cached == null) continue;

            long millis = cached.getMillisSinceDeath();
            if (millis > 1000) continue;

            remainingToSend.removeIf((i) -> i == entityId);
            event.setCancelled(true);

            // Send a death packet instead
            PacketContainer healthPacket = manager.createPacket(PacketType.Play.Server.ENTITY_METADATA);
            healthPacket.getIntegers().write(0, entityId);

            WrappedDataWatcher watcher = new WrappedDataWatcher();
            watcher.setEntity(cached.player());
            watcher.setObject(9, WrappedDataWatcher.Registry.get(Float.class), 0f);

            healthPacket.getWatchableCollectionModifier().write(0, watcher.getWatchableObjects());

            PacketContainer deathPacket = manager.createPacket(PacketType.Play.Server.ENTITY_STATUS);
            deathPacket.getIntegers().write(0, entityId);
            deathPacket.getBytes().write(0, ENTITY_DEATH_EVENT_ID);

            Bukkit.getAsyncScheduler().runDelayed(getPlugin(), (task) -> {
                manager.sendServerPacket(playerSendingTo, healthPacket);
                manager.sendServerPacket(playerSendingTo, deathPacket);
            }, 100L, TimeUnit.MILLISECONDS);

            Bukkit.getAsyncScheduler().runDelayed(getPlugin(), (task) -> {
                if (playerSendingTo.getWorld() == cached.player().getWorld() &&
                    playerSendingTo.getLocation().distanceSquared(cached.player().getLocation()) < cancelDistanceSquared)
                    return;

                PacketContainer removePacket = manager.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
                removePacket.getModifier().write(0, new IntArrayList(new int[] { entityId }));

                manager.sendServerPacket(playerSendingTo, removePacket, false);
            }, 1L, TimeUnit.SECONDS);
        }

        // Nothing was changed, we should make sure it is not cancelled
        if (remainingToSend.size() == entityIds.size()) {
            event.setCancelled(false);
            return;
        }

        PacketContainer remaining = manager.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
        remaining.getModifier().write(0, new IntArrayList(remainingToSend));

        manager.sendServerPacket(playerSendingTo, remaining, false);
    }
}
