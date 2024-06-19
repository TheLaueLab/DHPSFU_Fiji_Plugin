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
package uk.ac.cam.dhpsfu.plugins;

import java.awt.TextField;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;
import org.apache.commons.math3.ml.distance.EuclideanDistance;

import com.opencsv.exceptions.CsvValidationException;

import ij.IJ;
import ij.ImageJ;
import ij.Macro;
import ij.gui.Plot;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import ij.plugin.frame.Recorder;
import uk.ac.cam.dhpsfu.analysis.CalibData;
import uk.ac.cam.dhpsfu.analysis.FilterParas;
import uk.ac.cam.dhpsfu.analysis.FittingParas;
import uk.ac.cam.dhpsfu.analysis.GeneralParas;
import uk.ac.cam.dhpsfu.analysis.PeakResultDHPSFU;
import uk.ac.sussex.gdsc.core.ij.gui.ExtendedGenericDialog;
import uk.ac.sussex.gdsc.smlm.data.config.CalibrationProtos.CameraType;
import uk.ac.sussex.gdsc.smlm.data.config.CalibrationWriter;
import uk.ac.sussex.gdsc.smlm.data.config.UnitProtos.DistanceUnit;
import uk.ac.sussex.gdsc.smlm.data.config.UnitProtos.IntensityUnit;
import uk.ac.sussex.gdsc.smlm.data.config.UnitProtos.TimeUnit;
import uk.ac.sussex.gdsc.smlm.ij.plugins.ResultsManager;
import uk.ac.sussex.gdsc.smlm.ij.plugins.ResultsManager.InputSource;
import uk.ac.sussex.gdsc.smlm.results.MemoryPeakResults;
import uk.ac.sussex.gdsc.smlm.results.PeakResult;
import uk.ac.sussex.gdsc.smlm.results.procedures.PrecisionResultProcedure;
import uk.ac.sussex.gdsc.smlm.results.procedures.StandardResultProcedure;

public class DHPSFU implements PlugIn {

	private static final String TITLE = "DHPSFU"; // Plugin title
	private static String input = ""; // Input dataset name
	private static String input2 = "";

	private static String name1 = "Calibration"; // Dataset name
	private static String name2 = "Peakfit_data";

	/**
	 * Parameters for DHPSFU
	 */
	// General parameters
	private double pxSize = 210; // Pixel size in nm
	private double precisionCutoff = 30; // precision cutoff in nm
	private double calibStep = 33.3; // Step length of calibration in nm
	private String fittingMode = "Frame"; // Fitting mode, can be 'Frame', 'Angle', or 'Z'. Default is 'Frame'
	private int[] rangeToFit = { 5, 114 }; // Range for fitting. Units: 'Z' mode in nm; 'Angle' mode in degrees; 'Frame'
											// mode in number. Default is (1, 97) in frames
	private int[] initialDistanceFilter = { 3, 8 }; // Minimum and maximum distance between a pair of dots in px
	private int frameNumber = 10000;
	GeneralParas generalParas = new GeneralParas(pxSize, precisionCutoff, calibStep, fittingMode, rangeToFit,
			initialDistanceFilter, frameNumber);

	// Filtering parameters
	private boolean enableFilters = true; // true if enable all filters
	private boolean enableFilterCalibRange = true; // remove localisations out of the angular range of calibration; if
													// False,
	// polynomial fit is extrapolated beyond the range.
	private boolean enableFilterDistance = true; // remove dots with unexpected distances
	private double distanceDev = 0.2; // relative deviation of the distance between dots, compared to calibration
	private boolean enableFilterIntensityRatio = true; // filter based on the ratio of intensities between the dots
	private double intensityDev = 1; // relative deviation of the intensity difference between dots, compared to
										// calibration
	FilterParas filterParas = new FilterParas(enableFilters, enableFilterCalibRange, enableFilterDistance, distanceDev,
			enableFilterIntensityRatio, intensityDev);
	// Data paths
	private static String calibPath;
	private static String dataPath;
	private static String savePath;
	private String savingFormat = ".3d";

	// Extra options
	private boolean saveToFile = true;

	@Override
	public void run(String arg) {

		String macroOptions = Macro.getOptions();
		if (showDialog(macroOptions)) {
			DH_calibration();
			// ImageJUtils.log("Loaded Calibration Files: " + name1);
		}
	}

