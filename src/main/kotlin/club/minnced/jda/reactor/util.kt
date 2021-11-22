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
@file:JvmName("Utils")
package club.minnced.jda.reactor

import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder
import org.reactivestreams.Publisher
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks
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
 * @return Mono for the termination signal
 */
fun <R> Flux<*>.then(callback: () -> Mono<R>): Mono<R> = then(Mono.defer(callback))

/**
 * Combination of [Flux.filter] and [Flux.next].
 * Shortcut for `flux.filter(predicate).next()`.
 *
 * @param[predicate] The filter function
 *
 * @return `Mono<T>` for the matched result
 */
fun <T> Flux<T>.filterFirst(predicate: (T) -> Boolean): Mono<T> = filter(predicate).next()

/**
 * Similar to [filterFirst] but accepts a publisher for the result.
 * Shortcut for `flux.filterWhen(predicate).next()`
 *
 * @param[predicate] The filter function
 *
 * @return `Mono<T>` for the matched result
 */
fun <T> Flux<T>.filterFirstWhen(predicate: (T) -> Publisher<Boolean>): Mono<T> = filterWhen(predicate).next()

/**
 * Combination of [Flux.next] and [Mono.flatMap].
 * Shortcut for `flux.next().flatMap(func)`
 *
 * @param[func] The transformation function
 *
 * @return `Mono<R>` for the result
 */
fun <T, R> Flux<T>.nextWhen(func: (T) -> Mono<R>): Mono<R> = next().flatMap(func)

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
 * Make this JDABuilder use a [ReactiveEventManager]
 *
 * @param[block] Initializer block
 *
 * @return The current JDABuilder for chaining
 */
inline fun JDABuilder.reactive(sink: Sinks.Many<GenericEvent>? = null, block: ReactiveEventManager.() -> Unit = {}) = setEventManager(createManager(sink, block))

/**
 * Make this DefaultShardManagerBuilder use a [ReactiveEventManager]
 *
 * @param[block] Initializer block
 *
 * @return The current DefaultShardManagerBuilder for chaining
 */
fun DefaultShardManagerBuilder.reactive(sink: Sinks.Many<GenericEvent>? = null, block: ReactiveEventManager.() -> Unit = {}) = setEventManagerProvider { createManager(sink, block) }

/**
 * Creates a new [ReactiveEventManager]
 *
 * @return The [ReactiveEventManager].
 */
inline fun createManager(sink: Sinks.Many<GenericEvent>? = null, block: ReactiveEventManager.() -> Unit = {}): ReactiveEventManager {
    val manager = if (sink != null) ReactiveEventManager(sink)
                  else ReactiveEventManager()
    return manager.apply(block)
}