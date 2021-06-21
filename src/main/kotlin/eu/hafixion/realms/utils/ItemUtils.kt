package eu.hafixion.realms.utils

import org.bukkit.Material
import org.bukkit.inventory.ItemStack

fun List<ItemStack>.toStringList(): ArrayList<String> {
    val list = arrayListOf<String>()

    for (item in this) {
        val name = item.type.name.replace("_", " ").lowercase().capitalize()
        list.add("${item.amount}x $name")
    }

    return list
}

fun List<ItemStack>.toHashMap(): HashMap<Material, Int> {
    val list = hashMapOf<Material, Int>()

    for (item in this) if (item != null) {
        if (list.contains(item.type)) {
            list[item.type] = list[item.type]?.plus(item.amount)!!
        } else list[item.type] = item.amount
    }

    return list
}

fun HashMap<Material, Int>.materialToItemList(): ArrayList<ItemStack> {
    val list = arrayListOf<ItemStack>()

    this.forEach { list += ItemStack(it.key, it.value) }
    return list
}

fun HashMap<Material, Int>.removeMap(other: HashMap<Material, Int>) {
    for (entry in other) if (contains(entry.key)) {
        this[entry.key] = this[entry.key]?.minus(entry.value)!!

        if (this.containsKey(entry.key) && this[entry.key]!! <= 0.0) remove(entry.key)
    }
}


fun HashMap<Material, Int>.addMap(other: HashMap<Material, Int>) {
    for (entry in other) if (contains(entry.key)) {
        this[entry.key] = this[entry.key]?.plus(entry.value)!!
    } else this[entry.key] = entry.value
}

fun HashMap<Int, ItemStack>.intToItemList(): ArrayList<ItemStack> {
    val list = arrayListOf<ItemStack>()

    this.forEach { list += it.value }
    return list
}