package org.apache.metron.stellar.external;

import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.ParseException;
import org.apache.metron.stellar.dsl.Stellar;
import org.apache.metron.stellar.dsl.StellarFunction;
import java.util.List;

public class TimeFunction {
  @Stellar( name="NOW",
            description = "Right now!",
            params = {},
            returns="Timestamp"
          )
  public static class Now implements StellarFunction {
    
    public Object apply(List<Object> list, Context context) throws ParseException {
      return System.currentTimeMillis();
    }
    
    public void initialize(Context context) { }
    
    public boolean isInitialized() {
      return true;
    }
  }
}
