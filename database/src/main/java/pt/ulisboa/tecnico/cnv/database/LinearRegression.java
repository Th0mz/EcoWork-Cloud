package pt.ulisboa.tecnico.cnv.database;

import java.util.ArrayList;
import java.util.List;


public class LinearRegression {
    private final List<Double> x_coords, y_coords;
    private Double origin, slope;
    private boolean calculated = false;

    public LinearRegression(List<Double> x, List<Double> y) {
        x_coords = x;
        y_coords = y;
    }

    public void calculateRegression() {

        Double sumOfx = 0.0;
        Double sumOfy = 0.0;

        for(Double x : x_coords) {
            sumOfx  += x;
        }
        Double xBar = sumOfx / x_coords.size();
        Double x2Bar = 0.0;
        for(Double x : x_coords) {
            x2Bar += (x - xBar) * (x - xBar);
        }

        for(Double y : y_coords) {
            sumOfy += y;
        }
        Double yBar = sumOfy / x_coords.size();

        Double xyBar = 0.0;
        for (int i = 0; i < x_coords.size(); i++) {
            xyBar += (x_coords.get(i) - xBar) * (y_coords.get(i) - yBar);
        }

        slope  = xyBar / x2Bar;
        origin = yBar - slope * xBar;
        calculated = true;

    }

    public Double getSlope() {
        if(!calculated) calculateRegression();
        return slope;
    }

    public Double getOrigin() {
        if(!calculated) calculateRegression();
        return origin;
    }

}
