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

package club.minnced.jda.reactor;

import net.dv8tion.jda.api.events.ExceptionEvent;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.session.ShutdownEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.hooks.IEventManager;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.util.Logger;
import reactor.util.Loggers;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Level;

/**
 * Implementation if {@link IEventManager} which publishes events to a {@link Flux}.
 *
 * <p>This implementation provides access to event-specific flux instances with {@link #on(Class)}.
 */
public class ReactiveEventManager implements IEventManager, Disposable {
    private static final Logger log = Loggers.getLogger(ReactiveEventManager.class);
    private final Map<EventListener, Disposable> listeners = new HashMap<>();
    private final Sinks.Many<GenericEvent> sink;
    private final Disposable reference;
    private Flux<GenericEvent> flux;
    private boolean instance = true;

    /**
     * Create a new ReactiveEventManager with custom sink and flux configuration.
     * <br>This manager will automatically subscribe to the output flux. You can dispose this subscription with {@link #dispose()}.
     *
     * <p>This uses {@code Sinks.many().multicast().onBackpressureBuffer()} as the default sink.
     *
     * @see   #setInstance(boolean)
     * @see   #getFlux()
     * @see   #dispose()
     */
    public ReactiveEventManager() {
        this(Sinks.many().multicast().onBackpressureBuffer());
    }

    /**
     * Create a new ReactiveEventManager with custom sink and flux configuration.
     * <br>This manager will automatically subscribe to the output flux. You can dispose this subscription with {@link #dispose()}.
     *
     * @param sink
     *        The {@link Sinks.Many} instance this manager should use
     *
     * @see   #setInstance(boolean)
     * @see   #getFlux()
     * @see   #dispose()
     */
    public ReactiveEventManager(@Nonnull Sinks.Many<GenericEvent> sink) {
        this(sink, null);
    }

    /**
     * Create a new ReactiveEventManager with custom sink and flux configuration.
     * <br>This manager will automatically subscribe to the output flux. You can dispose this subscription with {@link #dispose()}.
     *
     * <p>This uses {@code Sinks.many().multicast().onBackpressureBuffer()} as the default sink.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * val manager = ReactiveEventManager({
     *     it.publishOn(Schedulers.elastic()
     *       .log(customLogger, Level.INFO, true)
     * })
     * }</pre>
     *
     * @param spec
     *        Possible further configuration for the {@link Flux} for event streaming.
     *        This can be useful to configure a custom scheduler or log level, by default this will use {@code log(logger, Level.FINEST, true)}.
     *
     * @see   #setInstance(boolean)
     * @see   #getFlux()
     * @see   #dispose()
     */
    public ReactiveEventManager(@Nullable Function<? super Flux<GenericEvent>, Flux<GenericEvent>> spec) {
        this(Sinks.many().multicast().onBackpressureBuffer(), spec);
    }

    /**
     * Create a new ReactiveEventManager with custom sink and flux configuration.
     * <br>This manager will automatically subscribe to the output flux. You can dispose this subscription with {@link #dispose()}.
     *
     * @param sink
     *        The {@link Sinks.Many} instance this manager should use
     * @param spec
     *        Possible further configuration for the {@link Flux} for event streaming.
     *        This can be useful to configure a custom scheduler or log level, by default this will use {@code log(logger, Level.FINEST, true)}.
     *
     * @see   #setInstance(boolean)
     * @see   #getFlux()
     * @see   #dispose()
     */
    public ReactiveEventManager(@Nonnull Sinks.Many<GenericEvent> sink, @Nullable Function<? super Flux<GenericEvent>, Flux<GenericEvent>> spec) {
        this.sink = sink;
        Flux<GenericEvent> tmp = sink.asFlux().log(log, Level.FINEST, true);
        this.flux = spec == null ? tmp : spec.apply(tmp);
        this.reference = flux.subscribe();
    }

    /**
     * The manager automatically subscribes to any flux it publishes from.
     * <br>This dispose implementation simply causes that subscription to get disposed.
     *
     * {@inheritDoc}
     */
    @Override
    public void dispose() {
        reference.dispose();
    }

    /**
     * The flux used for event publishing.
     * <br>Identical to {@code on(GenericEvent.class)}
     *
     * @return The {@link Flux} instance
     */
    @Nonnull
    public Flux<GenericEvent> getFlux() {
        return flux;
    }

    /**
     * Applies custom settings to the flux instance.
     *
     * @param  spec
     *         Function used to apply settings, must return updated flux instance
     *
     * @return Same manager instance
     */
    @Nonnull
    public ReactiveEventManager applySpec(@Nonnull Function<? super Flux<GenericEvent>, Flux<GenericEvent>> spec) {
        this.flux = Objects.requireNonNull(spec.apply(flux));
        return this;
    }

    /**
     * Whether the sink of this instance should be completed by a {@link ShutdownEvent}.
     * <br>Default: true
     *
     * @param enabled
     *        True, if shutdown should complete the sink
     */
    public void setInstance(boolean enabled) {
        this.instance = enabled;
    }

    @Override
    public void handle(@Nonnull GenericEvent event) {
        try {
            sink.tryEmitNext(event);
        } catch (Throwable t) {
            sink.tryEmitNext(new ExceptionEvent(event.getJDA(), t, false));
        }
        if (event instanceof ShutdownEvent) {
            dispose();
            if (instance)
                sink.tryEmitComplete();
        }
    }

    /**
     * Returns a {@link Flux} instance for the specific event type.
     * <br>Shortcut for {@code getFlux().ofType(type)}.
     *
     * @param  type
     *         Class instance for the event type
     * @param  <T>
     *         The event type
     *
     * @throws NullPointerException
     *         If null is provided
     *
     * @return {@link Flux}
     */
    @Nonnull
    public <T extends GenericEvent> Flux<T> on(@Nonnull Class<T> type) {
        return flux.ofType(type);
    }

    @Override
    public void register(@Nonnull Object listener) {
        if (listener instanceof EventListener) {
            EventListener eventListener = (EventListener) listener;
            Disposable disposable = on(GenericEvent.class).subscribe(eventListener::onEvent);
            listeners.put(eventListener, disposable);
        }
        else throw new UnsupportedOperationException();
    }

    @Override
    public void unregister(@Nonnull Object listener) {
        if (listener instanceof EventListener) {
            Disposable disposable = listeners.remove(listener);
            if (disposable != null)
                disposable.dispose();
        }
        else throw new UnsupportedOperationException();
    }

    @Nonnull
    @Override
    public List<Object> getRegisteredListeners() {
        return new ArrayList<>(listeners.keySet());
    }
}
