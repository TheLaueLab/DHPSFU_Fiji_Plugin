package uk.ac.cam.dhpsfu.analysis;

public class GeneralParas {
	  public double pxSize = 210;                   // nm
	  public double precisionCutoff = 100;          // precision cutoff in nm
	  public double calibStep = 33.3;                             // step length of calibration in nm
	  public String fittingMode = "Frame";                        // fitting mode, can be 'Frame', 'Angle', or 'Z'. Default is 'Frame'
	  public int[] rangeToFit = {5, 114};                        // range for fitting. Units: 'Z' mode in nm; 'Angle' mode in degrees; 'Frame' mode in number. Default is (1, 97) in frames
	  public int[] initialDistanceFilter = {3, 8};              // minimum and maximum distance between a pair of dots in px
	  public int frameNumber = 10000;
	  
	  public GeneralParas(double pxSize, double precisionCutoff, double calibStep, String fittingMode, int[] rangeToFit, int[] initialDistanceFilter, int frameNumber) {
		    this.pxSize = pxSize;
		    this.precisionCutoff = precisionCutoff;
		    this.calibStep = calibStep;
		    this.fittingMode = fittingMode;
		    this.rangeToFit = rangeToFit;
		    this.initialDistanceFilter = initialDistanceFilter; 
		    this.frameNumber = frameNumber;
		  }
			  
		  public double getPxSize() {
			    return pxSize;
			  }
		  public void setPxSize(double pxSize) {
			  this.pxSize= pxSize;
		  }


		  public double getPrecisionCutoff() {
			    return precisionCutoff;
			  }
		  public void setPrecisionCutoff(double precisionCutoff) {
			  this.precisionCutoff = precisionCutoff;
		  }
		  
		  public double getCalibStep() {
			    return calibStep;
			  }
		  public void setCalibStep(double calibStep) {
			  this.calibStep = calibStep;
		  }
		  
		  public String getFittingMode() {
			    return fittingMode;
			  }
		  public void setFittingMode(String fittingMode) {
				this.fittingMode = fittingMode;
			}
		  
		  public int[] getRangeToFit() {
			    return rangeToFit;
			  }
		  public void setRangeToFit(int[] rangeToFit) {
			  this.rangeToFit = rangeToFit;
		}
		  
		  public int[] getInitialDistanceFilter() {
			    return initialDistanceFilter;
			  }
		public void setAngleRange(int[] initialDistanceFilter) {
			  this.initialDistanceFilter = initialDistanceFilter;
		}
		
		public int getFrameNumber() {
		    return frameNumber;
		  }
		public void setFrameNumber(int frameNumber) {
		  this.frameNumber = frameNumber;
	}

}
