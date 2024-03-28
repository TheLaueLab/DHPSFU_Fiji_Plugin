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

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.SplittableRandom;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import ij.IJ;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import uk.ac.cam.dhpsfu.analysis.ArrayUtils;
import uk.ac.cam.dhpsfu.analysis.FileIndex;
import uk.ac.cam.dhpsfu.analysis.PeakResultDHPSFU;
import uk.ac.cam.dhpsfu.analysis.Read3DFileCalib;
import uk.ac.sussex.gdsc.core.ij.ImageJUtils;
import uk.ac.sussex.gdsc.core.ij.gui.ExtendedGenericDialog;
import uk.ac.sussex.gdsc.core.utils.SimpleArrayUtils;
import uk.ac.sussex.gdsc.core.utils.TextUtils;
import uk.ac.sussex.gdsc.smlm.data.config.CalibrationWriter;

import uk.ac.sussex.gdsc.smlm.results.AttributePeakResult;
import uk.ac.sussex.gdsc.smlm.results.MemoryPeakResults;
import uk.ac.sussex.gdsc.smlm.results.PeakResult;
import uk.ac.sussex.gdsc.smlm.results.procedures.PrecisionResultProcedure;
import uk.ac.sussex.gdsc.smlm.results.procedures.StandardResultProcedure;
import uk.ac.sussex.gdsc.smlm.data.config.CalibrationProtos.CameraType;
import uk.ac.sussex.gdsc.smlm.data.config.UnitProtos.IntensityUnit;
import uk.ac.sussex.gdsc.smlm.data.config.UnitProtos.DistanceUnit;
import uk.ac.sussex.gdsc.smlm.data.config.UnitProtos.TimeUnit;
//import uk.ac.sussex.gdsc.smlm.ij.plugins.LoadLocalisationsSettings;
import uk.ac.sussex.gdsc.smlm.ij.plugins.SmlmUsageTracker;

public class LoadLocalisationFIle implements PlugIn {
	private static final String TITLE = "Load Localisations";

	public static String fileType = "Peakfit";
	// Data paths
	private static String dataPath = "E:/Fiji_sampledata/slice0.tif.results.xls"; // Data path
	private static String name;
	// private boolean saveToFile = true;
	private static String savingFormat = ".3d";

	private static double pxSize = 210;
	// column index
	private int frameIndex = 0;
	private int xIndex = 9;
	private int yIndex = 10;
	private int zIndex = -1;
	private int intensityIndex = 8;
	private int precisionIndex = 13;
	private FileIndex fileIndex = new FileIndex(frameIndex, xIndex, yIndex, zIndex, intensityIndex, precisionIndex);
	private boolean ifManualIndex = false;

	private int skipLines;

	@Override
	public void run(String arg) {
		SmlmUsageTracker.recordPlugin(this.getClass(), arg);
		if (showDialog()) {
			Load();
		}
	}

	private void viewLocalisations(MemoryPeakResults results, String fileType) {
		if (MemoryPeakResults.isEmpty(results)) {
			IJ.error(TITLE, "No results could be loaded");
			return;
		}
		if (fileType == "DHPSFU") {
			StandardResultProcedure s = new StandardResultProcedure(results, DistanceUnit.PIXEL, IntensityUnit.PHOTON);
			s.getTxy();
			s.getZ();
			s.getI();
			int[] frame = s.frame;
			float[] x = s.x;
			float[] y = s.y;
			float[] z = s.z;
			float[] intensity = s.intensity;
			ResultsTable t = new ResultsTable();
			t.setValues("Frame", SimpleArrayUtils.toDouble(frame));
			t.setValues("X", SimpleArrayUtils.toDouble(x));
			t.setValues("Y", SimpleArrayUtils.toDouble(y));
			t.setValues("Z", SimpleArrayUtils.toDouble(z));
			t.setValues("Intensity (photon)", SimpleArrayUtils.toDouble(intensity));
			t.show("Loaded localisations"); // need to change table name
		} else {
			PrecisionResultProcedure p = new PrecisionResultProcedure(results);
			p.getPrecision(true);
			double[] precisions = p.precisions;
			StandardResultProcedure s = new StandardResultProcedure(results, DistanceUnit.PIXEL, IntensityUnit.PHOTON);
			s.getTxy();
			s.getI();
			int[] frame = s.frame;
			float[] x = s.x;
			// System.out.println(x);
			float[] y = s.y;
			float[] intensity = s.intensity;

			ResultsTable t = new ResultsTable();
			t.setValues("Frame", SimpleArrayUtils.toDouble(frame));
			t.setValues("X", SimpleArrayUtils.toDouble(x));
			t.setValues("Y", SimpleArrayUtils.toDouble(y));
			t.setValues("Intensity (photon)", SimpleArrayUtils.toDouble(intensity));
			t.setValues("Precision (nm)", precisions);
			t.show("Loaded localisations"); // need to change table name
		}
	}

