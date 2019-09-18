## A: Build Eiffel Gerrit Plugin Docker image based on Eiffel Gerrit Plugin from an Artifactory:
cd (git root dir)
docker build -t eiffel-gerrit-plugin:1.0.1 --build-arg URL=https://arm101-eiffel006.rnd.ki.sw.ericsson.se:8443/nexus/...../.../eiffel-gerrit-plugin-1.0.4.jar -f src/main/docker/Dockerfile .


## B: Build Gerrit and Eiffel Gerrit Plugin based on local source code changes
1. Build Eiffel Gerrit Plugin artifact:
cd (git root dir)
mvn package -DskipTests

2. Build Eiffel Gerrit Plugin Docker image:
cd (git root dir)/
export URL=target/$(ls target/ | grep '^eiffel-gerrit-plugin-[0-9]*.[0-9]*.[0-9]*.jar')
docker build -t eiffel-gerrit-plugin --build-arg URL=./${URL} -f src/main/docker/Dockerfile .


## Use docker-compose to set up environment for testing

The docker-compose file in this directory can be used to set up the proper
environment for running the integration tests on Eiffel Gerrit Plugin. It is possible to start up Gerrit with Eiffel Gerrit Plugin
also in this environment, to try out local changes. Just
replace the docker image for gerrit service with your locally built one.

Standing in the root directory, run the below command to set up environment:

  docker-compose -f src/main/docker/docker-compose.yml up -d