	private boolean showDialog(String arg) {
		if (arg != null) {
			parseArguments(arg);
		}

		ExtendedGenericDialog gd = new ExtendedGenericDialog(TITLE);
		gd.addMessage("Calibration file: ");

		ResultsManager.addInput(gd, input, InputSource.MEMORY);

		gd.addMessage("Data file: ");
		if (arg == null || arg.length() == 0) { // Assuming no arguments means manual mode
			ResultsManager.addInput(gd, input, InputSource.MEMORY);
			// IJ.log("1Input2=" + input2);
		}
		gd.addNumericField("Pixel size (nm)", pxSize, 1);
		gd.addNumericField("Calibration step (nm)", calibStep, 1);
		gd.addNumericField("Precision cutoff (nm)", precisionCutoff, 1);
		gd.addChoice("Fitting mode", new String[] { "Frame", "Angle (degree)", "Z" }, fittingMode);
		gd.addNumericField("Range to fit (from):", rangeToFit[0], 0);
		gd.addNumericField("Range to fit (to)", rangeToFit[1], 0);
		gd.addMessage("Filtering options:");
		gd.addCheckbox("Enable filter calibration range", enableFilterCalibRange);
		gd.addCheckbox("Enable filter distance", enableFilterDistance);
		gd.addNumericField("Initial distance filter (from)", initialDistanceFilter[0], 0);
		gd.addNumericField("Initial distance filter (to)", initialDistanceFilter[1], 0);
		gd.addNumericField("Distance deviation", distanceDev, 1);
		gd.addCheckbox("Enable filter intensity ratio", enableFilterIntensityRatio);
		gd.addNumericField("Intensity deviation", intensityDev, 1);
		gd.addMessage("File output:");
		gd.addCheckbox("Saving to file", saveToFile);
		gd.addDirectoryField("Save_directory", "--Please_Select--");
		String[] formats = { ".3d", ".csv" };
		gd.addChoice("Saving_format", formats, formats[0]);
		String html = "<html>" + "<h2>Instruction about DHPSFU Plugin</h2>"
		// +"<font size=+1>"
				+ "Descriptions: <br>" + "- DHPSFU converts a list of 2D peaks into 3D localisations. <br>"
				+ "- Simply select the calibration file and the data file from the ImageJ memory. Make sure that both of them are in the memory. <br>"
				+ "- The processed 3D data will also be saved in the ImageJ memory. <br>" + "<br>" + "Parameters:  <br>"
				+ "- Pixel size (nm): Camera pixel size in nm. <br>"
				+ "- Calibration step (nm): The distance in z between consecutive frames of the calibration series, in nm.   <br>"
				+ "- Precision cutoff (nm): Prior to analysis, exclude 2D peaks with precision greater than this threshold.  <br>"
				+ "- Range units : Units in which the “Range to fit” is specified. Can be 'Frame', 'Angle', or 'Z'. Default is 'Frame'.   <br>"
				+ "- Range to fit (from)/(to): Prior to model fitting, crop the calibration sequence to the selected range, in terms of the number of frames ('Frame' mode); DH angle in degrees ('Angle' mode); or z-range in nm ('Z' mode).  . <br>"
				+ "<br>" + "Filtering options: <br>"
				+ "- Enable filter calibration range: Remove localisations out of the angular range of calibration; if False, polynomial fit is extrapolated beyond the range.  <br>"
				+ "- Enable filter distance: Remove DH localisations with an unexpected distance between the lobes.  <br>"
				+ "- Initial distance filter (from)/(tos):Minimum and maximum distance between a pair of dots for this pair to be considered a DH localisation (in pixels).<br>"
				+ "** Distance deviation: Allowed relative deviation of the distance between dots, compared to calibration.<br>"
				+ "- Enable filter intensity ratio: Remove DH localisations with an unexpected ratio of intensities between the lobes. <br>"
				+ "** Intensity deviation: Allowed relative deviation of the intensity between dots, compared to calibration.<br>"
				+ "<br>" + "File output:  <br>"
				+ "- Save to file: Save the analysed data to user-specified directory.  <br>"
				+ "- Saving format: .3d (tab-separated, can be visualised directly in ViSP) or .csv (comma-separated). Data format: “x y z Intensity Frame”. <br>"
				+ "<br>" + "</font>";
		gd.addHelp(html);
		gd.showDialog();
		if (gd.wasCanceled()) {
			return false;
		}

		input = ResultsManager.getInputSource(gd);
		IJ.log("Calibration file selected: " + input);

		if (arg == null || arg.length() == 0) {

			input2 = ResultsManager.getInputSource(gd);
			IJ.log("Data file selected: " + input2);
		}

		Vector<?> numericFields = gd.getNumericFields();
		if (numericFields != null && numericFields.size() >= 9) {
			pxSize = getNumericFieldValue(numericFields, 0);
			calibStep = getNumericFieldValue(numericFields, 1);
			precisionCutoff = getNumericFieldValue(numericFields, 2);
			rangeToFit[0] = (int) getNumericFieldValue(numericFields, 3);
			rangeToFit[1] = (int) getNumericFieldValue(numericFields, 4);
			initialDistanceFilter[0] = (int) getNumericFieldValue(numericFields, 5);
			initialDistanceFilter[1] = (int) getNumericFieldValue(numericFields, 6);
			distanceDev = getNumericFieldValue(numericFields, 7);
			intensityDev = getNumericFieldValue(numericFields, 8);
		}

		if (arg == null || arg.length() == 0) {
			fittingMode = gd.getNextChoice();
//			IJ.log("Fitting=" + fittingMode);
			enableFilterCalibRange = gd.getNextBoolean();
			// IJ.log("enableFilterCalibRange=" + enableFilterCalibRange);
			enableFilterDistance = gd.getNextBoolean();
			// IJ.log("enableFilterDistance=" + enableFilterDistance);
			enableFilterIntensityRatio = gd.getNextBoolean();
			// IJ.log("enableFilterIntensityRatio=" + enableFilterIntensityRatio);
			saveToFile = gd.getNextBoolean();
			// IJ.log("saveToFile=" + saveToFile);
			savePath = gd.getNextString();
			// IJ.log("savePath=" + savePath);
			savingFormat = gd.getNextChoice();
			// IJ.log("savingFormat=" + savingFormat);

		} else {
			Vector<?> choiceFields = gd.getChoices();
			if (choiceFields != null && choiceFields.size() >= 3) {
				fittingMode = getChoiceFieldValue(choiceFields, 1);
				savingFormat = getChoiceFieldValue(choiceFields, 2);
			}

			// Parse checkbox fields
			Vector<?> checkboxFields = gd.getCheckboxes();
			if (checkboxFields != null && checkboxFields.size() >= 4) {
				enableFilterCalibRange = getCheckboxFieldValue(checkboxFields, 0);
				enableFilterDistance = getCheckboxFieldValue(checkboxFields, 1);
				enableFilterIntensityRatio = getCheckboxFieldValue(checkboxFields, 2);
				saveToFile = getCheckboxFieldValue(checkboxFields, 3);
			}

			// Parse string fields
			Vector<?> stringFields = gd.getStringFields();
			if (stringFields != null && stringFields.size() >= 2) {
				savePath = getStringFieldValue(stringFields, 1);
			}
			// fittingMode = gd.getNextChoice();
			IJ.log("Fitting=" + fittingMode);
//		//enableFilterCalibRange = gd.getNextBoolean();
			IJ.log("enableFilterCalibRange=" + enableFilterCalibRange);
//		//enableFilterDistance = gd.getNextBoolean();
			IJ.log("enableFilterDistance=" + enableFilterDistance);
//		//enableFilterIntensityRatio = gd.getNextBoolean();
			IJ.log("enableFilterIntensityRatio=" + enableFilterIntensityRatio);
//		//saveToFile = gd.getNextBoolean();
			IJ.log("saveToFile=" + saveToFile);
//		//savePath = gd.getNextString();
			IJ.log("savePath=" + savePath);
//		//savingFormat = gd.getNextChoice();
			IJ.log("savingFormat=" + savingFormat);

		}

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

		StringBuilder command = new StringBuilder();
		command.append("run(\"DHPSFU\", ");
		command.append("\"Calib_input=").append(input).append(" ");
		command.append("Data_input=").append(input2).append(" ");
		command.append("Pixel_size=").append(pxSize).append(" ");
		command.append("Calibration_step=").append(calibStep).append(" ");
		command.append("Precision_cutoff=").append(precisionCutoff).append(" ");
		command.append("Fitting_mode=").append(fittingMode).append(" ");
		command.append("Range_to_fit_from=").append(rangeToFit[0]).append(" ");
		command.append("Range_to_fit_to=").append(rangeToFit[1]).append(" ");
		command.append("Filter_calib_range=").append(enableFilterCalibRange).append(" ");
		command.append("Filter_dist=").append(enableFilterDistance).append(" ");
		command.append("Dist_from=").append(initialDistanceFilter[0]).append(" ");
		command.append("Dist_to=").append(initialDistanceFilter[1]).append(" ");
		command.append("Dist_deviation=").append(distanceDev).append(" ");
		command.append("Filter_intensity_ratio=").append(enableFilterIntensityRatio).append(" ");
		command.append("Intensity_deviation=").append(intensityDev).append(" ");
		command.append("Save_to_file=").append(saveToFile).append(" ");
		command.append("Save_directory=").append(savePath).append(" ");
		command.append("Save_format=").append(savingFormat).append("\");");

		if (Recorder.record) {
			Recorder.recordString(command.toString());
		}

		return true;
	} // End of shoeDialog

