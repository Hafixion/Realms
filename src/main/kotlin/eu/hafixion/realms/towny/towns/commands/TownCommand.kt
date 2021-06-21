package eu.hafixion.realms.towny.towns.commands

import br.com.devsrsouza.kotlinbukkitapi.dsl.command.CommandDSL
import br.com.devsrsouza.kotlinbukkitapi.dsl.command.command
import br.com.devsrsouza.kotlinbukkitapi.dsl.command.fail
import br.com.devsrsouza.kotlinbukkitapi.extensions.event.callEvent
import br.com.devsrsouza.kotlinbukkitapi.extensions.server.onlinePlayers
import br.com.devsrsouza.kotlinbukkitapi.extensions.text.msg
import br.com.devsrsouza.kotlinbukkitapi.utils.toBooleanOrNull
import eu.hafixion.realms.RealmsCorePlugin
import eu.hafixion.realms.RealmsPlayerDatabase
import eu.hafixion.realms.confirmation.ConfirmationBuilder
import eu.hafixion.realms.toRealmsPlayer
import eu.hafixion.realms.towny.towns.Town
import eu.hafixion.realms.towny.towns.TownDatabase
import eu.hafixion.realms.towny.towns.TownDatabase.getTown
import eu.hafixion.realms.towny.towns.commands.TownBroadcastMessages.newbie_turned_off
import eu.hafixion.realms.towny.towns.commands.TownBroadcastMessages.townChangedHomeblock
import eu.hafixion.realms.towny.towns.commands.TownBroadcastMessages.townChangedStockpile
import eu.hafixion.realms.towny.towns.commands.TownBroadcastMessages.townClaimSuccessClaim
import eu.hafixion.realms.towny.towns.commands.TownBroadcastMessages.townClaimSuccessUpgrade
import eu.hafixion.realms.towny.towns.commands.TownBroadcastMessages.town_delete
import eu.hafixion.realms.towny.towns.commands.TownBroadcastMessages.town_goodbye_player
import eu.hafixion.realms.towny.towns.commands.TownBroadcastMessages.town_kick_player
import eu.hafixion.realms.towny.towns.commands.TownBroadcastMessages.town_new_mayor
import eu.hafixion.realms.towny.towns.commands.TownBroadcastMessages.town_new_name
import eu.hafixion.realms.towny.towns.commands.TownBroadcastMessages.town_welcome_player
import eu.hafixion.realms.towny.towns.commands.TownCommandMessages.townRankingsTemplate
import eu.hafixion.realms.towny.towns.commands.TownCommandMessages.town_claim_fail_claimed
import eu.hafixion.realms.towny.towns.commands.TownCommandMessages.town_claim_fail_no_perm
import eu.hafixion.realms.towny.towns.commands.TownCommandMessages.town_claim_fail_no_stockpile
import eu.hafixion.realms.towny.towns.commands.TownCommandMessages.town_claim_fail_not_contiguous
import eu.hafixion.realms.towny.towns.commands.TownCommandMessages.town_claim_fail_poor_claim
import eu.hafixion.realms.towny.towns.commands.TownCommandMessages.town_claim_fail_poor_upgrade
import eu.hafixion.realms.towny.towns.commands.TownCommandMessages.town_claim_fail_top
import eu.hafixion.realms.towny.towns.commands.TownCommandMessages.town_command_fail_invalid_player
import eu.hafixion.realms.towny.towns.commands.TownCommandMessages.town_command_fail_new_town
import eu.hafixion.realms.towny.towns.commands.TownCommandMessages.town_command_fail_no_town
import eu.hafixion.realms.towny.towns.commands.TownCommandMessages.town_command_fail_not_in_own_town
import eu.hafixion.realms.towny.towns.commands.TownCommandMessages.town_command_fail_not_in_town
import eu.hafixion.realms.towny.towns.commands.TownCommandMessages.town_command_fail_null_player
import eu.hafixion.realms.towny.towns.commands.TownCommandMessages.town_command_fail_town_player
import eu.hafixion.realms.towny.towns.commands.TownCommandMessages.town_delete_confirmation
import eu.hafixion.realms.towny.towns.commands.TownCommandMessages.town_delete_fail_no_perm
import eu.hafixion.realms.towny.towns.commands.TownCommandMessages.town_home_fail_error
import eu.hafixion.realms.towny.towns.commands.TownCommandMessages.town_home_fail_other_town
import eu.hafixion.realms.towny.towns.commands.TownCommandMessages.town_home_fail_public
import eu.hafixion.realms.towny.towns.commands.TownCommandMessages.town_invite_fail_in_town
import eu.hafixion.realms.towny.towns.commands.TownCommandMessages.town_invite_fail_no_perm
import eu.hafixion.realms.towny.towns.commands.TownCommandMessages.town_kick_fail_in_town
import eu.hafixion.realms.towny.towns.commands.TownCommandMessages.town_kick_fail_no_perm
import eu.hafixion.realms.towny.towns.commands.TownCommandMessages.town_kick_fail_yourself
import eu.hafixion.realms.towny.towns.commands.TownCommandMessages.town_leave_confirmation
import eu.hafixion.realms.towny.towns.commands.TownCommandMessages.town_leave_fail_mayor
import eu.hafixion.realms.towny.towns.commands.TownCommandMessages.town_new_fail_null
import eu.hafixion.realms.towny.towns.commands.TownCommandMessages.town_permissions_fail_added
import eu.hafixion.realms.towny.towns.commands.TownCommandMessages.town_permissions_fail_invalid_1
import eu.hafixion.realms.towny.towns.commands.TownCommandMessages.town_permissions_fail_invalid_2
import eu.hafixion.realms.towny.towns.commands.TownCommandMessages.town_permissions_fail_mayor
import eu.hafixion.realms.towny.towns.commands.TownCommandMessages.town_permissions_fail_no_perm
import eu.hafixion.realms.towny.towns.commands.TownCommandMessages.town_permissions_fail_null
import eu.hafixion.realms.towny.towns.commands.TownCommandMessages.town_permissions_fail_perm_1
import eu.hafixion.realms.towny.towns.commands.TownCommandMessages.town_permissions_fail_perm_2
import eu.hafixion.realms.towny.towns.commands.TownCommandMessages.town_permissions_fail_removed
import eu.hafixion.realms.towny.towns.commands.TownCommandMessages.town_permissions_fail_self_1
import eu.hafixion.realms.towny.towns.commands.TownCommandMessages.town_permissions_fail_self_2
import eu.hafixion.realms.towny.towns.commands.TownCommandMessages.town_permissions_fail_self_no_perm_1
import eu.hafixion.realms.towny.towns.commands.TownCommandMessages.town_permissions_fail_self_no_perm_2
import eu.hafixion.realms.towny.towns.commands.TownCommandMessages.town_permissions_success_1
import eu.hafixion.realms.towny.towns.commands.TownCommandMessages.town_permissions_success_2
import eu.hafixion.realms.towny.towns.commands.TownCommandMessages.town_set_home_fail_no_perm
import eu.hafixion.realms.towny.towns.commands.TownCommandMessages.town_set_mayor_confirmation
import eu.hafixion.realms.towny.towns.commands.TownCommandMessages.town_set_mayor_fail_resident
import eu.hafixion.realms.towny.towns.commands.TownCommandMessages.town_set_name_fail_no_perm
import eu.hafixion.realms.towny.towns.commands.TownCommandMessages.town_set_name_fail_null
import eu.hafixion.realms.towny.towns.commands.TownCommandMessages.town_set_perm_fail_invalid
import eu.hafixion.realms.towny.towns.commands.TownCommandMessages.town_set_perm_fail_no_perm
import eu.hafixion.realms.towny.towns.commands.TownCommandMessages.town_set_stockpile_fail_no_chest
import eu.hafixion.realms.towny.towns.commands.TownCommandMessages.town_set_stockpile_fail_no_perm
import eu.hafixion.realms.towny.towns.commands.TownCommandMessages.town_status_fail_console
import eu.hafixion.realms.towny.towns.commands.TownCommandMessages.town_status_fail_invalid
import eu.hafixion.realms.towny.towns.commands.TownCommandMessages.town_toggle_explosions_off
import eu.hafixion.realms.towny.towns.commands.TownCommandMessages.town_toggle_explosions_on
import eu.hafixion.realms.towny.towns.commands.TownCommandMessages.town_toggle_fail_no_perm
import eu.hafixion.realms.towny.towns.commands.TownCommandMessages.town_toggle_fail_null
import eu.hafixion.realms.towny.towns.commands.TownCommandMessages.town_toggle_fire_off
import eu.hafixion.realms.towny.towns.commands.TownCommandMessages.town_toggle_fire_on
import eu.hafixion.realms.towny.towns.commands.TownCommandMessages.town_toggle_invalid_setting
import eu.hafixion.realms.towny.towns.commands.TownCommandMessages.town_toggle_mobs_off
import eu.hafixion.realms.towny.towns.commands.TownCommandMessages.town_toggle_mobs_on
import eu.hafixion.realms.towny.towns.commands.TownCommandMessages.town_toggle_newbie_confirmation
import eu.hafixion.realms.towny.towns.commands.TownCommandMessages.town_toggle_public_off
import eu.hafixion.realms.towny.towns.commands.TownCommandMessages.town_toggle_public_on
import eu.hafixion.realms.towny.towns.commands.TownCommandMessages.town_toggle_pvp_off
import eu.hafixion.realms.towny.towns.commands.TownCommandMessages.town_toggle_pvp_on
import eu.hafixion.realms.towny.towns.commands.TownCommandMessages.town_unclaim_confirmation
import eu.hafixion.realms.towny.towns.commands.TownCommandMessages.town_unclaim_fail_homeblock
import eu.hafixion.realms.towny.towns.commands.TownCommandMessages.town_unclaim_fail_stockpile
import eu.hafixion.realms.towny.towns.commands.TownCommandMessages.town_unclaim_success_claim
import eu.hafixion.realms.towny.towns.commands.TownCommandMessages.town_unclaim_success_upgrade
import eu.hafixion.realms.towny.towns.commands.TownPermissions.claim
import eu.hafixion.realms.towny.towns.commands.TownPermissions.delete
import eu.hafixion.realms.towny.towns.commands.TownPermissions.edit_perm
import eu.hafixion.realms.towny.towns.commands.TownPermissions.invite
import eu.hafixion.realms.towny.towns.commands.TownPermissions.set_home
import eu.hafixion.realms.towny.towns.commands.TownPermissions.set_name
import eu.hafixion.realms.towny.towns.commands.TownPermissions.set_perm
import eu.hafixion.realms.towny.towns.commands.TownPermissions.set_stockpile
import eu.hafixion.realms.towny.towns.commands.TownPermissions.toggle
import eu.hafixion.realms.towny.towns.events.TownUnclaimEvent
import eu.hafixion.realms.towny.towns.isValidTownPerm
import eu.hafixion.realms.utils.*
import org.bukkit.block.Chest
import org.bukkit.entity.Player
import java.util.*
import kotlin.math.roundToInt
import kotlin.time.seconds

