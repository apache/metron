Apache Metron on Amazon EC2
===========================

This project fully automates the provisioning of Apache Metron on Amazon EC2 infrastructure.  Starting with only your Amazon EC2 credentials, this project will create a fully-functioning, end-to-end, multi-node cluster running Apache Metron.

Getting Started
---------------

### Prerequisites

The host that will drive the provisioning process will need to have [Ansible](https://github.com/ansible/ansible), Python and PIP installed.  In most cases, a development laptop serves this purpose just fine.  Also, install the Python library `boto` and its dependencies.  

```
pip install boto six
```

Ensure that an SSH key has been generated using `ssh-keygen`.  By default, the playbook will attempt to register `~/.ssh/id_rsa` with each provisioned host so that an SSH connection can be established.  If this key does not exist or you would prefer to use another, provide the path to the private key in `conf/defaults.yml`.

```
key_file: ~/.ssh/metron-private-key
```

### Create User

1. Use Amazon's [Identity and Access Management](https://console.aws.amazon.com/iam/) tool to create a user account by clicking on `Users > Create New User`.  

2. Grant the user permission by clicking on `Permissions > Attach Policy` and adding the following policies.

  ```
  AmazonEC2FullAccess
  AmazonVPCFullAccess
  ```

3. Create an access key for the user by clicking on `Security Credentials > Create Access Key`.  Save the provided access key values in a safe place for use later.

4. Use the access key by exporting it in an environment variable.  This allows Ansible to authenticate with Amazon EC2.  For example:

  ```
  export AWS_ACCESS_KEY_ID="AKIAI6NRFEO27E5FFELQ"
  export AWS_SECRET_ACCESS_KEY="vTDydWJQnAer7OWauUS150i+9Np7hfCXrrVVP6ed"
  ```

### Deploy Metron

1. Ensure that Metron's streaming topology uber-jar has been built.

  ```
  cd ../../metron-streaming
  mvn clean package -DskipTests
  ```

2. Start the Metron playbook.  A full Metron deployment can consume up to 60 minutes.  Grab a coffee, relax and practice mindfulness meditation.  If the playbook fails mid-stream for any reason, simply re-run it.  

  ```
  export EC2_INI_PATH=conf/ec2.ini
  ansible-playbook -i ec2.py playbook.yml
  ```

### Explore Metron

1. Go to the [EC2 Management Console](https://aws.amazon.com/console) and find the running instances.  Look at the `Name` column which is populated with both the host group and environment name.  The Metron Web Console runs on the host in the `web` host group.  Once you have found the host, point your web browser at it.  For example:

  http://ec2-52-38-14-241.us-west-2.compute.amazonaws.com:5000

2. Each of the provisioned hosts will be accessible from the internet. Connecting to one over SSH as the user `centos` will not require a password as it will authenticate with the pre-defined SSH key.  

  ```
  ssh centos@ec2-52-91-215-174.compute-1.amazonaws.com
  ```

Usage
-----

### Multiple Environments

This process can support provisioning of multiple, isolated environments.  Simply change the `env` settings in `conf/defaults.yml`.  For example, you might provision separate development, test, and production environments.

```
env: metron-test
```

### Selective Provisioning

To provision only subsets of the entire Metron deployment, Ansible tags can be specified.  For example, to only deploy the sensors on an Amazon EC2 environment, run the following command.

```
ansible-playbook -i ec2.py playbook.yml --tags "ec2,sensors"
```
