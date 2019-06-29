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
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Scheduler
import java.util.concurrent.CompletionStage

/**
 * Same as `Mono.justOrEmpty(it)`
 *
 * @return Mono for the current nullable value
 */
fun <T> T?.toMono(): Mono<T> = Mono.justOrEmpty(this)

/**
 * Lazy version of `Mono.then(Mono)`.
 *
 * @param[R] The result type
 * @param[callback] The callback to pass to [Mono.defer]
 *
 * @return Mono for the termination signal
 */
fun <R> Mono<*>.then(callback: () -> Mono<R>): Mono<R> = then(Mono.defer(callback))

/**
 * Lazy version of `Flux.then(Mono)`.
 *
 * @param[R] The result type
 * @param[callback] The callback to pass to [Mono.defer]
 *
 * @return Flux for the termination signal
 */
fun <R> Flux<*>.then(callback: () -> Mono<R>): Mono<R> = then(Mono.defer(callback))

/**
 * Converts the iterable of completion stages into a Flux of the result types.
 *
 * @param[T] The result type
 *
 * @return [Flux] for the result
 */
fun <T> Iterable<CompletionStage<out T>>.asFlux(): Flux<T> {
    return Flux.fromIterable(this)
               .flatMap { Mono.fromCompletionStage(it) }
}

/**
 * The scheduler for this JDA instance
 *
 * @throws[IllegalStateException] If this does not use [ReactiveEventManager].
 *
 * @return The scheduler
 */
val JDA.scheduler: Scheduler get() {
    val eventManager = this.eventManager as? ReactiveEventManager ?: throw IllegalStateException("You are not using a ReactiveEventManager!")
    return eventManager.scheduler
}

/**
 * Make this JDABuilder use a [ReactiveEventManager]
 *
 * @param[block] Initializer block
 *
 * @return The current JDABuilder for chaining
 */
inline fun JDABuilder.reactive(block: ReactiveEventManager.Builder.() -> Unit = {}) = setEventManager(createManager(block))

/**
 * Make this DefaultShardManagerBuilder use a [ReactiveEventManager]
 *
 * @param[block] Initializer block
 *
 * @return The current DefaultShardManagerBuilder for chaining
 */
fun DefaultShardManagerBuilder.reactive(block: ReactiveEventManager.Builder.() -> Unit = {}) = setEventManagerProvider { createManager(block) }

/**
 * Creates a new [ReactiveEventManager]
 *
 * @return The [ReactiveEventManager].
 */
inline fun createManager(block: ReactiveEventManager.Builder.() -> Unit = {}): ReactiveEventManager {
    return ReactiveEventManager.Builder().apply(block).build()
}