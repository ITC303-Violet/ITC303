# ITC303
Software Development Project 1

## Architecture
The system is designed to run using docker containers:
* Tomcat runs the application
* Nginx forwards requests to tomcat and serves static content
* Postgresql is used for data storage

The system can also be run without using docker at all, however will need to be setup similarly to the docker based environment (this shouldn't require too much work)

## Environment Setup
* Download and install Docker
  * [Windows 10](https://store.docker.com/editions/community/docker-ce-desktop-windows)
  * [Windows 8](https://www.docker.com/products/docker-toolbox)
* Clone the repository to a local directory
* Within eclipse:
  * File->Import...
  * Maven->Existing Maven Projects
  * Browse...->Browse to the cloned repository and select the 'main' directory
  * Click Okay then Finish


## Running
### Using Docker
Start the docker containers by running docker-start-containers.bat in the main directory

To build and test the project within eclipse:

* Click run and choose "Maven build"
* If asked to enter some goals enter `clean tomcat7:redeploy`
* This should build and then deploy the application to the docker container running tomcat

The tomcat instance once running can be reached at [http://localhost:8080/](http://localhost:8080/) (*Docker Toolbox* will require the address of your docker machine which tends to be [http://192.168.99.100:8080/](http://192.168.99.100:8080/))

Or using purely maven:
Run `mvn clean tomcat7:redeploy`

### Without Docker
The project can still be used without docker

Perform the same settings as above however:

* Set your postgresql authentication details in `/violet-main/src/main/webapp/WEB-INF/classes/META-INF/persistence.xml`
* Copy settings.xml.default to %HOMEPATH%\\.m2 and rename to settings.xml (If you have an existing settings.xml you'll need to merge them)
* Edit the credentials within the new settings.xml to match your running tomcat server

After this, compiling and deployment should be the same as using docker.

**NOTE:** If docker toolbox is installed, you'll also need to un-comment `<!-- <activeProfile>violet-no-docker</activeProfile> -->` within settings.xml to use the tomcat instance running on localhost


## Debugging with Docker
Within eclipse:
* Click the arrow beside the "Debug" toolbar icon
* Debug configurations
* Right click "Remote Java Application" and create a new one
* The default connection properties should work for Docker For Windows
  * For Docker Toolbox you'll need to enter the ip of your docker machine (tends to be `192.168.99.100`)
This will attach a remote debugger to the docker container running tomcat and the project