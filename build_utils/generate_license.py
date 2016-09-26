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

import sets
import sys

category_a_licenses = [ "BSD Software License", 
                        "Creative Commons License",
                        "Common Development and Distribution License", 
                        "Common Development and Distribution License v1.1",
                        "Common Development and Distribution License v1.0",
                        "Common Public License v1.0",
                        "Eclipse Public License v1.0",
                        "MIT Software License",
                        "Mozilla Public License v2.0"
                     ]


license_mapping = {
    "ASLv2": "Apache Software License v2",
    "Apache 2": "Apache Software License v2",
    "Apache 2.0": "Apache Software License v2",
    "Apache License": "Apache Software License v2",
    "Apache License 2.0": "Apache Software License v2",
    "Apache License V2.0": "Apache Software License v2",
    "Apache License Version 2.0": "Apache Software License v2",
    "Apache Software Licenses": "Apache Software License v2",
    "Apache v2": "Apache Software License v2",
    "The Apache Software License": "Apache Software License v2",
    "BSD": "BSD Software License",
    "BSD 2-clause": "BSD Software License",
    "BSD 3-Clause \"New\" or \"Revised\" License (BSD-3-Clause)" : "BSD Software License",
    "BSD 3-Clause License": "BSD Software License",
    "BSD 3-clause": "BSD Software License",
    "BSD-like": "BSD Software License",
    "CC0 1.0 Universal": "Creative Commons License",
    "CDDL": "Common Development and Distribution License",
    "CDDL 1.1": "Common Development and Distribution License v1.1",
    "COMMON DEVELOPMENT AND DISTRIBUTION LICENSE (CDDL) Version 1.0": "Common Development and Distribution License v1.0",
    "Common Development and Distribution License (CDDL) v1.0": "Common Development and Distribution License v1.0",
    "Common Public License Version 1.0": "Common Public License v1.0",
    "Eclipse Public License 1.0": "Eclipse Public License v1.0",
    "MIT" : "MIT Software License",
    "MIT License" : "MIT Software License",
    "Mozilla Public License Version 2.0" : "Mozilla Public License v2.0",
    "New BSD License" : "BSD Software License",
    "New BSD license" : "BSD Software License",
    "Public" : "Public Domain",
    "Public Domain" : "Public Domain",
    "The BSD 3-Clause License" : "BSD Software License",
    "The BSD License" : "BSD Software License",
    "The MIT License" : "MIT Software License",
    "ACCEPTABLE" : "Ignore"
}

def read_component(i):
    with open(i, 'r') as fp:
        component_lines = fp.readlines()
        ret = {}
        for line in component_lines:
            if len(line) > 0:
                tokens = line.split(',')
                key = tokens[0]
                url = tokens[-1].strip()
                license = license_mapping[tokens[1].strip()]
                if license is None:
                    raise ValueError("unable to normalize license: " + tokens[1])
                l = line.split(',')[0].strip()
                ret[key] = { 'url' : url, 'license' : license }
        return ret

def read_license(f) :
    with open(f) as fp:
        return fp.read()

def get_blurb(component, license_info):
    tokens = component.split(':')
    artifact_id = tokens[1]
    version = tokens[3]
    return "This product bundles " + artifact_id + " " + version + ", which is available under a \"" + license_info['license'] + "\" license.  " + "For details, see " + license_info['url']

if __name__ == '__main__':
    components = read_component(sys.argv[1])
    license = read_license(sys.argv[2]) 
    for line in sys.stdin:
        component = line.strip() 
        if len(component) == 0 or component == 'none' or component not in components:
            continue
        else:
            license_info = components[component]
            if license_info['license'] in category_a_licenses:
                license = license + "\n" +  get_blurb(component, license_info)
            continue
    print license
