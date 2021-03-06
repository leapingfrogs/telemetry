package com.yammer.telemetry.tracing;

import java.math.BigInteger;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class AsynchronousSpanSink implements SpanSink {
    private final ExecutorService executor;
    private final JobFactory jobFactory;

    public AsynchronousSpanSink(ExecutorService executor, JobFactory jobFactory) {
        this.executor = executor;
        this.jobFactory = jobFactory;
    }

    @Override
    public void record(SpanData spanData) {
        Runnable job = jobFactory.createJob(spanData);
        executor.execute(job);
    }

    @Override
    public void recordAnnotation(BigInteger traceId, BigInteger spanId, AnnotationData annotationData) {
        Runnable job = jobFactory.createJob(traceId, spanId, annotationData);
        executor.execute(job);
    }

    /**
     * Shutdown the underlying executor in a graceful manner.
     *
     * @param timeout grace period for the shutdown to happen within
     * @param timeunit timeunit for grace period
     * @return the number of tasks remaining if executor was forcefully killed.
     */
    public int shutdown(long timeout, TimeUnit timeunit) {
        try {
            executor.shutdown();
            if (executor.awaitTermination(timeout, timeunit)) {
                return 0;
            }
        } catch (InterruptedException ignore) {
        }

        return executor.shutdownNow().size();
    }

    public static interface JobFactory {
        Runnable createJob(SpanData data);
        Runnable createJob(BigInteger traceId, BigInteger spanId, AnnotationData data);
    }
}
