package eu.hafixion.realms.utils

import br.com.devsrsouza.kotlinbukkitapi.extensions.event.callEvent
import br.com.devsrsouza.kotlinbukkitapi.extensions.text.msg
import br.com.devsrsouza.kotlinbukkitapi.flow.eventFlow
import eu.hafixion.realms.RealmsCorePlugin
import eu.hafixion.realms.toRealmsPlayer
import eu.hafixion.realms.towny.nations.Nation
import eu.hafixion.realms.towny.nations.NationDatabase
import eu.hafixion.realms.towny.nations.events.NationEnterEvent
import eu.hafixion.realms.towny.nations.events.NationLeaveEvent
import eu.hafixion.realms.towny.towns.Town
import eu.hafixion.realms.towny.towns.events.TownEntryEvent
import eu.hafixion.realms.towny.towns.events.TownLeaveEvent
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import net.md_5.bungee.api.ChatMessageType
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.player.AsyncPlayerChatEvent

fun errorMessage(msg: String) = "§4§lERROR §c$msg"

fun successMessage(msg: String) = "§2§lSUCCESS §a$msg"


fun blackLine() = "§8§m                                                                         §r"
fun CommandSender.sendLine() = msg("§8§m                                                                         §r")

fun Player.sendEnteringTown(town: Town, event: ChunkChangeEvent? = null) {
    if (event != null) TownEntryEvent(town, this, event).callEvent()
    sendActionBar("§eEntering §6§l${town.name}")
}

// For context, I've just left nation entering from towns here.
fun Player.sendLeavingTown(town: Town, event: ChunkChangeEvent? = null) {
    if (event != null) {
        TownLeaveEvent(town, this, event).callEvent()
        if (NationDatabase[event.to.asPos()] != null) sendEnteringNation(NationDatabase[event.to.asPos()]!!, event)
        else sendActionBar("§2§lWILDERNESS")
    }
}

fun Player.sendTownChange(townA: Town, townB: Town, event: ChunkChangeEvent? = null) {
    if (event != null)
        TownLeaveEvent(townA, this, event).callEvent()
    sendEnteringTown(townB, event)
}

fun Player.sendLeavingNation(nation: Nation, event: ChunkChangeEvent? = null) {
    if (event != null) NationLeaveEvent(nation, event).callEvent()
    sendActionBar("§2§lWILDERNESS")
}

fun Player.sendEnteringNation(nation: Nation, event: ChunkChangeEvent? = null) {
    if (event != null) NationEnterEvent(nation, event).callEvent()
    sendActionBar("§2§lWILDERNESS §dclaimed by §5§l${nation.name.uppercase()}")
}

fun Player.sendActionBar(msg: String) = spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent(msg))

suspend fun Player.confirmation(msg: String): Boolean {
    msg("§e$msg")
    msg("§aType yes into chat to confirm.")

    toRealmsPlayer().canTalk = false
    val event = withTimeoutOrNull(10000) {
        RealmsCorePlugin.instance.eventFlow<AsyncPlayerChatEvent>().filter { it.player.uniqueId == uniqueId }.first()
    }
    toRealmsPlayer().canTalk = true

    if (event == null) {
        msg(errorMessage("Confirmation Timed Out"))
        return false
    }

    if ("yes" in event.message) {
        event.isCancelled = true
        return true
    } else msg(errorMessage("Confirmation Rejected."))
    return false
}