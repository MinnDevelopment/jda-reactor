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
import net.dv8tion.jda.api.events.ShutdownEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.hooks.IEventManager;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.util.Logger;
import reactor.util.Loggers;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class ReactiveEventManager implements IEventManager, Disposable {
    private static final Logger log = Loggers.getLogger(ReactiveEventManager.class);
    private final Map<EventListener, Disposable> listeners = new HashMap<>();
    private final Sinks.Many<GenericEvent> sink;
    private boolean instance = true;
    private final Disposable reference;

    public ReactiveEventManager() {
        this(Sinks.many().multicast().onBackpressureBuffer());
    }

    public ReactiveEventManager(@Nonnull Sinks.Many<GenericEvent> sink) {
        this.sink = sink;
        this.reference = sink.asFlux().subscribe();
    }

    @Override
    public void dispose() {
        reference.dispose();
    }

    /**
     * Whether the sink of this instance should be completed by a {@link net.dv8tion.jda.api.events.ShutdownEvent}.
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

    @Nonnull
    public <T extends GenericEvent> Flux<T> on(@Nonnull Class<T> type) {
        return sink.asFlux()
                .log(log, Level.FINEST, true)
                .ofType(type);
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
