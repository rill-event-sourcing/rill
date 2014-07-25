#!/bin/bash
##################################################################################################
# install on basic Ubuntu 14.04
##################################################################################################
echo 'installing packages'
apt-get -y update
apt-get -y dist-upgrade
apt-get -y install build-essential git curl openssl libssl-dev wget software-properties-common ntp logrotate unattended-upgrades

echo 'setting unattended-upgrades'
echo 'APT::Periodic::Update-Package-Lists "1";' > /etc/apt/apt.conf.d/10periodic
echo 'APT::Periodic::Download-Upgradeable-Packages "1";' >> /etc/apt/apt.conf.d/10periodic
echo 'APT::Periodic::AutocleanInterval "7";' >> /etc/apt/apt.conf.d/10periodic
echo 'APT::Periodic::Unattended-Upgrade "1";' >> /etc/apt/apt.conf.d/20auto-upgrades
echo 'Unattended-Upgrade::Automatic-Reboot "false";' >> /etc/apt/apt.conf.d/50unattended-upgrades

echo 'adding studyflow user'
groupadd studyflow
useradd studyflow -s /bin/bash -m -g studyflow -G sudo
chpasswd << 'END'
  studyflow:sf1
END
echo '%studyflow   ALL=(ALL:ALL) NOPASSWD: ALL' >> /etc/sudoers

echo 'adding ssh_keys'
mkdir /home/studyflow/.ssh
cp /root/.ssh/authorized_keys /home/studyflow/.ssh/
chown -R studyflow:studyflow /home/studyflow/.ssh

echo 'setting up ssh'
sed -i 's/Port 22/Port 1022/g' /etc/ssh/sshd_config
# sed -i 's/PermitRootLogin yes/PermitRootLogin without-password/g' /etc/ssh/sshd_config
sed -i 's/#PasswordAuthentication yes/PasswordAuthentication no/g' /etc/ssh/sshd_config
service ssh restart

echo 'installing firewall'
ufw allow 1022
ufw allow from 10.129.0.0/16 # internal network
ufw allow from 84.105.16.173 # steven
ufw allow from 87.210.80.80  # davide
ufw allow from 83.80.250.233 # joost
ufw allow from 213.127.250.192 # HQ Studyflow Amsterdam
sed -i 's/IPV6=yes/IPV6=no/g' /etc/default/ufw
# ufw logging off
yes | ufw enable

echo 'set locale'
locale-gen nl_NL nl_NL.UTF-8
locale-gen en_US en_US.UTF-8
dpkg-reconfigure locales

echo 'set timezone'
echo 'Europe/Amsterdam' > /etc/timezone
dpkg-reconfigure --frontend noninteractive tzdata

# echo 'setting up remote logging'
# echo '$PreserveFQDN on' >> /etc/rsyslog.conf
# echo '*.* @10.129.221.99:514' > /etc/rsyslog.d/10-remote.conf

# echo 'setting up New Relic'
# echo 'deb http://apt.newrelic.com/debian/ newrelic non-free' >> /etc/apt/sources.list.d/newrelic.list
# wget -O- https://download.newrelic.com/548C16BF.gpg | apt-key add -
# apt-get update
# apt-get -y install newrelic-sysmond
# nrsysmond-config --set license_key=3544ff71137bf5ab26dad4b919087f4b9ab681e6
# /etc/init.d/newrelic-sysmond start

##################################################################################################
echo 'you can now login with: ssh -p1022 studyflow@ip-adres'
##################################################################################################


##################################################################################################
# setup for Rails server:
##################################################################################################
echo 'install apache2 packages'
apt-get -y install postgresql-client libpq-dev libyaml-dev libreadline-dev

echo 'install apache2'
apt-get -y install apache2 libapache2-mod-passenger libapache2-mod-xsendfile
a2enmod deflate expires headers passenger rewrite xsendfile
echo 'ServerName localhost' >> /etc/apache2/apache2.conf
cat <<EOF > /etc/apache2/sites-available/000-default.conf
<VirtualHost *:80>
DocumentRoot /rails/current/public
ErrorLog /rails/shared/log/error.log
CustomLog /rails/shared/log/access.log combined

<Directory "/rails/current">
  Order allow,deny
  Allow from all
  Require all granted
</Directory>

PassengerDefaultUser studyflow
PassengerMaxPoolSize 30
PassengerMinInstances 4
PassengerPoolIdleTime 150
PassengerPreStart http://localhost
RailsEnv $1

XSendFile on
XSendFilePath "/rails/current/public"

<LocationMatch "^/assets/.*$">
  # Some browsers still send conditional-GET requests if there's a
  # Last-Modified header or an ETag header even if they haven't
  # reached the expiry date sent in the Expires header.
  Header unset Last-Modified
  Header unset ETag
  FileETag None
  # RFC says only cache for 1 year
  ExpiresActive On
  ExpiresDefault "access plus 1 year"
</LocationMatch>

# compress text, html, javascript, css, xml:
AddOutputFilterByType DEFLATE text/plain
AddOutputFilterByType DEFLATE text/html
AddOutputFilterByType DEFLATE text/xml
AddOutputFilterByType DEFLATE text/css
AddOutputFilterByType DEFLATE application/xml
AddOutputFilterByType DEFLATE application/xhtml+xml
AddOutputFilterByType DEFLATE application/rss+xml
AddOutputFilterByType DEFLATE application/javascript
AddOutputFilterByType DEFLATE application/x-javascript
AddOutputFilterByType DEFLATE application/json

# Deactivate compression for buggy browsers
BrowserMatch ^Mozilla/4 gzip-only-text/html
BrowserMatch ^Mozilla/4\.0[678] no-gzip
BrowserMatch \bMSIE !no-gzip !gzip-only-text/html

# Set header information for proxies
Header append Vary User-Agent
</VirtualHost>
EOF

sed -i 's/export APACHE_RUN_USER=www-data/export APACHE_RUN_USER=studyflow/g' /etc/apache2/envvars
sed -i 's/export APACHE_RUN_GROUP=www-data/export APACHE_RUN_GROUP=studyflow/g' /etc/apache2/envvars

echo 'setinng up the apache dirs'
mkdir -p /rails/releases /rails/shared /rails/shared/cached-copy /rails/shared/log /rails/shared/pids /rails/shared/system
chown -R studyflow:studyflow /rails
chmod -R 775 /rails
service apache2 restart

#################################################
echo 'done setting up RAILS'
#################################################
