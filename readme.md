#PhotoHub

PhotoHub is a client/server application based on web.

Server side of application keeps photos catalog assembled from different cloud resources and local store.
Client part is a collected photos catalog viewer. 

## Features
For now realised:
*  Sync with photos on local disk.  Photos can be placed to store directly w/o photohub.
*  Sync Photos with Goole Picasa. Unfortunately Picasa API became readonly since 2015. 
*  in process Sync with google disk
*  in process Sync with yandex disk 


## Platform
This is native java project, so it should work on any planform.
Project was tested in Mac OSX.
For other platform need to check app/bin/startup.sh  or create other startup shell script.


## Build
Open build.gradle in project root directory. Edit ‘rootDir’ property string with path where you want to place compiled projects.

Run:

    gradle -x test install
 


During build will be created new dir structure:


    .
    ├── app
    │   ├── bin
    │   │   ├── photohub.jar
    │   │   └── startup.sh
    │   ├── config
    │   │   ├── defaults
    │   │   ├── logback.xml
    │   │   └── photohub-standalone.properties
    │   └── log
    │       ├── photohub.log
    │       └── tasks.log
    └── db
        ├── hsqldb
        │   ├── photohub
        │   ├── photohub.properties
        │   └── photohub.script
        └── photo-thumbnail

  
## Run 


In rootDir run: 
 
    app/bin/startup.sh
 
