FROM centos:7.5.1804

LABEL maintainer="Eiffel-Community"

ARG JAR_LOCATION

RUN yum -y install openssh-client initscripts sudo

# Add Gerrit packages repository
RUN rpm -i https://gerritforge.com/gerritforge-repo-1-2.noarch.rpm

# Install OpenJDK and Gerrit in two subsequent transactions
# (pre-trans Gerrit script needs to have access to the Java command)
RUN yum -y install java-1.8.0-openjdk
RUN yum -y install gerrit-2.14.16-1
RUN yum -y install bash

USER gerrit
RUN java -jar /var/gerrit/bin/gerrit.war init --batch -d /var/gerrit
RUN java -jar /var/gerrit/bin/gerrit.war reindex -d /var/gerrit

COPY ${JAR_LOCATION} /var/gerrit/plugins/eiffel-plugin.jar

# Allow incoming traffic
EXPOSE 29418 8080 5005

# Start Gerrit
CMD git config -f /var/gerrit/etc/gerrit.config gerrit.canonicalWebUrl http://localhost:8080/ && \
java -jar -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 /var/gerrit/bin/gerrit.war daemon -d /var/gerrit && \
tail -f /var/gerrit/logs/error_log
