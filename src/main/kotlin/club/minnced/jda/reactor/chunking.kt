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
import net.dv8tion.jda.api.utils.data.DataObject
import net.dv8tion.jda.internal.entities.GuildImpl
import net.dv8tion.jda.internal.requests.WebSocketCode
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.toFlux
import reactor.core.publisher.toMono
import java.time.Duration
import java.util.concurrent.TimeoutException

/**
 * Chunk the members of this guild.
 * If guild subscriptions are disabled, this will only work if raw events are enabled.
 *
 * **Using this with disabled guild subscriptions is experimental and might break in the future.**
 *
 * @param[timeout] Whether the chunking process should timeout automatically
 *
 * @return[Flux] Flux of members
 */
fun Guild.chunkMembers(timeout: Boolean = true): Flux<Member> {
    val guild = this as GuildImpl

    if (isLoaded)
        return memberCache.toFlux()
    else if (jda.isGuildSubscriptions)
        return Mono.fromFuture { retrieveMembers() }.flatMapMany { memberCache.toFlux() }

    //FIXME: Make this use the JDA interface instead of internals
    val request = DataObject.empty()
            .put("limit", 0)
            .put("query", "")
            .put("guild_id", getId())

    val packet = DataObject.empty()
            .put("op", WebSocketCode.MEMBER_CHUNK_REQUEST)
            .put("d", request)

    var publisher = jda.client.send(packet.toString()).toMono()
         .flatMapMany { jda.onRaw("GUILD_MEMBERS_CHUNK") }
         .map { it.payload }
         .filter { it.getUnsignedLong("guild_id") == guild.idLong }
    if (timeout) {
        publisher = publisher.timeout(Duration.ofSeconds(10))
                             .onErrorResume(TimeoutException::class.java) { Mono.empty() }
    }
    return publisher.takeUntil { it.getArray("members").length() < 1000 }
                    .flatMapIterable { payload ->
                        val members = payload.getArray("members")
                        List(members.length()) { index ->
                            val json = members.getObject(index)
                            jda.entityBuilder.createMember(guild, json)
                        }
                    }
}