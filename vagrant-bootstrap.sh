#!/bin/bash

sudo pacman -Syu --noconfirm
sudo pacman -S git jdk7-openjdk ghc subversion texlive-most vim --noconfirm

sudo ln -fs /vagrant/build/install/GRIT/ /home/vagrant/grit
