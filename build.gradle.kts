// More about the setup here: https://github.com/DevSrSouza/KotlinBukkitAPI/wiki/Getting-Started
plugins {
    kotlin("jvm") version "1.4.30"
    kotlin("plugin.serialization") version "1.4.0"
    id("net.minecrell.plugin-yml.bukkit") version "0.3.0"
    id("com.github.johnrengelman.shadow") version "6.0.0"
}

group = "eu.hafixion"
version = "1.0.0"

repositories {
    jcenter()
    // minecraft
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://oss.sonatype.org/content/repositories/snapshots/")

    //kotlinbukkitapi with backup repo
    maven("http://nexus.devsrsouza.com.br/repository/maven-public/")
    
    //plugins
    maven("https://jitpack.io")
    maven("http://repo.citizensnpcs.co/")
    maven("https://nexus.savagelabs.net/repository/maven-releases/")
    maven("http://repo.mikeprimm.com/")
    maven("http://repo.extendedclip.com/content/repositories/placeholderapi/")

    // NBT Tag library
    maven("https://repo.codemc.org/repository/maven-public/")

    // CoreProtect
    maven("https://maven.playpro.com")
    maven("https://maven.enginehub.org/repo/")
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    compileOnly(kotlin("stdlib-jdk8"))

    //minecraft
    compileOnly("org.spigotmc:spigot-api:1.17-R0.1-SNAPSHOT")

    //kotlinbukkitapi
    val changing = Action<ExternalModuleDependency> { isChanging = true }
    compileOnly("br.com.devsrsouza.kotlinbukkitapi:core:0.2.0-SNAPSHOT", changing)
    compileOnly("br.com.devsrsouza.kotlinbukkitapi:serialization:0.2.0-SNAPSHOT", changing)
    compileOnly("br.com.devsrsouza.kotlinbukkitapi:plugins:0.2.0-SNAPSHOT", changing)
    compileOnly("br.com.devsrsouza.kotlinbukkitapi:exposed:0.2.0-SNAPSHOT", changing)

    // reflections lib
    implementation("org.reflections:reflections:0.9.9-RC1")

    //plugins
    val transitive = Action<ExternalModuleDependency> { isTransitive = false }
    compileOnly("com.github.MilkBowl:VaultAPI:1.7", transitive)
    compileOnly("me.clip:placeholderapi:2.9.2", transitive)
    implementation("us.dynmap:dynmap-api:2.5")


    // CoreProtect
    compileOnly("net.coreprotect:coreprotect:2.15.0")

    // Citizens
    compileOnly("net.citizensnpcs:citizensapi:2.0.27-SNAPSHOT", transitive)
    compileOnly("net.citizensnpcs:citizens:2.0.27-SNAPSHOT", transitive)
    compileOnly("com.denizenscript:denizen:1.1.9-SNAPSHOT")

    // Apache Commons
    implementation("commons-io:commons-io:2.8.0")
}

bukkit {
    main = "eu.hafixion.realms.RealmsCorePlugin"
    description = "The main core plugin of Realms"
    author = "Hafixion"
    website = ""
    apiVersion = "1.16"
    load = net.minecrell.pluginyml.bukkit.BukkitPluginDescription.PluginLoadOrder.POSTWORLD
    depend = listOf("CoreProtect", "KotlinBukkitAPI", "PlaceholderAPI")
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
        kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.time.ExperimentalTime,kotlin.ExperimentalStdlibApi"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
        kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.time.ExperimentalTime,kotlin.ExperimentalStdlibApi"
    }
    shadowJar {
        classifier = null
    }
}

configurations.all {
    resolutionStrategy.cacheChangingModulesFor(120, "seconds")
}
