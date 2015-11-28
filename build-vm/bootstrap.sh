echo "Installing Java"
sudo apt-get update -y
sudo apt-get install -y openjdk-7-jre

echo "Downloading eclipse, this might take a while ..."
echo
wget -nv "http://www.eclipse.org/downloads/download.php?file=/technology/epp/downloads/release/mars/1/eclipse-java-mars-1-linux-gtk-x86_64.tar.gz&r=1" -O eclipse-java-mars-1-linux-gtk-x86_64.tar.gz

echo "Installing eclipse in /home/vagrant/eclipse and setting up paths ..."
(cd /home/vagrant && sudo tar xzf /home/vagrant/eclipse-java-mars-1-linux-gtk-x86_64.tar.gz)
sudo ln -s /home/vagrant/eclipse/eclipse /usr/local/bin/eclipse

echo "Downloading code bubbles binaries..."
echo
wget -nv "http://www.cs.brown.edu/people/spr/bubbles/bubbles.jar" -O bubbles.jar
sudo mkdir -p /home/vagrant/Desktop/Executable && cp bubbles.jar /home/vagrant/Desktop/Executable/
sudo chmod a+x /home/vagrant/Desktop/Executable/bubbles.jar

#Removing unnecessary launchers
sudo rm -f "/usr/share/applications/ubuntu-amazon-default.desktop"
sudo rm -f "/usr/share/applications/libreoffice-calc.desktop"
sudo rm -f "/usr/share/applications/libreoffice-writer.desktop"
sudo rm -f "/usr/share/applications/libreoffice-impress.desktop"
sudo rm -f "/usr/share/applications/ubuntu-software-center.desktop"

echo "Enabling auto login"
sudo  mkdir /etc/lightdm/lightdm.conf.d
sudo  bash -c 'printf "[SeatDefaults]\nautologin-user=Vagrant" > /etc/lightdm/lightdm.conf.d/50-myconfig.conf'

sudo mkdir /home/vagrant/.config/autostart

echo "Downloading required files to desktop."
wget -O "/home/vagrant/Desktop/README.txt" https://raw.githubusercontent.com/SoftwareEngineeringToolDemos/ICSE-2012-CodeBubbles/master/build-vm/vm-contents/README.txt
wget -O "/home/vagrant/Desktop/Installation.txt" https://raw.githubusercontent.com/SoftwareEngineeringToolDemos/ICSE-2012-CodeBubbles/master/build-vm/vm-contents/Installation.txt
wget -O "/home/vagrant/Desktop/License.txt" https://raw.githubusercontent.com/SoftwareEngineeringToolDemos/ICSE-2012-CodeBubbles/master/build-vm/vm-contents/License
wget -O "/home/vagrant/Desktop/How to use CodeBubbles in VM - YouTube.desktop" https://github.com/SoftwareEngineeringToolDemos/ICSE-2012-CodeBubbles/raw/master/build-vm/vm-contents/How%20to%20use%20CodeBubbles%20in%20VM%20-%20YouTube.desktop
wget -O "/home/vagrant/Desktop/Code Bubbles - YouTube.desktop.desktop" https://github.com/SoftwareEngineeringToolDemos/ICSE-2012-CodeBubbles/raw/master/build-vm/vm-contents/Demo%20Video/Code%20Bubbles_%20Rethinking%20the%20User%20Interface%20Paradigm%20of%20Integrated%20Development%20Environments%20-%20YouTube.desktop
wget -O "/home/vagrant/Downloads/bubbles.zip" https://github.com/SoftwareEngineeringToolDemos/ICSE-2012-CodeBubbles/raw/master/build-vm/vm-contents/bubbles.zip
wget -O "/home/vagrant/Downloads/workspace.zip" https://github.com/SoftwareEngineeringToolDemos/ICSE-2012-CodeBubbles/raw/master/build-vm/vm-contents/workspace.zip
wget -O "/home/vagrant/Downloads/Executable.zip" https://github.com/SoftwareEngineeringToolDemos/ICSE-2012-CodeBubbles/raw/master/build-vm/vm-contents/Executable.zip
wget -O "/home/vagrant/.config/autostart/launch_code_bubbles.sh.desktop" https://raw.githubusercontent.com/SoftwareEngineeringToolDemos/ICSE-2012-CodeBubbles/master/build-vm/vm-contents/launch_code_bubbles.sh.desktop
wget -O "/home/vagrant/Desktop/launch_code_bubbles.sh.desktop" https://raw.githubusercontent.com/SoftwareEngineeringToolDemos/ICSE-2012-CodeBubbles/master/build-vm/vm-contents/launch_code_bubbles.sh.desktop
wget -O "/home/vagrant/Desktop/Executable/launch_code_bubbles.sh" https://raw.githubusercontent.com/SoftwareEngineeringToolDemos/ICSE-2012-CodeBubbles/master/build-vm/vm-contents/launch_code_bubbles.sh
wget -O "/home/vagrant/eclipse/plugins/edu.brown.cs.bubbles.bedrock_1.0.0.jar" https://github.com/SoftwareEngineeringToolDemos/ICSE-2012-CodeBubbles/raw/master/build-vm/vm-contents/edu.brown.cs.bubbles.bedrock_1.0.0.jar
sudo chmod +x /home/vagrant/.config/autostart/launch_code_bubbles.sh.desktop
sudo chmod +x /home/vagrant/Desktop/launch_code_bubbles.sh.desktop
sudo chmod +x /home/vagrant/Desktop/Executable/launch_code_bubbles.sh 

sudo chown -R vagrant "/home/vagrant"
sudo chmod -R a+rwx "/home/vagrant/Desktop"

sudo -u vagrant unzip /home/vagrant/Downloads/bubbles.zip -d /home/vagrant
sudo -u vagrant unzip /home/vagrant/Downloads/workspace.zip -d /home/vagrant
sudo -u vagrant unzip /home/vagrant/Downloads/Executable.zip -d /home/vagrant/Desktop/Executable

#Restarting vm
sudo reboot



