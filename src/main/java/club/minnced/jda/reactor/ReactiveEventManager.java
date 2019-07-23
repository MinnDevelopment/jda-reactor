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
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxProcessor;
import reactor.core.publisher.FluxSink;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.util.Logger;
import reactor.util.Loggers;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class ReactiveEventManager implements IEventManager {
    private static final Logger log = Loggers.getLogger(ReactiveEventManager.class);
    private final FluxProcessor<GenericEvent, ? super GenericEvent> processor;
    private final Scheduler scheduler;
    private final FluxSink<GenericEvent> eventSink;
    private final Map<EventListener, Disposable> listeners = new HashMap<>();

    private boolean disposeOnShutdown = true;
    private boolean instance = true;

    public ReactiveEventManager() {
        this(FluxSink.OverflowStrategy.BUFFER);
    }

    public ReactiveEventManager(@Nonnull FluxSink.OverflowStrategy strategy) {
        this(EmitterProcessor.create(), Schedulers.newSingle("JDA-EventManager", true), strategy);
        scheduler.start();
    }

    public ReactiveEventManager(@Nonnull FluxProcessor<GenericEvent, ? super GenericEvent> processor, @Nonnull Scheduler scheduler, @Nonnull FluxSink.OverflowStrategy strategy) {
        this.processor = processor;
        this.scheduler = scheduler;
        this.eventSink = processor.sink(strategy);
    }

    /**
     * The scheduler for this instance
     *
     * @return The scheduler for this instance
     */
    @Nonnull
    public Scheduler getScheduler() {
        return scheduler;
    }

    /**
     * Whether to dispose of the scheduler when a {@link net.dv8tion.jda.api.events.ShutdownEvent} is received
     * <br>Default: true
     *
     * @param enabled
     *        True, if shutdown should dispose the scheduler
     */
    public void setDisposeOnShutdown(boolean enabled) {
        this.disposeOnShutdown = enabled;
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
            eventSink.next(event);
        } catch (Throwable t) {
            eventSink.next(new ExceptionEvent(event.getJDA(), t, false));
        }
        if (instance && event instanceof ShutdownEvent) {
            eventSink.complete();
            if (disposeOnShutdown)
                scheduler.dispose();
        }
    }

    @Nonnull
    public <T extends GenericEvent> Flux<T> on(@Nonnull Class<T> type) {
        return processor.publishOn(scheduler)
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

    public static class Builder {
        private FluxProcessor<GenericEvent, ? super GenericEvent> processor;
        private Scheduler scheduler;
        private FluxSink.OverflowStrategy overflowStrategy;
        private boolean instance = true, dispose = true;

        /**
         * The {@link reactor.core.publisher.FluxProcessor} to use.
         * <br>Default: {@link reactor.core.publisher.EmitterProcessor}.
         *
         * @param  processor
         *         The processor to use
         *
         * @return Current builder
         */
        @Nonnull
        public Builder setProcessor(@Nullable FluxProcessor<GenericEvent, ? super GenericEvent> processor) {
            this.processor = processor;
            return this;
        }

        /**
         * The {@link reactor.core.scheduler.Scheduler} to use.
         * <br>Default: {@link reactor.core.scheduler.Schedulers#newSingle(String, boolean)}, daemon = true.
         *
         * @param  scheduler
         *         The scheduler to use
         *
         * @return Current builder
         */
        @Nonnull
        public Builder setScheduler(@Nullable Scheduler scheduler) {
            this.scheduler = scheduler;
            return this;
        }

        /**
         * The {@link reactor.core.publisher.FluxSink.OverflowStrategy} to use.
         * <br>Default: {@link reactor.core.publisher.FluxSink.OverflowStrategy#BUFFER}.
         *
         * @param  overflowStrategy
         *         The overflow strategy
         *
         * @return Current builder
         */
        @Nonnull
        public Builder setOverflowStrategy(@Nullable FluxSink.OverflowStrategy overflowStrategy) {
            this.overflowStrategy = overflowStrategy;
            return this;
        }

        /**
         * Whether to complete the event sink when shutdown even is fired.
         * <br>Default: True
         *
         * @param  instance
         *         True, if shutdown should signal complete()
         *
         * @return Current builder
         */
        @Nonnull
        public Builder setInstance(boolean instance) {
            this.instance = instance;
            return this;
        }

        /**
         * Whether the scheduler should be disposed on shutdown.
         *
         * @param  dispose
         *         True, if the shutdown should call dispose() on the scheduler
         *
         * @return Current builder
         */
        @Nonnull
        public Builder setDispose(boolean dispose) {
            this.dispose = dispose;
            return this;
        }

        /**
         * The {@link reactor.core.publisher.FluxProcessor} that was last set with {@link #setProcessor(reactor.core.publisher.FluxProcessor)}.
         *
         * @return The processor or null
         */
        @Nullable
        public FluxProcessor<GenericEvent, ? super GenericEvent> getProcessor() {
            return processor;
        }

        /**
         * The {@link reactor.core.scheduler.Scheduler} that was last set with {@link #setScheduler(reactor.core.scheduler.Scheduler)}.
         *
         * @return The scheduler or null
         */
        @Nullable
        public Scheduler getScheduler() {
            return scheduler;
        }

        /**
         * The {@link reactor.core.publisher.FluxSink.OverflowStrategy} that was last set with {@link #setOverflowStrategy(reactor.core.publisher.FluxSink.OverflowStrategy)}.
         *
         * @return The overflow strategy or null
         */
        @Nullable
        public FluxSink.OverflowStrategy getOverflowStrategy() {
            return overflowStrategy;
        }

        /**
         * Whether the shutdown event will complete the flux sink.
         *
         * @return True, if the shutdown event will complete the flux sink
         */
        public boolean isInstance() {
            return instance;
        }

        /**
         * Whether the scheduler will be disposed on shutdown.
         *
         * @return True, if the scheduler will be disposed on shutdown.
         */
        public boolean isDispose() {
            return dispose;
        }

        /**
         * Creates a new {@link club.minnced.jda.reactor.ReactiveEventManager} instance
         * with the specified settings.
         *
         * @return The {@link club.minnced.jda.reactor.ReactiveEventManager}
         */
        @Nonnull
        public ReactiveEventManager build() {
            FluxProcessor<GenericEvent, ? super GenericEvent> processor = this.processor == null ? EmitterProcessor.create() : this.processor;
            Scheduler scheduler =  this.scheduler == null ? Schedulers.newSingle("JDA-EventManager", true) : this.scheduler;
            FluxSink.OverflowStrategy strategy = this.overflowStrategy == null ? FluxSink.OverflowStrategy.BUFFER : this.overflowStrategy;
            if (this.scheduler == null)
                scheduler.start();
            ReactiveEventManager manager = new ReactiveEventManager(processor, scheduler, strategy);
            manager.setDisposeOnShutdown(dispose);
            manager.setInstance(instance);
            return manager;
        }
    }
}
