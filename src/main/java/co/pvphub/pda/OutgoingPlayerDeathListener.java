package co.pvphub.pda;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityStatus;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class OutgoingPlayerDeathListener extends PacketListenerAbstract {
    private static final byte ENTITY_DEATH_EVENT_ID = 3;
    private final DeathAnimationsPlugin deathAnimations;
    private final double cancelDistanceSquared;

    public OutgoingPlayerDeathListener(DeathAnimationsPlugin deathAnimationsPlugin) {
        this.deathAnimations = deathAnimationsPlugin;
        // https://media1.tenor.com/m/skdxV4R475EAAAAC/magic-mr-bean.gif
        this.cancelDistanceSquared = Math.pow(Bukkit.getSimulationDistance() * 16.0, 2.0);
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() != PacketType.Play.Server.DESTROY_ENTITIES) return;

        Player playerSendingTo = (Player) event.getPlayer();

        WrapperPlayServerDestroyEntities outgoingPacket = new WrapperPlayServerDestroyEntities(event);

        List<Integer> remainingToSend = new ArrayList<>(Arrays.stream(outgoingPacket.getEntityIds())
            .boxed()
            .toList());

        ArrayList<DeathListener.DeathCache> delayedSend = new ArrayList<>();

        for (int entityId : outgoingPacket.getEntityIds()) {
            DeathListener.DeathCache cached = deathAnimations.getDeathListener().getCachedDeath(entityId);

            if (cached == null) continue;

            long millis = cached.getMillisSinceDeath();
            if (millis > 1000) continue;

            // Simulate the player dying for the user
            // We first need to send a health packet because Mojank:tm:
            WrapperPlayServerEntityMetadata healthPacket = new WrapperPlayServerEntityMetadata(
                entityId, List.of(new EntityData(9, EntityDataTypes.FLOAT, 0.0f))
            );

            WrapperPlayServerEntityStatus deathPacket = new WrapperPlayServerEntityStatus(entityId, ENTITY_DEATH_EVENT_ID);

            PacketEvents.getAPI().getPlayerManager().sendPacket(playerSendingTo, healthPacket);
            PacketEvents.getAPI().getPlayerManager().sendPacket(playerSendingTo, deathPacket);

            delayedSend.add(cached);
            remainingToSend.removeAll(List.of(entityId));
        }

        // Nothing changed so there's no need to modify the packet
        if (delayedSend.isEmpty()) {
            event.setCancelled(false);
            return;
        } else {
            event.setCancelled(true);
        }

        // Sends the remove packet 1s later for any valid entities
        Bukkit.getAsyncScheduler().runDelayed(deathAnimations, (task) -> {
            int[] entityIds = delayedSend.stream()
                .filter(cache -> !shouldCancelRemovePacket(playerSendingTo, cache.player()))
                .mapToInt(DeathListener.DeathCache::entityId)
                .toArray();

            WrapperPlayServerDestroyEntities delayedRemovePacket = new WrapperPlayServerDestroyEntities(entityIds);
            PacketEvents.getAPI().getPlayerManager().sendPacket(playerSendingTo, delayedRemovePacket);
        }, 1L, TimeUnit.SECONDS);

        // Still send destroy packets for entities that we didn't affect.
        if (!remainingToSend.isEmpty()) {
            int[] entityIds = remainingToSend.stream()
                .mapToInt(i -> i)
                .toArray();

            WrapperPlayServerDestroyEntities notModifiedRemovePacket = new WrapperPlayServerDestroyEntities(entityIds);
            PacketEvents.getAPI().getPlayerManager().sendPacket(playerSendingTo, notModifiedRemovePacket);
        }
    }

    private boolean shouldCancelRemovePacket(Player sending, Player dead) {
        boolean world = sending.getWorld() == dead.getWorld();

        // If they're not in the same world there's no way to see one another
        if (!world) {
            return false;
        }

        boolean distance = sending.getLocation().distanceSquared(dead.getLocation()) <= cancelDistanceSquared;
        boolean canSee = sending.canSee(dead) && dead.getGameMode() != GameMode.SPECTATOR;

        return distance && canSee;
    }
}
