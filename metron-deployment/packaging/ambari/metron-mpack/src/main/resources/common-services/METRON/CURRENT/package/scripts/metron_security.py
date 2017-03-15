from resource_management.core import global_lock
from resource_management.core.logger import Logger
from resource_management.core.resources.system import Execute


def kinit(kinit_path_local, keytab_path, principal_name, execute_user=None):
    # prevent concurrent kinit
    kinit_lock = global_lock.get_lock(global_lock.LOCK_TYPE_KERBEROS)
    kinit_lock.acquire()
    kinitcmd = "{0} -kt {1} {2}; ".format(kinit_path_local, keytab_path, principal_name)
    Logger.info("kinit command: " + kinitcmd + " as user: " + str(execute_user))
    try:
        if execute_user is None:
            Execute(kinitcmd)
        else:
            Execute(kinitcmd, user=execute_user)
    finally:
        kinit_lock.release()

