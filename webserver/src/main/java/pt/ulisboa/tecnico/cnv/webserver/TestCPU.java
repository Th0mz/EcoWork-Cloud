package pt.ulisboa.tecnico.cnv.webserver;

import java.lang.management.ManagementFactory;
import com.sun.management.OperatingSystemMXBean;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.ReentrantLock;;


public class TestCPU extends TimerTask {

    double avg = 0;
    int count = 0;
    final int PERIOD = 30;
    ArrayList<Double> values = new ArrayList<>();
    Timer _timer;
    ReentrantLock lock;

    public TestCPU() {
        _timer = new Timer();
        lock = new ReentrantLock();
    }

    public double getAvg(){
        lock.lock();
        double mean = avg;
        lock.unlock();
        return mean;
    }

    public void start(){
        _timer.schedule(this, 0, 1000);
        System.out.println("[Mertrics]: Running...");
    }

    public void stop(){
        _timer.cancel();
    }

    @Override
    public void run(){
            //System.out.println("[Mertrics]: Collecting...");

            OperatingSystemMXBean operatingSystemMXBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            double cpuLoad = operatingSystemMXBean.getSystemCpuLoad();

            values.add(cpuLoad * 100);

            // Compute Average
            if(count < PERIOD) {
                count++;
            } else {
                values.remove(0);
            }
            lock.lock();
            avg = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            lock.unlock();
            //DEBUG
            //System.out.println(cpuLoad);
            //System.out.println(avg);  
    }
    
}

