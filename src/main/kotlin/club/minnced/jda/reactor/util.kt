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

import reactor.core.publisher.Mono

/** Same as `Mono.justOrEmpty(it)` */
fun <T> T?.toMono(): Mono<T> = Mono.justOrEmpty(this)

/** Creates a new [ReactiveEventManager] */
inline fun createManager(block: ReactiveEventManager.Builder.() -> Unit = {}): ReactiveEventManager {
    return ReactiveEventManager.Builder().apply(block).build()
}