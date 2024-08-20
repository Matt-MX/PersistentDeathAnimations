package co.pvphub.pda;

import org.bukkit.entity.Entity;

public class Compatibility {

    public static boolean isNPC(Entity player) {
        return player.hasMetadata("NPC");
    }

}
