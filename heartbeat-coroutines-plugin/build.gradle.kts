import org.gradle.configurationcache.extensions.capitalized

val projectCore = project(":${rootProject.name}")

dependencies {
    implementation(projectCore)
}

extra.apply {
    set("pluginName",  rootProject.name.split('-').joinToString("") { it.capitalize() })
    set("packageName", rootProject.name.replace("-", "."))
    set("kotlinVersion", libs.versions.kotlin)
    set("coroutinesVersion", libs.versions.coroutines)
    set("paperVersion", libs.versions.paper)
}

tasks {
    // generate plugin.yml
    processResources {
        filesMatching("*.yml") {
            expand(project.properties)
            expand(extra.properties)
        }
    }

    fun registerJar(
        classifier: String
    ) = register<Jar>("${classifier}Jar") {
        archiveBaseName.set(rootProject.name)
        archiveClassifier.set(classifier)

        from(sourceSets["main"].output)
    }.also { jar ->
        register<Copy>("test${classifier.capitalized()}Jar") {
            dependsOn(projectCore.tasks.named("publishAllPublicationsToServerRepository"))

            val prefix = project.name
            val plugins = rootProject.file(".server/plugins-$classifier")
            val update = File(plugins, "update")
            val regex = Regex("($prefix).*(.jar)")

            from(jar)
            into(if (plugins.listFiles { _, it -> it.matches(regex) }?.isNotEmpty() == true) update else plugins)

            doLast {
                update.mkdirs()
                File(update, "RELOAD").delete()
            }
        }
    }

    registerJar("clip")
}
