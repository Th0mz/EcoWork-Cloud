# RUN : 
```
mvn install package
java -cp webserver/target/webserver-1.0.0-SNAPSHOT-jar-with-dependencies.jar -javaagent:javassist-tools/target/JavassistWrapper-1.0-jar-with-dependencies.jar=ServerICount:pt.ulisboa.tecnico.cnv:output pt.ulisboa.tecnico.cnv.webserver.WebServer
```