object TownCommand {
    val allTownNames get() = hashSetOf<String>().apply { TownDatabase.towns.forEach { add(it.name) }}

    fun RealmsCorePlugin.townCommand() {
        command( "town") {
            this.aliases.add("t")

            townSetupCommands()

            townSetCommand()

            townToggleCommand()

            townDebugCommands()

            townUtilCommands()

            townListCommand()

            townPermCommands()

            townInviteCommand()

            tabComplete {
                if (args.size <= 1)
                    allTownNames.filter { it.lowercase().startsWith((args.getOrNull(0) ?: "").lowercase()) }
                        .toMutableList().apply { addAll(defaultTabComplete(sender, alias, args)) }
                else defaultTabComplete(sender, alias, args)
            }

            executor {
                val name = (args.getOrNull(0)
                    ?: if (sender !is Player) fail(
                        errorMessage(town_status_fail_console)
                    ) else (sender as Player).toRealmsPlayer().town?.name
                        ?: fail(errorMessage(town_command_fail_no_town))).replace("`", "")

                val town = getTown(name) ?: fail(errorMessage(town_status_fail_invalid.format(name)))

                town.sendStatusMessage(sender)
            }

            command("stockpile") { executorPlayer {
                val townName = args.getOrNull(0) ?: ""
                val town = getTown(townName) ?: sender.toRealmsPlayer().town ?: fail(errorMessage(
                    "You need to specify a town, or join one."))

                if (town.stockpileChest != null) {
                    sender.msg(blackLine())
                    sender.msg("§6§l${town.name.uppercase()}'S STOCKPILE")
                    sender.msg("§6[§e ${town.stockpileChest!!.asPos().axis().joinToString()} §6]")
                    sender.msg(blackLine())
                } else sender.msg(errorMessage("${town.name} don't have a stockpile chest."))
            }}

            command("upkeep") { executorPlayer {
                val town = sender.toRealmsPlayer().town ?: fail(errorMessage(town_command_fail_no_town))

                sender.msg(blackLine())
                sender.msg("§6§lTOWN UPKEEP")

                val upkeep = town.hasItemsInStockpileChest(town.calculateUpkeep())
                if (upkeep.first) sender.msg("§7The town §acan§7 pay upkeep today.")
                else {
                    sender.msg("§7The town §ccannot§7 pay upkeep today.")
                    sender.msg("§6We still need:")
                    upkeep.second.toStringList().forEach { sender.msg("§e- $it") }
                }
                sender.msg("")
                sender.msg("§5Total Upkeep Due:")
                town.calculateUpkeep().toStringList().forEach { sender.msg("§d- $it") }
                sender.msg(blackLine())
            }}

            command("check-perm") { executorPlayer {
                val town = sender.toRealmsPlayer().town ?: fail(errorMessage(town_command_fail_no_town))

                val name = args.getOrNull(0) ?: fail(errorMessage(town_command_fail_null_player))
                val player = RealmsPlayerDatabase[name] ?: fail(errorMessage(town_command_fail_invalid_player.format(name)))

                if (player.town != town) sender.msg(errorMessage(town_kick_fail_in_town))

                sender.msg("""
                    ${blackLine()}
                    §6§l${player.name.uppercase()}
                    §6[§e ${town.permissions[player]?.joinToString()} §6]
                    ${blackLine()}
                """.trimIndent())
            }}
        }
    }

