package eu.hafixion.realms.towny.towns.listeners

import org.bukkit.Material

object ListenerUtils {

    val itemUseMaterials = arrayOf(
        Material.MINECART,
        Material.COMMAND_BLOCK_MINECART,
        Material.FURNACE_MINECART,
        Material.CHEST_MINECART,
        Material.HOPPER_MINECART,
        Material.OAK_BOAT,
        Material.BIRCH_BOAT,
        Material.SPRUCE_BOAT,
        Material.ACACIA_BOAT,
        Material.DARK_OAK_BOAT,
        Material.JUNGLE_BOAT,
        Material.ENDER_PEARL,
        Material.CHORUS_FRUIT,
        Material.LEAD
    )

    val switchMaterials = arrayOf(
        Material.CHEST,
        Material.SHULKER_BOX,
        Material.WHITE_SHULKER_BOX,
        Material.ORANGE_SHULKER_BOX,
        Material.MAGENTA_SHULKER_BOX,
        Material.LIGHT_BLUE_SHULKER_BOX,
        Material.LIGHT_GRAY_SHULKER_BOX,
        Material.YELLOW_SHULKER_BOX,
        Material.LIME_SHULKER_BOX,
        Material.PINK_SHULKER_BOX,
        Material.GRAY_SHULKER_BOX,
        Material.CYAN_SHULKER_BOX,
        Material.PURPLE_SHULKER_BOX,
        Material.BLUE_SHULKER_BOX,
        Material.BROWN_SHULKER_BOX,
        Material.GREEN_SHULKER_BOX,
        Material.RED_SHULKER_BOX,
        Material.BLACK_SHULKER_BOX,
        Material.TRAPPED_CHEST,
        Material.FURNACE,
        Material.BLAST_FURNACE,
        Material.DISPENSER,
        Material.HOPPER,
        Material.DROPPER,
        Material.JUKEBOX,
        Material.SMOKER,
        Material.COMPOSTER,
        Material.BELL,
        Material.BARREL,
        Material.BREWING_STAND,
        Material.LEVER,
        Material.OAK_PRESSURE_PLATE,
        Material.BIRCH_PRESSURE_PLATE,
        Material.SPRUCE_PRESSURE_PLATE,
        Material.ACACIA_PRESSURE_PLATE,
        Material.DARK_OAK_PRESSURE_PLATE,
        Material.JUNGLE_PRESSURE_PLATE,
        Material.OAK_BUTTON,
        Material.BIRCH_BUTTON,
        Material.SPRUCE_BUTTON,
        Material.ACACIA_BUTTON,
        Material.DARK_OAK_BUTTON,
        Material.JUNGLE_BUTTON,
        Material.OAK_DOOR,
        Material.BIRCH_DOOR,
        Material.SPRUCE_DOOR,
        Material.ACACIA_DOOR,
        Material.DARK_OAK_DOOR,
        Material.JUNGLE_DOOR,
        Material.OAK_FENCE_GATE,
        Material.BIRCH_FENCE_GATE,
        Material.SPRUCE_FENCE_GATE,
        Material.ACACIA_FENCE_GATE,
        Material.DARK_OAK_FENCE_GATE,
        Material.JUNGLE_FENCE_GATE,
        Material.OAK_TRAPDOOR,
        Material.BIRCH_TRAPDOOR,
        Material.SPRUCE_TRAPDOOR,
        Material.ACACIA_TRAPDOOR,
        Material.DARK_OAK_TRAPDOOR,
        Material.JUNGLE_TRAPDOOR,
        Material.MINECART,
        Material.COMMAND_BLOCK_MINECART,
        Material.FURNACE_MINECART,
        Material.CHEST_MINECART,
        Material.HOPPER_MINECART,
        Material.LODESTONE,
        Material.RESPAWN_ANCHOR,
        Material.TARGET
    )

    //todo change these to materials
    val redstoneInteractibles = arrayOf("COMPARATOR", "REPEATER", "DAYLIGHT_DETECTOR", "NOTE_BLOCK", "REDSTONE_WIRE")

    val pottedPlants = arrayOf(
        "POTTED_ACACIA_SAPLING",
        "POTTED_ALLIUM",
        "POTTED_AZURE_BLUET",
        "POTTED_BAMBOO",
        "POTTED_BIRCH_SAPLING",
        "POTTED_BLUE_ORCHID",
        "POTTED_BROWN_MUSHROOM",
        "POTTED_CACTUS",
        "POTTED_CORNFLOWER",
        "POTTED_DANDELION",
        "POTTED_DARK_OAK_SAPLING",
        "POTTED_DEAD_BUSH",
        "POTTED_FERN",
        "POTTED_JUNGLE_SAPLING",
        "POTTED_LILY_OF_THE_VALLEY",
        "POTTED_OAK_SAPLING",
        "POTTED_ORANGE_TULIP",
        "POTTED_OXEYE_DAISY",
        "POTTED_PINK_TULIP",
        "POTTED_POPPY",
        "POTTED_RED_MUSHROOM",
        "POTTED_RED_TULIP",
        "POTTED_SPRUCE_SAPLING",
        "POTTED_WHITE_TULIP",
        "POTTED_WITHER_ROSE"
    )

    val dyes = arrayOf(
        "BLACK_DYE",
        "BLUE_DYE",
        "BROWN_DYE",
        "CYAN_DYE",
        "GRAY_DYE",
        "GREEN_DYE",
        "LIGHT_BLUE_DYE",
        "LIGHT_GRAY_DYE",
        "LIME_DYE",
        "MAGENTA_DYE",
        "ORANGE_DYE",
        "PINK_DYE",
        "PURPLE_DYE",
        "RED_DYE",
        "WHITE_DYE",
        "YELLOW_DYE"
    )

    const val not_at_war_with_town = "You aren't at war with this town."
    const val town_newbie = "This town currently has newbie protection."

    // Switch Messages
    const val switch_fail = "You aren't allowed to switch here"
    const val switch_ask_mayor_for_perm = "Ask your mayor for permission to switch here."

    // Destroy Messages
    const val destroy_fail = "You aren't allowed to destroy here."
    const val destroy_ask_mayor_for_perm = "Ask your mayor for permission to destroy here."

    // Interact Messages
    const val interact_fail = "You aren't allowed to interact with items here."
    const val interact_ask_mayor_for_perm = "Ask your mayor for permission to interact here."

    // Build Messages
    const val block_fully_protected = "This chunk is fully protected, you won't be able to break blocks here."
    const val build_fail_frostwalk = "You aren't allowed to build here (Frost Walker)"
    const val build_ask_mayor_for_perm = "Ask your mayor for permission to build here."
    const val build_fail = "You aren't allowed to build here."

    const val fire_disabled = "Fire is disabled here"
}