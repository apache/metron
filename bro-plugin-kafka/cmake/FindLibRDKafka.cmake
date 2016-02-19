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

find_path(LibRDKafka_ROOT_DIR
  NAMES include/librdkafka/rdkafkacpp.h
)

find_library(LibRDKafka_LIBRARIES
  NAMES rdkafka++
  HINTS ${LibRDKafka_ROOT_DIR}/lib
)

find_library(LibRDKafka_C_LIBRARIES
	NAMES rdkafka
	HINTS ${LibRDKafka_ROT_DIR}/lib
)

find_path(LibRDKafka_INCLUDE_DIR
  NAMES librdkafka/rdkafkacpp.h
  HINTS ${LibRDKafka_ROOT_DIR}/include
)

include(FindPackageHandleStandardArgs)
find_package_handle_standard_args(LibRDKafka DEFAULT_MSG
  LibRDKafka_LIBRARIES
  LibRDKafka_C_LIBRARIES
  LibRDKafka_INCLUDE_DIR
)

mark_as_advanced(
  LibRDKafka_ROOT_DIR
  LibRDKafka_LIBRARIES
  LibRDKafka_C_LIBRARIES
  LibRDKafka_INCLUDE_DIR
)
