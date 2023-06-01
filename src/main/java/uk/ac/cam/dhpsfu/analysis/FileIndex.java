package uk.ac.cam.dhpsfu.analysis;

public class FileIndex {

	  private static int frameIndex;
	  private static int xIndex;
	  private static int yIndex;
	  private static int zIndex;
	  private static int intensityIndex;
	  private static int precisionIndex;
		
		
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
	  public FileIndex(int frameIndex, int xIndex, int yIndex, int zIndex, int intensityIndex, int precisionIndex) {
	    this.frameIndex = frameIndex;
	    this.xIndex = xIndex;
	    this.yIndex = yIndex;
	    this.zIndex = zIndex;
	    this.intensityIndex = intensityIndex;
	    this.precisionIndex = precisionIndex;
	  }
		  
	  public int getFrameIndex() {
		    return frameIndex;
		  }
	  public void setFrameIndex(int frameIndex) {
		  this.frameIndex = frameIndex;
	  }


	  public int getxIndex() {
		    return xIndex;
		  }
	  public void setxIndex(int xIndex) {
		  this.xIndex = xIndex;
	  }
	  
	  public int getyIndex() {
		    return yIndex;
		  }
	  public void setyIndex(int yIndex) {
		  this.yIndex = yIndex;
	  }
	  
	  public int getzIndex() {
		    return zIndex;
		  }
	  public void setzIndex(int zIndex) {
		  this.zIndex = zIndex;
	  }
	  
	  public int getIntensityIndex() {
		    return intensityIndex;
		  }
	  public void setIntensityIndex(int intensityIndex) {
			this.intensityIndex = intensityIndex;
		}
	  
	  public int getPrecisionIndex() {
		    return precisionIndex;
		  }
	  public void setPrecisionIndex(int precisionIndex) {
		  this.precisionIndex = precisionIndex;
	}
	  



}
