package org.apache.metron.stellar.dsl.functions;

import org.apache.metron.stellar.dsl.BaseStellarFunction;
import org.apache.metron.stellar.dsl.Stellar;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class OrdinalFunctions {

    /**
     * Stellar Function: MAX
     * <p>
     * Return the maximum value of a list of input values in a Stellar list
     */
    @Stellar(name = "MAX"
            , description = "Returns the maximum value of a list of input values"
            , params = {"list_of_values - Stellar list of values to evaluate. The list may only contain 1 type of object (only strings or only numbers)" +
                        " and the objects must be comparable / ordinal"}
            , returns = "The highest value in the list, null if the list is empty or the input values could not be ordered")
    public static class Max extends BaseStellarFunction {

        @Override
        public Object apply(List<Object> args) {
            if (args.size() < 1 || args.get(0) == null) {
                throw new IllegalStateException("MAX function requires at least a Stellar list of values");
            }
            List list = (List<Object>) args.get(0);
            return orderList(list, true);
        }
    }

    /**
     * Stellar Function: MIN
     * <p>
     * Return the minimum value of a list of input values in a Stellar list
     */
    @Stellar(name = "MIN"
            , description = "Returns the minimum value of a list of input values"
            , params = {"list_of_values - Stellar list of values to evaluate. The list may only contain 1 type of object (only strings or only numbers)" +
            " and the objects must be comparable / ordinal"}
            , returns = "The lowest value in the list, null if the list is empty or the input values could not be ordered")
    public static class Min extends BaseStellarFunction {
        @Override
        public Object apply(List<Object> args) {
            if (args.size() < 1 || args.get(0) == null) {
                throw new IllegalStateException("MIN function requires at least a Stellar list of values");
            }
            List list = (List<Object>) args.get(0);
            return orderList(list, false);
        }
    }

    private static Object orderList(List<Object> list, Boolean max) {
        if (list.isEmpty()) {
            return null;
        }
        List filteredList = (List<Object>) list.stream().filter(index -> !(index == null)).collect(Collectors.toList());
        if (filteredList.isEmpty()) {
            return null;
        }
        try {
            if (max) {
                Collections.sort(filteredList,Collections.reverseOrder());
            }
            else {
                Collections.sort(filteredList);
            }
        } catch (ClassCastException e) {
            throw new IllegalStateException("Mixed objects were submitted to MAX/MIN function. The Stellar list can only contain comparable objects of 1 type");
        }
        return filteredList.get(0);
    }
}
