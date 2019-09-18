
## Performance tests  ##
The performance tests can be automatically run with the dockerfile "Dockerfile_performance". The performance tests
won't make any asserts but will generate a folder called "report" wherever you choose. This report folder
contains HTML files where graphs and other statistics from the test are stored. These can then be compared between
different versions of the plugin.

### Running the performance tests ###
1. Start the docker-compose file. Instructions in src/main/docker folder.
2. docker build -t jmeter-docker --build-arg TEST_PLAN=gerrit-plugin-testplan.jmx --build-arg GERRIT_HOST=<HOST_TO_GERRIT> --build-arg GERRIT_PORT=<PORT_TO_GERRIT>\
   --build-arg RABBITMQ_HOST=<HOST_TO_RABBITMQ_NOT_LOCALHOST> --build-arg LOOP_COUNT=<Amount of loops per user> --build-arg NUMBER_OF_USERS=<Amount of simulated users> --build-arg TZ=Europe/Stockholm -f Dockerfile_performance .
3. docker run -v <FULL_PATH_TO_REPORT_FOLDER>:/report jmeter-docker

### Viewing the report ###
The report will be placed inside the path provided in the docker run command (<FULL_PATH_TO_REPORT_FOLDER>). Just open the index.html page.

### Editing the performance tests ###
1. Download Jmeter on your computer and start it.
2. Open the gerrit-plugin-testplan.jmx file.
3. Happy editing <(^^,)>