	private boolean showDialog() {
		ExtendedGenericDialog gd = new ExtendedGenericDialog(TITLE);
		gd.addMessage("File directory:");
		Preferences preferences = Preferences.userNodeForPackage(LoadLocalisationFIle.class);
		String defaultDirectory = preferences.get("defaultDirectory", "");
		gd.addFilenameField("File_directory", defaultDirectory);
		gd.addStringField("File_name", "Localisations");
		String[] formats2 = { "Peakfit", "DHPSFU" };
		gd.addChoice("Data_format", formats2, formats2[0]);
		String[] formats = { ".3d", ".csv", ".xls" };
		gd.addChoice("File_format", formats, formats[2]);
		gd.addNumericField("Pixel size (nm)", pxSize, 1);
		gd.addCheckbox("Manually set column Index", ifManualIndex);
		gd.addMessage("Column index (Set to -1 if not reading.):");
		gd.addNumericField("Header", skipLines, 0);
		gd.addNumericField("Frame", fileIndex.getFrameIndex(), 0);
		gd.addNumericField("X", fileIndex.getxIndex(), 0);
		gd.addNumericField("Y", fileIndex.getyIndex(), 0);
		gd.addNumericField("Z", fileIndex.getzIndex(), 0);
		gd.addNumericField("Intensity", fileIndex.getIntensityIndex(), 0);
		gd.addNumericField("Precision", fileIndex.getPrecisionIndex(), 0);
		gd.showDialog();
		if (gd.wasCanceled()) {
			return false;
		}

		dataPath = gd.getNextString();
		final File file = new File(dataPath);
		if (!file.exists()) {
			IJ.error(TITLE, "File does not exist.");
			return false;
		}
		preferences.put("defaultDirectory", dataPath);
		try {
			preferences.flush();
		} catch (BackingStoreException e) {
			e.printStackTrace();
		}
		name = gd.getNextString();
		fileType = gd.getNextChoice();
		savingFormat = gd.getNextChoice();
		pxSize = gd.getNextNumber();
		ifManualIndex = gd.getNextBoolean();

		if (!ifManualIndex) {
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
	} // End of shoeDialog

	private FileIndex getColumnIndex(String fileType) {
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

	private static MemoryPeakResults LoadLocalisations(String dataPath, FileIndex fileIndex, String savingFormat,
			int skipLines) {
		SplittableRandom rng = new SplittableRandom();
		Read3DFileCalib importer = new Read3DFileCalib();
		double[][] data = null;
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
					float s = (float) rng.nextDouble(1 * 0.9, 1 * 1.3);
					parameters[PeakResultDHPSFU.BACKGROUND] = (float) frame[i];
					parameters[PeakResultDHPSFU.X] = (float) x[i];
					parameters[PeakResultDHPSFU.Y] = (float) y[i];
					parameters[PeakResultDHPSFU.INTENSITY] = (float) intensity[i];
					parameters[PeakResultDHPSFU.PRECISION] = (float) precision[i];
					parameters[PeakResultDHPSFU.STANDARD_PARAMETERS] = s;
					Math.sqrt(parameters[PeakResultDHPSFU.INTENSITY]);
					PeakResultDHPSFU r = new PeakResultDHPSFU((int) frame[i], parameters[2], parameters[3],
							parameters[1], parameters[4]);
					AttributePeakResult ap = new AttributePeakResult(r);
					ap.setPrecision(precision[i]);
					results.add(ap);
				}
			}
			results.end();
			results.sort();

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
			// PSF.Builder psf =
			// PsfProtosHelper.DefaultOneAxisGaussian2dPsf.INSTANCE.toBuilder();
			// psf.getParametersBuilder(0).setValue(1);
			// results.setPsf(psf.build());
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
					parameters[PeakResultDHPSFU.BACKGROUND] = (float) frame[i];
					parameters[PeakResultDHPSFU.X] = (float) x[i];
					parameters[PeakResultDHPSFU.Y] = (float) y[i];
					parameters[PeakResultDHPSFU.INTENSITY] = (float) intensity[i];
					PeakResult r = new PeakResult((int) frame[i], parameters[2], parameters[3], parameters[1]);
					r.setZPosition((float) z[i]);
					results.add(r);
				}
			}
			results.end();
			results.sort();
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
			return results;
		}
		return results;
	}

	private static MemoryPeakResults LoadLocalisationsManual(String dataPath, FileIndex fileIndex, String fileType,
			int skipLines) {
		SplittableRandom rng = new SplittableRandom();
		Read3DFileCalib importer = new Read3DFileCalib();
		double[][] data = null;

		MemoryPeakResults results = new MemoryPeakResults();
		results.setName(name);
		if (fileType == "Peakfit") {
			try {
				data = importer.readCSVDouble(Paths.get(dataPath), skipLines);
			} catch (IOException e) {
				e.printStackTrace();
			}
			final String msg = "Data is " + TextUtils.pleural(data != null ? 1 : 0, "");
			IJ.showStatus(msg);
			ImageJUtils.log(msg);
			double[] frame = null;
			if (fileIndex.getFrameIndex() != -1) {
				frame = ArrayUtils.getColumn(data, fileIndex.getFrameIndex());
			}

			double[] x = null;
			if (fileIndex.getxIndex() != -1) {
				x = ArrayUtils.getColumn(data, fileIndex.getxIndex());
			}

			double[] y = null;
			if (fileIndex.getyIndex() != -1) {
				y = ArrayUtils.getColumn(data, fileIndex.getyIndex());
			}

			double[] intensity = null;
			if (fileIndex.getIntensityIndex() != -1) {
				intensity = ArrayUtils.getColumn(data, fileIndex.getIntensityIndex());
			}

			double[] precision = null;
			if (fileIndex.getPrecisionIndex() != -1) {
				precision = ArrayUtils.getColumn(data, fileIndex.getPrecisionIndex());
			}

			if (data != null) {
				results.begin();
				for (int i = 0; i < data.length; i++) {
					float[] parameters = new float[7];
					float s = (float) rng.nextDouble(1 * 0.9, 1 * 1.3);

					if (frame != null) {
						parameters[PeakResultDHPSFU.BACKGROUND] = (float) frame[i];
					}
					if (x != null) {
						parameters[PeakResultDHPSFU.X] = (float) x[i];
					}
					if (y != null) {
						parameters[PeakResultDHPSFU.Y] = (float) y[i];
					}
					if (intensity != null) {
						parameters[PeakResultDHPSFU.INTENSITY] = (float) intensity[i];
					}
					if (precision != null) {
						parameters[PeakResultDHPSFU.PRECISION] = (float) precision[i];
					}

					parameters[PeakResultDHPSFU.STANDARD_PARAMETERS] = s;
					// Set noise assuming photons have a Poisson distribution
					if (intensity != null) {
						Math.sqrt(parameters[PeakResultDHPSFU.INTENSITY]);
					}
					PeakResultDHPSFU r = new PeakResultDHPSFU((int) frame[i], parameters[2], parameters[3],
							parameters[1], parameters[4]);
					AttributePeakResult ap = new AttributePeakResult(r);
					if (precision != null) {
						ap.setPrecision(precision[i]);
					}
					results.add(ap);
				}
			}
			results.end();
			results.sort();
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
			return results;

		} else if (fileType == "DHPSFU") {
			try {
				data = importer.readCSVDouble(Paths.get(dataPath), skipLines);
			} catch (IOException e) {
				e.printStackTrace();
			}
			final String msg = "Data is " + TextUtils.pleural(data != null ? 1 : 0, "");
			IJ.showStatus(msg);
			ImageJUtils.log(msg);
			ImageJUtils.log(fileType);
			ImageJUtils.log(TextUtils.pleural(skipLines, " lines"));

			final int frameIndex = fileIndex.getFrameIndex();
			final int xIndex = fileIndex.getxIndex();
			final int yIndex = fileIndex.getyIndex();
			final int zIndex = fileIndex.getzIndex();
			final int intensityIndex = fileIndex.getIntensityIndex();

			double[] frame = null;
			if (frameIndex != -1) {
				frame = ArrayUtils.getColumn(data, frameIndex);
			}
			double[] x = null;
			if (xIndex != -1) {
				x = ArrayUtils.getColumn(data, xIndex);
			}
			double[] y = null;
			if (yIndex != -1) {
				y = ArrayUtils.getColumn(data, yIndex);
			}
			double[] z = null;
			if (zIndex != -1) {
				z = ArrayUtils.getColumn(data, zIndex);
			}
			double[] intensity = null;
			if (intensityIndex != -1) {
				intensity = ArrayUtils.getColumn(data, intensityIndex);
			}
			if (data != null) {
				results.begin();
				for (int i = 0; i < frame.length; i++) {
					float[] parameters = new float[7];
					if (frame != null) {
						parameters[PeakResultDHPSFU.BACKGROUND] = (float) frame[i];
					}
					if (x != null) {
						parameters[PeakResultDHPSFU.X] = (float) (x[i] / pxSize);
					}
					if (y != null) {
						parameters[PeakResultDHPSFU.Y] = (float) (y[i] / pxSize);
					}
					if (intensity != null) {
						parameters[PeakResultDHPSFU.INTENSITY] = (float) intensity[i];
					}
					PeakResult r = new PeakResult((int) frame[i], parameters[2], parameters[3], parameters[1]);
					// AttributePeakResult ap = new AttributePeakResult(r);
					if (z != null) {
						r.setZPosition((float) (z[i] / pxSize));
					}
					results.add(r);
				}
			}
			results.end();
			results.sort();
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
			return results;
		}
		return results;
	}

	private void Load() {
		MemoryPeakResults localisations = new MemoryPeakResults();
		if (ifManualIndex == false) {
			localisations = LoadLocalisations(dataPath, fileIndex, savingFormat, skipLines);
		} else {
			localisations = LoadLocalisationsManual(dataPath, fileIndex, fileType, skipLines);
		}
		if (localisations == null) {
			// Cancelled
			return;
		}
		if (localisations.isEmpty()) {
			IJ.error(TITLE, "No localisations could be loaded");
			return;
		}
		if (localisations.size() > 0) {
			MemoryPeakResults.addResults(localisations);
		}
		CalibrationWriter cw = localisations.getCalibrationWriterSafe();
		cw.setNmPerPixel(pxSize);
		cw.setDistanceUnit(DistanceUnit.PIXEL);
		localisations.setCalibration(cw.getCalibration());
		final String msg = "Loaded " + TextUtils.pleural(localisations.size(), "localisation");
		IJ.showStatus(msg);
		ImageJUtils.log(msg);
		viewLocalisations(localisations, fileType);
	}
}
