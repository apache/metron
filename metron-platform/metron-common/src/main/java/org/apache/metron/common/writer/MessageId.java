package org.apache.metron.common.writer;

import java.util.Objects;

public class MessageId {

  private String id;

  public MessageId(String id) {
    this.id = id;
  }

  public String getId() {
    return id;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    MessageId messageId = (MessageId) o;
    return Objects.equals(id, messageId.id);
  }

  @Override
  public int hashCode() {

    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return "MessageId{" +
            "id='" + id + '\'' +
            '}';
  }


}
