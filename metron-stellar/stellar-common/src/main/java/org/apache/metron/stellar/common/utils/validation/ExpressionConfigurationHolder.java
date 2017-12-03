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

package org.apache.metron.stellar.common.utils.validation;

import java.lang.annotation.IncompleteAnnotationException;
import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.NullArgumentException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.metron.stellar.common.utils.validation.annotations.StellarConfiguration;
import org.apache.metron.stellar.common.utils.validation.annotations.StellarConfigurationList;
import org.apache.metron.stellar.common.utils.validation.annotations.StellarExpressionField;
import org.apache.metron.stellar.common.utils.validation.annotations.StellarExpressionList;
import org.apache.metron.stellar.common.utils.validation.annotations.StellarExpressionMap;

/**
 * <p>
 * {@code ExpressionConfigurationHolder}  is a wrapper class for Objects that represent
 * configurations that have been annotated with the annotations from the validation.annotations
 * package.
 * </p>
 * <p>
 * Holders understand how to discover the stellar expressions contained in them by understanding how
 * to evaluate thier own annotated fields.
 * </p>
 * <p>
 * No knowledge of the implementation of the configuration
 * Object itself is required by this class.  Instead it evaluates it's own member fields for
 * {@code StellarExpressionField}, {@code StellarExpressionList}, {@code StellarExpressionMap} instances.
 * </p>
 * <p>
 * Complex types annotated by {@code StellarConfiguration} or {@code StellarConfigurationList} are
 * treated as children of this class.
 * </p>
 */
public class ExpressionConfigurationHolder implements StellarConfiguredStatementContainer {

  private Object holderObject;
  private String name;
  private String parentName;
  private String fullName;
  private List<ExpressionConfigurationHolder> children = new LinkedList<>();
  private List<Field> expressionList;
  private List<Field> expressionListList;
  private List<Field> expressionMapList;

  /**
   * Constructs a new {@code ExpressionConfigurationHolder}.
   *
   * @param parentName the name of the parent object of this holder
   * @param name the name of to be used for this holder
   * @param holderObject the object being held
   */
  public ExpressionConfigurationHolder(String parentName, String name, Object holderObject) {
    if (holderObject == null) {
      throw new NullArgumentException("holderObject");
    }
    if (StringUtils.isEmpty(name)) {
      throw new NullArgumentException("name");
    }
    this.name = name;
    this.parentName = parentName;

    this.fullName = StringUtils.isEmpty(parentName) ? this.name
        : String.format("%s/%s", this.parentName, this.name);

    this.holderObject = holderObject;
  }

  /**
   * Returns the {@code Object} being held.
   *
   * @return {@code Object}
   */
  public Object getHolderObject() {
    return holderObject;
  }

  /**
   * Returns the name for this {@code ExpressionConfigurationHolder}.
   *
   * @return String value of the name
   */
  public String getName() {
    return name;
  }

  /**
   * Returns the name of the parent of this {@code ExpressionConfigurationHolder}.
   *
   * @return String value of the name
   */
  public String getParentName() {
    return parentName;
  }

  /**
   * Returns the full name of this {@code ExpressionConfigurationHolder}.
   * This represents the name taken together with the parentName, path delimeted by a "/"
   *
   * @return String value of the name
   */
  public String getFullName() {
    return fullName;
  }

  @Override
  public void visit(StatementVisitor visitor, ErrorConsumer errorConsumer) {
    visitExpressions(visitor, errorConsumer);
    visitLists(visitor, errorConsumer);
    visitMaps(visitor, errorConsumer);
    visitChilden(visitor, errorConsumer);
  }

  private void visitExpressions(StatementVisitor visitor, ErrorConsumer errorConsumer) {
    expressionList.forEach((f) -> {
      String thisFullName = String
          .format("%s/%s", getFullName(), f.getAnnotation(StellarExpressionField.class).name());
      try {
        Object thisExpressionObject = FieldUtils.readField(f, holderObject, true);
        if (thisExpressionObject == null || StringUtils.isEmpty(thisExpressionObject.toString())) {
          return;
        }
        visitExpression(thisFullName, thisExpressionObject.toString(), visitor, errorConsumer);
      } catch (IllegalAccessException e) {
        errorConsumer.consume(thisFullName, e);
      }
    });
  }

  private void visitExpression(String expressionName, String expression, StatementVisitor visitor,
      ErrorConsumer errorConsumer) {
    if (StringUtils.isEmpty(expression)) {
      return;
    }
    try {
      visitor.visit(expressionName, expression);
    } catch (Exception e) {
      errorConsumer.consume(expressionName, e);
    }
  }

  private void visitLists(StatementVisitor visitor, ErrorConsumer errorConsumer) {
    expressionListList.forEach((l) -> {
      String thisFullName = String
          .format("%s/%s", getFullName(), l.getAnnotation(StellarExpressionList.class).name());
      try {
        Object possibleIterable = FieldUtils.readField(l, holderObject, true);
        if (possibleIterable == null) {
          return;
        }
        Iterable it = (Iterable) possibleIterable;
        int index = 0;
        for (Object expressionObject : it) {
          String expressionFullName = String.format("%s/%s", thisFullName, index);
          visitExpression(expressionFullName, expressionObject.toString(), visitor, errorConsumer);
          index++;
        }
      } catch (IllegalAccessException e) {
        errorConsumer.consume(thisFullName, e);
      }
    });
  }

