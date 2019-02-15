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
# Build Utilities

The aim of the build utilities project is to provide some scripting
around the care and maintenance of the building infrastructure.  At the
moment the primary mission is around utilities that assist us manage the
licenses of our dependencies and generate the appropriate notices or
licenses.  These utilities assume you have already built the repo via `mvn
clean install -DskipTests`.

## `dependencies_with_url.csv`
This file is the reference file for all of our dependencies.
If you add a dependency, you must add a line to the
`dependencies_with_url.csv` file.

## `list_dependencies.sh`

List all of the transitive dependencies for the project rooted at cwd.

## `verify_licenses.sh`

This script, as run by our travis build infrastructure, will look at the
dependencies and verify that we know about them. Travis will use this
script which takes the transitive dependency list and check against the
`dependencies_with_url.csv` file to ensure that it's listed. This will
make sure we track dependencies and do not have any unacceptable
dependencies.

If you want to dump all of the dependencies that it doesn't know about,
from the top level directory:

```
dev-utilities/build-utils/list_dependencies.sh | python dev-utilities/build-utils/verify_license.py ./dependencies_with_url.csv dump
```


## `create_bundled_licenses.sh`

This script is intended to regenerate the licenses for each project that
bundles its dependencies.  Because we bundle our dependencies in a
shaded jar, we
[must](http://www.apache.org/dev/licensing-howto.html#deps-of-deps) specify a `LICENSE` file with the permissively
licensed dependencies notated as per [here](http://www.apache.org/dev/licensing-howto.html#permissive-deps)

Example command to regenerate licenses (run from top level directory):

```
for i in $(find . -name LICENSE | grep src | grep META-INF | awk -Fsrc '{print $1}');do dev-utilities/build-utils/create_bundled_licenses.sh $i;done
```