    private fun CommandDSL.townPermCommands() {
        command("add-perm") {
            tabComplete {
                if (args.size >= 3) TownPermissions.all.toList().filter { it.startsWith((args.getOrNull(0) ?: ""))}
                else mutableListOf<String>().apply { onlinePlayers.forEach { add(it.name) } }.filter { it.startsWith((args.getOrNull(0) ?: ""))}
            } // town add-perm <player> <perm>
            // town 0 1 2

            executorPlayer {
            val town = sender.toRealmsPlayer().town ?: fail(errorMessage(town_command_fail_no_town))
            if (!sender.toRealmsPlayer().hasTownPermission(edit_perm))
                fail(errorMessage(town_permissions_fail_no_perm))

            val name = args.getOrNull(0) ?: fail(errorMessage(town_command_fail_null_player))
            val player = RealmsPlayerDatabase[name] ?: fail(errorMessage(town_command_fail_invalid_player.format(name)))
            if (player.town != town) fail(errorMessage(town_command_fail_town_player))

            if (player == sender.toRealmsPlayer()) fail(errorMessage(town_permissions_fail_self_1))
            if (player == town.mayor) fail(errorMessage(town_permissions_fail_mayor))

            val perm = args.getOrNull(1) ?: fail(errorMessage(town_permissions_fail_null))
            if (player.hasTownPermission(perm)) fail(errorMessage(town_permissions_fail_added))
            if (!perm.isValidTownPerm()) fail(errorMessage(town_permissions_fail_invalid_1))

            if (perm == edit_perm) fail(errorMessage(town_permissions_fail_perm_1))

            if (!sender.toRealmsPlayer().hasTownPermission(perm))
                fail(errorMessage(town_permissions_fail_self_no_perm_1))

            town.permissions[player] = town.permissions[player] ?: hashSetOf()
            town.permissions[player]!!.add(perm)

            sender.msg(successMessage(town_permissions_success_1.format(player.name)))
        }}

        command("remove-perm") {
            tabComplete {
                if (args.size >= 3)
                    TownPermissions.all.toList().filter { it.startsWith((args.getOrNull(0) ?: ""))}
                else mutableListOf<String>().apply {
                    onlinePlayers.forEach { add(it.name) } }
                    .filter { it.startsWith((args.getOrNull(0) ?: ""))
                    }
            }

            executorPlayer {
            val town = sender.toRealmsPlayer().town ?: fail(errorMessage(town_command_fail_no_town))
            if (!sender.toRealmsPlayer().hasTownPermission(edit_perm))
                fail(errorMessage(town_permissions_fail_no_perm))

            val name = args.getOrNull(0) ?: fail(errorMessage(town_command_fail_null_player))
            val player = RealmsPlayerDatabase[name] ?: fail(errorMessage(town_command_fail_invalid_player.format(name)))
            if (player.town != town) fail(errorMessage(town_command_fail_town_player))

            if (player == sender.toRealmsPlayer()) fail(errorMessage(town_permissions_fail_self_2))
            if (player == town.mayor) fail(errorMessage(town_permissions_fail_mayor))

            val perm = args.getOrNull(1) ?: fail(errorMessage(town_permissions_fail_null))
            if (!player.hasTownPermission(perm)) fail(errorMessage(town_permissions_fail_removed))
            if (!perm.isValidTownPerm()) fail(errorMessage(town_permissions_fail_invalid_2))

            if (perm == edit_perm && !sender.toRealmsPlayer().isMayor) fail(errorMessage(town_permissions_fail_perm_2))

            if (!sender.toRealmsPlayer().hasTownPermission(perm))
                fail(errorMessage(town_permissions_fail_self_no_perm_2))

            town.permissions[player] = town.permissions[player] ?: hashSetOf()
            town.permissions[player]!!.remove(perm)

            sender.msg(successMessage(town_permissions_success_2.format(player.name)))
        }}
    }

