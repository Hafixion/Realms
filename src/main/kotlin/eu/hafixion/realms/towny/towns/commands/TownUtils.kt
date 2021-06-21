package eu.hafixion.realms.towny.towns.commands

import eu.hafixion.realms.towny.towns.Town
import eu.hafixion.realms.utils.BlockPos
import eu.hafixion.realms.utils.ChunkPos

object TownCommandMessages {
    const val town_command_fail_no_town = "You aren't in a town."
    const val town_command_fail_not_in_own_town = "You aren't inside your town."
    const val town_command_fail_not_in_town = "You aren't in a town."
    const val town_command_fail_new_town = "Your town does not have newbie protection."


    const val town_new_fail_null = "You need to specify a name for your town."


    const val town_command_fail_null_player =  "You need to specify the player's name."
    const val town_command_fail_invalid_player = "%s isn't a valid player."
    const val town_command_fail_town_player = "That player isn't in your town."


    const val town_status_fail_console = "You need to specify a town, or execute this command as a player."
    const val town_status_fail_invalid = "%s isn't a valid town."


    const val town_permissions_fail_no_perm = "You don't have permission to edit people's permissions."
    const val town_permissions_fail_null = "You need to specify a permission to give to that player."

    const val town_permissions_fail_added = "They already have that permission."
    const val town_permissions_fail_removed = "They don't have that permission."

    const val town_permissions_fail_mayor = "The Mayor has all permissions."
    const val town_permissions_fail_self_1 = "You can't give yourself extra permissions."
    const val town_permissions_fail_self_2 = "You can't remove your own permissions."
    const val town_permissions_fail_self_no_perm_1 = "You can't give others permissions you yourself don't have."
    const val town_permissions_fail_self_no_perm_2 = "You can't remove permissions from others you yourself don't have."

    const val town_permissions_fail_perm_1 = "You can't give others that permissions!"
    const val town_permissions_fail_perm_2 = "You can't remove that permissions from others!"

    const val town_permissions_fail_invalid_1 = "That isn't a valid permission to add."
    const val town_permissions_fail_invalid_2 = "That isn't a valid permission to remove."

    const val town_permissions_success_1 = "Successfully added that permission to %s"
    const val town_permissions_success_2 = "Successfully removed that permission from %s"


    const val town_invite_fail_no_perm = "You don't have permission to invite people."
    const val town_invite_fail_in_town = "That player is currently in a town."
    const val town_invite_fail_yourself = "You can't invite yourself dummy."
    const val town_kick_fail_no_perm = "You don't have permission to kick people."
    const val town_kick_fail_yourself = "You can't kick yourself dummy."
    const val town_kick_fail_in_town = "That player isn't currently in your town."

    const val town_toggle_fail_no_perm = "You don't have permission to toggle settings."
    const val town_toggle_fail_null = "You need to specify a setting to toggle."
    const val town_toggle_invalid_setting = "That's not a valid setting."

    const val town_toggle_fire_on = "Fire has been enabled."
    const val town_toggle_fire_off = "Fire has been disabled."

    const val town_toggle_mobs_on = "Mob spawning has been enabled."
    const val town_toggle_mobs_off = "Mob spawning been disabled."

    const val town_toggle_explosions_on = "Explosions have been enabled."
    const val town_toggle_explosions_off = "Explosions have been disabled."

    const val town_toggle_public_on = "The town is now public for outsiders to tp into."
    const val town_toggle_public_off = "The town is no longer public for outsiders to tp into."

    const val town_toggle_newbie_confirmation = "Are you sure you want to disabled newbie protection? Players will be able to break blocks and PVP will be forcibly enabled."

    const val town_toggle_pvp_on = "PVP is now enabled."
    const val town_toggle_pvp_off = "PVP is now disabled."


    const val town_claim_fail_no_perm = "You don't have permission to claim in your town."
    const val town_claim_fail_no_stockpile = "Your town doesn't have a set stockpile."

