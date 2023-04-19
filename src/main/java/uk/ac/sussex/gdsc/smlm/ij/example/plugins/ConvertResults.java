package uk.ac.sussex.gdsc.smlm.ij.example.plugins;

//import uk.ac.sussex.gdsc.smlm.ij.plugins;

import ij.IJ;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import java.util.concurrent.atomic.AtomicReference;
import uk.ac.sussex.gdsc.core.ij.gui.ExtendedGenericDialog;
import uk.ac.sussex.gdsc.core.utils.SimpleArrayUtils;
import uk.ac.sussex.gdsc.smlm.data.config.CalibrationProtos.Calibration;
import uk.ac.sussex.gdsc.smlm.data.config.CalibrationReader;
import uk.ac.sussex.gdsc.smlm.data.config.CalibrationWriter;
import uk.ac.sussex.gdsc.smlm.data.config.UnitProtos.AngleUnit;
import uk.ac.sussex.gdsc.smlm.data.config.UnitProtos.DistanceUnit;
import uk.ac.sussex.gdsc.smlm.data.config.UnitProtos.IntensityUnit;
import uk.ac.sussex.gdsc.smlm.ij.example.analysis.PeakResultDHPSFU;
import uk.ac.sussex.gdsc.smlm.ij.plugins.HelpUrls;
import uk.ac.sussex.gdsc.smlm.ij.plugins.ResultsManager;
import uk.ac.sussex.gdsc.smlm.ij.plugins.ResultsManager.InputSource;
import uk.ac.sussex.gdsc.smlm.ij.plugins.SmlmUsageTracker;
import uk.ac.sussex.gdsc.smlm.ij.settings.SettingsManager;
import uk.ac.sussex.gdsc.smlm.results.MemoryPeakResults;
import uk.ac.sussex.gdsc.smlm.results.PeakResult;
import uk.ac.sussex.gdsc.smlm.results.procedures.PrecisionResultProcedure;
import uk.ac.sussex.gdsc.smlm.results.procedures.StandardResultProcedure;



public class ConvertResults implements PlugIn {
	
	/*-
	 * #%L
	 * Genome Damage and Stability Centre SMLM ImageJ Plugins
	 *
	 * Software for single molecule localisation microscopy (SMLM)
	 * %%
	 * Copyright (C) 2011 - 2022 Alex Herbert
	 * %%
	 * This program is free software: you can redistribute it and/or modify
	 * it under the terms of the GNU General Public License as
	 * published by the Free Software Foundation, either version 3 of the
	 * License, or (at your option) any later version.
	 *
	 * This program is distributed in the hope that it will be useful,
	 * but WITHOUT ANY WARRANTY; without even the implied warranty of
	 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	 * GNU General Public License for more details.
	 *
	 * You should have received a copy of the GNU General Public
	 * License along with this program.  If not, see
	 * <http://www.gnu.org/licenses/gpl-3.0.html>.
	 * #L%
	 */



	/**
	 * Allows results held in memory to be converted to different units.
	 */
	  private static final String TITLE = "Convert Results";

	  private static final AtomicReference<String> INPUT_OPTION_REF = new AtomicReference<>("");

	  private String inputOption;
	  private double pxSize;
	  
	  @Override
	  public void run(String arg) {
	    SmlmUsageTracker.recordPlugin(this.getClass(), arg);

	    if (!showInputDialog()) {
	      return;
	    }

	    final MemoryPeakResults results =
	        ResultsManager.loadInputResults(inputOption, false, null, null);
	    if (MemoryPeakResults.isEmpty(results)) {
	      IJ.error(TITLE, "No results could be loaded");
	      return;
	    }

	    if (!showDialog(results)) {
	      return;
	    }
	    
	    viewLocalisations2(results);
	    
	    IJ.showStatus("Converted " + results.getName());
	  }
	  
	  
	  

	  private boolean showInputDialog() {
	    final int size = MemoryPeakResults.countMemorySize();
	    if (size == 0) {
	      IJ.error(TITLE, "There are no fitting results in memory");
	      return false;
	    }

	    final ExtendedGenericDialog gd = new ExtendedGenericDialog(TITLE);
	    gd.addHelp(HelpUrls.getUrl("convert-results"));
	    gd.addMessage("Select results to convert");

	    inputOption = INPUT_OPTION_REF.get();

	    ResultsManager.addInput(gd, inputOption, InputSource.MEMORY);

	    gd.showDialog();

	    if (gd.wasCanceled()) {
	      return false;
	    }

	    inputOption = ResultsManager.getInputSource(gd);
	    INPUT_OPTION_REF.set(inputOption);

	    return true;
	  }

