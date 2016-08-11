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
package org.apache.metron.maas.service.yarn;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.io.DataOutputBuffer;
import org.apache.hadoop.security.Credentials;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.security.token.Token;
import org.apache.hadoop.yarn.api.ApplicationConstants;
import org.apache.hadoop.yarn.api.records.Container;
import org.apache.hadoop.yarn.api.records.ContainerStatus;
import org.apache.hadoop.yarn.api.records.timeline.TimelineEntity;
import org.apache.hadoop.yarn.api.records.timeline.TimelineEvent;
import org.apache.hadoop.yarn.api.records.timeline.TimelinePutResponse;
import org.apache.hadoop.yarn.client.api.TimelineClient;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.apache.hadoop.yarn.security.AMRMTokenIdentifier;
import org.apache.metron.maas.service.ApplicationMaster;
import org.apache.metron.maas.service.ContainerEvents;

import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.nio.ByteBuffer;
import java.security.PrivilegedExceptionAction;
import java.util.Iterator;

public enum YarnUtils {
  INSTANCE;
  private static final Log LOG = LogFactory.getLog(YarnUtils.class);
  public UserGroupInformation createUserGroup(Credentials credentials) throws IOException {
    credentials = credentials == null? UserGroupInformation.getCurrentUser().getCredentials():credentials;
    String appSubmitterUserName =
            System.getenv(ApplicationConstants.Environment.USER.name());
    UserGroupInformation appSubmitterUgi =
            UserGroupInformation.createRemoteUser(appSubmitterUserName);
    appSubmitterUgi.addCredentials(credentials);
    return appSubmitterUgi;
  }

  public ByteBuffer tokensFromCredentials(Credentials credentials) throws IOException {
    // Note: Credentials, Token, UserGroupInformation, DataOutputBuffer class
    // are marked as LimitedPrivate
    credentials = credentials == null? UserGroupInformation.getCurrentUser().getCredentials():credentials;
    DataOutputBuffer dob = new DataOutputBuffer();
    credentials.writeTokenStorageToStream(dob);
    // Now remove the AM->RM token so that containers cannot access it.
    Iterator<Token<?>> iter = credentials.getAllTokens().iterator();
    LOG.info("Executing with tokens:");
    while (iter.hasNext()) {
      Token<?> token = iter.next();
      LOG.info(token);
      if (token.getKind().equals(AMRMTokenIdentifier.KIND_NAME)) {
        iter.remove();
      }
    }
    return ByteBuffer.wrap(dob.getData(), 0, dob.getLength());
  }
  public void publishContainerEndEvent(
          final TimelineClient timelineClient, ContainerStatus container,
          String domainId, UserGroupInformation ugi) {
    final TimelineEntity entity = new TimelineEntity();
    entity.setEntityId(container.getContainerId().toString());
    entity.setEntityType(ApplicationMaster.DSEntity.DS_CONTAINER.toString());
    entity.setDomainId(domainId);
    entity.addPrimaryFilter("user", ugi.getShortUserName());
    TimelineEvent event = new TimelineEvent();
    event.setTimestamp(System.currentTimeMillis());
    event.setEventType(ContainerEvents.CONTAINER_END.toString());
    event.addEventInfo("State", container.getState().name());
    event.addEventInfo("Exit Status", container.getExitStatus());
    entity.addEvent(event);
    try {
      timelineClient.putEntities(entity);
    } catch (YarnException | IOException e) {
      LOG.error("Container end event could not be published for "
              + container.getContainerId().toString(), e);
    }
  }
  public void publishApplicationAttemptEvent(
          final TimelineClient timelineClient, String appAttemptId,
          ContainerEvents appEvent, String domainId, UserGroupInformation ugi) {
    final TimelineEntity entity = new TimelineEntity();
    entity.setEntityId(appAttemptId);
    entity.setEntityType(ApplicationMaster.DSEntity.DS_APP_ATTEMPT.toString());
    entity.setDomainId(domainId);
    entity.addPrimaryFilter("user", ugi.getShortUserName());
    TimelineEvent event = new TimelineEvent();
    event.setEventType(appEvent.toString());
    event.setTimestamp(System.currentTimeMillis());
    entity.addEvent(event);
    try {
      timelineClient.putEntities(entity);
    } catch (YarnException | IOException e) {
      LOG.error("App Attempt "
              + (appEvent.equals(ContainerEvents.APP_ATTEMPT_START) ? "start" : "end")
              + " event could not be published for "
              + appAttemptId.toString(), e);
    }
  }
  public void publishContainerStartEvent(
          final TimelineClient timelineClient, Container container, String domainId,
          UserGroupInformation ugi) {
    final TimelineEntity entity = new TimelineEntity();
    entity.setEntityId("" + container.getId());
    entity.setEntityType(ApplicationMaster.DSEntity.DS_CONTAINER.toString());
    entity.setDomainId(domainId);
    entity.addPrimaryFilter("user", ugi.getShortUserName());
    TimelineEvent event = new TimelineEvent();
    event.setTimestamp(System.currentTimeMillis());
    event.setEventType(ContainerEvents.CONTAINER_START.toString());
    event.addEventInfo("Node", container.getNodeId().toString());
    event.addEventInfo("Resources", container.getResource().toString());
    entity.addEvent(event);

    try {
      ugi.doAs(new PrivilegedExceptionAction<TimelinePutResponse>() {
        @Override
        public TimelinePutResponse run() throws Exception {
          return timelineClient.putEntities(entity);
        }
      });
    } catch (Exception e) {
      LOG.error("Container start event could not be published for "
                      + container.getId().toString(),
              e instanceof UndeclaredThrowableException ? e.getCause() : e);
    }
  }
}
