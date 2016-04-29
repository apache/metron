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

ifeq ($(RTE_SDK),)
$(error "Please define RTE_SDK environment variable")
endif

# Default target, can be overriden by command line or environment
RTE_TARGET ?= x86_64-native-linuxapp-gcc

include $(RTE_SDK)/mk/rte.vars.mk

# binary name
APP = fastcapa

# all source are stored in SRCS-y
SRCS-y := main.c args.c kafka.c

KAFKALIB = -L/usr/local/lib -lrdkafka 
KAFKAINC = -I/usr/local/include/librdkafka/

GLIB     = $(shell pkg-config --libs glib-2.0) -lgthread-2.0
GLIBINC  = $(shell pkg-config --cflags glib-2.0)

LDLIBS += $(KAFKALIB) $(GLIB)
CFLAGS += $(WERROR_FLAGS) $(KAFKAINC) $(GLIBINC)

# workaround for a gcc bug with noreturn attribute
# http://gcc.gnu.org/bugzilla/show_bug.cgi?id=12603
ifeq ($(CONFIG_RTE_TOOLCHAIN_GCC),y)
CFLAGS_main.o += -Wno-return-type
endif

#EXTRA_CFLAGS += -O3 -Wfatal-errors
EXTRA_CFLAGS += -g -Wall

include $(RTE_SDK)/mk/rte.extapp.mk
