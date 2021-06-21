package eu.hafixion.realms

import br.com.devsrsouza.kotlinbukkitapi.architecture.KotlinPlugin
import br.com.devsrsouza.kotlinbukkitapi.extensions.bukkit.broadcast
import br.com.devsrsouza.kotlinbukkitapi.extensions.server.pluginManager
import br.com.devsrsouza.kotlinbukkitapi.extensions.server.scheduler
import eu.hafixion.realms.RealmsCommands.realmsCommands
import eu.hafixion.realms.dynmap.DynmapManager
import eu.hafixion.realms.towny.GenericListener
import eu.hafixion.realms.towny.nations.Nation
import eu.hafixion.realms.towny.nations.NationDatabase
import eu.hafixion.realms.towny.nations.commands.NationCommand.nationCommand
import eu.hafixion.realms.towny.towns.Town
import eu.hafixion.realms.towny.towns.TownDatabase
import eu.hafixion.realms.towny.towns.commands.TownCommand.townCommand
import eu.hafixion.realms.utils.ChunkChangeEvent
import eu.hafixion.realms.utils.blackLine
import me.clip.placeholderapi.expansion.PlaceholderExpansion
import net.coreprotect.CoreProtect
import net.coreprotect.CoreProtectAPI
import net.md_5.bungee.api.ChatColor
import org.apache.commons.io.FileUtils
import org.bukkit.Bukkit
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import org.dynmap.DynmapAPI
import org.dynmap.markers.MarkerAPI
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.util.*
import kotlin.time.minutes


class RealmsCorePlugin : KotlinPlugin() {

    companion object {
        lateinit var instance: RealmsCorePlugin

        lateinit var coreProtect: CoreProtectAPI
        lateinit var dynmapAPI: DynmapAPI
        lateinit var markerAPI: MarkerAPI

        const val town_newbie_enabled = true
        lateinit var upkeepTask: BukkitTask
        lateinit var upkeepAlertTask: BukkitTask

        var lastUpkeep = System.currentTimeMillis()
        var upkeepEnabled = true
    }


    override fun onPluginEnable() {
        instance = this

        // Requirements
        setupCoreProtect()
        townCommand()
        nationCommand()
        realmsCommands()

        try {
            lastUpkeep = YamlConfiguration().apply { load("plugins/RealmsCore/data.yml") }.getLong("last-upkeep")
        } catch (e: Exception) {}

        // Debug!!!!
        RealmsPlayerDatabase.realmsPlayers.add(RealmsPlayer(UUID.fromString("0b560de4-9eb5-43b9-81b4-4edd505fa887")))

        // Listeners
        ChunkChangeEvent.setupListeners(this)
        GenericListener(this)

        reloadDatabase()

        RealmsExtension().register()

        scheduler.scheduleSyncRepeatingTask(this, { serializeData() }, 12000, 12000)

        setupDynmap()
        scheduler.scheduleSyncRepeatingTask(this, { setupDynmap() }, 3600, 3600)

        val delay = System.currentTimeMillis() - lastUpkeep
        val secondDelay = delay - 5.minutes.toLongMilliseconds()
        upkeepAlertTask = scheduler.runTaskTimer(this, Runnable { broadcast("""
            ${blackLine()}
            §4§lUPKEEP ALERT§c Reminder: Upkeep is going to be taken in 5 minutes!
        """.trimIndent()) }, secondDelay, 1728000)
        upkeepTask = scheduler.runTaskTimer(this, Runnable { upkeep() }, delay, 1728000)
    }

    fun upkeep() {
        if (upkeepEnabled) for (town in TownDatabase.towns.clone() as HashSet<Town>) if (!town.isNew()) if (!town.takeUpkeep().first) town.delete()
        if (upkeepEnabled) broadcast("""
             ${blackLine()}
             §4§lUPKEEP ALERT §cUpkeep has been collected from towns, towns that could not afford have been deleted.
             ${blackLine()}
         """.trimIndent())
        lastUpkeep = System.currentTimeMillis()
    }

    private fun setupDynmap() {
        dynmapAPI = pluginManager.getPlugin("dynmap") as DynmapAPI
        markerAPI = dynmapAPI.markerAPI
        markerAPI.markerSets.filter { it.markerSetID == "bnations" || it.markerSetID == "atowns" }.forEach {
            it.deleteMarkerSet()
        }

        val nationSet = markerAPI.createMarkerSet("bnations", "BNations", markerAPI.markerIcons, false)
        val townSet = markerAPI.createMarkerSet("atowns", "ATowns", markerAPI.markerIcons, false)

        val townManager = DynmapManager(townSet, markerAPI)
        townManager.updateTowns()

        val nationManager = DynmapManager(nationSet, markerAPI)
        nationManager.updateNations()
    }

