#!/bin/bash

# written by Verena Berg on 2015-06-25
# changed by Verena Berg on 2015-06-29
# changed by Verena Berg on 2015-07-04
# changed by Verena Berg on 2015-07-07
# changed by Verena Berg on 2015-07-13
# changed by Verena Berg on 2015-07-14
# changed by Verena Berg on 2015-07-16

# This is an installation script for GRIT.
# This script uses sudo and apt-get.
# If you want to do the installation via this script, please make sure sudo and apt-get is enabled.
# This is a direkt installation. That means you'll don't need vagrant.


#install required software first

#update your packages

sudo apt-get update

#In the folloing the -y option is used that you do not have to say 'yes' when the installations ask you if they should continue.

#You will need the javac Compiler. The javac Compiler comes with the openjdk-7-jdk package

sudo apt-get -y install openjdk-7-jdk

#install Texlive

sudo apt-get -y install texlive-full

#install the Gnu-C-Compiler

sudo apt-get -y install gcc

#install Glasgow-Haskell-Compiler

sudo apt-get -y install ghc

#install Subversion

sudo apt-get -y install subversion libapache2-svn

#install Secure Copy

sudo apt-get -y install openssh-server openssh-client

#install the G++ Compiler

sudo apt-get -y install g++

#install Virtualbox. You will need this for ILIAS

sudo apt-get -y install virtualbox

#install Git. You will need git to get GRIT.

sudo apt-get -y install git

#install Gradle. You will need it for the installation of GRIT

sudo apt-get -y install gradle

#If you've read the guide for the manual installation you'll maybe miss the installation of vagrant.
#This is no neglegt. We do not need to install vagrant here, because this script installs GRIT directly on your computer.
#You do not have to use vagrant later on.

#If you already have GRIT then it will be updated to the newest version. Otherwise GRIT will be cloned.
#Normally GRIT is cloned from the master branch. But here we clone from the development branch, because this is where you'll get the newest version.
#But we made our changes to the development branch. So we clone the development branch instead.

if [ -d grit ]
	then
	  cd grit/build/install/GRIT/res/web

	  #copy course- and exercise-files temporarily that they won't get lost.
	  cp -R pdf /home/$USER

	  #move to grit where the file gradle is.
	  cd ..
	  cd ..
	  cd ..
	  cd ..
	  cd ..

	  #change sourcecode, etc., to actual version.
	  git pull

	  #use gradlew to install actual version.
	  ./gradlew install

	  cd /home/$USER

	  #copy course- and exercise-files back to where they belong.
	  cp -R pdf grit/build/install/GRIT/res/web
#
	  #delete the temporarily created directory
	  rm -r pdf

else

	#git clone https://github.com/VARCID/grit.git

	git clone https://github.com/VARCID/grit.git --branch master

	#now there should be a directory called grit. Move there to install GRIT.

	cd grit
	#use gradlew to install GRIT on your computer.

	./gradlew install

fi


