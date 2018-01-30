package org.apache.metron.solr.schema;

public class FieldType {
  private String name;
  private String solrClass;
  private boolean sortMissingLast;
  private boolean docValues;
  private boolean multiValued;
  private boolean stored;
  private boolean indexed;

  /**
   * Create a new field type.  The default values for the fields are
   * the implicit values from
   * https://lucene.apache.org/solr/guide/6_6/field-type-definitions-and-properties.html#FieldTypeDefinitionsandProperties-FieldTypeDefinitionsinschema.xml
   * @param name
   * @param solrClass
   */
  public FieldType(String name, String solrClass) {
    this(name, solrClass, false, false, false, true, true);
  }

  private FieldType(String name
                         , String solrClass
                         , boolean sortMissingLast
                         , boolean docValues
                         , boolean multiValued
                         , boolean indexed
                         , boolean stored
  ) {
    this.name = name;
    this.solrClass = solrClass;
    this.sortMissingLast = sortMissingLast;
    this.docValues = docValues;
    this.multiValued = multiValued;
    this.indexed = indexed;
    this.stored = stored;
  }

  public String getName() {
    return name;
  }

  public FieldType sortMissingLast() {
    this.sortMissingLast = true;
    return this;
  }

  public FieldType docValues() {
    this.docValues = true;
    return this;
  }

  public FieldType multiValued() {
    this.multiValued= true;
    return this;
  }

  public FieldType indexed() {
    this.indexed = true;
    return this;
  }

  public FieldType stored() {
    this.stored = true;
    return this;
  }
  @Override
  public String toString() {
    return String.format("<fieldType name=\"%s\" " +
                    "stored=\"%s\" " +
                    "indexed=\"%s\" " +
                    "multiValued=\"%s\" " +
                    "class=\"%s\" " +
                    "sortMissingLast=\"%s\"" +
                    "docValues=\"%s\"" +
                    "/>"
            , name, stored + "", indexed + "", multiValued + "", solrClass + "", sortMissingLast + "", docValues + ""
    );
  }
}