    const val town_claim_fail_top = "This chunk is already at the highest level it can be."
    const val town_claim_fail_claimed = "This chunk is already claimed by another town!"
    const val town_claim_fail_poor_claim = "Your stockpile chest doesn't have enough items to claim this chunk!"
    const val town_claim_fail_poor_upgrade = "Your stockpile chest doesn't have enough items to upgrade this chunk!"
    const val town_claim_fail_not_contiguous = "This chunk isn't linked to any other town chunks."

    const val town_unclaim_confirmation = "Are you sure you want to unclaim this chunk? Any disconnected claims will also be removed."
    const val town_unclaim_fail_homeblock = "Your homeblock is in this chunk."
    const val town_unclaim_fail_stockpile = "Your stockpile chest is in this chunk."
    const val town_unclaim_success_claim = "Successfully unclaimed this chunk."
    const val town_unclaim_success_upgrade = "Successfully downgraded this chunk."


    const val town_set_perm_fail_no_perm = "You don't have permission to change the town's permissions."
    const val town_set_perm_fail_invalid = "You need to specify a boolean."

    const val town_set_mayor_fail_resident = "You aren't the mayor silly, don't try to coup them."
    const val town_set_mayor_confirmation = "Are you sure you want to transfer your mayor-ship to that player?"

    const val town_set_stockpile_fail_no_perm = "You don't have permission to do that, ask your mayor to give you it."
    const val town_set_stockpile_fail_no_chest = "No chest around you was found!"

    const val town_set_home_fail_no_perm = "You don't have permission to change the town's homeblock."

    const val town_set_name_fail_no_perm = "You don't have permission to change our name."
    const val town_set_name_fail_null = "You need to specify a new name."


    const val town_leave_fail_mayor = "You're the mayor, you can't abandon your town like this!"
    const val town_leave_confirmation = "Are you sure you want to leave your town?"


    const val town_delete_fail_no_perm = "You don't have permission to delete the town."
    const val town_delete_confirmation = "Are you sure you want to delete your town?"


    const val town_home_fail_error = "You can't teleport to your town at this time."
    const val town_home_fail_other_town = "You can't teleport to that town at this time."
    const val town_home_fail_public = "That town isn't public."


    fun townRankingsTemplate(town: Town) = "§e${town.name} - §6(${town.residents.size})"
}

object TownBroadcastMessages {
    const val town_welcome_player = "Welcome %s to the town!"
    const val town_goodbye_player = "%s has left the town."
    const val town_kick_player = "%s has been kicked from the town."
    const val town_new_mayor = "%s is the new mayor!"
    const val town_new_name = "The town's name has been changed to %s"
    const val town_delete = "§c%s deleted the town."
    const val newbie_turned_off = "The town is no longer under newbie protection. Other players can now break blocks and PVP is enabled."

    fun townChangedHomeblock(pos: BlockPos) = "The town's homeblock has been changed to §6${pos.x} ${pos.y} ${pos.z}"
    fun townChangedStockpile(pos: BlockPos) = "Stockpile chest has been changed to ${pos.axis().joinToString()}"
    fun townClaimSuccessUpgrade(name: String, pos: ChunkPos) = "$name has upgraded the town's claims in §6${pos.x} ${pos.z}§e."
    fun townClaimSuccessClaim(name: String, pos: ChunkPos) = "$name has claimed §6${pos.x}, ${pos.z}§e for the town."

}

object TownPermissions {

    //todo .* permission
    const val edit_perm = "perm"
    const val invite = "invite"
    const val toggle = "toggle"
    const val claim = "claim"
    const val delete = "delete"
    const val kick = "kick"

    const val set_perm = "set.perm"
    const val set_name = "set.name"
    const val set_home = "set.home"
    const val set_stockpile = "set.stockpile"

    const val nation_leave = "leave"
    const val nation_join = "join"

    val all = setOf(
        edit_perm,
        invite,
        toggle,
        claim,
        delete,
        set_perm,
        set_name,
        set_home,
        set_stockpile,
        nation_leave,
        kick,
        nation_join,
        "*"
    )
}
