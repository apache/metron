package org.apache.metron.rest.service.impl;

import org.apache.metron.indexing.dao.IndexDao;
import org.apache.metron.indexing.dao.update.OriginalNotFoundException;
import org.apache.metron.indexing.dao.update.PatchRequest;
import org.apache.metron.indexing.dao.update.ReplaceRequest;
import org.apache.metron.rest.RestException;
import org.apache.metron.rest.service.UpdateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UpdateServiceImpl implements UpdateService {
  private IndexDao dao;

  @Autowired
  public UpdateServiceImpl(IndexDao dao) {
    this.dao = dao;
  }


  @Override
  public void patch(PatchRequest request) throws RestException, OriginalNotFoundException {
    try {
      dao.patch(request, Optional.of(System.currentTimeMillis()));
    } catch (Exception e) {
      throw new RestException(e.getMessage(), e);
    }
  }

  @Override
  public void replace(ReplaceRequest request) throws RestException {
    try {
      dao.replace(request, Optional.of(System.currentTimeMillis()));
    } catch (Exception e) {
      throw new RestException(e.getMessage(), e);
    }
  }
}
