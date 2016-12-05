package org.apache.metron.common.utils;

import org.apache.metron.common.dsl.*;
import org.apache.metron.common.stellar.StellarPredicateProcessor;
import org.apache.metron.common.stellar.StellarProcessor;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

public class StellarProcessorUtils {

    /**
     * This ensures the basic contract of a stellar expression is adhered to:
     * 1. Validate works on the expression
     * 2. The output can be serialized and deserialized properly
     *
     * @param rule
     * @param variables
     * @param context
     * @return
     */
    public static Object run(String rule, Map<String, Object> variables, Context context) {
        StellarProcessor processor = new StellarProcessor();
        Assert.assertTrue(rule + " not valid.", processor.validate(rule, context));
        Object ret = processor.parse(rule, x -> variables.get(x), StellarFunctions.FUNCTION_RESOLVER(), context);
        byte[] raw = SerDeUtils.toBytes(ret);
        Object actual = SerDeUtils.fromBytes(raw, Object.class);
        Assert.assertEquals(ret, actual);
        return ret;
    }

    public static Object run(String rule, Map<String, Object> variables) {
        return run(rule, variables, Context.EMPTY_CONTEXT());
    }

    public static boolean runPredicate(String rule, Map resolver) {
        return runPredicate(rule, resolver, Context.EMPTY_CONTEXT());
    }

    public static boolean runPredicate(String rule, Map resolver, Context context) {
        return runPredicate(rule, new MapVariableResolver(resolver), context);
    }

    public static boolean runPredicate(String rule, VariableResolver resolver) {
        return runPredicate(rule, resolver, Context.EMPTY_CONTEXT());
    }

    public static boolean runPredicate(String rule, VariableResolver resolver, Context context) {
        StellarPredicateProcessor processor = new StellarPredicateProcessor();
        Assert.assertTrue(rule + " not valid.", processor.validate(rule));
        return processor.parse(rule, resolver, StellarFunctions.FUNCTION_RESOLVER(), context);
    }
}
