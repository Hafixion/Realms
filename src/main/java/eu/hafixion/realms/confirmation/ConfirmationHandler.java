package eu.hafixion.realms.confirmation;

import eu.hafixion.realms.RealmsCorePlugin;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static eu.hafixion.realms.utils.MessageUtilsKt.errorMessage;

/**
 * A class that handles the processing confirmations sent in Towny.
 *
 * @author Lukas Mansour (ArticDive)
 * @author Suneet Tipirneni (Siris)
 */
public class ConfirmationHandler {

    private final static Map<CommandSender, ConfirmationContext> confirmations = new ConcurrentHashMap<>();

    private static final class ConfirmationContext {
        final Confirmation confirmation;
        final int taskID;

        ConfirmationContext(Confirmation confirmation, int taskID) {
            this.confirmation = confirmation;
            this.taskID = taskID;
        }
    }

    /**
     * Revokes the confirmation associated with the given sender.
     *
     * @param sender The sender to get the confirmation from.
     */
    public static void revokeConfirmation(CommandSender sender) {
        ConfirmationContext context = confirmations.get(sender);

        Bukkit.getScheduler().cancelTask(context.taskID);
        Confirmation confirmation = context.confirmation;
        confirmations.remove(sender);

        // Run the cancel handler.
        if (confirmation.getCancelHandler() != null) {
            confirmation.getCancelHandler().run();
        }

        sender.sendMessage("§4§lCANCELLED§c Successfully cancelled confirmation.");
    }

    public static void cancelConfirmation(CommandSender sender) {
        ConfirmationContext context = confirmations.get(sender);
        Bukkit.getScheduler().cancelTask(context.taskID);
        confirmations.remove(sender);

        sender.sendMessage("§4§lCANCELLED§c Your previous confirmation has been invalidated.");
    }

    /**
     * Registers and begins the timeout timer for the confirmation.
     *
     * @param sender The sender to receive the confirmation.
     * @param confirmation The confirmation to add.
     */
    public static void sendConfirmation(CommandSender sender, Confirmation confirmation) {

        // Check if confirmation is already active and perform appropriate actions.
        if (confirmations.containsKey(sender)) {
            // Cancel prior Confirmation actions.
            revokeConfirmation(sender);
        }

        // Send the confirmation message.
        String title = confirmation.getTitle();
        sendConfirmationMessage(sender, title);

        int duration = confirmation.getDuration();

        Runnable handler = () -> {
            // Show cancel messages only if the confirmation exists.
            if (hasConfirmation(sender)) {
                confirmations.remove(sender);
                sender.sendMessage(errorMessage("Confirmation Timed out."));
            }
        };

        int taskID;
        long ticks = 20L * duration;
        taskID = Bukkit.getScheduler().runTaskLater(RealmsCorePlugin.instance, handler, ticks).getTaskId();

        // Cache the task.
        confirmations.put(sender, new ConfirmationContext(confirmation, taskID));
    }

    private static void sendConfirmationMessage(CommandSender sender, String title) {
        sender.sendMessage(title);
        sender.sendMessage(ChatColor.of("#FFD27F") + "Type /accept or /deny to handle this confirmation.");
    }

    /**
     * Internal use only.
     *
     * @param sender The sender using the confirmation.
     */
    public static void acceptConfirmation(CommandSender sender) {
        // Get confirmation
        ConfirmationContext context = confirmations.get(sender);

        // Get handler
        Runnable handler = context.confirmation.getAcceptHandler();

        // Cancel task.
        Bukkit.getScheduler().cancelTask(context.taskID);

        // Remove confirmation as it's been handled.
        confirmations.remove(sender);

        // Execute handler.
        if (context.confirmation.isAsync()) {
            Bukkit.getScheduler().runTaskAsynchronously(RealmsCorePlugin.instance, handler);
        } else {
            Bukkit.getScheduler().runTask(RealmsCorePlugin.instance, handler);
        }
    }

    public static boolean hasConfirmation(CommandSender sender) {
        return confirmations.containsKey(sender);
    }
}