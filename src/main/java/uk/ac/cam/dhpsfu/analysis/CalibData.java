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

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.SplittableRandom;
import uk.ac.sussex.gdsc.smlm.data.config.CalibrationProtos.CameraType;
import uk.ac.sussex.gdsc.smlm.data.config.CalibrationWriter;
import uk.ac.sussex.gdsc.smlm.data.config.PSFProtos.PSF;
import uk.ac.sussex.gdsc.smlm.data.config.PsfProtosHelper;
import uk.ac.sussex.gdsc.smlm.data.config.UnitProtos.DistanceUnit;
import uk.ac.sussex.gdsc.smlm.data.config.UnitProtos.IntensityUnit;
import uk.ac.sussex.gdsc.smlm.data.config.UnitProtos.TimeUnit;
import uk.ac.sussex.gdsc.smlm.results.MemoryPeakResults;
//import uk.ac.sussex.gdsc.smlm.ij.example.analysis.PeakResultDHPSFU;
import uk.ac.sussex.gdsc.smlm.results.AttributePeakResult;

public class CalibData {
	 static String filePath = "E:/Fiji_sampledata/calib.xls";
     static int skipLines = 9;
	  /** No instances. */
	  private CalibData() {}

	  /**
	   * Create a random set of GDSC localisations (with precision).
	   *
	   * <p>The results are calibrated so the values have a unit and the localisations are
	   * associated with a point spread function (PSF). For this example the
	   * only configuration option is the name of the dataset.
	   *
	   * @param name the name of the results
	   * @return the results
	   */
	  public static MemoryPeakResults createRandomResults(String name, String filePath) {
		SplittableRandom rng = new SplittableRandom();
    	Read3DFileCalib importer = new Read3DFileCalib();
    	double [][] data = null;
    	double sx = 1.2;

        try {
            /*
        	List<List<String>> data = importer.readCSV(Paths.get(filePath), skipLines);
            for (List<String> row : data) {
            	*/
        	 data = importer.readCSVDouble(Paths.get(filePath), skipLines);
            /*
        	 for (double[] row : data) {
                 for (double value : row) {
                     //System.out.print(value + "\t");
                 }
                // System.out.println();
             }
             */
         } catch (IOException e) {
             e.printStackTrace();
        }    
	    
        
        double[] frame = ArrayUtils.getColumn(data, 0);
        double[] x = ArrayUtils.getColumn(data, 9);
        double[] y = ArrayUtils.getColumn(data, 10);
        double[] intensity = ArrayUtils.getColumn(data, 8);
        double[] precision = ArrayUtils.getColumn(data, 13);
        
	    MemoryPeakResults results = new MemoryPeakResults();
	    
	    //PeakResult[] test = results.toArray();
	    //double precision = test[0].getPrecision();
	    if (data != null) {
	        results.begin();
		    for (int i = 0; i < data.length; i++) {
		      float[] parameters = new float[7];
		      float s = (float) rng.nextDouble(sx * 0.9, sx * 1.3);
		      
		      
		      parameters[PeakResultDHPSFU.BACKGROUND] = (float)frame[i];
		      parameters[PeakResultDHPSFU.X] = (float)x[i];
		      parameters[PeakResultDHPSFU.Y] = (float)y[i];
		      // Ignore z
		      parameters[PeakResultDHPSFU.INTENSITY] = (float)intensity[i];
		      // The peak width as the Gaussian standard deviation is the first non-standard parameter
		      parameters[PeakResultDHPSFU.PRECISION] = (float)precision[i];
	
		      parameters[PeakResultDHPSFU.STANDARD_PARAMETERS] = s;
		     
		   
		      
		      // Set noise assuming photons have a Poisson distribution
		      float noise = (float) Math.sqrt(parameters[PeakResultDHPSFU.INTENSITY]);
		      
		      //double precision = (double) rng.nextDouble(minPrecision);
		      //PeakResult r = new PeakResult(t, (int) x, (int) y, 0, 0, noise, meanIntensity, params, null);
		      //PeakResult r = new PeakResult(t, parameters[1], parameters[2], parameters[3]);
		      PeakResultDHPSFU r = new PeakResultDHPSFU((int)frame[i], parameters[2], parameters[3], parameters[1], parameters[4]);
		      AttributePeakResult ap = new AttributePeakResult(r);
		      ap.setPrecision(precision[i]); 
		      
		      results.add(ap);
	    }
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
	    cw.getBuilder().getCameraCalibrationBuilder()
	      .setCameraType(CameraType.EMCCD).setBias(100).setQuantumEfficiency(0.95).setReadNoise(1.6);
	    results.setCalibration(cw.getCalibration());

	    PSF.Builder psf = PsfProtosHelper.DefaultOneAxisGaussian2dPsf.INSTANCE.toBuilder();
	    psf.getParametersBuilder(0).setValue(sx);
	    results.setPsf(psf.build());
	    return results;
	  }
	
public String getFilePath() {
	return filePath;
}
}

