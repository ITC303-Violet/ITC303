# ITC303
Software Development Project 1

## Environment Setup
* Download and install [Docker](https://store.docker.com/editions/community/docker-ce-desktop-windows)
* Clone the repository to a local directory
* Within eclipse:
  * File->Import...
  * Maven->Existing Maven Projects
  * Browse...->Browse to the cloned repository and select the 'main' directory
  * Click Okay then Finish

If you encounter any issues with docker reaching the internet, consider entering the Google DNS address (8.8.8.8) in the network tab

## Running
![Eclipse debug/run buttons](http://i.imgur.com/rJdl64V.png)
Start the docker containers by clicking the arrow beside the "External Tools" icon on the running toolbar (far right) and selecting "Start docker containers"

To build and test the project within eclipse:
* Click the arrow beside the "Run" toolbar icon
* Select "Build and deploy"
This will compile and package the project, and redeploy the package to the tomcat container

The tomcat instance once running can be reached at http://localhost:8080/

Or using purely maven:
Run `mvn clean package install`

## Debugging
Within eclipse:
* Click the arrow beside the "Debug" toolbar icon
* Select "Docker container"
This will attach a remote debugger to the docker container running tomcat and the project