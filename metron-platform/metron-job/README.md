<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->

# Metron Job

This module holds abstractions for creating jobs. The main actors are a JobManager interface and subsequent implementation, InMemoryJobManger, that handles maintaining a cache of running and completed Statusable jobs. Each Statusable
can provide a Finalizer implementation that should be executed on completion of the underlying job. Successful jobs should return a Pageable object that allow consumers to request results on a per-page basis.

## Job State Statechart

![Job State Statechart](metron-job_state_statechart_diagram.svg)