	  private static boolean showDialog(MemoryPeakResults results) {
	    final ExtendedGenericDialog gd = new ExtendedGenericDialog(TITLE);
	    gd.addMessage("Convert the current units for the results");
	    gd.addHelp(HelpUrls.getUrl("convert-results"));

	    final CalibrationReader cr = CalibrationWriter.create(results.getCalibration());

	    gd.addChoice("Distance_unit", SettingsManager.getDistanceUnitNames(),
	        cr.getDistanceUnitValue());
	    gd.addNumericField("Calibration (nm/px)", cr.getNmPerPixel(), 2);
	    gd.addChoice("Intensity_unit", SettingsManager.getIntensityUnitNames(),
	        cr.getIntensityUnitValue());
	    gd.addNumericField("Gain (Count/photon)", cr.getCountPerPhoton(), 2);
	    gd.addChoice("Angle_unit", SettingsManager.getAngleUnitNames(), cr.getAngleUnitValue());

	    gd.showDialog();
	    if (gd.wasCanceled()) {
	      return false;
	    }

	    final CalibrationWriter cw = results.getCalibrationWriterSafe();
	    final DistanceUnit distanceUnit =
	        SettingsManager.getDistanceUnitValues()[gd.getNextChoiceIndex()];
	    cw.setNmPerPixel(Math.abs(gd.getNextNumber()));

	    // Don't set the calibration with bad values
	    if (distanceUnit.getNumber() > 0 && !(cw.getNmPerPixel() > 0)) {
	      IJ.error(TITLE, "Require positive nm/pixel for conversion");
	      return false;
	    }

	    final IntensityUnit intensityUnit =
	        SettingsManager.getIntensityUnitValues()[gd.getNextChoiceIndex()];
	    cw.setCountPerPhoton(Math.abs(gd.getNextNumber()));
	    if (intensityUnit.getNumber() > 0 && !(cw.getCountPerPhoton() > 0)) {
	      IJ.error(TITLE, "Require positive Count/photon for conversion");
	      return false;
	    }

	    final Calibration newCalibration = cw.getCalibration();
	    results.setCalibration(newCalibration);

	    final AngleUnit angleUnit = SettingsManager.getAngleUnitValues()[gd.getNextChoiceIndex()];

	    
	    if (!results.convertToUnits(distanceUnit, intensityUnit, angleUnit)) {
	      IJ.error(TITLE, "Conversion failed");
	      return false;
	    }

	    return true;
	  }
		
	  
	  
	  
//	  private MemoryPeakResults distConverter(MemoryPeakResults results, double pxSize) {	    
//		    PrecisionResultProcedure p = new PrecisionResultProcedure(results);
//
//		    p.getPrecision(true);
//		    double[] precisions = p.precisions;
//
//		    StandardResultProcedure s = new StandardResultProcedure(results);
//
//		    s.getTxy();
//		    s.getI();
//
//		    int[] frame = s.frame;
//		    float[] x = s.x;
//		    //System.out.println(x);
//		    for (int i = 0; i < x.length; i++) {
//	            x[i] = (float) (x[i] / pxSize);
//	        }
//		    float[] y = s.y;
//		    for (int i = 0; i < y.length; i++) {
//	            y[i] = (float) (y[i] / pxSize);
//	        }
//		    float[] intensity = s.intensity;
//		    
//		    MemoryPeakResults finalResult = new MemoryPeakResults(); 
//	        for (int i = 0; i < x.length; i++) {
//			      float[] parameters = new float[7];		      
//			      parameters[PeakResultDHPSFU.BACKGROUND] = (float)frame[i];
//			      parameters[PeakResultDHPSFU.X] = (float)x[i];
//			      parameters[PeakResultDHPSFU.Y] = (float)y[i];
//			      // Ignore z
//			      parameters[PeakResultDHPSFU.INTENSITY] = (float)intensity[i];
//			      // The peak width as the Gaussian standard deviation is the first non-standard parameter
//			      
//			      // Set noise assuming photons have a Poisson distribution
//			      float noise = (float) Math.sqrt(parameters[PeakResultDHPSFU.INTENSITY]);
//
//			      PeakResult r = new PeakResult((int)frame[i], parameters[2], parameters[3], parameters[1]);
//			      //AttributePeakResult ap = new AttributePeakResult(r);
//			      //r.setZPosition((float)z[i]); 
//			      
//			      finalResult.add(r);
//		    }
//		    
//	        finalResult.end();
//	        finalResult.sort();
//	        finalResult.setName("Converted");
//	        return finalResult;
//		    
//	  }
	  
	  
	  
	 private void viewLocalisations2(MemoryPeakResults results) {	    
		    if (MemoryPeakResults.isEmpty(results)) {
		      IJ.error(TITLE, "No results could be loaded");
		      return;
		    }
		    
		    PrecisionResultProcedure p = new PrecisionResultProcedure(results);

		    p.getPrecision(true);
		    double[] precisions = p.precisions;

		    StandardResultProcedure s = new StandardResultProcedure(results);

		    s.getTxy();
		    s.getI();
		    int[] frame = s.frame;
		    float[] x = s.x;
		    //System.out.println(x);
		    float[] y = s.y;
		    float[] intensity = s.intensity;

		    ResultsTable t = new ResultsTable();
		    t.setValues("Frame", SimpleArrayUtils.toDouble(frame));
		    t.setValues("X (px)", SimpleArrayUtils.toDouble(x));
		    t.setValues("Y (px)", SimpleArrayUtils.toDouble(y));
		    t.setValues("Intensity (photon)", SimpleArrayUtils.toDouble(intensity));
		    t.show("Converted Results");   //need to change table name 
		  }
	}
	
	




