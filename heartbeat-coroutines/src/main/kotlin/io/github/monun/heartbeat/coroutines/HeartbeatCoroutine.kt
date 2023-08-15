package io.github.monun.heartbeat.coroutines

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.server.PluginDisableEvent
import org.bukkit.plugin.Plugin

private object HeartbeatCoroutine {
    private val plugin: Plugin = Downstream.pullPlugin()

    private var session: HeartbeatSession? = null

    fun session(): HeartbeatSession {
        // double-checked locking
        if (session == null) {
            synchronized(this) {
                if (session == null) {
                    val plugin = plugin
                    require(plugin.isEnabled) { "Plugin attempted to register HeartbeatCoroutine while not enabled" }

                    val server = plugin.server

                    session = HeartbeatSession(plugin).also { activity ->
                        server.pluginManager.registerEvents(object : Listener {
                            @EventHandler(priority = EventPriority.LOWEST)
                            fun onPluginDisable(event: PluginDisableEvent) {
                                if(event.plugin == plugin)
                                    synchronized(this@HeartbeatCoroutine) {
                                        session = null
                                        activity.cancel()
                                    }
                            }
                        }, plugin)
                    }
                }
            }
        }

        return session.validate()
    }
}

private fun HeartbeatSession?.validate(): HeartbeatSession {
    requireNotNull(this) { "Failed to create HeartbeatCoroutine" }
    require(isValid) { "Invalid HeartbeatCoroutine session" }

    return this
}

/**
 * Bukkit의 mainHeartBeat에서 실행하는 [CoroutineDispatcher]를 가져옵니다.
 *
 * 라이브러리를 로드한 [Plugin]의 생명주기를 따릅니다.
 */
val Dispatchers.Heartbeat: CoroutineDispatcher
    get() = HeartbeatCoroutine.session().dispatcher

/**
 * [Dispatchers.Heartbeat]를 기본 [CoroutineDispatcher]로 가진 [CoroutineScope]를 생성합니다.
 *
 * 라이브러리를 로드한 [Plugin]의 생명주기를 따릅니다.
 */
@Suppress("FunctionName")
fun HeartbeatScope(): CoroutineScope = HeartbeatCoroutine.session().let { session ->
    CoroutineScope(session.dispatcher + Job(session.supervisorJob))
}