    private fun CommandDSL.townInviteCommand() {
        command("invite") {
            tabComplete {
                mutableListOf<String>().apply { onlinePlayers.forEach { add(it.name) } }
                    .filter { it.startsWith((args.getOrNull(0) ?: "")) }
            }

            executorPlayer {

            val town = sender.toRealmsPlayer().town ?: fail(errorMessage(town_command_fail_no_town))
            if (!sender.toRealmsPlayer().hasTownPermission(invite))
                fail(errorMessage(town_invite_fail_no_perm))

            val name = args.getOrNull(0) ?: fail(errorMessage(town_command_fail_null_player))
            val player = RealmsPlayerDatabase[name] ?: fail(errorMessage(town_command_fail_invalid_player.format(name)))

            if (!player.online) fail(errorMessage("That player isn't online."))
            if (player.town != null) fail(errorMessage(town_invite_fail_in_town))

            if (player.hasConfirmation()) fail(errorMessage("A town has already invited $name"))

            town.broadcast("${sender.name} has invited $name to the town.")
            ConfirmationBuilder().apply {
                setTitle("§6§lTOWN INVITE§e Do you want to join §6${town.name}?")
                runOnCancel { sender.msg(errorMessage("$name has denied the invite.")) }
                runOnAccept {
                    town.citizens.add(player)
                    town.broadcast(town_welcome_player.format(player.name))
                }
                sendTo(player.player)
            }
        }}

        command("kick") {
            tabComplete { mutableListOf<String>().apply { onlinePlayers.forEach { add(it.name) } } }
            executorPlayer {

            val town = sender.toRealmsPlayer().town ?: fail(errorMessage(town_command_fail_no_town))
            if (!sender.toRealmsPlayer().hasTownPermission(invite))
                fail(errorMessage(town_kick_fail_no_perm))

            val name = args.getOrNull(0) ?: fail(errorMessage(town_command_fail_null_player))
            val player = RealmsPlayerDatabase[name] ?: fail(errorMessage(town_command_fail_invalid_player.format(name)))

            if (player == sender.toRealmsPlayer()) fail(errorMessage(town_kick_fail_yourself))
            if (player.town != town) fail(errorMessage(town_kick_fail_in_town))

            town.citizens.remove(player)
            town.broadcast(town_kick_player.format(player.name))
        }}

        command("add") { executorPlayer {
            if (!sender.isOp) fail(errorMessage("This is an Op-only command."))
            val town = sender.toRealmsPlayer().town ?: fail(errorMessage(town_command_fail_no_town))

            val name = args.getOrNull(0) ?: fail(errorMessage(town_command_fail_null_player))
            val player = RealmsPlayerDatabase[name] ?: fail(errorMessage(town_command_fail_invalid_player.format(name)))

            if (player.town != null) player.town?.citizens?.remove(player)

            town.citizens.add(player)
            fail("done sir")
        }}
    }

