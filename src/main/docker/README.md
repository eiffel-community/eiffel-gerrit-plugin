## Build Gerrit and Eiffel Gerrit Plugin based on local source code changes

**Option 1**
Use the included help script:

```bash
cd (git root dir)
./eiffel-gerrit-plugin-script build start
```

For more options in how to use the script, and execute tests you may type:

```bash
./eiffel-gerrit-plugin-script -help
```

**Option 2**
Build Eiffel Gerrit Plugin artifact:

```bash
cd (git root dir)
mvn package -DskipTests
```

Build Eiffel Gerrit Plugin Docker image:

```bash
cd (git root dir)
export URL=target/$(ls target/ | grep '^eiffel-gerrit-plugin-[0-9]*.[0-9]*.[0-9]*.jar')
docker build -t eiffel-gerrit-plugin --build-arg URL=./${URL} -f src/main/docker/Dockerfile .
```

## Use docker-compose to set up environment for testing

The docker-compose file in this directory can be used to set up the proper
environment for running the integration tests on Eiffel Gerrit Plugin.
It is possible to start up Gerrit with Eiffel Gerrit Plugin
also in this environment, to try out local changes. Just
replace the docker image for gerrit service with your locally built one.

Standing in the root directory, run the below command to set up environment:

```bash
docker-compose -f src/main/docker/docker-compose.yml up -d
```