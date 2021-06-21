package eu.hafixion.realms

import org.apache.commons.io.FileUtils
import java.io.File
import java.io.FileReader
import java.io.FileWriter

/**
 * The main object that handles data persistence and saving, as well as querying.
 * All objects in it must implement [RealmsSerializable].
 *
 * For deep querying, remember that to decide your level, remove your original path from the entry and count the number of steps.
 *
 * @author Hafixion
 * @since 4/25/2021
 */
object RealmsDataSource {
    const val usingSql = false

    const val sqlHostname = "localhost"
    const val sqlPort = 3306
    const val sqlName = "realms"
    const val sqlTableName = "realms_"
    const val sqlUsername = "root"
    const val sqlPassword = ""

    val tables = hashMapOf<String, HashMap<String, RealmsSerializable>>()
    val players = arrayListOf<RealmsPlayer>()

    /**
     * Deep Querying, gets all data entries inside specified directory.
     *
     * @param path The path that is going to be queried
     * @param levels the amount of levels to query, lower than 0 will result in all.
     *
     * @return A list of all entries in the path.
     */
    operator fun get(path: String, levels: Int = 0): HashMap<String, RealmsSerializable> {
        when {
            levels < 0 -> {
                val keys = arrayListOf<String>().apply { addAll(tables.keys) }
                keys.removeIf { !it.startsWith(path) }

                return hashMapOf<String, RealmsSerializable>().apply {
                    tables.filter { keys.contains(it.key) }.forEach { putAll(it.value) }
                }
            }
            levels == 0 -> return if (tables[path] != null) tables[path]!! else hashMapOf()
            else -> {
                val keys = arrayListOf<String>().apply { addAll(tables.keys) }
                keys.removeIf { !it.startsWith(path) }

                // If the string is deep, and it has more than a few directories it removes the ones that are too deep.
                keys.removeIf {
                    if (it != path) it.replace("$path.", "").split(".").size > levels
                }

                return hashMapOf<String, RealmsSerializable>().apply {
                    tables.filter { keys.contains(it.key) }.forEach { putAll(it.value) }
                }
            }
        }
    }

    /**
     * Adds a [RealmsSerializable] to the specified path.
     *
     * @param path The path that the [RealmsSerializable] is getting added to
     * @param name The name of the object that is going to be assigned to it.
     * @param serial The object that is getting assigned to the path
     */
    fun add(path: String, name: String, serial: RealmsSerializable) {
        if (tables[path] == null) tables[path] = hashMapOf(name to serial) else tables[path]!![name] = serial
    }


    /**
     * Removes a [RealmsSerializable] from the specified path.
     *
     * @param path The path that the [RealmsSerializable] is getting removed from
     * @param serial The object that is getting removed from the path.
     */
    fun remove(path: String, serial: RealmsSerializable) {
        if (tables[path] == null) tables[path] = hashMapOf() else tables[path]!!.values.remove(serial)
    }

    /**
     * Removes a [RealmsSerializable] with the specified name in the specified path.
     *
     * @param path The path that the [RealmsSerializable] is getting removed from
     * @param name The name of the object that is going to be removed.
     */
    fun remove(path: String, name: String) {
        if (tables[path] == null) tables[path] = hashMapOf() else tables[path]!!.remove(name)
    }

    /**
     * Removes a [RealmsSerializable] with the specified name in the specified path, if it has the specified value.
     *
     * @param path The path that the [RealmsSerializable] is getting removed from
     * @param name The name of the object that is going to be removed.
     * @param serial The object that is getting removed from the path.
     */
    fun remove(path: String, name: String, serial: RealmsSerializable) {
        if (tables[path] == null) tables[path] = hashMapOf() else tables[path]!!.remove(name, serial)
    }

    /**
     * Sets a path directly instead of adding or removing elements.
     *
     * @param path The path that is getting assigned.
     * @param serial The arraylist of objects that is being assigned to the path.
     */
    operator fun set(path: String, serial: HashMap<String, RealmsSerializable>) {
        tables[path] = serial
    }

    fun save(folder: File) {
        val path = folder.toPath().toString()
        var number = 0
        for (entry in tables) for (entry1 in entry.value) {
            val file = File("$path/${entry.key.replace(':', '/')}/${entry1.key}.txt")
            val fileWriter = FileWriter(file)
            fileWriter.append(entry1.value.serialize())
            fileWriter.close()

            number++
        }
    }

    fun load(folder: File) {
        val files = FileUtils.listFiles(folder, arrayOf(".txt"), true)

        for (file in files) {
            var path = file.path
            path = path.replace("${folder.path}/", "")
            path = path.replace("/${file.name}", "")

            val reader = FileReader(file)
            val hashMap = (tables[path] ?: hashMapOf()).clone() as HashMap<String, RealmsSerializable>
            hashMap[file.name.replace(".txt", "")] = StringSerializable(reader.readText())
            reader.close()

            tables[path.replace('/', '.')] = hashMap
        }
    }
}

fun dataSource() = RealmsDataSource