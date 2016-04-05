
#  Licensed to the Apache Software Foundation (ASF) under one
#  or more contributor license agreements.  See the NOTICE file
#  distributed with this work for additional information
#  regarding copyright ownership.  The ASF licenses this file
#  to you under the Apache License, Version 2.0 (the
#  "License"); you may not use this file except in compliance
#  with the License.  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.

import json
import sys

def merge_dicts(*dict_args):
    result = {}
    for dictionary in dict_args:
        result.update(dictionary)
    return result

def get_statement(component, version, license, location):
    #This product bundles SuperWidget 1.2.3, which is available under a
    #"3-clause BSD" license.
    s = "This product bundles " + component + " " + version \
      + ", which is available under a \"" + license + "\" license. " \
      + "For details, see " + location + "/LICENSE." + component
    return s

licenses = {}
for i in xrange(1, len(sys.argv)):
    with open(sys.argv[i]) as f:
        license_summary = f.read()
        licenses = merge_dicts(licenses, json.loads(license_summary))
for component, value in licenses.iteritems():
    if not(value['license'].startswith("Apache")):
        print get_statement(component, value['version'], value['license'], value['location'])

