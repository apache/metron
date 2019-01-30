<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->
# `MessageParser` implementation notes 


1. Supporting multiple JSONObject returns from a single byte[]
The original `MessageParser` interface supported parsing a message and returning a `List<JSONObject>`.  Therefore explicitly supporting multiple messages from one input.
While this is fine, it only allows for the complete failure of a message for any reason.  There can only be one exception thrown.  This means that if there _are_ multiple messages in the buffer, any one failure will necessarily fail all of them.
To improve on this situation, a new method was added to the `MessageParser` interface (with a default implementation), that introduces a return type to provide not only the JSONObjects produced, but also a `Map` of messages -> throwable.

To support this in your parser, you should:

- Implement the new method

```java
 @Override
  public Optional<MessageParserResult<JSONObject>> parseOptionalResult(byte[] rawMessage)
```

- Implement the original `List<JSONObject> parse(byte[] message)` to delegate to that method such as below:

```java
 @Override
  public List<JSONObject> parse(byte[] rawMessage) {
    Optional<MessageParserResult<JSONObject>> resultOptional = parseOptionalResult(rawMessage);
    if (!resultOptional.isPresent()) {
      return Collections.EMPTY_LIST;
    }
    Map<Object,Throwable> errors = resultOptional.get().getMessageThrowables();
    if (!errors.isEmpty()) {
      throw new RuntimeException(errors.entrySet().iterator().next().getValue());
    }

    return resultOptional.get().getMessages();
  }
```

- You *may* want to govern treating the incoming buffer as multiline or not by adding a configuration option for your parser, such as `"multiline":"true"|"false"`

- See the org.apache.metron.parsers.GrokParser for an example of this implementation.

The Metron system itself will call the new `parseOptionalResult` method during processing.  The default implementation in the interface handles backwards compatability with previous implementations.
