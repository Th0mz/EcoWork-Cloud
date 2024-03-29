package pt.ulisboa.tecnico.cnv.util;

import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsRequest;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsResult;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import pt.ulisboa.tecnico.cnv.database.MetricsDB;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Random;
import java.util.Scanner;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class SystemState {

    // Instance information
    public static int AVG_STARTUP = 25;
    public static int CHECK_PENDING = 5;
    public static int CHECK_RUNNING = 5;
    public static int REMOVE_CHECK = 30;
    public static int DB_UPDATE = 60;

    public static String INSTANCE_TYPE = "t2.micro";
    public static String AWS_REGION = System.getenv("AWS_DEFAULT_REGION");
    public static String SECURITY_GROUP_NAME = System.getenv("AWS_SECURITY_GROUP");
    public String SECURITY_GROUP = null;
    public static String KEY_NAME = System.getenv("AWS_KEYPAIR_NAME");

    // TODO : read from image.id file
    public String AMI_ID;

    public static String PROTOCOL = "http://";
    public static String PORT = ":8000";

    private Timer timer = new Timer();

    private AmazonEC2 ec2Client;
    private AmazonCloudWatch cloudWatch;

    // Instances :
    protected ConcurrentHashMap<String, TimerTask> pendingInstances = new ConcurrentHashMap();
    protected ConcurrentHashMap<String, InstanceState> runningInstances = new ConcurrentHashMap<>();

    private Random generator = new Random();

    //Metrics Data : 
    //these are actual hashmaps
    private HashMap<Integer, Double> foxRabbitMetrics;
    private HashMap<String, List<Double>> compressionMetrics;
    private ArrayList<Double> insectWarMetrics;
    private ArrayList<Double> perArmyRatio;
    private double cpuAvg;

    public HashMap<String, List<Double>> getCompressionMetrics() {return compressionMetrics;}
    public HashMap<Integer, Double> getFoxRabbitMetrics() {return foxRabbitMetrics;}
    public ArrayList<Double> getInsectWarMetrics() {return insectWarMetrics;}
    public ArrayList<Double> getPerArmyRatio() {return perArmyRatio;}
    public double getCPUAvg() {return cpuAvg;}
    public void setCPUAvg(double avg) {this.cpuAvg = avg;}

    public SystemState() {

        // Check if the environment variables were set
        if (AWS_REGION == null || SECURITY_GROUP_NAME == null || KEY_NAME == null) {
            System.out.println("Error : Environment variables not set");
            System.exit(0);
        }

        try {
            File imageFile = new File("../scripts/image.id");
            Scanner scanner = new Scanner(imageFile);

            AMI_ID = scanner.nextLine();

        } catch (FileNotFoundException e) {
            // Check if image.id exists
            System.out.println("Error : File scripts/image.id doesn't exist");
            System.exit(-1);
        }

        System.out.println("creating ec2 client");
        this.ec2Client = AmazonEC2ClientBuilder.standard()
                .withRegion(AWS_REGION)
                .withCredentials(new EnvironmentVariableCredentialsProvider())
                .build();

        this.cloudWatch = AmazonCloudWatchClientBuilder.standard()
                .withRegion(AWS_REGION)
                .withCredentials(new EnvironmentVariableCredentialsProvider())
                .build();


        System.out.println("getting security id from name");
        this.getSecurityGroupID();
        System.out.println("Security group " + this.SECURITY_GROUP);

        System.out.println("Launching three instances with image id " + AMI_ID);
        this.launchInstance();
        this.launchInstance();
        this.launchInstance();

        RunningCheckTask runningCheckTask = new RunningCheckTask();
        timer.scheduleAtFixedRate(runningCheckTask, 35000, CHECK_RUNNING * 1000);

        RetrieveDBMetricsTask metricsTask = new RetrieveDBMetricsTask();
        timer.scheduleAtFixedRate(metricsTask, 10000, DB_UPDATE * 1000);

    }

    public boolean isRunning(String id){
        return this.runningInstances.containsKey(id);
    }

    public InstanceState getInstance() {

        if (this.runningInstances.size() == 0) {
            return null;
        }

        InstanceState bestInstance = null;
        for (InstanceState instance : this.runningInstances.values()) {
            if(bestInstance != null){
                //System.out.println("\n\n\nBEST"+ bestInstance.getMetric()+ "\n\n\n");
                //System.out.println("\n\n\nCURRENT"+ instance.getMetric()+ "\n\n\n");
                
            }
            if (bestInstance == null || bestInstance.getMetric() > instance.getMetric()) {
                //System.out.println("BEST CHANGED");
                bestInstance = instance;
            }
        }

        return bestInstance;
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
        InstanceState instance =  this.runningInstances.remove(instanceID);
        if (instance == null) {
            return;
        }

        if (!instance.hasRequests()) {
            // terminate the instance in background
            TerminateInstancesRequest terminateInstancesRequest = new TerminateInstancesRequest()
                    .withInstanceIds(instanceID);

            ec2Client.terminateInstances(terminateInstancesRequest);
            System.out.println("Instance " + instance.getId() + " terminated");
        } else {
            CheckTerminate task = new CheckTerminate(instance);
            timer.schedule(task, REMOVE_CHECK * 1000);
        }
    }

    public ArrayList<InstanceState> getRunningInstances() {
        //Does this need locks?
        return new ArrayList<>(this.runningInstances.values());
    }

    public ArrayList<String> getPending() {
        //Does this need locks?
        return new ArrayList<String>(this.pendingInstances.keySet());
    }

    public int getPendingNr(){
        return pendingInstances.size();
    }

    public void updateDBMetrics() {
        System.out.println("[State]: Updating DB Metrics...");
        this.foxRabbitMetrics = MetricsDB.getFoxRabbitMetrics();
        this.insectWarMetrics = MetricsDB.getInsectWarMetrics();
        this.perArmyRatio = MetricsDB.getPerArmyRatio();
        this.compressionMetrics = MetricsDB.getCompressMetrics();

    }

    public void updateCPUMetrics() {

        System.out.println("Updating CPU metrics");
        // Create a connection to the target URL
        try {
            for (InstanceState instance : this.runningInstances.values()) {
                String link = instance.getUrl() + "/cpu";
                URL url = new URL(link);

                /* Create connection to webserver
                 *  ============================== */
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                // Set request method and headers
                connection.setRequestMethod("GET");
                connection.setDoOutput(true);

                /*  Process received response from webserver
                 *  =========================================*/
                int responseCode = connection.getResponseCode();
                // DEBUG : System.out.println("Response code = " + responseCode);

                InputStream responseStream = connection.getInputStream();
                byte[] responseBody = responseStream.readAllBytes();

                Double avgCPU = Double.parseDouble(new String(responseBody));
                instance.updateCPUAvg(avgCPU);
                System.out.println("Instance " + instance.getId() + " was an average CPU usage of " + avgCPU);
            }
        } catch (Exception e) {
            System.err.println("[updateCPUMetrics] " + e.getMessage());
        }
    }

    public boolean checkInstanceRunning(InstanceState instance) {

        String urlString = instance.getUrl() + "/test";

        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setDoOutput(true);

            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                return false;
            }
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    public void checkRunningInstances () {
        System.out.println("Checking if some instance failed");
        for (InstanceState instance : this.runningInstances.values()) {
            boolean isRunning = checkInstanceRunning(instance);
            if (!isRunning) {
                System.out.println("Instance " + instance.getId() + " stopped running");
                runningInstances.remove(instance.getId());
            }
        }
    }


    private class CheckTerminate extends TimerTask {

        private InstanceState instance;

        public CheckTerminate(InstanceState instance) {
            this.instance = instance;
        }

        @Override
        public void run() {
            if (!this.instance.hasRequests()) {
                TerminateInstancesRequest terminateInstancesRequest = new TerminateInstancesRequest()
                        .withInstanceIds(this.instance.getId());

                ec2Client.terminateInstances(terminateInstancesRequest);
                System.out.println("All requests handled from instance " + instance.getId() + ", terminating it");
                return;
            }

            // reschedule task
            CheckTerminate task = new CheckTerminate(instance);
            timer.schedule(task, REMOVE_CHECK * 1000);
            this.cancel();
        }
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
                InstanceState instanceState = new InstanceState(iid, url);

                WaitForWebserverTask task = new WaitForWebserverTask(instanceState);
                pendingInstances.put(instanceState.getId(), task);

                timer.schedule(task, 3000);

                System.out.println("Instance " + this.instanceID + " is running");

            } else {
                // Instance isn't yet running
                WaitForRunningTask task = new WaitForRunningTask(this.instanceID);
                timer.schedule(task, CHECK_PENDING * 1000);
                pendingInstances.put(instanceID, task);
            }

            this.cancel();
        }
    }


    private class WaitForWebserverTask extends TimerTask {
        private InstanceState instanceState;

        public WaitForWebserverTask(InstanceState instanceState) {
            this.instanceState = instanceState;
        }

        public InstanceState getInstance() {return instanceState;}

        @Override
        public void run() {

            Boolean running = checkInstanceRunning(instanceState);
            if (running) {
                // Webserver is running
                instanceState.setRunning();
                String instanceID = instanceState.getId();
                runningInstances.put(instanceID, instanceState);
                pendingInstances.remove(instanceID);

                System.out.println("Webserver of instance " + instanceID + " is running");
            } else {
                // Webserver isn't yet running
                WaitForWebserverTask task = new WaitForWebserverTask(this.instanceState);
                timer.schedule(task, CHECK_PENDING * 1000);
                pendingInstances.put(this.instanceState.getId(), task);
            }
        }
    }

    private class RunningCheckTask extends TimerTask {
        @Override
        public void run() {
            checkRunningInstances();
        }
    }

    private class RetrieveDBMetricsTask extends TimerTask {
        @Override
        public void run() {
            updateDBMetrics();
        }
    }
}
