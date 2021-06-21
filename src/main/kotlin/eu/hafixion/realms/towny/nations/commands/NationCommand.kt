package eu.hafixion.realms.towny.nations.commands

import br.com.devsrsouza.kotlinbukkitapi.dsl.command.CommandDSL
import br.com.devsrsouza.kotlinbukkitapi.dsl.command.command
import br.com.devsrsouza.kotlinbukkitapi.dsl.command.fail
import br.com.devsrsouza.kotlinbukkitapi.extensions.bukkit.broadcast
import br.com.devsrsouza.kotlinbukkitapi.extensions.event.callEvent
import br.com.devsrsouza.kotlinbukkitapi.extensions.text.msg
import br.com.devsrsouza.kotlinbukkitapi.utils.toBooleanOrNull
import eu.hafixion.realms.RealmsCorePlugin
import eu.hafixion.realms.RealmsPlayer
import eu.hafixion.realms.RealmsPlayerDatabase
import eu.hafixion.realms.confirmation.ConfirmationBuilder
import eu.hafixion.realms.confirmation.ConfirmationHandler
import eu.hafixion.realms.toRealmsPlayer
import eu.hafixion.realms.towny.nations.Nation
import eu.hafixion.realms.towny.nations.NationDatabase
import eu.hafixion.realms.towny.nations.commands.NationPermission.claim
import eu.hafixion.realms.towny.nations.commands.NationPermission.delete
import eu.hafixion.realms.towny.nations.commands.NationPermission.invite
import eu.hafixion.realms.towny.nations.commands.NationPermission.kick
import eu.hafixion.realms.towny.nations.commands.NationUtils.nationListTemplate
import eu.hafixion.realms.towny.nations.commands.NationUtils.nation_claim_fail_claimed
import eu.hafixion.realms.towny.nations.commands.NationUtils.nation_claim_fail_contiguous
import eu.hafixion.realms.towny.nations.commands.NationUtils.nation_claim_fail_far
import eu.hafixion.realms.towny.nations.commands.NationUtils.nation_claim_fail_no_perm
import eu.hafixion.realms.towny.nations.commands.NationUtils.nation_claim_fail_poor
import eu.hafixion.realms.towny.nations.commands.NationUtils.nation_claim_success
import eu.hafixion.realms.towny.nations.commands.NationUtils.nation_command_fail_no_nation
import eu.hafixion.realms.towny.nations.commands.NationUtils.nation_command_fail_no_town
import eu.hafixion.realms.towny.nations.commands.NationUtils.nation_create_confirmation
import eu.hafixion.realms.towny.nations.commands.NationUtils.nation_create_fail_long
import eu.hafixion.realms.towny.nations.commands.NationUtils.nation_create_fail_mayor
import eu.hafixion.realms.towny.nations.commands.NationUtils.nation_create_fail_name
import eu.hafixion.realms.towny.nations.commands.NationUtils.nation_create_fail_name_exists
import eu.hafixion.realms.towny.nations.commands.NationUtils.nation_create_fail_nation
import eu.hafixion.realms.towny.nations.commands.NationUtils.nation_create_fail_poor
import eu.hafixion.realms.towny.nations.commands.NationUtils.nation_delete_broadcast
import eu.hafixion.realms.towny.nations.commands.NationUtils.nation_delete_confirmation
import eu.hafixion.realms.towny.nations.commands.NationUtils.nation_delete_fail_no_perm
import eu.hafixion.realms.towny.nations.commands.NationUtils.nation_invite_broadcast
import eu.hafixion.realms.towny.nations.commands.NationUtils.nation_invite_fail_in_nation
import eu.hafixion.realms.towny.nations.commands.NationUtils.nation_invite_fail_no_perm
import eu.hafixion.realms.towny.nations.commands.NationUtils.nation_kick_broadcast
import eu.hafixion.realms.towny.nations.commands.NationUtils.nation_kick_capital
import eu.hafixion.realms.towny.nations.commands.NationUtils.nation_kick_confirmation
import eu.hafixion.realms.towny.nations.commands.NationUtils.nation_kick_fail_no_perm
import eu.hafixion.realms.towny.nations.commands.NationUtils.nation_kick_not_in_nation
import eu.hafixion.realms.towny.nations.commands.NationUtils.nation_kick_occupied
import eu.hafixion.realms.towny.nations.commands.NationUtils.nation_kick_self
import eu.hafixion.realms.towny.nations.commands.NationUtils.nation_leave_broadcast
import eu.hafixion.realms.towny.nations.commands.NationUtils.nation_leave_confirmation
import eu.hafixion.realms.towny.nations.commands.NationUtils.nation_leave_fail_no_perm
import eu.hafixion.realms.towny.nations.commands.NationUtils.nation_leave_fail_occupied
import eu.hafixion.realms.towny.nations.commands.NationUtils.nation_set_leader_broadcast
import eu.hafixion.realms.towny.nations.commands.NationUtils.nation_set_leader_confirmation
import eu.hafixion.realms.towny.nations.commands.NationUtils.nation_set_leader_fail_invalid
import eu.hafixion.realms.towny.nations.commands.NationUtils.nation_set_leader_fail_leader
import eu.hafixion.realms.towny.nations.commands.NationUtils.nation_set_leader_fail_mayor
import eu.hafixion.realms.towny.nations.commands.NationUtils.nation_set_leader_fail_null
import eu.hafixion.realms.towny.nations.commands.NationUtils.nation_set_leader_fail_other_nation
import eu.hafixion.realms.towny.nations.commands.NationUtils.nation_set_name_broadcast
import eu.hafixion.realms.towny.nations.commands.NationUtils.nation_unclaim_fail_invalid
import eu.hafixion.realms.towny.nations.events.NationDeleteEvent
import eu.hafixion.realms.towny.nations.events.TownJoinEvent
import eu.hafixion.realms.towny.nations.events.TownLeaveEvent
import eu.hafixion.realms.towny.towns.TownDatabase.getTown
import eu.hafixion.realms.towny.towns.commands.TownCommandMessages
import eu.hafixion.realms.towny.towns.commands.TownPermissions
import eu.hafixion.realms.towny.towns.commands.TownPermissions.nation_join
import eu.hafixion.realms.towny.towns.commands.TownPermissions.nation_leave
import eu.hafixion.realms.towny.towns.isValidTownPerm
import eu.hafixion.realms.utils.*
import org.bukkit.entity.Player
import java.util.*

