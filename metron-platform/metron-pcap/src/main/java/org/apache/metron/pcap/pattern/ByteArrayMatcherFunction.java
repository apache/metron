/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.metron.pcap.pattern;

import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.ParseException;
import org.apache.metron.stellar.dsl.Stellar;
import org.apache.metron.stellar.dsl.StellarFunction;

import javax.xml.bind.DatatypeConverter;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Stellar(namespace="BYTEARRAY"
        ,name="MATCHER"
        ,description = "Determine if a regex defined sequence of bytes (or strings) exists in a byte array."
        ,params = {
            "binary_regex - Regex defining what to look for in the byte array. Note syntax guide for binary regex is at https://github.com/nishihatapalmer/byteseek/blob/master/sequencesyntax.md"
           ,"data - The byte array to evaluate."
                  }
        ,returns="result: Boolean indicating whether or not the byte array is a match."
        )
public class ByteArrayMatcherFunction implements StellarFunction {
  @Override
  public Object apply(List<Object> args, Context context) throws ParseException {
    if(args.size() != 2) {
      return new IllegalStateException("Expected 2 arguments: regex and data");
    }
    String regex = (String)args.get(0);
    byte[] data = (byte[])args.get(1);
    try {
      return ByteArrayMatchingUtil.INSTANCE.match(regex, data);
    }
    catch (ExecutionException e) {
      throw new IllegalStateException("Unable to process " + regex + " against " + DatatypeConverter.printHexBinary(data));
    }
  }

  @Override
  public void initialize(Context context) {

  }

  @Override
  public boolean isInitialized() {
    return true;
  }
}
