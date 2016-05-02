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

package rx.plugins;

import rx.Scheduler;
import rx.annotations.Experimental;
import rx.functions.Action0;
import rx.internal.schedulers.CachedThreadScheduler;
import rx.internal.schedulers.EventLoopsScheduler;
import rx.internal.schedulers.NewThreadScheduler;
import rx.schedulers.Schedulers;

/**
 * 这个插件让你可以使用你选择的调度器覆盖默认的计算、IO和新线程调度 (Scheduler)
 * 这个插件提供两个途径去订做调度器的功能
 * 1.假若你想重新定义全部的调度器，那么重写return Scheduler (io(), computation(), newThread()这3个方法
 * 2.你可以在action0传入来的时候装饰（重定义）传入来的action的行为，系统提供的调度器都使用这个hook，所以
 * 这是一个很方便的途径去修改调度器的行为而不用批量的修改调度器。（说白了，就是重写onSchedule()修饰一下传入的action）
 *
 * This plugin class provides 2 ways to customize {@link Scheduler} functionality
 * 1.  You may redefine entire schedulers, if you so choose.  To do so, override
 * the 3 methods that return Scheduler (io(), computation(), newThread()).
 * 2.  You may wrap/decorate an {@link Action0}, before it is handed off to a Scheduler.  The system-
 * supplied Schedulers (Schedulers.ioScheduler, Schedulers.computationScheduler,
 * Scheduler.newThreadScheduler) all use this hook, so it's a convenient way to
 * modify Scheduler functionality without redefining Schedulers wholesale.
 *
 * Also, when redefining Schedulers, you are free to use/not use the onSchedule decoration hook.
 * <p>
 * See {@link RxJavaPlugins} or the RxJava GitHub Wiki for information on configuring plugins:
 * <a href="https://github.com/ReactiveX/RxJava/wiki/Plugins">https://github.com/ReactiveX/RxJava/wiki/Plugins</a>.
 */
public class RxJavaSchedulersHook {

    /**
     * Create an instance of the default {@link Scheduler} used for {@link Schedulers#computation()}.
     */
    @Experimental
    public static Scheduler createComputationScheduler() {
        return new EventLoopsScheduler();
    }

    /**
     * Create an instance of the default {@link Scheduler} used for {@link Schedulers#io()}.
     */
    @Experimental
    public static Scheduler createIoScheduler() {
        return new CachedThreadScheduler();
    }

    /**
     * Create an instance of the default {@link Scheduler} used for {@link Schedulers#newThread()}.
     */
    @Experimental
    public static Scheduler createNewThreadScheduler() {
        return new NewThreadScheduler();
    }

    protected RxJavaSchedulersHook() {

    }

    private final static RxJavaSchedulersHook DEFAULT_INSTANCE = new RxJavaSchedulersHook();

    /**
     * Scheduler to return from {@link rx.schedulers.Schedulers#computation()} or null if default should be
     * used.
     *
     * This instance should be or behave like a stateless singleton;
     */
    public Scheduler getComputationScheduler() {
        return null;
    }

    /**
     * Scheduler to return from {@link rx.schedulers.Schedulers#io()} or null if default should be used.
     *
     * This instance should be or behave like a stateless singleton;
     */
    public Scheduler getIOScheduler() {
        return null;
    }

    /**
     * Scheduler to return from {@link rx.schedulers.Schedulers#newThread()} or null if default should be used.
     *
     * This instance should be or behave like a stateless singleton;
     */
    public Scheduler getNewThreadScheduler() {
        return null;
    }

    /**
     * 在真正的action传递到调度器之前调用这个方法，可以用作包裹，代理，修饰，打印log等待，默认的只是返回它本身
     * Invoked before the Action is handed over to the scheduler.  Can be used for wrapping/decorating/logging.
     * The default is just a pass through.
     * @param action action to schedule
     * @return wrapped action to schedule
     */
    public Action0 onSchedule(Action0 action) {
        return action;
    }

    public static RxJavaSchedulersHook getDefaultInstance() {
        return DEFAULT_INSTANCE;
    }
}
