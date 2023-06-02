package pt.ulisboa.tecnico.cnv.util;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class SystemState {

    public static String AWS_REGION = "us-east-1";

    private ArrayList<String> instances = new ArrayList<>();
    private AmazonEC2 ec2Client;
    private int instanceIndex = 0;

    public SystemState() {
        this.instances.add("http://localhost:8000");
        BasicAWSCredentials awsCredentials = new BasicAWSCredentials("AKIAUD7FME3RJEN6TCLP", "F2AUuI9FcoEnzPsbp6QN/b2dPmBje3GDjGA0EZaL");

        System.out.println("creating ec2 client");
        this.ec2Client = AmazonEC2ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
                .withRegion(AWS_REGION)
                .build();



        Set<Instance> instances = new HashSet<Instance>();
        for (Reservation reservation : this.ec2Client.describeInstances().getReservations()) {
            instances.addAll(reservation.getInstances());
        }

        System.out.println("instances : ");
        for (Instance instance : instances) {
            String iid = instance.getInstanceId();
            String state = instance.getState().getName();
            System.out.println("iid : " + iid + "  state : " + state);
        }

    }

    public String getInstance() {
        this.instanceIndex = (this.instanceIndex + 1) % this.instances.size();
        return this.instances.get(this.instanceIndex);
    }
}
