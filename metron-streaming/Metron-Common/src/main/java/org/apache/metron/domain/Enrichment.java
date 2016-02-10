package org.apache.metron.domain;

import org.apache.metron.enrichment.interfaces.EnrichmentAdapter;

import java.io.Serializable;
import java.util.List;

public class Enrichment<T extends EnrichmentAdapter> implements Serializable {

  private String name;
  private List<String> fields;
  private T adapter;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public List<String> getFields() {
    return fields;
  }

  public void setFields(List<String> fields) {
    this.fields = fields;
  }

  public T getAdapter() {
    return adapter;
  }

  public void setAdapter(T adapter) {
    this.adapter = adapter;
  }
}