object NationCommand {

    fun RealmsCorePlugin.nationCommand() {
        command("nation") {
            aliases.add("n")

            executor {
                val nationName = (args.getOrNull(0) ?: "").replace("`", "")
                val nation = NationDatabase[nationName] ?: ((sender as? Player) ?:
                fail(errorMessage("You need to specify a nation name console."))).toRealmsPlayer().nation ?:
                fail(errorMessage(nation_command_fail_no_nation))

                nation.sendStatusMessage(sender)
            }

            nationSetupCommands()
            nationListCommand()
            nationUtilCommands()
            nationInviteCommand()
            nationSetCommands()
            nationPermCommands()
            nationEnemyCommands()

            command("home") { executorPlayer {
                val nation = sender.toRealmsPlayer().nation ?: fail(errorMessage(nation_command_fail_no_nation))

                if (!sender.toRealmsPlayer().canTeleportToTown(nation.capital)) fail(errorMessage("You cannot teleport at this time."))

                if (System.currentTimeMillis() - sender.toRealmsPlayer().lastTeleported < 5000) fail(errorMessage("You need to wait at least 5 seconds before teleporting again!"))

                if (sender.toRealmsPlayer().combatTagged) fail(errorMessage("You're combat tagged currently."))
                sender.teleport(nation.capital.homeBlock.location)
                sender.sendEnteringTown(nation.capital)
                sender.toRealmsPlayer().lastTeleported = System.currentTimeMillis()
            }}

            command("check-perm") { executorPlayer {
                val nation = sender.toRealmsPlayer().nation ?: fail(errorMessage(nation_command_fail_no_nation))

                val name = args.getOrNull(0) ?: fail(errorMessage(TownCommandMessages.town_command_fail_null_player))
                val player = RealmsPlayerDatabase[name] ?: fail(errorMessage(TownCommandMessages.town_command_fail_invalid_player.format(name)))

                if (player.nation != nation) sender.msg(errorMessage("That player isn't in your nation."))

                sender.msg("""
                    ${blackLine()}
                    §5§l${player.name.uppercase()}
                    §5[§d ${nation.permissions[player]?.joinToString()} §5]
                    ${blackLine()}
                """.trimIndent())
            }}
        }

    }

