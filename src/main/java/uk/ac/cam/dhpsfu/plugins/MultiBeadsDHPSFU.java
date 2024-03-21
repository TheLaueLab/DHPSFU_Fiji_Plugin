/*-
 * #%L
 * Double Helix PSF SMLM analysis tool.
 * %%
 * Copyright (C) 2023 - 2024 Laue Lab
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
package uk.ac.cam.dhpsfu.plugins;
	/*
	 * Have no idea of how to write the copyright!
	 */

import ij.IJ;
import ij.ImageJ;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;

import java.io.File;
import java.net.URISyntaxException;
import java.util.List;
import java.util.*;
import java.util.Arrays;
import java.util.Map;
import java.util.Collections;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.swing.SwingUtilities;

import java.util.concurrent.atomic.AtomicInteger;

import uk.ac.sussex.gdsc.core.ij.ImageJUtils;
import uk.ac.sussex.gdsc.core.ij.gui.ExtendedGenericDialog;
import uk.ac.sussex.gdsc.core.utils.SimpleArrayUtils;
import uk.ac.sussex.gdsc.smlm.data.config.CalibrationWriter;
import uk.ac.sussex.gdsc.smlm.data.config.CalibrationProtos.CameraType;
import uk.ac.sussex.gdsc.smlm.data.config.UnitProtos.DistanceUnit;
import uk.ac.sussex.gdsc.smlm.data.config.UnitProtos.IntensityUnit;
import uk.ac.sussex.gdsc.smlm.data.config.UnitProtos.TimeUnit;
import uk.ac.cam.dhpsfu.analysis.CalibData;
import uk.ac.sussex.gdsc.smlm.ij.plugins.ResultsManager;
import uk.ac.sussex.gdsc.smlm.ij.plugins.ResultsManager.InputSource;
import uk.ac.sussex.gdsc.smlm.results.MemoryPeakResults;
import uk.ac.sussex.gdsc.smlm.results.PeakResult;
import uk.ac.sussex.gdsc.smlm.results.procedures.PrecisionResultProcedure;
import uk.ac.sussex.gdsc.smlm.results.procedures.StandardResultProcedure;
import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.QRDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import uk.ac.cam.dhpsfu.analysis.GeneralParas;
import uk.ac.cam.dhpsfu.analysis.PeakResultDHPSFU;
import uk.ac.cam.dhpsfu.analysis.FittingParas;
import uk.ac.cam.dhpsfu.analysis.FilterParas;
//import uk.ac.sussex.gdsc.smlm.ij.example.plugin.ResultManager;
import org.apache.commons.math3.ml.distance.EuclideanDistance;

import java.awt.Button;
import java.awt.FlowLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * DHPSFU
 */

public class MultiBeadsDHPSFU implements PlugIn {

  private static final String TITLE = "Multi Beads DHPSFU";                        			// Plugin title  
  private static final AtomicInteger dataset = new AtomicInteger();    			// Counter for random datasets.

  // Here we persist settings between invocations of the plugin using 'static' members.
  /** Input dataset name. */
  private static String input = "";
  private static String input2 = "";

  // Settings used only by the instance. We could reuse the static members but keeping
  // an instance copy allows them to be changed without affecting the persistence.

  /** Dataset name. */
  private static String name1 = "Calibration";
  private static String name2 = "Peakfit_data"; 
  
  /**
   * Parameters for DHPSFU
   */
  // General parameters
  private double pxSize = 210;                    								 // Pixel size in nm
  private double precisionCutoff = 30;          								 // precision cutoff in nm
  private double calibStep = 33.3;                             					 // Step length of calibration in nm
  private String fittingMode = "Frame";                        					 // Fitting mode, can be 'Frame', 'Angle', or 'Z'. Default is 'Frame'
  private int[] rangeToFit = {5, 114};                        					 // Range for fitting. Units: 'Z' mode in nm; 'Angle' mode in degrees; 'Frame' mode in number. Default is (1, 97) in frames
  private int[] initialDistanceFilter = {3, 8};              					 // Minimum and maximum distance between a pair of dots in px
  private int frameNumber = 10000;
  GeneralParas generalParas = new GeneralParas(pxSize, precisionCutoff, calibStep, fittingMode, rangeToFit, initialDistanceFilter, frameNumber);
  
  // Filtering parameters
  private boolean enableFilters = true;                          				 // true if enable all filters
  private boolean enableFilterCalibRange = true;              							 // remove localisations out of the angular range of calibration; if False, polynomial fit is extrapolated beyond the range.
  private boolean enableFilterDistance = true;                  						 // remove dots with unexpected distances
  private double distanceDev = 0.2;                             				 // relative deviation of the distance between dots, compared to calibration
  private boolean enableFilterIntensityRatio = true;           					 	 // filter based on the ratio of intensities between the dots
  private double intensityDev = 1;                              				 // relative deviation of the intensity difference between dots, compared to calibration
  FilterParas filterParas = new FilterParas(enableFilters, enableFilterCalibRange, enableFilterDistance, distanceDev, enableFilterIntensityRatio, intensityDev);
  
  // Data paths
  private static String calibPath = "E:/Fiji_sampledata/badcalib.xls";  		 // Path for the calibration file
  private static String dataPath = "E:/Fiji_sampledata/slice0.tif.results.xls";  // Data path
  private static String batchCalibPath = "E:/Fiji_sampledata/Multibeads";  // Data path
  private static String savePath;
  
  private static List<String> dataNames;
  //private boolean saveToFile = true; 
  private String savingFormat = ".3d";
  
  
  // Extra options
  private boolean saveToFile = true;   

  @Override
  public void run(String arg) {
//	  ResultManager resultManager = new ResultManager();
//	  List<String> dataNames = resultManager.batchLoad();
//	  String dataNamesString = dataNames.toString();
	  System.out.println("Data names: ");
	showInstruction();
    if (showDialog()) {
    	
    	
    	ImageJUtils.log("Test: " + dataNames);	
      DH_calibration();
      
    }
  }
  
