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

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PatchUtilsTest {

  @Rule
  public final ExpectedException exception = ExpectedException.none();

  @Test
  public void addOperationShouldAddValue() {
    List<Map<String, Object>> patches = new ArrayList<>();
    patches.add(new HashMap<String, Object>() {{
      put(PatchUtils.OP, PatchOperation.ADD.name());
      put(PatchUtils.PATH, "/path");
      put(PatchUtils.VALUE, "value");
    }});

    Map<String, Object> expected = new HashMap<String, Object>() {{
      put("path", "value");
    }};

    Assert.assertEquals(expected, PatchUtils.INSTANCE.applyPatch(patches, new HashMap<>()));
  }

  @Test
  public void removeOperationShouldRemoveValue() {
    List<Map<String, Object>> patches = new ArrayList<>();
    patches.add(new HashMap<String, Object>() {{
      put(PatchUtils.OP, PatchOperation.REMOVE.name());
      put(PatchUtils.PATH, "/remove/path");
    }});

    Map<String, Object> expected = new HashMap<String, Object>() {{
      put("path", "value");
      put("remove", new HashMap<>());
    }};

    Assert.assertEquals(expected, PatchUtils.INSTANCE.applyPatch(patches, new HashMap<String, Object>() {{
      put("path", "value");
      put("remove", new HashMap<String, Object>() {{
        put("path", "removeValue");
      }});
    }}));
  }

  @Test
  public void copyOperationShouldCopyValue() {
    List<Map<String, Object>> patches = new ArrayList<>();
    patches.add(new HashMap<String, Object>() {{
      put(PatchUtils.OP, PatchOperation.COPY.name());
      put(PatchUtils.FROM, "/from");
      put(PatchUtils.PATH, "/path");
    }});

    Map<String, Object> expected = new HashMap<String, Object>() {{
      put("from", "value");
      put("path", "value");
    }};

    Assert.assertEquals(expected, PatchUtils.INSTANCE.applyPatch(patches, new HashMap<String, Object>() {{
      put("from", "value");
    }}));
  }

  @Test
  public void copyOperationShouldCopyNestedValue() {
    List<Map<String, Object>> patches = new ArrayList<>();
    patches.add(new HashMap<String, Object>() {{
      put(PatchUtils.OP, PatchOperation.COPY.name());
      put(PatchUtils.FROM, "/nested/from");
      put(PatchUtils.PATH, "/nested/path");
    }});

    Map<String, Object> expected = new HashMap<String, Object>() {{
      put("nested", new HashMap<String, Object>() {{
        put("from", "value");
        put("path", "value");
      }});
    }};

    Assert.assertEquals(expected, PatchUtils.INSTANCE.applyPatch(patches, new HashMap<String, Object>() {{
      put("nested", new HashMap<String, Object>() {{
        put("from", "value");
      }});
    }}));
  }

  @Test
  public void moveOperationShouldMoveValue() {
    List<Map<String, Object>> patches = new ArrayList<>();
    patches.add(new HashMap<String, Object>() {{
      put(PatchUtils.OP, PatchOperation.MOVE.name());
      put(PatchUtils.FROM, "/from");
      put(PatchUtils.PATH, "/path");
    }});

    Map<String, Object> expected = new HashMap<String, Object>() {{
      put("path", "value");
    }};

    Assert.assertEquals(expected, PatchUtils.INSTANCE.applyPatch(patches, new HashMap<String, Object>() {{
      put("from", "value");
    }}));
  }

  @Test
  public void testOperationShouldCompareStrings() {
    List<Map<String, Object>> patches = new ArrayList<>();
    patches.add(new HashMap<String, Object>() {{
      put(PatchUtils.OP, PatchOperation.TEST.name());
      put(PatchUtils.PATH, "/path");
      put(PatchUtils.VALUE, "value");
    }});

    Map<String, Object> expected = new HashMap<String, Object>() {{
      put("path", "value");
    }};

    Assert.assertEquals(expected, PatchUtils.INSTANCE.applyPatch(patches, new HashMap<String, Object>() {{
      put("path", "value");
    }}));
  }

  @Test
  public void testOperationShouldCompareNumbers() {
    List<Map<String, Object>> patches = new ArrayList<>();
    patches.add(new HashMap<String, Object>() {{
      put(PatchUtils.OP, PatchOperation.TEST.name());
      put(PatchUtils.PATH, "/path");
      put(PatchUtils.VALUE, 100);
    }});

    Map<String, Object> expected = new HashMap<String, Object>() {{
      put("path", 100);
    }};

    Assert.assertEquals(expected, PatchUtils.INSTANCE.applyPatch(patches, new HashMap<String, Object>() {{
      put("path", 100);
    }}));
  }

  @Test
  public void testOperationShouldCompareArrays() {
    List<Map<String, Object>> patches = new ArrayList<>();
    patches.add(new HashMap<String, Object>() {{
      put(PatchUtils.OP, PatchOperation.TEST.name());
      put(PatchUtils.PATH, "/path");
      put(PatchUtils.VALUE, Arrays.asList(1, 2, 3));
    }});

    Map<String, Object> expected = new HashMap<String, Object>() {{
      put("path", Arrays.asList(1, 2, 3));
    }};

    Assert.assertEquals(expected, PatchUtils.INSTANCE.applyPatch(patches, new HashMap<String, Object>() {{
      put("path", Arrays.asList(1, 2, 3));
    }}));
  }

  @Test
  public void testOperationShouldCompareObjects() {
    List<Map<String, Object>> patches = new ArrayList<>();
    patches.add(new HashMap<String, Object>() {{
      put(PatchUtils.OP, PatchOperation.TEST.name());
      put(PatchUtils.PATH, "/path");
      put(PatchUtils.VALUE, new HashMap<String, Object>() {{
        put("key", "value");
      }});
    }});

    Map<String, Object> expected = new HashMap<String, Object>() {{
      put("path", new HashMap<String, Object>() {{
        put("key", "value");
      }});
    }};

    Assert.assertEquals(expected, PatchUtils.INSTANCE.applyPatch(patches, new HashMap<String, Object>() {{
      put("path", new HashMap<String, Object>() {{
        put("key", "value");
      }});
    }}));
  }

  @Test
  public void testOperationShouldThrowExceptionOnFailedCompare() {
    exception.expect(PatchException.class);
    exception.expectMessage("TEST operation failed: supplied value [value1] != target value [value2]");

    List<Map<String, Object>> patches = new ArrayList<>();
    patches.add(new HashMap<String, Object>() {{
      put(PatchUtils.OP, PatchOperation.TEST.name());
      put(PatchUtils.PATH, "/path");
      put(PatchUtils.VALUE, "value1");
    }});

    PatchUtils.INSTANCE.applyPatch(patches, new HashMap<String, Object>() {{
      put("path", "value2");
    }});
  }

  @Test
  public void shouldThrowExceptionOnInvalidPath() {
    exception.expect(IllegalArgumentException.class);
    exception.expectMessage("Invalid path: /missing/path");

    List<Map<String, Object>> patches = new ArrayList<>();
    patches.add(new HashMap<String, Object>() {{
      put(PatchUtils.OP, PatchOperation.REMOVE.name());
      put(PatchUtils.PATH, "/missing/path");
    }});

    PatchUtils.INSTANCE.applyPatch(patches, new HashMap<String, Object>() {{
      put("path", "value");
    }});

  }

  @Test
  public void shouldThrowExceptionOnInvalidOperation() {
    exception.expect(UnsupportedOperationException.class);
    exception.expectMessage("The invalid operation is not supported");

    List<Map<String, Object>> patches = new ArrayList<>();
    patches.add(new HashMap<String, Object>() {{
      put(PatchUtils.OP, "invalid");
      put(PatchUtils.PATH, "/path");
    }});

    PatchUtils.INSTANCE.applyPatch(patches, new HashMap<String, Object>() {{
      put("path", "value");
    }});

  }
}
