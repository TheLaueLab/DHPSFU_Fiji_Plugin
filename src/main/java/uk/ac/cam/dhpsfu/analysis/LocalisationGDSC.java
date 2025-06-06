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

import java.util.SplittableRandom;
import uk.ac.sussex.gdsc.smlm.data.config.CalibrationProtos.CameraType;
import uk.ac.sussex.gdsc.smlm.data.config.CalibrationWriter;
import uk.ac.sussex.gdsc.smlm.data.config.PSFProtos.PSF;
import uk.ac.sussex.gdsc.smlm.data.config.PsfProtosHelper;
import uk.ac.sussex.gdsc.smlm.data.config.UnitProtos.DistanceUnit;
import uk.ac.sussex.gdsc.smlm.data.config.UnitProtos.IntensityUnit;
import uk.ac.sussex.gdsc.smlm.data.config.UnitProtos.TimeUnit;
import uk.ac.sussex.gdsc.smlm.results.Gaussian2DPeakResultHelper;
import uk.ac.sussex.gdsc.smlm.results.MemoryPeakResults;
//import uk.ac.sussex.gdsc.smlm.ij.example.analysis.PeakResultDHPSFU;
import uk.ac.sussex.gdsc.smlm.results.AttributePeakResult;

public class LocalisationGDSC {

	/** No instances. */
	private LocalisationGDSC() {
	}

	/**
	 * Create a random set of GDSC localisations (with precision).
	 *
	 * <p>
	 * The results are calibrated so the values have a unit and the localisations
	 * are associated with a point spread function (PSF). For this example the only
	 * configuration option is the name of the dataset.
	 *
	 * @param name the name of the results
	 * @return the results
	 */
	public static MemoryPeakResults createRandomResults(String name) {
		SplittableRandom rng = new SplittableRandom();
		int frames = 10;
		// 2D Gaussian standard deviation for the PSF
		double sx = 1.2;

		double maxX = 100;
		double maxY = 50;
		double maxBackground = 1;
		double minIntensity = 500;
		double maxIntensity = 1500;
		int n = 10;
		double minPrecision = 23;

		MemoryPeakResults results = new MemoryPeakResults(n);

		// PeakResult[] test = results.toArray();
		// double precision = test[0].getPrecision();

		results.begin();
		for (int i = 1; i <= n; i++) {
			int t = 1 + rng.nextInt(frames);
			float[] parameters = new float[7];
			float x = (float) rng.nextDouble(maxX);
			float y = (float) rng.nextDouble(maxY);
			float intensity = (float) rng.nextDouble(minIntensity, maxIntensity);
			float s = (float) rng.nextDouble(sx * 0.9, sx * 1.3);

			parameters[PeakResultDHPSFU.BACKGROUND] = (float) rng.nextDouble(maxBackground);
			parameters[PeakResultDHPSFU.X] = x;
			parameters[PeakResultDHPSFU.Y] = y;
			// Ignore z
			parameters[PeakResultDHPSFU.INTENSITY] = intensity;
			// The peak width as the Gaussian standard deviation is the first non-standard
			// parameter
			parameters[PeakResultDHPSFU.PRECISION] = (float) rng.nextDouble(minPrecision);

			parameters[PeakResultDHPSFU.STANDARD_PARAMETERS] = s;

			Math.sqrt(parameters[PeakResultDHPSFU.INTENSITY]);
			Gaussian2DPeakResultHelper.getMeanSignalUsingP05(intensity, s, s);
			double precision = (double) rng.nextDouble(minPrecision);
			// PeakResult r = new PeakResult(t, (int) x, (int) y, 0, 0, noise,
			// meanIntensity, params, null);
			// PeakResult r = new PeakResult(t, parameters[1], parameters[2],
			// parameters[3]);
			PeakResultDHPSFU r = new PeakResultDHPSFU(t, parameters[1], parameters[2], parameters[3], parameters[4]);
			AttributePeakResult ap = new AttributePeakResult(r);
			ap.setPrecision(precision);

			results.add(ap);
		}
		results.end();
		results.sort();
		results.setName(name);

		// Calibrate the results
		CalibrationWriter cw = new CalibrationWriter();
		cw.setIntensityUnit(IntensityUnit.PHOTON);
		cw.setDistanceUnit(DistanceUnit.PIXEL);
		cw.setTimeUnit(TimeUnit.FRAME);
		cw.setExposureTime(50);
		cw.setNmPerPixel(100);
		cw.setCountPerPhoton(45);
		cw.getBuilder().getCameraCalibrationBuilder().setCameraType(CameraType.EMCCD).setBias(100)
				.setQuantumEfficiency(0.95).setReadNoise(1.6);
		results.setCalibration(cw.getCalibration());

		PSF.Builder psf = PsfProtosHelper.DefaultOneAxisGaussian2dPsf.INSTANCE.toBuilder();
		psf.getParametersBuilder(0).setValue(sx);
		results.setPsf(psf.build());
		return results;
	}
}
