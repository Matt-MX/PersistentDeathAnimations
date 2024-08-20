# 🔥 Persistent Death Animations

If your server has the need to immediately respawn your players using a
plugin or the `doImmediateRespawn` game rule, you've likely noticed there
is no death animation!

This plugin adds the death animation even with immediate respawns!

### How it works

The plugin simply catches the client-bound `EntityDestroy` before it is sent and
checks if any of the entities are players who have recently died.

If there are any recently deceased players the client will be sent `Health` and
`EntityStatus` packets to simulate the player's death.

After a second the client is sent an `EntityDestroy` packet **if** the player
hasn't somehow returned to their render distance in order to avoid invisible
players. The plugin will check `simulationDistance`, `gameMode`, `world` and
if they can actually see each other (`Player#canSee(Player other)`).

### Contributions

While not needed since the plugin won't really need any attention unless bugs
are discovered, contributions are more than welcome.

### Hooking in

Add the jar as a local dependency to your project.

Get the api instance using Bukkit's plugin manager by name.

To prevent a death animation from playing, hook into the Bukkit `PlayerDeathEvent` with an event priority of more than `NORMAL`.

You can then remove the cached death with `plugin.getDeathListener().removeCachedDeath(int entityId)` or peek it with `DeathListener#getCachedDeath(int entityId)`

Here's an example:
```java

@EventHandler(priority = EventPriority.HIGH)
public void onDeath(@NotNull PlayerDeathEvent event) {
    DeathAnimations deathAnimations = (DeathAnimations) Bukkit.getPluginManager().getPlugin("PersistentDeathAnimations");

    // Insert some arbirtary check here as a condition to cancel the fake death.

    deathAnimations.getDeathListener().removeCachedDeath(event.getPlayer().getEntityId());
}

```
