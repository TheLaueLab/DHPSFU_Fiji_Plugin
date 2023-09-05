package uk.ac.cam.dhpsfu.analysis;

import org.apache.commons.math3.complex.Complex;

import java.util.ArrayList;

public class DCresult {
    ArrayList<double[]> data_corrected;
    double[][] drift_by_loc;
    ArrayList<ArrayList<Double>> drift_by_frame;

    public DCresult(ArrayList<double[]> data_corrected, double[][] drift_by_loc, ArrayList<ArrayList<Double>> drift_by_frame) {
        this.data_corrected = data_corrected;
        this.drift_by_frame = drift_by_frame;
        this.drift_by_loc = drift_by_loc;
    }

    public ArrayList<double[]> getData_corrected() {
        return data_corrected;
    }

    public ArrayList<ArrayList<Double>> getDrift_by_frame() {
        return drift_by_frame;
    }

    public double[][] getDrift_by_loc() {
        return drift_by_loc;
    }

    public void setData_corrected(ArrayList<double[]> data_corrected) {
        this.data_corrected = data_corrected;
    }

    public void setDrift_by_frame(ArrayList<ArrayList<Double>> drift_by_frame) {
        this.drift_by_frame = drift_by_frame;
    }

    public void setDrift_by_loc(double[][] drift_by_loc) {
        this.drift_by_loc = drift_by_loc;
    }
}
