/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.metron.rest.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.metron.common.configuration.SensorParserConfig;
import org.apache.metron.rest.audit.UserRevEntity;
import org.apache.metron.rest.model.SensorParserConfigVersion;
import org.apache.metron.rest.model.SensorParserConfigHistory;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.query.AuditEntity;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class SensorParserConfigHistoryService {

    @Autowired
    private ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private GrokService grokService;

    @Autowired
    private SensorParserConfigService sensorParserConfigService;

    public SensorParserConfigHistory findOne(String name) throws Exception {
        SensorParserConfigHistory sensorParserConfigHistory = null;
        AuditReader reader = AuditReaderFactory.get(entityManager);
        try{
            Object[] latestResults = (Object[]) reader.createQuery().forRevisionsOfEntity(SensorParserConfigVersion.class, false, true)
                    .add(AuditEntity.property("name").eq(name))
                    .addOrder(AuditEntity.revisionNumber().desc())
                    .setMaxResults(1)
                    .getSingleResult();
            SensorParserConfigVersion sensorParserConfigVersion = (SensorParserConfigVersion) latestResults[0];
            sensorParserConfigHistory = new SensorParserConfigHistory();
            sensorParserConfigHistory.setConfig(deserializeSensorParserConfig(sensorParserConfigVersion.getConfig()));

            UserRevEntity latestUserRevEntity = (UserRevEntity) latestResults[1];
            sensorParserConfigHistory.setModifiedBy(latestUserRevEntity.getUsername());
            sensorParserConfigHistory.setModifiedByDate(new DateTime(latestUserRevEntity.getTimestamp()));

            if (grokService.isGrokConfig(sensorParserConfigHistory.getConfig())) {
                grokService.addGrokStatementToConfig(sensorParserConfigHistory.getConfig());
            }

            Object[] firstResults = (Object[]) reader.createQuery().forRevisionsOfEntity(SensorParserConfigVersion.class, false, true)
                    .add(AuditEntity.property("name").eq(name))
                    .addOrder(AuditEntity.revisionNumber().asc())
                    .setMaxResults(1)
                    .getSingleResult();
            UserRevEntity firstUserRevEntity = (UserRevEntity) firstResults[1];
            sensorParserConfigHistory.setCreatedBy(firstUserRevEntity.getUsername());
            sensorParserConfigHistory.setCreatedDate(new DateTime(firstUserRevEntity.getTimestamp()));

        } catch (NoResultException e){
          SensorParserConfig sensorParserConfig = sensorParserConfigService.findOne(name);
          sensorParserConfigHistory = new SensorParserConfigHistory();
          sensorParserConfigHistory.setConfig(sensorParserConfig);
        }
        return sensorParserConfigHistory;
    }

    public List<SensorParserConfigHistory> getAll() throws Exception {
        List<SensorParserConfigHistory> historyList = new ArrayList<>();
        for(String sensorType: sensorParserConfigService.getAllTypes()) {
            historyList.add(findOne(sensorType));
        }
        return historyList;
    }

    public List<SensorParserConfigHistory> history(String name) throws Exception {
        List<SensorParserConfigHistory> historyList = new ArrayList<>();
        AuditReader reader = AuditReaderFactory.get(entityManager);
        List results = reader.createQuery().forRevisionsOfEntity(SensorParserConfigVersion.class, false, true)
                .add(AuditEntity.property("name").eq(name))
                .addOrder(AuditEntity.revisionNumber().asc())
                .getResultList();
        String createdBy = "";
        DateTime createdDate = null;
        for(int i = 0; i < results.size(); i++) {
            Object[] revision = (Object[]) results.get(i);
            SensorParserConfigVersion sensorParserConfigVersion = (SensorParserConfigVersion) revision[0];
            UserRevEntity userRevEntity = (UserRevEntity) revision[1];
            String username = userRevEntity.getUsername();
            DateTime dateTime = new DateTime(userRevEntity.getTimestamp());
            if (i == 0) {
                createdBy = username;
                createdDate = dateTime;
            }
            SensorParserConfigHistory sensorParserInfo = new SensorParserConfigHistory();
            String config = sensorParserConfigVersion.getConfig();
            if (config != null) {
                sensorParserInfo.setConfig(deserializeSensorParserConfig(config));
            } else {
                sensorParserInfo.setConfig(null);
            }
            sensorParserInfo.setCreatedBy(createdBy);
            sensorParserInfo.setCreatedDate(createdDate);
            sensorParserInfo.setModifiedBy(username);
            sensorParserInfo.setModifiedByDate(dateTime);
            historyList.add(0, sensorParserInfo);
        }
        return historyList;
    }

    private SensorParserConfig deserializeSensorParserConfig(String config) throws IOException {
        return objectMapper.readValue(config, new TypeReference<SensorParserConfig>() {});
    }
}
