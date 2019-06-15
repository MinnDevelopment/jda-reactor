[ ![version](https://api.bintray.com/packages/minndevelopment/maven/jda-reactor/images/download.svg) ](https://bintray.com/minndevelopment/maven/jda-reactor/_latestVersion)
# jda-reactor

A collection of kotlin extensions for JDA that make use with reactor-core easier.

## Installation

Replace the `$VERSION` with the latest release version.
<br>Replace `$JDA_VERSION` with the latest stable JDA v4 release.

### Gradle

```gradle
dependencies {
    implementation("net.dv8tion:JDA:$JDA_VERSION")
    implementation("club.minnced:jda-reactor:$VERSION")
}

repositories {
    jcenter()
}
```

### Maven

```xml
<dependency>
    <groupId>net.dv8tion</groupId>
    <artifactId>JDA</artifactId>
    <version>$JDA_VERSION</version>
</dependency>
<dependency>
    <groupId>club.minnced</groupId>
    <artifactId>jda-reactor</artifactId>
    <version>$VERSION</version>
</dependency>
```

```xml
<repository>
    <name>jcenter</name>
    <id>jcenter-bintray</id>
    <url>https://jcenter.bintray.com</url>
</repository>
```

## Examples

Some small example usages of the components supported by this library.

There is a complete bot written as an example available at [reactive-jda-bot](https://github.com/MinnDevelopment/reactive-jda-bot).

### ReactiveEventManager

```kotlin
fun main() {
    // Create a ReactiveEventManager for Flux event streams
    val manager = ReactiveEventManager()
    // subscribe directly on the manager instance
    manager.on<ReadyEvent>()                       // Flux<ReadyEvent>
           .next()                                 // Mono<ReadyEvent>
           .subscribe { println("Ready to go!")  } // Subscribe to event
    manager.on<MessageReceivedEvent>()             // Flux<MessageReceivedEvent>
           .map { it.message }                     // Flux<Message>
           .filter { it.contentRaw == "!ping" }    // filter by content
           .map { it.channel }                     // Flux<MessageChannel>
           .map { it.sendMessage("Pong!") }        // Flux<MessageAction>
           .subscribe { it.queue() }               // Subscribe to event -> send message on success

   val jda = JDABuilder(BOT_TOKEN)
        .setEventManager(manager)
        .build()

   // you can also subscribe to events from the JDA instance
   jda.on<ShutdownEvent>()                         // Flux<ShutdownEvent>
      .subscribe { println("That was fun!") }      // Make a statement on shutdown, not guaranteed to run if daemon scheduler (default)
}
```

### Mono/Flux RestAction

Every RestAction receives an `asMono` extensions which converts them into a `Mono<T>` of the same result type.
<br>Additionally some more specific types such as `PaginationAction` can be streamed into a `Flux<T>`
which will automatically paginate the endpoint as demanded by the subscription.

#### PaginationAction\<T, *>

```kotlin
fun getMessagesForUser(channel: MessageChannel, user: User): Flux<Message> {
    val action = channel.iterableHistory
    return action.asFlux()                     // Flux<Message>
                 .filter { it.author == user } // filter by user
}
```

#### RestAction\<T>

```kotlin
fun sendAndLog(channel: MessageChannel, content: String) {
    val action = channel.sendMessage(content)
    action.asMono()                                             // Mono<Message>
          .flatMap { it.addReaction(EMOTE).asMono() }           // Mono<Void!> = empty mono
          .doOnSuccess { println("${channel.name}: $content") } // add side-effect
          .subscribe()                                          // subscribe to empty stream
}
```

#### RestAction<Iterable\<T>>

```kotlin
fun getResponsibleModerators(guild: Guild): Flux<String> {
 return guild.retrieveBanList()     // RestAction<List<Guild.Ban>>
             .toFlux()              // Flux<Ban>
             .map { it.user }       // Flux<User>
             .map { it.asTag }      // Flux<String>
}
```

### Entity Observers

```kotlin
fun onNextMessage(channel: MessageChannel, callback: (Message) -> Unit) {
    channel.onMessage()                // Flux<MessageReceivedEvent>
           .next()                     // Mono<MessageReceivedEvent>
           .map { it.message }         // Mono<Message>
           .subscribe { callback(it) }
}

fun onReaction(message: Message, reaction: String): Flux<User> {
    return message.on<MessageReactionAddEvent>()                // Flux<MessageReactionAddEvent>
                  .filter { it.reactionEmote.name == reaction } // Flux<MessageReactionAddEvent> with filter
                  .map { it.user }                              // Flux<User>
}
```

```kotlin
fun onNameChange(user: User): Flux<String> {
    return user.on<UserUpdateNameEvent>() // Flux<UserUpdateNameEvent>
               .map { it.newName }        // Flux<String>
}

fun onNameChange(channel: VoiceChannel): Flux<String> {
    return channel.onUpdate<VoiceChannelUpdateNameEvent>() // Flux<VoiceChannelUpdateNameEvent>
                  .map { it.newName }                      // Flux<String>
}
```
