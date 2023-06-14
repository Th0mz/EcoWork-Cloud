package pt.ulisboa.tecnico.cnv;


import pt.ulisboa.tecnico.cnv.util.SystemState;
import java.util.Timer;
import java.util.TimerTask;


public class AutoScaler extends TimerTask {

    SystemState _state;
    Timer _timer;
    public AutoScaler(SystemState state){
        _state = state;
        _timer  = new Timer();
    }

    public void start(){
        _timer.schedule(this, 20000, 15000);
        System.out.println("[AS]: Running...");
    }

    public void stop(){
        _timer.cancel();
    }

    @Override
    public void run(){
        System.out.println("[AS]: Scaling...");

       //Update CPU Metrics
        _state.updateCPUMetrics();
        
        //Get Intances;
        ArrayList<InstanceState> currentState = _state.getRunningInstances();
        
        /* AUTO SCALLING LOGIC */
        int count = 0;
        double sum = 0;

        for (InstanceState instance: currentState){
            count ++;
            sum += instance.getCPUAvg();
        }

        double avg = sum/count;
        //Two levels of overload and underload
        // If over/underloaded increase/decrease by 5%
        // If extremely over/underloade increase/decrease by 10%
        // Minimum of 1 instance always runnung
        if (avg >= 80 && avg < 90) {
            launchInstances(Math.ceil(count * 0.05));
        } else if (avg > 90) {
            launchInstances(Math.ceil(count * 0.1));
        } else if ( avg <= 25 && avg > 15) {
            terminateInstances(currentState, Math.ceil(count * 0.05))
        } else if (avg <= 15 && count > 1) {
            terminateInstances(currentState, Math.ceil(count*0.1));
        }
    }

    private void launchInstances(int nrInsNew) {
        for (int i = 0; i < nrInsNew; i++) {
            _state.launchInstance();
        }

    }

    private void terminateInstances(ArrayList<InstanceState> instances, int nrInsTerminate) {
        Collections.sort(instances, new Comparator<InstanceState>() {
            @Override
            public int compare(final InstanceState lhs, InstanceState rhs) {
              return (int)(lhs.getCPUAvg - rhs.getCPUAvg);
            }
          });

        //Terminate k instances with lowest avg CPU usage
        for (int i = 0; i < nrInsTerminate; i++) {
            _state.terminateInstance(instances.get(i).getId());
        }
    }
}