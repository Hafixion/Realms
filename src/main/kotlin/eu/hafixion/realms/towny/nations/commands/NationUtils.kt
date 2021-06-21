package eu.hafixion.realms.towny.nations.commands

import eu.hafixion.realms.towny.nations.Nation

object NationUtils {
    const val nation_command_fail_no_town = "You aren't in a town."
    const val nation_command_fail_no_nation = "You aren't in a nation."

    const val nation_create_confirmation = "Are you sure you want to create a nation?"
    const val nation_create_fail_nation = "You already have a nation."
    const val nation_create_fail_mayor = "You aren't the mayor."
    const val nation_create_fail_name = "You need to specify a name for your nation."
    const val nation_create_fail_poor = "Your town doesn't have enough items to afford a nation."
    const val nation_create_fail_long = "That name is too long, keep it under 15 characters."
    const val nation_create_fail_name_exists = "A nation already exists with that name."

    const val nation_delete_confirmation = "Are you sure you want to delete %s?"
    const val nation_delete_fail_no_perm = "You don't have permission to delete the nation."
    const val nation_delete_broadcast = "%s deleted the nation."

    const val nation_leave_fail_no_perm = "You don't have permission to make your town leave the nation."
    const val nation_leave_fail_occupied = "Your town is currently being occupied, you cannot leave as of now."
    const val nation_leave_broadcast = "%s has left the nation."
    const val nation_leave_confirmation = "Are you sure you want to leave your nation?"

    const val nation_invite_fail_in_nation = "That town is already in a nation."
    const val nation_invite_fail_no_perm = "You don't have permission to invite towns."
    const val nation_invite_broadcast = "Welcome %s to the nation!"

    const val nation_kick_confirmation = "Are you sure you want to kick %s?"
    const val nation_kick_fail_no_perm = "You don't have permission to kick towns."
    const val nation_kick_not_in_nation = "That town isn't in the nation."
    const val nation_kick_occupied = "That town is currently occupied, de-occupy them first before kicking them."
    const val nation_kick_capital = "That town is our capital!"
    const val nation_kick_self = "That's your town, you can't kick yourself!"
    const val nation_kick_broadcast = "%s has been kicked from that nation."

    const val nation_claim_fail_no_perm = "You don't have permission to claim."
    const val nation_claim_fail_claimed = "This land is already claimed by a nation or town."
    const val nation_claim_fail_far = "This chunk is too far from your nation's towns to be able to be claimed"
    const val nation_claim_fail_contiguous = "This chunk isn't connected to any previous claims."
    const val nation_claim_fail_poor = "The capital stockpile doesn't have enough resources to support more claims."
    const val nation_claim_success = "Successfully claimed this chunk to the nation."

    const val nation_unclaim_fail_invalid = "This chunk isn't claimed by our nation already."

    const val nation_set_leader_fail_leader = "GOSH! A coup d'état? Unacceptable."
    const val nation_set_leader_fail_null = "Specify a player."
    const val nation_set_leader_fail_invalid = "%s isn't an actual player."
    const val nation_set_leader_fail_other_nation = "%s isn't in our nation."
    const val nation_set_leader_fail_mayor = "%s isn't a mayor."
    const val nation_set_leader_confirmation = "Are you sure you want them to become the new leader of our nation? This will change the capital."
    const val nation_set_leader_broadcast = "%s is our new supreme leader."

    const val nation_set_name_broadcast = "Our nation name was changed to %s"

    const val nation_set_color_fail_no_perm = "You don't have permission to change the color."
    const val nation_set_color_fail_invalid = "That isn't a valid color."

    fun nationListTemplate(n: Nation) = "§d${n.name} - §5(${n.residents.size})"
}

object NationPermission {
    const val delete = "delete"
    const val invite = "invite"
    const val kick = "kick"
    const val claim = "claim"

    const val destroy = "break"
    const val build = "build"
    const val interact = "interact"
    const val name = "name"

    const val color = "color"
    const val enemy = "enemy"
}