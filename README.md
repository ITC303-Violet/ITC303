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

In order for docker to have access to some of the configuration files, you'll need to setup drive sharing
I suggest creating a local account for docker
On Windows 10 this can be accomplished by:
* Hit the Start button
* Select "Settings"
* Accounts->Family & other people
* Add someone else to this PC
  * Hit "I don't have this person's sign-in information"
  * Select "Add a user without a Microsoft account"
  * Fill in the details for your local docker account (i.e. user:docker) with a random password (write it down)
* In the task bar open the [docker settings](https://docs.docker.com/docker-for-windows/#docker-settings) by right clicking the docker icon and choosing "Settings..."
* Open "Shared Drives"
  * Tick the box beside the drive that you cloned the repository to
  * Hit apply
  * Enter the details of the account you created

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