	private double getNumericFieldValue(Vector<?> numericFields, int index) {
		TextField field = (TextField) numericFields.get(index);
		try {
			return Double.parseDouble(field.getText());
		} catch (NumberFormatException e) {
			IJ.log("Error parsing numeric value: " + e.getMessage());
			return Double.NaN;
		}
	}

	private static String getChoiceFieldValue(Vector<?> fields, int index) {
		return ((java.awt.Choice) fields.get(index)).getSelectedItem();
	}

	private static boolean getCheckboxFieldValue(Vector<?> fields, int index) {
		return ((java.awt.Checkbox) fields.get(index)).getState();
	}

	private static String getStringFieldValue(Vector<?> fields, int index) {
		return ((java.awt.TextField) fields.get(index)).getText();
	}

	private void parseArguments(String arg) {
		String[] params = arg.split(" ");
		for (String param : params) {
			String[] keyVal = param.split("=");
			if (keyVal.length == 2) {
				switch (keyVal[0]) {
				case "Calib_input":
					input = keyVal[1];
					break;
				case "Data_input":
					input2 = keyVal[1];
					break;
				case "Pixel_size":
					pxSize = Double.parseDouble(keyVal[1]);
					break;
				case "Calibration_step":
					calibStep = Double.parseDouble(keyVal[1]);
					break;
				case "Precision_cutoff":
					precisionCutoff = Double.parseDouble(keyVal[1]);
					break;
				case "Fitting_mode":
					fittingMode = keyVal[1];
					break;
				case "Range_to_fit_from":
					rangeToFit[0] = Integer.parseInt(keyVal[1]);
					break;
				case "Range_to_fit_to=":
					rangeToFit[1] = Integer.parseInt(keyVal[1]);
					break;
				case "Filter_calib_range":
					enableFilterCalibRange = Boolean.parseBoolean(keyVal[1]);
					break;
				case "Filter_dist":
					enableFilterDistance = Boolean.parseBoolean(keyVal[1]);
					break;
				case "Dist_from":
					initialDistanceFilter[0] = Integer.parseInt(keyVal[1]);
					break;
				case "Dist_to":
					initialDistanceFilter[1] = Integer.parseInt(keyVal[1]);
					break;
				case "Dist_deviation":
					distanceDev = Double.parseDouble(keyVal[1]);
					break;
				case "Filter_intensity_ratio":
					enableFilterIntensityRatio = Boolean.parseBoolean(keyVal[1]);
					break;
				case "Intensity_deviation":
					intensityDev = Double.parseDouble(keyVal[1]);
					break;
				case "Save_to_file":
					saveToFile = Boolean.parseBoolean(keyVal[1]);
					break;
				case "Save_directory":
					savePath = keyVal[1];
					break;
				case "Save_format":
					savingFormat = keyVal[1];
					break;

				}
			}
			// IJ.log("Set " + keyVal[0] + " to " + keyVal[1]);
		}
	}

