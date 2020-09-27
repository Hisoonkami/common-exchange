package com.adev.common.exchange.http.service;

import com.adev.common.exchange.exception.HttpClientUtilException;
import com.adev.common.exchange.http.client.HttpClientUtil;
import com.adev.common.exchange.http.domain.RequestParam;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.*;

public class HttpStreamingService {
    private static final Logger LOG = LoggerFactory.getLogger(HttpStreamingService.class);

    private ScheduledExecutorService pool;
    final int corePoolSize = 60;

    private class Subscription {
        final ObservableEmitter<String> emitter;
        final RequestParam requestParam;

        public Subscription(ObservableEmitter<String> emitter, RequestParam requestParam) {
            this.emitter = emitter;
            this.requestParam = requestParam;
        }
    }

    protected Map<String, Subscription> channels = new ConcurrentHashMap<>();

    protected Map<String, ScheduledFuture> scheduledFutures = new ConcurrentHashMap<>();

    public HttpStreamingService(String name) {
        //pool = Executors.newScheduledThreadPool(10);
        /*
        线程池处理策略：
        1、AbortPolicy：终止策略是默认的饱和策略，当队列满时，会抛出一个RejectExecutionException异常（第一段代码就是例子），客户可以捕获这个异常，根据需求编写自己的处理代码
        2、DiscardPolicy：策略会悄悄抛弃该任务。
        3、DiscardOldestPolicy：策略将会抛弃下一个将要执行的任务，如果此策略配合优先队列PriorityBlockingQueue，该策略将会抛弃优先级最高的任务
        4、CallerRunsPolicy：调用者运行策略，该策略不会抛出异常，不会抛弃任务，而是将任务回退给调用者线程执行（调用execute方法的线程），由于任务需要执行一段时间，所以在此期间不能提交任务，从而使工作线程有时间执行正在执行的任务。
         */
        ThreadFactory namedThreadFactory =
                new ThreadFactoryBuilder().setNameFormat(name + "-http-polling-%d").build();
//        pool = new ScheduledThreadPoolExecutor(corePoolSize, namedThreadFactory, new ThreadPoolExecutor.DiscardOldestPolicy());
        pool = new ScheduledThreadPoolExecutor(corePoolSize, namedThreadFactory, new ThreadPoolExecutor.CallerRunsPolicy());
    }

    public Observable<String> pollingRestApi(RequestParam requestParam) {
        LOG.info("Subscribing to channel {}.", requestParam.getChannelKey());
        return Observable.<String> create(e -> {
            try {
                Subscription newSubscription = null;
                if (channels.containsKey(requestParam.getChannelKey())) {
                    newSubscription = channels.get(requestParam.getChannelKey());
                } else {
                    newSubscription = new Subscription(e, requestParam);
                    channels.put(requestParam.getChannelKey(), newSubscription);
                }
                if (newSubscription != null) {
                    ScheduledFuture scheduledFuture=polling(newSubscription);
                    if(null!=scheduledFuture){
                        scheduledFutures.put(requestParam.getChannelKey(),scheduledFuture);
                    }
                }

            } catch (HttpClientUtilException exception) {
                e.onError(exception);
                return;
            }

        }).doOnDispose(() -> {
            ScheduledFuture scheduledFuture=scheduledFutures.get(requestParam.getChannelKey());
            if(null!=scheduledFuture){
                scheduledFuture.cancel(true);
            }
        });
    }

    private ScheduledFuture polling(Subscription subscription) {
        final Runnable task = () -> {
            try {
                //long start = System.currentTimeMillis();
                //LOG.debug("send http: {}", subscription.requestParam.getUrl());
                String data = HttpClientUtil.sendHttp(subscription.requestParam);
                //long end = System.currentTimeMillis();
                //ScheduledThreadPoolExecutor executor = (ScheduledThreadPoolExecutor) pool;
                //LOG.info("request use time：{}， poolSize={}，activeCount={}，queueSize={}， taskCount={}， completedTaskCount={} ", end - start, executor.getPoolSize(), executor.getActiveCount(), executor.getQueue().size(), executor.getTaskCount(), executor.getCompletedTaskCount());
//                LOG.debug("received message: {}", data);
                if (data != null) {
                    subscription.emitter.onNext(data);
                }
            } catch (Exception e) {
                LOG.error("request error: {}", e.getMessage());
            }

        };

        if (subscription.requestParam.isPolling()) {

            //initialDelay秒后开始发送请求，往后固定每PollingTime秒执行一次
            return pool.scheduleWithFixedDelay(task, subscription.requestParam.getInitialDelay(),
                    subscription.requestParam.getPollingTime(), TimeUnit.MILLISECONDS);

        } else {
            // 只执行一次请求
            return pool.schedule(task, subscription.requestParam.getInitialDelay(), TimeUnit.MILLISECONDS);
        }

    }

    public Completable connect() {
        return Completable.create(completable -> {
            if (pool != null) {
                completable.onComplete();
            } else {
                completable.onError(new RuntimeException("pool init failed."));
            }
        });
    }

    public boolean isAlive() {
        return !pool.isShutdown();
    }

    public Completable disconnect() {
        return Completable.create(completable -> {
            if (!pool.isShutdown()) {
                try {
                    pool.shutdown();
                    if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
                        pool.shutdownNow();
                    }
                    completable.onComplete();
                } catch (InterruptedException e) {
                    LOG.error("shutdown pool exception: {}", e.getMessage());
                    try {
                        pool.shutdownNow();
                    } catch (Exception e1) {
                        LOG.error("shutdown pool exception2: {}", e1.getMessage());
                    }
                }
            }
        });
    }
}
