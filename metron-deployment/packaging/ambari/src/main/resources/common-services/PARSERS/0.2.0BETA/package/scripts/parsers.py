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


def parsers_init():
    import params
    params.HdfsResource(params.metron_apps_dir,
                        type="directory",
                        action="create_on_execute",
                        owner=params.metron_user,
                        mode=0775,
                        source=params.local_grok_patterns_dir)


def get_parsers(params):
    return params.parsers.replace(' ', '').split(',')
