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

package rx.internal.operators;

import java.util.concurrent.atomic.AtomicLong;

import rx.*;
import rx.Observable.OnSubscribe;

public final class OnSubscribeFromArray<T> implements OnSubscribe<T> {
    final T[] array;
    public OnSubscribeFromArray(T[] array) {
        this.array = array;
    }
    
    @Override
    public void call(Subscriber<? super T> child) {
        //调用的是subscriber的方法，即通知订阅者可以从observable那里拉取数据，其拉取的流量可以使用subscriber的request方法控制
        //假如这个observable与subscriber处于相同的线程，无论request方法一次请求多大的数据，也无论subscriber处理速度如何，
        //这个线程都会阻塞，因此当他们处于相同的线程的时候，其实是并不会发生backpressure这种需求的情况。然而，当他们处于不同的进程
        //的时候，rxjava会因为subscriber处理的速度跟不上observable发送的速度，而使rxJava一直缓存者observable发送出来的
        //数据，造成raJava使用的内存不断的增长，这时候，应当使用request方法可以控制流量
        child.setProducer(new FromArrayProducer<T>(child, array));
    }
    
    static final class FromArrayProducer<T>
    extends AtomicLong
    implements Producer {
        /** */
        private static final long serialVersionUID = 3534218984725836979L;
        
        final Subscriber<? super T> child;
        final T[] array;
        
        int index;
        
        public FromArrayProducer(Subscriber<? super T> child, T[] array) {
            this.child = child;
            this.array = array;
        }
        
        @Override
        public void request(long n) {
            if (n < 0) {
                throw new IllegalArgumentException("n >= 0 required but it was " + n);
            }
            if (n == Long.MAX_VALUE) {
                if (BackpressureUtils.getAndAddRequest(this, n) == 0) {
                    fastPath();
                }
            } else
            if (n != 0) {
                if (BackpressureUtils.getAndAddRequest(this, n) == 0) {
                    slowPath(n);
                }
            }
        }
        
        void fastPath() {
            final Subscriber<? super T> child = this.child;
            
            for (T t : array) {
                if (child.isUnsubscribed()) {
                    return;
                }
                
                child.onNext(t);
            }
            
            if (child.isUnsubscribed()) {
                return;
            }
            child.onCompleted();
        }


        void slowPath(long r) {
            final Subscriber<? super T> child = this.child;
            final T[] array = this.array;
            final int n = array.length;
            
            long e = 0L;
            //取出当前遍历到的数组下标index
            int i = index;

            for (;;) {

                //往subscriber发送长度为r的数据。
                while (r != 0L && i != n) {
                    if (child.isUnsubscribed()) {
                        return;
                    }
                    
                    child.onNext(array[i]);
                    
                    i++;
                    
                    if (i == n) {
                        if (!child.isUnsubscribed()) {
                            child.onCompleted();
                        }
                        return;
                    }
                    
                    r--;
                    e--;
                }
                
                r = get() + e;

                //更新下标index
                if (r == 0L) {
                    index = i;
                    r = addAndGet(e);
                    if (r == 0L) {
                        return;
                    }
                    e = 0L;
                }
            }
        }
    }
}
