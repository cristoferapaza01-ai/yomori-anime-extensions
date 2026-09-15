pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "yomori-anime-extensions"

include(":lib-extractors")
project(":lib-extractors").projectDir = file("lib/extractors")

// Auto-include all extensions in src/
file("src").listFiles()?.filter { it.isDirectory }?.forEach { langDir ->
    langDir.listFiles()?.filter { it.isDirectory }?.forEach { extDir ->
        val projName = ":extensions:${langDir.name}:${extDir.name}"
        include(projName)
        project(projName).projectDir = extDir
    }
}
