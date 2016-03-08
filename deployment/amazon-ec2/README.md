Metron Deployment on Amazon Web services
========================================

This project automates the provisioning and deployment of Metron on EC2 infrastructure provided by Amazon Web Services.

Getting Started
---------------

Ensure that your working host has Ansible, Python and PIP installed.

Install the Python library `boto` which enables Ansible to communicate with AWS.

```
pip install boto six
```

Use AWS's [IAM](https://console.aws.amazon.com/iam/) tool to generate an access key.  Export these access keys in an environment variable so that Ansible can communicate with Amazon EC2.

```
export AWS_ACCESS_KEY_ID="..."
export AWS_SECRET_ACCESS_KEY="..."
```

Kick-off the provisioning process.

```
export EC2_INI_PATH=conf/ec2.ini
ansible-playbook -i ec2.py playbook.yml
```
