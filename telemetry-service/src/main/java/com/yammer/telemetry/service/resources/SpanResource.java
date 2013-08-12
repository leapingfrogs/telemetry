package com.yammer.telemetry.service.resources;

import com.yammer.telemetry.tracing.AnnotationData;
import com.yammer.telemetry.tracing.SpanSink;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

@Path("/spans/{traceId}/{spanId}")
public class SpanResource {
    private final SpanSink sink;

    public SpanResource(SpanSink sink) {
        this.sink = sink;
    }

    @POST
    public void logAnnotation(@PathParam("traceId") long traceId, @PathParam("spanId") long spanId, AnnotationData annotation) {
        sink.recordAnnotation(traceId, spanId, annotation);
    }
}
