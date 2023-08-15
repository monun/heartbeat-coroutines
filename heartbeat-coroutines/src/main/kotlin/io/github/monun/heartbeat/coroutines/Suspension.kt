package io.github.monun.heartbeat.coroutines

import kotlinx.coroutines.yield

/**
 * Coroutine의 지연 작업을 흐른 시간에 따라 유연하게 처리합니다.
 */
class Suspension {
    private val currentMillis: Long
        get() = System.nanoTime() / 1_000_000L

    private var delayMillis = currentMillis

    /**
     * 지연 누적 시간을 추가합니다.
     *
     * 누적된 시간이 미래일 경우 도래할 때까지 지연합니다.
     *
     * 누적된 시간이 과거일 경우 [yield]를 호출합니다.
     */
    suspend fun delay(timeMillis: Long) {
        val delayMillis = (delayMillis + timeMillis).also { delayMillis = it }
        val actualDelay = delayMillis - currentMillis

        if (actualDelay > 0) kotlinx.coroutines.delay(actualDelay)
        else yield()
    }

    /**
     * 지연 누적 시간을 현재 시간으로 설정합니다.
     */
    fun reset() {
        delayMillis = currentMillis
    }
}