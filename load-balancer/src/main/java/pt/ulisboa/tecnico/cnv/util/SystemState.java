package pt.ulisboa.tecnico.cnv.util;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.*;

import java.util.*;

public class SystemState {

    public static String INSTANCE_TYPE = "t2.micro";
    public static String AWS_REGION = System.getenv("AWS_DEFAULT_REGION");
    public static String SECURITY_GROUP_NAME = System.getenv("AWS_SECURITY_GROUP");
    public String SECURITY_GROUP = null;
    public static String KEY_NAME = System.getenv("AWS_KEYPAIR_NAME");


    // TODO : read from image.id file
    public static String AMI_ID = "ami-0565b1205b7daec41";

    private AmazonEC2 ec2Client;

    // Instances :
    private ArrayList<String> pendingInstances = new ArrayList<>();
    private HashMap<String, String> runningInstances = new HashMap<>();

    private Random generator = new Random();

    public SystemState() {

        System.out.println("creating ec2 client");
        this.ec2Client = AmazonEC2ClientBuilder.standard()
                .withRegion(AWS_REGION)
                .withCredentials(new EnvironmentVariableCredentialsProvider())
                .build();

        /*
        BasicAWSCredentials awsCredentials = new BasicAWSCredentials("AKIAUD7FME3RJEN6TCLP", "F2AUuI9FcoEnzPsbp6QN/b2dPmBje3GDjGA0EZaL");
        this.ec2Client = AmazonEC2ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
                .withRegion(AWS_REGION)
                .build();

         */

        System.out.println("getting security id from name");
        this.getSecurityGroupID();
        System.out.println("Security group id = " + this.SECURITY_GROUP);

        System.out.println("Launching three instances");
        this.launchInstance();
        this.launchInstance();
        this.launchInstance();

        System.out.println("Waiting for instances to start");
        try {
            Thread.sleep(30000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        updatePendingInstances();
        System.out.println(pendingInstances);
        System.out.println(runningInstances);

        ArrayList<String> instancesIDs = new ArrayList<>(this.runningInstances.keySet());
        int index = generator.nextInt(instancesIDs.size());
        String instanceID = instancesIDs.get(index);

        System.out.println("terminating instance " + instanceID);
        this.terminateInstance(instanceID);
    }

    public String getInstance() {
        ArrayList<String> instances = new ArrayList<>(this.runningInstances.values());
        int index = generator.nextInt(instances.size());

        return instances.get(index);
    }

    public void getSecurityGroupID() {
        // Retrieve the security group ID based on its name
        DescribeSecurityGroupsRequest describeRequest = new DescribeSecurityGroupsRequest()
                .withFilters(new Filter("group-name").withValues(SECURITY_GROUP_NAME));

        DescribeSecurityGroupsResult describeResult = ec2Client.describeSecurityGroups(describeRequest);
        SECURITY_GROUP = describeResult.getSecurityGroups().get(0).getGroupId();
    }

    public void launchInstance() {

        // Create the request to launch the EC2 instance
        RunInstancesRequest runInstancesRequest = new RunInstancesRequest()
                .withImageId(AMI_ID)
                .withInstanceType(INSTANCE_TYPE)
                .withMinCount(1)
                .withMaxCount(1)
                .withKeyName(KEY_NAME)
                .withSecurityGroupIds(SECURITY_GROUP);


        // Launch the EC2 instance
        RunInstancesResult runInstancesResult = ec2Client.runInstances(runInstancesRequest);

        Instance createdInstance = runInstancesResult.getReservation().getInstances().get(0);
        String instanceId = createdInstance.getInstanceId();

        this.pendingInstances.add(instanceId);

    }

    public void updatePendingInstances() {
        DescribeInstancesRequest describeRequest = new DescribeInstancesRequest()
                .withInstanceIds(this.pendingInstances);

        DescribeInstancesResult describeResult = ec2Client.describeInstances(describeRequest);
        List<Reservation> reservations = describeResult.getReservations();

        for (Reservation reservation : reservations) {
            List<Instance> instances = reservation.getInstances();
            if (instances.size() > 1) {
                System.out.println("Error : reservation with more that one instance on it");
            }

            Instance instance = instances.get(0);
            String iid = instance.getInstanceId();
            String ipAddress = instance.getPublicIpAddress();

            if (instance.getState().getCode() == 16) {
                String url = "http://" + ipAddress + ":8000";
                this.runningInstances.put(iid, url);
                this.pendingInstances.remove(iid);
            }
        }
    }

    public void terminateInstance(String instanceID) {

        // remove instance from the running structure
        this.runningInstances.remove(instanceID);

        // terminate the instance in background
        TerminateInstancesRequest terminateInstancesRequest = new TerminateInstancesRequest()
                .withInstanceIds(instanceID);

        TerminateInstancesResult terminateInstancesResult = ec2Client.terminateInstances(terminateInstancesRequest);

        /*
        // Error checking
        if (terminateInstancesResult.getTerminatingInstances().isEmpty()) {
            System.out.println("Instance termination initiated successfully.");
        } else {
            System.out.println("Error terminating instance: " + terminateInstancesResult.getTerminatingInstances().get(0).getPreviousState().getName());
        }
        */
    }
}
