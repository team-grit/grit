#!/bin/bash
# Backup script for grit which copys the wdir, pdf folder and
# config folder into a backup folder
# IF no path is provided the backup folder is created in the 
# grit directory

#get current Path
my_path="$(pwd)/"

#get current Time and Date to create a name for specific backup folder
timestamp="$(date +"%y_%m_%d_%H_%M")"

# shutdown grit 
echo "Shut down Grit"
cd build/install/GRIT/
bash shutdownGrit.sh

# going back to grit folder
cd "$my_path"

#check if a path is provided 

if [ "$1" = "" ]; then
   # Create default folder in the same folder, if
   # it does not exist
   if [ ! -d "backup/"$timestamp"/" ]; then
       mkdir -p backup/"$timestamp"/wdir
       mkdir -p backup/"$timestamp"/pdf
       mkdir -p backup/"$timestamp"/config
       echo "Backup folder created in current directory"
   fi 
   # copy files
   echo "Copy files into Backup folder"
   cp -r build/install/GRIT/wdir/* backup/"$timestamp"/wdir/
   cp -r build/install/GRIT/config/* backup/"$timestamp"/config/
   cp -r build/install/GRIT/res/web/pdf/* backup/"$timestamp"/pdf/
else
   # check if folder already exist
   if [ ! -d "$1/"$timestamp"/" ]; then
      echo "Create backup folder"
      mkdir -p $1/"$timestamp"/wdir
      mkdir -p $1/"$timestamp"/config
      mkdir -p $1/"$timestamp"/pdf
   fi
   # copy files 
   echo "Copy files into Backup folder"
   cp -r build/install/GRIT/wdir/* $1/"$timestamp"/wdir/
   cp -r build/install/GRIT/config/* $1/"$timestamp"/config/
   cp -r build/install/GRIT/res/web/pdf/* $1/"$timestamp"/pdf/
fi

# update Grit
echo "update Grit"
git_info="$(git pull)"

# Try to compile new version if update was pulled

if [ "Already up-to-date." = "$git_info" ]; then
   echo "$git_info"
else
   echo "compile new version"
   bash gradlew install
fi

# Same procedure as above, check if path
# was provided and copy files back
# into working directory

echo "Copy files back into working directory"
if [ "$1" = "" ]; then
  
  cp -r backup/"$timestamp"/wdir build/install/GRIT/
  cp -r backup/"$timestamp"/config build/install/GRIT/
  cp -r backup/"$timestamp"/pdf build/install/GRIT/res/web/

else

  cp -r $1/"$timestamp"/wdir build/install/GRIT/
  cp -r $1/"$timestamp"/pdf build/install/GRIT/res/web/
  cp -r $1/"$timestamp"/config build/install/GRIT/

fi

echo "Start Grit"
cd build/install/GRIT/
bash runscript.sh
