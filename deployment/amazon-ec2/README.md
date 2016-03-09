Apache Metron on Amazon EC2
===========================

This project fully automates the provisioning of Apache Metron on Amazon EC2 infrastructure.  Starting with only your Amazon EC2 credentials, this project will create a fully-functioning, end-to-end, multi-node cluster running Apache Metron.

Getting Started
---------------

The host that will be used to drive the provisioning process will need to have [Ansible](https://github.com/ansible/ansible), Python and PIP installed.  In most cases, a development laptop serves this purpose just fine.  For better performance, run this playbook on a pre-provisioned EC2 host.

Ensure that an SSH key has been generated.  The playbook will attempt to register `~/.ssh/id_rsa.pub` with each provisioned host so that an SSH connection can be established.  If one does not exist, use `ssh-keygen` to create one.

1. Install the Python library `boto` and its dependencies.  This enables Ansible to communicate with Amazon EC2.

  ```
  pip install boto six
  ```

2. Use Amazon's [IAM](https://console.aws.amazon.com/iam/) tool to generate an access key.  Export these access keys in an environment variable so that Ansible can authenticate with Amazon EC2.

  ```
  export AWS_ACCESS_KEY_ID="..."
  export AWS_SECRET_ACCESS_KEY="..."
  ```

3. Build Metron's streaming topology uber-jar.

  ```
  cd ../../metron-streaming
  mvn clean package -DskipTests
  ```

4. Kick-off the provisioning playbook.  If the playbook fails mid-stream for any reason, simply re-run it.  This will attempt to re-provision on the previously instantiated EC2 hosts.

  ```
  export EC2_INI_PATH=conf/ec2.ini
  ansible-playbook -i ec2.py playbook.yml
  ```

Each of the provisioned hosts will be externally accessible from the internet at-large. Connecting to one over SSH as the user `centos` will not require a password as it will authenticate with the pre-defined SSH key.  

```
ssh centos@ec2-52-91-215-174.compute-1.amazonaws.com
```

Multiple Environments
---------------------

This process can support provisioning of multiple, isolated environments.  Simply change the `env` settings in `conf/defaults.yml`.  For example, you might provision separate development, test, and production environments.

```
env: metron-test
```
