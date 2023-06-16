# Setup AWS

## Webserver
``` 
mvn clean install
cd scripts/
```


Configure `config.sh` and create `config_aws.sh` :  
``` 
#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

export PATH=~/aws-cli-bin:$PATH
export AWS_DEFAULT_REGION=us-east-1
export AWS_ACCOUNT_ID=<account_id>
export AWS_ACCESS_KEY_ID=<access_key>
export AWS_SECRET_ACCESS_KEY=<secret_access_key>
export AWS_EC2_SSH_KEYPAR_PATH=/home/ec2-user/CNV-lab.pem
export AWS_SECURITY_GROUP=CNV-ssh+http
export AWS_KEYPAIR_NAME=CNV-lab
``` 

Make sure that `aws-cli-bin` is on `~/`, and your AWS key is named `CNV-lab.pem` and is on project's `../`

```
./create_image.sh
```

## Autoscaler 
Make sure that you've created Webserver instance successfully
```java
// on SystemState.java (line 75) set
File imageFile = new File("/home/ec2-user/scripts/image.id");
```
```
./launch_load_balancer.sh
```

## Load Balancer/Auto Scaler
# Local Run : 
```
mvn install package
java -cp webserver/target/webserver-1.0.0-SNAPSHOT-jar-with-dependencies.jar -Xbootclasspath/a:javassist-tools/target/JavassistWrapper-1.0-jar-with-dependencies.jar -javaagent:javassist-tools/target/JavassistWrapper-1.0-jar-with-dependencies.jar=ServerICount:pt.ulisboa.tecnico.cnv.foxrabbit,pt.ulisboa.tecnico.cnv.compression,pt.ulisboa.tecnico.cnv.insectwar,pt.ulisboa.tecnico.cnv.webserver,javax.imageio:output pt.ulisboa.tecnico.cnv.webserver.WebServer
```