    private fun CommandDSL.nationEnemyCommands() {
        command("enemy") {

            command("remove") { executorPlayer {
                val name = args.getOrNull(0) ?: ""
                val nation = NationDatabase[name] ?: fail(errorMessage("That isn't a valid nation."))

                if (sender.toRealmsPlayer().nation == null) fail(errorMessage("You don't have a nation"))
                if (!sender.toRealmsPlayer().hasNationPermission("enemy"))
                    fail(errorMessage("You don't have permission to enemy other nations."))

                if (!sender.toRealmsPlayer().nation!!.enemies.contains(nation)) fail(errorMessage("We're aren't enemies!"))

                if (!sender.confirmation("Are you sure you want to un-enemy $name?")) return@executorPlayer

                sender.toRealmsPlayer().nation!!.enemyUUIDs.remove(nation.uuid)

                if (!nation.enemies.contains(sender.toRealmsPlayer().nation!!)) {
                    broadcast(
                        """
                    ${blackLine()}
                    §l§6PEACE§e ${sender.toRealmsPlayer().nation!!.name} has declared a state of neutrality between them and ${nation.name}.
                    ${blackLine()}
                """.trimIndent()
                    )
                } else {
                    broadcast(
                        """
                    ${blackLine()}
                    §l§6PEACE§e ${sender.toRealmsPlayer().nation!!.name} has proclaimed that they are no longer hostile to ${nation.name}, they however, still are.
                    ${blackLine()}
                """.trimIndent()
                    )
                }
            }}

            command("add") { executorPlayer {
                val name = args.getOrNull(0) ?: ""
                val nation = NationDatabase[name] ?: fail(errorMessage("That isn't a valid nation."))

                if (sender.toRealmsPlayer().nation == null) fail(errorMessage("You don't have a nation"))
                if (!sender.toRealmsPlayer().hasNationPermission("enemy"))
                    fail(errorMessage("You don't have permission to enemy other nations."))

                if (sender.toRealmsPlayer().nation!!.enemies.contains(nation)) fail(errorMessage("We're already enemies!"))

                if (!sender.confirmation("Are you sure you want to enemy $name?")) return@executorPlayer

                sender.toRealmsPlayer().nation!!.enemyUUIDs.add(nation.uuid)

                if (!nation.enemies.contains(sender.toRealmsPlayer().nation!!)) {
                    broadcast(
                        """
                    ${blackLine()}
                    §l§4NEW ENEMIES§c ${sender.toRealmsPlayer().nation!!.name} has declared ${nation.name} their enemy.
                    ${blackLine()}
                """.trimIndent()
                    )
                } else {
                    broadcast(
                        """
                    ${blackLine()}
                    §l§4NEW ENEMIES§c ${sender.toRealmsPlayer().nation!!.name} and ${nation.name} have declared themselves mutual enemies.
                    ${blackLine()}
                """.trimIndent()
                    )
                }
            }}
        }
    }

