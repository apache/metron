"""
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

"""

from resource_management.core.logger import Logger
from resource_management.libraries.functions.get_user_call_output import get_user_call_output
from resource_management.core.exceptions import ExecutionFailed
from resource_management.core.exceptions import ComponentIsNotRunning

def service_check(cmd, user, label):
    """
    Executes a service check command that adheres to LSB-compliant
    return codes.  The return codes are interpreted as defined
    by the LSB.

    See http://refspecs.linuxbase.org/LSB_3.0.0/LSB-PDA/LSB-PDA/iniscrptact.html
    for more information.

    :param cmd: The service check command to execute.
    :param label: The name of the service.
    """
    Logger.info("Performing service check; cmd={0}, user={1}, label={2}".format(cmd, user, label))
    rc, out, err = get_user_call_output(cmd, user, is_checked_call=False)

    if len(err) > 0:
      Logger.error(err)

    if rc in [1, 2, 3]:
      # if return code in [1, 2, 3], then 'program is not running' or 'program is dead'
      Logger.info("{0} is not running".format(label))
      raise ComponentIsNotRunning()

    elif rc == 0:
      # if return code = 0, then 'program is running or service is OK'
      Logger.info("{0} is running".format(label))

    else:
      # else service state is unknown
      err_msg = "{0} service check failed; cmd '{1}' returned {2}".format(label, cmd, rc)
      Logger.error(err_msg)
      raise ExecutionFailed(err_msg, rc, out, err)
