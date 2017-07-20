package org.apache.metron.rest.service;

import org.apache.metron.indexing.dao.update.OriginalNotFoundException;
import org.apache.metron.indexing.dao.update.PatchRequest;
import org.apache.metron.indexing.dao.update.ReplaceRequest;
import org.apache.metron.rest.RestException;

public interface UpdateService {

  void patch(PatchRequest request) throws RestException, OriginalNotFoundException;
  void replace(ReplaceRequest request) throws RestException;
}
