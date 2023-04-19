package uk.ac.sussex.gdsc.smlm.ij.example.plugins;

import java.awt.Label;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

import javax.swing.JFileChooser;

import ij.IJ;
import ij.ImageJ;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import uk.ac.sussex.gdsc.core.data.NotImplementedException;
import uk.ac.sussex.gdsc.core.data.utils.TypeConverter;
import uk.ac.sussex.gdsc.core.ij.ImageJUtils;
import uk.ac.sussex.gdsc.core.ij.gui.ExtendedGenericDialog;
import uk.ac.sussex.gdsc.core.utils.SimpleArrayUtils;
import uk.ac.sussex.gdsc.core.utils.TextUtils;
import uk.ac.sussex.gdsc.smlm.data.config.CalibrationWriter;
import uk.ac.sussex.gdsc.smlm.data.config.PsfHelper;
import uk.ac.sussex.gdsc.smlm.data.config.PsfProtosHelper;

import uk.ac.sussex.gdsc.smlm.results.AttributePeakResult;
import uk.ac.sussex.gdsc.smlm.results.Gaussian2DPeakResultHelper;
import uk.ac.sussex.gdsc.smlm.results.MemoryPeakResults;
import uk.ac.sussex.gdsc.smlm.results.PeakResult;
import uk.ac.sussex.gdsc.smlm.results.procedures.PrecisionResultProcedure;
import uk.ac.sussex.gdsc.smlm.results.procedures.StandardResultProcedure;
import uk.ac.sussex.gdsc.smlm.data.config.CalibrationProtos.CameraType;
import uk.ac.sussex.gdsc.smlm.data.config.PSFProtos.PSF;
import uk.ac.sussex.gdsc.smlm.data.config.UnitProtos.IntensityUnit;
import uk.ac.sussex.gdsc.smlm.data.config.CalibrationWriter;
import uk.ac.sussex.gdsc.smlm.data.config.UnitProtos.DistanceUnit;
import uk.ac.sussex.gdsc.smlm.data.config.UnitProtos.IntensityUnit;
import uk.ac.sussex.gdsc.smlm.data.config.UnitProtos.TimeUnit;
import uk.ac.sussex.gdsc.smlm.ij.example.analysis.ArrayUtils;
import uk.ac.sussex.gdsc.smlm.ij.example.analysis.FileIndex;
import uk.ac.sussex.gdsc.smlm.ij.example.analysis.PeakResultDHPSFU;
import uk.ac.sussex.gdsc.smlm.ij.example.analysis.Read3DFileCalib;
import uk.ac.sussex.gdsc.smlm.ij.example.plugins.DHPSFU;
//import uk.ac.sussex.gdsc.smlm.ij.plugins.LoadLocalisationsSettings;
import uk.ac.sussex.gdsc.smlm.ij.plugins.ResultsManager;
import uk.ac.sussex.gdsc.smlm.ij.plugins.SmlmUsageTracker;
import uk.ac.sussex.gdsc.smlm.ij.plugins.LoadLocalisations.LocalisationList;
import uk.ac.sussex.gdsc.smlm.ij.plugins.ResultsManager.InputSource;
import uk.ac.sussex.gdsc.smlm.ij.settings.SettingsManager;

public class LoadLocalisationFIle implements PlugIn {
	private static final String TITLE = "Load Localisations";
	
	
	public static String fileType = "Peakfit";
	// Data paths
  private static String dataPath = "E:/Fiji_sampledata/slice0.tif.results.xls";  // Data path
  private static String savePath;
  private static String name;
  //private boolean saveToFile = true; 
  private static String savingFormat = ".3d";
  
  private static double pxSize = 210;               
  private static String distanceUnit = "px";
  private static String intensityUnit = "photon";
  
  // column index 
  private static int frameIndex = 0;
  private static int xIndex = 9;
  private static int yIndex = 10;
  private static int zIndex = -1;
  private static int intensityIndex = 8;
  private static int precisionIndex = 13;
  private static FileIndex fileIndex = new FileIndex(frameIndex, xIndex, yIndex, zIndex, intensityIndex, precisionIndex);
  
  private int skipLines;
  private static boolean ifSetOwnIndex = false;
  //private static MemoryPeakResults allLocalisations = new MemoryPeakResults();
  private List<MemoryPeakResults> allLocalisationsList = new ArrayList<>();
  
  @Override
  public void run(String arg) {
	  SmlmUsageTracker.recordPlugin(this.getClass(), arg);
	  showDialog();
	  Load();
//	    }
  }
  
  private String selectFile() {
	    JFileChooser fileChooser = new JFileChooser();
	    fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
	    int returnValue = fileChooser.showOpenDialog(null);
	    if (returnValue == JFileChooser.APPROVE_OPTION) {
	        return fileChooser.getSelectedFile().getAbsolutePath();
	    }
	    return null;
	}
  