    private fun CommandDSL.townToggleCommand() {
        tabComplete { listOf("newbie", "pvp", "fire", "explosion", "mobs", "public") }

        command("toggle") { executorPlayer {
            val town = sender.toRealmsPlayer().town ?: fail(errorMessage(town_command_fail_no_town))
            if (!sender.toRealmsPlayer().hasTownPermission(toggle))
                fail(errorMessage(town_toggle_fail_no_perm))

            val setting = args.getOrNull(0)
                ?: fail(errorMessage(town_toggle_fail_null))
            when (setting) {
                "newbie" -> if (town.isNew() && sender.confirmation(town_toggle_newbie_confirmation)) {
                    town.newbie = false
                    town.broadcast(newbie_turned_off)
                } else sender.msg(errorMessage(town_command_fail_new_town))
                "pvp" -> {
                    if (System.currentTimeMillis() - town.lastPvpToggle < 30.seconds.toLongMilliseconds())
                        fail(errorMessage("Town PVP is currently on cooldown, wait ${((30.seconds.toLongMilliseconds() - (System.currentTimeMillis() - town.lastPvpToggle)).toDouble() / 1000).roundToInt()}"))

                    town.pvp = !town.pvp

                    if (town.pvp) town.broadcast(town_toggle_pvp_on)
                    else town.broadcast(town_toggle_pvp_off)

                    town.lastPvpToggle = System.currentTimeMillis()
                }
                "fire" -> {
                    town.fire = !town.fire
                    if (town.fire) town.broadcast(town_toggle_fire_on)
                    else town.broadcast(town_toggle_fire_off)
                }
                "mobs" -> {
                    town.mobs = !town.mobs
                    if (town.mobs) town.broadcast(town_toggle_mobs_on)
                    else town.broadcast(town_toggle_mobs_off)
                }
                "explosion" -> {
                    town.explosions = !town.explosions
                    if (town.explosions) town.broadcast(town_toggle_explosions_on)
                    else town.broadcast(town_toggle_explosions_off)
                }
                "public" -> {
                    town.public = !town.public
                    if (town.public) town.broadcast(town_toggle_public_on)
                    else town.broadcast(town_toggle_public_off)
                }
                else -> sender.msg(errorMessage(town_toggle_invalid_setting))
            }
        }}
    }

    private fun CommandDSL.townSetupCommands() {
        command("new") {
            aliases.add("create")
            executorPlayer {
                val name = args.getOrNull(0) ?: fail(errorMessage(town_new_fail_null))
                Town.newTown(sender, name)
            }}

        command("claim") { executorPlayer {
            val town = sender.toRealmsPlayer().town ?: fail(errorMessage(town_command_fail_no_town))

            if (!sender.toRealmsPlayer().hasTownPermission(claim))
                fail(errorMessage(town_claim_fail_no_perm))

            if (town.stockpileChest == null) fail(errorMessage(town_claim_fail_no_stockpile))

            if (town.claims.keys.size < town.claimLimit) sender.confirmation("Are you sure you want to claim this chunk, remember to do /check-chunk to figure out the price.")
            else sender.confirmation("Are you sure you want to claim/upgrade this chunk? Be aware that we are currently overclaiming.")

            val claimStatus = town.claim(sender.location.chunk)
            when (claimStatus.first) {
                ClaimResult.UPGRADED -> town.broadcast(
                    townClaimSuccessUpgrade(
                        sender.name,
                        sender.location.chunk.asPos()
                    )
                )
                ClaimResult.TOP -> fail(errorMessage(town_claim_fail_top))
                ClaimResult.CLAIMED -> town.broadcast(
                    townClaimSuccessClaim(
                        sender.name,
                        sender.location.chunk.asPos()
                    )
                )
                ClaimResult.FAILED -> fail(errorMessage(town_claim_fail_claimed))
                ClaimResult.POOR_CLAIM -> sender.msg(errorMessage(town_claim_fail_poor_claim))
                ClaimResult.POOR_UPGRADE -> sender.msg(errorMessage(town_claim_fail_poor_upgrade))
                ClaimResult.NOT_CONTIGUOUS -> fail(errorMessage(town_claim_fail_not_contiguous))
            }

            if (claimStatus.second.isNotEmpty()) {
                sender.msg("§6You still need§e:")
                for (itemString in claimStatus.second.toStringList()) sender.msg("§e- $itemString")
            }
        }}

        command("unclaim") { executorPlayer {
            val town = sender.toRealmsPlayer().town ?: fail(errorMessage(town_command_fail_no_town))

            if (!sender.toRealmsPlayer().hasTownPermission(claim))
                fail(errorMessage(town_claim_fail_no_perm))

            val chunk = sender.location.chunk
            if (getTown(chunk) != town) fail(errorMessage(town_command_fail_not_in_town))
            if (town.homeBlock.chunk == chunk) fail(errorMessage(town_unclaim_fail_homeblock))
            if (town.stockpileChest?.chunk == chunk) fail(errorMessage(town_unclaim_fail_stockpile))

            if (!sender.confirmation(town_unclaim_confirmation)) return@executorPlayer
            when (town.unclaim(chunk)) {
                ClaimResult.CLAIMED -> fail(successMessage(town_unclaim_success_claim))
                ClaimResult.UPGRADED -> fail(successMessage(town_unclaim_success_upgrade))
                else -> {}
            }
            TownUnclaimEvent(town, chunk.asPos()).callEvent()
        }}
    }

