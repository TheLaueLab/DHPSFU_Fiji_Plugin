package uk.ac.cam.dhpsfu.analysis;

import org.apache.commons.math3.complex.Complex;

public class Dftresult {
        double[] output;
        Complex[][] Greg;

        public Dftresult(double[] output, Complex[][] Greg) {
            this.output = output;
            this.Greg = Greg;
        }

        public double[] getOutput(){
            return output;
        }
        public void setOutput(double[] px_size){
            this.output = output;
        }

        public Complex[][] getGreg(){
            return Greg;
        }
        public void setGreg(Complex[][] Greg){
            this.Greg = Greg;
        }
}

