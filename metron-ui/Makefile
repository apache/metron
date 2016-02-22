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


testcmd=./node_modules/istanbul/lib/cli.js cover \
	./node_modules/mocha/bin/_mocha -- --check-leaks -R spec

testwatchcmd=./node_modules/istanbul/lib/cli.js cover \
	./node_modules/mocha/bin/_mocha -- --check-leaks --watch -R spec

test: test-all

test-watch:
ifeq ($(IN_TRAVIS),true)
	PORT=4000 NODE_ENV=ci $(testwatchcmd)
else
	PORT=4000 NODE_ENV=test $(testwatchcmd)
endif

test-all:
ifeq ($(IN_TRAVIS),true)
	PORT=4000 NODE_ENV=ci $(testcmd)
else
	PORT=4000 NODE_ENV=test $(testcmd)
endif

# Load test data into DB
seed:
	node script/es_fetch.js && script/es_seed.sh

clean:
	rm -rf ./node_modules ./coverage