	private static MemoryPeakResults getResults(String fileName, String filePath) throws CsvValidationException {
		// Use a utility class and provide the name for the dataset
		return CalibData.createRandomResults(fileName, filePath);
	} // End of getResults

	// Remove bad frames which do not have 2 and only 2 localisations.
	private List<Integer> removeBadFrame(int[] frame) {
		int frameNum = Arrays.stream(frame).max().getAsInt();
		System.out.println("No. of calibration frames = " + frameNum);
		List<Integer> frameList = Arrays.stream(frame).boxed().collect(Collectors.toList());
		List<Integer> badFrames = IntStream.rangeClosed(1, frameNum)
				.filter(i -> Collections.frequency(frameList, i) == 1 || Collections.frequency(frameList, i) > 2)
				.boxed().collect(Collectors.toList());
		System.out.println("The bad frames are = " + badFrames);
		return badFrames;
	} // End of removeBadFrame

	// Remove values from data arrays corresponding to bad frames
	private float[][] removeValuesForBadFrames(List<Integer> badFrames, int[] frame, float[] x, float[] y,
			float[] intensity, double[] precisions) {
		// Convert badFrames to a Set for efficient lookup
		Set<Integer> badFramesSet = new HashSet<>(badFrames);
		int numFrames = x.length;
		int numGoodFrames = numFrames - badFrames.size();
		float[][] goodData = new float[numGoodFrames][5];
		int goodFrameIndex = 0;
		for (int i = 0; i < numFrames; i++) {
			int currentFrame = Math.round(frame[i]);
			if (!badFramesSet.contains(currentFrame)) {
				goodData[goodFrameIndex][0] = frame[i];
				goodData[goodFrameIndex][1] = x[i];
				goodData[goodFrameIndex][2] = y[i];
				goodData[goodFrameIndex][3] = intensity[i];
				goodData[goodFrameIndex][4] = (float) precisions[i];
				goodFrameIndex++;
			}
		}
		return goodData;
	} // End of removeValuesForBadFrames

	// Calculate the x, y, distance, intensity, intensity ration and angle of the
	// given dataset.
	private double[][] DHPSFUCalculation(float[][] goodData) {
		if (goodData == null || goodData.length == 0) {
			throw new IllegalArgumentException("goodData must not be null or empty");
		}
		List<Integer> frameList = Arrays.asList(new Integer[goodData.length]);
		for (int i = 0; i < goodData.length; i++) {
			frameList.set(i, (int) goodData[i][0]);
		}
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
		List<Integer> trueFrameList = frameList.stream().distinct().collect(Collectors.toList());
		double[][] calculated = new double[trueFrameList.size()][6];
		for (int i = 0; i < trueFrameList.size(); i++) {
			int oneFrame = trueFrameList.get(i);
			List<Integer> indexFrame = IntStream.range(0, frameList.size()).filter(j -> frameList.get(j) == oneFrame)
					.boxed().collect(Collectors.toList());
			int fm1 = indexFrame.get(0);
			int fm2 = indexFrame.get(indexFrame.size() > 1 ? 1 : 0);
			double x1 = xList.get(fm1);
			double x2 = xList.get(fm2);
			double y1 = yList.get(fm1);
			double y2 = yList.get(fm2);
			double avgX = (x1 + x2) / 2;
			double avgY = (y1 + y2) / 2;
			double distXY = Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
			double avgIntensity = (intensityList.get(fm1) + intensityList.get(fm2)) / 2;
			double ratioIntensity = Math.max(intensityList.get(fm1), intensityList.get(fm2))
					/ Math.min(intensityList.get(fm1), intensityList.get(fm2));
			double angle = Math.atan2(y2 - y1, x2 - x1);
			if (angle < 0) {
				angle = angle + Math.PI;
			}
			calculated[i] = new double[] { oneFrame, avgX, avgY, distXY, avgIntensity, ratioIntensity, angle };
		}
		return calculated;
	} // End of DHPSFUCalculation

