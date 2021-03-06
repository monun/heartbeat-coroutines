package io.github.monun.heartbeat.coroutines

import kotlinx.coroutines.*
import org.bukkit.plugin.Plugin
import kotlin.coroutines.CoroutineContext

internal class HeartbeatSession(
    val plugin: Plugin
) {
    val dispatcher: CoroutineDispatcher = HeartbeatDispatcher(this)

    val supervisorJob = SupervisorJob()

    var isValid = true
        private set

    fun cancel() {
        if (!isValid) return

        isValid = false
        supervisorJob.cancel()
    }
}

internal class HeartbeatDispatcher(
    private val session: HeartbeatSession
) : CoroutineDispatcher() {
    override fun dispatch(context: CoroutineContext, block: Runnable) {
        val session = session; if (!session.isValid) return

        val plugin = session.plugin
        val server = plugin.server

        if (server.isPrimaryThread) block.run()
        else server.scheduler.runTask(plugin, block)
    }
}