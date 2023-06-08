package pt.ulisboa.tecnico.cnv.util;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SystemState {

    // Instance information
    public static int AVG_STARTUP = 25;
    public static int CHECK_SLEEP = 5;
    public static String INSTANCE_TYPE = "t2.micro";
    public static String AWS_REGION = System.getenv("AWS_DEFAULT_REGION");
    public static String SECURITY_GROUP_NAME = System.getenv("AWS_SECURITY_GROUP");
    public String SECURITY_GROUP = null;
    public static String KEY_NAME = System.getenv("AWS_KEYPAIR_NAME");

    public static String PROTOCOL = "http://";
    public static String PORT = ":8000";
    private Timer timer = new Timer();


    // TODO : read from image.id file
    public static String AMI_ID = "ami-0565b1205b7daec41";

    private AmazonEC2 ec2Client;

    // Instances :
    protected ConcurrentHashMap<String, WaitForRunningTask> pendingInstances = new ConcurrentHashMap();
    protected ConcurrentHashMap<String, InstanceState> runningInstances = new ConcurrentHashMap<>();

    private Random generator = new Random();

    public SystemState() {

        // Check if the environment variables were set
        if (AWS_REGION == null || SECURITY_GROUP_NAME == null || KEY_NAME == null) {
            System.out.println("Error : Environment variables not set");
            System.exit(0);
        }
        
        System.out.println("creating ec2 client");
        this.ec2Client = AmazonEC2ClientBuilder.standard()
                .withRegion(AWS_REGION)
                .withCredentials(new EnvironmentVariableCredentialsProvider())
                .build();


        System.out.println("getting security id from name");
        this.getSecurityGroupID();
        System.out.println("Security group id = " + this.SECURITY_GROUP);

        System.out.println("Launching three instances");
        this.launchInstance();
        this.launchInstance();
        this.launchInstance();
    }

    public String getInstance() {
        ArrayList<InstanceState> instances = new ArrayList<>(this.runningInstances.values());
        int index = generator.nextInt(instances.size());

        return instances.get(index).getUrl();
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

        // set up a timer to check when do the instance becomes available to use
        WaitForRunningTask task = new WaitForRunningTask(instanceId);
        timer.schedule(task, AVG_STARTUP * 1000);
        pendingInstances.put(instanceId, task);

        System.out.println("Launched instance " + instanceId);
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

    private class WaitForRunningTask extends TimerTask {
        private String instanceID;

        public WaitForRunningTask(String instanceID) {
            this.instanceID = instanceID;
        }

        @Override
        public void run() {
            DescribeInstancesRequest describeRequest = new DescribeInstancesRequest()
                    .withInstanceIds(this.instanceID);

            DescribeInstancesResult describeResult = ec2Client.describeInstances(describeRequest);
            Instance instance = describeResult.getReservations().get(0).getInstances().get(0);

            String iid = instance.getInstanceId();
            if (!this.instanceID.equals(iid)) {
                System.out.println("Error : while checking " + instanceID + " state, came across " + iid + " state");
            }

            String ipAddress = instance.getPublicIpAddress();
            if (instance.getState().getCode() == 16) {
                // Instance is running
                String url = PROTOCOL + ipAddress + PORT;
                runningInstances.put(iid, new InstanceState(url));
                pendingInstances.remove(instanceID);

                System.out.println("Instance " + this.instanceID + " is running");
            } else {
                // Instance isn't yet running
                WaitForRunningTask task = new WaitForRunningTask(this.instanceID);
                timer.schedule(task, CHECK_SLEEP * 1000);
                pendingInstances.put(instanceID, task);
                this.cancel();
            }
        }
    }
}
