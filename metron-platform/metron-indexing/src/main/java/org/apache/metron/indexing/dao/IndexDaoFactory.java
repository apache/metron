package org.apache.metron.indexing.dao;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

public class IndexDaoFactory {
  public static IndexDao create(String daoImpl, Map<String, Object> globalConfig, AccessConfig config) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
    Class<? extends IndexDao> clazz = (Class<? extends IndexDao>) Class.forName(daoImpl);
    IndexDao instance = clazz.getConstructor().newInstance();
    instance.init(globalConfig, config);
    return instance;
  }
}
