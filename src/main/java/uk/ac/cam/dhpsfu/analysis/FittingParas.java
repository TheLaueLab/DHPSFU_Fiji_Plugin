package uk.ac.cam.dhpsfu.analysis;

public class FittingParas {
	
	public double[] dx;
	public double[] dy;
	public double[] dz;
	public double[] dd;
	public double[] dr;
	public double[] angleRange;
	
	
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
  public FittingParas(double[] dx, double[] dy, double[] dz, double[] dd, double[] dr, double[] angleRange) {
    this.dx = dx;
    this.dy = dy;
    this.dz = dz;
    this.dd = dd;
    this.dr = dr;
    this.angleRange = angleRange; 
  }
	  
  public double[] getDx() {
	    return dx;
	  }
  public void setDx(double[] dx) {
	  this.dx = dx;
  }


  public double[] getDy() {
	    return dy;
	  }
  public void setDy(double[] dy) {
	  this.dy = dy;
  }
  
  public double[] getDz() {
	    return dz;
	  }
  public void setDz(double[] dz) {
	  this.dz = dz;
  }
  
  public double[] getDd() {
	    return dd;
	  }
  public void setDd(double[] dd) {
		this.dd = dd;
	}
  
  public double[] getDr() {
	    return dr;
	  }
  public void setDr(double[] dr) {
	  this.dr = dr;
}
  
  public double[] getAngleRange() {
	    return angleRange;
	  }
public void setAngleRange(double[] angleRange) {
	  this.angleRange = angleRange;
}


	  
	  
	  
	  
	  
	  

}