	// Filter the calibration data
	private double[][] filterData(double[][] calculated, GeneralParas generalParas) {
		if (calculated == null || calculated.length == 0) {
			throw new IllegalArgumentException("calculated must not be null or empty");
		}
		int distanceLowerBound = generalParas.getInitialDistanceFilter()[0];
		int distanceUpperBound = generalParas.getInitialDistanceFilter()[1];
		String fittingMode = generalParas.getFittingMode();
		int rangeLowerBound = generalParas.getRangeToFit()[0];
		int rangeUpperBound = generalParas.getRangeToFit()[1];
		double calibStep = generalParas.getCalibStep();
		int avgDistanceIndex = 3;
		int frameIndex = 0;
		int angleIndex = 6;
		int numRows = 0;
		for (double[] row : calculated) {
			if (row[avgDistanceIndex] >= distanceLowerBound && row[avgDistanceIndex] <= distanceUpperBound) {
				numRows++;
			}
		}
		double[][] filteredData = new double[numRows][calculated[0].length];
		int rowIndex = 0;
		// Filter the data based on distance and mode
		for (double[] row : calculated) {
			if (row[avgDistanceIndex] >= distanceLowerBound && row[avgDistanceIndex] <= distanceUpperBound) {
				if (fittingMode.equals("Frame") && row[frameIndex] >= rangeLowerBound
						&& row[frameIndex] <= rangeUpperBound) {
					filteredData[rowIndex] = row;
					rowIndex++;
				} else if (fittingMode.equals("Angle") && row[angleIndex] >= (rangeLowerBound * Math.PI / 180)
						&& row[angleIndex] <= (rangeLowerBound * Math.PI / 180)) {
					filteredData[rowIndex] = row;
					rowIndex++;
				} else if (fittingMode.equals("Z") && row[frameIndex] * calibStep >= rangeLowerBound
						&& row[frameIndex] * calibStep <= rangeUpperBound) {
					filteredData[rowIndex] = row;
					rowIndex++;
				}
			}
		}
		filteredData = Arrays.copyOf(filteredData, rowIndex);
		return filteredData;
	} // End of filterData

	// Polynomial fit of the calibData
	private FittingParas polyFitting(double[][] filteredData, GeneralParas generalParas) {
		if (filteredData == null || filteredData.length == 0) {
			throw new IllegalArgumentException("filteredData must not be null or empty");
		}
		for (double[] row : filteredData) {
			if (row.length < 7) { // Assuming there are 7 columns in the dataset
				throw new IllegalArgumentException("Each row of filteredData must have at least 7 columns");
			}
		}
		double calibStep = generalParas.getCalibStep();
		int angleIndex = 6;
		int frameIndex = 0;
		int avgXIndex = 1;
		int avgYIndex = 2;
		int avgDistanceIndex = 3;
		int ratioIndex = 5;
		// Fitting
		double[] dx = polyFit(filteredData, frameIndex, avgXIndex, calibStep, 1, 5);
		double[] dy = polyFit(filteredData, frameIndex, avgYIndex, calibStep, 1, 5);
		double[] dz = polyFit(filteredData, angleIndex, frameIndex, 1, calibStep, 5);
		double[] dd = polyFit(filteredData, angleIndex, avgDistanceIndex, 1, 1, 8);
		double[] dr = polyFit(filteredData, angleIndex, ratioIndex, 1, 1, 8);
		double angleMin = Arrays.stream(filteredData).mapToDouble(row -> row[angleIndex]).min().orElse(0.0);
		double angleMax = Arrays.stream(filteredData).mapToDouble(row -> row[angleIndex]).max().orElse(0.0);
		double[] angleRange = { angleMin, angleMax };
		return new FittingParas(dx, dy, dz, dd, dr, angleRange);
	} // End of polyFitting

	// PolynomialCurveFitter
	private double[] polyFit(double[][] data, int xIndex, int yIndex, double calibStep, double calibStep2, int degree) {
		WeightedObservedPoints obs = new WeightedObservedPoints();
		for (double[] row : data) {
			double x = row[xIndex] * calibStep;
			double y = row[yIndex] * calibStep2;
			obs.add(1.0, x, y);
		}
		return PolynomialCurveFitter.create(degree).fit(obs.toList());
	} // End of polyFit

