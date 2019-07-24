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

package org.apache.metron.indexing.dao.update;

import java.util.*;

import static org.apache.metron.indexing.dao.update.PatchOperation.*;

public enum PatchUtils {
  INSTANCE;

  public static final String OP = "op";
  public static final String VALUE = "value";
  public static final String PATH = "path";
  public static final String FROM = "from";
  private static final String PATH_SEPARATOR = "/";

  public Map<String, Object> applyPatch(List<Map<String, Object>> patches, Map<String, Object> source) {
    Map<String, Object> patchedObject = new HashMap<>(source);
    for(Map<String, Object> patch: patches) {

      // parse patch request parameters
      String operation = (String) patch.get(OP);
      PatchOperation patchOperation;
      try {
        patchOperation = PatchOperation.valueOf(operation.toUpperCase());
      } catch(IllegalArgumentException e) {
        throw new UnsupportedOperationException(String.format("The %s operation is not supported", operation));
      }

      Object value = patch.get(VALUE);
      String path = (String) patch.get(PATH);

      // locate the nested object
      List<String> fieldNames = getFieldNames(path);
      String nestedFieldName = fieldNames.get(fieldNames.size() - 1);
      Map<String, Object> nestedObject = getNestedObject(fieldNames, patchedObject);

      // apply the patch operation
      if (ADD.equals(patchOperation) || REPLACE.equals(patchOperation)) {
        nestedObject.put(nestedFieldName, value);
      } else if (REMOVE.equals(patchOperation)) {
        nestedObject.remove(nestedFieldName);
      } else if (COPY.equals(patchOperation) || MOVE.equals(patchOperation)) {

        // locate the nested object to copy/move the value from
        String from = (String) patch.get(FROM);
        List<String> fromFieldNames = getFieldNames(from);
        String fromNestedFieldName = fromFieldNames.get(fromFieldNames.size() - 1);
        Map<String, Object> fromNestedObject = getNestedObject(fromFieldNames, patchedObject);

        // copy the value
        Object copyValue = fromNestedObject.get(fromNestedFieldName);
        nestedObject.put(nestedFieldName, copyValue);
        if (MOVE.equals(patchOperation)) {

          // remove the from value in case of a move
          nestedObject.remove(fromNestedFieldName);
        }
      } else if (TEST.equals(patchOperation)) {

        Object testValue = nestedObject.get(nestedFieldName);
        if (!Objects.equals(value, testValue)) {
          throw new PatchException(String.format("TEST operation failed: supplied value [%s] != target value [%s]", value, testValue));
        }
      }
    }
    return patchedObject;
  }

  private List<String> getFieldNames(String path) {
    String[] parts = path.split(PATH_SEPARATOR);
    return new ArrayList<>(Arrays.asList(parts).subList(1, parts.length));
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> getNestedObject(List<String> fieldNames, Map<String, Object> patchedObject) {
    Map<String, Object> nestedObject = patchedObject;
    for(int i = 0; i < fieldNames.size() - 1; i++) {
      Object object = nestedObject.get(fieldNames.get(i));
      if (object == null || !(object instanceof Map)) {
        throw new IllegalArgumentException(String.format("Invalid path: /%s", String.join(PATH_SEPARATOR, fieldNames)));
      } else {
        nestedObject = (Map<String, Object>) object;
      }
    }
    return nestedObject;
  }
}