    private fun CommandDSL.nationInviteCommand() {
        command("invite") {
            aliases.add("add")

            executorPlayer {
                val nation = sender.toRealmsPlayer().nation ?: fail(errorMessage(nation_command_fail_no_nation))
                val town = getTown(args.getOrNull(0) ?: "")
                    ?: fail(TownCommandMessages.town_status_fail_invalid)

                if (town.nation != null) fail(errorMessage(nation_invite_fail_in_nation))

                if (!sender.toRealmsPlayer().hasNationPermission(invite)) fail(errorMessage(nation_invite_fail_no_perm))

                if (System.currentTimeMillis() - town.lastNationInvite < 300000)
                    fail(errorMessage("This town has been invited to a nation recently, wait a couple minutes before trying again."))

                val onlinePlayers = hashSetOf<RealmsPlayer>()
                for (pl in town.residents) if (pl.online && pl.hasTownPermission(nation_join)) onlinePlayers.add(pl)

                if (onlinePlayers.isEmpty()) fail(errorMessage("No one who can accept your invite is online."))

                town.lastNationInvite = System.currentTimeMillis()
                for (player in onlinePlayers) {

                    town.broadcast("${sender.name} has invited us to the Nation of ${nation.name}")
                    nation.broadcast("${sender.name} has invited ${town.name} to our nation.")
                    ConfirmationBuilder().apply {
                        setTitle("§5§lNATION INVITE§d Do you want our town to join §5${nation.name}")
                        runOnCancel {
                            for (pl in onlinePlayers) if (pl.hasConfirmation())
                                ConfirmationHandler.cancelConfirmation(pl.player)

                            nation.broadcast("The town of $name has denied our invite.")
                        }
                        runOnAccept {
                            for (pl in onlinePlayers) if (pl.hasConfirmation())
                                ConfirmationHandler.cancelConfirmation(pl.player)

                            TownJoinEvent(town, nation).callEvent()
                            nation.towns.add(town)
                            nation.broadcast(nation_invite_broadcast.format(town.name))
                        }
                        sendTo(player.player)
                    }
                }

            }
        }

        command("kick") {
            aliases.add("remove")

            executorPlayer {
                val nation = sender.toRealmsPlayer().nation ?: fail(errorMessage(nation_command_fail_no_nation))
                val town = getTown(args.getOrNull(0) ?: "")
                    ?: fail(TownCommandMessages.town_status_fail_invalid)

                if (town.nation != nation) fail(errorMessage(nation_kick_not_in_nation))

                if (!sender.toRealmsPlayer().hasNationPermission(kick)) fail(errorMessage(nation_kick_fail_no_perm))
                if (nation.capital == town) fail(errorMessage(nation_kick_capital))
                if (town == sender.toRealmsPlayer().town) fail(errorMessage(nation_kick_self))

                if (town.occupied) fail(errorMessage(nation_kick_occupied))

                if (sender.confirmation(nation_kick_confirmation.format(town.name))) {
                    TownLeaveEvent(town, nation).callEvent()
                    nation.towns.remove(town)
                    nation.broadcast(nation_kick_broadcast)
                }
            }
        }
    }

