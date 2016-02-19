metron-ui
==========

User interface for Metron

## Deployment

 Here are the minimal steps for deployment on a Ubuntu 14.04. These instructions will need to be altered for Ubuntu 12.04 as the nodejs package is too old. Assume that the code is in ```/opt/portal``` and the user is ```portal```.

* Install dependencies:

```bash
apt-get update
apt-get install -y libpcap-dev tshark redis-server nodejs npm
ln -s /usr/bin/nodejs /usr/bin/node
npm install -g pm2

su - portal
cd /opt/portal
npm install --production
```

* Add a file name ```config.json``` to the repo root (```/opt/portal``` in our setup). The config should point to the various services. The following is an example config, all fields are required:

```json
{
    "secret": "some secret",
    "elasticsearch": {
      "url": "http://192.168.33.10:9200"
    },
    "redis": {
      "host": "127.0.0.1",
      "port": 6379
    },
    "ldap": {
      "url": "ldap://127.0.0.1:389",
      "searchBase": "dc=metron,dc=dev",
      "searchFilter": "(mail={{username}})",
      "searchAttributes": ["cn", "uid", "mail", "givenName", "sn", "memberOf"],
      "adminDn": "cn=admin,dc=metron,dc=dev",
      "adminPassword": "metron"
    },
    "permissions": {
      "pcap": "cn=investigators,ou=groups,dc=metron,dc=dev"
    }
  }
```

* Run the server:

```bash
pm2 start index.js -i max --name "metron"
```


## Setup development environment

### Step 1: Install Virtualbox and Vagrant

Download the latest package for your platform here:

1. [Virtualbox](https://www.virtualbox.org/wiki/Downloads)
2. [Vagrant](https://www.vagrantup.com/downloads.html)

### Step 2: Clone repo

```bash
git clone git@github.com:apache/incubator-metron.git
cd incubator-metron/metron-ui
```

### Step 3: Download and provision the development environment

```bash
vagrant up
```

You might see a couple warnings, but usually these can be ignored. Check for any obvious errors as this can cause problems running the portal later.

### Step 4: SSH into the vm
All dependencies will be installed in the VM. The repository root is shared between the host and VM. The shared volume is mounted at /vagrant. Use the following command to ssh into the newly built VM:

```bash
vagrant ssh
cd vagrant
```

###  Step 5: Seed the development VM

To generate seed data for use with the metron-ui, use the following command.

```bash
script/es_gen.js
```

On the other hand, to duplicate another ES installation use:

```bash
ES_HOST=changeme.com script/es_fetch.js
```

You should now have seed data in ```seed/es```. You can load this into the dev ES instance with:

```bash
script/es_seed
```

For authentication, make sure you set up the LDAP directory structure with:

```bash
script/ldap_seed
```

### Step 6: Ensure tests pass

You can now run the tests:

```bash
make test
```

### Step 7: Launch the server

The ```nodemon``` utility automatically watches for changed files and reloads the node server automatically. Run the following commands from with the vagrant vm.

```bash
vagrant ssh
cd /vagrant
npm install -g nodemon
nodemon
```

You can then access the Metron ui at ```http://localhost:5000```.
