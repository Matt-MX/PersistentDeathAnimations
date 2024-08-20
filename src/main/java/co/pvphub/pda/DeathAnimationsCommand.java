package co.pvphub.pda;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class DeathAnimationsCommand implements CommandExecutor {
    private final @NotNull DeathAnimationsPlugin plugin;

    public DeathAnimationsCommand(@NotNull DeathAnimationsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        sender.sendMessage("Reloading config.");
        plugin.reloadConfig();
        plugin.reload();
        sender.sendMessage("Done!");
        return false;
    }
}
