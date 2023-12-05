package uk.ac.cam.dhpsfu.analysis;

import uk.ac.sussex.gdsc.smlm.results.MemoryPeakResults;

public class LoadedResult {
    public  MemoryPeakResults results;
    public double[][] resultArray;

    public LoadedResult(MemoryPeakResults results, double[][] resultArray){
        this.results = results;
        this.resultArray = resultArray;

    }

    public MemoryPeakResults getResults() {
        return results;
    }

    public void setResults(MemoryPeakResults results) {
        this.results = results;
    }

    public double[][] getResultArray() {
        return resultArray;
    }

    public void setResultArray(double[][] resultArray) {
        this.resultArray = resultArray;
    }
}
