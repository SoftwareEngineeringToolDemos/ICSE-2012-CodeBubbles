#How to build your own Virtual Machine?
###The following steps shows how you can spin up a Virtual Machine for tool CodeBubbles :

1. Install [vagrant] (https://www.vagrantup.com/downloads.html) and [virtualbox] (https://www.virtualbox.org/wiki/Downloads) on your host machine.
2. Download the [Vagrantfile] (https://github.com/SoftwareEngineeringToolDemos/ICSE-2012-CodeBubbles/blob/master/build-vm/Vagrantfile) and [bootstrap] (https://github.com/SoftwareEngineeringToolDemos/ICSE-2012-CodeBubbles/blob/master/build-vm/bootstrap.sh) from [build-vm] (https://github.com/SoftwareEngineeringToolDemos/ICSE-2012-CodeBubbles/blob/master/build-vm) folder on your machine and save it in a folder where you want to install the VM.
3. From the host, navigate to that folder (via bash on Linux Machine or Powershell or CommandPrompt on Windows Machine) and execute the command :  
      "vagrant up"

###Note :
 -  The Virtual Machine will boot up quickly and can be viewed from the Virtual Box but has to wait for the "vagrant up" command for nearly half an hour to complete as it provisions the VM for use.
 -  Deploys Base Vagrant Box : [Ubuntu 14.04 Desktop] (https://vagrantcloud.com/box-cutter/boxes/ubuntu1404-desktop)
 -  Default VM Login Credentials:  
      user: vagrant  
      password: vagrant

###Softwares:

This VM contains Open-JDK-7 and Eclipse Mars 4.5 with Ubuntu 14.04.


###References:

1. http://askubuntu.com/questions/159008/how-to-add-startup-applications-in-lubuntu
2. https://help.ubuntu.com/community/AutoLogin
3. http://aruizca.com/steps-to-create-a-vagrant-base-box-with-ubuntu-14-04-desktop-gui-and-virtualbox/
4. http://askubuntu.com/questions/230358/problems-creating-a-desktop-entry-for-a-shell-script

