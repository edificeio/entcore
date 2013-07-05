# Espace de travail

## Pré-requis
* Avoir mongodb installé sur son poste

## Installer mongodb sous debian wheezy (en root)
`apt-key adv --keyserver keyserver.ubuntu.com --recv 7F0CEB10
echo 'deb http://downloads-distro.mongodb.org/repo/debian-sysvinit dist 10gen' | tee /etc/apt/sources.list.d/10gen.list
aptitude update
aptitude install mongodb-10gen`
