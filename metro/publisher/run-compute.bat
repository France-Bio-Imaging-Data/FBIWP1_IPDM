#!/bin/bash

java -Xmx1024m -classpath apps/IcyProtocol/:lib/icore.jar:lib/iclient.jar:lib/icompute.jar:lib/axis1.4.2.jar:lib/axis-ant1.4.jar:lib/axis-schema.jar:lib/jaxrpc1.1.jar:lib/saaj1.2.jar:lib/wsdl4j-1.5.1.jar:lib/gson-1.6.jar:lib/commons-exec-1.1.jar:lib/commons-logging-1.0.4.jar:lib/commons-discovery-0.2.jar:lib/commons-httpclient-3.0.1.jar:lib/commons-codec-1.3.jar:lib/pbs4java.jar com.strandgenomics.imaging.icompute.ComputeDaemon icompute.properties publisher.properties

