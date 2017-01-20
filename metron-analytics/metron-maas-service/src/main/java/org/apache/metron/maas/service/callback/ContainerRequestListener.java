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
package org.apache.metron.maas.service.callback;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.yarn.api.records.*;
import org.apache.hadoop.yarn.client.api.AMRMClient;
import org.apache.hadoop.yarn.client.api.TimelineClient;
import org.apache.hadoop.yarn.client.api.async.AMRMClientAsync;
import org.apache.hadoop.yarn.client.api.async.NMClientAsync;
import org.apache.metron.maas.discovery.ServiceDiscoverer;
import org.apache.metron.maas.config.ModelRequest;
import org.apache.metron.maas.service.ContainerTracker;
import org.apache.metron.maas.service.yarn.YarnUtils;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class ContainerRequestListener implements AMRMClientAsync.CallbackHandler, NMClientAsync.CallbackHandler  {

  private static final Log LOG = LogFactory.getLog(ContainerRequestListener.class);
  private ContainerTracker state;
  private AMRMClientAsync<AMRMClient.ContainerRequest> amRMClient;
  @VisibleForTesting
  private UserGroupInformation appSubmitterUgi;
  private String domainId = null;
  private ConcurrentMap<ContainerId, Container> containers = new ConcurrentHashMap<>();
  @VisibleForTesting
  private TimelineClient timelineClient;
  private NMClientAsync nmClient;
  private ServiceDiscoverer serviceDiscoverer;
  public ContainerRequestListener( TimelineClient timelineClient
                                 , UserGroupInformation appSubmitterUgi
                                 , String domainId
                                 , int minMemorySize
                                 )
  {
    this.domainId = domainId;
    this.appSubmitterUgi = appSubmitterUgi;
    this.timelineClient = timelineClient;
    state = new ContainerTracker(minMemorySize);
  }

  public void initialize(AMRMClientAsync<AMRMClient.ContainerRequest> amRMClient
                        , NMClientAsync nmClient
                        , ServiceDiscoverer serviceDiscoverer
                        )
  {
    this.nmClient = nmClient;
    this.amRMClient = amRMClient;
    this.serviceDiscoverer = serviceDiscoverer;
  }



  public void removeContainers(int number, ModelRequest request) {
    int i = 0;
    for(Container c : state.getList(request)) {
      if(i < number) {
        amRMClient.releaseAssignedContainer(c.getId());
        nmClient.stopContainerAsync(c.getId(), c.getNodeId());
      }
      else {
        break;
      }
      i++;
    }
  }

  public ContainerTracker getContainerState() {
    return state;
  }

  private void removeContainer(ContainerId id) {
    containers.remove(id);
    state.removeContainer(id);
  }

  public void requestContainers(int number, Resource characteristic) {
    Priority pri = Priority.newInstance(0);
    state.getQueue(characteristic);
    AMRMClient.ContainerRequest request = new AMRMClient.ContainerRequest(characteristic, null, null, pri, true);
    for(int i = 0;i < number;++i) {
      amRMClient.addContainerRequest(request);
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public void onContainersCompleted(List<ContainerStatus> completedContainers) {
    LOG.info("Got response from RM for container ask, completedCnt="
            + completedContainers.size());
    for (ContainerStatus containerStatus : completedContainers) {
      LOG.info("Got container status for containerID="
              + containerStatus.getContainerId() + ", state="
              + containerStatus.getState() + ", exitStatus="
              + containerStatus.getExitStatus() + ", diagnostics="
              + containerStatus.getDiagnostics());
      removeContainer(containerStatus.getContainerId());
      LOG.info("REMOVING CONTAINER " + containerStatus.getContainerId());
      serviceDiscoverer.unregisterByContainer(containerStatus.getContainerId() + "");
      // non complete containers should not be here
      assert (containerStatus.getState() == ContainerState.COMPLETE);
      // increment counters for completed/failed containers
      int exitStatus = containerStatus.getExitStatus();
      if (0 != exitStatus) {
        // container failed
        if (ContainerExitStatus.ABORTED != exitStatus) {
          // shell script failed
          // counts as completed
        } else {
          // container was killed by framework, possibly preempted
          // we should re-try as the container was lost for some reason
          // we do not need to release the container as it would be done
          // by the RM
        }
      } else {
        // nothing to do
        // container completed successfully
        LOG.info("Container completed successfully." + ", containerId="
                + containerStatus.getContainerId());
      }
      if(timelineClient != null) {
        YarnUtils.INSTANCE.publishContainerEndEvent(
                timelineClient, containerStatus, domainId, appSubmitterUgi);
      }
    }
  }

  public BlockingQueue<Container> getContainers(Resource resource) {
    return state.getQueue(resource);
  }

  @Override
  public void onContainersAllocated(List<Container> allocatedContainers) {
    LOG.info("Got response from RM for container ask, allocatedCnt="
            + allocatedContainers.size());
    for (Container allocatedContainer : allocatedContainers) {
      containers.put(allocatedContainer.getId(), allocatedContainer);
      state.registerContainer(allocatedContainer.getResource(), allocatedContainer);
      LOG.info("Launching shell command on a new container."
              + ", containerId=" + allocatedContainer.getId()
              + ", containerNode=" + allocatedContainer.getNodeId().getHost()
              + ":" + allocatedContainer.getNodeId().getPort()
              + ", containerNodeURI=" + allocatedContainer.getNodeHttpAddress()
              + ", containerResourceMemory="
              + allocatedContainer.getResource().getMemory()
              + ", containerResourceVirtualCores="
              + allocatedContainer.getResource().getVirtualCores());
    }
  }



  @Override
  public void onShutdownRequest() {
  }

  @Override
  public void onNodesUpdated(List<NodeReport> updatedNodes) {}

  @Override
  public float getProgress() {
    // set progress to deliver to RM on next heartbeat
    float progress = 0;
    return progress;
  }

  @Override
  public void onError(Throwable e) {
    LOG.error(e.getMessage(), e);
  }

  @Override
  public void onContainerStopped(ContainerId containerId) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Succeeded to stop Container " + containerId);
    }
    if(containerId == null) {
      LOG.error("Container stopped callback: Empty container ID!");
      throw new IllegalStateException("onContainerStopped returned null container ID!");
    }
    serviceDiscoverer.unregisterByContainer(containerId.getContainerId() + "");
    removeContainer(containerId);
  }

    @Override
    public void onContainerStatusReceived(ContainerId containerId,
                                          ContainerStatus containerStatus) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Container Status: id=" + containerId + ", status=" +
                containerStatus);
      }
    }

    @Override
    public void onContainerStarted(ContainerId containerId,
                                   Map<String, ByteBuffer> allServiceResponse) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Succeeded to start Container " + containerId);
      }
      Container container = containers.get(containerId);
      if (container != null) {
        nmClient.getContainerStatusAsync(containerId, container.getNodeId());
      }
      if(timelineClient != null && container != null) {
        YarnUtils.INSTANCE.publishContainerStartEvent( timelineClient, container, domainId, appSubmitterUgi);
      }
    }

    @Override
    public void onStartContainerError(ContainerId containerId, Throwable t) {
      LOG.error("Failed to start Container " + containerId);
      serviceDiscoverer.unregisterByContainer(containerId.getContainerId() + "");
      removeContainer(containerId);
    }

    @Override
    public void onGetContainerStatusError(
            ContainerId containerId, Throwable t) {
      LOG.error("Failed to query the status of Container " + containerId);
    }

    @Override
    public void onStopContainerError(ContainerId containerId, Throwable t) {
      LOG.error("Failed to stop Container " + containerId);
      serviceDiscoverer.unregisterByContainer(containerId.getContainerId() + "");
      removeContainer(containerId);
    }
}
