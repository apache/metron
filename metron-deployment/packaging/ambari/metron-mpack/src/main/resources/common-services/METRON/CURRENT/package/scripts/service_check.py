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

from resource_management.libraries.script import Script

from indexing_commands import IndexingCommands
from parser_commands import ParserCommands


class ServiceCheck(Script):
    def service_check(self, env):
        from params import params
        parsercommands = ParserCommands(params)
        indexingcommands = IndexingCommands(params)
        all_found = parsercommands.topologies_running(env) and indexingcommands.is_topology_active(env)
        if all_found:
            exit(0)
        else:
            exit(1)


if __name__ == "__main__":
    ServiceCheck().execute()
