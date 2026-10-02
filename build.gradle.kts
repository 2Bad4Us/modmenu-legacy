plugins {
    idea
    java
    id("gg.essential.loom") version "1.3.12"
    id("dev.architectury.architectury-pack200") version "0.1.3"
}

group = "modmenu.forge"
version = "1.1.0"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(8))
}

loom {
    runConfigs {
        "client" {
            property("fml.coreMods.load", "")
            if (project.hasProperty("screenshots")) {
                val dir = project.property("screenshots").toString()
                property("modmenulegacy.screenshots", if (dir.isBlank() || dir == "true") project.projectDir.absolutePath else dir)
                programArgs("--width", "1600", "--height", "900")
                project.findProperty("shotSelect")?.let { property("modmenulegacy.select", it.toString()) }
            }
            isIdeConfigGenerated = true
        }
        remove(getByName("server"))
    }
    forge {
        pack200Provider.set(dev.architectury.pack200.java.Pack200Adapter())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    minecraft("com.mojang:minecraft:1.8.9")
    mappings("de.oceanlabs.mcp:mcp_stable:22-1.8.9")
    forge("net.minecraftforge:forge:1.8.9-11.15.1.2318-1.8.9")
}

tasks.withType(JavaCompile::class) {
    options.encoding = "UTF-8"
}

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("mcmod.info") {
        expand("version" to project.version)
    }
}

base {
    archivesName.set("modmenu-legacy-1.8.9")
}

tasks.jar {
    archiveClassifier.set("dev")
}

tasks.remapJar {
    inputFile.set(tasks.jar.flatMap { it.archiveFile })
    archiveClassifier.set("")
}

tasks.withType(JavaExec::class).configureEach {
    javaLauncher.set(javaToolchains.launcherFor(java.toolchain))
}