  private void viewLocalisations(MemoryPeakResults results, String fileType) {	    
	    if (MemoryPeakResults.isEmpty(results)) {
	      IJ.error(TITLE, "No results could be loaded");
	      return;
	    }
	    if (fileType =="DHPSFU") {

	    StandardResultProcedure s = new StandardResultProcedure(results, DistanceUnit.PIXEL, IntensityUnit.PHOTON);

	    s.getTxy();
	    
	    s.getZ();
	    s.getI();
	    int[] frame = s.frame;
	    float[] x = s.x;
	    //System.out.println(x);
	    float[] y = s.y;
	    float[] z = s.z;
	    float[] intensity = s.intensity;

	    ResultsTable t = new ResultsTable();
	    t.setValues("Frame", SimpleArrayUtils.toDouble(frame));
	    t.setValues("X", SimpleArrayUtils.toDouble(x));
	    t.setValues("Y", SimpleArrayUtils.toDouble(y));
	    t.setValues("Z", SimpleArrayUtils.toDouble(z));
	    t.setValues("Intensity (photon)", SimpleArrayUtils.toDouble(intensity));
	    
	    t.show("My results table");   //need to change table name 
	    } else { 
	    	
	    PrecisionResultProcedure p = new PrecisionResultProcedure(results);
	    p.getPrecision(true);
	    double[] precisions = p.precisions;

	    StandardResultProcedure s = new StandardResultProcedure(results, DistanceUnit.PIXEL, IntensityUnit.PHOTON);

	    s.getTxy();
	    s.getI();
	    int[] frame = s.frame;
	    float[] x = s.x;
	    //System.out.println(x);
	    float[] y = s.y;
	    float[] intensity = s.intensity;

	    ResultsTable t = new ResultsTable();
	    t.setValues("Frame", SimpleArrayUtils.toDouble(frame));
	    t.setValues("X", SimpleArrayUtils.toDouble(x));
	    t.setValues("Y", SimpleArrayUtils.toDouble(y));
	    t.setValues("Intensity (photon)", SimpleArrayUtils.toDouble(intensity));
	    t.setValues("Precision (nm)", precisions);
	    t.show("My results table");   //need to change table name     	
	    }
	  }
  
  
  public boolean showDialog() {
	    ExtendedGenericDialog gd = new ExtendedGenericDialog(TITLE);
	    gd.addMessage("File directory:");
	    gd.addFilenameField("File_directory", "");
	    
//	    String filePath = selectFile();
//	    if (filePath == null) {
//	        return false;
//	    }
//	    Label filePathLabel = new Label(filePath);
//	    //gd.addMessage("Selected file:");
//	    gd.add(filePathLabel);
	    
	    
	    gd.addStringField("File_name", "Localisations");
	    String[] formats2 = {"Peakfit", "DHPSFU"};
	    gd.addChoice("Data_format", formats2, formats2[0]);
	    
	    String[] formats = {".3d", ".csv", ".xls"};
	    gd.addChoice("File_format", formats, formats[2]);
	    gd.addNumericField("Pixel size (nm)", pxSize, 1);
	    gd.addCheckbox("Manually set column Index", ifSetOwnIndex);
	    
	    gd.addMessage("Column index:");
	    gd.addNumericField("Header", skipLines, 1);
	    gd.addNumericField("Frame", fileIndex.getIntensityIndex(), 1);
	    gd.addNumericField("X", fileIndex.getxIndex(), 1);
	    gd.addNumericField("Y", fileIndex.getyIndex(), 1);
	    gd.addNumericField("Z", fileIndex.getzIndex(), 1);
	    gd.addNumericField("Intensity", fileIndex.getIntensityIndex(), 1);
	    gd.addNumericField("Precision", fileIndex.getPrecisionIndex(), 1);

	    gd.showDialog();

	    if (gd.wasCanceled()) {
	      return false;
	    }
        //dataPath = filePath;
	    dataPath = gd.getNextString();		    
	    name = gd.getNextString();
	    fileType = gd.getNextChoice();
	    savingFormat = gd.getNextChoice();
	    pxSize = gd.getNextNumber();
	    ifSetOwnIndex = gd.getNextBoolean();
	    
	    if (!ifSetOwnIndex) {
	    	fileIndex = getColumnIndex(fileType);
	    	if (fileType == "Peakfit") {
	    		skipLines = 9;
	    	} else if (fileType == "DHPSFU") {
	    		skipLines = 0;
	    	}
	    } else {
	    skipLines = (int) gd.getNextNumber();
	    frameIndex = (int) gd.getNextNumber();
	    xIndex = (int) gd.getNextNumber();
	    yIndex = (int) gd.getNextNumber();
	    zIndex = (int) gd.getNextNumber();
	    intensityIndex = (int) gd.getNextNumber();
	    precisionIndex = (int) gd.getNextNumber();
	    
	    fileIndex.setFrameIndex(frameIndex);
	    fileIndex.setxIndex(xIndex);
	    fileIndex.setyIndex(yIndex);
	    fileIndex.setzIndex(zIndex);
	    fileIndex.setIntensityIndex(intensityIndex);
	    fileIndex.setPrecisionIndex(precisionIndex);
	    }
	    return true;   
	  }  // End of shoeDialog
  
  
  public FileIndex getColumnIndex(String fileType) {
	  FileIndex fileIndex = new FileIndex(frameIndex, xIndex, yIndex, zIndex, intensityIndex, precisionIndex);
	  if (fileType == "Peakfit") {
		  fileIndex.setFrameIndex(0);
		  fileIndex.setxIndex(9);
		  fileIndex.setyIndex(10);
		  fileIndex.setzIndex(-1);
		  fileIndex.setIntensityIndex(8);
		  fileIndex.setPrecisionIndex(13);
	  } else if (fileType == "DHPSFU") {
		  fileIndex.setFrameIndex(4);
		  fileIndex.setxIndex(0);
		  fileIndex.setyIndex(1);
		  fileIndex.setzIndex(2);
		  fileIndex.setIntensityIndex(3);
		  fileIndex.setPrecisionIndex(-1);	 		  
		  }		  
		  return fileIndex;
		  }
	  
	  
	  
