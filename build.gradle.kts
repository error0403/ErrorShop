import io.papermc.hangarpublishplugin.model.Platforms
val ktVersion: String by project
val easylibVersion: String by project

plugins {
    id("io.papermc.hangar-publish-plugin") version "0.1.2"
    java
    id("io.github.goooler.shadow")
    id("com.xbaimiao.easylib")
    kotlin("jvm")
}

group = "com.pixelserver.errorshop"
version = "0.16"

easylib {
    env {
        mainClassName = "com.pixelserver.errorshop.ErrorShopPlugin"
        pluginName = "ErrorShop"
        kotlinVersion = ktVersion
    }
    version = easylibVersion
    relocate("com.xbaimiao.easylib", "${project.group}.easylib", false)
}

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://repo.codemc.io/repository/maven-releases/")
    maven("https://repo.codemc.io/repository/maven-snapshots/")
    maven("https://repo.momirealms.net/releases/")
    maven("https://r.irepo.space/maven/")

}

dependencies {
    implementation("com.mysql:mysql-connector-j:9.1.0")
    implementation("redis.clients:jedis:5.2.0")
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation(kotlin("stdlib-jdk8"))
    implementation("net.kyori:adventure-text-minimessage:4.17.0")
    implementation("net.kyori:adventure-text-serializer-legacy:4.17.0")
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")

//    compileOnly("org.spigotmc:spigot-api:1.18.2-R0.1-SNAPSHOT")
}

tasks {
    assemble {
        dependsOn(shadowJar)
    }
    compileJava {
        options.encoding = "UTF-8"
    }
    processResources {
        outputs.upToDateWhen { false }
    }
    shadowJar {
        dependencies {
            easylib.library.forEach {
                if (it.cloud) {
                    exclude(dependency(it.id))
                }
            }
            exclude(dependency("org.slf4j:"))
            exclude(dependency("org.jetbrains:annotations:"))
            exclude(dependency("com.google.code.gson:gson:"))
        }
        archiveClassifier.set("")
        easylib.relocate.forEach {
            relocate(it.pattern, it.replacement)
        }
        // Do not minimize: Kotlin runtime classes are required on clean Paper servers.
    }

}

hangarPublish {
    publications.register("plugin") {
        version.set(project.version as String)
        channel.set("Release")
        id.set("ErrorShop")
        apiKey.set(System.getenv("HANGAR_API_TOKEN"))
        pages.resourcePage(file("../drafts/ErrorShop-Canonical-Release-Wiki.md").readText())
        platforms {
            register(Platforms.PAPER) {
                jar.set(tasks.shadowJar.flatMap { it.archiveFile })
                platformVersions.set(listOf(
                    "1.20.6", "1.21", "1.21.1", "1.21.2", "1.21.3",
                    "1.21.4", "1.21.5", "1.21.6", "1.21.7", "1.21.8"
                ))
                dependencies {
                    url("Vault", "https://www.spigotmc.org/resources/vault.34315/") {
                        required.set(false)
                    }
                    url("PlayerPoints", "https://www.spigotmc.org/resources/playerpoints.80745/") {
                        required.set(false)
                    }
                }
            }
        }
    }
}

tasks.named("publishPluginPublicationToHangar") {
    dependsOn(tasks.shadowJar)
}
tasks.named("syncPluginPublicationMainResourcePagePageToHangar") {
    dependsOn(tasks.shadowJar)
}
