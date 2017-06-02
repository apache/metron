package org.apache.metron.common.stellar;

public enum ContextVarieties {
  BOOLEAN_AND,
  BOOLEAN_OR, COMPARISON_EXPR;

  public static class Context {
    private ContextVarieties variety;
    public Context(ContextVarieties variety) {
      this.variety = variety;
    }

    public ContextVarieties getVariety() {
      return variety;
    }

    @Override
    public String toString() {
      return "Context{" +
              "variety=" + variety +
              '}';
    }
  }

  public Context create() {
    return new Context(this);
  }
}
