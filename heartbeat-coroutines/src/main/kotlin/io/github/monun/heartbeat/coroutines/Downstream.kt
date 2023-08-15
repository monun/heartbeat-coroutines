package io.github.monun.heartbeat.coroutines

import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.java.PluginClassLoader


/**
 * Bukkit의 LibraryLoader로 로딩되었을 때 ClassLoader의 플러그인을 불러옵니다.
 */
internal object Downstream {
    private val classLoaderFields
        get() = PluginClassLoader::class.java.declaredFields.filter {
            ClassLoader::class.java.isAssignableFrom(it.type)
        }.onEach { field ->
            field.isAccessible = true
        }

    private val PluginClassLoader.internalLoaders: List<ClassLoader>
        get() = classLoaderFields.map { it.get(this) }.filterIsInstance<ClassLoader>()

    fun pullPlugin(): Plugin {
        val classLoader = Downstream::class.java.classLoader

        return Bukkit.getPluginManager().plugins.find { plugin ->
            val pluginClassLoader = plugin.javaClass.classLoader as PluginClassLoader?

            pluginClassLoader != null && (pluginClassLoader === classLoader || pluginClassLoader.internalLoaders.any { it === classLoader })
        } ?: error("heartbeat-coroutine must be loaded from PluginClassLoader")
    }
}