	// Filter out peakfit data above the precision cutoff
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
		// System.out.println(frame);
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
	} // End of filterDataByPrecision

	private Set<Integer> getUniqueFrames(double[][] DataFilteredPrecision) {
		Set<Integer> uniqueFrames = new LinkedHashSet<>();
		for (double[] row : DataFilteredPrecision) {
			uniqueFrames.add((int) row[0]);
		}
		System.out.println("Unique Frames: " + uniqueFrames.size());
		for (int frameValue : uniqueFrames) {
			System.out.print(frameValue + " ");
		}
		System.out.println();
		return uniqueFrames;
	} // End of getUniqueFrames

	private List<double[]> filterDataByFrame(double[][] data, int frame) {
		List<double[]> filteredData = new ArrayList<>();
		for (double[] row : data) {
			if ((int) row[0] == frame) {
				filteredData.add(row);
			}
		}
		return filteredData;
	} // End of filterDataByFrame

	// Calculate the Euclidean Distance with given XY coordinates
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
	} // End of calculateDistances

	// Process the peakfit data using general parameters
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
					if (distances[i][j] > generalParas.getInitialDistanceFilter()[1]
							|| distances[i][j] < generalParas.getInitialDistanceFilter()[0]) {
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
						// System.out.println(frame);
						frames.add((double) frame);
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
		processedResult.add(frames);
		processedResult.add(dists);
		processedResult.add(x);
		processedResult.add(y);
		processedResult.add(angle);
		processedResult.add(ratio);
		processedResult.add(intensity);
		return processedResult;
	} // End of processData

	// Calculate the xyz coordinates from the polynomial fit
	private List<List<Double>> calculateCoordinates(List<List<Double>> processedResult, FittingParas fittingParas,
			GeneralParas generalParas) {
		double zMin = polyval(fittingParas.getDz(), fittingParas.getAngleRange()[0]);
		// double zMax = polyval(fittingParas.getDz(), fittingParas.getAngleRange()[1]);
		List<Double> zN = processedResult.get(4).stream().map(angle -> polyval(fittingParas.getDz(), angle))
				.collect(Collectors.toList());
		List<Double> xN = new ArrayList<>();
		List<Double> yN = new ArrayList<>();
		for (int i = 0; i < processedResult.get(2).size(); i++) {
			xN.add((processedResult.get(2).get(i)
					- (polyval(fittingParas.getDx(), zN.get(i)) - polyval(fittingParas.getDx(), zMin)))
					* generalParas.getPxSize());
			yN.add((processedResult.get(3).get(i)
					- (polyval(fittingParas.getDy(), zN.get(i)) - polyval(fittingParas.getDy(), zMin)))
					* generalParas.getPxSize());
		}
		List<List<Double>> xyzN = new ArrayList<>();
		xyzN.add(xN);
		xyzN.add(yN);
		xyzN.add(zN);

		// plot
		// Generate a series of z values within the desired range
		final int points = 200; // Number of points to generate for the plot
		double angleMin = fittingParas.getAngleRange()[0];
		double angleMax = fittingParas.getAngleRange()[1];

		double[] coefficientsZ = fittingParas.getDz();
		double[] angleArray = IntStream.rangeClosed(0, points)
				.mapToDouble(i -> angleMin + i * (angleMax - angleMin) / points).toArray();
		double[] zArray = new double[angleArray.length];

		for (int i = 0; i < angleArray.length; i++) {
			zArray[i] = polyval(coefficientsZ, angleArray[i]);
		}

		double zMax = Arrays.stream(zArray).max().getAsDouble();

		Plot plot = new Plot("Calibration curve", "Angle (Radian)", "Z (nm)");
		plot.setLimits(angleMin, angleMax, zMin, zMax);
		plot.addPoints(angleArray, zArray, Plot.LINE);

		plot.show();

		return xyzN;
	} // End of calculateCoordinates

	private double polyval(double[] coefficients, double x) {
		double result = 0;
		for (int i = 0; i < coefficients.length; i++) {
			result += coefficients[i] * Math.pow(x, i);
		}
		return result;
	} // End of polyval

	// Filtering the peakfit data with different filters and parameters
	private List<List<Double>> filterPeakfitData(List<List<Double>> processedResult, List<List<Double>> xyzN,
			FilterParas filterParas, FittingParas fittingParas) {
		int[] marker = new int[processedResult.get(0).size()];
		Arrays.fill(marker, 1);
		if (filterParas.isEnableFilters()) {
			if (filterParas.isEnableFilterCalibRange()) {
				List<Integer> lowAngle = IntStream.range(0, processedResult.get(0).size())
						.filter(i -> processedResult.get(4).get(i) < fittingParas.getAngleRange()[0]).boxed()
						.collect(Collectors.toList());
				List<Integer> highAngle = IntStream.range(0, processedResult.get(0).size())
						.filter(i -> processedResult.get(4).get(i) > fittingParas.getAngleRange()[1]).boxed()
						.collect(Collectors.toList());
				lowAngle.forEach(i -> marker[i] = -1);
				highAngle.forEach(i -> marker[i] = -1);
			}
			if (filterParas.isEnableFilterDistance()) {
				List<Integer> largeDistance = IntStream.range(0, processedResult.get(0).size())
						.filter(i -> Math
								.abs(processedResult.get(1).get(i)
										- polyval(fittingParas.getDd(), processedResult.get(4).get(i)))
								/ processedResult.get(1).get(i) > filterParas.getDistanceDev())
						.boxed().collect(Collectors.toList());
				largeDistance.forEach(i -> marker[i] = -1);
			}
			if (filterParas.isEnableFilterIntensityRatio()) {
				List<Integer> badRatio = IntStream.range(0, processedResult.get(0).size())
						.filter(i -> Math.abs(processedResult.get(5).get(i)
								- polyval(fittingParas.getDr(), processedResult.get(4).get(i))) > filterParas
										.getIntensityDev())
						.boxed().collect(Collectors.toList());
				List<Integer> badRatio2 = IntStream.range(0, processedResult.get(0).size())
						.filter(i -> processedResult.get(5).get(i) > 4).boxed().collect(Collectors.toList());
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
				.filter(i -> marker[i] >= 0).mapToObj(i -> {
					List<Double> row = new ArrayList<>();
					for (List<Double> column : processedResultWithZN) {
						row.add(column.get(i));
					}
					return row;
				}).collect(Collectors.toList());
		return filteredPeakResult;
	} // End of filterPeakfitData

	/**
	 * Get the save path of the .3d result file
	 */
//    private String getSavePath(String dataPath) {
//    	 String targetSubstring = ".tif.results.xls";
//         int index = dataPath.lastIndexOf(targetSubstring);
//
//         if (index == -1) {
//             return dataPath + ".3d";
//         } else {
//             return dataPath.substring(0, index) + ".3d";
//         }
//     }   // End of getSavePath

	// Save the final filtered result to .3D file
	private void saveTo3D(List<List<Double>> filteredPeakResult, String fileName, String savingFormat) {
		String name = fileName + "_DH";
		Path outputPath;
		if (savingFormat == ".3d") {
			outputPath = Paths.get(savePath, name + savingFormat);
			try (BufferedWriter writer = Files.newBufferedWriter(outputPath)) {
				for (List<Double> row : filteredPeakResult) {
					String csvRow = row.stream().map(Object::toString).collect(Collectors.joining("\t"));
					writer.write(csvRow);
					writer.newLine();
				}
			} catch (IOException e) {
				System.err.println("Error writing to file: " + name);
				e.printStackTrace();
			}
		} else {
			outputPath = Paths.get(savePath, name + savingFormat);
			try (BufferedWriter writer = Files.newBufferedWriter(outputPath)) {
				for (List<Double> row : filteredPeakResult) {
					String csvRow = row.stream().map(Object::toString).collect(Collectors.joining(","));
					writer.write(csvRow);
					writer.newLine();
				}
			} catch (IOException e) {
				System.err.println("Error writing to file: " + name);
				e.printStackTrace();
			}
		}
	} // End of saveTo3D

	// View the result in a table in imageJ
	private void view3DResult(List<List<Double>> filteredPeakResult) {
		double[][] doubleFilteredPeakResult = toDouble(filteredPeakResult);
		double[] frame = doubleFilteredPeakResult[4];
		double[] x = doubleFilteredPeakResult[0];
		double[] y = doubleFilteredPeakResult[1];
		double[] z = doubleFilteredPeakResult[2];
		double[] intensity = doubleFilteredPeakResult[3];
		ResultsTable t = new ResultsTable();
		t.setValues("X (nm)", x);
		t.setValues("Y (nm)", y);
		t.setValues("Z (nm)", z);
		t.setValues("Intensity (photon)", intensity);
		t.setValues("Frame", frame);
		t.show("DHPSFU results");
	} // End of view3DResult

	private MemoryPeakResults saveToMemory(String input, List<List<Double>> filteredPeakResult) {
		String name = input + "_DH";
		double[][] doubleFilteredPeakResult = toDouble(filteredPeakResult);
		double[] frame = doubleFilteredPeakResult[4];
		double[] x = doubleFilteredPeakResult[0];
		double[] y = doubleFilteredPeakResult[1];
		double[] z = doubleFilteredPeakResult[2];
		double[] intensity = doubleFilteredPeakResult[3];
		MemoryPeakResults finalResult = new MemoryPeakResults();
		for (int i = 0; i < frame.length; i++) {
			float[] parameters = new float[7];
			parameters[PeakResultDHPSFU.BACKGROUND] = (float) frame[i];
			parameters[PeakResultDHPSFU.X] = (float) x[i];
			parameters[PeakResultDHPSFU.Y] = (float) y[i];
			parameters[PeakResultDHPSFU.INTENSITY] = (float) intensity[i];
			// Set noise assuming photons have a Poisson distribution
			// float noise = (float) Math.sqrt(parameters[PeakResultDHPSFU.INTENSITY]);
			PeakResult r = new PeakResult((int) frame[i], parameters[2], parameters[3], parameters[1]);
			r.setZPosition((float) z[i]);
			finalResult.add(r);
		}
		finalResult.end();
		finalResult.sort();
		finalResult.setName(name);
		System.out.println();
		return finalResult;
	} // End of view3DResult

	// Main function for DHPSFU
	private void DH_calibration() {
		long startTime = System.currentTimeMillis();
		// Processing the calibration data
		MemoryPeakResults results = ResultsManager.loadInputResults(name1, false, null, null);
		// System.out.println(name1);
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
		float[] y = s.y;
		float[] intensity = s.intensity;
		List<Integer> badFrames = removeBadFrame(frame);
		float[][] goodData = removeValuesForBadFrames(badFrames, frame, x, y, intensity, precisions);
		double[][] calculated = DHPSFUCalculation(goodData);
		double[][] filteredData = filterData(calculated, generalParas);
		FittingParas fittingParas = polyFitting(filteredData, generalParas);

		// Processing the peakfit data
		MemoryPeakResults PeakfitData = ResultsManager.loadInputResults(name2, false, null, null);
		if (MemoryPeakResults.isEmpty(PeakfitData)) {
			IJ.error(TITLE, "No peakfit results could be loaded");
			return;
		}
		double[][] DataFilteredPrecision = filterDataByPrecision(PeakfitData, precisionCutoff);
		List<List<Double>> processedResult = processData(DataFilteredPrecision, generalParas);
		List<List<Double>> xyzN = calculateCoordinates(processedResult, fittingParas, generalParas);
		// plotPolynomial(xyzN);
		List<List<Double>> filteredPeakResult = filterPeakfitData(processedResult, xyzN, filterParas, fittingParas);

		// Save files
		if (saveToFile == true) {
			saveTo3D(filteredPeakResult, input2, savingFormat);
		}

		// View localisation
		view3DResult(filteredPeakResult);
		MemoryPeakResults finalResult = saveToMemory(input2, filteredPeakResult);
		MemoryPeakResults.addResults(finalResult);
		CalibrationWriter cw = finalResult.getCalibrationWriterSafe();
		cw.setIntensityUnit(IntensityUnit.PHOTON);
		cw.setDistanceUnit(DistanceUnit.NM);
		cw.setTimeUnit(TimeUnit.FRAME);
		cw.setExposureTime(50);
		cw.setNmPerPixel(pxSize);
		cw.setCountPerPhoton(45);
		cw.getBuilder().getCameraCalibrationBuilder().setCameraType(CameraType.EMCCD).setBias(100)
				.setQuantumEfficiency(0.95).setReadNoise(1.6);
		finalResult.setCalibration(cw.getCalibration());
		IJ.log("No. of 3D localisations: " + filteredPeakResult.size());
		// System.out.println("Number of localisation left: " +
		// filteredPeakResult.size());
		long endTime = System.currentTimeMillis();
		long duration = endTime - startTime;
		double seconds = (double) duration / 1000.0;
		IJ.log("DHPSFU runtime: " + seconds + " seconds");
	} // End of DH_calibration

	// Convert the List<List<Double>> object into double [][]
	private double[][] toDouble(List<List<Double>> list) {
		int rows = list.size();
		int cols = list.get(0).size();
		return IntStream.range(0, cols)
				.mapToObj(col -> IntStream.range(0, rows).mapToDouble(row -> list.get(row).get(col)).toArray())
				.toArray(double[][]::new);
	} // End of toDouble

	/**
	 * Main method for debugging.
	 *
	 * For debugging, it is convenient to have a method that starts ImageJ and calls
	 * the plugin, e.g. after setting breakpoints.
	 *
	 * @param args unused
	 * @throws URISyntaxException     if the URL cannot be converted to a URI
	 * @throws CsvValidationException
	 */
	public static void main(String[] args) throws URISyntaxException, CsvValidationException {
		// Set the base directory for plugins
		// see: https://stackoverflow.com/a/7060464/1207769
		Class<DHPSFU> clazz = DHPSFU.class;
		java.net.URL url = clazz.getProtectionDomain().getCodeSource().getLocation();
		File file = new File(url.toURI());
		// Note: This returns the base path. ImageJ will find plugins in here that have
		// an
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
			ij.Menus.installPlugin(clazz.getName(), ij.Menus.PLUGINS_MENU, clazz.getSimpleName().replace('_', ' '), "",
					IJ.getInstance());
		}

		// Initialise for testing, e.g. create some random datasets
		MemoryPeakResults.addResults(getResults(name1, calibPath));
		MemoryPeakResults.addResults(getResults(name2, dataPath));

		// Run the plugin
		IJ.runPlugIn(clazz.getName(), "");
	} // End of main
} // End of class
