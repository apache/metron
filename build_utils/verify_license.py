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

def read_component(i):
    with open(i, 'r') as fp:
        component_lines = fp.readlines()
        ret = []
        for line in component_lines:
            if len(line) > 0:
                l = line.split(',')[0].strip()
                ret.append(l)
        return sets.Set(ret)

if __name__ == '__main__':
    components = read_component(sys.argv[1])
    components_not_found = []
    for line in sys.stdin:
        component = line.strip() 
        if len(component) == 0 or component == 'none' or component in components:
            continue
        else:
            if len(sys.argv) > 2:
                print component
            else:
                components_not_found.append(component)
    if len(components_not_found) > 0:
        raise ValueError("Unable to find these components: \n  " + "\n  ".join(components_not_found) + "\nin the acceptable list of components: " + sys.argv[1])