  @SuppressWarnings("unchecked")
  private void visitMaps(StatementVisitor visitor, ErrorConsumer errorConsumer) {
    expressionMapList.forEach((f) -> {
      String thisFullName = String
          .format("%s/%s", getFullName(), f.getAnnotation(StellarExpressionMap.class).name());
      try {
        // if we have configured a qualifying field as a type of flag we need to check it
        // for example, if StellarFoo.class.isAssignableFrom(this.foo.getClass()) then
        // bar is a StellarExpressionMap
        if (!StringUtils
            .isEmpty(f.getAnnotation(StellarExpressionMap.class).qualify_with_field())) {
          String fieldName = f.getAnnotation(StellarExpressionMap.class).qualify_with_field();
          Class type = f.getAnnotation(StellarExpressionMap.class).qualify_with_field_type();
          Object theObject = FieldUtils.readField(holderObject, fieldName, true);
          if (theObject == null) {
            errorConsumer.consume(thisFullName,
                new IncompleteAnnotationException(StellarExpressionMap.class, "fieldName"));
            return;
          }
          if (!type.isAssignableFrom(theObject.getClass())) {
            return;
          }
        }

        Map map = (Map) FieldUtils.readField(f, holderObject, true);

        // some maps actually nest the config, so check and dig to get the real map
        String[] innerKeys = f.getAnnotation(StellarExpressionMap.class).inner_map_keys();
        if (innerKeys.length != 0) {
          for (String key : innerKeys) {
            if (StringUtils.isEmpty(key)) {
              return;
            }
            Object innerObject = map.get(key);
            if (innerObject == null) {
              return;
            }
            thisFullName = String.format("%s/%s", thisFullName, key);
            if (!Map.class.isAssignableFrom(innerObject.getClass())) {
              errorConsumer.consume(thisFullName,
                  new Exception("The annotation specified an inner map that was not a map"));
            }
            map = (Map) innerObject;
          }
        }

        final String finalName = thisFullName;
        map.forEach((k, v) -> {
          String mapKeyFullName = String.format("%s/%s", finalName, k.toString());
          if (Map.class.isAssignableFrom(v.getClass())) {
            Map innerMap = (Map) v;
            innerMap.forEach((ik, iv) -> {
              if (iv == null) {
                return;
              }
              visitExpression(String.format("%s/%s", mapKeyFullName, ik.toString()), iv.toString(),
                  visitor, errorConsumer);
            });
            return;
          }
          visitExpression(mapKeyFullName, v.toString(), visitor, errorConsumer);
        });
      } catch (IllegalAccessException e) {
        errorConsumer.consume(thisFullName, e);
      }
    });
  }

  private void visitChilden(StatementVisitor visitor, ErrorConsumer errorConsumer) {
    children.forEach((c) -> c.visit(visitor, errorConsumer));
  }

  /**
   * When {@code discover} is called, the {@code ExpressionConfigurationHolder} evaluates it's
   * members such that it discovers the fields annotated with any of the validation.annotations.
   * <p>
   * The annotated objects found are grouped by annotation type.  Any objects annotated as
   * {@code StellarConfiguration} will have new {@code ExpressionConfigurationHolder} objects
   * created for them, and these objects will be added as children of this
   * {@code ExpressionConfigurationHolder}.
   * </p>
   * <p>
   *   Discovery uses the {@code org.apache.commons.lang3.reflect.FieldUtils} classes for finding
   *   and reading the values of annotated fields.
   *   This class uses fields as opposed to methods to avoid an possible mutation or side effects
   *   from the discovery operation.
   * </p>
   * The same for the contents of an object annotated as a {@code StellarConfigurationList}.
   */
  @Override
  public void discover() throws Exception {
    expressionList = FieldUtils
        .getFieldsListWithAnnotation(holderObject.getClass(), StellarExpressionField.class);
    expressionListList = FieldUtils
        .getFieldsListWithAnnotation(holderObject.getClass(), StellarExpressionList.class);
    expressionMapList = FieldUtils
        .getFieldsListWithAnnotation(holderObject.getClass(), StellarExpressionMap.class);
    List<Field> holderList = FieldUtils
        .getFieldsListWithAnnotation(holderObject.getClass(), StellarConfiguration.class);
    List<Field> holderListList = FieldUtils
        .getFieldsListWithAnnotation(holderObject.getClass(), StellarConfigurationList.class);

    for (Field f : holderList) {
      Object potentialChild = FieldUtils.readField(f, holderObject, true);
      if (potentialChild == null) {
        break;
      }

      ExpressionConfigurationHolder child = new ExpressionConfigurationHolder(getFullName(),
          f.getName(), potentialChild);

      child.discover();
      children.add(child);
    }

    for (Field f : holderListList) {
      String thisFullName = String
          .format("%s/%s", getFullName(), f.getAnnotation(StellarConfigurationList.class).name());
      Object potentialChild = FieldUtils.readField(f, holderObject, true);
      if (potentialChild == null) {
        break;
      }
      if (!Iterable.class.isAssignableFrom(potentialChild.getClass())) {
        break;
      }
      Iterable it = (Iterable) FieldUtils.readField(f, holderObject, true);
      int index = 0;
      for (Object thisHolderObject : it) {
        if (thisHolderObject == null) {
          break;
        }
        ExpressionConfigurationHolder child = new ExpressionConfigurationHolder(thisFullName,
            String.valueOf(index), thisHolderObject);
        index++;
        child.discover();
        children.add(child);
      }
    }
  }
}
