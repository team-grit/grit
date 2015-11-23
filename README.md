#GRIT

[![Join the chat at https://gitter.im/team-grit/grit](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/team-grit/grit?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

##How to install:
*  download source from github: ````git clone git@github.com:team-grit/grit````
* install vagrant from [here](https://www.vagrantup.com/)
* run ````vagrant up````, this will create a VM with all prerequisites installed.
*  execute the gradlew script in the root directory of the cloned repository to 
    install dependencies for development.
  *  Important gradlew commands:
    * ````gradlew eclipse```` creates an eclipse project
    * ````gradlew idea```` creates an intelliJ IDEA project
    * ````gradlew install```` installs GRIT to the folder GRITFOLDER/build/install/GRIT
        (Also deploys to the VM)
    * ````gradlew distzip```` creates a zipfile to distribute GRIT

##How to use GRIT with vagrant
* Start the vm with ````vagrant up````
* Connect to it with ````vagrant ssh````
* Enter ````cd GRIT```` to get to the application directory
* run GRIT with ````./runscript.sh````

## Debugging GRIT
* You can debug GRIT with a remote debugger by connecting to the debug port specified in the gradle properties (Default is ````6001````)

## More information
[javadoc](http://team-grit.github.io/grit/)    
more about team grit: [Website](http://team-grit.com)    