    private fun CommandDSL.nationSetupCommands() {
        command("new") {
            aliases.add("create")

            executorPlayer {
                val town = sender.toRealmsPlayer().town ?: fail(errorMessage(nation_command_fail_no_town))
                if (town.mayor != sender.toRealmsPlayer()) fail(errorMessage(nation_create_fail_mayor))
                if (town.nation != null) fail(errorMessage(nation_create_fail_nation))

                val name = args.getOrNull(0) ?: fail(errorMessage(nation_create_fail_name))
                if (name.length > 15) fail(errorMessage(nation_create_fail_long))
                if (name.contains('`')) fail(errorMessage("You aren't allowed to have backticks in town names."))

                if (NationDatabase[name] != null) fail(errorMessage(nation_create_fail_name_exists))

                if (!sender.confirmation(nation_create_confirmation)) return@executorPlayer

                val remainingItems = town.takeFromStockpileChest(Nation.nationPrice.toList())
                if (!remainingItems.first) {
                    sender.msg(errorMessage(nation_create_fail_poor))
                    sender.msg("§5You still need§d:")
                    for (item in remainingItems.second.toStringList()) sender.msg("§e-§6 $item")
                    return@executorPlayer
                }

                val nation = Nation(name =name, capital =town)
                NationDatabase.nations.add(nation)

                broadcast(
                    """
                     ${blackLine()}
                    §5§lNEW NATION §d${sender.name} has just founded the nation of §5$name§d in §5${town.name}§5!
                     ${blackLine()}
                    """.trimIndent()
                )
                nation.setupListener()
            }
        }

        command("claim") { executorPlayer   {
            val nation = sender.toRealmsPlayer().nation ?: fail(errorMessage(nation_command_fail_no_nation))

            if (!sender.toRealmsPlayer().hasNationPermission(claim))
                fail(errorMessage(nation_claim_fail_no_perm))
            if (NationDatabase[sender.location] != null) fail(errorMessage(nation_claim_fail_claimed))

            val claimResult = nation.claim(sender.location.chunk)
            when (claimResult.first) {
                ClaimResult.CLAIMED -> fail(successMessage(nation_claim_success))
                ClaimResult.TOP -> fail(errorMessage(nation_claim_fail_far))
                ClaimResult.NOT_CONTIGUOUS -> fail(errorMessage(nation_claim_fail_contiguous))
                ClaimResult.POOR_CLAIM -> sender.msg(errorMessage(nation_claim_fail_poor))
                else -> {}
            }

            if (claimResult.second.isNotEmpty()) {
                sender.msg("§5You still need§d:")
                for (itemString in claimResult.second.toStringList()) sender.msg("§d- $itemString")
            }
        }}

        command("unclaim") { executorPlayer {
            val nation = sender.toRealmsPlayer().nation ?: fail(errorMessage(nation_command_fail_no_nation))

            if (!sender.toRealmsPlayer().hasNationPermission(claim))
                fail(errorMessage(nation_claim_fail_no_perm))
            if (NationDatabase[sender.location] != nation) fail(errorMessage(nation_unclaim_fail_invalid))

            nation.unclaim(sender.location.chunk)
            // this doesn't really deserve a custom message thing
            nation.broadcast("${sender.name} has unclaimed §5${sender.location.chunk.x}§d, §5${sender.location.chunk.z}")
        }}
    }

    private fun CommandDSL.nationUtilCommands() {
        command("online") { executorPlayer {
            val arg = args.getOrNull(0) ?: ""
            val nation = NationDatabase[arg] ?: sender.toRealmsPlayer().nation ?: fail(errorMessage(nation_command_fail_no_nation))

            sender.msg("""
                    §5§lONLINE PLAYERS IN ${nation.name.uppercase()}
                    §5 [ §d${nation.residents.filter { it.online }.joinToString { it.name }}§5 ]
                """.trimIndent())
        }}

        command("delete") { executorPlayer {
            val nation = sender.toRealmsPlayer().nation ?: fail(errorMessage(nation_command_fail_no_nation))
            if (!sender.toRealmsPlayer().hasNationPermission(delete))
                fail(errorMessage(nation_delete_fail_no_perm))

            if (sender.confirmation(nation_delete_confirmation.format(nation.name))) {
                nation.broadcast(nation_delete_broadcast.format(sender.name))
                nation.delete()
            }
            NationDeleteEvent(nation).callEvent()
        }}

        command("leave") { executorPlayer {
            val nation = sender.toRealmsPlayer().nation ?: fail(errorMessage(nation_command_fail_no_nation))
            if (!sender.toRealmsPlayer().hasTownPermission(nation_leave))
                fail(errorMessage(nation_leave_fail_no_perm))

            if (sender.toRealmsPlayer().town!!.occupied)
                fail(errorMessage(nation_leave_fail_occupied))

            if (sender.toRealmsPlayer().town!! == nation.capital) fail(errorMessage(nation_kick_capital))

            if (sender.confirmation(nation_leave_confirmation)) {
                nation.broadcast(nation_leave_broadcast.format(sender.toRealmsPlayer().town!!.name))
                nation.towns.remove(sender.toRealmsPlayer().town!!)
            }
            TownLeaveEvent(sender.toRealmsPlayer().town!!, nation).callEvent()
        }}
    }

