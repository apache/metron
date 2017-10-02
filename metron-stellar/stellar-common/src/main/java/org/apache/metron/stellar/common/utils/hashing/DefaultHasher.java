/*
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
package org.apache.metron.stellar.common.utils.hashing;

import com.google.common.base.Joiner;
import org.apache.commons.codec.BinaryEncoder;
import org.apache.commons.codec.Charsets;
import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.metron.stellar.common.utils.ConversionUtils;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.*;
import java.util.function.Function;

public class DefaultHasher implements Hasher {

  public enum Config implements EnumConfigurable {
    CHARSET("charset"),
    ;
    private String key;
    Config(String key) {
      this.key = key;
    }

    @Override
    public String getKey() {
      return key;
    }

  }

  private String algorithm;
  private BinaryEncoder encoder;
  private Charset charset;

  /**
   * Builds a utility to hash values based on a given algorithm.
   * @param algorithm The algorithm used when hashing a value.
   * @param encoder The encoder to use to encode the hashed value.
   * @param charset The charset that will be used during hashing and encoding.
   * @see java.security.Security
   * @see java.security.MessageDigest
   */
  public DefaultHasher(final String algorithm, final BinaryEncoder encoder, final Charset charset) {
    this.algorithm = algorithm;
    this.encoder = encoder;
    this.charset = charset;
  }

  /**
   * Builds a utility to hash values based on a given algorithm. Uses {@link StandardCharsets#UTF_8} for encoding.
   * @param algorithm The algorithm used when hashing a value.
   * @param encoder The encoder to use to encode the hashed value.
   * @see java.security.Security
   * @see java.security.MessageDigest
   */
  public DefaultHasher(final String algorithm, final BinaryEncoder encoder) {
    this.algorithm = algorithm;
    this.encoder = encoder;
    this.charset = StandardCharsets.UTF_8;
  }

  /**
   * Builds a utility to hash values based on a given algorithm. Uses {@link StandardCharsets#UTF_8} for encoding.
   * @param algorithm The algorithm used when hashing a value.
   * @see java.security.Security
   * @see java.security.MessageDigest
   */
  public DefaultHasher(final String algorithm) {
    this(algorithm, new Hex(StandardCharsets.UTF_8));
  }

  /**
   * {@inheritDoc}
   *
   * Returns a hash which has been encoded using the supplied encoder. If input is null then a string
   * containing all '0' will be returned. The length of the string is determined by the hashing algorithm
   * used.
   * @param toHash The value to hash.
   * @return A hash of {@code toHash} that has been encoded.
   * @throws EncoderException If unable to encode the hash then this exception occurs.
   * @throws NoSuchAlgorithmException If the supplied algorithm is not known.
   */
  @Override
  public String getHash(final Object toHash) throws EncoderException, NoSuchAlgorithmException {
    final MessageDigest messageDigest = MessageDigest.getInstance(algorithm);

    if (toHash == null) {
      return StringUtils.repeat("00", messageDigest.getDigestLength());
    } else if (toHash instanceof String) {
      return getHash(messageDigest, toHash.toString().getBytes(charset));
    } else if (toHash instanceof Serializable) {
      final byte[] serialized = SerializationUtils.serialize((Serializable) toHash);
      return getHash(messageDigest, serialized);
    }

    return null;
  }

  private String getHash(final MessageDigest messageDigest, final byte[] toHash) throws EncoderException {
    messageDigest.update(toHash);
    final byte[] encode = encoder.encode(messageDigest.digest());

    return new String(encode, charset);
  }

  @Override
  public void configure(Optional<Map<String, Object>> config) {
    if(config.isPresent() && !config.get().isEmpty()) {
      charset = Config.CHARSET.get(config.get()
              , o -> {
                String charset = ConversionUtils.convert(o, String.class);
                if(charset != null) {
                  Charset set = Charset.forName(charset);
                  return set;
                }
                return null;
              }
      ).orElse(charset);
    }
  }

  public static final Set<String> supportedHashes() {
    return new HashSet<>(Security.getAlgorithms("MessageDigest"));
  }

}
