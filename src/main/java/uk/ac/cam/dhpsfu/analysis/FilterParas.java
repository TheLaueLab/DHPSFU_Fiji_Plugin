package uk.ac.cam.dhpsfu.analysis;

public class FilterParas {

		
	  public boolean enableFilters;                          // true if enable all filters
	  public boolean enableFilterCalibRange;              // remove localisations out of the angular range of calibration; if False, polynomial fit is extrapolated beyond the range.
	  public boolean enableFilterDistance;                  // remove dots with unexpected distances
	  public double distanceDev;                             // relative deviation of the distance between dots, compared to calibration
	  public boolean enableFilterIntensityRatio;           // filter based on the ratio of intensities between the dots
	  public double intensityDev;                              // relative deviation of the intensity difference between dots, compared to calibration

		
		
	  /**
	   * Instantiates a new fitting parameter result.
	   *
	   * @param frame the frame
	   * @param origX the original X position
	   * @param origY the original Y position
	   * @param origValue the original value
	   * @param error the error
	   * @param noise the noise
	   * @param meanIntensity the mean intensity
	   * @param params the params (must not be null and must have at least {@value #STANDARD_PARAMETERS}
	   *        parameters)
	   * @param paramsStdDev the params standard deviations (if not null must match the length of the
	   *        params array)
	   * @throws IllegalArgumentException the illegal argument exception if the parameters are invalid
	   */
	  public FilterParas(boolean enableFilters, boolean enableFilterCalibRange, boolean enableFilterDistance, double distanceDev, boolean enableFilterIntensityRatio, double intensityDev) {
	    this.enableFilters = enableFilters;
	    this.enableFilterCalibRange = enableFilterCalibRange;
	    this.enableFilterDistance = enableFilterDistance;
	    this.distanceDev = distanceDev;
	    this.enableFilterIntensityRatio = enableFilterIntensityRatio;
	    this.intensityDev = intensityDev; 
	  }
		  
	  public boolean isEnableFilters() {
		    return enableFilters;
		  }
	  public void setEnableFilters(boolean enableFilters) {
		  this.enableFilters = enableFilters;
	  }

	  
	  public boolean isEnableFilterCalibRange() {
		    return enableFilterCalibRange;
	  }
	  public void setEnableFilterCalibRange(boolean enableFilterCalibRange) {
		  this.enableFilterCalibRange = enableFilterCalibRange;
	  }
	  
	  public boolean isEnableFilterDistance() {
		    return enableFilterDistance;
		  }
	  public void setEnableFilterDistance(boolean enableFilterDistance) {
		  this.enableFilterDistance = enableFilterDistance;
	  }

	  
	  public double getDistanceDev() {
		    return distanceDev;
		  }
	  public void setDistanceDev(double distanceDev) {
		  this.distanceDev = distanceDev;
	  }

	  
	  public boolean isEnableFilterIntensityRatio() {
		    return enableFilterIntensityRatio;
		  }
	  public void setEnableFilterIntensityRatio(boolean enableFilterIntensityRatio) {
		  this.enableFilterIntensityRatio = enableFilterIntensityRatio;
	  }
	  
	  public double getIntensityDev() {
		    return intensityDev;
		  }
	  public void setIntensityDev(double intensityDev) {
		  this.intensityDev = intensityDev;
	  }

	}
