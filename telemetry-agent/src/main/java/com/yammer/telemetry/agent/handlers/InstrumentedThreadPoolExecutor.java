package com.yammer.telemetry.agent.handlers;

import com.google.common.base.Optional;
import com.yammer.telemetry.tracing.Span;

import java.math.BigInteger;
import java.util.concurrent.*;

@SuppressWarnings("UnusedDeclaration")
public class InstrumentedThreadPoolExecutor extends ThreadPoolExecutor {
    private ThreadLocal<Span> local = new ThreadLocal<>();

    public InstrumentedThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
    }

    public InstrumentedThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
    }

    public InstrumentedThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, handler);
    }

    public InstrumentedThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
    }

    @Override
    protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
        return taskFor(super.newTaskFor(runnable, value), runnable.getClass().getName() + ":" + value);
    }

    @Override
    protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
        return taskFor(super.newTaskFor(callable), callable.getClass().getName());
    }

    @Override
    protected void beforeExecute(Thread t, Runnable r) {
        super.beforeExecute(t, r);

        if (r instanceof InstrumentedRunnableFuture) {
            InstrumentedRunnableFuture instrumentedRunnable = (InstrumentedRunnableFuture) r;

            BigInteger traceId = instrumentedRunnable.getTraceId();
            BigInteger spanId = instrumentedRunnable.getSpanId();

            if (traceId != null && spanId != null) {
                Span span = Span.attachSpan(traceId, spanId, instrumentedRunnable.getName());
                local.set(span);
                span.addAnnotation("Before", instrumentedRunnable.getName());
            }
        }
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);

        if (r instanceof InstrumentedRunnableFuture) {
            Span span = local.get();
            if (span != null) {
                span.addAnnotation("After", ((InstrumentedRunnableFuture) r).getName());
                span.end();
            }
        }
    }

    private <T> RunnableFuture<T> taskFor(RunnableFuture<T> future, String name) {
        BigInteger traceId = null;
        BigInteger spanId = null;
        Optional<Span> currentSpan = Span.currentSpan();
        if (currentSpan.isPresent()) {
            traceId = currentSpan.get().getTraceId();
            spanId = currentSpan.get().getSpanId();
            currentSpan.get().addAnnotation("Task", name);
        }
        return new InstrumentedRunnableFuture<>(future, name, traceId, spanId);
    }
}
