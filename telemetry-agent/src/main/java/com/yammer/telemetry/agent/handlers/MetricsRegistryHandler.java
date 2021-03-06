package com.yammer.telemetry.agent.handlers;

import javassist.*;

import java.io.IOException;

public class MetricsRegistryHandler extends SubTypeInstrumentationHandler {
    public MetricsRegistryHandler() {
        super("com.yammer.metrics.core.MetricsRegistry");
    }

    @Override
    protected boolean transform(CtClass cc, ClassPool pool) throws NotFoundException, CannotCompileException, IOException {
        if (!superTypeName.equals(cc.getName())) {
            return false;
        }

        switchImplementation(cc, pool, "com.yammer.metrics.core.Timer", "com.yammer.metrics.core.InstrumentedTimer");
        switchImplementation(cc, pool, "com.yammer.metrics.core.Meter", "com.yammer.metrics.core.InstrumentedMeter");
        switchImplementation(cc, pool, "com.yammer.metrics.core.Counter", "com.yammer.metrics.core.InstrumentedCounter");

        CtMethod getOrAdd = cc.getDeclaredMethod("getOrAdd");
        getOrAdd.insertBefore(
                "{" +
                        "  if ($2 instanceof com.yammer.metrics.core.MetricNameAware) {" +
                        "    ((com.yammer.metrics.core.MetricNameAware)$2).setMetricName((com.yammer.metrics.core.MetricName)$1);" +
                        "  }" +
                        "}");

        return true;
    }

    public static void switchImplementation(CtClass cc, ClassPool pool, String from, String to) throws NotFoundException, CannotCompileException {
        CtClass oldClass = pool.get(from);
        CtClass newClass = pool.get(to);

        CodeConverter converter = new CodeConverter();
        converter.replaceNew(oldClass, newClass);

        cc.instrument(converter);
    }
}
