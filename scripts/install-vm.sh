#!/bin/bash

source config.sh

# Install java.
cmd="sudo yum update -y; sudo yum install java-11-amazon-corretto.x86_64 -y;"
ssh -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH ec2-user@$(cat instance.dns) $cmd

# Install web server.
scp -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH $DIR/config.sh ec2-user@$(cat instance.dns):
scp -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH $DIR/../webserver/target/webserver-1.0.0-SNAPSHOT-jar-with-dependencies.jar ec2-user@$(cat instance.dns):
scp -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH $DIR/../javassist-tools/target/JavassistWrapper-1.0-jar-with-dependencies.jar ec2-user@$(cat instance.dns):

# Setup web server to start on instance launch.
cmd="chmod +x /home/ec2-user/config.sh ;source /home/ec2-user/config.sh; echo \"java -cp /home/ec2-user/webserver-1.0.0-SNAPSHOT-jar-with-dependencies.jar -Xbootclasspath/a:/home/ec2-user/JavassistWrapper-1.0-jar-with-dependencies.jar -javaagent:/home/ec2-user/JavassistWrapper-1.0-jar-with-dependencies.jar=ServerICount:pt.ulisboa.tecnico.cnv.foxrabbit,pt.ulisboa.tecnico.cnv.compression,pt.ulisboa.tecnico.cnv.insectwar,pt.ulisboa.tecnico.cnv.webserver,javax.imageio:output pt.ulisboa.tecnico.cnv.webserver.WebServer\" | sudo tee -a /etc/rc.local; sudo chmod +x /etc/rc.local"
ssh -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH ec2-user@$(cat instance.dns) $cmd