    private fun CommandDSL.nationListCommand() {
        command("list") { executorPlayer {
            sender.msg(blackLine())
            sender.msg("§5§lNATION RANKINGS")

            //todo pagination
            val nationlist = NationDatabase.nations.toList()
            Collections.sort(nationlist, Collections.reverseOrder())
            for ((n, nation) in nationlist.withIndex()) {
                if (n >= 10) break
                sender.msg(nationListTemplate(nation))
            }

            sender.msg(blackLine())
        }}
    }

    private fun CommandDSL.nationSetCommands() {
        command("set") {

            command("leader") { executorPlayer {
                val nation = sender.toRealmsPlayer().nation ?: fail(errorMessage(nation_command_fail_no_nation))
                if (nation.leader != sender.toRealmsPlayer())
                    fail(errorMessage(nation_set_leader_fail_leader))

                val name = args.getOrNull(0) ?: fail(errorMessage(nation_set_leader_fail_null))
                val player = RealmsPlayerDatabase[name] ?: fail(errorMessage(nation_set_leader_fail_invalid.format(name)))

                if (player.nation != nation) fail(errorMessage(nation_set_leader_fail_other_nation))

                if (!player.isMayor) fail(errorMessage(nation_set_leader_fail_mayor))
                if (player == nation.leader) fail(errorMessage("You're already the leader?"))

                if (sender.confirmation(nation_set_leader_confirmation)) {
                    nation.capital = player.town!!
                    nation.towns.add(sender.toRealmsPlayer().town!!)
                    nation.broadcast(nation_set_leader_broadcast.format(player.name))
                }
            }}


            command("perm") {

                command("nation-build") { executorPlayer {
                    val nation = sender.toRealmsPlayer().nation ?: fail(errorMessage(nation_command_fail_no_nation))

                    if (!sender.toRealmsPlayer().hasTownPermission(TownPermissions.set_perm))
                        fail(errorMessage(TownCommandMessages.town_set_perm_fail_no_perm))

                    val bool = (args.getOrNull(0)
                        ?: fail(errorMessage(TownCommandMessages.town_set_perm_fail_invalid))).toBooleanOrNull()
                        ?: fail(errorMessage(TownCommandMessages.town_set_perm_fail_invalid))

                    if (bool) {
                        nation.nationPerms[0] = true
                        nation.broadcast("Nation Build Permissions are now enabled.")
                    } else {
                        nation.nationPerms[0] = false
                        nation.broadcast("Nation Build Permissions are now disabled.")
                    }
                }}

                command("nation-destroy") { executorPlayer {

                    val nation = sender.toRealmsPlayer().nation ?: fail(errorMessage(nation_command_fail_no_nation))

                    if (!sender.toRealmsPlayer().hasTownPermission(TownPermissions.set_perm))
                        fail(errorMessage(TownCommandMessages.town_set_perm_fail_no_perm))

                    val bool = (args.getOrNull(0)
                        ?: fail(errorMessage(TownCommandMessages.town_set_perm_fail_invalid))).toBooleanOrNull()
                        ?: fail(errorMessage(TownCommandMessages.town_set_perm_fail_invalid))

                    if (bool) {
                        nation.nationPerms[1] = true
                        nation.broadcast("Nation Destroy Permissions are now enabled.")
                    } else {
                        nation.nationPerms[1] = false
                        nation.broadcast("Nation Destroy Permissions are now disabled.")
                    }
                }}

                command("nation-interact") { executorPlayer {

                    val nation = sender.toRealmsPlayer().nation ?: fail(errorMessage(nation_command_fail_no_nation))

                    if (!sender.toRealmsPlayer().hasTownPermission(TownPermissions.set_perm))
                        fail(errorMessage(TownCommandMessages.town_set_perm_fail_no_perm))

                    val bool = (args.getOrNull(0)
                        ?: fail(errorMessage(TownCommandMessages.town_set_perm_fail_invalid))).toBooleanOrNull()
                        ?: fail(errorMessage(TownCommandMessages.town_set_perm_fail_invalid))

                    if (bool) {
                        nation.nationPerms[2] = true
                        nation.broadcast("Nation Interact Permissions are now enabled.")
                    } else {
                        nation.nationPerms[2] = false
                        nation.broadcast("Nation Interact Permissions are now disabled.")
                    }
                }}


                command("outsider-build") { executorPlayer {

                    val nation = sender.toRealmsPlayer().nation ?: fail(errorMessage(nation_command_fail_no_nation))

                    if (!sender.toRealmsPlayer().hasTownPermission(TownPermissions.set_perm))
                        fail(errorMessage(TownCommandMessages.town_set_perm_fail_no_perm))

                    val bool = (args.getOrNull(0)
                        ?: fail(errorMessage(TownCommandMessages.town_set_perm_fail_invalid))).toBooleanOrNull()
                        ?: fail(errorMessage(TownCommandMessages.town_set_perm_fail_invalid))

                    if (bool) {
                        nation.outsiderPerms[0] = true
                        nation.broadcast("Outsider Build Permissions are now enabled.")
                    } else {
                        nation.outsiderPerms[0] = false
                        nation.broadcast("Outsider Build Permissions are now disabled.")
                    }
                }}

                command("outsider-destroy") { executorPlayer {

                    val nation = sender.toRealmsPlayer().nation ?: fail(errorMessage(nation_command_fail_no_nation))

                    if (!sender.toRealmsPlayer().hasTownPermission(TownPermissions.set_perm))
                        fail(errorMessage(TownCommandMessages.town_set_perm_fail_no_perm))

                    val bool = (args.getOrNull(0)
                        ?: fail(errorMessage(TownCommandMessages.town_set_perm_fail_invalid))).toBooleanOrNull()
                        ?: fail(errorMessage(TownCommandMessages.town_set_perm_fail_invalid))

                    if (bool) {
                        nation.outsiderPerms[1] = true
                        nation.broadcast("Outsider Destroy Permissions are now enabled.")
                    } else {
                        nation.outsiderPerms[1] = false
                        nation.broadcast("Outsider Destroy Permissions are now disabled.")
                    }
                }}

                command("outsider-interact") { executorPlayer {

                    val nation = sender.toRealmsPlayer().nation ?: fail(errorMessage(nation_command_fail_no_nation))

                    if (!sender.toRealmsPlayer().hasTownPermission(TownPermissions.set_perm))
                        fail(errorMessage(TownCommandMessages.town_set_perm_fail_no_perm))

                    val bool = (args.getOrNull(0)
                        ?: fail(errorMessage(TownCommandMessages.town_set_perm_fail_invalid))).toBooleanOrNull()
                        ?: fail(errorMessage(TownCommandMessages.town_set_perm_fail_invalid))

                    if (bool) {
                        nation.outsiderPerms[2] = true
                        nation.broadcast("Outsider Interact Permissions are now enabled.")
                    } else {
                        nation.outsiderPerms[2] = false
                        nation.broadcast("Outsider Interact Permissions are now disabled.")
                    }
                }}
            }
            
            command("color") { executorPlayer {
                val nation = sender.toRealmsPlayer().nation ?: fail(errorMessage(nation_command_fail_no_nation))
                if (!sender.toRealmsPlayer().hasNationPermission(NationPermission.color))
                    fail(errorMessage(NationUtils.nation_set_color_fail_no_perm))

                val hexString = args.getOrNull(0) ?: fail(errorMessage(NationUtils.nation_set_color_fail_invalid))
                try {

                    if (hexString.length < 7 || hexString.first() != '#') {
                        sender.msg(errorMessage(NationUtils.nation_set_color_fail_invalid))
                        sender.msg(errorMessage("This might be due to your hex color being more than 6 digits long."))
                        return@executorPlayer
                    }
                    nation.color = hexString
                    sender.msg(successMessage("Successfully set the color of the nation."))
                } catch (e: NumberFormatException) { fail(errorMessage(NationUtils.nation_set_color_fail_invalid)) }
            }}

            command("name") { executorPlayer {
                val newName = args.getOrNull(0) ?: fail(errorMessage(TownCommandMessages.town_set_name_fail_null))
                val nation = sender.toRealmsPlayer().nation ?: fail(errorMessage(nation_command_fail_no_nation))

                if (!sender.toRealmsPlayer().hasTownPermission(NationPermission.name))
                    fail(errorMessage(TownCommandMessages.town_set_name_fail_no_perm))

                if (name.length > 15) fail(errorMessage("Name is too long, maximum 15 characters."))

                nation.broadcast(nation_set_name_broadcast.format(newName))
                nation.name = newName
            }}
        }
    }

