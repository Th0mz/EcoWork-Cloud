# RUN : 
```
mvn install package
java -cp webserver/target/webserver-1.0.0-SNAPSHOT-jar-with-dependencies.jar -Xbootclasspath/a:javassist/target/JavassistWrapper-1.0-jar-with-dependencies.jar -javaagent:javassist-tools/target/JavassistWrapper-1.0-jar-with-dependencies.jar=ServerICount:pt.ulisboa.tecnico.cnv.foxrabbit,pt.ulisboa.tecnico.cnv.compression,pt.ulisboa.tecnico.cnv.insectwar,pt.ulisboa.tecnico.cnv.webserver,javax.imageio:output pt.ulisboa.tecnico.cnv.webserver.WebServer
```
