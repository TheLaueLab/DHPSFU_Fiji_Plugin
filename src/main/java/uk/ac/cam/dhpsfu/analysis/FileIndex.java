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
	 * @param frame         the frame
	 * @param origX         the original X position
	 * @param origY         the original Y position
	 * @param origValue     the original value
	 * @param error         the error
	 * @param noise         the noise
	 * @param meanIntensity the mean intensity
	 * @param params        the params (must not be null and must have at least
	 *                      {@value #STANDARD_PARAMETERS} parameters)
	 * @param paramsStdDev  the params standard deviations (if not null must match
	 *                      the length of the params array)
	 * @throws IllegalArgumentException the illegal argument exception if the
	 *                                  parameters are invalid
	 */
	public FileIndex(int frameIndex, int xIndex, int yIndex, int zIndex, int intensityIndex, int precisionIndex) {
		FileIndex.frameIndex = frameIndex;
		FileIndex.xIndex = xIndex;
		FileIndex.yIndex = yIndex;
		FileIndex.zIndex = zIndex;
		FileIndex.intensityIndex = intensityIndex;
		FileIndex.precisionIndex = precisionIndex;
	}

	public int getFrameIndex() {
		return frameIndex;
	}

	public void setFrameIndex(int frameIndex) {
		FileIndex.frameIndex = frameIndex;
	}

	public int getxIndex() {
		return xIndex;
	}

	public void setxIndex(int xIndex) {
		FileIndex.xIndex = xIndex;
	}

	public int getyIndex() {
		return yIndex;
	}

	public void setyIndex(int yIndex) {
		FileIndex.yIndex = yIndex;
	}

	public int getzIndex() {
		return zIndex;
	}

	public void setzIndex(int zIndex) {
		FileIndex.zIndex = zIndex;
	}

	public int getIntensityIndex() {
		return intensityIndex;
	}

	public void setIntensityIndex(int intensityIndex) {
		FileIndex.intensityIndex = intensityIndex;
	}

	public int getPrecisionIndex() {
		return precisionIndex;
	}

	public void setPrecisionIndex(int precisionIndex) {
		FileIndex.precisionIndex = precisionIndex;
	}

}
