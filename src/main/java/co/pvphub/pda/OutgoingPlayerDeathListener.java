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
        this.cancelDistanceSquared = Bukkit.getSimulationDistance() * 16.0;
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() != PacketType.Play.Server.DESTROY_ENTITIES) return;

        Player playerSendingTo = (Player) event.getPlayer();
        WrapperPlayServerDestroyEntities packet = new WrapperPlayServerDestroyEntities(event);

        List<Integer> remainingToSend = new ArrayList<>(Arrays.stream(packet.getEntityIds())
            .boxed()
            .toList());

        for (int entityId : packet.getEntityIds()) {
            DeathListener.DeathCache cached = deathAnimations.getDeathListener().getCachedDeath(entityId);

            if (cached == null) continue;

            long millis = cached.getMillisSinceDeath();
            if (millis > 1000) continue;

            // To get around removeAt(int index) being invoked
            remainingToSend.removeIf((i) -> i == entityId);
            event.setCancelled(true);

            // Send a death packet instead
            WrapperPlayServerEntityMetadata healthPacket = new WrapperPlayServerEntityMetadata(
                entityId, List.of(new EntityData(9, EntityDataTypes.FLOAT, 0.0f))
            );

            WrapperPlayServerEntityStatus deathPacket = new WrapperPlayServerEntityStatus(entityId, ENTITY_DEATH_EVENT_ID);

            PacketEvents.getAPI().getPlayerManager().sendPacket(playerSendingTo, healthPacket);
            PacketEvents.getAPI().getPlayerManager().sendPacket(playerSendingTo, deathPacket);

            Bukkit.getAsyncScheduler().runDelayed(deathAnimations, (task) -> {
                if (shouldCancelRemovePacket(playerSendingTo, cached.player()))
                    return;

                WrapperPlayServerDestroyEntities removePacket = new WrapperPlayServerDestroyEntities(entityId);
                PacketEvents.getAPI().getPlayerManager().sendPacket(playerSendingTo, removePacket);
            }, 1L, TimeUnit.SECONDS);
        }

        WrapperPlayServerDestroyEntities remainingPacket = new WrapperPlayServerDestroyEntities(remainingToSend.stream().mapToInt(i -> i).toArray());
        PacketEvents.getAPI().getPlayerManager().sendPacket(playerSendingTo, remainingPacket);
    }

    private boolean shouldCancelRemovePacket(Player sending, Player dead) {
        boolean world = sending.getWorld() == dead.getWorld();
        boolean distance = sending.getLocation().distanceSquared(dead.getLocation()) < cancelDistanceSquared;
        boolean canSee = sending.canSee(dead) && dead.getGameMode() != GameMode.SPECTATOR;

        return world && distance && canSee;
    }
}