    override fun onPluginDisable() {
        serializeData()

        YamlConfiguration().apply { set("last-upkeep", lastUpkeep) }.save("plugins/RealmsCore/data.yml")
    }

    fun setupCoreProtect() {
        val plugin = Bukkit.getServer().pluginManager.getPlugin("CoreProtect")

        // Check that CoreProtect is loaded
        if (plugin == null || plugin !is CoreProtect)  error("§cCoreProtect is not enabled.")

        // Check that the API is enabled
        val coreProtectAPI = plugin.api
        if (!coreProtectAPI.isEnabled) error("§cCoreProtect is not enabled.")

        // Check that a compatible version of the API is loaded
        coreProtect = if (coreProtectAPI.APIVersion() < 6) error("§cInvalid CoreProtect API version.") else coreProtectAPI
    }

    fun serializeData() {
        val nationPath = "plugins/RealmsCore/nations/"
        val townPath = "plugins/RealmsCore/towns/"
        val playersPath = "plugins/RealmsCore/players/"

        val currentTime = System.currentTimeMillis()

        try {
            FileUtils.moveDirectory(File(nationPath), File("plugins/RealmsCore/backups/$currentTime/nations"))
        } catch (e: Exception) {}

        try {
        FileUtils.moveDirectory(File(townPath), File("plugins/RealmsCore/backups/$currentTime/towns"))
        } catch (e: Exception) {}

        try {
        FileUtils.moveDirectory(File(playersPath), File("plugins/RealmsCore/backups/$currentTime/players"))
        } catch (e: Exception) {}


        File(nationPath).purgeDirectory()
        File(townPath).purgeDirectory()
        File(playersPath).purgeDirectory()

        NationDatabase.serialize().forEach { (name, data) ->
            val file = File(nationPath + name)
            YamlConfiguration().save(file)
            val writer = FileWriter(file)
            writer.append(data)
            writer.close()
        }

        TownDatabase.serialize().forEach { (name, data) ->
            val file = File(townPath + name)
            YamlConfiguration().save(file)
            val writer = FileWriter(file)
            writer.append(data)
            writer.close()
        }

        RealmsPlayerDatabase.serialize().forEach { (name, data) ->
            val file = File(playersPath + name)
            YamlConfiguration().save(file)
            val writer = FileWriter(file)
            writer.append(data)
            writer.close()
        }
    }

    fun File.purgeDirectory() {
        if (listFiles() != null) for (file in listFiles()) {
            if (file.isDirectory) file.purgeDirectory()
            file.delete()
        }
    }

    fun reloadDatabase() {
        val nationFile = File("plugins/RealmsCore/nations/")
        val townFile = File("plugins/RealmsCore/towns/")
        val playersFile = File("plugins/RealmsCore/players/")

        RealmsPlayerDatabase.realmsPlayers.clear()
        playersFile.listFiles()?.forEach {
            val reader = FileReader(it)
            val data = reader.readLines().joinToString("\n")
            reader.close()

            RealmsPlayerDatabase.realmsPlayers.add(RealmsPlayer.deserialize(data))
        }

        TownDatabase.towns.clear()
        townFile.listFiles()?.forEach {
            val reader = FileReader(it)
            val data = reader.readLines().joinToString("\n")
            reader.close()

            TownDatabase.towns.add(Town.deserialize(data))
        }

        NationDatabase.nations.clear()
        nationFile.listFiles()?.forEach {
            val reader = FileReader(it)
            val data = reader.readLines().joinToString("\n")
            reader.close()

            NationDatabase.nations.add(Nation.deserialize(data))
        }
    }


    internal class RealmsExtension : PlaceholderExpansion() {

        override fun getIdentifier() = "realms"

        override fun getAuthor() = "Hafixion"

        override fun getVersion() = "1.0.0"

        override fun persist() = true

        override fun canRegister() = true

        override fun onPlaceholderRequest(p: Player, params: String): String {

            return when (params) {
                "town" -> p.toRealmsPlayer().town?.name ?: "no town"
                "nation" -> p.toRealmsPlayer().nation?.name ?: "no nation"
                "tag" -> when {
                    p.toRealmsPlayer().town != null && p.toRealmsPlayer().nation != null ->
                        "${ChatColor.of(p.toRealmsPlayer().nation!!.color)}§l${p.toRealmsPlayer().nation!!.name.uppercase()}${ChatColor.of("#FFA500")},§l ${p.toRealmsPlayer().town!!.name.uppercase()} "
                    p.toRealmsPlayer().town != null -> "${ChatColor.of("#FFA500")}§l${p.toRealmsPlayer().town!!.name.uppercase()} "
                    else -> ""
                }
                    // NATION, TOWN
                else -> "haha papi go brr"
            }
        }
    }
}
