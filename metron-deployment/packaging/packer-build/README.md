<!--
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
-->
Build Metron Images
=========================

Based on the fantastic [Bento](https://github.com/chef/bento) project developed by Chef.

Images Provided
---------------------
- base-centos-6.7: Centos 6.7 + HDP. Used in the full-dev-platform Vagrant image

Prerequisites
---------------------
- [Packer](https://www.packer.io/) 0.12.2
- [Virtualbox](https://www.virtualbox.org/) 5.0.16+ (Tested with 5.0.20)

Build Both Images
----------------------
  Navigate to \<your-project-directory\>/metron-deployment/packer-build
  Execute bin/bento build

  Packer will build both images and export .box files to the ./builds directory.

Build Single Images
----------------------
 Navigate to *your-project-directory*/metron-deployment/packer-build
 * Base Centos (full-dev)
```
bin/bento build base-centos-6.7.json
```

Using Your New Box File
----------------------
Modify the relevant Vagrantfile (full-dev-platform) replacing the lines:
```
<pre><code>config.vm.box = "<i>box_name</i>"
config.ssh.insert_key = true</code></pre>
```
with
```
<pre></code>config.vm.box = "<i>test_box_name</i>"
config.vm.box = "<i>PathToBoxfile/Boxfilename</i>"
config.ssh.insert_key = true</code></pre>
```
Launch the image as usual.

Node: Vagrant will cache boxes, you can force Vagrant to reload your box by running <code>vagrant box remove <i>test_box_name</i></code> before launching your new image.
