package org.apache.metron.rest.service.impl;

import org.apache.metron.common.system.Environment;
import org.apache.metron.indexing.dao.IndexDao;
import org.apache.metron.indexing.dao.search.InvalidSearchException;
import org.apache.metron.indexing.dao.search.SearchRequest;
import org.apache.metron.indexing.dao.search.SearchResponse;
import org.apache.metron.rest.MetronRestConstants;
import org.apache.metron.rest.RestException;
import org.apache.metron.rest.service.GlobalConfigService;
import org.apache.metron.rest.service.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class IndexDaoSearchServiceImpl implements SearchService {
  private IndexDao dao;

  @Autowired
  public IndexDaoSearchServiceImpl(IndexDao dao) {
    this.dao = dao;
  }

  @Override
  public SearchResponse search(SearchRequest searchRequest) throws RestException {
    try {
      return dao.search(searchRequest);
    }
    catch(InvalidSearchException ise) {
      throw new RestException(ise.getMessage(), ise);
    }
  }
}
