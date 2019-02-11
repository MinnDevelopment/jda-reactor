/*
 * Copyright 2019  Florian Spieß and the contributors of jda-reactor
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package club.minnced.jda.reactor

import net.dv8tion.jda.api.requests.RestAction
import net.dv8tion.jda.api.requests.restaction.pagination.PaginationAction
import reactor.core.publisher.Flux
import reactor.core.publisher.FluxSink
import reactor.core.publisher.Mono
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

fun <T> RestAction<T>.asMono() = Mono.fromFuture(this::submit)

fun <T, M> PaginationAction<T, M>.asFlux(overflowStrategy: FluxSink.OverflowStrategy = FluxSink.OverflowStrategy.LATEST): Flux<T>
    where M : PaginationAction<T, M> = Flux.create<T>({ sink ->
    cache(false)
    var task = CompletableFuture.completedFuture<Void>(null)
    val remaining = AtomicLong(0L)
    val lock = ReentrantLock()
    var cancelled = false

    sink.onCancel { cancelled = true }

    sink.onRequest { amount ->
        if (amount <= 0) return@onRequest

        lock.withLock {
            if (remaining.get() >= amount) return@withLock

            var counter = amount - remaining.get()
            remaining.addAndGet(counter)
            task = task.thenCompose {
                forEachRemainingAsync {
                    sink.next(it)
                    lock.withLock { remaining.decrementAndGet() }
                    !cancelled && (counter == Long.MAX_VALUE || --counter > 0)
                }.exceptionally {
                    sink.error(it)
                    null
                }.thenRun {
                    if (counter > 0)
                        sink.complete()
                }
            }
        }
    }
}, overflowStrategy)