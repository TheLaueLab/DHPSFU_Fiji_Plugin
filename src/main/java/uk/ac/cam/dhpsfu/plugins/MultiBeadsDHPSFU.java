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

import ij.IJ;
import ij.ImageJ;
import ij.Macro;
import ij.gui.Plot;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import ij.plugin.frame.Recorder;

import java.io.File;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import uk.ac.sussex.gdsc.core.ij.ImageJUtils;
import uk.ac.sussex.gdsc.core.ij.gui.ExtendedGenericDialog;
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
import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;

import com.opencsv.exceptions.CsvValidationException;

import java.awt.Button;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class MultiBeadsDHPSFU implements PlugIn {

	private static final String TITLE = "Multi Beads DHPSFU"; // Plugin title
	private static String input2 = "";
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
													// False, polynomial fit is extrapolated beyond the range.
	private boolean enableFilterDistance = true; // remove dots with unexpected distances
	private double distanceDev = 0.2; // relative deviation of the distance between dots, compared to calibration
	private boolean enableFilterIntensityRatio = true; // filter based on the ratio of intensities between the dots
	private double intensityDev = 1; // relative deviation of the intensity difference between dots, compared to
										// calibration
	FilterParas filterParas = new FilterParas(enableFilters, enableFilterCalibRange, enableFilterDistance, distanceDev,
			enableFilterIntensityRatio, intensityDev);

	private static String dataPath = "C:/Users/yw525/Documents/test_data_may2024/test/Peakfit/slice0.tif.trim.results.xls"; // Data
																															// //
																															// path
	private static String savePath = "C:/Users/yw525/Documents/test_data_may2024/test_Analysis/Peakfit/DHPSFU";
	private static List<String> dataNames;
	private String savingFormat = ".3d";
	private boolean saveToFile = true;

	@Override
	public void run(String arg) {
		String macroOptions = Macro.getOptions();
		if (showDialog(macroOptions)) {
			ImageJUtils.log("Loaded Calibration Files: " + dataNames);
			DH_calibration();

		}
	}

	private boolean showDialog(String arg) {
		if (arg != null) {
			parseArguments(arg);
		}
		ExtendedGenericDialog gd = new ExtendedGenericDialog(TITLE);
		gd.addMessage("Calibration file format: Only support peakfit result from GDSC SMLM.");
		Panel panel = new Panel();
		panel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
		Label message = new Label("Select Multi Beads Calibration files from:");
		panel.add(message);
		Button button = new Button("Calib files");
		button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						dataNames = LoadMultiBeadsCalib();
						ImageJUtils.log("Loaded calibration files: " + dataNames.toString());
					}
				}).start();
			}
		});
		panel.add(button);
		gd.addPanel(panel);
		gd.addMessage("Data file: ");
		// Add input2 if running manually
		if (arg == null || arg.length() == 0) { // Assuming no arguments means manual mode
			ResultsManager.addInput(gd, input2, InputSource.MEMORY);
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
		gd.addCheckbox("Save to file", saveToFile);
		gd.addDirectoryField("Save_directory", "--Please_Select--");
		String[] formats = { ".3d", ".csv" };
		gd.addChoice("Saving_format", formats, formats[0]);
		String html = "<html>" + "<h2>Instruction about DHPSFU-MultiBeads Plugin</h2>"
		// +"<font size=+1>"
				+ "Descriptions: <br>"
				+ " - On top of the DHPSFU plugin, this plugin allows you to correct for the lateral spacial variation of the DH across the FOV by importing multiple calibration files from different regions of the FOV.. <br>"
				+ "- The peak xy coordinates in imported calibration files must correspond to their actual position within the FOV, and the FOV must be the same as in the data. <br>"
				+ "<br>" + "Parameters:  <br>" + "- Pixel size (nm): Camera pixel size in nm. <br>"
				+ "- Calibration step (nm): The distance in z between consecutive frames of the calibration series, in nm.    <br>"
				+ "- Precision cutoff (nm): Prior to analysis, exclude 2D peaks with precision greater than this threshold.   <br>"
				+ "- Range units: Units in which the “Range to fit” is specified. Can be 'Frame', 'Angle', or 'Z'. Default is 'Frame'.    <br>"
				+ "- Range to fit (from)/(to): Prior to model fitting, crop the calibration sequence to the selected range, in terms of the number of frames ('Frame' mode); DH angle in degrees ('Angle' mode); or z-range in nm ('Z' mode). <br>"
				+ "<br>" + "Filtering options: <br>"
				+ "- Enable filter calibration range: Remove localisations out of the angular range of calibration; if False, polynomial fit is extrapolated beyond the range.  <br>"
				+ "- Enable filter distance: Remove DH localisations with an unexpected distance between the lobes.   <br>"
				+ "- Initial distance filter (from)/(to): Minimum and maximum distance between a pair of dots for this pair to be considered a DH localisation (in pixels).<br>"
				+ "** Distance deviation: Allowed relative deviation of the distance between dots, compared to calibration.<br>"
				+ "- Enable filter intensity ratio: Remove DH localisations with an unexpected ratio of intensities between the lobes.  <br>"
				+ "** Intensity deviation: Allowed relative deviation of the intensity between dots, compared to calibration.  <br>"
				+ "<br>" + "File output:  <br>"
				+ "- Save to file: Save the analysed data to user-speficied directory.  <br>"
				+ "- Saving format: .3d (tab-separated, can be visualised directly in ViSP) or .csv (comma-separated). Data format: “x y z Intensity Frame”.  <br>"
				+ "<br>" + "</font>";
		gd.addHelp(html);
		gd.showDialog();
		if (gd.wasCanceled()) {
			return false;
		}

		if (arg == null || arg.length() == 0) {
			input2 = ResultsManager.getInputSource(gd);
			IJ.log("Data file selected: " + input2);
		}
		ResultsManager.loadInputResults(input2, true, DistanceUnit.PIXEL, IntensityUnit.PHOTON);
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
			enableFilterCalibRange = gd.getNextBoolean();
			enableFilterDistance = gd.getNextBoolean();

		} else {
			Vector<?> choiceFields = gd.getChoices();
			if (choiceFields != null && choiceFields.size() >= 2) {
				fittingMode = getChoiceFieldValue(choiceFields, 0);
				savingFormat = getChoiceFieldValue(choiceFields, 1);
			}
			Vector<?> checkboxFields = gd.getCheckboxes();
			if (checkboxFields != null && checkboxFields.size() >= 4) {
				enableFilterCalibRange = getCheckboxFieldValue(checkboxFields, 0);
				enableFilterDistance = getCheckboxFieldValue(checkboxFields, 1);
				enableFilterIntensityRatio = getCheckboxFieldValue(checkboxFields, 2);
				saveToFile = getCheckboxFieldValue(checkboxFields, 3);
			}
			Vector<?> stringFields = gd.getStringFields();
			if (stringFields != null && stringFields.size() >= 2) {
				savePath = getStringFieldValue(stringFields, 1);
			}

		}
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
		command.append("run(\"DHPSFU-MultiBeads\", ");
		command.append("\"Calib_input=").append(joinWithoutSpaces(dataNames)).append(" ");
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
					List<String> stringList = Arrays.asList(keyVal[1].split(","));
					dataNames = stringList;
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

	private List<String> LoadMultiBeadsCalib() {
		ResultManager resultManager = new ResultManager();
		List<String> dataNames = resultManager.batchLoad();
		return dataNames;
	}

	public static String joinWithoutSpaces(List<String> list) {
		if (list == null || list.isEmpty()) {
			return "";
		}
		return list.stream().collect(Collectors.joining(","));
	}

	private List<Integer> removeBadFrame(int[] frame) {
		int frameNum = Arrays.stream(frame).max().getAsInt();
		// System.out.println("No. of calibration frames = " + frameNum);
		List<Integer> frameList = Arrays.stream(frame).boxed().collect(Collectors.toList());
		List<Integer> badFrames = IntStream.rangeClosed(1, frameNum)
				.filter(i -> Collections.frequency(frameList, i) == 1 || Collections.frequency(frameList, i) > 2)
				.boxed().collect(Collectors.toList());
		System.out.println("The bad frames are = " + badFrames);
		return badFrames;
	} // End of removeBadFrame

	private float[][] removeValuesForBadFrames(List<Integer> badFrames, int[] frame, float[] x, float[] y,
			float[] intensity, int[] beadID, double[] precisions) {
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
				goodData[goodFrameIndex][4] = (float) precisions[i];
				goodData[goodFrameIndex][5] = beadID[i];
				goodFrameIndex++;
			}
		}
		return goodData; // = Calib_datalist in python
	} // End of removeValuesForBadFrames

	private double[][] DHPSFUCalculation(float[][] goodData) {
		List<Integer> frameList = Arrays.asList(new Integer[goodData.length]);
		for (int i = 0; i < goodData.length; i++) {
			frameList.set(i, (int) goodData[i][0]);
		}
		// System.out.println("Number of unique frames: " + frameList.size());
		List<Double> xList = Arrays.asList(new Double[goodData.length]);
		for (int i = 0; i < goodData.length; i++) {
			xList.set(i, (double) goodData[i][1]);
		}
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
			double beadID = beadIDList.get(fm2);
			if (angle < 0) {
				angle = angle + Math.PI;
			}
			calculated[i] = new double[] { oneFrame, avgX, avgY, distXY, avgIntensity, ratioIntensity, angle, beadID };
		} // = profile in python
		return calculated;
	} // End of DHPSFUCalculation

	private double[][] dataWithAngleDiff(double[][] calculated) {
		int frameIndex = 0;
		int angleIndex = 6;

		List<double[]> dataWithAngleDiff = new ArrayList<>();
		Map<Integer, List<double[]>> frameMap = Arrays.stream(calculated)
				.collect(Collectors.groupingBy(row -> (int) row[frameIndex]));

		double maxIntraangle = 0.5;
		List<Integer> badFrames = new ArrayList<>();

		for (Map.Entry<Integer, List<double[]>> entry : frameMap.entrySet()) {
			int frame = entry.getKey();
			List<double[]> frameRows = entry.getValue();
			double meanAngle = frameRows.stream().mapToDouble(row -> row[angleIndex]).average().orElse(0.0);
			double[] angleDiffs = frameRows.stream().mapToDouble(row -> row[angleIndex] - meanAngle).toArray();

			if (Arrays.stream(angleDiffs).map(Math::abs).max().orElse(0.0) > maxIntraangle) {
				badFrames.add(frame);
			} else {
				for (int i = 0; i < frameRows.size(); i++) {
					double[] newRow = Arrays.copyOf(frameRows.get(i), frameRows.get(i).length + 1);
					newRow[newRow.length - 1] = angleDiffs[i];
					dataWithAngleDiff.add(newRow);
				}
			}
		}

		// Convert to array and filter out bad frames
		double[][] profile = dataWithAngleDiff.stream().filter(row -> !badFrames.contains((int) row[frameIndex]))
				.toArray(double[][]::new);
		return profile;
	}

	private double[] linearRegression(double[][] profile) {

		int avgXIndex = 1;
		int avgYIndex = 2;
		int beadIDIndex = 7;
		int angleDiffIndex = 8;

		Map<Double, List<double[]>> beadIDMap = Arrays.stream(profile)
				.collect(Collectors.groupingBy(row -> row[beadIDIndex], LinkedHashMap::new, Collectors.toList()));

		List<Double> xAvgList = new ArrayList<>();
		List<Double> yAvgList = new ArrayList<>();
		List<Double> aAvgList = new ArrayList<>();

		for (Map.Entry<Double, List<double[]>> entry : beadIDMap.entrySet()) {
			List<double[]> rows = entry.getValue();
			double xAvg = rows.stream().mapToDouble(row -> row[avgXIndex]).average().orElse(0.0);
			double yAvg = rows.stream().mapToDouble(row -> row[avgYIndex]).average().orElse(0.0);
			double aAvg = rows.stream().mapToDouble(row -> row[angleDiffIndex]).average().orElse(0.0);

			xAvgList.add(xAvg);
			yAvgList.add(yAvg);
			aAvgList.add(aAvg);
		}

		double[] xAvg = xAvgList.stream().mapToDouble(Double::doubleValue).toArray();
		double[] yAvg = yAvgList.stream().mapToDouble(Double::doubleValue).toArray();
		double[] aAvg = aAvgList.stream().mapToDouble(Double::doubleValue).toArray();

		// Perform linear regression
		double[][] predictors = new double[xAvg.length][2];
		for (int i = 0; i < xAvg.length; i++) {
			predictors[i][0] = xAvg[i];
			predictors[i][1] = yAvg[i];
		}
		// Perform plane fit
		OLSMultipleLinearRegression regression = new OLSMultipleLinearRegression();
		regression.newSampleData(aAvg, predictors);
		double[] regressionParameters = regression.estimateRegressionParameters();

		return regressionParameters;
	}

	private double[][] filterData(double[][] profile, double[] regressionParameters, GeneralParas generalParas) {
		int distanceLowerBound = generalParas.getInitialDistanceFilter()[0];
		int distanceUpperBound = generalParas.getInitialDistanceFilter()[1];
		String fittingMode = generalParas.getFittingMode();
		int rangeLowerBound = generalParas.getRangeToFit()[0];
		int rangeUpperBound = generalParas.getRangeToFit()[1];
		double calibStep = generalParas.getCalibStep();
		int avgDistanceIndex = 3;
		int frameIndex = 0;
		int angleCorrIndex = 9;
		int numRows = 0;
		int avgXIndex = 1;
		int avgYIndex = 2;
		int angleIndex = 6;

		double intercept = regressionParameters[0];
		double coefficientX = regressionParameters[1];
		double coefficientY = regressionParameters[2];

		double[][] profileFinal = new double[profile.length][profile[0].length + 1];

		double[] angleCorrected = new double[profile.length];
		for (int i = 0; i < profile.length; i++) {
			double predictedAngle = intercept + coefficientX * profile[i][avgXIndex]
					+ coefficientY * profile[i][avgYIndex];
			angleCorrected[i] = profile[i][angleIndex] - predictedAngle;
		}

		for (int i = 0; i < profile.length; i++) {
			System.arraycopy(profile[i], 0, profileFinal[i], 0, profile[i].length);
			profileFinal[i][profile[i].length] = angleCorrected[i];
		}

		for (double[] row : profileFinal) {
			if (row[avgDistanceIndex] >= distanceLowerBound && row[avgDistanceIndex] <= distanceUpperBound) {
				numRows++;
			}
		}

		double[][] filteredData = new double[numRows][profileFinal[0].length];
		int rowIndex = 0;
		for (double[] row : profileFinal) {
			if (fittingMode.equals("Frame") && row[frameIndex] >= rangeLowerBound
					&& row[frameIndex] <= rangeUpperBound) {
				filteredData[rowIndex] = row;
				rowIndex++;
			} else if (fittingMode.equals("Angle") && row[angleCorrIndex] >= (rangeLowerBound * Math.PI / 180)
					&& row[angleCorrIndex] <= (rangeLowerBound * Math.PI / 180)) {
				filteredData[rowIndex] = row;
				rowIndex++;
			} else if (fittingMode.equals("Z") && row[frameIndex] * calibStep >= rangeLowerBound
					&& row[frameIndex] * calibStep <= rangeUpperBound) {
				filteredData[rowIndex] = row;
				rowIndex++;
			}
		}
		filteredData = Arrays.copyOf(filteredData, rowIndex);

		System.out.println("Filtered:" + filteredData.length);
		return filteredData;
	} // End of filterData

	private FittingParas polyFitting(double[][] filteredData, GeneralParas generalParas) {
		if (filteredData == null || filteredData.length == 0) {
			throw new IllegalArgumentException("filteredData must not be null or empty");
		}
		for (double[] row : filteredData) {
			if (row.length < 7) {
				throw new IllegalArgumentException("Each row of filteredData must have at least 7 columns");
			}
		}
		double calibStep = generalParas.getCalibStep();
		int frameIndex = 0;
		int avgXIndex = 1;
		int avgYIndex = 2;
		int avgDistanceIndex = 3;
		int angleCorrIndex = 9;
		int ratioIndex = 5;
		int beadIDIndex = 7;

		double angleMin = Arrays.stream(filteredData).mapToDouble(row -> row[angleCorrIndex]).min().orElse(0.0);
		double angleMax = Arrays.stream(filteredData).mapToDouble(row -> row[angleCorrIndex]).max().orElse(0.0);
		double[] angleRange = { angleMin, angleMax };

		List<Double> xnormList = new ArrayList<>();
		List<Double> ynormList = new ArrayList<>();
		List<Integer> fnormList = new ArrayList<>();
		Map<Double, Integer> beadObservations = new HashMap<>();
		for (double[] row : filteredData) {
			double beadID = row[beadIDIndex];
			beadObservations.put(beadID, beadObservations.getOrDefault(beadID, 0) + 1);
		}
		int maxOb = Collections.max(beadObservations.values());

		List<Double> beadOrder = new ArrayList<>(beadObservations.keySet());
		for (double b : beadOrder) {
			List<Integer> poses = Arrays.stream(filteredData).filter(row -> row[beadIDIndex] == b)
					.sorted(Comparator.comparingDouble(row -> row[frameIndex]))
					.map(row -> Arrays.asList(filteredData).indexOf(row)).collect(Collectors.toList());
			if (poses.size() > 0.95 * maxOb) {
				double meanX = poses.stream().mapToDouble(p -> filteredData[p][avgXIndex]).average().getAsDouble();
				double meanY = poses.stream().mapToDouble(p -> filteredData[p][avgYIndex]).average().getAsDouble();
				List<Double> xnormBead = poses.stream().mapToDouble(p -> filteredData[p][avgXIndex] - meanX).boxed()
						.collect(Collectors.toList());
				List<Double> ynormBead = poses.stream().mapToDouble(p -> filteredData[p][avgYIndex] - meanY).boxed()
						.collect(Collectors.toList());
				List<Integer> fnormBead = poses.stream().map(p -> (int) filteredData[p][frameIndex])
						.collect(Collectors.toList());
				xnormList.addAll(xnormBead);
				ynormList.addAll(ynormBead);
				fnormList.addAll(fnormBead);
			}
		}

		double[] xnormArr = xnormList.stream().mapToDouble(Double::doubleValue).toArray();
		double[] ynormArr = ynormList.stream().mapToDouble(Double::doubleValue).toArray();
		int[] fnormArr = fnormList.stream().mapToInt(Integer::intValue).toArray();
		Arrays.stream(filteredData).mapToInt(row -> (int) row[frameIndex]).toArray();

		// Fitting
		double[] dx = polyFit2(IntStream.of(fnormArr).mapToDouble(f -> f * calibStep).toArray(), xnormArr, 5);
		double[] dy = polyFit2(IntStream.of(fnormArr).mapToDouble(f -> f * calibStep).toArray(), ynormArr, 5);
		double[] dz = polyFit(filteredData, angleCorrIndex, frameIndex, 1, calibStep, 5);
		double[] dd = polyFit(filteredData, angleCorrIndex, avgDistanceIndex, 1, 1, 8);
		double[] dr = polyFit(filteredData, angleCorrIndex, ratioIndex, 1, 1, 8);

		final int points = 200;
		double[] coefficientsZ = dz;
		double[] angleArray = IntStream.rangeClosed(0, points)
				.mapToDouble(i -> angleMin + i * (angleMax - angleMin) / points).toArray();
		double[] zArray = new double[angleArray.length];
		for (int i = 0; i < angleArray.length; i++) {
			zArray[i] = polyval(coefficientsZ, angleArray[i]);
		}
		double zMax = Arrays.stream(zArray).max().getAsDouble();
		double zMin = Arrays.stream(zArray).min().getAsDouble();
		Plot plot = new Plot("Calibration Curve", "Angle (Radian)", "Z (nm)");
		plot.setLimits(angleMin, angleMax, zMin, zMax);
		plot.setColor(Color.RED);
		plot.addPoints(
				IntStream.range(0, filteredData.length).mapToDouble(i -> filteredData[i][angleCorrIndex]).toArray(),
				IntStream.range(0, filteredData.length).mapToDouble(i -> filteredData[i][frameIndex] * calibStep)
						.toArray(),
				Plot.CIRCLE);
		
		plot.setColor(Color.BLACK);
		plot.setLineWidth(1);
		plot.addPoints(angleArray, zArray, Plot.LINE);

		plot.show();

		return new FittingParas(dx, dy, dz, dd, dr, angleRange);
	} // End of polyFitting

	private double[] polyFit(double[][] data, int xIndex, int yIndex, double calibStep, double calibStep2, int degree) {
		WeightedObservedPoints obs = new WeightedObservedPoints();
		for (double[] row : data) {
			double x = row[xIndex] * calibStep;
			double y = row[yIndex] * calibStep2;
			obs.add(1.0, x, y);
		}
		return PolynomialCurveFitter.create(degree).fit(obs.toList());
	} // End of polyFit

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

	private double[][] filterDataByPrecision(MemoryPeakResults PeakfitData, double precisionCutoff) {
		PrecisionResultProcedure p = new PrecisionResultProcedure(PeakfitData);
		p.getPrecision(true);
		double[] precisions = p.precisions;
		// System.out.println("Precision = " + precisions.length);
		StandardResultProcedure s = new StandardResultProcedure(PeakfitData, DistanceUnit.PIXEL, IntensityUnit.PHOTON);
		PeakfitData.getCalibration();

		s.getTxy();
		s.getI();
		int[] frame = s.frame;
		float[] x = s.x;
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

	private List<List<Double>> processData(double[][] DataFilteredPrecision, GeneralParas generalParas,
			double[] regressionParameters) {
		Set<Integer> uniqueFrames = getUniqueFrames(DataFilteredPrecision);
		List<List<Double>> processedResult = new ArrayList<>();

		List<Double> frames = new ArrayList<>();
		List<Double> dists = new ArrayList<>();
		List<Double> x = new ArrayList<>();
		List<Double> y = new ArrayList<>();
		List<Double> angle = new ArrayList<>();
		List<Double> ratio = new ArrayList<>();
		List<Double> intensity = new ArrayList<>();
		double intercept = regressionParameters[0];
		double coefficientX = regressionParameters[1];
		double coefficientY = regressionParameters[2];
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
		double[] xArray = x.stream().mapToDouble(Double::doubleValue).toArray();
		double[] yArray = y.stream().mapToDouble(Double::doubleValue).toArray();
		double[] angleArray = angle.stream().mapToDouble(Double::doubleValue).toArray();

		double[][] xyArray = new double[xArray.length][2];
		for (int i = 0; i < xArray.length; i++) {
			xyArray[i][0] = xArray[i];
			xyArray[i][1] = yArray[i];
		}

		for (int i = 0; i < angleArray.length; i++) {
			double predictedAngle = intercept + coefficientX * xyArray[i][0] + coefficientY * xyArray[i][1];
			angleArray[i] -= predictedAngle;
		}

		angle = Arrays.stream(angleArray).boxed().collect(Collectors.toList());
		processedResult.add(frames);
		processedResult.add(dists);
		processedResult.add(x);
		processedResult.add(y);
		processedResult.add(angle);
		processedResult.add(ratio);
		processedResult.add(intensity);
		System.out.println("Processed result line= " + processedResult.get(0).size());

		return processedResult;
	} // End of processData

	private List<List<Double>> calculateCoordinates(List<List<Double>> processedResult, FittingParas fittingParas,
			GeneralParas generalParas) {

		double zMin = polyval(fittingParas.getDz(), fittingParas.getAngleRange()[0]);

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

		return xyzN;
	} // End of calculateCoordinates

	private double polyval(double[] coefficients, double x) {
		double result = 0;
		for (int i = 0; i < coefficients.length; i++) {
			result += coefficients[i] * Math.pow(x, i);
		}
		return result;
	} // End of polyval

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

	private void saveTo3D(List<List<Double>> filteredPeakResult, String fileName, String savingFormat) {
		String name = fileName + "_DH";
		// Path outputPath = Paths.get(fileName);
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
			Math.sqrt(parameters[PeakResultDHPSFU.INTENSITY]);
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

	private void DH_calibration() {
		long startTime = System.currentTimeMillis();

		List<double[][]> calibDataList = new ArrayList<>();
		int index = 0;
		for (String c : dataNames) {
			MemoryPeakResults results = ResultsManager.loadInputResults(c, false, null, null);
			System.out.println(dataNames);
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
			float[] y = s.y;
			float[] intensity = s.intensity;
			int[] dataIndexes = new int[x.length];
			for (int i = 0; i < dataIndexes.length; i++) {
				dataIndexes[i] = index + 1;
			}
			index++;
			List<Integer> badFrames = removeBadFrame(frame);
			float[][] goodData = removeValuesForBadFrames(badFrames, frame, x, y, intensity, dataIndexes, precisions);
			double[][] calculated = DHPSFUCalculation(goodData);
			calibDataList.add(calculated);
		}
		int totalRows = 0;
		for (double[][] calibData : calibDataList) {
			totalRows += calibData.length;
		}
		double[][] concatenatedData = new double[totalRows][];
		int currentRow = 0;
		for (double[][] calibData : calibDataList) {
			for (double[] row : calibData) {
				concatenatedData[currentRow++] = row;
			}
		}
		double[][] profile = dataWithAngleDiff(concatenatedData);
		double[] regressionParameters = linearRegression(profile);
		double[][] filteredData = filterData(profile, regressionParameters, generalParas);

		FittingParas fittingParas = polyFitting(filteredData, generalParas);
		// System.out.println("concatenatedData: " + concatenatedData.length);
		// System.out.println("filteredData: " + filteredData.length);
		// Load the data:
		MemoryPeakResults PeakfitData = ResultsManager.loadInputResults(name2, false, null, null);
		if (MemoryPeakResults.isEmpty(PeakfitData)) {
			IJ.error(TITLE, "No peakfit results could be loaded");
			return;
		}
		double[][] DataFilteredPrecision = filterDataByPrecision(PeakfitData, precisionCutoff);
		System.out.println("DataFilteredPrecision: " + DataFilteredPrecision.length);

		List<List<Double>> processedResult = processData(DataFilteredPrecision, generalParas, regressionParameters);
		List<List<Double>> xyzN = calculateCoordinates(processedResult, fittingParas, generalParas);
		List<List<Double>> filteredPeakResult = filterPeakfitData(processedResult, xyzN, filterParas, fittingParas);
		if (saveToFile == true) {
			saveTo3D(filteredPeakResult, name2, savingFormat);
		}
		// View localisation
		view3DResult(filteredPeakResult);
		MemoryPeakResults finalResult = saveToMemory(name2, filteredPeakResult);
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
		Class<MultiBeadsDHPSFU> clazz = MultiBeadsDHPSFU.class;
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
		// MemoryPeakResults.addResults(getResults(name1, calibPath));
		MemoryPeakResults.addResults(getResults(name2, dataPath));

		// Run the plugin
		IJ.runPlugIn(clazz.getName(), "");
	} // End of main
		// End of class

//public static void main(String[] args) {
//	new MultiBeadsDHPSFU().run(
//			"fileType=DHPSFU savingFormat=.3d pxSize=100 ifManualIndex=false dataPath=C:/Users/yw525/Documents/cell2_2nM_slice7_corr.3d");
//}
}
