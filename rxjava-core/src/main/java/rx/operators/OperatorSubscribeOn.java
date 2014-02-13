/**
 * Copyright 2014 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.operators;

import rx.Observable;
import rx.Observable.Operator;
import rx.Scheduler;
import rx.Scheduler.Inner;
import rx.Subscriber;
import rx.observables.GroupedObservable;
import rx.subjects.PublishSubject;
import rx.util.functions.Action1;

/**
 * Subscribes and unsubscribes Observers on the specified Scheduler.
 * <p>
 * Will occur asynchronously except when subscribing to `GroupedObservable`, `PublishSubject` and possibly other "hot" Observables
 * in which case it will subscribe synchronously and buffer/block onNext calls until the subscribe has occurred.
 * <p>
 * See https://github.com/Netflix/RxJava/issues/844 for more information on the "time gap" issue that the synchronous
 * subscribe is solving.
 * 
 * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/subscribeOn.png">
 */
public class OperatorSubscribeOn<T> implements Operator<T, Observable<T>> {

    private final Scheduler scheduler;
    /**
     * Indicate that events fired between the original subscription time and
     * the actual subscription time should not get lost.
     */
    private final boolean dontLoseEvents;
    /** The buffer size to avoid flooding. Negative value indicates an unbounded buffer. */
    private final int bufferSize;

    public OperatorSubscribeOn(Scheduler scheduler, boolean dontLoseEvents) {
        this(scheduler, dontLoseEvents, -1);
    }

    /**
     * Construct a SubscribeOn operator.
     * 
     * @param scheduler
     *            the target scheduler
     * @param dontLoseEvents
     *            indicate that events should be buffered until the actual subscription happens
     * @param bufferSize
     *            if dontLoseEvents == true, this indicates the buffer size. Filling the buffer will
     *            block the source. -1 indicates an unbounded buffer
     */
    public OperatorSubscribeOn(Scheduler scheduler, boolean dontLoseEvents, int bufferSize) {
        this.scheduler = scheduler;
        this.dontLoseEvents = dontLoseEvents;
        this.bufferSize = bufferSize;
    }

    @Override
    public Subscriber<? super Observable<T>> call(final Subscriber<? super T> subscriber) {
        return new Subscriber<Observable<T>>() {

            @Override
            public void onCompleted() {
                // ignore
            }

            @Override
            public void onError(Throwable e) {
                subscriber.onError(e);
            }

            boolean checkNeedBuffer(Observable<?> o) {
                /*
                 * Included are some Observable types known to be "hot" and thus needing
                 * buffering when subscribing across thread boundaries otherwise
                 * we can lose data.
                 * 
                 * See https://github.com/Netflix/RxJava/issues/844 for more information.
                 */
                return dontLoseEvents
                        || ((o instanceof GroupedObservable<?, ?>)
                        || (o instanceof PublishSubject<?>)
                        // || (o instanceof BehaviorSubject<?, ?>)
                        );
            }

            @Override
            public void onNext(final Observable<T> o) {
                if (checkNeedBuffer(o)) {
                    // use buffering (possibly blocking) for a possibly synchronous subscribe
                    final BufferUntilSubscriber<T> bus = new BufferUntilSubscriber<T>(bufferSize, subscriber);
                    o.subscribe(bus);
                    subscriber.add(scheduler.schedule(new Action1<Inner>() {
                        @Override
                        public void call(final Inner inner) {
                            bus.enterPassthroughMode();
                        }
                    }));
                    return;
                } else {
                    // no buffering (async subscribe)
                    scheduler.schedule(new Action1<Inner>() {

                        @Override
                        public void call(final Inner inner) {
                            o.subscribe(new Subscriber<T>(subscriber) {

                                @Override
                                public void onCompleted() {
                                    subscriber.onCompleted();
                                }

                                @Override
                                public void onError(Throwable e) {
                                    subscriber.onError(e);
                                }

                                @Override
                                public void onNext(T t) {
                                    subscriber.onNext(t);
                                }
                            });
                        }
                    });
                }
            }

        };
    }
}
