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
@file:JvmName("Observers")
package club.minnced.jda.reactor

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.entities.channel.Channel
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.RawGatewayEvent
import net.dv8tion.jda.api.events.channel.update.GenericChannelUpdateEvent
import net.dv8tion.jda.api.events.guild.GenericGuildEvent
import net.dv8tion.jda.api.events.guild.member.GenericGuildMemberEvent
import net.dv8tion.jda.api.events.guild.update.GenericGuildUpdateEvent
import net.dv8tion.jda.api.events.message.GenericMessageEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.user.GenericUserEvent
import net.dv8tion.jda.api.hooks.EventListener
import net.dv8tion.jda.api.sharding.ShardManager
import reactor.core.publisher.Flux

// Guild events

inline fun <reified T : GenericGuildEvent> Guild.on() = on(T::class.java)

fun <T : GenericGuildEvent> Guild.on(type: Class<T>): Flux<T> {
    val id = this.idLong
    return jda.on(type).filter { it.guild.idLong == id }
}

inline fun <reified T : GenericGuildUpdateEvent<*>> Guild.onUpdate() = onUpdate(T::class.java)

fun <T : GenericGuildUpdateEvent<*>> Guild.onUpdate(type: Class<T>): Flux<T> {
    val id = this.idLong
    return jda.on(type).filter { it.guild.idLong == id }
}

 // User events

inline fun <reified T : GenericUserEvent> User.on() = on(T::class.java)

fun <T : GenericUserEvent> User.on(type: Class<T>): Flux<T> {
    val id = this.idLong
    return jda.on(type).filter { it.user.idLong == id }
}

inline fun <reified T : GenericGuildMemberEvent> Member.on() = on(T::class.java)

fun <T : GenericGuildMemberEvent> Member.on(type: Class<T>): Flux<T> {
    val guildId = this.guild.idLong
    val userId = this.idLong
    return guild.on(type).filter { it.guild.idLong == guildId } .filter { it.user.idLong == userId }
}

 // Channel Updates

inline fun <reified T : GenericChannelUpdateEvent<*>> Channel.onUpdate() = onUpdate(T::class.java)

fun <T : GenericChannelUpdateEvent<*>> Channel.onUpdate(type: Class<T>): Flux<T> {
    val id = this.idLong
    return jda.on(type).filter { it.channel.idLong == id }
}

 // Message events

inline fun <reified T : GenericMessageEvent> Message.on() = on(T::class.java)

fun <T : GenericMessageEvent> Message.on(type: Class<T>): Flux<T> {
    val id = this.idLong
    return jda.on(type).filter { it.messageIdLong == id }
}

fun MessageChannel.onMessage(): Flux<MessageReceivedEvent> {
    val id = this.idLong
    return jda.on(MessageReceivedEvent::class.java).filter { it.channel.idLong == id }
}

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
inline fun <reified T : GenericEvent> ShardManager.on(): Flux<T> = Flux.create { sink ->
    addEventListener(object : EventListener {
        override fun onEvent(event: GenericEvent) {
            if (event is T) {
                sink.next(event)
            }
        }
    })
}

inline fun <reified T : GenericEvent> ReactiveEventManager.on() = on(T::class.java)

/**
 * Filters a [RawGatewayEvent] by the provided type.
 *
 * Must be enabled by [JDABuilder#setRawEventsEnabled][net.dv8tion.jda.api.JDABuilder.setRawEventsEnabled]
 * otherwise this flux will never publish any events.
 *
 * @param types The types to filter
 *
 * @return [Flux] of [RawGatewayEvent] for the provided type
 */
fun JDA.onRaw(vararg types: String): Flux<RawGatewayEvent> = on(RawGatewayEvent::class.java).filter { it.type in types }