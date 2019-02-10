
# jda-reactor

A collection of kotlin extensions for JDA that make use with reactor-core easier.

## Installation

Replace the `$VERSION` with the latest release version.

### Gradle

```kotlin
dependencies {
    implementation("club.minnced:jda-reactor:$VERSION")
}

repositories {
    jcenter()
}
```

### Maven

```xml
<dependency>
    <artifactId>jda-reactor</artifactId>
    <groupId>club.minnced</groupId>
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

### ReactiveEventManager

```kotlin
fun main() {
    val manager = ReactiveEventManager()
    manager.on(ReadyEvent::class.java)
           .subscribe { println("Ready to go!")  }
    manager.on(MessageReceivedEvent::class.java)
           .filter { it.contentRaw == "!ping" }
           .map { it.channel }
           .map { it.sendMessage("Pong!") }
           .subscribe { it.queue() }

   JDABuilder(BOT_TOKEN)
        .setEventManager(manager)
        .build()
}
```

### Mono/Flux RestAction

```kotlin
fun getMessagesForUser(channel: MessageChannel, user: User): Flux<Message> {
    val action = channel.iterableHistory
    return action.asFlux().filter { it.user == user }
}
```

```kotlin
fun sendAndLog(channel: MessageChannel, content: String) {
    val action = channel.sendMessage(content)
    action.asMono()
          .flatMap { it.addReaction(EMOTE).asMono() }
          .subscribe { println("${channel.name}: $content") }
}
```
