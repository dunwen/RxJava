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

import rx.*;
import rx.annotations.Beta;
import rx.exceptions.Exceptions;

/**
 * 这个插件让你可以注册一个函数处理传递给 Subscriber.onError(Throwable) 的错误。
 *
 * Abstract class for defining error handling logic in addition to the normal
 * {@link Subscriber#onError(Throwable)} behavior.
 * <p>
 * For example, all {@code Exception}s can be logged using this handler even if
 * {@link Subscriber#onError(Throwable)} is ignored or not provided when an {@link Observable} is subscribed to.
 * <p>
 * This plugin is also responsible for augmenting rendering of {@code OnErrorThrowable.OnNextValue}.
 * <p>
 * See {@link RxJavaPlugins} or the RxJava GitHub Wiki for information on configuring plugins: <a
 * href="https://github.com/ReactiveX/RxJava/wiki/Plugins">https://github.com/ReactiveX/RxJava/wiki/Plugins</a>.
 */
public abstract class RxJavaErrorHandler {

    /**
     * Receives all {@code Exception}s from an {@link Observable} passed to
     * 接受所有从Observable传递过来的错误，
     *
     * {@link Subscriber#onError(Throwable)}.
     * <p>
     * This should <em>never</em> throw an {@code Exception}. Make sure to try/catch({@code Throwable}) all code
     * inside this method implementation.
     * 
     * @param e
     *            the {@code Exception}
     */
    public void handleError(Throwable e) {
        // do nothing by default
    }

    protected static final String ERROR_IN_RENDERING_SUFFIX = ".errorRendering";

    /**
     * 从OnErrorThrowable.OnNextValue接受item并提供一个选择一个字符串代表这个错误的机会。意思就是把错误（error）转换成字符串标识这个错误咯

     * Receives items causing {@code OnErrorThrowable.OnNextValue} and gives a chance to choose the String
     * representation of the item in the {@code OnNextValue} stacktrace rendering. Returns {@code null} if this
     * type of item is not managed and should use default rendering.
     *
     *
     * <p>
     * Note that primitive types are always rendered as their {@code toString()} value.
     * <p>
     * If a {@code Throwable} is caught when rendering, this will fallback to the item's classname suffixed by
     * {@value #ERROR_IN_RENDERING_SUFFIX}.
     *
     * @param item the last emitted item, that caused the exception wrapped in
     *             {@code OnErrorThrowable.OnNextValue}
     * @return a short {@link String} representation of the item if one is known for its type, or null for
     *         default
     * @since (if this graduates from Experimental Beta to supported, replace this parenthetical with the
     *        release number)
     */
    @Beta
    public final String handleOnNextValueRendering(Object item) {

        try {
            return render(item);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Throwable t) {
            Exceptions.throwIfFatal(t);
        }
        return item.getClass().getName() + ERROR_IN_RENDERING_SUFFIX;
    }

    /**
     * Override this method to provide rendering for specific types other than primitive types and null.
     * 重写这个方法提供对原始类型的错误的转换，，，
     * <p>
     * For performance and overhead reasons, this should should limit to a safe production of a short
     * {@code String} (as large renderings will bloat up the stacktrace). Prefer to try/catch({@code Throwable})
     * all code inside this method implementation.
     * <p>
     * If a {@code Throwable} is caught when rendering, this will fallback to the item's classname suffixed by
     * {@value #ERROR_IN_RENDERING_SUFFIX}.
     *
     * @param item the last emitted item, that caused the exception wrapped in
     *             {@code OnErrorThrowable.OnNextValue}
     * @return a short {@link String} representation of the item if one is known for its type, or null for
     *         default
     *         返回一个字符串代表它的错误类型，默认是返回null
     *
     * @throws InterruptedException if the rendering thread is interrupted
     * @since (if this graduates from Experimental/Beta to supported, replace this parenthetical with the
     *        release number)
     */
    @Beta
    protected String render (Object item) throws InterruptedException {
        //do nothing by default
        return null;
    }

}
