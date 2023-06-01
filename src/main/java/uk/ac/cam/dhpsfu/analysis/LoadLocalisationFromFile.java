package uk.ac.cam.dhpsfu.analysis;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.SplittableRandom;

import uk.ac.sussex.gdsc.smlm.data.config.CalibrationWriter;
import uk.ac.sussex.gdsc.smlm.data.config.PsfProtosHelper;
import uk.ac.sussex.gdsc.smlm.data.config.CalibrationProtos.CameraType;
import uk.ac.sussex.gdsc.smlm.data.config.PSFProtos.PSF;
import uk.ac.sussex.gdsc.smlm.data.config.UnitProtos.DistanceUnit;
import uk.ac.sussex.gdsc.smlm.data.config.UnitProtos.IntensityUnit;
import uk.ac.sussex.gdsc.smlm.data.config.UnitProtos.TimeUnit;
import uk.ac.sussex.gdsc.smlm.results.AttributePeakResult;
import uk.ac.sussex.gdsc.smlm.results.MemoryPeakResults;
import uk.ac.sussex.gdsc.smlm.results.PeakResult;

public class LoadLocalisationFromFile {
	static String datapath = "E:/Fiji_sampledata/slice0.tif.results.xls"; 
    static int skipLines = 8;
    static String savePath;
    static String name;
    static String fileType;
	  /** No instances. */

	// Data paths

  //private boolean saveToFile = true; 
   static String savingFormat = ".3d";      

  
  // column index 
  private static int frameIndex = 0;
  private static int xIndex = 9;
  private static int yIndex = 10;
  private static int zIndex = -1;
  private static int intensityIndex = 8;
  private static int precisionIndex = 13;
  private static FileIndex fileIndex = new FileIndex(frameIndex, xIndex, yIndex, zIndex, intensityIndex, precisionIndex);
  //private static MemoryPeakResults allLocalisations = new MemoryPeakResults();
  
    
    
    
    
	private LoadLocalisationFromFile() {}
	
	
	public static MemoryPeakResults LoadLocalisations(String dataPath, FileIndex fileIndex, String savingFormat, int skipLines) {
		SplittableRandom rng = new SplittableRandom();
    	Read3DFileCalib importer = new Read3DFileCalib();
    	double [][] data = null;
           
        
        if (fileType == "Peakfit") {   
	        if (savingFormat == ".xls") {
	        	try {	
	           	 data = importer.readCSVDouble(Paths.get(dataPath), skipLines);
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
	        } else if (savingFormat == ".csv") {
	        	try {	
		           	 data = importer.readCSVDoubleFile(Paths.get(dataPath), skipLines);
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
	           }
	        	double[] frame = ArrayUtils.getColumn(data, fileIndex.getFrameIndex());
		        double[] x = ArrayUtils.getColumn(data, fileIndex.getxIndex());
		        double[] y = ArrayUtils.getColumn(data, fileIndex.getyIndex());
		        double[] intensity = ArrayUtils.getColumn(data, fileIndex.getIntensityIndex());
		        double[] precision = ArrayUtils.getColumn(data, fileIndex.getPrecisionIndex());
		        
			    MemoryPeakResults results = new MemoryPeakResults();
	
			    if (data != null) {
			        results.begin();
				    for (int i = 0; i < data.length; i++) {
				      float[] parameters = new float[7];
				      float s = (float) rng.nextDouble(1* 0.9, 1 * 1.3);
				      
				      
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
			    psf.getParametersBuilder(0).setValue(1);
			    results.setPsf(psf.build());       
			    return results;
        } else if (fileType == "DHPSFU") {
        	if (savingFormat == ".3d") {
	        	try {	
	           	 data = importer.readCSVDouble(Paths.get(dataPath), skipLines);
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
	        } else if (savingFormat == ".csv") {
	        	try {	
		           	 data = importer.readCSVDoubleFile(Paths.get(dataPath), skipLines);
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
	           }
        	double[] frame = ArrayUtils.getColumn(data, fileIndex.getFrameIndex());
	        double[] x = ArrayUtils.getColumn(data, fileIndex.getxIndex());
	        double[] y = ArrayUtils.getColumn(data, fileIndex.getyIndex());
	        double[] z = ArrayUtils.getColumn(data, fileIndex.getzIndex());
	        double[] intensity = ArrayUtils.getColumn(data, fileIndex.getIntensityIndex());
	        
		    MemoryPeakResults results = new MemoryPeakResults();

		    if (data != null) {
		        results.begin();
		        for (int i = 0; i < frame.length; i++) {
				      float[] parameters = new float[7];		      
				      parameters[PeakResultDHPSFU.BACKGROUND] = (float)frame[i];
				      parameters[PeakResultDHPSFU.X] = (float)x[i];
				      parameters[PeakResultDHPSFU.Y] = (float)y[i];
				      // Ignore z
				      parameters[PeakResultDHPSFU.INTENSITY] = (float)intensity[i];
				      // The peak width as the Gaussian standard deviation is the first non-standard parameter
				      
				      // Set noise assuming photons have a Poisson distribution
				      float noise = (float) Math.sqrt(parameters[PeakResultDHPSFU.INTENSITY]);

				      PeakResult r = new PeakResult((int)frame[i], parameters[2], parameters[3], parameters[1]);
				      //AttributePeakResult ap = new AttributePeakResult(r);
				      r.setZPosition((float)z[i]); 
				      
				      results.add(r);
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
		    psf.getParametersBuilder(0).setValue(1);
		    results.setPsf(psf.build());       
		    return results;      	
        }
        return new MemoryPeakResults();
		  }

}