  public void showInstruction(){
      IJ.log(" -------- Instruction about DHPSFU-MultiBeads Plugin ---------");
      IJ.log(" ");
      IJ.log(" Descriptions: ");
      IJ.log("   - On top of the DHPSFU plugin, this plugin allows you to import multiple calibration files.");
      IJ.log("   - This plugin corrects the lateral spacial variation of the DH across the FOV.");
      IJ.log("   - By importing calibration stacks of different regions of the imaging FOV, this algorithm can compute the degree of spacial variation and correct the imaging data. ");
      IJ.log(" ");
      IJ.log(" Parameters: ");
      IJ.log("   - Pixel size (nm): Camera pixel size in nm. ");
      IJ.log("   - Calibration step (nm): Calibration step size in nm. ");
      IJ.log("   - Precision cutoff (nm): Remove localisations with percision greater than this threshold. ");
      IJ.log("   - Fitting mode: Fitting mode, can be 'Frame', 'Angle', or 'Z'. Default is 'Frame'. ");
      IJ.log("   - Range to fit (from)/(to): Only fit data within this selected range. 'Frame' mode in number; 'Angle' mode in degrees; 'Z' mode in nm. ");
      IJ.log(" ");
      IJ.log(" Filtering options: ");
      IJ.log("   - Enable filter calibration range: Remove localisations out of the angular range of calibration; if False, polynomial fit is extrapolated beyond the range. ");
      IJ.log("   - Enable filter distance: Remove DH pairs with unexpected distances. ");
      IJ.log("       - Initial distance filter (from)/(to): Minimum and maximum distance between a pair of dots in pixel. ");    
      IJ.log("       - Distance deviation: Relative deviation of the distance between dots, compared to calibration. ");        
      IJ.log("   - Enable filter intensity ratio: Filter based on the ratio of intensities between the dots. ");
      IJ.log("       - Intensity deviation: Relative deviation of the intensity between dots, compared to calibration. ");    
      IJ.log(" ");
      IJ.log(" File output: ");
      IJ.log("   - Save to file: Save the analysed data to user-speficied directory. ");
      IJ.log("   - Saving format: .3d (essentially a tsv. that can be visualised in ViSP) and .csv. ");
      
     
      
  }
  
