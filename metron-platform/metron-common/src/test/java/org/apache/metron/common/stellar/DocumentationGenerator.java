package org.apache.metron.common.stellar;

import com.google.common.collect.Lists;
import org.apache.metron.common.dsl.FunctionResolverSingleton;
import org.apache.metron.common.dsl.Stellar;
import org.apache.metron.common.dsl.StellarFunction;
import org.apache.metron.common.dsl.StellarFunctionInfo;
import org.reflections.Reflections;
import org.reflections.util.ConfigurationBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static org.apache.metron.common.dsl.FunctionResolverSingleton.effectiveClassPathUrls;

public class DocumentationGenerator {

  public static void main(String... argv) {
    List<StellarFunctionInfo> functions = Lists.newArrayList(FunctionResolverSingleton.getInstance().getFunctionInfo());
    Collections.sort(functions, (o1, o2) -> o1.getName().compareTo(o2.getName()));
    for(StellarFunctionInfo info: functions) {
      System.out.println( "* `" + info.getName() + "`");
      System.out.println( "  * Description: " + info.getDescription() );
      System.out.println( "  * Input:");
      for(String param :info.getParams()) {
        System.out.println( "    * " + param );
      }
      System.out.println( "  * Returns: " + info.getReturns() );
    }
  }


}
