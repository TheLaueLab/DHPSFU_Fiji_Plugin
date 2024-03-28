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
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;

import java.util.*;
import java.util.stream.IntStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class BlinkingCorrection implements PlugIn {

	private static final String TITLE = "Blinking Correction";
	private static String input = ""; // Input dataset name
	private static String name1 = "File"; // Dataset name
	private static String PixUnit = "Pixel";

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

	@Override
	public void run(String arg) {
		// showInstruction();
		if (showDialog()) {
			blinkingCorrection();
		}
	}

	private boolean showDialog() {
		ExtendedGenericDialog gd = new ExtendedGenericDialog(TITLE);
		gd.addMessage("Localisation file: ");
		ResultsManager.addInput(gd, input, InputSource.MEMORY);
		gd.addMessage("Parameters: ");
		// gd.addCheckbox("Is data unit in Pixel?", PixUnit);
		String[] formats = { "Pixel", "nm" };
		gd.addChoice("Distance Unit", formats, formats[1]);
		gd.addNumericField("Pixel size", pxSize, 1);
		gd.addNumericField("Dimensions", numDimension, 0);
		gd.addNumericField("MaxJumpDist", maxJumpDist, 0);
		gd.addNumericField("MaxFrameGap", maxFrameGap, 0);
		gd.addNumericField("MinNumLocs", minNumPos, 1);
		gd.addMessage("File output:");
		gd.addCheckbox("Save corrected localisations", saveToFile);
		gd.addCheckbox("Save track info", saveInfoToFile);
		gd.addDirectoryField("Save_directory", "");
		String html = "<html>" + "<h2>Instruction about Blinking Correction Plugin</h2>"
		// +"<font size=+1>"
				+ "*** For temporal grouping of localisations ***<br>" + "<br>"
				+ "  - Input data: DHPSFU processed .3d format files from <font color=red>MEMORY</font>. <br>"
				+ "               (You can load file from directory using the Load Localisation function) <br>"
				+ "  - Need to spefify the distance unit of the data, Pixel or nm. <br>"
				+ "  - Make sure you choose the <font color=red>CORRECT DISTANCE UNIT</font> of the data. It might be different when you import the file from directory. <br>"
				+ "<br>" + "Parameters:  <br>" + "  - Dimensions: Dimension of the data. Default = 3.  <br>"
				+ "  - MaxJumpDist: Maximum jump distance allowed between frames.  <br>"
				+ "  - MaxFrameGap: Mmaximum change in frame number for two consecutive positions on track.  <br>"
				+ "  - MinNumLocs: Minimum number of localisations on track to be further considered.  <br>" + "<br>"
				+ "File output:  <br>"
				+ "  - Save corrected localisations: Tick if you want to save the corrected localisations into file. File format is also .3d.  <br>"
				+ "  - Save track info: Tick if you want to save all track info into a file. File format is .csv.  <br>"
				+ "<br>" + "Columns in Track_info.csv:  <br>" + "  - # track: number of track.  <br>"
				+ "  - numberPositions: number of localisations within this track.  <br>"
				+ "  - deltaFrame: frame difference within track.  <br>"
				+ "  - averageIntensity/averageX/averageY/averageZ: average intensity/x/y/z of all localisations within this track. <br>"
				+ "<br>" + "</font>";
		gd.addHelp(html);
		gd.showDialog();
		if (gd.wasCanceled()) {
			return false;
		}

		input = ResultsManager.getInputSource(gd);
		if (input == "Pixel") {
			ResultsManager.loadInputResults(input, true, DistanceUnit.PIXEL, IntensityUnit.PHOTON);
		} else {
			ResultsManager.loadInputResults(input, true, DistanceUnit.NM, IntensityUnit.PHOTON);
		}
		PixUnit = gd.getNextChoice();
		pxSize = gd.getNextNumber();
		numDimension = (int) gd.getNextNumber();
		maxJumpDist = gd.getNextNumber();
		maxFrameGap = (int) gd.getNextNumber();
		minNumPos = (int) gd.getNextNumber();
		saveToFile = gd.getNextBoolean();
		saveInfoToFile = gd.getNextBoolean();
		savePath = gd.getNextString();
		name1 = input;
		System.out.print(name1);
		return true;
	} // End of shoeDialog

	private static double[][] resultToArray(MemoryPeakResults results, String PixUnit, double pxSize) {
		if (MemoryPeakResults.isEmpty(results)) {
			IJ.error(TITLE, "No results could be loaded");
		}
		int size = results.size();
		System.out.println("Size" + size);
		double[][] r = new double[size][5];
		for (int i = 0; i < size; i++) {
			if (PixUnit == "Pixel") {
				r[i][0] = results.get(i).getXPosition() * pxSize;
				r[i][1] = results.get(i).getYPosition() * pxSize;
				r[i][2] = results.get(i).getZPosition() * pxSize;
			} else {
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
		final String msg = "Number of tracks after filtering for >= " + minNumPositions + " positions = "
				+ filteredTracks.size();
		IJ.showStatus(msg);
		ImageJUtils.log(msg);
		System.out.println(
				"Number of tracks after filtering for >= " + minNumPositions + " positions = " + filteredTracks.size());
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

	public void writeTracksSummaryToFile(List<BC_track> tracks, String fileName) {
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
		t.setValues("averageIntensity", IArray);
		t.setValues("Average X", xArray);
		t.setValues("Average Y", yArray);
		t.setValues("Average Z", zArray);
		t.show("Track info");
	} // End of viewInfoResult

	private double[][] toDouble(List<List<Double>> list) {
		int rows = list.size();
		int cols = list.get(0).size();
		return IntStream.range(0, cols)
				.mapToObj(col -> IntStream.range(0, rows).mapToDouble(row -> list.get(row).get(col)).toArray())
				.toArray(double[][]::new);
	} // End of toDouble

	private MemoryPeakResults saveToMemory(List<List<Double>> result) {
		String name = "Corrected Result";
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
		t.setValues("X (px)", x);
		t.setValues("Y (px)", y);
		t.setValues("Z (px)", z);
		t.setValues("Intensity (photon)", intensity);
		t.setValues("Frame", frame);
		t.show("Corrected results");
	} // End of view3DResult

	private void blinkingCorrection() {
		double[][] threed_data;
		IJ.log("load from memory: " + name1);
		MemoryPeakResults r = MemoryPeakResults.getResults(name1);
		CalibrationWriter cw1 = new CalibrationWriter();
		cw1.setIntensityUnit(IntensityUnit.PHOTON);
		cw1.setDistanceUnit(DistanceUnit.PIXEL);
		cw1.setTimeUnit(TimeUnit.FRAME);
		cw1.setExposureTime(50);
		cw1.setNmPerPixel(pxSize);
		cw1.setCountPerPhoton(45);
		cw1.getBuilder().getCameraCalibrationBuilder().setCameraType(CameraType.EMCCD).setBias(100)
				.setQuantumEfficiency(0.95).setReadNoise(1.6);
		r.setCalibration(cw1.getCalibration());
		threed_data = resultToArray(r, PixUnit, pxSize);
		System.out.print(threed_data.length + " = Row");
		System.out.print(threed_data[1].length + " = Column");
		List<BC_track> filteredTracks = determineTracks(threed_data, maxJumpDist, maxFrameGap, minNumPos);
		List<List<Double>> result = saveTracksToMemory(filteredTracks);
		view3DResult(result);
		viewInfoResult(filteredTracks);
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
		MemoryPeakResults finalResult = saveToMemory(result);
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
