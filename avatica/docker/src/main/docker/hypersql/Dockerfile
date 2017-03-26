FROM openjdk:8-jre
MAINTAINER Apache Avatica <dev@calcite.apache.org>

# Create an avatica user
RUN useradd -m avatica
RUN mkdir -p /home/avatica/classpath

# Dependencies
ADD avatica-standalone-server-1.10.0-SNAPSHOT-shaded.jar /home/avatica/classpath
ADD https://repo1.maven.org/maven2/net/hydromatic/scott-data-hsqldb/0.1/scott-data-hsqldb-0.1.jar /home/avatica/classpath/
ADD https://repo1.maven.org/maven2/org/hsqldb/hsqldb/2.3.1/hsqldb-2.3.1.jar /home/avatica/classpath/

# Make sure avatica owns its files
RUN chown -R avatica: /home/avatica

# Expose the default port as a convenience
EXPOSE 8765
# Switch off of the root user
USER avatica

ENTRYPOINT ["/usr/lib/jvm/java-8-openjdk-amd64/bin/java", "-cp", "/home/avatica/classpath/*", "org.apache.calcite.avatica.standalone.StandaloneServer", "-u", "jdbc:hsqldb:res:scott"]
