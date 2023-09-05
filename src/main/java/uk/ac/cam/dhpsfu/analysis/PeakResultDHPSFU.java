package uk.ac.cam.dhpsfu.analysis;

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


import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;
import org.apache.commons.lang3.ArrayUtils;
import uk.ac.sussex.gdsc.core.utils.MathUtils;
import uk.ac.sussex.gdsc.smlm.results.PeakResult;

/**
 * Specifies a peak fitting result.
 */
public class PeakResultDHPSFU extends PeakResult implements Serializable  {
  private static final long serialVersionUID = 20190319L;

  /** Index of the background in the parameters array. */
  public static final int BACKGROUND = 0;
  /** Index of the intensity in the parameters array. */
  public static final int INTENSITY = 1;
  /** Index of the x-position in the parameters array. */
  public static final int X = 2;
  /** Index of the y-position in the parameters array. */
  public static final int Y = 3;
  /** Index of the z-position in the parameters array. */
  public static final int Z = 4;
  /** Index of the precision in the parameters array. */
  public static final int PRECISION = 5;
  /** Number of standard parameters. */
  public static final int STANDARD_PARAMETERS = 6;
  

  private static final String[] NAMES = {"Background", "Intensity", "X", "Y", "Z", "Precision"};

  private int frame;
  private int origX;
  private int origY;
  private float origValue;
  private double error;
  private float noise;
  private float meanIntensity;
  private float fitX;
  private float fitY;
  private float intensity;
  private float precision;

  /**
   * The parameters (for the standard parameters plus any PSF specific parameters). This is never
   * null.
   */
  private float[] params;
  /**
   * The parameter standard deviations (for the standard parameters plus any PSF specific
   * parameters). This may be null or the same length as {@link #params}.
   */
  private float[] paramStdDevs;

  /**
   * Gets the parameter name.
   *
   * <p>Valid for the standard parameters ({@code index <} {@link #STANDARD_PARAMETERS}).
   *
   * @param index the index
   * @return the parameter name
   */
  public static String getParameterName(int index) {
    return NAMES[index];
  }

  /**
   * Instantiates a new peak result.
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
  public PeakResultDHPSFU(int frame, float fitX, float fitY, float intensity, float precision) {
    super(frame, fitX, fitY, intensity);
    params = new float[6];
    params[PRECISION] = precision;
  }
  
  /*
public PeakResultDHPSFU(int frame, float fitX, float fitY, float intensity, float[] params) {
	super(frame, fitX, fitY, intensity);
	this.params = params;
	  }
	  */

  public float getThePrecision() {
	    return params[PRECISION];
	  }
  public void getThePrecision(float precision) {
	  params[PRECISION] = precision;
	  }

  

  public float getFitX() {
    return fitX;
  }
  public void setFitX(float fitX) {
    this.fitX = fitX;
  }
  
  
  public float getFitY() {
    return fitY;
  }
  public void setFitY(float fitY) {
    this.fitY = fitY;
  }
  
  public float getintensity() {
    return intensity;
  }
  public void setintensity(float intensity) {
    this.intensity = intensity;
  }
  
  public float getprecision() {
    return precision;
  }
  public void setprecision(float precision) {
    this.precision = precision;
  }
  
  public float[] getParameters() {
    return params;
  }
  
  public float getParameter(int index) {
	    return params[index];
	  }

  
 // public PeakResultDHPSFU[] toArray() {
//	    return this.results.toArray();
	//  }
 
}
