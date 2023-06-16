#!/bin/bash

source config.sh

# ===== Launch New VM ===== #
# Run new instance.
aws ec2 run-instances \
	--image-id resolve:ssm:/aws/service/ami-amazon-linux-latest/amzn2-ami-hvm-x86_64-gp2 \
	--instance-type t2.micro \
	--key-name $AWS_KEYPAIR_NAME \
	--security-group-ids $AWS_SECURITY_GROUP \
	--monitoring Enabled=true | jq -r ".Instances[0].InstanceId" > instance.id
echo "New instance with id $(cat instance.id)."

# Wait for instance to be running.
aws ec2 wait instance-running --instance-ids $(cat instance.id)
echo "New instance with id $(cat instance.id) is now running."

# Extract DNS nane.
aws ec2 describe-instances \
	--instance-ids $(cat instance.id) | jq -r ".Reservations[0].Instances[0].NetworkInterfaces[0].PrivateIpAddresses[0].Association.PublicDnsName" > instance.dns
echo "New instance with id $(cat instance.id) has address $(cat instance.dns)."

# Wait for instance to have SSH ready.
while ! nc -z $(cat instance.dns) 22; do
	echo "Waiting for $(cat instance.dns):22 (SSH)..."
	sleep 0.5
done
echo "New instance with id $(cat instance.id) is ready for SSH access."

# ===== Install VM ===== #
# Install java.
cmd="sudo yum update -y; sudo yum install java-11-amazon-corretto.x86_64 -y;"
ssh -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH ec2-user@$(cat instance.dns) $cmd

# create scripts directory
ssh -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH ec2-user@$(cat instance.dns) "mkdir scripts"

# Install web server.
scp -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH $DIR/../../CNV-lab.pem ec2-user@$(cat instance.dns):
scp -o StrictHostKeyChecking=no -r -i $AWS_EC2_SSH_KEYPAR_PATH ~/aws-cli-bin ec2-user@$(cat instance.dns):

scp -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH $DIR/image.id ec2-user@$(cat instance.dns):~/scripts/
scp -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH $DIR/config_aws.sh ec2-user@$(cat instance.dns):
scp -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH $DIR/../load-balancer/target/load-balancer-1.0-SNAPSHOT-jar-with-dependencies.jar ec2-user@$(cat instance.dns):

# Setup web server to start on instance launch.
cmd="echo \"chmod +x /home/ec2-user/config_aws.sh ;source /home/ec2-user/config_aws.sh;java -cp /home/ec2-user/load-balancer-1.0-SNAPSHOT-jar-with-dependencies.jar pt.ulisboa.tecnico.cnv.LoadBalancer \" | sudo tee -a /etc/rc.local; sudo chmod +x /etc/rc.local"
ssh -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAR_PATH ec2-user@$(cat instance.dns) $cmd

# Requesting an instance reboot.
aws ec2 reboot-instances --instance-ids $(cat instance.id)
echo "Rebooting instance to test web server auto-start."

# Letting the instance shutdown.
sleep 1

# Wait for port 8000 to become available.
while ! nc -z $(cat instance.dns) 8000; do
	echo "Waiting for $(cat instance.dns):8000..."
	sleep 0.5
done

# Sending a query!
echo "Sending a query!"
curl $(cat instance.dns):8000/test\?testing-after-reboot