  public static MemoryPeakResults LoadLocalisations(String dataPath, FileIndex fileIndex, String savingFormat, int skipLines) {
		SplittableRandom rng = new SplittableRandom();
    	Read3DFileCalib importer = new Read3DFileCalib();
    	double [][] data = null;
    	
    	 MemoryPeakResults results = new MemoryPeakResults();
    	 results.setName(name);
           
        
        if (fileType == "Peakfit") {   
	        if (savingFormat == ".xls") {
	        	try {	
	           	 data = importer.readCSVDouble(Paths.get(dataPath), skipLines);
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
			    //results.setName(name);
	
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

	            } catch (IOException e) {
	                e.printStackTrace();
	           }
	        } else if (savingFormat == ".csv") {
	        	try {	
		           	 data = importer.readCSVDoubleFile(Paths.get(dataPath), skipLines);
		            } catch (IOException e) {
		                e.printStackTrace();
		           } 
	           }
        	double[] frame = ArrayUtils.getColumn(data, fileIndex.getFrameIndex());
	        double[] x = ArrayUtils.getColumn(data, fileIndex.getxIndex());
	        for (int i = 0; i < x.length; i++) {
	            x[i] = x[i] / pxSize;
	        }
	        double[] y = ArrayUtils.getColumn(data, fileIndex.getyIndex());
	        for (int i = 0; i < y.length; i++) {
	            y[i] = y[i] / pxSize;
	        }
	        double[] z = ArrayUtils.getColumn(data, fileIndex.getzIndex());
	        for (int i = 0; i < z.length; i++) {
	            z[i] = z[i] / pxSize;
	        }
	        double[] intensity = ArrayUtils.getColumn(data, fileIndex.getIntensityIndex());
	        

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
		        //results.setName(name);

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

		    //PSF.Builder psf = PsfProtosHelper.DefaultOneAxisGaussian2dPsf.INSTANCE.toBuilder();
		    //psf.getParametersBuilder(0).setValue(1);
		    //results.setPsf(psf.build());       
		    return results;      	
        }
        return results;
		  }
	  

  
	  public void Load() {
		  FileIndex fileIndex = getColumnIndex(fileType);
		  MemoryPeakResults localisations = LoadLocalisations(dataPath, fileIndex, savingFormat, skipLines);		    
		    if (localisations == null) {
		      // Cancelled
		      return;
		    }

		    if (localisations.isEmpty()) {
		      IJ.error(TITLE, "No localisations could be loaded");
		      return;
		    }

		    // Create the in-memory results
		    if (localisations.size() > 0) {
		      MemoryPeakResults.addResults(localisations);
		    }
		    
		    CalibrationWriter cw = localisations.getCalibrationWriterSafe();

		    cw.setNmPerPixel(pxSize);

		    cw.setDistanceUnit(DistanceUnit.PIXEL);

		    localisations.setCalibration(cw.getCalibration());
		    
//		    //String uniqueName = name
//
//		    // Set the unique name to the loaded localisations
//		    localisations.setName(name);
//
//		    // Add the loaded localisations to memory
//		    MemoryPeakResults.addResults(localisations);

		    final String msg = "Loaded " + TextUtils.pleural(localisations.size(), "localisation");
		    IJ.showStatus(msg);
		    ImageJUtils.log(msg);
		    
		    viewLocalisations(localisations, fileType);
		  
	  }
	  
}
