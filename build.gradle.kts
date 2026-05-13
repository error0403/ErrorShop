val ktVersion: String by project
val easylibVersion: String by project

plugins {
    java
    id("io.github.goooler.shadow")
    id("com.xbaimiao.easylib")
    kotlin("jvm")
}

group = "com.pixelserver.errorshop"
version = "0.12"

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
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation(kotlin("stdlib-jdk8"))
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
