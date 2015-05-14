#!/bin/bash

sudo apt-get update
sudo apt-get install -y git openjdk-8-jdk ghc subversion texlive-full vim gcc g++
sudo ln -fs /vagrant/build/install/GRIT/ /home/vagrant/grit
