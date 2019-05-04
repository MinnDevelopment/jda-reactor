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

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.requests.RestAction
import net.dv8tion.jda.api.requests.restaction.pagination.PaginationAction
import reactor.core.publisher.Flux
import reactor.core.publisher.FluxSink
import reactor.core.publisher.Mono
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

fun <T : GenericEvent> JDA.on(type: Class<T>) : Flux<T> {
    val manager = eventManager as? ReactiveEventManager ?: throw IllegalStateException("You are not using a ReactiveEventManager!")
    return manager.on(type)
}

/**
 * Constructs an event flow using a [Flux] of the specified type.
 *
 * # Example
 *
 * ```
 * jda.on<MessageReceivedEvent>()                // Flux<MessageReceivedEvent>
 *    .map { it.message }                        // Flux<Message>
 *    .filter { it.author.asTag == "Minn#6688" } // Flux<Message>
 *    .subscribe { println("Minn#6688 said ${it.contentDisplay}") }
 * ```
 */
inline fun <reified T : GenericEvent> JDA.on() = on(T::class.java)
inline fun <reified T : GenericEvent> ReactiveEventManager.on() = on(T::class.java)

/**
 * Converts the RestAction into a [Mono] of the same result type.
 *
 * - If the result type is a list you can use [toFlux] instead.
 * - If the type of RestAction is a PaginationAction you can use [asFlux] instead.
 *
 * This is a shortcut for `Mono.fromFuture(action.submit())`.
 *
 * # Example
 *
 * ```
 * channel.sendMessage("This message will self-destruct in 5 seconds) // RestAction<Message>
 *        .asMono()                                                   // Mono<Message>
 *        .delay(Duration.ofSeconds(5))                               // Mono<Message>
 *        .flatMap { it.delete().asMono() }                           // Mono<Void>
 *        .subscribe()
 * ```
 */
fun <T> RestAction<T>.asMono() = Mono.fromFuture(this::submit)!!

/**
 * Maps the response of this RestAction into a Flux.
 *
 * If this is a [PaginationAction] use [asFlux] instead.
 *
 * This is a shortcut for `action.asMono().flatMapIterable{it}`.
 *
 * # Example
 *
 * ```
 * guild.retrieveBanList()     // RestAction<List<Guild.Ban>>
 *      .toFlux()              // Flux<Ban>
 *      .map { it.user }       // Flux<User>
 *      .map { it.asTag }      // Flux<String>
 *      .subscribe {
 *          // Print the DiscordTag (Example: Minn#6688)
 *          println(it)
 *      }
 * ````
 */
fun <T> RestAction<List<T>>.toFlux() = asMono().flatMapIterable { it }!!

/**
 * Converts a PaginationAction into a streamed flux of data.
 * Unlike [toFlux] this will make multiple requests (as needed) in order to satisfy
 * the requested resources. [toFlux] makes a single request and streams the result into a Flux publisher
 * while this will paginate the underlying endpoint until it satisfied the demand or reaches an end.
 *
 * # Example
 *
 * ```
 * guild.retrieveAuditLogs()  // AuditLogPaginationAction : PaginationAction<AuditLogEntry, *>
 *      .type(ActionType.BAN) // AuditLogPaginationAction : PaginationAction<AuditLogEntry, *>
 *      .asFlux()                                                // Flux<AuditLogEntry>
 *      .take(5)                                                 // Flux<AuditLogEntry>
 *      .map { "${it.user} banned user with id ${it.targetId}" } // Flux<String>
 *      .subscribe { println(it) }
 * ```
 *
 * @param overflowStrategy
 *        The OverflowStrategy to apply (default [LATEST][FluxSink.OverflowStrategy.LATEST])
 */
fun <T, M> PaginationAction<T, M>.asFlux(overflowStrategy: FluxSink.OverflowStrategy = FluxSink.OverflowStrategy.LATEST) : Flux<T>
    where M : PaginationAction<T, M> = Flux.create<T>({ sink ->
    cache(false)
    var task: CompletionStage<*> = CompletableFuture.completedFuture(null)
    val remaining = AtomicLong(0)
    var done = false
    val lock = ReentrantLock()

    sink.onRequest { amount ->
        lock.withLock {
            if (amount <= remaining.get() || done)
                return@onRequest
            if (amount == Long.MAX_VALUE)
                remaining.set(Long.MAX_VALUE)
            else
                remaining.addAndGet(amount)

            task = task.thenCompose {
                when {
                    done || remaining.get() <= 0 -> CompletableFuture.completedFuture<Void>(null)

                    sink.isCancelled -> {
                        done = true
                        sink.complete()
                        CompletableFuture.completedFuture<Void>(null)
                    }

                    else -> forEachRemainingAsync {
                        sink.next(it)
                        !sink.isCancelled && remaining.decrementAndGet() > 0
                    }.thenRun {
                        if (remaining.get() > 0) lock.withLock {
                            done = true
                            sink.complete()
                        }
                    }
                }
            }
        }
    }
}, overflowStrategy)