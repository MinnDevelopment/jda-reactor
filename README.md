[ ![version](https://shields.io/github/v/tag/MinnDevelopment/jda-reactor) ](#Installation)
# jda-reactor

A collection of kotlin extensions for JDA that make use with reactor-core easier.

## Installation

Replace the `$VERSION` with the latest release version.
<br>Replace `$JDA_VERSION` with the latest stable JDA v5 release.

### Gradle

```gradle
dependencies {
    implementation("net.dv8tion:JDA:$JDA_VERSION")
    implementation("com.github.MinnDevelopment:jda-reactor:$VERSION")
}

repositories {
    mavenCentral()
    maven("https://jitpack.io")
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
    <groupId>com.github.MinnDevelopment</groupId>
    <artifactId>jda-reactor</artifactId>
    <version>$VERSION</version>
</dependency>
```

```xml
<repository> <!-- jda-reactor -->
    <name>jitpack</name>
    <id>jitpack</id>
    <url>https://jitpack.io</url>
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
           .flatMap { it.asMono() }                // Flux<Message> (send message and provide result)
           .subscribe()                            // Subscribe to event

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
fun getBannedUsers(guild: Guild): Flux<String> {
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
                  .filter { it.emoji.name == reaction }         // Flux<MessageReactionAddEvent> with filter
                  .map { it.user }                              // Flux<User>
}
```

```kotlin
fun onNameChange(user: User): Flux<String> {
    return user.on<UserUpdateNameEvent>() // Flux<UserUpdateNameEvent>
               .map { it.newValue }       // Flux<String>
}

fun onNameChange(channel: GuildChannel): Flux<String> {
    return channel.onUpdate<ChannelUpdateNameEvent>() // Flux<ChannelUpdateNameEvent>
                  .map { it.newValue }                // Flux<String>
}
```

### CacheView

I've added a special `toFluxLocked` which makes use of the `lockedIterator()` that was introduced in JDA version 4. This will automatically lock the cache view for read access when `subscribe()` is invoked and unlock it on the completion signal.

#### Example toFluxLocked

```kotlin
fun findUserByName(jda: JDA, name: String): Mono<User> {
    return jda.userCache
              .toFluxLocked()                  // Flux<User> lazy locked user cache
              .filterFirst { it.name == name } // Mono<User> unlock on first match
}

fun sendToUser(jda: JDA, name: String, content: String) {
    return findUserByName(name)                          // Mono<User>
           .flatMap { it.openPrivateChannel().asMono() } // Mono<PrivateChannel>
           .flatMap { it.sendMessage(content).asMono() } // Mono<Message>
           .subscribe() // lock the user cache and look for the user by name
}
```

### Quality of Life Extensions

I've added a few extensions to reactor itself that might be useful when working with JDA.

- `T?.toMono()` improvement of `T.toMono()` which uses `Mono.justOrEmpty` instead
- `Mono.then(() -> Mono<R>)` lazy version of `Mono.then(Mono<R>)` similar to `Mono.flatMap`
- `Flux.then(() -> Mono<R>)` same as above
- `Flux.filterFirst((T) -> Boolean)` combination of `filter().next()`
- `Flux.filterFirstWhen((T) -> Publisher<Boolean>)` combination of `filterWhen().next()`
- `Flux.nextWhen((T) -> Mono<R>)` combination of `next().flatMap()`
- `Iterable<CompletionStage<T>>.asFlux(): Flux<T>` flatten lists of completion stages
