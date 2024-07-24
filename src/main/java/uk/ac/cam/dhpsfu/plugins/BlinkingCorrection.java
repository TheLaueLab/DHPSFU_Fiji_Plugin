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
import ij.Macro;
import ij.Prefs;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import ij.plugin.frame.Recorder;

import java.util.*;
import java.util.stream.IntStream;

import uk.ac.sussex.gdsc.core.ij.ImageJUtils;
import uk.ac.sussex.gdsc.core.ij.gui.ExtendedGenericDialog;
import uk.ac.sussex.gdsc.smlm.data.config.CalibrationWriter;
import uk.ac.sussex.gdsc.smlm.data.config.CalibrationProtos.CameraType;
import uk.ac.sussex.gdsc.smlm.data.config.UnitProtos.DistanceUnit;
import uk.ac.sussex.gdsc.smlm.data.config.UnitProtos.IntensityUnit;
import uk.ac.sussex.gdsc.smlm.data.config.UnitProtos.TimeUnit;
import uk.ac.cam.dhpsfu.analysis.BC_track;
import uk.ac.sussex.gdsc.smlm.ij.plugins.ResultsManager;
import uk.ac.sussex.gdsc.smlm.ij.plugins.ResultsManager.InputSource;
import uk.ac.sussex.gdsc.smlm.results.MemoryPeakResults;
import uk.ac.sussex.gdsc.smlm.results.PeakResult;
import uk.ac.cam.dhpsfu.analysis.PeakResultDHPSFU;

