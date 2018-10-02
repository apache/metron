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

import sys
from ansible import __version__

from ansible.plugins.callback import CallbackBase
from ansible.utils.display import Display

def display(*args, **kwargs):
  display_instance = Display()
  display_instance.display(*args, **kwargs)

MINIMUM_ANSIBLE_VERSION = '2.4.0'

def version_requirement(version):
    return version >= MINIMUM_ANSIBLE_VERSION

class CallbackModule(CallbackBase):
  """
  This enforces a minimum version of ansible
  """

  CALLBACK_NAME = 'minimum_ansible_version'

  def __init__(self):
    super(CallbackModule, self).__init__()
    if not version_requirement(__version__):
      display('Metron requires Ansible %s or newer, current version is %s' % (MINIMUM_ANSIBLE_VERSION, __version__), color='red')
      sys.exit(1)
