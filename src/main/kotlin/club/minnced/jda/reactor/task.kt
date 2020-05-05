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

import net.dv8tion.jda.api.utils.concurrent.Task
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.function.Function

/**
 * Converts this task to a [Mono]
 *
 * @return[Mono]
 */
fun <T> Task<T>.asMono(): Mono<T> = Mono.create { sink ->
    onSuccess(sink::success)
    onError(sink::error)
    sink.onCancel(this::cancel)
}

/**
 * Converts this task to a [Flux]
 *
 * @return[Flux]
 */
fun <T> Task<out Collection<T>>.asFlux(): Flux<T> = asMono().flatMapIterable(Function.identity())