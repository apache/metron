#!/usr/bin/env python
#
#  Licensed to the Apache Software Foundation (ASF) under one or more
#  contributor license agreements.  See the NOTICE file distributed with
#  this work for additional information regarding copyright ownership.
#  The ASF licenses this file to You under the Apache License, Version 2.0
#  (the "License"); you may not use this file except in compliance with
#  the License.  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#
from cabby import create_client

try:

    # create a connection
    client = create_client('{{ opentaxii_domain }}', discovery_path='/services/discovery')

    # iterate through each defined collection
    collections = client.get_collections(uri='/services/collection')
    for collection in collections:

        # how many records in each collection?
        count = client.get_content_count(collection_name=collection.name)
        print "%-50s %-10d" % (collection.name, count.count)

except:

    print "Services not defined"
