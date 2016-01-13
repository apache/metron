package org.apache.metron.parsing.parsers;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class MetronGarbage implements Serializable {

	private static final long serialVersionUID = -7158895945268018603L;
	private List<String> toRemove;
	  private Map<String, Object> toRename;

	  /**
	   * Create a new {@code Garbage} object.
	   */
	  public MetronGarbage() {

	    toRemove = new ArrayList<String>();
	    toRename = new TreeMap<String, Object>();
	    /** this is a default value to remove */
	    toRemove.add("UNWANTED");
	  }

	  /**
	   * Set a new name to be change when exporting the final output.
	   *
	   * @param origin : original field name
	   * @param value : New field name to apply
	   */
	  public void addToRename(String origin, Object value) {
	    if (origin == null || value == null) {
	      return;
	    }

	    if (!origin.isEmpty() && !value.toString().isEmpty()) {
	      toRename.put(origin, value);
	    }
	  }

	  /**
	   * Set a field to be remove when exporting the final output.
	   *
	   * @param name of the field to remove
	   */
	  public void addToRemove(String name) {
	    if (name == null) {
	      return;
	    }

	    if (!name.isEmpty()) {
	      toRemove.add(name);
	    }
	  }

	  /**
	   * Set a list of field name to be remove when exporting the final output.
	   *
	   * @param lst
	   */
	  public void addToRemove(List<String> lst) {
	    if (lst == null) {
	      return;
	    }

	    if (!lst.isEmpty()) {
	      toRemove.addAll(lst);
	    }
	  }

	  /**
	   * Remove from the map the unwilling items.
	   *
	   * @param map to clean
	   * @return nb of deleted item
	   */
	  public int remove(Map<String, Object> map) {
	    int item = 0;

	    if (map == null) {
	      return item;
	    }

	    if (map.isEmpty()) {
	      return item;
	    }

	    for (Iterator<Map.Entry<String, Object>> it = map.entrySet().iterator(); it.hasNext();) {
	      Map.Entry<String, Object> entry = it.next();
	      for (int i = 0; i < toRemove.size(); i++) {
	        if (entry.getKey().equals(toRemove.get(i))) {
	          it.remove();
	          item++;
	        }
	      }
	    }
	    return item;
	  }

	  /**
	   * Rename the item from the map.
	   *
	   * @param map
	   * @return nb of renamed items
	   */
	  public int rename(Map<String, Object> map) {
	    int item = 0;

	    if (map == null) {
	      return item;
	    }

	    if (map.isEmpty() || toRename.isEmpty()) {
	      return item;
	    }

	    for (Iterator<Map.Entry<String, Object>> it = toRename.entrySet().iterator(); it.hasNext();) {
	      Map.Entry<String, Object> entry = it.next();
	      if (map.containsKey(entry.getKey())) {
	        Object obj = map.remove(entry.getKey());
	        map.put(entry.getValue().toString(), obj);
	        item++;
	      }
	    }
	    return item;
	  }

	}