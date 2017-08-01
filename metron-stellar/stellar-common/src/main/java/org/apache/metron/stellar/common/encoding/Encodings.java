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

package org.apache.metron.stellar.common.encoding;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base32;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.BinaryCodec;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang.StringUtils;

/**
 * Endodings utility enum for Stellar supported encodings.
 */
public enum Encodings {
  BASE32((possible) -> new Base32().isInAlphabet(possible),
      (possible) -> new String(new Base32().decode(possible), StandardCharsets.UTF_8),
      (possible) -> new String( new Base32().encode(possible.getBytes(StandardCharsets.UTF_8)),StandardCharsets.UTF_8)),
  BASE32HEX((possible) -> new Base32(true).isInAlphabet(possible),
      (possible) -> new String(new Base32(true).decode(possible.getBytes(StandardCharsets.UTF_8)),
          StandardCharsets.UTF_8),
      (possible) -> new String(new Base32(true).encode(possible.getBytes(StandardCharsets.UTF_8)),
          StandardCharsets.UTF_8)),
  BASE64((possible) -> Base64.isBase64(possible),
      (possible) -> new String(new Base64().decode(possible.getBytes(StandardCharsets.UTF_8)),
          StandardCharsets.UTF_8),
      (possible) -> new String(new Base64().encode(possible.getBytes(StandardCharsets.UTF_8)),
          StandardCharsets.UTF_8)),
  BINARY((possible) -> {
      for (byte b : possible.getBytes(StandardCharsets.UTF_8)) {
        if ((b != 48 && b != 49)) {
          return false;
        }
      }
      return true;
    },
      (possible) -> {
        String str = new String(BinaryCodec.fromAscii(possible.getBytes(StandardCharsets.UTF_8)),
            StandardCharsets.UTF_8);
        if (StringUtils.isEmpty(str.trim())) {
          return possible;
        }
        return str;
      },
      (possible) -> BinaryCodec.toAsciiString(possible.getBytes(StandardCharsets.UTF_8))),
  HEX((possible) -> {
      try {
        Hex hex = new Hex(StandardCharsets.UTF_8);
        hex.decode(possible.getBytes(StandardCharsets.UTF_8));
        return true;
      } catch (DecoderException e) {
        return false;
      }
    },
      (possible) -> {
        try {
          Hex hex = new Hex(StandardCharsets.UTF_8);
          return new String(hex.decode(possible.getBytes(StandardCharsets.UTF_8)),
              StandardCharsets.UTF_8);
        } catch (DecoderException e) {
          return possible;
        }
      },
      (possible) -> new String(new Hex(StandardCharsets.UTF_8).encode(possible.getBytes(StandardCharsets.UTF_8)),StandardCharsets.UTF_8));

  Predicate<String> is;
  Function<String, String> decode;
  Function<String, String> encode;

  /**
   * Create a specialed Endodings enum member.
   *
   * @param is function for detecting
   * @param decode funtion for decoding
   */
  Encodings(Predicate<String> is, Function<String, String> decode, Function<String,String> encode) {
    this.is = is;
    this.decode = decode;
    this.encode = encode;
  }

  public static final List<String> SUPPORTED_LIST = new ArrayList<>(Arrays.asList(BASE32.name(),
      BASE32HEX.name(), BASE64.name(), BINARY.name() , HEX.name()));

  /**
   * Determines if the passed String is encoded in this encoding.
   * A given String may be compatible with the encoding, but not actually encoded.
   *
   * @param possible the String to test
   * @return true or false
   */
  public boolean is(String possible) {
    return is.test(possible);
  }

  /**
   * Attempts to decode a given String without verification.
   * Any failure or compatibility issues will result in the original
   * String being returned.
   *
   * @param encoded the String to decode
   * @return The String decoded, or the original String
   */
  public String decode(String encoded) {
    return decode(encoded, false);
  }

  /**
   * Attempts to decode a given String, optionally verifying first.
   * Any failure or compatibility issues will result in the original
   * String being returned.
   *
   * @param encoded the String to decode
   * @param verify flag to perform verification
   * @return The String decoded, or the original String
   */
  public String decode(String encoded, boolean verify) {
    if (verify) {
      if (is.test(encoded)) {
        return decode.apply(encoded);
      } else {
        return encoded;
      }
    }
    return decode.apply(encoded);
  }

  /**
   * Encodes the given String
   * @param toEncode
   * @return an encoded String
   */
  public String encode(String toEncode) {
    return encode.apply(toEncode);
  }
}