    private fun CommandDSL.townSetCommand() {
        command("set") {

            command("name") { executorPlayer {
                val newName = args.getOrNull(0) ?: fail(errorMessage(town_set_name_fail_null))
                val town = sender.toRealmsPlayer().town ?: fail(errorMessage(town_command_fail_no_town))

                if (newName.length > 15) fail(errorMessage("Your town's name has too many characters, maximum is 15!"))

                if (newName.contains('`'))
                    fail(errorMessage("You aren't allowed to have backticks in town names."))

                if (!sender.toRealmsPlayer().hasTownPermission(set_name))
                    fail(errorMessage(town_set_name_fail_no_perm))

                town.broadcast(town_new_name.format(newName))
                town.name = newName
            }}

            command("mayor") { executorPlayer {
                val town = sender.toRealmsPlayer().town ?: fail(errorMessage(town_command_fail_no_town))
                if (town.mayor != sender.toRealmsPlayer())
                    fail(errorMessage(town_set_mayor_fail_resident))

                val name = args.getOrNull(0) ?: fail(errorMessage(town_command_fail_null_player))
                val player = RealmsPlayerDatabase[name] ?: fail(errorMessage(town_command_fail_invalid_player.format(name)))

                if (player.town != town) fail(errorMessage(town_command_fail_town_player))

                if (sender.confirmation(town_set_mayor_confirmation)) {
                    town.citizens.remove(player)
                    town.citizens.add(sender.toRealmsPlayer())
                    town.mayor = player
                    town.broadcast(town_new_mayor.format(player.name))
                }
            }}

            command("stockpile") { executorPlayer {
                if (sender.toRealmsPlayer().town == null) fail(errorMessage(town_command_fail_no_town))
                if (!sender.toRealmsPlayer().hasTownPermission(set_stockpile))
                    fail(errorMessage(town_set_stockpile_fail_no_perm))

                for (loc in sender.location.getNearbyBlocks()) if (loc.block.state is Chest) {
                    val town = sender.toRealmsPlayer().town!!
                    town.stockpileChest = loc

                    town.broadcast(townChangedStockpile(loc.block.asPos()))
                    return@executorPlayer
                }

                sender.msg(errorMessage(town_set_stockpile_fail_no_chest))
            }}

            command("home") { executorPlayer {
                val town = sender.toRealmsPlayer().town ?: fail(errorMessage(town_command_fail_no_town))

                if (!sender.toRealmsPlayer().hasTownPermission(set_home))
                    fail(errorMessage(town_set_home_fail_no_perm))

                if (getTown(sender.location) != town) fail(errorMessage(town_command_fail_not_in_own_town))

                town.homeBlock = sender.location.block
                town.broadcast(townChangedHomeblock(town.homeBlock.asPos()))
            }}

            command("perm") {
                executorPlayer {
                    sender.toRealmsPlayer().town ?: fail(errorMessage(town_command_fail_no_town))

                    if (!sender.toRealmsPlayer().hasTownPermission(set_perm))
                        fail(errorMessage(town_set_perm_fail_no_perm))
                }

                command("nation-build") { executorPlayer {
                    val town = sender.toRealmsPlayer().town ?: fail(errorMessage(town_command_fail_no_town))

                    if (!sender.toRealmsPlayer().hasTownPermission(set_perm))
                        fail(errorMessage(town_set_perm_fail_no_perm))

                    val bool = (args.getOrNull(0)
                        ?: fail(errorMessage(town_set_perm_fail_invalid))).toBooleanOrNull()
                        ?: fail(errorMessage(town_set_perm_fail_invalid))

                    if (bool) {
                        town.nationPerms[0] = true
                        town.broadcast("Nation Build Permissions are now enabled.")
                    } else {
                        town.nationPerms[0] = false
                        town.broadcast("Nation Build Permissions are now disabled.")
                    }
                }}

                command("nation-destroy") { executorPlayer {
                    val town = sender.toRealmsPlayer().town ?: fail(errorMessage(town_command_fail_no_town))

                    if (!sender.toRealmsPlayer().hasTownPermission(set_perm))
                        fail(errorMessage(town_set_perm_fail_no_perm))

                    val bool = (args.getOrNull(0)
                        ?: fail(errorMessage(town_set_perm_fail_invalid))).toBooleanOrNull()
                        ?: fail(errorMessage(town_set_perm_fail_invalid))

                    if (bool) {
                        town.nationPerms[1] = true
                        town.broadcast("Nation Destroy Permissions are now enabled.")
                    } else {
                        town.nationPerms[1] = false
                        town.broadcast("Nation Destroy Permissions are now disabled.")
                    }
                }}

                command("nation-interact") { executorPlayer {
                    val town = sender.toRealmsPlayer().town ?: fail(errorMessage(town_command_fail_no_town))

                    if (!sender.toRealmsPlayer().hasTownPermission(set_perm))
                        fail(errorMessage(town_set_perm_fail_no_perm))

                    val bool = (args.getOrNull(0)
                        ?: fail(errorMessage(town_set_perm_fail_invalid))).toBooleanOrNull()
                        ?: fail(errorMessage(town_set_perm_fail_invalid))

                    if (bool) {
                        town.nationPerms[2] = true
                        town.broadcast("Nation Interact Permissions are now enabled.")
                    } else {
                        town.nationPerms[2] = false
                        town.broadcast("Nation Interact Permissions are now disabled.")
                    }
                }}

                command("nation-switch") { executorPlayer {
                    val town = sender.toRealmsPlayer().town ?: fail(errorMessage(town_command_fail_no_town))

                    if (!sender.toRealmsPlayer().hasTownPermission(set_perm))
                        fail(errorMessage(town_set_perm_fail_no_perm))

                    val bool = (args.getOrNull(0)
                        ?: fail(errorMessage(town_set_perm_fail_invalid))).toBooleanOrNull()
                        ?: fail(errorMessage(town_set_perm_fail_invalid))

                    if (bool) {
                        town.nationPerms[3] = true
                        town.broadcast("Nation Switch Permissions are now enabled.")
                    } else {
                        town.nationPerms[3] = false
                        town.broadcast("Nation Switch Permissions are now disabled.")
                    }
                }}


                command("town-build") { executorPlayer {
                    val town = sender.toRealmsPlayer().town ?: fail(errorMessage(town_command_fail_no_town))

                    if (!sender.toRealmsPlayer().hasTownPermission(set_perm))
                        fail(errorMessage(town_set_perm_fail_no_perm))

                    val bool = (args.getOrNull(0)
                        ?: fail(errorMessage(town_set_perm_fail_invalid))).toBooleanOrNull()
                        ?: fail(errorMessage(town_set_perm_fail_invalid))

                    if (bool) {
                        town.townPerms[0] = true
                        town.broadcast("Town Build Permissions are now enabled.")
                    } else {
                        town.townPerms[0] = false
                        town.broadcast("Town Build Permissions are now disabled.")
                    }
                }}

                command("town-destroy") { executorPlayer {
                    val town = sender.toRealmsPlayer().town ?: fail(errorMessage(town_command_fail_no_town))

                    if (!sender.toRealmsPlayer().hasTownPermission(set_perm))
                        fail(errorMessage(town_set_perm_fail_no_perm))

                    val bool = (args.getOrNull(0)
                        ?: fail(errorMessage(town_set_perm_fail_invalid))).toBooleanOrNull()
                        ?: fail(errorMessage(town_set_perm_fail_invalid))

                    if (bool) {
                        town.townPerms[1] = true
                        town.broadcast("Town Destroy Permissions are now enabled.")
                    } else {
                        town.townPerms[1] = false
                        town.broadcast("Town Destroy Permissions are now disabled.")
                    }
                }}

                command("town-interact") { executorPlayer {
                    val town = sender.toRealmsPlayer().town ?: fail(errorMessage(town_command_fail_no_town))

                    if (!sender.toRealmsPlayer().hasTownPermission(set_perm))
                        fail(errorMessage(town_set_perm_fail_no_perm))

                    val bool = (args.getOrNull(0)
                        ?: fail(errorMessage(town_set_perm_fail_invalid))).toBooleanOrNull()
                        ?: fail(errorMessage(town_set_perm_fail_invalid))

                    if (bool) {
                        town.townPerms[2] = true
                        town.broadcast("Town Interact Permissions are now enabled.")
                    } else {
                        town.townPerms[2] = false
                        town.broadcast("Town Interact Permissions are now disabled.")
                    }
                }}

                command("town-switch") { executorPlayer {
                    val town = sender.toRealmsPlayer().town ?: fail(errorMessage(town_command_fail_no_town))

                    if (!sender.toRealmsPlayer().hasTownPermission(set_perm))
                        fail(errorMessage(town_set_perm_fail_no_perm))

                    val bool = (args.getOrNull(0)
                        ?: fail(errorMessage(town_set_perm_fail_invalid))).toBooleanOrNull()
                        ?: fail(errorMessage(town_set_perm_fail_invalid))

                    if (bool) {
                        town.townPerms[3] = true
                        town.broadcast("Town Switch Permissions are now enabled.")
                    } else {
                        town.townPerms[3] = false
                        town.broadcast("Town Switch Permissions are now disabled.")
                    }
                }}


                command("outsider-build") { executorPlayer {
                    val town = sender.toRealmsPlayer().town ?: fail(errorMessage(town_command_fail_no_town))

                    if (!sender.toRealmsPlayer().hasTownPermission(set_perm))
                        fail(errorMessage(town_set_perm_fail_no_perm))

                    val bool = (args.getOrNull(0)
                        ?: fail(errorMessage(town_set_perm_fail_invalid))).toBooleanOrNull()
                        ?: fail(errorMessage(town_set_perm_fail_invalid))

                    if (bool) {
                        town.outsiderPerms[0] = true
                        town.broadcast("Outsider Build Permissions are now enabled.")
                    } else {
                        town.outsiderPerms[0] = false
                        town.broadcast("Outsider Build Permissions are now disabled.")
                    }
                }}

                command("outsider-destroy") { executorPlayer {
                    val town = sender.toRealmsPlayer().town ?: fail(errorMessage(town_command_fail_no_town))

                    if (!sender.toRealmsPlayer().hasTownPermission(set_perm))
                        fail(errorMessage(town_set_perm_fail_no_perm))

                    val bool = (args.getOrNull(0)
                        ?: fail(errorMessage(town_set_perm_fail_invalid))).toBooleanOrNull()
                        ?: fail(errorMessage(town_set_perm_fail_invalid))

                    if (bool) {
                        town.outsiderPerms[1] = true
                        town.broadcast("Outsider Destroy Permissions are now enabled.")
                    } else {
                        town.outsiderPerms[1] = false
                        town.broadcast("Outsider Destroy Permissions are now disabled.")
                    }
                }}

                command("outsider-interact") { executorPlayer {
                    val town = sender.toRealmsPlayer().town ?: fail(errorMessage(town_command_fail_no_town))

                    if (!sender.toRealmsPlayer().hasTownPermission(set_perm))
                        fail(errorMessage(town_set_perm_fail_no_perm))

                    val bool = (args.getOrNull(0)
                        ?: fail(errorMessage(town_set_perm_fail_invalid))).toBooleanOrNull()
                        ?: fail(errorMessage(town_set_perm_fail_invalid))

                    if (bool) {
                        town.outsiderPerms[2] = true
                        town.broadcast("Outsider Interact Permissions are now enabled.")
                    } else {
                        town.outsiderPerms[2] = false
                        town.broadcast("Outsider Interact Permissions are now disabled.")
                    }
                }}

                command("outsider-switch") { executorPlayer {
                    val town = sender.toRealmsPlayer().town ?: fail(errorMessage(town_command_fail_no_town))

                    if (!sender.toRealmsPlayer().hasTownPermission(set_perm))
                        fail(errorMessage(town_set_perm_fail_no_perm))

                    val bool = (args.getOrNull(0)
                        ?: fail(errorMessage(town_set_perm_fail_invalid))).toBooleanOrNull()
                        ?: fail(errorMessage(town_set_perm_fail_invalid))

                    if (bool) {
                        town.outsiderPerms[3] = true
                        town.broadcast("Outsider Switch Permissions are now enabled.")
                    } else {
                        town.outsiderPerms[3] = false
                        town.broadcast("Outsider Switch Permissions are now disabled.")
                    }
                }}
            }
        }
    }

