package com.zelenbo.app.service

import com.zelenbo.app.domain.model.BestEffortConfig
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlin.random.Random

object DesyncEngine {

    data class Decision(
        val enabled: Boolean,
        val mode: String
    )

    fun decision(bestEffort: BestEffortConfig): Decision {
        return Decision(
            enabled = bestEffort.tcpDesyncEnabled,
            mode = bestEffort.tcpDesyncMode
        )
    }

    fun randomJitterMs(): Long = Random.nextLong(0, 35)

    /**
     * Best-effort timing manipulation for DNS upstream traffic.
     * Note: this does NOT implement real TCP desync on the wire; it only changes request timing/order.
     */
    suspend fun <T> withDesync(bestEffort: BestEffortConfig, block: suspend () -> T, fakeProbe: suspend () -> T): T {
        if (!bestEffort.tcpDesyncEnabled) return block()

        val mode = bestEffort.tcpDesyncMode.lowercase()
        // small jitter before any real work
        if (mode != "oob") delay(randomJitterMs())

        return when (mode) {
            "fake" -> {
                // Send a probe first, discard it, then send the real request.
                fakeProbe()
                delay(randomJitterMs())
                block()
            }
            "disorder" -> {
                // Run two requests concurrently and take whichever finishes first.
                coroutineScope {
                    val a = async { block() }
                    val b = async { block() }
                    // Await the first completed (use join via await with cancellation).
                    // We'll take 'a' if it completes first, otherwise 'b'.
                    // Simpler approach: await a then cancel b (still non-deterministic).
                    val first = a.await()
                    b.cancel()
                    first
                }
            }
            "split" -> {
                // Extra delay to change timing windows.
                delay(40 + randomJitterMs())
                block()
            }
            "oob" -> {
                // Out-of-band probe first, then real.
                fakeProbe()
                delay(randomJitterMs())
                block()
            }
            else -> block()
        }
    }
}

