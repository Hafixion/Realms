package eu.hafixion.realms

import br.com.devsrsouza.kotlinbukkitapi.dsl.command.CommandDSL
import br.com.devsrsouza.kotlinbukkitapi.dsl.command.command
import br.com.devsrsouza.kotlinbukkitapi.dsl.command.fail
import br.com.devsrsouza.kotlinbukkitapi.extensions.server.offlinePlayer
import br.com.devsrsouza.kotlinbukkitapi.extensions.server.scheduler
import br.com.devsrsouza.kotlinbukkitapi.extensions.text.msg
import eu.hafixion.realms.towny.nations.Nation
import eu.hafixion.realms.towny.nations.NationDatabase
import eu.hafixion.realms.towny.towns.TownDatabase
import eu.hafixion.realms.utils.asPos
import eu.hafixion.realms.utils.errorMessage
import eu.hafixion.realms.utils.successMessage
import org.bukkit.plugin.Plugin
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit

object RealmsCommands {

    fun RealmsCorePlugin.realmsCommands() {
        repairCommand(this)

        command("realms") {
            command("debug") {
                debugCommands()
            }

            command("upkeep") { executor {
                val diff = RealmsCorePlugin.lastUpkeep + 86400000 - System.currentTimeMillis()

                val time = String.format("%02d:%02d:%02d",
                    TimeUnit.MILLISECONDS.toHours(diff),
                    TimeUnit.MILLISECONDS.toMinutes(diff) -
                            TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(diff)), // The change is in this line
                    TimeUnit.MILLISECONDS.toSeconds(diff) -
                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(diff)));

                sender.msg("""
                    §3§lNEXT UPKEEP IS DUE IN
                    §b$time
                """.trimIndent())


            }}

            command("prices") { executor {
                sender.msg("""
                    §3§lPRICES
                    §bTo set up a town, you need 64 cobblestone and 64 oak planks in your inventory. To pay upkeep, you need 2 gold ingots in your stockpile chest, and you need not pay upkeep for as long as your town has newbie protection.
                    §b- Claim Prices and Upkeep are as follows:
                    §3•§b Level 1: 1 gold ingots / 0 gold for upkeep
                    §3•§b Level 2: 4 gold ingots / 1 gold for upkeep
                    §3•§b Level 3: 6 gold ingots / 2 gold for upkeep
                    §3•§b Level 4: 8 gold ingots / 4 gold for upkeep

                    §bTo set up a nation, you need 2 stacks of gold ingots (128 Gold) in your stockpile chest. To pay upkeep, the nation capital needs 10 gold per day to keep the nation running.
                    §b- Claim Price is a measly 2 gold ingots, and nothing for upkeep.
                    
                    §eIf you have a town, remember to check /town upkeep to see how much upkeep is due
                """.trimIndent())
            }}
        }
        
        command("player") { executor { 
            val name = args.getOrNull(0) ?: fail(errorMessage("You need to specify a player."))
            val player = offlinePlayer(name).apply { if (!hasPlayedBefore()) 
                fail(errorMessage("That player hasn't joined before.")) }
            
            if (player.name == null) fail(errorMessage("They don't exist!"))

            val town = RealmsPlayerDatabase[player.uniqueId]?.town
            val townString = if (town != null) "§6Town:§e ${town.name}" else "§eThis player has no town."

            sender.msg("""
                     §8§m                   §r §6§l${player.name!!.toUpperCase()} §8§m                   §r
                     
                    $townString
                     
                    §eJoined on §6${SimpleDateFormat("MM/dd/yyyy").format(player.firstPlayed)}
                """.trimIndent()
            )
        }}

        commandPlot(this)
    }

    private fun commandPlot(pl: Plugin) {
        command("check-chunk", plugin=pl) { executorPlayer {
            val loc = sender.location

            when {
                NationDatabase[loc.chunk.asPos()] != null -> {
                    sender.msg("§3§lPLOT §bThis chunk is claimed by a nation and is reinforced by a factor of §3${Nation.claimProtection}")
                }
                TownDatabase.getTown(loc) != null -> {
                    sender.msg("§3§lPLOT §bThis chunk is claimed by a town and has a claim level of " +
                            "§3${TownDatabase.getTown(loc)!!.claims.entries.first { it.key.equalsPos(loc.chunk)}.value}")
                }
                else -> sender.msg(errorMessage("You aren't standing in a claimed chunk."))
            }
        }}
    }

    private fun repairCommand(pl: Plugin) {
        command("repair", plugin=pl) { executorPlayer {
            sender.toRealmsPlayer().repairMode = !sender.toRealmsPlayer().repairMode
            if (sender.toRealmsPlayer().repairMode) sender.msg(successMessage("Repair Mode is now enabled."))
            else sender.msg(successMessage("Repair Mode is now disabled."))
        }}
    }

    private fun CommandDSL.debugCommands() {
        command("takeUpkeep") { executor {
            if (!sender.isOp) {
                sender.msg(errorMessage("Nice try."))
                return@executor
            }

            RealmsCorePlugin.upkeepTask.cancel()
            RealmsCorePlugin.instance.upkeep()
            RealmsCorePlugin.upkeepTask =
                scheduler.runTaskTimer(
                    RealmsCorePlugin.instance,
                    Runnable { RealmsCorePlugin.instance.upkeep() },
                    1728000,
                    1728000
                )
        }}

        command("toggleUpkeep") { executor {
            if (!sender.isOp) {
                sender.msg(errorMessage("Nice try."))
                return@executor
            }

            RealmsCorePlugin.upkeepEnabled = !RealmsCorePlugin.upkeepEnabled
            if (RealmsCorePlugin.upkeepEnabled) sender.msg("enabled sir")
            else sender.msg("disabled sir")
        }}

        command("saveData") { executor {
            if (!sender.isOp) {
                sender.msg(errorMessage("Nice try."))
                return@executor
            }

            RealmsCorePlugin.instance.serializeData()
            sender.msg("Saved Data.")
        }}

        command("loadData") { executor {

            if (!sender.isOp) {
                sender.msg(errorMessage("Nice try."))
                return@executor
            }

            RealmsCorePlugin.instance.reloadDatabase()
            sender.msg("done sir")
        }}

        command("createPlayer") { executor {
            if (!sender.isOp) {
                sender.msg(errorMessage("Nice try."))
                return@executor
            }

            val name = args.getOrNull(0) ?: fail("console pls name")
            val uniqueId = offlinePlayer(name).uniqueId
            RealmsPlayerDatabase[uniqueId] ?: RealmsPlayer(uniqueId)
            fail("done sir")
        }}
    }
}