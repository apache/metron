Build Metron Images
=========================

Based on the fantastic [Bento](https://github.com/chef/bento) project developed by Chef.

Images Provided
---------------------
- hdp-centos-6.7: Centos 6.7 + HDP. Used in the quick-dev-platform Vagrant image
- metron-centos-6.7: Centos 6.7 + HDP + Metron. Used for the codelab-platform Vagrant image.

Prerequisites
---------------------
- Packer 0.10.1 https://www.packer.io/
- Virtualbox 5.0.16 https://www.virtualbox.org/
- Be sure to build Metron prior to building the images- (cd *your-project-directory*/metron-platform ; mvn clean package -DskipTests)

Build Both Images
---------------------- 
  Navigate to <your-project-directory>/metron-deployment/packer-build
  Execute bin/bento build
  
  Packer will build both images and export .box files to the ./builds directory.
  
Build Single Images
---------------------- 
 Navigate to *your-project-directory*/metron-deployment/packer-build
 * HDP Centos 
 ```bin/bento build hdp-centos-6.7.json```
 * Full Metron
 ```bin/bento build metron-centos-6.7.json```

Using Your New Box File
---------------------- 
Modify the relevant Vagrantfile (codelab-platform or quick-dev-platform) replacing the lines:

<pre><code>config.vm.box = "<i>box_name</i>"
config.ssh.insert_key = true</code></pre>

with

<pre></code>config.vm.box = "<i>test_box_name</i>"
config.vm.box = "<i>PathToBoxfile/Boxfilename</i>"
config.ssh.insert_key = true</code></pre>

Launch the image as usual.

Node: Vagrant will cache boxes, you can force Vagrant to reload your box by running <code>vagrant box remove <i>test_box_name</i></code> before launching your new image.

