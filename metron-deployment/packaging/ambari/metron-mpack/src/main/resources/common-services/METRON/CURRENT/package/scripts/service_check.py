#!/usr/bin/env python
"""
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

"""
from __future__ import print_function

from resource_management.core.logger import Logger
from resource_management.libraries.script import Script

from parser_commands import ParserCommands
from enrichment_commands import EnrichmentCommands
from indexing_commands import IndexingCommands
from profiler_commands import ProfilerCommands
from rest_commands import RestCommands
from management_ui_commands import ManagementUICommands
from alerts_ui_commands import AlertsUICommands

class ServiceCheck(Script):

    def service_check(self, env):
        from params import params

        # check the parsers
        Logger.info("Performing Parser service check")
        parser_cmds = ParserCommands(params)
        parser_cmds.service_check(env)

        # check enrichment
        Logger.info("Performing Enrichment service check")
        enrichment_cmds = EnrichmentCommands(params)
        enrichment_cmds.service_check(env)

        # check indexing
        Logger.info("Performing Indexing service check")
        indexing_cmds = IndexingCommands(params)
        indexing_cmds.service_check(env)

        # check the profiler
        Logger.info("Performing Profiler service check")
        profiler_cmds = ProfilerCommands(params)
        profiler_cmds.service_check(env)

        # check the rest api
        Logger.info("Performing REST application service check")
        rest_cmds = RestCommands(params)
        rest_cmds.service_check(env)

        # check the management UI
        Logger.info("Performing Management UI service check")
        mgmt_cmds = ManagementUICommands(params)
        mgmt_cmds.service_check(env)

        # check the alerts UI
        Logger.info("Performing Alerts UI service check")
        alerts_cmds = AlertsUICommands(params)
        alerts_cmds.service_check(env)

        Logger.info("Metron service check completed successfully")
        exit(0)


if __name__ == "__main__":
    ServiceCheck().execute()
