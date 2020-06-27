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

import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import reactor.core.publisher.Flux

/**
 * Load and stream the members of this guild.
 *
 * This required [GatewayIntent.GUILD_MEMBERS][net.dv8tion.jda.api.requests.GatewayIntent#GUILD_MEMBERS] to be enabled.
 *
 * @throws[IllegalStateException] If the GUILD_MEMBERS intent is not enabled
 *
 * @return[Flux] Flux of members
 */
fun Guild.streamMembers(): Flux<Member> = Flux.create { sink ->
    val task = loadMembers {
        sink.next(it)
    }

    sink.onCancel(task::cancel)
    task.onError(sink::error)
    task.onSuccess { sink.complete() }
}