  /*
   * Method to show GUI and result table in imageJ 
   */
  private boolean showDialog() {
	    ExtendedGenericDialog gd = new ExtendedGenericDialog(TITLE);
	 // Create a panel with a custom layout to hold the message and button
	    gd.addMessage("Calibration file format: Only support peakfit result from GDSC SMLM.");
	    Panel panel = new Panel();
	    panel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));

	    // Add the message to the panel
	    Label message = new Label("Select Multi Beads Calibration files from:");
	    panel.add(message);

	    Button button = new Button("Calib files");
	    button.addActionListener(new ActionListener() {
	        @Override
	        public void actionPerformed(ActionEvent e) {
	            new Thread(new Runnable() {
	                @Override
	                public void run() {
	                    dataNames = LoadMultiBeadsCalib(batchCalibPath);
	                    ImageJUtils.log("Loaded: " + dataNames.toString());
	                }
	            }).start();
	        }
	    });
	    panel.add(button);
	    // Add the custom panel to the dialog
	    gd.addPanel(panel);
	    
	    
	    //ResultsManager.addInput(gd, "Input", InputSource.FILE);
	    
	    //ResultsManager.addInput(gd, input, InputSource.MEMORY);
	    gd.addMessage("Data file: ");
	    
	    ResultsManager.addInput(gd, input2, InputSource.MEMORY);
	    
	    // Get the values from the input boxes
	    gd.addNumericField("Pixel size (nm)", pxSize, 1);
	    gd.addNumericField("Calibration step (nm)", calibStep, 1);
	    gd.addNumericField("Precision cutoff (nm)", precisionCutoff, 1);
	    gd.addChoice("Fitting mode", new String[]{"Frame", "Angle (degree)", "Z"}, fittingMode);
	    gd.addNumericField("Range to fit (from):", rangeToFit[0], 0);
	    gd.addNumericField("Range to fit (to)", rangeToFit[1], 0);
	    
	    //gd.addCheckbox("Enable filters", enableFilters);
	    
	    // Add filtering parameters
	    gd.addMessage("Filtering options:");
	    gd.addCheckbox("Enable filter calibration range", enableFilterCalibRange);
	    gd.addCheckbox("Enable filter distance", enableFilterDistance);
	    gd.addNumericField("Initial distance filter (from)", initialDistanceFilter[0], 0);
	    gd.addNumericField("Initial distance filter (to)", initialDistanceFilter[1], 0);  
	    gd.addNumericField("Distance deviation", distanceDev, 1);	    
	    gd.addCheckbox("Enable filter intensity ratio", enableFilterIntensityRatio);
	    gd.addNumericField("Intensity deviation", intensityDev, 1);
	    // HI
	    gd.addMessage("File output:");
	    gd.addCheckbox("Saving to file", saveToFile);
	    
	    gd.addDirectoryField("Save_directory", "");
	    String[] formats = {".3d", ".csv"};
	    gd.addChoice("Saving_format", formats, formats[0]);
	    gd.showDialog();

	    if (gd.wasCanceled()) {
	      return false;
	    }
	    
	    //batchCalibPath = gd.getNextString();
	    //input =  ResultsManager.getInputSource(gd);
	    input2 =  ResultsManager.getInputSource(gd);
	    //MemoryPeakResults calibresults = ResultsManager.loadInputResults(input, true, DistanceUnit.PIXEL, IntensityUnit.PHOTON);
	    //MemoryPeakResults calibresults = getResults(input, calibPath);
	    //MemoryPeakResults.addResults(calibresults);
	    
	    //calibresults.setName(input);
	    MemoryPeakResults dataresults = ResultsManager.loadInputResults(input2, true, DistanceUnit.PIXEL, IntensityUnit.PHOTON);
	    //MemoryPeakResults.addResults(dataresults);
	    //dataresults.setName(input2);
	   
	    pxSize = gd.getNextNumber();
	    calibStep = gd.getNextNumber();
	    precisionCutoff = gd.getNextNumber();
	    fittingMode = gd.getNextChoice();
	    rangeToFit[0] = (int) gd.getNextNumber();
	    rangeToFit[1] = (int) gd.getNextNumber();
	    
	    //enableFilters = gd.getNextBoolean();
	    enableFilterCalibRange = gd.getNextBoolean();
	    enableFilterDistance = gd.getNextBoolean();
	    initialDistanceFilter[0] = (int) gd.getNextNumber();
	    initialDistanceFilter[1] = (int) gd.getNextNumber();
	    distanceDev = gd.getNextNumber();
	    enableFilterIntensityRatio = gd.getNextBoolean();
	    intensityDev = gd.getNextNumber();
	    saveToFile = gd.getNextBoolean();
	    savePath = gd.getNextString();
	    savingFormat = gd.getNextChoice();

	    // Update the general parameters and the filtering parameters   
	    name1 = input;
	    name2 = input2;
	    
	    generalParas.setPxSize(pxSize);
	    generalParas.setCalibStep(calibStep);
	    generalParas.setPrecisionCutoff(precisionCutoff);
	    generalParas.setFittingMode(fittingMode);
	    generalParas.setRangeToFit(rangeToFit);
	    generalParas.setAngleRange(initialDistanceFilter);	    
	    
	    filterParas.setEnableFilters(enableFilters);
	    filterParas.setEnableFilterCalibRange(enableFilterCalibRange);
	    filterParas.setEnableFilterDistance(enableFilterDistance);
	    filterParas.setDistanceDev(distanceDev);
	    filterParas.setEnableFilterIntensityRatio(enableFilterIntensityRatio);
	    filterParas.setIntensityDev(intensityDev);
	    

	    return true;   
	  }  // End of shoeDialog
  
  
  
  

  private void viewLocalisations2() {
    MemoryPeakResults results = ResultsManager.loadInputResults(name1, false, null, null);
    
    if (MemoryPeakResults.isEmpty(results)) {
      IJ.error(TITLE, "No results could be loaded");
      return;
    }
    
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
/*
    TextWindow tw = new TextWindow("My Results text window", "T\tx\ty\tIntensity\tPrecision", "", 800, 400);
    for (int i = 0; i < frame.length; i++) {
      tw.append(frame[i] + "\t" + x[i] +"\t" + y[i] +"\t" + intensity[i] + "\t" + precisions[i]);
    }
*/
    ResultsTable t = new ResultsTable();
    t.setValues("Frame", SimpleArrayUtils.toDouble(frame));
    t.setValues("X (px)", SimpleArrayUtils.toDouble(x));
    t.setValues("Y (px)", SimpleArrayUtils.toDouble(y));
    t.setValues("Intensity (photon)", SimpleArrayUtils.toDouble(intensity));
    t.setValues("Precision (nm)", precisions);
    t.show("DHPSFU results");   //need to change table name 
  }
  
  
  /*
   * Method to get results from memory. 
   */
  private static MemoryPeakResults getResults(String fileName, String filePath) {
    // Use a utility class and provide the name for the dataset
    return CalibData.createRandomResults(fileName, filePath);
  }  // End of getResults
  

  /*
   * Method to get results from memory using file name. 
   */
  private static MemoryPeakResults getResults(String fileName) {

     MemoryPeakResults beadData = MemoryPeakResults.getResults(fileName);
    return beadData;
  }  // End of getResults
  
  private List<String> LoadMultiBeadsCalib(String batchCalibPath){
	  ResultManager resultManager = new ResultManager();
	  List<String> dataNames = resultManager.batchLoad();
	  String dataNamesString = dataNames.toString();
	  System.out.println("Data names: " + dataNamesString);
	  return dataNames;
  }
  
  /*
   * Remove bad frames which do not have 2 and only 2 localisations.  
   */
  private List<Integer> removeBadFrame(int[] frame) {
	int frameNum = Arrays.stream(frame).max().getAsInt();
	System.out.println("No. of calibration frames = " + frameNum);
	List<Integer> frameList = Arrays.stream(frame).boxed().collect(Collectors.toList());
	List<Integer> badFrames = IntStream.rangeClosed(1, frameNum)
			  				  .filter(i -> Collections.frequency(frameList, i) == 1 || Collections.frequency(frameList, i) > 2)
			  				  .boxed().collect(Collectors.toList());
    System.out.println("The bad frames are = " + badFrames);
    return badFrames;
  }  // End of removeBadFrame
  
  /*
   *  Remove values from data arrays corresponding to bad frames
   */
  private float[][]  removeValuesForBadFrames(List<Integer> badFrames, int[] frame, float[] x, float[] y, float[] intensity, int[] beadID, double[] precisions) {
    // Convert badFrames to a Set for efficient lookup
    Set<Integer> badFramesSet = new HashSet<>(badFrames);
    int numFrames = x.length;
    int numGoodFrames = numFrames - badFrames.size();
    float[][] goodData = new float[numGoodFrames][6];
    int goodFrameIndex = 0;
    for (int i = 0; i < numFrames; i++) {
        int currentFrame = Math.round(frame[i]);
        if (!badFramesSet.contains(currentFrame)) {
            goodData[goodFrameIndex][0] = frame[i];
            goodData[goodFrameIndex][1] = x[i];
            goodData[goodFrameIndex][2] = y[i];
            goodData[goodFrameIndex][3] = intensity[i];
            goodData[goodFrameIndex][4] = (float)precisions[i];
            goodData[goodFrameIndex][5] = beadID[i];
            goodFrameIndex++;
        }
    }
    return goodData;
  }   // End of removeValuesForBadFrames

  
  /*
   *  Calculate the x, y, distance, intensity, intensity ration and angle of the given dataset 
   */
  private double[][] DHPSFUCalculation(float[][] goodData) {
	  List<Integer> frameList = Arrays.asList(new Integer[goodData.length]);
	  for (int i = 0; i < goodData.length; i++) {
	      frameList.set(i, (int) goodData[i][0]);
	  }
	  //System.out.println("Number of unique frames: " + frameList.size());
	  List<Double> xList = Arrays.asList(new Double[goodData.length]);
	  for (int i = 0; i < goodData.length; i++) {
		  xList.set(i, (double) goodData[i][1]);
	  }
      System.out.println("Number of good data: " + xList.size());
      List<Double> yList = Arrays.asList(new Double[goodData.length]);	  
      for (int i = 0; i < goodData.length; i++) {
		  yList.set(i, (double) goodData[i][2]);
	  }
	  List<Double> intensityList = Arrays.asList(new Double[goodData.length]);
	  for (int i = 0; i < goodData.length; i++) {
		  intensityList.set(i, (double) goodData[i][3]);
	  }
	  
	  List<Double> beadIDList = Arrays.asList(new Double[goodData.length]);
	  for (int i = 0; i < goodData.length; i++) {
		  beadIDList.set(i, (double) goodData[i][5]);
	  }
      List<Integer> trueFrameList = frameList.stream().distinct().collect(Collectors.toList()); 
      
      double[][] calculated = new double[trueFrameList.size()][6];
      
      for (int i = 0; i < trueFrameList.size(); i++) {
          int oneFrame = trueFrameList.get(i);
          List<Integer> indexFrame = IntStream.range(0, frameList.size())
                .filter(j -> frameList.get(j) == oneFrame)
                .boxed().collect(Collectors.toList());
        //System.out.println("indexFrame size: " + indexFrame.size());
        int fm1 = indexFrame.get(0);
        int fm2 = indexFrame.get(indexFrame.size() > 1 ? 1 : 0);           // Retrieves the element at index 1 from the indexFrame list if its size is greater than 1; otherwise, it retrieves the element at index 0
        //System.out.println("fm1: " + fm1 + ", fm2: " + fm2);
        double x1 = xList.get(fm1);
        double x2 = xList.get(fm2);
        double y1 = yList.get(fm1);
        double y2 = yList.get(fm2);
        double avgX = (x1 + x2) / 2;
        double avgY = (y1 + y2) / 2;
        double distXY = Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
        double avgIntensity = (intensityList.get(fm1) + intensityList.get(fm2)) / 2;
        double ratioIntensity = Math.max(intensityList.get(fm1), intensityList.get(fm2)) / Math.min(intensityList.get(fm1), intensityList.get(fm2));
        double angle = Math.atan2(y2 - y1, x2 - x1);
        double beadID = beadIDList.get(fm2);
        if (angle < 0) {
            angle = angle + Math.PI;
        }
        calculated[i] = new double[]{oneFrame, avgX, avgY, distXY, avgIntensity, ratioIntensity, angle, beadID};
    }    
    return calculated;
	}   // End of DHPSFUCalculation
  
  
  /*
   *  Filter the calibration data
   */
  private double[][]  filterData(double[][] calculated, GeneralParas generalParas) {
	    int distanceLowerBound = generalParas.getInitialDistanceFilter()[0];
	    int distanceUpperBound = generalParas.getInitialDistanceFilter()[1];
	    String fittingMode = generalParas.getFittingMode();
	    int rangeLowerBound = generalParas.getRangeToFit()[0];
	    int rangeUpperBound = generalParas.getRangeToFit()[1];
	    double calibStep = generalParas.getCalibStep();

	    int avgDistanceIndex = 3; 	  	 // Index of 'avg_distance' column
	    int frameIndex = 0;				 // Index of 'Frame' column
	    int angleIndex = 6; 			 // Index of 'Angle' column

	    // Count the number of rows that satisfy the distance filter
	    int numRows = 0;
	    for (double[] row : calculated) {
	        if (row[avgDistanceIndex] >= distanceLowerBound && row[avgDistanceIndex] <= distanceUpperBound) {
	            numRows++;
	        }
	    }
	    
	    System.out.println(numRows);

	    double[][] filteredData = new double[numRows][calculated[0].length];
	    int rowIndex = 0;

	    // Filter the data based on distance and mode
	    for (double[] row : calculated) {
	        //if (row[avgDistanceIndex] >= distanceLowerBound && row[avgDistanceIndex] <= distanceUpperBound) {
	            if (fittingMode.equals("Frame") && row[frameIndex] >= rangeLowerBound && row[frameIndex] <= rangeUpperBound) {
	            	filteredData[rowIndex] = row;
	                rowIndex++;
	            } else if (fittingMode.equals("Angle") && row[angleIndex] >= (rangeLowerBound * Math.PI/180) && row[angleIndex] <= (rangeLowerBound * Math.PI/180)) {
	            	filteredData[rowIndex] = row;
	                rowIndex++;
	            } else if (fittingMode.equals("Z") && row[frameIndex] * calibStep >= rangeLowerBound && row[frameIndex] * calibStep <= rangeUpperBound) {
	            	filteredData[rowIndex] = row;
	                rowIndex++;
	            }
	        //}
	    }
	    System.out.println(rowIndex);

	    filteredData = Arrays.copyOf(filteredData, rowIndex);
	    return filteredData;
	}  // End of filterData
  
  
  /*
   *  Polynomial fit of the calibData
   */
  private FittingParas polyFitting(double[][] filteredData, GeneralParas generalParas) {
	  // Add validation checks
	    if (filteredData == null || filteredData.length == 0) {
	        throw new IllegalArgumentException("filteredData must not be null or empty");
	    }
	    for (double[] row : filteredData) {
	        if (row.length < 7) { // Assuming there are 7 columns in the dataset
	            throw new IllegalArgumentException("Each row of filteredData must have at least 7 columns");
	        }
	    }

	    double calibStep = generalParas.getCalibStep();
	    int angleIndex = 6;         // Index of 'Angle' column
	    int frameIndex = 0;         // Index of 'Frame' column
	    int avgXIndex = 1;          // Index of 'avg_x' column
	    int avgYIndex = 2;          // Index of 'avg_y' column
	    int avgDistanceIndex = 3;   // Index of 'avg_distance' column
	    int ratioIndex = 5;         // Index of 'ratio' column
	    int beadIDIndex = 7;        // Index of 'BeadID' column 

	    // Calculate angle range
	    double angleMin = Arrays.stream(filteredData)
	            .mapToDouble(row -> row[angleIndex])
	            .min().orElse(0.0);
	    double angleMax = Arrays.stream(filteredData)
	            .mapToDouble(row -> row[angleIndex])
	            .max().orElse(0.0);
	    double[] angleRange = {angleMin, angleMax};
	    
	    //System.out.println(Arrays.toString(angleRange));


	   // System.out.println("BeadID order: " + beadOrder);

	    List<Double> xnormList = new ArrayList<>();
	    List<Double> ynormList = new ArrayList<>();
	    List<Integer> fnormList = new ArrayList<>();
	    Map<Double, Integer> beadObservations = new HashMap<>();
	    for (double[] row : filteredData) {
	        double beadID = row[beadIDIndex];
	        beadObservations.put(beadID, beadObservations.getOrDefault(beadID, 0) + 1);
	    }
	    int maxOb = Collections.max(beadObservations.values());
	    //System.out.println(maxOb);
	    
	    List<Double> beadOrder = new ArrayList<>(beadObservations.keySet());
	    for (double b : beadOrder) {
	        List<Integer> poses = Arrays.stream(filteredData)
	                .filter(row -> row[beadIDIndex] == b)
	                .sorted(Comparator.comparingDouble(row -> row[frameIndex]))
	                .map(row -> Arrays.asList(filteredData).indexOf(row))
	                .collect(Collectors.toList());
//	                System.out.println("BeadID order: " + b);
//	                for (int d : poses) {
//	                    System.out.print(d + " ");
//	                }
//	                System.out.println(" ");
	                if (poses.size() > 0.95 * maxOb) {
	                    double meanX = poses.stream()
	                            .mapToDouble(p -> filteredData[p][avgXIndex])
	                            .average()
	                            .getAsDouble();
//	                    System.out.println(meanX + " x");
	                    double meanY = poses.stream()
	                            .mapToDouble(p -> filteredData[p][avgYIndex])
	                            .average()
	                            .getAsDouble();
//	                    System.out.println(meanY + " y");
	                    List<Double> xnormBead = poses.stream()
	                            .mapToDouble(p -> filteredData[p][avgXIndex] - meanX)
	                            .boxed()
	                            .collect(Collectors.toList());
	                    List<Double> ynormBead = poses.stream()
	                            .mapToDouble(p -> filteredData[p][avgYIndex] - meanY)
	                            .boxed()
	                            .collect(Collectors.toList());
	                    List<Integer> fnormBead = poses.stream()
	                            .map(p -> (int) filteredData[p][frameIndex])
	                            .collect(Collectors.toList());
	                    xnormList.addAll(xnormBead);
	                    ynormList.addAll(ynormBead);
	                    fnormList.addAll(fnormBead);
	                }
	            };

//	    
//	    System.out.print("xnormList: ");
//	    for (Double d : xnormList) {
//	        System.out.print(d + " ");
//	    }
//	    System.out.println();
//	    
//	    System.out.print("ynormList: ");
//	    for (Double d : ynormList) {
//	        System.out.print(d + " ");
//	    }
//	    System.out.println();
//	    
//	    System.out.print("fnormList: ");
//	    for (int d : fnormList) {
//	        System.out.print(d + " ");
//	    }
//	    System.out.println();
//	    
//	    System.out.println("Length of xnormList: " + xnormList.size());
//	    System.out.println("Length of xnormList: " + ynormList.size());
//	    System.out.println("Length of xnormList: " + fnormList.size());



	    // Convert normalized values to arrays for fitting
	    double[] xnormArr = xnormList.stream().mapToDouble(Double::doubleValue).toArray();
	    double[] ynormArr = ynormList.stream().mapToDouble(Double::doubleValue).toArray();
	    int[] fnormArr = fnormList.stream().mapToInt(Integer::intValue).toArray();
	 // Convert other columns to arrays for fitting
	    int[] fallArr = Arrays.stream(filteredData)
	            .mapToInt(row -> (int) row[frameIndex])
	            .toArray();

	    // Fitting
	    double[] dx = polyFit2(IntStream.of(fnormArr).mapToDouble(f -> f * calibStep).toArray(), xnormArr, 5);
	    double[] dy = polyFit2(IntStream.of(fnormArr).mapToDouble(f -> f * calibStep).toArray(), ynormArr, 5);
	    double[] dz = polyFit(filteredData, angleIndex, frameIndex, 1, calibStep, 5);
	    double[] dd = polyFit(filteredData, angleIndex, avgDistanceIndex, 1, 1, 8);
	    double[] dr = polyFit(filteredData, angleIndex, ratioIndex, 1, 1, 8);
	    
	    
//	    System.out.println("dx is " + Arrays.toString(dx));
//	    System.out.println("dy is " + Arrays.toString(dy));
//	    System.out.println("dz is " + Arrays.toString(dz));
//	    System.out.println("dd is " + Arrays.toString(dd));
//	    System.out.println("dr is " + Arrays.toString(dr));

	    //double[][] fittingResults = polyFitting(filteredData, generalParas);
//	    for (int i = 0; i < fittingResults.length; i++) {
//	        System.out.print("Parameter " + (i+1) + ": ");
//	        for (int j = 0; j < fittingResults[i].length; j++) {
//	            System.out.print(fittingResults[i][j] + " ");
//	        }
//	        System.out.println();
//	    }
	    
	    
	    
	    return new FittingParas(dx, dy, dz, dd, dr, angleRange);
	    
	    
	}  //  End of polyFitting

  
  /*
   *  PolynomialCurveFitter
   */
	private double[] polyFit(double[][] data, int xIndex, int yIndex, double calibStep, double calibStep2, int degree) {
	    WeightedObservedPoints obs = new WeightedObservedPoints();
	    for (double[] row : data) {
	        double x = row[xIndex] * calibStep;
	        double y = row[yIndex] * calibStep2;
	        obs.add(1.0, x, y);
	    }
	    return PolynomialCurveFitter.create(degree).fit(obs.toList());
	}  // End of polyFit
	
	public double[] polyFit2(double[] x, double[] y, int degree) {
	    int n = x.length;
	    double[][] X = new double[n][degree + 1];
	    for (int i = 0; i < n; i++) {
	        for (int j = 0; j <= degree; j++) {
	            X[i][j] = Math.pow(x[i], j);
	        }
	    }
	    RealMatrix matrixX = MatrixUtils.createRealMatrix(X);
	    RealVector vectorY = MatrixUtils.createRealVector(y);
	    RealVector coefficients = new QRDecomposition(matrixX).getSolver().solve(vectorY);
	    return coefficients.toArray();
	}
  
	
	/*
    *  Filter out peakfit data above the precision cutoff
    */
	private double[][] filterDataByPrecision(MemoryPeakResults PeakfitData, double precisionCutoff) {
		PrecisionResultProcedure p = new PrecisionResultProcedure(PeakfitData);
	    p.getPrecision(true);
	    double[] precisions = p.precisions;
	    System.out.println("Precision = " + precisions.length);
	    StandardResultProcedure s = new StandardResultProcedure(PeakfitData, DistanceUnit.PIXEL, IntensityUnit.PHOTON);
	    PeakfitData.getCalibration();
	    
	    s.getTxy();
	    s.getI();
	    int[] frame = s.frame;
	    float[] x = s.x;
	    //System.out.println(frame);
	    float[] y = s.y;
	    float[] intensity = s.intensity;
		
	    List<double[]> filteredData = new ArrayList<>();
	    for (int i = 0; i < precisions.length; i++) {
	        if (precisions[i] < precisionCutoff) {
	            double[] row = new double[5];
	            row[0] = frame[i];
	            row[1] = x[i];
	            row[2] = y[i];
	            row[3] = intensity[i];
	            row[4] = precisions[i];
	            filteredData.add(row);
	        }
	    }
	    double[][] DataFilteredPrecision = new double[filteredData.size()][5];
	    for (int i = 0; i < filteredData.size(); i++) {
	    	DataFilteredPrecision[i] = filteredData.get(i);
	    }

	    return DataFilteredPrecision;
    }    // End of filterDataByPrecision
	
	
	/* 
	 * 
	 */
	private Set<Integer> getUniqueFrames(double[][] DataFilteredPrecision) {
	    Set<Integer> uniqueFrames = new LinkedHashSet<>();
	    for (double[] row : DataFilteredPrecision) {
	    	uniqueFrames.add((int) row[0]);
	    }
	    System.out.println("Unique Frames: " + uniqueFrames.size());
//	    for (int frameValue : uniqueFrames) {
//	        System.out.print(frameValue + " ");
//	    }
//	    System.out.println();
	    return uniqueFrames;
	}  // End of getUniqueFrames
  
	
	private List<double[]> filterDataByFrame(double[][] data, int frame) {
	    List<double[]> filteredData = new ArrayList<>();
	    for (double[] row : data) {
	        if ((int) row[0] == frame) {
	            filteredData.add(row);
	        }
	    }
	    return filteredData;
	}  // End of filterDataByFrame
	
	
   /*
    * Calculate the Euclidean Distance with given XY coordinates
    */
	private double[][] calculateDistances(double[][] xyCoord) {
	    int n = xyCoord.length;
	    double[][] distances = new double[n][n];
	    EuclideanDistance euclideanDistance = new EuclideanDistance();
	    for (int i = 0; i < n; i++) {
	        for (int j = i + 1; j < n; j++) {
	            distances[i][j] = euclideanDistance.compute(xyCoord[i], xyCoord[j]);
	        }
	    }
	    return distances;
	}  // End of calculateDistances
	
	/*
    * Process the peakfit data using general parameters
    */
	private List<List<Double>> processData(double[][] DataFilteredPrecision, GeneralParas generalParas) {
	    Set<Integer> uniqueFrames = getUniqueFrames(DataFilteredPrecision);
	    List<List<Double>> processedResult = new ArrayList<>();

	    List<Double> frames = new ArrayList<>();
        List<Double> dists = new ArrayList<>();
        List<Double> x = new ArrayList<>();
        List<Double> y = new ArrayList<>();
        List<Double> angle = new ArrayList<>();
        List<Double> ratio = new ArrayList<>();
        List<Double> intensity = new ArrayList<>();
        
	    for (int frame : uniqueFrames) {
	        List<double[]> frameData = filterDataByFrame(DataFilteredPrecision, frame);
	        double[] xs = frameData.stream().mapToDouble(row -> row[1]).toArray();
	        double[] ys = frameData.stream().mapToDouble(row -> row[2]).toArray();
	        double[] ints = frameData.stream().mapToDouble(row -> row[3]).toArray();

	        double[][] coordinates = new double[frameData.size()][2];
	        for (int i = 0; i < frameData.size(); i++) {
	            coordinates[i][0] = xs[i];
	            coordinates[i][1] = ys[i];
	        }

	        double[][] distances = calculateDistances(coordinates);

	        for (int i = 0; i < distances.length; i++) {
	            for (int j = 0; j < distances[0].length; j++) {
	                if (distances[i][j] > generalParas.getInitialDistanceFilter()[1] || distances[i][j] < generalParas.getInitialDistanceFilter()[0]) {
	                    distances[i][j] = 0;
	                }
	            }
	        }
	       
	        for (int i = 0; i < distances.length; i++) {
	            for (int j = i + 1; j < distances[0].length; j++) {
	                if (distances[i][j] > 0) {
	                    double ratioVal = Math.max(ints[i], ints[j]) / Math.min(ints[i], ints[j]);
	                    double intensityVal = (ints[i] + ints[j]) / 2;
	                    double angleVal = Math.atan2(ys[j] - ys[i], xs[j] - xs[i]);
	                    double avgX = (xs[i] + xs[j]) / 2;
	                    double avgY = (ys[i] + ys[j]) / 2;

	                    if (angleVal < 0) {
	                        angleVal += Math.PI;
	                    }
	                    //System.out.println(frame);	  
	                    frames.add((double) frame);
	                    //System.out.print(frames);
	                    dists.add(distances[i][j]);
	                    x.add(avgX);
	                    y.add(avgY);
	                    angle.add(angleVal);
	                    ratio.add(ratioVal);
	                    intensity.add(intensityVal);
	              
	                    }
                    }
                }	      	        
	    }
	    //System.out.print(frames);
	    processedResult.add(frames);
        processedResult.add(dists);
        processedResult.add(x);
        processedResult.add(y);
        processedResult.add(angle);
        processedResult.add(ratio);
        processedResult.add(intensity);	  

	    return processedResult;
	}  // End of processData
	
	/*
    * Calculate the xyz coordinates from the polynomial fit
    */
	private List<List<Double>> calculateCoordinates(List<List<Double>> processedResult, FittingParas fittingParas, GeneralParas generalParas) {
	    double zMin = polyval(fittingParas.getDz(), fittingParas.getAngleRange()[0]);
	    List<Double> zN = processedResult.get(4).stream().map(angle -> polyval(fittingParas.getDz(), angle)).collect(Collectors.toList());
	    List<Double> xN = new ArrayList<>();
	    List<Double> yN = new ArrayList<>();

	    for (int i = 0; i < processedResult.get(2).size(); i++) {
	        xN.add((processedResult.get(2).get(i) - (polyval(fittingParas.getDx(), zN.get(i)) - polyval(fittingParas.getDx(), zMin))) * generalParas.getPxSize());
	        yN.add((processedResult.get(3).get(i) - (polyval(fittingParas.getDy(), zN.get(i)) - polyval(fittingParas.getDy(), zMin))) * generalParas.getPxSize());
	    }
	    List<List<Double>> xyzN = new ArrayList<>();
	    xyzN.add(xN);
	    xyzN.add(yN);
	    xyzN.add(zN);

	    return xyzN;
	}  // End of calculateCoordinates
	
	
    private double polyval(double[] coefficients, double x) {
    	double result = 0;
        for (int i = 0; i < coefficients.length; i++) {
            result += coefficients[i] * Math.pow(x, i);
        }
        return result;
    }  // End of polyval
    

    /**
     * Filtering the peakfit data with different filters and parameters
     */
    private List<List<Double>> filterPeakfitData(List<List<Double>> processedResult, List<List<Double>> xyzN, FilterParas filterParas, FittingParas fittingParas) {
    	 int[] marker = new int[processedResult.get(0).size()];
    	    Arrays.fill(marker, 1);

    	    if (filterParas.isEnableFilters()) {
    	        if (filterParas.isEnableFilterCalibRange()) {
    	            List<Integer> lowAngle = IntStream.range(0, processedResult.get(0).size())
    	                    .filter(i -> processedResult.get(4).get(i) < fittingParas.getAngleRange()[0])
    	                    .boxed()
    	                    .collect(Collectors.toList());

    	            List<Integer> highAngle = IntStream.range(0, processedResult.get(0).size())
    	                    .filter(i -> processedResult.get(4).get(i) > fittingParas.getAngleRange()[1])
    	                    .boxed()
    	                    .collect(Collectors.toList());

    	            lowAngle.forEach(i -> marker[i] = -1);
    	            highAngle.forEach(i -> marker[i] = -1);    	                	            
    	        }

    	        if (filterParas.isEnableFilterDistance()) {
    	            List<Integer> largeDistance = IntStream.range(0, processedResult.get(0).size())
    	                    .filter(i -> Math.abs(processedResult.get(1).get(i) - polyval(fittingParas.getDd(), processedResult.get(4).get(i))) / processedResult.get(1).get(i) > filterParas.getDistanceDev())
    	                    .boxed()
    	                    .collect(Collectors.toList());

    	            largeDistance.forEach(i -> marker[i] = -1);    	            	            
    	        }

    	        if (filterParas.isEnableFilterIntensityRatio()) {
    	            List<Integer> badRatio = IntStream.range(0, processedResult.get(0).size())
    	                    .filter(i -> Math.abs(processedResult.get(5).get(i) - polyval(fittingParas.getDr(), processedResult.get(4).get(i))) > filterParas.getIntensityDev())
    	                    .boxed()
    	                    .collect(Collectors.toList());

    	            List<Integer> badRatio2 = IntStream.range(0, processedResult.get(0).size())
    	                    .filter(i -> processedResult.get(5).get(i) > 4)
    	                    .boxed()
    	                    .collect(Collectors.toList());

    	            badRatio.forEach(i -> marker[i] = -1);
    	            badRatio2.forEach(i -> marker[i] = -1);
    	        }
    	    }

    	    List<List<Double>> processedResultWithZN = new ArrayList<>(); 	    
    	    processedResultWithZN.add(xyzN.get(0));
    	    processedResultWithZN.add(xyzN.get(1));
    	    processedResultWithZN.add(xyzN.get(2));
    	    processedResultWithZN.add(processedResult.get(6));
    	    processedResultWithZN.add(processedResult.get(0));
    	    
    	    List<List<Double>> filteredPeakResult = IntStream.range(0, processedResult.get(0).size())
    	            .filter(i -> marker[i] >= 0)
    	            .mapToObj(i -> {
    	                List<Double> row = new ArrayList<>();
    	                for (List<Double> column : processedResultWithZN) {
    	                    row.add(column.get(i));
    	                }
    	                return row;
    	            })
    	            .collect(Collectors.toList());

    	    return filteredPeakResult;
    	}  // End of filterPeakfitData
    
    
    /**
     * Get the save path of the .3d result file
     */
    private String getSavePath(String dataPath) {
    	 String targetSubstring = ".tif.results.xls";
         int index = dataPath.lastIndexOf(targetSubstring);

         if (index == -1) {
             return dataPath + ".3d";
         } else {
             return dataPath.substring(0, index) + ".3d";
         }
     }   // End of getSavePath
    	    
    
    /* 
     * Save the final filtered result to .3D file
     */ 
    private void saveTo3D(List<List<Double>> filteredPeakResult, String fileName, String savingFormat) {
        //Path outputPath = Paths.get(fileName);
    	Path outputPath;
        if (savingFormat == ".3d") {
        	outputPath = Paths.get(savePath, fileName + savingFormat);
        try (BufferedWriter writer = Files.newBufferedWriter(outputPath)) {
            for (List<Double> row : filteredPeakResult) {
                String csvRow = row.stream()
                        .map(Object::toString)
                        .collect(Collectors.joining("\t"));
                writer.write(csvRow);
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error writing to file: " + fileName);
            e.printStackTrace();
        }
        } else { 
        	outputPath = Paths.get(savePath, fileName + savingFormat);
        	try (BufferedWriter writer = Files.newBufferedWriter(outputPath)) {
            for (List<Double> row : filteredPeakResult) {
                String csvRow = row.stream()
                        .map(Object::toString)
                        .collect(Collectors.joining(","));
                writer.write(csvRow);
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error writing to file: " + fileName);
            e.printStackTrace();
        }
        	
        }
        
    }   // End of saveTo3D
    
    /* 
     * View the result in a table in imageJ
     */ 
    private void view3DResult(List<List<Double>> filteredPeakResult) {
  	  
    	double[][] doubleFilteredPeakResult = toDouble(filteredPeakResult);
    	
        double[] frame = doubleFilteredPeakResult[4];
        double[] x = doubleFilteredPeakResult[0];
        double[] y = doubleFilteredPeakResult[1];
        double[] z = doubleFilteredPeakResult[2];
        double[] intensity = doubleFilteredPeakResult[3];

        ResultsTable t = new ResultsTable();
        
        t.setValues("X (px)", x);
        t.setValues("Y (px)", y);
        t.setValues("Z (px)", z);
        t.setValues("Intensity (photon)", intensity);
        t.setValues("Frame", frame);
        t.show("DHPSFU results");
    	}  // End of view3DResult
    
    
    private MemoryPeakResults saveToMemory(List<List<Double>> filteredPeakResult) {
    	String name = "DHPSFUresult"; 
    	double[][] doubleFilteredPeakResult = toDouble(filteredPeakResult);
    	
        double[] frame = doubleFilteredPeakResult[4];
        double[] x = doubleFilteredPeakResult[0];
        double[] y = doubleFilteredPeakResult[1];
        double[] z = doubleFilteredPeakResult[2];
        double[] intensity = doubleFilteredPeakResult[3];
        
        MemoryPeakResults finalResult = new MemoryPeakResults(); 
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
		      
		      finalResult.add(r);
	    }
	    
        finalResult.end();
        finalResult.sort();
        finalResult.setName(name);
        System.out.println();
        return finalResult;
        
    	}  // End of view3DResult
    	  
    	  
/*
 * Main function for DHPSFU	
 */
  private void DH_calibration() {
	// Processing the calibration data
	  
	List<double[][]> calibDataList = new ArrayList<>();
	int index = 0;
	for (String c : dataNames) {
		MemoryPeakResults results = ResultsManager.loadInputResults(c, false, null, null);
	    System.out.println(c);
	    if (MemoryPeakResults.isEmpty(results)) {
	      IJ.error(TITLE, "No calibration results could be loaded");
	      return;
	    }		    
	    PrecisionResultProcedure p = new PrecisionResultProcedure(results);
	    p.getPrecision(true);
	    double[] precisions = p.precisions;
	    StandardResultProcedure s = new StandardResultProcedure(results, DistanceUnit.PIXEL, IntensityUnit.PHOTON);
	    s.getTxy();
	    s.getI();
	    int[] frame = s.frame;
	    float[] x = s.x;
	    //System.out.println(frame);
	    float[] y = s.y;
	    float[] intensity = s.intensity;
	 // Create an int[] with the index of c in dataNames
	    int[] dataIndexes = new int[x.length]; // Assuming x, y, and intensity arrays have the same length
	    for (int i = 0; i < dataIndexes.length; i++) {
	        dataIndexes[i] = index + 1; // Add 1 if you want to start from 1 instead of 0
	    }

	    index++; // Increment the index for the next iteration
	    
        //List<String> cols = Arrays.asList(allData[0]);
	    List<Integer> badFrames = removeBadFrame(frame);
	    float[][] goodData = removeValuesForBadFrames(badFrames, frame, x, y, intensity, dataIndexes, precisions);
	    double[][] calculated = DHPSFUCalculation(goodData);
        calibDataList.add(calculated);
    }
	
	
	int totalRows = 0;
	for (double[][] calibData : calibDataList) {
	    totalRows += calibData.length;
	}
	// Create a new double[][] to store the concatenated data
	double[][] concatenatedData = new double[totalRows][];
	// Copy the rows from each double[][] in the list to the concatenatedData array
	int currentRow = 0;
	for (double[][] calibData : calibDataList) {
	    for (double[] row : calibData) {
	        concatenatedData[currentRow++] = row;
	    }
	}
	
	double[][] filteredData = filterData(concatenatedData, generalParas);
	

    FittingParas fittingParas =  polyFitting(filteredData, generalParas);
	
	

	
	System.out.println("concatenatedData: " + concatenatedData.length);
	System.out.println("filteredData: " + filteredData.length);
//	for (int i = 1; i < 2; i++) {
//        System.out.println("Array " + (i+1) + ":");
//        double[][] calibData = calibDataList.get(i);
//        for (double[] row : calibData) {
//            for (double value : row) {
//                System.out.printf("%.4f ", value);
//            }
//            System.out.println();
//        }
//        System.out.println();
//    }

//    
   // Load the data:
    MemoryPeakResults PeakfitData = ResultsManager.loadInputResults(name2, false, null, null);
    if (MemoryPeakResults.isEmpty(PeakfitData)) {
        IJ.error(TITLE, "No peakfit results could be loaded");
        return;
      }		
//    
    double[][] DataFilteredPrecision = filterDataByPrecision(PeakfitData, precisionCutoff);
//    
//	for (double[] row : DataFilteredPrecision) {
//    for (double value : row) {
//        System.out.printf("%.4f ", value);
//    }
//    System.out.println();
//}
	System.out.println("DataFilteredPrecision: " + DataFilteredPrecision.length);
    
    List<List<Double>> processedResult = processData(DataFilteredPrecision, generalParas);
    List<List<Double>> xyzN = calculateCoordinates(processedResult, fittingParas, generalParas);
    List<List<Double>> filteredPeakResult = filterPeakfitData(processedResult, xyzN, filterParas, fittingParas); 
    
    
//    for (List<Double> innerList : filteredPeakResult) {
//        for (Double value : innerList) {
//            System.out.print(value + " ");
//        }
//        System.out.println();
//    }
//    
//    System.out.println("filteredPeakResult: " + filteredPeakResult.size());
    
    
    //
//    
//    
    // Save files
    if (saveToFile = true) {
        saveTo3D(filteredPeakResult, name2, savingFormat);
        }
    
    
    
    // View localisation
    view3DResult(filteredPeakResult);
    MemoryPeakResults finalResult = saveToMemory(filteredPeakResult);
    MemoryPeakResults.addResults(finalResult);
    
    
    
    
    CalibrationWriter cw = finalResult.getCalibrationWriterSafe();
    cw.setIntensityUnit(IntensityUnit.PHOTON);
    cw.setDistanceUnit(DistanceUnit.NM);
    cw.setTimeUnit(TimeUnit.FRAME);
    cw.setExposureTime(50);
    cw.setNmPerPixel(pxSize);
    cw.setCountPerPhoton(45);
    cw.getBuilder().getCameraCalibrationBuilder()
      .setCameraType(CameraType.EMCCD).setBias(100).setQuantumEfficiency(0.95).setReadNoise(1.6);
    finalResult.setCalibration(cw.getCalibration());
    
    
    System.out.println("No. of localisation left: " + filteredPeakResult.size());
  //System.out.println("No. of localisation left: " + processedResultWithZN.get(0).size());
}  // End of DH_calibration
  
  /*
   * Convert the List<List<Double>> object into double [][]
   */
  private double[][] toDouble(List<List<Double>> list) {
	   int rows = list.size();
	    int cols = list.get(0).size();

	    return IntStream.range(0, cols).mapToObj(col ->
	        IntStream.range(0, rows).mapToDouble(row ->
	        list.get(row).get(col)
	        ).toArray()
	    ).toArray(double[][]::new);
	}   // End of toDouble
	    
  
  /**
   * Main method for debugging.
   *
   * For debugging, it is convenient to have a method that starts ImageJ and calls the plugin, e.g.
   * after setting breakpoints.
   *
   * @param args unused
   * @throws URISyntaxException if the URL cannot be converted to a URI
   */
  public static void main(String[] args) throws URISyntaxException {
    // Set the base directory for plugins
    // see: https://stackoverflow.com/a/7060464/1207769
    Class<MultiBeadsDHPSFU> clazz = MultiBeadsDHPSFU.class;
    java.net.URL url = clazz.getProtectionDomain().getCodeSource().getLocation();
    File file = new File(url.toURI());
    // Note: This returns the base path. ImageJ will find plugins in here that have an
    // underscore in the name. But it will not search recursively through the
    // package structure to find plugins. Adding this at least puts it on ImageJ's
    // classpath so plugins not satisfying these requirements can be loaded.
    System.setProperty("plugins.dir", file.getAbsolutePath());

    // Start ImageJ and exit when closed
    ImageJ imagej = new ImageJ();
    imagej.exitWhenQuitting(true);

    // If this is in a sub-package or has no underscore then manually add the plugin
    String packageName = clazz.getName().replace(clazz.getSimpleName(), "");
    if (!packageName.isEmpty() || clazz.getSimpleName().indexOf('_') < 0) {
      // Add a spacer
      ij.Menus.installPlugin("", ij.Menus.PLUGINS_MENU, "-", "", IJ.getInstance());
      ij.Menus.installPlugin(clazz.getName(),
          ij.Menus.PLUGINS_MENU, clazz.getSimpleName().replace('_', ' '), "", IJ.getInstance());
    }

    // Initialise for testing, e.g. create some random datasets
    //MemoryPeakResults.addResults(getResults(name1, calibPath));
    MemoryPeakResults.addResults(getResults(name2, dataPath));
  

    // Run the plugin
    IJ.runPlugIn(clazz.getName(), "");
  }  // End of main
	}  // End of class