    private fun CommandDSL.townUtilCommands() {
        command("leave") { executorPlayer {
            val town = sender.toRealmsPlayer().town ?: fail(errorMessage(town_command_fail_no_town))
            if (town.mayor == sender.toRealmsPlayer())
                fail(errorMessage(town_leave_fail_mayor))

            if (sender.confirmation(town_leave_confirmation)) {
                town.broadcast(town_goodbye_player.format(sender.name))
                town.citizens.remove(sender.toRealmsPlayer())
            }
        }}

        command("delete") { executorPlayer {
            val town = sender.toRealmsPlayer().town ?: fail(errorMessage(town_command_fail_no_town))
            if (!sender.toRealmsPlayer().hasTownPermission(delete))
                fail(errorMessage(town_delete_fail_no_perm))

            if (sender.confirmation(town_delete_confirmation)) {
                town.broadcast(town_delete.format(sender.name))
                town.delete()
            }
        }}

        command("here") { executorPlayer {
            val town = getTown(sender.location) ?: fail(errorMessage(town_command_fail_not_in_town))

            town.sendStatusMessage(sender)
        }}

        command("online") {
            this.tabComplete { allTownNames.filter { it.lowercase().startsWith((args.getOrNull(0) ?: "").lowercase()) } }
            executorPlayer {
            val town = getTown(args.getOrNull(0) ?: "")
                ?: sender.toRealmsPlayer().town ?: fail(errorMessage(town_command_fail_no_town))

            sender.msg("""
                    §6§lONLINE PLAYERS IN ${town.name.uppercase()}
                    §6 [ §e${town.residents.filter { it.online }.joinToString { it.name }}§6 ]
                """.trimIndent())
        }}
        command("home") {
            this.aliases.add("spawn")
            this.tabComplete { allTownNames.filter { it.lowercase().startsWith((args.getOrNull(0) ?: "").lowercase()) } }
            executorPlayer {
                val town = getTown(args.getOrNull(0) ?: "")
                    ?: sender.toRealmsPlayer().town ?: fail(errorMessage(town_command_fail_no_town))

                if (town == sender.toRealmsPlayer().town) {
                    if (!sender.toRealmsPlayer().canTeleportHome()) fail(errorMessage(town_home_fail_error))
                } else if (!sender.toRealmsPlayer().canTeleportToTown(town))
                    fail(errorMessage(town_home_fail_other_town))
                else if (!town.public)
                    fail(errorMessage(town_home_fail_public))

                if (System.currentTimeMillis() - sender.toRealmsPlayer().lastTeleported < 5000) fail(errorMessage("You need to wait at least 5 seconds before teleporting again!"))
                if (sender.toRealmsPlayer().combatTagged) fail(errorMessage("You're combat tagged currently."))

                if (town.pvp) if (!sender.confirmation("Are you sure you want to teleport to this town? PVP is enabled.")) return@executorPlayer

                sender.teleport(town.homeBlock.location)
                sender.sendEnteringTown(town)
                sender.toRealmsPlayer().lastTeleported = System.currentTimeMillis()
            }}
    }

    private fun CommandDSL.townListCommand() {
        command("list") { executor {
            sender.msg(blackLine())
            sender.msg("§6§lTOWN RANKINGS")

            //todo pagination
            val townList = TownDatabase.towns.toList()
            Collections.sort(townList, Collections.reverseOrder())
            for ((n, town) in townList.withIndex()) {
                if (n >= 15) break
                sender.msg(townRankingsTemplate(town))
            }

            sender.msg(blackLine())
        }}
    }

    private fun CommandDSL.townDebugCommands() {
//        command("takeUpkeep") { executorPlayer {
//            val name = args.getOrNull(0) ?: fail(errorMessage("Please specify a town."))
//            val town = getTown(name) ?: fail(errorMessage("Invalid town specified."))
//
//            if (!town.takeUpkeep().first) town.delete()
//            sender.msg(successMessage("Took upkeep from $name successfully."))
//        }}
    }
}