    private fun CommandDSL.nationPermCommands() {
        command("add-perm") { executorPlayer {
            val town = sender.toRealmsPlayer().nation ?: fail(errorMessage(nation_command_fail_no_nation))
            if (!sender.toRealmsPlayer().hasTownPermission(TownPermissions.edit_perm))
                fail(errorMessage(TownCommandMessages.town_permissions_fail_no_perm))

            val name = args.getOrNull(0) ?: fail(errorMessage(TownCommandMessages.town_command_fail_null_player))
            val player = RealmsPlayerDatabase[name] ?: fail(errorMessage(TownCommandMessages.town_command_fail_invalid_player.format(name)))
            if (player.nation != town) fail(errorMessage("That player isn't in the nation"))

            if (player == sender.toRealmsPlayer()) fail(errorMessage(TownCommandMessages.town_permissions_fail_self_1))
            if (player == town.leader) fail(errorMessage("The leader has all permissions"))

            val perm = args.getOrNull(1) ?: fail(errorMessage(TownCommandMessages.town_permissions_fail_null))
            if (player.hasTownPermission(perm)) fail(errorMessage(TownCommandMessages.town_permissions_fail_added))
            if (!perm.isValidTownPerm()) fail(errorMessage(TownCommandMessages.town_permissions_fail_invalid_1))

            if (perm == TownPermissions.edit_perm) fail(errorMessage(TownCommandMessages.town_permissions_fail_perm_1))

            if (!sender.toRealmsPlayer().hasTownPermission(perm))
                fail(errorMessage(TownCommandMessages.town_permissions_fail_self_no_perm_1))

            town.permissions[player] = town.permissions[player] ?: hashSetOf()
            town.permissions[player]!!.add(perm)

            sender.msg(successMessage(TownCommandMessages.town_permissions_success_1.format(player.name)))
        }}

        command("remove-perm") { executorPlayer {
            val town = sender.toRealmsPlayer().nation ?: fail(errorMessage(nation_command_fail_no_nation))
            if (!sender.toRealmsPlayer().hasTownPermission(TownPermissions.edit_perm))
                fail(errorMessage(TownCommandMessages.town_permissions_fail_no_perm))

            val name = args.getOrNull(0) ?: fail(errorMessage(TownCommandMessages.town_command_fail_null_player))
            val player = RealmsPlayerDatabase[name] ?: fail(errorMessage(TownCommandMessages.town_command_fail_invalid_player.format(name)))
            if (player.nation != town) fail(errorMessage("That player isn't in the nation"))

            if (player == sender.toRealmsPlayer()) fail(errorMessage(TownCommandMessages.town_permissions_fail_self_2))
            if (player == town.leader) fail(errorMessage("The leader has all permissions."))

            val perm = args.getOrNull(1) ?: fail(errorMessage(TownCommandMessages.town_permissions_fail_null))
            if (!player.hasTownPermission(perm)) fail(errorMessage(TownCommandMessages.town_permissions_fail_removed))
            if (!perm.isValidTownPerm()) fail(errorMessage(TownCommandMessages.town_permissions_fail_invalid_2))

            if (perm == TownPermissions.edit_perm && !sender.toRealmsPlayer().isMayor) fail(errorMessage(
                TownCommandMessages.town_permissions_fail_perm_2
            ))

            if (!sender.toRealmsPlayer().hasTownPermission(perm))
                fail(errorMessage(TownCommandMessages.town_permissions_fail_self_no_perm_2))

            town.permissions[player] = town.permissions[player] ?: hashSetOf()
            town.permissions[player]!!.remove(perm)

            sender.msg(successMessage(TownCommandMessages.town_permissions_success_2.format(player.name)))
        }}
    }
}
