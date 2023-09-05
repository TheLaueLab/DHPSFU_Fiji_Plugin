/*-
 * #%L
 * Double Helix PSF SMLM analysis tool.
 * %%
 * Copyright (C) 2023 Laue Lab
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
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
