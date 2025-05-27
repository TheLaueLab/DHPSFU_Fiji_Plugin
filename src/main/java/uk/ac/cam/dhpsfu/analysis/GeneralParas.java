/*-
 * #%L
 * Double Helix PSF SMLM analysis tool.
 * %%
 * Copyright (C) 2024 Laue Lab
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

public class GeneralParas {
	public double pxSize = 210; // nm
	public double precisionCutoff = 100; // precision cutoff in nm
	public double calibStep = 33.3; // step length of calibration in nm
	public int polyDegree = 20; // Polynomial fitting degree
	public String fittingMode = "Frame"; // fitting mode, can be 'Frame', 'Angle', or 'Z'. Default is 'Frame'
	public int[] rangeToFit = { 5, 114 }; // range for fitting. Units: 'Z' mode in nm; 'Angle' mode in degrees; 'Frame'
											// mode in number. Default is (1, 97) in frames
	public int[] initialDistanceFilter = { 3, 8 }; // minimum and maximum distance between a pair of dots in px
	public int frameNumber = 10000;

	public GeneralParas(double pxSize, double precisionCutoff, double calibStep, String fittingMode, int[] rangeToFit,
			int[] initialDistanceFilter, int frameNumber, int polyDegree) {
		this.pxSize = pxSize;
		this.precisionCutoff = precisionCutoff;
		this.calibStep = calibStep;
		this.polyDegree = polyDegree;
		this.fittingMode = fittingMode;
		this.rangeToFit = rangeToFit;
		this.initialDistanceFilter = initialDistanceFilter;
		this.frameNumber = frameNumber;
	}

	public double getPxSize() {
		return pxSize;
	}

	public void setPxSize(double pxSize) {
		this.pxSize = pxSize;
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
	
	public int getPolyDegree() {
		return polyDegree;
	}

	public void setPolyDegree(int polyDegree) {
		this.polyDegree = polyDegree;
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
