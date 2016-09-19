package org.apache.metron.common.stellar;

import org.apache.metron.common.dsl.FunctionResolverSingleton;
import org.apache.metron.common.dsl.Stellar;
import org.apache.metron.common.dsl.StellarFunction;
import org.apache.metron.common.dsl.StellarFunctionInfo;
import org.reflections.Reflections;
import org.reflections.util.ConfigurationBuilder;

import static org.apache.metron.common.dsl.FunctionResolverSingleton.effectiveClassPathUrls;

public class DocumentationGenerator {

  public static void main(String... argv) {
    System.out.println("<table>");
    System.out.println("<tr>");
    System.out.println("<th>Stellar Function</th><th>Description</th><th>Input</th><th>Returns</th>");
    System.out.println("</tr>");
    for(StellarFunctionInfo info: FunctionResolverSingleton.getInstance().getFunctionInfo()) {
      String row = "<tr>";
      row += "<td>" + info.getName() + "</td>";
      row += "<td>" + info.getDescription() + "</td>";
      row += "<td><ul>";
      for(String param :info.getParams()) {
        row += "<li>" + param + "</li>";
      }
      row += "</ul></td>";
      row += "<td>" + info.getReturns() + "</td>";
      row += "</tr>";
      System.out.println(row);
    }
    System.out.println("</table>");
  }


}
