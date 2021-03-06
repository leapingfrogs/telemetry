package com.yammer.telemetry.tracing;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemorySpanSinkSource implements SpanSink, SpanSource {
    private final ConcurrentMap<BigInteger, Trace> traces = new ConcurrentHashMap<>();

    @Override
    public Collection<Trace> getTraces() {
        return traces.values();
    }

    @Override
    public Trace getTrace(BigInteger traceId) {
        return traces.get(traceId);
    }

    @Override
    public void record(SpanData spanData) {
        // This is cleaner from a code perspective, but it means we allocate a new Trace on
        // every request even if one is already in the map. This may be worth changing if
        // performance suffers here due to the frequency of calls.
        final Trace newTrace = new Trace(spanData.getTraceId());
        newTrace.addSpan(spanData);

        final Trace trace = traces.putIfAbsent(spanData.getTraceId(), newTrace);
        if (trace != null) {
            trace.addSpan(spanData);
        }
    }

    @Override
    public void recordAnnotation(BigInteger traceId, BigInteger spanId, AnnotationData annotationData) {
        final Trace newTrace = new Trace(traceId);
        newTrace.addAnnotation(spanId, annotationData);

        final Trace trace = traces.putIfAbsent(traceId, newTrace);
        if (trace != null) {
            trace.addAnnotation(spanId, annotationData);
        }
    }
}
