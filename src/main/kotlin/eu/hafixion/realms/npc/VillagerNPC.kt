package eu.hafixion.realms.npc

import eu.hafixion.realms.towny.towns.Town
import net.citizensnpcs.api.trait.Trait

class VillagerNPC {

}

class VillagerNpcTrait(val town: Town, val depositTime: Int = 10000) : Trait("realms_villager") {

    override fun run() {
        //TODO In a list, what I need to get working is stockpile deposits and inventories
        //TODO then, I can focus on making professions useful, like armorers and toolsmiths
        //TODO then, I can focus on miners, lumberjacks, archers, and soldiers.
    }
}