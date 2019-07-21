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
@file:JvmName("CacheViews")
package club.minnced.jda.reactor

import net.dv8tion.jda.api.utils.cache.CacheView
import reactor.core.publisher.Flux
import reactor.core.publisher.toFlux

/**
 * Creates a Flux using [CacheView.lockedIterator] and closes it after usage.
 *
 * @return Lazy flux that will apply a read-lock on the cache
 */
fun <T : Any> CacheView<T>.toFluxLocked(): Flux<T> = Flux.defer {
    // Use defer to make this lazy, otherwise it will lock this right away before subscribe() is called!
    val iterator = lockedIterator()
    iterator.toFlux().doFinally { iterator.close() }
}
