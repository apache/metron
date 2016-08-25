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
package org.apache.metron.maas.util;

import com.google.common.collect.Iterables;
import com.google.common.io.CharStreams;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public enum RESTUtil {
  INSTANCE;
  public static ThreadLocal<HttpClient> CLIENT = new ThreadLocal<HttpClient>() {

    @Override
    protected HttpClient initialValue() {
      //TODO: Figure out connection management
      return new DefaultHttpClient();
    }
  };

  public String getRESTJSONResults(URL endpointUrl, Map<String, String> getArgs) throws IOException, URISyntaxException { String encodedParams = encodeParams(getArgs);
    HttpGet get = new HttpGet(appendToUrl(endpointUrl, encodedParams).toURI());
    get.addHeader("accept", "application/json");
    HttpResponse response = CLIENT.get().execute(get);

    if (response.getStatusLine().getStatusCode() != 200) {
      throw new IllegalStateException("Failed : HTTP error code : "
              + response.getStatusLine().getStatusCode());
    }

    return new BufferedReader(new InputStreamReader(response.getEntity().getContent()))
            .lines().collect(Collectors.joining("\n"));
  }
  public URL appendToUrl(URL endpointUrl, String params) throws MalformedURLException {
    return new URL(endpointUrl.toString() + "?" + params);
  }
  public String encodeParams(Map<String, String> params) {
    Iterable<NameValuePair> nvp = Iterables.transform(params.entrySet()
            , kv -> new BasicNameValuePair(kv.getKey(), kv.getValue())
    );

    return URLEncodedUtils.format(nvp, Charset.defaultCharset());
  }
}