import java.awt.TextField;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class BlinkingCorrection implements PlugIn {

	private static final String TITLE = "Blinking Correction";
	private static String input = ""; // Input dataset name
	private static String name1 = "File"; // Dataset name
	// private static String PixUnit = "Pixel";

	// Parameters for blinking correctio
	private int numDimension = 3; // Pixel size in nm
	private double maxJumpDist = 300; // precision cutoff in nm
	private int maxFrameGap = 50; // Step length of calibration in nm
	private int minNumPos = 1; // Fitting mode, can be 'Frame', 'Angle', or 'Z'. Default is 'Frame'
	private static double pxSize = 210;

	private static String savePath;
	// Extra options
	private boolean saveToFile;
	private boolean saveInfoToFile;
	private boolean saveAssignedTrack;

	@Override
	public void run(String arg) {
		String macroOptions = Macro.getOptions();
		
		// showInstruction();
		if (showDialog(macroOptions)) {
			blinkingCorrection();
		
		}
	}

	private boolean showDialog(String arg) {
		if (arg != null) {
			parseArguments(arg);
		}
		ExtendedGenericDialog gd = new ExtendedGenericDialog(TITLE);
		gd.addMessage("Localisation file: ");
		ResultsManager.addInput(gd, input, InputSource.MEMORY);
		gd.addMessage("Parameters: ");
		// gd.addCheckbox("Is data unit in Pixel?", PixUnit);
		// String[] formats = { "Pixel", "nm" };
		// gd.addChoice("Distance Unit", formats, formats[0]);
		double pxSize = Prefs.get("BlinkingCorrection.pxSize", 210.0);
		int numDimension = (int) Prefs.get("BlinkingCorrection.numDimension", 3);
		double maxJumpDist = Prefs.get("BlinkingCorrection.maxJumpDist", 300);
		int maxFrameGap = (int) Prefs.get("BlinkingCorrection.maxFrameGap", 50);
		int minNumPos = (int) Prefs.get("BlinkingCorrection.minNumPos", 1);
		boolean saveToFile = Prefs.get("BlinkingCorrection.saveToFile", false);
		boolean saveInfoToFile = Prefs.get("BlinkingCorrection.saveInfoToFile", false);
		boolean saveAssignedTrack = Prefs.get("BlinkingCorrection.saveAssignedTrack", false);
		String saveDirectory = Prefs.get("BlinkingCorrection.saveDirectory", "--Please_Select--");
		
		gd.addNumericField("Pixel_size", pxSize, 1);
		gd.addNumericField("Dimensions", numDimension, 0);
		gd.addNumericField("MaxJumpDist", maxJumpDist, 0);
		gd.addNumericField("MaxFrameGap", maxFrameGap, 0);
		gd.addNumericField("MinNumLocs", minNumPos, 0);
		gd.addMessage("File output:");
		gd.addCheckbox("Save corrected localisations", saveToFile);
		gd.addCheckbox("Save track info", saveInfoToFile);
		gd.addCheckbox("Save assigned track", saveAssignedTrack);
		gd.addDirectoryField("Save_directory", "--Please_Select--");

		String html = "<html>" + "<h2>Instruction about Blinking Correction Plugin</h2>"
		// +"<font size=+1>"
				+ "*** For temporal grouping of localisations ***<br>" + "<br>"
				+ "  - Input data: a list of 2D/3D localisations in <font color=red>ImageJ MEMORY</font>. <br>"
				+ "               (You can load file from directory using the Load Localisation function) <br>"
				+ "<br>" + "Parameters:  <br>" 
				+ "  - Pixel size (nm): Camera pixel size in nm. <br>"
				+ "  - Dimensions: Number of dimensions of the data. Default = 3.  <br>"
				+ "  - MaxJumpDist: Maximum jump distance allowed between frames to count the two localisations as originating from the same molecule.   <br>"
				+ "  - MaxFrameGap: Mmaximum change in frame number for two consecutive positions of the same molecule.  <br>"
				+ "  - MinNumLocs: A filter on the minimum number of localisations originating from the same molecule.  <br>"
				+ "<br>" + "File output:  <br>"
				+ "  - Save corrected localisations: save the average coordinate of each molecule into a new .3d (tab-separarted “x y z Intensity Frame”) file.   <br>"
				+ "  - Save track info: save additional track info into a Track_info.csv, containing: <br>"
				+ "  ** # track: number of track.  <br>"
				+ "  ** numberPositions: number of localisations within this track.  <br>"
				+ "  ** deltaFrame: the number of frames this track covers, i.e. last frame – first frame.  <br>"
				+ "  ** averageIntensity/averageX/averageY/averageZ: average intensity/x/y/z of all localisations within this track. <br>"
				+ "  - Save assigned track: save the original localisations and track assignment, containing: <br>"
				+ "  ** # track, x, y, z, internsity, frame  <br>" + "<br>" 
				+ "- Save directory: Save the processed data to user-speficied directory.  <br>"
				+ "</font>";
		
		gd.addHelp(html);
		gd.showDialog();
		if (gd.wasCanceled()) {
			return false;
		}
		input = ResultsManager.getInputSource(gd);
		
		// if (input == "Pixel") {
		ResultsManager.loadInputResults(input, true, IntensityUnit.PHOTON);
		// } else {
		// ResultsManager.loadInputResults(input, true, DistanceUnit.NM,
		// IntensityUnit.PHOTON);
		// }
		// PixUnit = gd.getNextChoice();
		if (arg == null || arg.length() == 0) {
			pxSize = gd.getNextNumber();
			numDimension = (int) gd.getNextNumber();
			maxJumpDist = gd.getNextNumber();
			maxFrameGap = (int) gd.getNextNumber();
			minNumPos = (int) gd.getNextNumber();
			saveToFile = gd.getNextBoolean();
			saveInfoToFile = gd.getNextBoolean();
			saveAssignedTrack = gd.getNextBoolean();
			savePath = gd.getNextString();
			name1 = input;
		} else {

		Vector<?> numericFields = gd.getNumericFields();
		if (numericFields != null && numericFields.size() >= 5) {
			pxSize = getNumericFieldValue(numericFields, 0);
			numDimension = (int) getNumericFieldValue(numericFields, 1);
			maxJumpDist = getNumericFieldValue(numericFields, 2);
			maxFrameGap = (int) getNumericFieldValue(numericFields, 3);
			minNumPos = (int) getNumericFieldValue(numericFields, 4);
		}

			// Parse checkbox fields
			Vector<?> checkboxFields = gd.getCheckboxes();
			if (checkboxFields != null && checkboxFields.size() >= 3) {
				saveToFile = getCheckboxFieldValue(checkboxFields, 0);
				saveInfoToFile = getCheckboxFieldValue(checkboxFields, 1);
				saveAssignedTrack = getCheckboxFieldValue(checkboxFields, 2);
			}

			// Parse string fields
			Vector<?> stringFields = gd.getStringFields();
			if (stringFields != null && stringFields.size() >= 2) {
				savePath = getStringFieldValue(stringFields, 1);
			}
			name1 = input;
		}

		Prefs.set("BlinkingCorrection.pxSize", pxSize);
		Prefs.set("BlinkingCorrection.numDimension", numDimension);
		Prefs.set("BlinkingCorrection.maxJumpDist", maxJumpDist);
		Prefs.set("BlinkingCorrection.maxFrameGap", maxFrameGap);
		Prefs.set("BlinkingCorrection.minNumPos", minNumPos);
		Prefs.set("BlinkingCorrection.saveToFile", saveToFile);
		Prefs.set("BlinkingCorrection.saveInfoToFile", saveInfoToFile);
		Prefs.set("BlinkingCorrection.saveAssignedTrack", saveAssignedTrack);
		Prefs.set("BlinkingCorrection.saveDirectory", saveDirectory);

		// Ensure the preferences are saved to disk
		Prefs.savePreferences();
		
		StringBuilder command = new StringBuilder();
		command.append("run(\"Blinking Correction\", ");
		command.append("\"Input=").append(input).append(" ");
		command.append("Pixel_size=").append(pxSize).append(" ");
		command.append("Dimensions=").append(numDimension).append(" ");
		command.append("MaxJumpDist=").append(maxJumpDist).append(" ");
		command.append("MaxFrameGap=").append(maxFrameGap).append(" ");
		command.append("MinNumLocs=").append(minNumPos).append(" ");
		command.append("Save_corrected_localisations=").append(saveToFile).append(" ");
		command.append("Save_track_info=").append(saveInfoToFile).append(" ");
		command.append("Save_assigned_track=").append(saveAssignedTrack).append(" ");
		command.append("Save_directory=").append(savePath).append("\");");
		if (Recorder.record) {
			Recorder.recordString(command.toString());
		}

		// System.out.print(name1);
		return true;
	} // End of shoeDialog

	private void parseArguments(String arg) {
		String[] params = arg.split(" ");
		for (String param : params) {
			String[] keyVal = param.split("=");
			if (keyVal.length == 2) {
				switch (keyVal[0]) {
				case "Input":
					input = keyVal[1];
					break;
				case "Pixel_size":
					pxSize = Double.parseDouble(keyVal[1]);
					break;
				case "Dimensions":
					numDimension = Integer.parseInt(keyVal[1]);
					break;
				case "MaxJumpDist":
					maxJumpDist = Double.parseDouble(keyVal[1]);
					break;
				case "MaxFrameGap":
					maxFrameGap = Integer.parseInt(keyVal[1]);
					break;
				case "MinNumLocs":
					minNumPos = Integer.parseInt(keyVal[1]);
					break;
				case "Save_corrected_localisations":
					saveToFile = Boolean.parseBoolean(keyVal[1]);
					break;
				case "Save_track_info":
					saveInfoToFile = Boolean.parseBoolean(keyVal[1]);
					break;
				case "Save_assigned_track":
					saveAssignedTrack = Boolean.parseBoolean(keyVal[1]);
					break;
				case "Save_directory":
					savePath = keyVal[1];
					break;

				}
			}
			// IJ.log("Set " + keyVal[0] + " to " + keyVal[1]);
		}
	}

	private double getNumericFieldValue(Vector<?> numericFields, int index) {
		TextField field = (TextField) numericFields.get(index);
		try {
			return Double.parseDouble(field.getText());
		} catch (NumberFormatException e) {
			IJ.log("Error parsing numeric value: " + e.getMessage());
			return Double.NaN;
		}
	}

//	private static String getChoiceFieldValue(Vector<?> fields, int index) {
//		return ((java.awt.Choice) fields.get(index)).getSelectedItem();
//	}

	private static boolean getCheckboxFieldValue(Vector<?> fields, int index) {
		return ((java.awt.Checkbox) fields.get(index)).getState();
	}

	private static String getStringFieldValue(Vector<?> fields, int index) {
		return ((java.awt.TextField) fields.get(index)).getText();
	}

	private static double[][] resultToArray(MemoryPeakResults results, double pxSize) {
		if (MemoryPeakResults.isEmpty(results)) {
			IJ.error(TITLE, "No results could be loaded");
		}
		int size = results.size();
		System.out.println("Size" + size);
		double[][] r = new double[size][5];
		for (int i = 0; i < size; i++) {
			if (results.getDistanceUnit() == DistanceUnit.PIXEL) {
				r[i][0] = results.get(i).getXPosition() * pxSize;
				r[i][1] = results.get(i).getYPosition() * pxSize;
				r[i][2] = results.get(i).getZPosition() * pxSize;
			} else if (results.getDistanceUnit() == DistanceUnit.NM) {
				r[i][0] = results.get(i).getXPosition();
				r[i][1] = results.get(i).getYPosition();
				r[i][2] = results.get(i).getZPosition();
			}
			r[i][3] = results.get(i).getIntensity();
			r[i][4] = results.get(i).getFrame();
		}
		return r;
	} // End of resultToArray

	private void processPosition(Set<BC_track> finishedTracks, Set<BC_track> currentTracks, double[] position,
			int frame, double intensity, double maxJumpDistance, int maxFrameGap) {
		double[] newPosition = new double[] { position[0], position[1], position[2] };
		double bestDist = -1.0;
		BC_track bestTrack = null;
		List<BC_track> tracksToRemove = new ArrayList<>();
		for (BC_track track : currentTracks) {
			if (frame > track.frame.get(track.frame.size() - 1) + maxFrameGap) {
				tracksToRemove.add(track);
				finishedTracks.add(track);
			} else if (frame > track.frame.get(track.frame.size() - 1)) {
				double distance = BC_track.calcAdjustedDistance(newPosition[0], newPosition[1], newPosition[2], frame,
						track);
				if (distance < maxJumpDistance && (bestTrack == null || distance < bestDist)) {
					bestDist = distance;
					bestTrack = track;
				}
			}
		}
		currentTracks.removeAll(tracksToRemove);
		if (bestTrack != null) {
			bestTrack.addPosition(newPosition[0], newPosition[1], newPosition[2], frame, intensity);
		} else {
			BC_track track = new BC_track();
			track.addPosition(newPosition[0], newPosition[1], newPosition[2], frame, intensity);
			currentTracks.add(track);
		}
	} // End of processPosition

	private List<BC_track> determineTracks(double[][] threed_data, double maxJumpDistance, int maxFrameGap,
			int minNumPositions) {
		System.out.println("found " + threed_data.length + " records");
		Set<BC_track> finishedTracks = new HashSet<>();
		Set<BC_track> currentTracks = new HashSet<>();
		Arrays.sort(threed_data, Comparator.comparingDouble(row -> row[4]));
		for (int n = 0; n < threed_data.length; n++) {
			int frame = (int) threed_data[n][4];
			double x = threed_data[n][0];
			double y = threed_data[n][1];
			double z = threed_data[n][2];
			double intensity = threed_data[n][3];
			double[] position = { x, y, z };
			processPosition(finishedTracks, currentTracks, position, frame, intensity, maxJumpDistance, maxFrameGap);
		}
		finishedTracks.addAll(currentTracks);
		System.out.println("Number of tracks = " + finishedTracks.size());
		// Filter out short tracks
		List<BC_track> filteredTracks = new ArrayList<>();
		for (BC_track track : finishedTracks) {
			if (track.x.size() >= minNumPositions) {
				filteredTracks.add(track);
			}
		}
		// Sort the filteredTracks based on frame
		Collections.sort(filteredTracks, Comparator.comparingInt(track -> track.frame.get(0)));

		final String msg = "Number of localisations after filtering for >= " + minNumPositions + " positions = "
				+ filteredTracks.size();
		IJ.showStatus(msg);
		ImageJUtils.log(msg);

		return filteredTracks;
	} // End of determineTracks

	public void printFilteredTracks(List<BC_track> filteredTracks) {
		for (int i = 0; i < filteredTracks.size(); i++) {
			BC_track track = filteredTracks.get(i);
			System.out.println("Track " + (i + 1) + ":");
			System.out.println("Number of positions: " + track.x.size());
			System.out.println("Frames: " + track.frame);
			System.out.println("X positions: " + track.x);
			System.out.println("Y positions: " + track.y);
			System.out.println("Z positions: " + track.z);
			System.out.println("Intensities: " + track.intensity);
			System.out.println("Distances: " + track.distances);
			System.out.println();
		}
	} // End of determineTracks

	public static double[][] transpose(double[][] original) {
		int numRows = original.length;
		int numCols = original[0].length;
		double[][] transposed = new double[numCols][numRows];
		for (int i = 0; i < numRows; i++) {
			for (int j = 0; j < numCols; j++) {
				transposed[j][i] = original[i][j];
			}
		}
		return transposed;
	} // End of transpose

	public static void writeTracksToFile(String fileName, List<BC_track> tracks) {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
			for (BC_track track : tracks) {
				double x = track.getAverageX();
				double y = track.getAverageY();
				double z = track.getAverageZ();
				double intensity = track.getAverageI();
				int frame = track.frame.get(0);
				StringBuilder sb = new StringBuilder();
				sb.append(x).append('\t');
				sb.append(y).append('\t');
				sb.append(z).append('\t');
				sb.append(intensity).append('\t');
				sb.append(frame);
				writer.write(sb.toString());
				writer.newLine();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	} // End of writeTracksToFile

	public static List<List<Double>> saveTracksToMemory(List<BC_track> tracks) {
		List<List<Double>> result = new ArrayList<>();
		for (BC_track track : tracks) {
			List<Double> trackData = new ArrayList<>();
			double x = track.getAverageX();
			double y = track.getAverageY();
			double z = track.getAverageZ();
			double intensity = track.getAverageI();
			int frame = track.frame.get(0);
			trackData.add(x);
			trackData.add(y);
			trackData.add(z);
			trackData.add(intensity);
			trackData.add((double) frame);
			result.add(trackData);
		}
		return result;
	} // End of saveTracksToMemory

	private void writeTracksSummaryToFile(List<BC_track> tracks, String fileName) {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
			writer.write("# track, numberPositions, deltaFrames, averageIntensity, averageX,averageY,averageZ");
			writer.newLine();
			for (int n = 0; n < tracks.size(); n++) {
				BC_track track = tracks.get(n);
				String averagePositionX = String.format("%.1f", track.getAverageX());
				String averagePositionY = String.format("%.1f", track.getAverageY());
				String averagePositionZ = String.format("%.1f", track.getAverageZ());
				writer.write(String.format("%d,%d,%d,%.1f,%s,%s,%s", n + 1, track.x.size(), track.getDeltaFrames(),
						track.getAverageI(), averagePositionX, averagePositionY, averagePositionZ));
				writer.newLine();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	} // End of writeTracksSummaryToFile

	private void saveTracks(List<BC_track> tracks, String fileName) {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
			writer.write("#track,x,y,z,intensity,frame\n");

			for (int n = 0; n < tracks.size(); n++) {
				BC_track track = tracks.get(n);

				List<Double> x = track.getX_list();
				List<Double> y = track.getY_list();
				List<Double> z = track.getZ_list();
				List<Integer> frames = track.getFrame_list();
				List<Double> intensities = track.getI_list();

				for (int i = 0; i < frames.size(); i++) {
					double position0, position1, position2;
					int frame = frames.get(i);
					double intensity = intensities.get(i);

					if (x.size() <= i || y.size() <= i || z.size() <= i) {
						// Handle the case where there are not enough positions
						position0 = position1 = position2 = 0.0;
					} else {
						position0 = x.get(i);
						position1 = y.get(i);
						position2 = z.get(i);
					}

					String[] fields = { String.valueOf(n + 1), String.valueOf(position0), String.valueOf(position1),
							String.valueOf(position2), String.valueOf(intensity), String.valueOf(frame) };
					writer.write(String.join(",", fields) + "\n");
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	} // End of saveTracks

	private void viewInfoResult(List<BC_track> tracks) {
		double[] xArray = new double[tracks.size()];
		double[] yArray = new double[tracks.size()];
		double[] zArray = new double[tracks.size()];
		double[] trackNumbers = new double[tracks.size()];
		double[] posNum = new double[tracks.size()];
		double[] FrameDelta = new double[tracks.size()];
		double[] IArray = new double[tracks.size()];
		for (int n = 0; n < tracks.size(); n++) {
			BC_track track = tracks.get(n);
			double averagePositionX = track.getAverageX();
			double averagePositionY = track.getAverageY();
			double averagePositionZ = track.getAverageZ();
			double numOfTrack = n + 1;
			double numPos = track.x.size();
			double deltaFrame = track.getDeltaFrames();
			double averageI = track.getAverageI();
			xArray[n] = averagePositionX;
			yArray[n] = averagePositionY;
			zArray[n] = averagePositionZ;
			trackNumbers[n] = numOfTrack;
			posNum[n] = numPos;
			FrameDelta[n] = deltaFrame;
			IArray[n] = averageI;
		}
		ResultsTable t = new ResultsTable();
		t.setValues("# Track", trackNumbers);
		t.setValues("numberPositions", posNum);
		t.setValues("deltaFrames", FrameDelta);
		t.setValues("average Intensity", IArray);
		t.setValues("Average X", xArray);
		t.setValues("Average Y", yArray);
		t.setValues("Average Z", zArray);
		t.show("Track info");
	} // End of viewInfoResult

	private void viewTrackResults(List<BC_track> tracks) {
		ResultsTable table = new ResultsTable();
		for (int n = 0; n < tracks.size(); n++) {
			BC_track track = tracks.get(n);
			List<Double> x = track.getX_list();
			List<Double> y = track.getY_list();
			List<Double> z = track.getZ_list();
			List<Integer> frames = track.getFrame_list();
			List<Double> intensities = track.getI_list();
			for (int i = 0; i < frames.size(); i++) {
				double position0, position1, position2, intensity;
				int frame;
				if (x.size() <= i || y.size() <= i || z.size() <= i || frames.size() <= i || intensities.size() <= i) {
					position0 = position1 = position2 = intensity = 0.0;
					frame = 0;
				} else {
					position0 = x.get(i);
					position1 = y.get(i);
					position2 = z.get(i);
					intensity = intensities.get(i);
					frame = frames.get(i);
				}
				table.incrementCounter();
				table.addValue("# Track", n + 1);
				table.addValue("X", position0);
				table.addValue("Y", position1);
				table.addValue("Z", position2);
				table.addValue("Intensity", intensity);
				table.addValue("Frame", frame);
			}
		}
		table.show("Track assigned results");
	} // End of viewTrackResults

	private double[][] toDouble(List<List<Double>> list) {
		int rows = list.size();
		int cols = list.get(0).size();
		return IntStream.range(0, cols)
				.mapToObj(col -> IntStream.range(0, rows).mapToDouble(row -> list.get(row).get(col)).toArray())
				.toArray(double[][]::new);
	} // End of toDouble

	private MemoryPeakResults saveToMemory(String input, List<List<Double>> result) {
		String name = input + "_BC";
		double[][] doubleFilteredPeakResult = toDouble(result);
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
	} // End of saveToMemory

	private void view3DResult(List<List<Double>> filteredPeakResult) {
		double[][] doubleFilteredPeakResult = toDouble(filteredPeakResult);
		double[] frame = doubleFilteredPeakResult[4];
		double[] x = doubleFilteredPeakResult[0];
		double[] y = doubleFilteredPeakResult[1];
		double[] z = doubleFilteredPeakResult[2];
		double[] intensity = doubleFilteredPeakResult[3];
		ResultsTable t = new ResultsTable();
		t.setValues("X", x);
		t.setValues("Y", y);
		t.setValues("Z", z);
		t.setValues("Intensity", intensity);
		t.setValues("Frame", frame);
		t.show("Blinking corrected results");
	} // End of view3DResult

	private void blinkingCorrection() {
		long startTime = System.currentTimeMillis();
		double[][] threed_data;
		IJ.log("Data file selected: " + input);
		MemoryPeakResults r = MemoryPeakResults.getResults(name1);
//		CalibrationWriter cw1 = new CalibrationWriter();
//		cw1.setIntensityUnit(IntensityUnit.PHOTON);
//		cw1.setDistanceUnit(DistanceUnit.PIXEL);
//		cw1.setTimeUnit(TimeUnit.FRAME);
//		cw1.setExposureTime(50);
//		cw1.setNmPerPixel(pxSize);
//		cw1.setCountPerPhoton(45);
//		cw1.getBuilder().getCameraCalibrationBuilder().setCameraType(CameraType.EMCCD).setBias(100)
//				.setQuantumEfficiency(0.95).setReadNoise(1.6);
//		r.setCalibration(cw1.getCalibration());
		threed_data = resultToArray(r, pxSize);
		//System.out.print(threed_data.length + " = Row");
		//System.out.print(threed_data[1].length + " = Column");
		List<BC_track> filteredTracks = determineTracks(threed_data, maxJumpDist, maxFrameGap, minNumPos);
		List<List<Double>> result = saveTracksToMemory(filteredTracks);
		view3DResult(result);
		viewInfoResult(filteredTracks);
		viewTrackResults(filteredTracks);
		System.out.print(saveToFile);
		System.out.print(saveInfoToFile);
		if (saveToFile == true) {
			String savePath2 = savePath + name1 + "_BC.3d";
			System.out.print("Path " + savePath2);
			writeTracksToFile(savePath2, filteredTracks);
		}
		if (saveInfoToFile == true) {
			String saveInfoPath = savePath + name1 + "_Track_Info.csv";
			writeTracksSummaryToFile(filteredTracks, saveInfoPath);
		}
		if (saveAssignedTrack == true) {
			String savePath3 = savePath + name1 + "_Assigned.csv";
			saveTracks(filteredTracks, savePath3);
		}
		MemoryPeakResults finalResult = saveToMemory(name1, result);
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
		long endTime = System.currentTimeMillis();
		long duration = endTime - startTime;
		double seconds = (double) duration / 1000.0;
		IJ.log("Blinking Correction runtime: " + seconds + " seconds");
	} // End of blinkingCorrection

	/*
	 * public static void main(String[] args) throws URISyntaxException { // Set the
	 * base directory for plugins // see:
	 * https://stackoverflow.com/a/7060464/1207769 Class<BlinkingCorrection> clazz =
	 * BlinkingCorrection.class; java.net.URL url =
	 * clazz.getProtectionDomain().getCodeSource().getLocation(); File file = new
	 * File(url.toURI()); // Note: This returns the base path. ImageJ will find
	 * plugins in here that have an // underscore in the name. But it will not
	 * search recursively through the // package structure to find plugins. Adding
	 * this at least puts it on ImageJ's // classpath so plugins not satisfying
	 * these requirements can be loaded. System.setProperty("plugins.dir",
	 * file.getAbsolutePath());
	 * 
	 * // Start ImageJ and exit when closed ImageJ imagej = new ImageJ();
	 * imagej.exitWhenQuitting(true);
	 * 
	 * // If this is in a sub-package or has no underscore then manually add the
	 * plugin String packageName = clazz.getName().replace(clazz.getSimpleName(),
	 * ""); if (!packageName.isEmpty() || clazz.getSimpleName().indexOf('_') < 0) {
	 * // Add a spacer ij.Menus.installPlugin("", ij.Menus.PLUGINS_MENU, "-", "",
	 * IJ.getInstance()); ij.Menus.installPlugin(clazz.getName(),
	 * ij.Menus.PLUGINS_MENU, clazz.getSimpleName().replace('_', ' '), "",
	 * IJ.getInstance()); }
	 * 
	 * // Initialise for testing, e.g. create some random datasets load();
	 * 
	 * // Run the plugin IJ.runPlugIn(clazz.getName(), ""); } // End of main
	 */

}
