/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.metron.stellar.dsl.functions;

import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.apache.metron.stellar.common.encoding.Encodings;
import org.apache.metron.stellar.dsl.BaseStellarFunction;
import org.apache.metron.stellar.dsl.Stellar;

/**
 * Support for Stellar Functions based on the Apache Commons Codec library
 * http://commons.apache.org/proper/commons-codec/index.html
 */
public class EncodingFunctions {

  @Stellar(name = "GET_SUPPORTED_ENCODINGS",
      description = "Returns a list of the encodings that are currently supported as a list",
      params = {},
      returns = "A list of supported encodings"
  )
  public static class GetSupportedEncodings extends BaseStellarFunction {

    @Override
    public Object apply(List<Object> list) {
      return Encodings.SUPPORTED_LIST;
    }
  }

  @Stellar(name = "IS_ENCODING",
      description = "Returns if the passed string is encoded in one of the supported encodings",
      params = {"string - the string to test",
          "encoding - the encoding to test, must be one of encodings returned from "
              + "LIST_SUPPORTED_ENCODINGS"
      },
      returns = "true if it is encoded, false if not"
  )
  public static class IsEncoding extends BaseStellarFunction {

    @Override
    public Object apply(List<Object> list) {
      if (list.size() < 2) {
        throw new IllegalStateException(
            "IS_ENCODING expects two args: [string, encoding] where encoding is one from "
                + "the supported list");
      }
      String str = (String) list.get(0);
      String encoding = (String) list.get(1);
      if (StringUtils.isEmpty(str) || StringUtils.isEmpty(encoding)) {
        return false;
      }

      Encodings enc = null;
      try {
        enc = Encodings.valueOf(encoding.toUpperCase());
      } catch (IllegalArgumentException iae) {
        throw new IllegalStateException(String.format("Encoding %s not supported", encoding), iae);
      }
      return enc.is(str);
    }
  }

  @Stellar(name = "DECODE",
      description = "Decodes the passed string with the provided encoding, "
          + " must be one of the encodings returned from LIST_SUPPORTED_ENCODINGS",
      params = {"string - the string to decode",
          "encoding - the encoding to use, must be one of encodings returned from "
              + "LIST_SUPPORTED_ENCODINGS",
          "verify - (optional), true or false to determine if string should be verified as being "
              + "encoded with the passed encoding"
      },
      returns = "The decoded string on success\n"
       + "The original string the string cannot be decoded\n"
       + "null on usage error"
  )
  public static class Decode extends BaseStellarFunction {

    @Override
    public Object apply(List<Object> list) {
      if (list.size() != 2 && list.size() != 3) {
        throw new IllegalStateException(
            "DECODE expects two or three args: [string, encoding] or "
                + "[string, encoding, verify] where encoding is one from "
                + "the supported list");
      }
      Boolean verify = false;
      String str = (String) list.get(0);
      String encoding = (String) list.get(1);

      if (list.size() == 3) {
        verify = (Boolean)list.get(2);
      }
      if (StringUtils.isEmpty(str) || StringUtils.isEmpty(encoding)) {
        return null;
      }

      Encodings enc = null;
      try {
        enc = Encodings.valueOf(encoding.toUpperCase());
      } catch (IllegalArgumentException iae) {
        throw new IllegalStateException(String.format("Encoding %s not supported", encoding), iae);
      }
      return enc.decode(str, verify);
    }
  }

  @Stellar(name = "ENCODE",
      description = "Encodes the passed string with the provided encoding, "
          + " must be one of the encodings returned from LIST_SUPPORTED_ENCODINGS",
      params = {"string - the string to encode",
          "encoding - the encoding to use, must be one of encodings returned from "
              + "LIST_SUPPORTED_ENCODINGS"
      },
      returns = "The encoded string or null on error"
  )
  public static class Encode extends BaseStellarFunction {

    @Override
    public Object apply(List<Object> list) {
      if (list.size() != 2 && list.size() != 3) {
        throw new IllegalStateException(
            "ENCODE expects two or three args: [string, encoding] where encoding is one from "
                + "the supported list");
      }
      String str = (String) list.get(0);
      String encoding = (String) list.get(1);

      if (StringUtils.isEmpty(str) || StringUtils.isEmpty(encoding)) {
        return null;
      }

      Encodings enc = null;
      try {
        enc = Encodings.valueOf(encoding.toUpperCase());
      } catch (IllegalArgumentException iae) {
        throw new IllegalStateException(String.format("Encoding %s not supported", encoding), iae);
      }
      return enc.encode(str);
    }
  }
}
