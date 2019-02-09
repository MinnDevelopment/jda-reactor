package club.minnced.jda.reactor

import net.dv8tion.jda.api.requests.RestAction
import net.dv8tion.jda.api.requests.restaction.pagination.PaginationAction
import net.dv8tion.jda.internal.requests.restaction.pagination.MessagePaginationActionImpl
import reactor.core.publisher.Flux
import reactor.core.publisher.FluxSink
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono

fun <T> RestAction<T>.asMono() = Mono.fromFuture(this::submit)

fun <T, M> PaginationAction<T, M>.asFlux(overflowStrategy: FluxSink.OverflowStrategy = FluxSink.OverflowStrategy.BUFFER): Flux<T>
    where M : PaginationAction<T, M> = Flux.create<T>({ sink ->
    cache(false)
    forEachAsync {
        sink.next(it)
        true
    }.toMono().doOnError(sink::error).doFinally { sink.complete() }
}, overflowStrategy)

fun main() {
    val action = MessagePaginationActionImpl(null)
    action.asFlux()
            .map { "${it.author.asTag}: ${it.contentDisplay}" }
            .subscribe { println(it) }
}