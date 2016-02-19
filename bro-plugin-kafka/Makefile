#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

#
# Convenience Makefile providing a few common top-level targets.
#

cmake_build_dir=build
arch=`uname -s | tr A-Z a-z`-`uname -m`

all: build-it

build-it:
	@test -e $(cmake_build_dir)/config.status || ./configure
	-@test -e $(cmake_build_dir)/CMakeCache.txt && \
      test $(cmake_build_dir)/CMakeCache.txt -ot `cat $(cmake_build_dir)/CMakeCache.txt | grep BRO_DIST | cut -d '=' -f 2`/build/CMakeCache.txt && \
      echo Updating stale CMake cache && \
      touch $(cmake_build_dir)/CMakeCache.txt

	( cd $(cmake_build_dir) && make )

install:
	( cd $(cmake_build_dir) && make install )

clean:
	( cd $(cmake_build_dir) && make clean )

distclean:
	rm -rf $(cmake_build_dir)

test:
	make -C tests
