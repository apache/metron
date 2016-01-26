Kafka @ Hortonworks Data Platform
=================================

An Ansible Role that installs a single-node Kafka broker from the Hortonworks Data Platform distribution.  Zookeeper is also installed to support the Kafka broker.  

Getting Started
---------------

Add to your playbook's requirements.yml:

```
- src: https://github.com/nickwallen/ansible-hdp-kafka
```

and then run:

```
ansible-galaxy install -r requirements.yml --ignore-errors
```

Requirements
------------

None.

Role Variables
--------------


Dependencies
------------

None.

Example Playbook
----------------

    - hosts: kafka*
      roles:
         - { role: ansible-hdp-kafka, x: 42 }

License
-------

BSD

Author Information
------------------

Nick Allen
