plugins {
    idea
    kotlin("jvm") version libs.versions.kotlin
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

allprojects {
    repositories {
        mavenCentral()
        maven("https://papermc.io/repo/repository/maven-public/")
    }

    apply(plugin = "org.jetbrains.kotlin.jvm")

    dependencies {
        compileOnly(rootProject.project.libs.paper)

        api(kotlin("stdlib"))
        api(rootProject.project.libs.coroutines)
    }
}

idea {
    module {
        excludeDirs.add(file(".server"))
        excludeDirs.addAll(allprojects.map { it.buildDir })
        excludeDirs.addAll(allprojects.map { it.file(".gradle") })
    }
}