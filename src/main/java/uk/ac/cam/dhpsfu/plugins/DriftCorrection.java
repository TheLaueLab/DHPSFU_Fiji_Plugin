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
import ij.ImagePlus;
import ij.WindowManager;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import ij.plugin.frame.Recorder;
import ij.process.ImageProcessor;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.linear.Array2DRowFieldMatrix;
import org.apache.commons.math3.linear.FieldMatrix;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.TickUnits;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.LookupPaintScale;
import org.jfree.chart.title.PaintScaleLegend;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.RectangleInsets;
import org.jtransforms.fft.DoubleFFT_2D;
import uk.ac.cam.dhpsfu.analysis.*;
import uk.ac.sussex.gdsc.core.ij.gui.ExtendedGenericDialog;
import uk.ac.sussex.gdsc.core.utils.SimpleArrayUtils;
import uk.ac.sussex.gdsc.smlm.data.config.CalibrationWriter;
import uk.ac.sussex.gdsc.smlm.data.config.CalibrationProtos.CameraType;
import uk.ac.sussex.gdsc.smlm.data.config.UnitProtos.DistanceUnit;
import uk.ac.sussex.gdsc.smlm.data.config.UnitProtos.IntensityUnit;
import uk.ac.sussex.gdsc.smlm.data.config.UnitProtos.TimeUnit;
import uk.ac.sussex.gdsc.smlm.ij.plugins.ResultsManager;

import java.awt.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Vector;
import java.util.prefs.Preferences;

import uk.ac.sussex.gdsc.smlm.results.MemoryPeakResults;
import uk.ac.sussex.gdsc.smlm.results.PeakResult;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
//import java.awt.image.BufferedImage;
import java.util.stream.Collectors;
import ij.ImageStack;
import ij.Macro;
import ij.process.FloatProcessor;

public class DriftCorrection implements PlugIn {
	private static final String TITLE = "Drift Correction";
	// private static boolean memory3D = true;
	private static boolean memoryWL = false;

	private double px_size;
	private double upFactor;
	private double burst;
	private double cycle;
	private static String WL_path;
	private static String threed_path;
	private static String saving_directory = "";

	// private static String threedName = "";
	private static String selectedTitle = "";
	private static String average_drift = "";

	private static String threed_memory = ""; // the name that you want to store in the fiji memory
	public static DC_GeneralParas dc_generalParas = new DC_GeneralParas(210, 100, 20, 500);
	public static DC_Paras dc_paras = new DC_Paras(false, false, false, false, average_drift, false, true);
	Boolean WL_oca = dc_paras.getWl_occasional();
	Boolean DCtwice = dc_paras.getCorrectionTwice();
	Boolean saveDCIm = dc_paras.getSave_DC_WL();

	private static String savePath;
	private String savingFormat = ".3d";

	// Extra options
	private boolean saveToFile;

	private boolean showDialog(String arg) {
		if (arg != null) {
			parseArguments(arg);
		}

		final ExtendedGenericDialog gd = new ExtendedGenericDialog(TITLE);
		gd.addMessage("Load .3d file from ImageJ memory:");
		// gd.addCheckbox("Load .3d file from ImageJ memory", memory3D);
		// final Checkbox load_3D_file = gd.getLastCheckbox();
		// memory3D = true;
		Preferences.userNodeForPackage(DriftCorrection.class);
//		// 3d file input
		ResultManager.addInput(gd, "--Please Select--", ResultManager.InputSource.MEMORY);

		// gd.addMessage("select .3d File from File Directory:");
		// gd.addFilenameField(".3d_File_directory", "--Please Select--");
		// gd.addStringField("File_name", "3D");
		// white light image input
		gd.addCheckbox("Load Ref image from opened window", memoryWL);

		String[] imageTitles = WindowManager.getImageTitles();

		gd.addChoice("Select opened image", imageTitles, "--Please_Select--");

		gd.addMessage("Select Ref image from File Directory ");
		gd.addFilenameField(".tif_File_directory", "--Please_Select--");
		// parameters
		gd.addCheckbox("Ref occasional", dc_paras.wl_occasional);
		gd.addCheckbox("Drift correction twice", dc_paras.correction_twice);
		// Checkbox flip_x = gd.addAndGetCheckbox("Flip FOV in x ", dc_paras.flip_x);
		// Checkbox flip_y = gd.addAndGetCheckbox("Flip FOV in y", dc_paras.flip_y);
		String[] formats = { "Average_drift", "Average_image" };
		gd.addChoice("Drift averaging", formats, formats[1]);
		// Checkbox group_burst = gd.addAndGetCheckbox("Group burst",
		// dc_paras.group_burst);
		gd.addNumericField("Pixel size (nm)", dc_generalParas.px_size, 1);
		gd.addNumericField("Upsampling factor", dc_generalParas.upFactor);
		gd.addNumericField("No. of Ref frames in each burst", dc_generalParas.burst);
		gd.addNumericField("No. of SR frames in each cycle", dc_generalParas.cycle);
		gd.addMessage("File Output:");
		gd.addCheckbox("Save drift corrected WL images", dc_paras.save_DC_WL);
		gd.addDirectoryField("Ref save directory", "--Please_Select--");
		gd.addCheckbox("Saving data", saveToFile);
		gd.addDirectoryField("Data save directory", "--Please_Select--");
		String[] formats2 = { ".3d", ".csv" };
		gd.addChoice("Saving_format", formats2, formats2[0]);
		String html = "<html>" + "<h2>Instruction about Drift Correction Plugin</h2>"
				+ "Load the localisation file for drift correction from FIJI memory. <br>" + "<br>"
				+ "If \\\"Load Ref image from opened window\\\" is ticked: <br>"
				+ "- The program will use the image saved in the \\\"Select opened image\\\" field.  <br>"
				+ "- Else it will use the image selected in \\\".tif file directory\\\". <br>" + "<br>"
				+ "Once you select \\\"OK\\\", it will takes 10-20 seconds to calculate the result. <br>"
				+ "(* Program running time depends on your input data size *) <br>" + "<br>" + "Parameters:  <br>"
				+ "- Reference occasional:  False means that the reference and the SR images were acquired in parallel, i.e. they have a one-to-one correspondence. True means that the reference images were acquired occasionally, every N SR frames.  <br>"
				+ "- Drift correction twice: True means that the reference images were taken at the beginning and end of each SR burst, i.e. ref-SR-ref, ref-SR-ref, …; False means that only one reference image was acquired per SR burst, i.e. ref-SR, ref-SR, ...   <br>"
				// + "- Flip FOV in x/y: True means WL-SR-WL, WL-SR-WL, ... False means WL-SR,
				// WL-SR, ... <br>"
				+ "- Drift averaging: If each time, multiple reference image frames were taken, the accuracy of drift estimation can be increased by averaging in two ways.   <br>"
				+ "** Average drift: Calculating the drift for each frame independently, and then averaging the values;   <br>"
				+ "** Average image: Constructing an average projection from all the frames and using that to calculate drift.   <br>"
				+ "- Pixel size (nm): Camera pixel size in nm. <br>"
				+ "- Upsampling factor: upsampling factor for cross correlation. Default is 100. <br>"
				+ "- No. of Ref frames in each burst: Number of reference frames taken in each burst. <br>"
				+ "- No. of SR frames in each cycle: Number of SR frames taken in each cycle. <br>" + "<br>"
				+ "File output:  <br>"
				+ "- Save drift-corrected Ref images: Save the drift corrected reference images in a TIFF stack.  <br>"
				+ "- Save directory: Directory for the drift corrected reference stack.  <br>" + "<br>" + "</font>";
		gd.addHelp(html);
		gd.showDialog();
		if (gd.wasCanceled()) {
			return false;
		}

		threed_memory = ResultsManager.getInputSource(gd);
		// IJ.log("3dfile=" + threed_memory);

		if (arg == null || arg.length() == 0) {
			memoryWL = gd.getNextBoolean();
			selectedTitle = gd.getNextChoice();
			WL_path = gd.getNextString();
			WL_oca = gd.getNextBoolean();
			DCtwice = gd.getNextBoolean();
			average_drift = gd.getNextChoice();
			px_size = gd.getNextNumber();
			upFactor = gd.getNextNumber();
			burst = gd.getNextNumber();
			cycle = gd.getNextNumber();
			saveDCIm = gd.getNextBoolean();
			saving_directory = gd.getNextString();
			saveToFile = gd.getNextBoolean();
			savePath = gd.getNextString();
			savingFormat = gd.getNextChoice();

		} else {
			Vector<?> numericFields = gd.getNumericFields();
			if (numericFields != null && numericFields.size() >= 4) {
				px_size = getNumericFieldValue(numericFields, 0);
				upFactor = getNumericFieldValue(numericFields, 1);
				burst = getNumericFieldValue(numericFields, 2);
				cycle = getNumericFieldValue(numericFields, 3);
			}

			Vector<?> choiceFields = gd.getChoices();
			if (choiceFields != null && choiceFields.size() >= 4) {
				selectedTitle = getChoiceFieldValue(choiceFields, 1);
				average_drift = getChoiceFieldValue(choiceFields, 2);
				savingFormat = getChoiceFieldValue(choiceFields, 3);
			}

			// Parse checkbox fields
			Vector<?> checkboxFields = gd.getCheckboxes();
			if (checkboxFields != null && checkboxFields.size() >= 6) {
				memoryWL = getCheckboxFieldValue(checkboxFields, 1);
				WL_oca = getCheckboxFieldValue(checkboxFields, 2);
				DCtwice = getCheckboxFieldValue(checkboxFields, 3);
				saveDCIm = getCheckboxFieldValue(checkboxFields, 4);
				saveToFile = getCheckboxFieldValue(checkboxFields, 5);
			}

			// Parse string fields
			Vector<?> stringFields = gd.getStringFields();
			if (stringFields != null && stringFields.size() >= 4) {
				WL_path = getStringFieldValue(stringFields, 1);
				saving_directory = getStringFieldValue(stringFields, 2);
				savePath = getStringFieldValue(stringFields, 3);

			}
//			IJ.log("px_size=" + px_size);
//			IJ.log("upFactor=" + upFactor);
//			IJ.log("burst=" + burst);
//			IJ.log("cycle=" + cycle);
//			IJ.log("memoryWL=" + memoryWL);
//			IJ.log("Title=" + selectedTitle);
//			IJ.log("WL_path=" + WL_path);
//			IJ.log("WL_oca=" + WL_oca);
//			IJ.log("DCtwice=" + DCtwice);
//			IJ.log("average_drift=" + average_drift);
//			IJ.log("saveDCIm=" + saveDCIm);
//			IJ.log("saving_directory=" + saving_directory);
//			IJ.log("saveToFile=" + saveToFile);
//			IJ.log("savePath=" + savePath);
//			IJ.log("savingFormat=" + savingFormat);

		}

		// update parameters
		dc_paras.setWl_occasional(WL_oca);
		dc_paras.setCorrection_twice(DCtwice);
		// dc_paras.setFlip_x(flip_x.getState());
		// dc_paras.setFlip_y(flip_y.getState());
		dc_paras.setAverage_drift(average_drift);
		dc_paras.setSave_DC_WL(saveDCIm);
		dc_generalParas.setPxSize(px_size);
		dc_generalParas.setUpFactor(upFactor);
		dc_generalParas.setBurst(burst);
		dc_generalParas.setCycle(cycle);

		StringBuilder command = new StringBuilder();
		command.append("run(\"Drift Correction\", ");
		command.append("\"Data_input=").append(threed_memory).append(" ");
		command.append("Load_WL=").append(memoryWL).append(" ");
		command.append("WL_input=").append(selectedTitle).append(" ");
		command.append("WL_directory=").append(WL_path).append(" ");

		command.append("WL_occasional=").append(WL_oca).append(" ");
		command.append("DCtwice=").append(DCtwice).append(" ");
		command.append("Average_drift=").append(average_drift).append(" ");
		command.append("Pixel_size=").append(px_size).append(" ");
		command.append("Up_factor=").append(upFactor).append(" ");
		command.append("WL_Burst=").append(burst).append(" ");
		command.append("SR_Burst=").append(cycle).append(" ");
		command.append("Save_DC_image=").append(saveDCIm).append(" ");
		command.append("Ref_directory=").append(saving_directory).append(" ");
		command.append("Save_to_file=").append(saveToFile).append(" ");
		command.append("Save_directory=").append(savePath).append(" ");
		command.append("Save_format=").append(savingFormat).append("\");");

		if (Recorder.record) {
			Recorder.recordString(command.toString());
		}

		return true;
	}

	private void parseArguments(String arg) {
		String[] params = arg.split(" ");
		for (String param : params) {
			String[] keyVal = param.split("=");
			if (keyVal.length == 2) {
				switch (keyVal[0]) {
				case "Data_input":
					threed_memory = keyVal[1];
					break;
				case "Load_WL":
					memoryWL = Boolean.parseBoolean(keyVal[1]);
					break;
				case "WL_input":
					selectedTitle = keyVal[1];
					break;
				case "WL_directory":
					WL_path = keyVal[1];
					break;
				case "WL_occasional":
					WL_oca = Boolean.parseBoolean(keyVal[1]);
					break;
				case "DCtwice":
					DCtwice = Boolean.parseBoolean(keyVal[1]);
					break;
				case "Average_drift":
					average_drift = keyVal[1];
					break;
				case "Pixel_size=":
					px_size = Double.parseDouble(keyVal[1]);
					break;
				case "Up_factor":
					upFactor = Double.parseDouble(keyVal[1]);
					break;
				case "WL_Burst":
					burst = Double.parseDouble(keyVal[1]);
					break;
				case "SR_Burst":
					cycle = Double.parseDouble(keyVal[1]);
					break;
				case "Save_DC_image":
					saveDCIm = Boolean.parseBoolean(keyVal[1]);
					break;
				case "Ref_directory":
					saving_directory = keyVal[1];
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
			//IJ.log("Set " + keyVal[0] + " to " + keyVal[1]);
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

	private static String getChoiceFieldValue(Vector<?> fields, int index) {
		return ((java.awt.Choice) fields.get(index)).getSelectedItem();
	}

	private static boolean getCheckboxFieldValue(Vector<?> fields, int index) {
		return ((java.awt.Checkbox) fields.get(index)).getState();
	}

	private static String getStringFieldValue(Vector<?> fields, int index) {
		return ((java.awt.TextField) fields.get(index)).getText();
	}

	@Override
	public void run(String arg) {
		String macroOptions = Macro.getOptions();
		if (showDialog(macroOptions)) {
			 
			if (saving_directory.equals("--Please_Select--") && dc_paras.save_DC_WL) {
				IJ.log("Save wl corr is " + dc_paras.save_DC_WL);
				IJ.error("Please select a place to save your corrected white light image ");
				return;
				// } else if (!memory3D && threed_path.equals("--Please Select--")) {
				// IJ.error("Please select a 3d file");
				// return;
			} else if (memoryWL && !WL_path.equals("--Please_Select--")) {
				// IJ.log("WL " + String.valueOf(memoryWL));
				IJ.error("Please select an opened image");
				return;
			} else {
				// load();
				long startTime = System.currentTimeMillis();
				DCresult dCresult = drift_correction(threed_path, threed_memory, selectedTitle, WL_path,
						dc_generalParas, dc_paras, px_size);

				ArrayList<double[]> result = dCresult.getData_corrected();
				// MemoryPeakResults r = loadResult(arrayListToArray(result), threed_memory);
				double[][] arrayResult = arrayListToArray(result);
				MemoryPeakResults dcFinalResult = saveToMemory(arrayResult, threed_memory, px_size);
				MemoryPeakResults.addResults(dcFinalResult);

				// int num = MemoryPeakResults.countMemorySize();
				// IJ.log("int" + String.valueOf(num));

				ArrayList<ArrayList<Double>> dbf = dCresult.getDrift_by_frame();
				dCresult.getDrift_by_loc();

				// IJ.log(threed_memory+"_Drift_by_Frame.csv");

				// saveLocData(dCresult.getDrift_by_loc(), savePath, dcFinalResult,
				// threed_memory);

				// Save files
				if (saveToFile == true) {
					IJ.log("saveToFile is " + saveToFile);
					saveData(dbf, savePath, dcFinalResult, threed_memory);
					List<List<Double>> doubleList = memoryPeakResultToDoubleList(dcFinalResult);
					saveTo3D(doubleList, threed_memory, savingFormat, dcFinalResult);
				}
				viewLoadedResult(dcFinalResult, "Drift corrected result");
				long endTime = System.currentTimeMillis();
				long duration = endTime - startTime;
				double seconds = (double) duration / 1000.0;
				IJ.log("Drift correction runtime: " + seconds + " seconds");
				JFreeChart chart = createChartPanel(dbf);
				JFrame frame = new JFrame();
				frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
				JButton saveButton = new JButton("Save Chart");
				saveButton.addActionListener(e -> saveChart(chart));
				frame.add(saveButton, BorderLayout.SOUTH);
				ChartPanel panel = new ChartPanel(chart);
				frame.add(panel, BorderLayout.CENTER);
				frame.setSize(800, 600);
				frame.pack();
				frame.setVisible(true);
				if (dc_paras.save_DC_WL) {
					try {
						IJ.log(" ");
						IJ.log("--- Saving corrected white light image ---");
						align_wl(WL_path, selectedTitle, dc_paras, dc_generalParas);
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
				IJ.log(" ");
				IJ.log("--- Algorithm finish executing ---");
				IJ.log(" ");
			}
		}
	}

	private List<List<Double>> memoryPeakResultToDoubleList(MemoryPeakResults r) {
		List<List<Double>> doubleList = new ArrayList<>();
		int size = r.size();
		double[] frame = new double[size];
		double[] x = new double[size];
		double[] y = new double[size];
		double[] z = new double[size];
		double[] intensity = new double[size];
		for (int i = 0; i < size; i++) {
			frame[i] = r.get(i).getFrame();
			x[i] = r.get(i).getXPosition();
			y[i] = r.get(i).getYPosition();
			z[i] = r.get(i).getZPosition();
			intensity[i] = r.get(i).getIntensity();
			List<Double> sublist = new ArrayList<>();
			sublist.add(x[i]);
			sublist.add(y[i]);
			sublist.add(z[i]);
			sublist.add(intensity[i]);
			sublist.add(frame[i]);
			doubleList.add(sublist);
		}
		return doubleList;
	}

	// Save the final filtered result to .3D file
	private void saveTo3D(List<List<Double>> filteredPeakResult, String threed_memory, String savingFormat,
			MemoryPeakResults r) {
		// r.setName(threed_memory);
		String outputFilename = r.getName();
		Path outputPath;
		if (savingFormat == ".3d") {
			outputPath = Paths.get(savePath, outputFilename + savingFormat);
			try (BufferedWriter writer = Files.newBufferedWriter(outputPath)) {
				for (List<Double> row : filteredPeakResult) {
					String csvRow = row.stream().map(Object::toString).collect(Collectors.joining("\t"));
					writer.write(csvRow);
					writer.newLine();
				}
			} catch (IOException e) {
				System.err.println("Error writing to file: " + threed_memory);
				e.printStackTrace();
			}
		} else {
			outputPath = Paths.get(savePath, outputFilename + savingFormat);
			try (BufferedWriter writer = Files.newBufferedWriter(outputPath)) {
				for (List<Double> row : filteredPeakResult) {
					String csvRow = row.stream().map(Object::toString).collect(Collectors.joining(","));
					writer.write(csvRow);
					writer.newLine();
				}
			} catch (IOException e) {
				System.err.println("Error writing to file: " + threed_memory);
				e.printStackTrace();
			}
		}
	} // End of saveTo3D

	public static void writedbf(ArrayList<ArrayList<Double>> a, String filename) {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
			for (int i = 0; i < a.size(); i++) {
				writer.write(a.get(i).get(0) + "," + a.get(i).get(1));
				writer.newLine();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void saveChart(JFreeChart chart) {
		JFileChooser fileChooser = new JFileChooser();
		FileNameExtensionFilter pngFilter = new FileNameExtensionFilter("PNG Images", "png");
		FileNameExtensionFilter jpegFilter = new FileNameExtensionFilter("JPEG Images", "jpeg", "jpg");
		fileChooser.addChoosableFileFilter(pngFilter);
		fileChooser.addChoosableFileFilter(jpegFilter);
		fileChooser.setFileFilter(pngFilter);
		int userSelection = fileChooser.showSaveDialog(null);
		if (userSelection == JFileChooser.APPROVE_OPTION) {
			File fileToSave = fileChooser.getSelectedFile();
			String filePath = fileToSave.getAbsolutePath();
			String ext = ((FileNameExtensionFilter) fileChooser.getFileFilter()).getExtensions()[0];
			try {
				if ("png".equals(ext)) {
					ChartUtils.saveChartAsPNG(new File(filePath + ".png"), chart, 600, 400);
				} else if ("jpeg".equals(ext) || "jpg".equals(ext)) {
					ChartUtils.saveChartAsJPEG(new File(filePath + ".jpg"), chart, 600, 400);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

//	private static void writeCorrected(ArrayList<double[]> a, String filename) {
//		try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
//			for (int i = 0; i < a.size(); i++) {
//				writer.write(
//						a.get(i)[0] + "," + a.get(i)[1] + "," + a.get(i)[2] + "," + a.get(i)[3] + "," + a.get(i)[4]);
//				writer.newLine();
//			}
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
//
//	private static void writedbl(double[][] a, String filename) {
//		try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
//			for (int i = 0; i < a.length; i++) {
//				writer.write(a[i][0] + "," + a[i][1]);
//				writer.newLine();
//			}
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}

	private MemoryPeakResults saveToMemory(double[][] arr, String threed_memory, double px_size) {

		// results.getName();
		// double[][] returnArray = null;
		// IJ.log(String.valueOf("arr lengh " +arr.length));

		double[] x = ArrayUtils.getColumn(arr, 0);
		double[] y = ArrayUtils.getColumn(arr, 1);
		double[] z = ArrayUtils.getColumn(arr, 2);
		double[] intensity = ArrayUtils.getColumn(arr, 3);
		double[] frame = ArrayUtils.getColumn(arr, 4);
		// returnArray = new double[][] { x, y, z, intensity, frame };
		// results.begin();
		MemoryPeakResults result = new MemoryPeakResults();

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
			result.add(r);
		}
		// results.begin();
//		for (int i = 0; i < arr.length; i++) {
//			float[] parameters = new float[7];
//			parameters[0] = (float) x[i];
//			parameters[1] = (float) y[i];
//			parameters[2] = (float) z[i];
//			parameters[3] = (float) intensity[i];
//			parameters[4] = (float) frame[i];
//			PeakResult r = new PeakResult((int) parameters[4], parameters[0], parameters[1], parameters[3]);
//			r.setZPosition(parameters[2]);
//			//AttributePeakResult ap = new AttributePeakResult(r);
//			results.add(r);
//		}
		result.end();
		result.sort();
		// String name = threed_memory +"_DC";
		String name = threed_memory + "_DC";
		result.setName(name);
		CalibrationWriter cw = new CalibrationWriter();
		cw.setIntensityUnit(IntensityUnit.PHOTON);
		cw.setDistanceUnit(DistanceUnit.NM);
		cw.setTimeUnit(TimeUnit.FRAME);
		cw.setExposureTime(30);
		cw.setNmPerPixel(px_size);
		cw.setCountPerPhoton(45);
		cw.getBuilder().getCameraCalibrationBuilder().setCameraType(CameraType.EMCCD).setBias(100)
				.setQuantumEfficiency(0.95).setReadNoise(1.6);
		result.setCalibration(cw.getCalibration());

		return result;
	}

	/**
	 * load the .3d file and add the loaded data into imageJ memory
	 * 
	 * @param filePath the .3d file path
	 * @param fileName the name of the loaded data which stores in imageJ memory
	 * @return
	 */
//	private static LoadedResult load3DFile(String filePath) {
//		Read3DFileCalib importer = new Read3DFileCalib();
//		double[][] data = null;
//		String fileExtension = getFileExtension(filePath);
//		if (!fileExtension.equals(".3d")) {
//			IJ.error(TITLE, "You must select a .3d file");
//		} else {
//			try {
//				data = importer.readCSVDouble(Paths.get(filePath), 0);
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//		}
//		return loadResult(data, threed_memory);
//	}

	// load the 3d file and show the data using result table
//	private static void load() {
//		// MemoryPeakResults threed_result;
//		// if (!memory3D) {
//		// threed_result = load3DFile(threed_path, threed_memory).getResults();
//		// } else {
//		MemoryPeakResults threed_result = ResultsManager.loadInputResults(threed_memory, true, DistanceUnit.PIXEL,
//				IntensityUnit.PHOTON);
//		// }
//		if (threed_result == null) {
//			return;
//		}
//		if (threed_result.isEmpty()) {
//			IJ.error(TITLE, "No localisations could be loaded");
//			return;
//		}
////		if (threed_result.size() > 0) {
////			MemoryPeakResults.addResults(threed_result);
////		}
//		IJ.log(" ");
//		final String msg = "Loaded " + TextUtils.pleural(threed_result.size(), "lines");
//		IJ.log(" ");
//		IJ.showStatus(msg);
//		ImageJUtils.log(msg);
//		viewLoadedResult(threed_result, "Loaded_3d_file");
//	}

	private static double[][] resultToArray(MemoryPeakResults results, double px_size) {
		if (MemoryPeakResults.isEmpty(results)) {
			IJ.error(TITLE, "No results could be loaded");
		}
		int size = results.size();
		double[][] r = new double[5][size];

		if (results.getDistanceUnit() == DistanceUnit.PIXEL) {
			for (int i = 0; i < size; i++) {
				r[0][i] = px_size * results.get(i).getXPosition();
				r[1][i] = px_size * results.get(i).getYPosition();
				r[2][i] = px_size * results.get(i).getZPosition();
				r[3][i] = results.get(i).getIntensity();
				r[4][i] = results.get(i).getFrame();
			}
		} else if (results.getDistanceUnit() == DistanceUnit.NM) {
			for (int i = 0; i < size; i++) {
				r[0][i] = results.get(i).getXPosition();
				r[1][i] = results.get(i).getYPosition();
				r[2][i] = results.get(i).getZPosition();
				r[3][i] = results.get(i).getIntensity();
				r[4][i] = results.get(i).getFrame();
			}

		}
		return r;
	}

	private static void viewLoadedResult(MemoryPeakResults results, String windowTitle) {
		if (MemoryPeakResults.isEmpty(results)) {
			IJ.error(TITLE, "No results could be loaded");
			return;
		}
		int size = results.size();
		int[] frame = new int[size];
		float[] x = new float[size];
		float[] y = new float[size];
		float[] z = new float[size];
		float[] intensity = new float[size];
		for (int i = 0; i < size; i++) {
			frame[i] = results.get(i).getFrame();
			x[i] = results.get(i).getXPosition();
			y[i] = results.get(i).getYPosition();
			z[i] = results.get(i).getZPosition();
			intensity[i] = results.get(i).getIntensity();
		}
		ResultsTable t = new ResultsTable();
		t.setValues("X", SimpleArrayUtils.toDouble(x));
		t.setValues("Y", SimpleArrayUtils.toDouble(y));
		t.setValues("Z", SimpleArrayUtils.toDouble(z));
		t.setValues("Intensity", SimpleArrayUtils.toDouble(intensity));
		t.setValues("Frame", SimpleArrayUtils.toDouble(frame));
		// t.setValues("Frame", new double[] { 1, 2, 3, 4, 5 });
		t.show(windowTitle);
	}

//    private  static void  checkResult(ArrayList<ArrayList<Double>> r,String title){
//        ResultsTable t = new ResultsTable();
//        double[] x = new double[r.size()];
//        double[] y = new double[r.size()];
//        for(int i =0 ;i < r.size(); i++){
//            x[i] = r.get(i).get(0);
//            y[i] = r.get(i).get(1);
//        }
//        t.setValues("X",x);
//        t.setValues("Y",y);
//        t.setPrecision(8);
//        t.show(title);
//    }
//    private  static void  checkResult(double[][] r,String title){
//        ResultsTable t = new ResultsTable();
//        double[] x = new double[r.length];
//        double[] y = new double[r.length];
//        for(int i =0 ;i < r.length; i++){
//            x[i] = r[i][0];
//            y[i] = r[i][1];
//        }
//        t.setValues("X",x);
//        t.setValues("Y",y);
//        t.setPrecision(8);
//        t.show(title);
//    }
	private static String getFileExtension(String filePath) {
		int lastIndex = filePath.lastIndexOf(".");
		if (lastIndex == -1) {
			return ""; // empty extension
		}
		return filePath.substring(lastIndex);
	}

	private static int[][][] loadtifFile(String wl_fpath, boolean window) {
		ImagePlus imp;
		if (!getFileExtension(wl_fpath).equals(".tif")) {
			IJ.error(TITLE, "You must select a .tif file");
		}
		if (window) {
			imp = WindowManager.getImage(wl_fpath);
		} else {
			imp = new ImagePlus(wl_fpath);
		}
		int width = imp.getWidth();
		int height = imp.getHeight();
		int slices = imp.getStackSize();
		int[][][] array3D = new int[slices][width][height];
		for (int slice = 1; slice <= slices; slice++) {
			ImageProcessor ip = imp.getStack().getProcessor(slice);
			for (int x = 0; x < width; x++) {
				for (int y = 0; y < height; y++) {
					array3D[slice - 1][y][x] = ip.getPixel(x, y);
				}
			}
		}
		return array3D;
	}

	private static Complex[][] ftPad(Complex[][] imFT, int[] outsize) {
		int row = imFT.length;
		int col = imFT[0].length;
		int[] nin = { row, col };
		// fft shift for imFT
		imFT = Fftshift.fftShift2D(imFT);
		int[] center = { imFT.length / 2, imFT[0].length / 2 };
		// initialise imFTout, each element in this array is zero
		Complex[][] imFTout = new Complex[outsize[0]][outsize[1]];
		for (int i = 0; i < imFTout.length; i++) {
			for (int j = 0; j < imFTout[0].length; j++) {
				imFTout[i][j] = new Complex(0, 0);
			}
		}
		int[] centerOut = { imFTout.length / 2, imFTout[0].length / 2 };

		int[] cenout_cen = new int[centerOut.length];
		for (int i = 0; i < cenout_cen.length; i++) {
			cenout_cen[i] = centerOut[i] - center[i];
		}
		int im_max1 = Math.max(cenout_cen[0] + 1, 1) - 1;
		int im_min1 = Math.min(cenout_cen[0] + nin[0], outsize[0]);

		int im_max2 = Math.max(cenout_cen[1] + 1, 1) - 1;
		int im_min2 = Math.min(cenout_cen[1] + nin[1], outsize[1]);

		int im_max3 = Math.max(-cenout_cen[0] + 1, 1) - 1;
		Math.min(-cenout_cen[0] + outsize[0], nin[0]);

		int im_max4 = Math.max(-cenout_cen[1] + 1, 1) - 1;
		Math.min(-cenout_cen[1] + outsize[1], nin[1]);

		// copy the relevant region from imFT to imFTout
		for (int i = im_max1; i < im_min1; i++) {
			for (int j = im_max2; j < im_min2; j++) {
				imFTout[i][j] = imFT[im_max3 + (i - im_max1)][im_max4 + (j - im_max2)];
			}
		}
		// perform inverse shift and scale
		imFTout = Fftshift.ifftShift2D(imFTout);
		for (int i = 0; i < imFTout.length; i++) {
			for (int j = 0; j < imFTout[0].length; j++) {
				imFTout[i][j] = imFTout[i][j].multiply(outsize[0] * outsize[1]).divide((nin[0] * nin[1]));
			}
		}
		return imFTout;
	}

	private static Complex[][] dftups(Complex[][] inn, int nor, int noc, int usfac, int roff, int coff) {
		int nr = inn.length;
		int nc = inn[0].length;
		double[] fftpara = new double[nc];
		for (int i = 0; i < nc; i++) {
			fftpara[i] = i;
		}
		fftpara = Fftshift.ifftShift1D(fftpara);
		Complex[] kernc_1 = new Complex[nc];
		for (int i = 0; i < nc; i++) {
			kernc_1[i] = new Complex(0, -2 * Math.PI / (nc * usfac)).multiply(fftpara[i] - Math.floor((double) nc / 2));
		}
		// reshape kernc_1
		Complex[][] reshaped_kernc_1 = new Complex[1][kernc_1.length];
		System.arraycopy(kernc_1, 0, reshaped_kernc_1[0], 0, kernc_1.length);

		double[] kernc_2 = new double[noc];
		for (int i = 0; i < noc; i++) {
			kernc_2[i] = i - coff;
		}
		// reshape kernc_2
		double[][] reshaped_kernc_2 = reshape(kernc_2.length, 1, kernc_2);
		// multiplication between Complex and double
		int rows = reshaped_kernc_2.length; // Assuming 2D array
		int cols = reshaped_kernc_1[0].length; // Assuming non-empty 2D array
		Complex[][] multiResult = new Complex[rows][cols];
		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < cols; j++) {
				Complex sum = Complex.ZERO;
				for (int k = 0; k < reshaped_kernc_2[0].length; k++) {
					sum = sum.add(new Complex(reshaped_kernc_2[i][k], 0).multiply(reshaped_kernc_1[k][j]));
				}
				multiResult[i][j] = sum;
			}
		}
		FieldMatrix<Complex> result = new Array2DRowFieldMatrix<Complex>(multiResult);
		for (int i = 0; i < result.getRowDimension(); i++) {
			for (int j = 0; j < result.getColumnDimension(); j++) {
				Complex value = result.getEntry(i, j);
				double realPart = value.getReal();
				double imaginaryPart = value.getImaginary();
				double expRealPart = Math.exp(realPart);
				Complex expComplex = new Complex(expRealPart * Math.cos(imaginaryPart),
						expRealPart * Math.sin(imaginaryPart));
				result.setEntry(i, j, expComplex);
			}
		}
		FieldMatrix<Complex> kerncM = result.transpose();
		Complex[][] kernc = matrixToArray(kerncM);

		// kernr_1
		double[] arange0nor = new double[nor];
		for (int i = 0; i < nor; i++) {
			arange0nor[i] = i;
		}
		Complex[] kernr_1 = new Complex[nor];
		for (int i = 0; i < nor; i++) {
			kernr_1[i] = new Complex(0, -2 * Math.PI / (nr * usfac)).multiply(arange0nor[i] - roff);
		}
		// reshape kernr_1
		Complex[][] reshaped_kernr_1 = new Complex[1][kernr_1.length];
		System.arraycopy(kernr_1, 0, reshaped_kernr_1[0], 0, kernr_1.length);

		// kernr_2
		double[] arange0nr = new double[nr];
		for (int i = 0; i < nr; i++) {
			arange0nr[i] = i;
		}
		arange0nr = Fftshift.ifftShift1D(arange0nr);
		double[] kernr_2 = new double[nr];
		for (int i = 0; i < nr; i++) {
			kernr_2[i] = arange0nr[i] - Math.floor((double) nr / 2);
		}
		// reshape kernr_2
		double[][] reshaped_kernr_2 = reshape(kernr_2.length, 1, kernr_2);

		// multiplication between Complex and double
		int nrrows = reshaped_kernr_2.length; // Assuming 2D array
		int nrcols = reshaped_kernr_1[0].length; // Assuming non-empty 2D array

		Complex[][] multiResultNr = new Complex[nrrows][nrcols];

		for (int i = 0; i < nrrows; i++) {
			for (int j = 0; j < nrcols; j++) {
				Complex sum = Complex.ZERO;
				for (int k = 0; k < reshaped_kernr_2[0].length; k++) {
					sum = sum.add(new Complex(reshaped_kernr_2[i][k], 0).multiply(reshaped_kernr_1[k][j]));
				}
				multiResultNr[i][j] = sum;
			}
		}
		FieldMatrix<Complex> nrresult = new Array2DRowFieldMatrix<Complex>(multiResultNr);
		// exp
		for (int i = 0; i < nrresult.getRowDimension(); i++) {
			for (int j = 0; j < nrresult.getColumnDimension(); j++) {
				Complex value = nrresult.getEntry(i, j);
				double realPart = value.getReal();
				double imaginaryPart = value.getImaginary();
				double expRealPart = Math.exp(realPart);
				Complex expComplex = new Complex(expRealPart * Math.cos(imaginaryPart),
						expRealPart * Math.sin(imaginaryPart));
				nrresult.setEntry(i, j, expComplex);
			}
		}
		// kernc
		FieldMatrix<Complex> kernrM = nrresult.transpose();
		Complex[][] kernr = matrixToArray(kernrM);
		int finalrow1 = kernr.length; // Assuming 2D array
		int finalcol1 = inn[0].length; // Assuming non-empty 2D array

		Complex[][] interResult = new Complex[finalrow1][finalcol1];

		for (int i = 0; i < finalrow1; i++) {
			for (int j = 0; j < finalcol1; j++) {
				Complex sum = Complex.ZERO;
				for (int k = 0; k < kernr[0].length; k++) {
					sum = sum.add((kernr[i][k]).multiply(inn[k][j]));
				}
				interResult[i][j] = sum;
			}
		}
//        long endTime = System.currentTimeMillis();
//        System.out.println("8: "+ (endTime-startTime));
		// dot(interResult,kernc)
		int r = interResult.length;
		int c = kernc[0].length;
//        startTime = System.currentTimeMillis();
		Complex[][] out = new Complex[r][c];
		for (int i = 0; i < r; i++) {
			for (int j = 0; j < c; j++) {
				Complex sum = Complex.ZERO;
				for (int k = 0; k < interResult[0].length; k++) {
					sum = sum.add(interResult[i][k].multiply(kernc[k][j]));
				}
				out[i][j] = sum;
			}
		}
		return out;
	}

	private static Complex[][] matrixToArray(FieldMatrix<Complex> matrix) {
		int rows = matrix.getRowDimension();
		int cols = matrix.getColumnDimension();
		Complex[][] array = new Complex[rows][cols];
		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < cols; j++) {
				array[i][j] = matrix.getEntry(i, j);
			}
		}
		return array;
	}

	private static Dftresult dftregistration(Complex[][] buf1ft, Complex[][] buf2ft, int usfac) {
		int nr = buf2ft.length;
		int nc = buf2ft[0].length;

		int start = (int) Math.floor(nr / 2.0) * -1;
		int stop = (int) Math.ceil(nr / 2.0);
		int[] Nr = arange(start, stop);
		Nr = Fftshift.ifftShift1D(Nr);
		start = (int) Math.floor(nc / 2.0) * -1;
		stop = (int) Math.ceil(nc / 2.0);
		int[] Nc = arange(start, stop);
		Nc = Fftshift.ifftShift1D(Nc);
		double row_shift = 0;
		double col_shift = 0;
		Complex ccMax = new Complex(0, 0);
		double ccmax = 0;
		if (usfac == 0) {
			ccMax = new Complex(0, 0);
			for (int i = 0; i < buf1ft.length; i++) {
				for (int j = 0; j < buf1ft[i].length; j++) {
					double r1 = buf2ft[i][j].getReal();
					double im1 = -buf2ft[i][j].getImaginary();
					double r2 = buf1ft[i][j].getReal();
					double im2 = buf1ft[i][j].getImaginary();
					// (r2,im2)*(r1,im1)
					Complex product = new Complex(r2 * r1 - im2 * im1, r2 * im1 + r1 * im2);
					ccMax = ccMax.add(product); // Add to the sum
				}
			}
			row_shift = 0;
			col_shift = 0;
		} else if (usfac == 1) {
			Complex[][] cc = new Complex[buf1ft.length][buf1ft[0].length];
			for (int i = 0; i < buf1ft.length; i++) {
				for (int j = 0; j < buf1ft[i].length; j++) {
					double r1 = buf2ft[i][j].getReal();
					double im1 = -buf2ft[i][j].getImaginary();
					double r2 = buf1ft[i][j].getReal();
					double im2 = buf1ft[i][j].getImaginary();
					// (r2,im2)*(r1,im1)
					cc[i][j] = new Complex(r2 * r1 - im2 * im1, r2 * im1 + r1 * im2);
				}
			}
			double[][] doubleComplex = convertToInterleaved(cc);
			DoubleFFT_2D fft2 = new DoubleFFT_2D(doubleComplex.length, doubleComplex[0].length / 2);
			fft2.complexInverse(doubleComplex, true);
			cc = convertToComplex(doubleComplex);

			double[][] ccAbs = absoluteValue(cc);
			double maxVal = Double.NEGATIVE_INFINITY;

			for (int i = 0; i < ccAbs.length; i++) {
				for (int j = 0; j < ccAbs[i].length; j++) {
					if (ccAbs[i][j] > maxVal) {
						maxVal = ccAbs[i][j];
						row_shift = i;
						col_shift = j;
					}
				}
			}
			ccMax = cc[(int) row_shift][(int) col_shift].multiply(nc * nr);
			row_shift = Nr[(int) row_shift];
			col_shift = Nc[(int) col_shift];

		} else if (usfac > 1) {

			Complex[][] interCC = new Complex[buf1ft.length][buf1ft[0].length];
			for (int i = 0; i < buf1ft.length; i++) {
				for (int j = 0; j < buf1ft[i].length; j++) {
//                    interCC[i][j] = buf1ft[i][j].multiply((buf2ft[i][j]).conjugate());
					double r1 = buf2ft[i][j].getReal();
					double im1 = -buf2ft[i][j].getImaginary();
					double r2 = buf1ft[i][j].getReal();
					double im2 = buf1ft[i][j].getImaginary();
					// (r2,im2)*(r1,im1)
					interCC[i][j] = new Complex(r2 * r1 - im2 * im1, r2 * im1 + r1 * im2);
				}
			}
			interCC = ftPad(interCC, new int[] { 2 * nr, 2 * nc });
			double[][] doubleComplex = convertToInterleaved(interCC);
			DoubleFFT_2D fft2 = new DoubleFFT_2D(doubleComplex.length, doubleComplex[0].length / 2);
			fft2.complexInverse(doubleComplex, true);
			interCC = convertToComplex(doubleComplex);
			double[][] cc = new double[interCC.length][interCC[0].length];
			for (int i = 0; i < interCC.length; i++) {
				for (int j = 0; j < interCC[0].length; j++) {
					cc[i][j] = interCC[i][j].getReal();
				}
			}
			double[][] ccAbs = new double[cc.length][cc[0].length];
			for (int i = 0; i < 2 * nr; i++) {
				for (int j = 0; j < 2 * nc; j++) {
					ccAbs[i][j] = Math.abs(cc[i][j]);
				}
			}
			double maxVal = Double.NEGATIVE_INFINITY;

			for (int i = 0; i < ccAbs.length; i++) {
				for (int j = 0; j < ccAbs[i].length; j++) {
					if (ccAbs[i][j] > maxVal) {
						maxVal = ccAbs[i][j];
						row_shift = i;
						col_shift = j;
					}
				}
			}

			ccmax = cc[(int) row_shift][(int) col_shift] * nr * nc;
			double[] Nr2 = Fftshift.ifftShift1D(arange(-Math.floor(nr), Math.ceil(nr)));
			double[] Nc2 = Fftshift.ifftShift1D(arange(-Math.floor(nc), Math.ceil(nc)));

			row_shift = Nr2[(int) row_shift] / 2;
			col_shift = Nc2[(int) col_shift] / 2;

			if (usfac > 2) {

				Complex[][] interCC1 = new Complex[buf1ft.length][buf1ft[0].length];
				for (int i = 0; i < buf1ft.length; i++) {
					for (int j = 0; j < buf1ft[i].length; j++) {
//                        interCC1[i][j] = buf2ft[i][j].multiply((buf1ft[i][j]).conjugate());
						double r1 = buf1ft[i][j].getReal();
						double im1 = -buf1ft[i][j].getImaginary();
						double r2 = buf2ft[i][j].getReal();
						double im2 = buf2ft[i][j].getImaginary();
						// (r2,im2)*(r1,im1)
						interCC1[i][j] = new Complex(r2 * r1 - im2 * im1, r2 * im1 + r1 * im2);
					}
				}
				row_shift = (double) (Math.round(row_shift * usfac)) / usfac;
				col_shift = (double) (Math.round(col_shift * usfac)) / usfac;
				double dftshift = (Math.ceil(usfac * 1.5)) / 2;
				// np.fix
				dftshift = (dftshift < 0) ? Math.ceil(dftshift) : Math.floor(dftshift);
//                Complex[][] newCC = new Complex[interCC1.length][interCC1[0].length];
				Complex[][] interCC11 = dftups(interCC1, (int) Math.ceil(usfac * 1.5), (int) Math.ceil(usfac * 1.5),
						usfac, (int) (dftshift - row_shift * usfac), (int) (dftshift - col_shift * usfac));

				Complex[][] newCC = new Complex[interCC11.length][interCC11[0].length];
				for (int i = 0; i < newCC.length; i++) {
					for (int j = 0; j < newCC[i].length; j++) {
						double r = interCC11[i][j].getReal();
						double im = interCC11[i][j].getImaginary();
						newCC[i][j] = new Complex(r, -im);
					}
				}
				ccAbs = absoluteValue(newCC);
				int rloc = -1;
				int cloc = -1;

				for (int i = 0; i < ccAbs.length; i++) {
					for (int j = 0; j < ccAbs[i].length; j++) {
						if (ccAbs[i][j] > maxVal) {
							maxVal = ccAbs[i][j];
							rloc = i;
							cloc = j;
						}
					}
				}
				ccMax = newCC[rloc][cloc];
				rloc = rloc - (int) dftshift;
				cloc = cloc - (int) dftshift;
				row_shift = row_shift + (double) (rloc) / usfac;
				col_shift = col_shift + (double) (cloc) / usfac;

			}
		}
		if (nr == 1) {
			row_shift = 0;
		}
		if (nc == 1) {
			col_shift = 0;
		}
		double rg00 = compute00(buf1ft);
		double rf00 = compute00(buf2ft);
		double error = 0;
		double diffphase;
		if (Objects.equals(ccMax, new Complex(0, 0))) {
			// cc max is double
			error = 1 - (Math.abs(ccmax) * Math.abs(ccmax)) / (rg00 * rf00);
			error = Math.sqrt(Math.abs(error));
			if (ccmax > 0) {
				diffphase = 0;
			} else if (ccmax < 0) {
				diffphase = Math.PI;
			} else {
				// CCmax is 0, angle is undefined
				diffphase = Double.NaN; // or handle this case as you see fit
			}
		} else {
			// ccmax is complex
			error = 1 - (ccMax.abs() * ccMax.abs()) / (rg00 * rf00);
			error = Math.sqrt(Math.abs(error));
			diffphase = Math.atan2(ccMax.getImaginary(), ccMax.getReal());
		}
		double[] output = { error, diffphase, row_shift, col_shift };
		Complex[][] greg = new Complex[buf2ft.length][buf2ft[0].length];
		if (usfac > 0) {
			int[][] newNc = new int[Nr.length][Nc.length];
			int[][] newNr = new int[Nr.length][Nc.length];
			for (int i = 0; i < Nr.length; i++) {
				for (int j = 0; j < Nc.length; j++) {
					newNc[i][j] = Nc[j];
					newNr[i][j] = Nr[i];
				}
			}
			for (int i = 0; i < nr; i++) {
				for (int j = 0; j < nc; j++) {
					double exponent = 2 * Math.PI * (-row_shift * newNr[i][j] / nr - col_shift * newNc[i][j] / nc);
					Complex multiplier = new Complex(Math.cos(exponent), Math.sin(exponent));
					greg[i][j] = buf2ft[i][j].multiply(multiplier);
				}
			}
			Complex multiplier = Complex.valueOf(Math.cos(diffphase), Math.sin(diffphase)); // Using Euler's formula

			for (int i = 0; i < nr; i++) {
				for (int j = 0; j < nc; j++) {
					greg[i][j] = greg[i][j].multiply(multiplier);
				}
			}
		} else if (usfac == 0) {
			Complex multiplier = Complex.valueOf(Math.cos(diffphase), Math.sin(diffphase)); // Using Euler's formula

			for (int i = 0; i < nr; i++) {
				for (int j = 0; j < nc; j++) {
					greg[i][j] = buf2ft[i][j].multiply(multiplier);
				}
			}
		}
//        DecimalFormat df = new DecimalFormat("#.########");
//
//        for (int i = 0; i < greg.length; i++) {
//            for (int j = 0; j < greg[i].length; j++) {
//                double real = Double.parseDouble(df.format(greg[i][j].getReal()));
//                double imaginary = Double.parseDouble(df.format(greg[i][j].getImaginary()));
//                greg[i][j] = new Complex(real, imaginary);
//            }
//        }
		return new Dftresult(output, greg);
	}

	private static double[][][] division3dArray(int[][][] input_array, int factor) {
		double[][][] out = new double[input_array.length][input_array[0].length][input_array[0][0].length];
//        DecimalFormat df = new DecimalFormat("#.########");
		for (int i = 0; i < input_array.length; i++) {
			for (int j = 0; j < input_array[0].length; j++) {
				for (int k = 0; k < input_array[0][0].length; k++) {
//                    out[i][j][k] = Double.parseDouble(df.format((double) input_array[i][j][k] / factor));
					out[i][j][k] = (double) input_array[i][j][k] / factor;
				}
			}
		}
		return out;
	}

	private static double[][] division2dArray(int[][] input_array, int factor) {
		double[][] out = new double[input_array.length][input_array[0].length];
//        DecimalFormat df = new DecimalFormat("#.########");
		for (int i = 0; i < input_array.length; i++) {
			for (int j = 0; j < input_array[0].length; j++) {
//                out[i][j] = Double.parseDouble(df.format((double) input_array[i][j] / factor));
				out[i][j] = (double) input_array[i][j] / factor;
			}
		}
		return out;
	}

	private static int findMax3dArray(int[][][] input_array) {
		int maxVal = 0;
		for (int i = 0; i < input_array.length; i++) {
			for (int j = 0; j < input_array[0].length; j++) {
				for (int k = 0; k < input_array[0][0].length; k++) {
					maxVal = Math.max(maxVal, input_array[i][j][k]);
				}
			}
		}
		return maxVal;
	}

	private static int findMax2dArray(int[][] input_array) {
		int maxVal = 0;
		for (int i = 0; i < input_array.length; i++) {
			for (int j = 0; j < input_array[0].length; j++) {
				maxVal = Math.max(maxVal, input_array[i][j]);
			}
		}
		return maxVal;
	}

	// normalise data type for 3d array
	private static double[][][] normalise_dtype(int[][][] input_array, boolean auto_range, int range_max) {
		double[][][] out = new double[input_array.length][input_array[0].length][input_array[0][0].length];
		if (!auto_range) {
			out = division3dArray(input_array, range_max);
		} else {
			if (checkDtype(input_array).equals("uint16")) {
				out = division3dArray(input_array, 65535);
			} else if (checkDtype(input_array).equals("uint8")) {
				out = division3dArray(input_array, 255);
			} else {
				int max = findMax3dArray(input_array);
				out = division3dArray(input_array, max);
			}
		}
		return out;
	}

	// normalise data type for 2d array
	private static double[][] normalise_dtype(int[][] input_array, boolean auto_range, int range_max) {
		double[][] out = new double[input_array.length][input_array[0].length];
		if (!auto_range) {
			out = division2dArray(input_array, range_max);
		} else {
			if (checkDtype(input_array).equals("uint16")) {
				out = division2dArray(input_array, 65535);
			} else if (checkDtype(input_array).equals("uint8")) {
				out = division2dArray(input_array, 255);
			} else {
				int max = findMax2dArray(input_array);
				out = division2dArray(input_array, max);
			}
		}
		return out;
	}

	private static double[][] realToComplex(double[][] a) {
		double[][] complexData = new double[a.length][2 * a[0].length];
		for (int i = 0; i < a.length; i++) {
			for (int j = 0; j < a[0].length; j++) {
				complexData[i][2 * j] = a[i][j]; // real part
				complexData[i][2 * j + 1] = 0.0; // imaginary part set to 0
			}
		}
		return complexData;
	}

	public static DCresult drift_correction(String threed_path, String threed_name, String selectedTitle,
			String wl_path, DC_GeneralParas gp, DC_Paras paras, double px_size) {

		double[][] threed_data;
		// if (!memory3D) {
		// IJ.log("load from file" + threed_path);
		// threed_data = load3DFile(threed_path, threed_name).getResultArray();
		// } else {
		IJ.log("loaded " + threed_name + " from memory.");
		MemoryPeakResults re = MemoryPeakResults.getResults(threed_name);
		// IJ.log(String.valueOf(re.getDistanceUnit()!= DistanceUnit.PIXEL));

		// if (re.getDistanceUnit()!= DistanceUnit.PIXEL) {

		// }

		threed_data = resultToArray(re, px_size);
		// }
		int[][][] wlArray;
		if (!memoryWL) {
			IJ.log("load from file " + wl_path);
			wlArray = loadtifFile(wl_path, false);
		} else {
			IJ.log("load from window" + selectedTitle);
			wlArray = loadtifFile(selectedTitle, true);
		}

		// the data set now has 5 array in a 2d array
		// column 1 is all value of x ...
//        double[][] threed_data = load3DFile(threed_path,threed_name).getResultArray();
//        double[] x = threed_data[0];
//        double[] y = threed_data[1];
//        double[] z = threed_data[2];
//        double[] intensity = threed_data[3];
//        double[] frame = threed_data[4];
		// reformat the data set
		// in data_arr, one column contains all xyz intensity frame
		// data_arr[i] = [x,y,z,intensity,frame]

		double[][] data_arr = new double[threed_data[0].length][5];
		for (int i = 0; i < threed_data[0].length; i++) {
			data_arr[i] = new double[] { threed_data[0][i], threed_data[1][i], threed_data[2][i], threed_data[3][i],
					threed_data[4][i] };
		}

		// find the position of first frame
		// there can be multiple first frames, with different xyz values
		ArrayList<Integer> locs1 = new ArrayList<Integer>();
		ArrayList<ArrayList<Double>> drift_by_frame = new ArrayList<>();
		double min = Double.MAX_VALUE;
		for (int i = 0; i < threed_data[4].length; i++) {
			if (min > threed_data[4][i]) {
				min = threed_data[4][i];
				locs1.clear();
				locs1.add(i);
			} else if (min == threed_data[4][i]) {
				locs1.add(i);
			}
		}

		ArrayList<double[]> data_corrected = new ArrayList<>();
		for (int i = 0; i < locs1.size(); i++) {
			data_corrected.add(data_arr[locs1.get(i)]);
		}

//		double[][] data_arr = new double[threed_data[0].length][5];
//		for (int i = 0; i < threed_data[0].length; i++) {
//			data_arr[i] = new double[] { threed_data[0][i], threed_data[1][i], threed_data[2][i], threed_data[3][i],
//					threed_data[4][i] };
//		}
//
//		// find the position of first frame
//		// sometimes there's multiple first frame, with different xyz value
//		ArrayList<Integer> locs1 = new ArrayList<Integer>();
//		double min = Integer.MAX_VALUE;
//		for (int i = 0; i < threed_data[4].length; i++) {
//			if (min > threed_data[4][i]) {
//				min = threed_data[4][i];
//				locs1.add(i);
//			}
//		}
//		ArrayList<double[]> data_corrected = new ArrayList<>();
//		ArrayList<ArrayList<Double>> drift_by_frame = new ArrayList<>();
////        double[][] data_corrected = new double[locs1.size()][5];
//		for (int i = 0; i < locs1.size(); i++) {
//			data_corrected.add(data_arr[locs1.get(i)]);
//		}
		if (!paras.wl_occasional) {
//            int[][][] wlArray = loadtifFile(wl_path);
			double[][] f = normalise_dtype(wlArray[0], true, 4096);

			ArrayList<double[]> shifts = new ArrayList<>();
			ArrayList<Double> xxx = new ArrayList<>();
			ArrayList<Double> yyy = new ArrayList<>();

			double maxVal = getMax(threed_data[4]);
			for (int i = 1; i < maxVal; i++) {
				// get the current frame
				ArrayList<Integer> locs = new ArrayList<>();
				for (int j = 0; j < threed_data[4].length; j++) {
					if (threed_data[4][j] == i) {
						locs.add(j);
						if (threed_data[4][j + 1] > i) {
							break;
						}
					}
				}
				double[][] g = normalise_dtype(wlArray[i - 1], true, 4096);
				double[][] newG = realToComplex(g);
				double[][] newF = realToComplex(f);

				DoubleFFT_2D fft2 = new DoubleFFT_2D(newG.length, newG[0].length);
				fft2.complexForward(newG);
				fft2.complexForward(newF);
				Dftresult r = dftregistration(convertToComplex(newF), convertToComplex(newG), (int) gp.upFactor);
				shifts.add(new double[] { i, r.getOutput()[3], r.getOutput()[2] });
				double xx = r.getOutput()[3] * gp.px_size;
				double yy = r.getOutput()[2] * gp.px_size;
				xxx.add(xx);
				yyy.add(yy);
				double[][] data_to_correct = new double[locs.size()][5];
				for (int j = 0; j < locs.size(); j++) {
					data_to_correct[j] = data_arr[locs.get(j)];
				}
				double[] corr_x = new double[data_to_correct.length];
//
//				if (paras.flip_x) {
//					for (int j = 0; j < corr_x.length; j++) {
//						corr_x[j] = data_to_correct[j][0] - xx;
//					}
//				} else {
//					for (int j = 0; j < corr_x.length; j++) {
//						corr_x[j] = data_to_correct[j][0] + xx;
//					}
//				}
//				double[] corr_y = new double[data_to_correct.length];
//
//				if (paras.flip_y) {
//					for (int j = 0; j < corr_y.length; j++) {
//						corr_y[j] = data_to_correct[j][1] - yy;
//					}
//				} else {
//					for (int j = 0; j < corr_y.length; j++) {
//						corr_y[j] = data_to_correct[j][1] + yy;
//					}
//				}

				for (int j = 0; j < corr_x.length; j++) {
					corr_x[j] = data_to_correct[j][0] + xx;
				}

				double[] corr_y = new double[data_to_correct.length];

				for (int j = 0; j < corr_y.length; j++) {
					corr_y[j] = data_to_correct[j][1] + yy;
				}

				double[][] corr_x_reshape = reshape(corr_x.length, 1, corr_x);
				double[][] corr_y_reshape = reshape(corr_y.length, 1, corr_y);
//                double[][] slice_data = new double[data_to_correct.length][3];
//                for(int j = 0; j < data_to_correct.length; j ++){
//                    for(int k = 2,a = 0 ; k < 5; k ++, a ++){
//                        slice_data[j][a] = data_to_correct[j][k];
//                    }
//                }
				double[][] slice_data = arraySliceCol2d(data_to_correct, 2);
				double[][] corrected = new double[slice_data.length][slice_data[0].length + corr_x_reshape[0].length
						+ corr_y_reshape[0].length];
				for (int j = 0; j < corrected.length; j++) {
					corrected[j][0] = corr_x_reshape[j][0];
					corrected[j][1] = corr_y_reshape[j][0];
					corrected[j][2] = slice_data[j][0];
					corrected[j][3] = slice_data[j][1];
					corrected[j][4] = slice_data[j][2];
				}
				for (int j = 0; j < corrected.length; j++) {
					data_corrected.add(corrected[j]);
				}
			}
			for (int j = 0; j < xxx.size(); j++) {
				ArrayList<Double> a = new ArrayList<>();
				a.add(xxx.get(j));
				a.add(yyy.get(j));
				drift_by_frame.add(a);
			}

		} else if (paras.wl_occasional && !paras.correction_twice && paras.getAverage_drift() == "Average_drift") {
//            int[][][] wlArray = loadtifFile(wl_path);

			double[][] f1 = normalise_dtype(wlArray[0], true, 4096);
			double[] fxx = zeros1col(gp.burst);
			double[] fyy = zeros1col(gp.burst);

			for (int i = 1; i < gp.burst; i++) {
				double[][] f = normalise_dtype(wlArray[i], true, 4096);
				double[][] newF = realToComplex(f);
				double[][] newF1 = realToComplex(f1);

				DoubleFFT_2D fft2 = new DoubleFFT_2D(f1.length, f1[0].length);
				fft2.complexForward(newF);
				fft2.complexForward(newF1);
				Dftresult r = dftregistration(convertToComplex(newF1), convertToComplex(newF), (int) gp.upFactor);
				fxx[i] = r.getOutput()[3];
				fyy[i] = r.getOutput()[2];
			}
			double avFxx = getMean(fxx);
			double avFyy = getMean(fyy);

			double max = getMax(threed_data[4]);
//            for (double[] doubles : data_arr) {
//                if (doubles[4] > max) {
//                    max = doubles[4];
//                }
//            }
			double[] avGxx = zeros1col(Math.ceil(max / gp.cycle) + 1 + 1);
			avGxx[avGxx.length - 1] = (avFxx);
			double[] avGyy = zeros1col(Math.ceil(max / gp.cycle) + 1 + 1);
			avGyy[avGyy.length - 1] = (avFyy);

			ArrayList<double[]> xxx = new ArrayList<>();
			ArrayList<double[]> yyy = new ArrayList<>();

			for (int i = 1; i < Math.ceil(max / gp.cycle) + 1; i++) {
				double[] Gxx = zeros1col(gp.burst);
				double[] Gyy = zeros1col(gp.burst);
				for (int j = 0; j < gp.burst; j++) {
					int frameAbs = (int) (i * gp.burst + j);
					if (frameAbs > wlArray.length) {
						frameAbs = (int) ((i * gp.burst + j) - gp.burst);
					}
					double[][] g = normalise_dtype(wlArray[frameAbs], true, 4096);
					double[][] newG = realToComplex(g);
					double[][] newF1 = realToComplex(f1);

					DoubleFFT_2D fft2 = new DoubleFFT_2D(f1.length, f1[0].length);
					fft2.complexForward(newG);
					fft2.complexForward(newF1);
					Dftresult r = dftregistration(convertToComplex(newF1), convertToComplex(newG), (int) gp.upFactor);
					Gxx[j] = r.getOutput()[3];
					Gyy[j] = r.getOutput()[2];
				}
				avGxx[i] = getMean(Gxx);
				avGyy[i] = getMean(Gyy);
				double[] ary = linspace(0, gp.cycle, (int) gp.cycle);
				double[] xx = interp(ary, new double[] { 0, gp.cycle }, new double[] { avGxx[i - 1], avGxx[i] });
				double[] yy = interp(ary, new double[] { 0, gp.cycle }, new double[] { avGyy[i - 1], avGyy[i] });
				xxx.add(xx);
				yyy.add(yy);

				for (int j = 1; j < gp.cycle + 1; j++) {
					double frameNumber = (i - 1) * gp.cycle + j;
					ArrayList<Integer> locs = new ArrayList<>();
					for (int k = 0; k < threed_data[4].length; k++) {
						if (threed_data[4][k] == frameNumber) {
							locs.add(k);
							if (k + 1 < threed_data[4].length && threed_data[4][k + 1] > frameNumber) {
								break;
							}
						}
					}
					if (locs.size() == 0) {
						continue;
					}
					double[][] data_to_correct = new double[locs.size()][5];
					for (int k = 0; k < locs.size(); k++) {
						data_to_correct[k] = data_arr[locs.get(k)];
					}
					double[] corr_x = new double[data_to_correct.length];
//					if (paras.flip_x) {
//						for (int k = 0; k < corr_x.length; k++) {
//							corr_x[k] = data_to_correct[k][0] - xx[j - 1] * gp.px_size;
//						}
//					} else {
//						for (int k = 0; k < corr_x.length; k++) {
//							corr_x[k] = data_to_correct[k][0] + xx[j - 1] * gp.px_size;
//						}
//					}
//					double[] corr_y = new double[data_to_correct.length];
//
//					if (paras.flip_y) {
//						for (int k = 0; k < corr_y.length; k++) {
//							corr_y[k] = data_to_correct[k][1] - yy[j - 1] * gp.px_size;
//						}
//					} else {
//						for (int k = 0; k < corr_y.length; k++) {
//							corr_y[k] = data_to_correct[k][1] + yy[j - 1] * gp.px_size;
//						}
//					}

					for (int k = 0; k < corr_x.length; k++) {
						corr_x[k] = data_to_correct[k][0] + xx[j - 1] * gp.px_size;
					}

					double[] corr_y = new double[data_to_correct.length];

					for (int k = 0; k < corr_y.length; k++) {
						corr_y[k] = data_to_correct[k][1] + yy[j - 1] * gp.px_size;
					}

					double[][] corr_x_reshape = reshape(corr_x.length, 1, corr_x);
					double[][] corr_y_reshape = reshape(corr_y.length, 1, corr_y);
					double[][] slice_data = new double[data_to_correct.length][3];
					for (int k = 0; k < data_to_correct.length; k++) {
						for (int f = 2, a = 0; f < 5; f++, a++) {
							slice_data[k][a] = data_to_correct[j][f];
						}
					}
					double[][] corrected = new double[slice_data.length][slice_data[0].length + corr_x_reshape[0].length
							+ corr_y_reshape[0].length];
					for (int k = 0; k < corrected.length; k++) {
						corrected[k][0] = corr_x_reshape[k][0];
						corrected[k][1] = corr_y_reshape[k][0];
						corrected[k][2] = slice_data[k][0];
						corrected[k][3] = slice_data[k][1];
						corrected[k][4] = slice_data[k][2];

					}
					for (int k = 0; k < corrected.length; k++) {
						data_corrected.add(corrected[k]);
					}
				}
				data_corrected.remove(0);
				ArrayList<Double> interX = new ArrayList<>();
				// ;put all value into one array
				for (double[] xArr : xxx) {
					for (double v : xArr) {
						interX.add(v * gp.px_size);
					}
				}
				ArrayList<Double> interY = new ArrayList<>();
				// ;put all value into one array
				for (double[] yArr : yyy) {
					for (double v : yArr) {
						interY.add(v * gp.px_size);
					}
				}
				// write drift_by_frame
				for (int k = 0; k < interX.size(); k++) {
					ArrayList<Double> subList = new ArrayList<>();
					subList.add(interX.get(k));
					subList.add(interY.get(k));
					drift_by_frame.add(subList);
				}
			}
		} else if (paras.wl_occasional && !paras.correction_twice && paras.getAverage_drift() == "Average_image") {
//            int[][][] wlArray = loadtifFile(wl_path);
			double[][][] wl_img = normalise_dtype(wlArray, true, 4096);
			int num_burst = (int) Math.floor(wl_img.length / gp.burst);

			ArrayList<double[][]> wl_avg = new ArrayList<>();
			for (int bur = 0; bur < num_burst; bur++) {
				int upperIndex = (int) (bur * gp.burst);
				int lowerIndex = (int) ((bur + 1) * gp.burst);
				double[][][] img_burst = new double[lowerIndex - upperIndex][][];

				for (int i = upperIndex; i < lowerIndex; i++) {
					img_burst[i - upperIndex] = wl_img[i];
				}
				double[][] burst_avg = meanAxis0(img_burst);
				wl_avg.add(burst_avg);
			}
			double last_loc = getMax(threed_data[4]);
			if (last_loc / gp.cycle > num_burst) {
				// add the last item in this list to this list
				wl_avg.add(wl_avg.get(wl_avg.size() - 1));
				num_burst += 1;
			}
			double[][] f1 = wl_avg.get(0);
			double[] Gxx = zeros1col(num_burst);
			double[] Gyy = zeros1col(num_burst);

			ArrayList<double[]> xxInter = new ArrayList<>();
			ArrayList<double[]> yyInter = new ArrayList<>();

			for (int i = 1; i < num_burst; i++) {
				double[][] g = wl_avg.get(i);
				double[][] newF1 = realToComplex(f1);
				double[][] newG = realToComplex(g);

				DoubleFFT_2D fft2 = new DoubleFFT_2D(f1.length, f1[0].length);
				fft2.complexForward(newF1);
				fft2.complexForward(newG);
				Dftresult r = dftregistration(convertToComplex(newF1), convertToComplex(newG), (int) gp.upFactor);

				Gxx[i] = r.getOutput()[3];
				Gyy[i] = r.getOutput()[2];
				double[] ary = linspace(0, gp.cycle, (int) gp.cycle);
				double[] xx_1 = interp(ary, new double[] { 0, gp.cycle }, new double[] { Gxx[i - 1], Gxx[i] });
				double[] yy_1 = interp(ary, new double[] { 0, gp.cycle }, new double[] { Gyy[i - 1], Gyy[i] });

				xxInter.add(xx_1);
				yyInter.add(yy_1);
			}
			ArrayList<Double> xx = concatenate(xxInter);
			ArrayList<Double> yy = concatenate(yyInter);

			ArrayList<Double> corr_x_list = new ArrayList<>();
			ArrayList<Double> corr_y_list = new ArrayList<>();

			for (int i = 0; i < data_arr[4].length; i++) {
				double frm = data_arr[4][i];
				double corr_x;

//				if (paras.flip_x) {
//					// data_Arr[1] is all y value
//					corr_x = threed_data[0][i] - xx.get((int) (frm - 1)) * gp.px_size;
//				} else {
//					corr_x = threed_data[0][i] + xx.get((int) (frm - 1)) * gp.px_size;
//				}
//				double corr_y;
//
//				if (paras.flip_y) {
//					// data_Arr[1] is all y value
//					corr_y = threed_data[1][i] - yy.get((int) (frm - 1)) * gp.px_size;
//				} else {
//					corr_y = threed_data[1][i] + yy.get((int) (frm - 1)) * gp.px_size;
//				}

				corr_x = threed_data[0][i] + xx.get((int) (frm - 1)) * gp.px_size;

				double corr_y;

				corr_y = threed_data[1][i] + yy.get((int) (frm - 1)) * gp.px_size;

				corr_x_list.add(corr_x);
				corr_y_list.add(corr_y);
			}
			ArrayList<double[]> xy = arrayT(corr_x_list, corr_y_list);
			double[][] inter_data_corrected = arraySliceCol2d(data_arr, 2);
			data_corrected = concatenateAxis1(xy, inter_data_corrected);
			arrayMultiplicaion(xx, gp.px_size);
			arrayMultiplicaion(yy, gp.px_size);
			drift_by_frame = arrayListT(xx, yy);

		} else if (paras.wl_occasional && paras.correction_twice && paras.getAverage_drift() == "Average_drift") {
//            int[][][] wlArray = loadtifFile(wl_path);
			double[][] f1 = normalise_dtype(wlArray[0], true, 4096);
			double[] fxx = zeros1col(gp.burst);
			double[] fyy = zeros1col(gp.burst);

			for (int i = 1; i < gp.burst; i++) {
				double[][] f = normalise_dtype(wlArray[i], true, 4096);
				double[][] newF = realToComplex(f);
				double[][] newF1 = realToComplex(f1);

				DoubleFFT_2D fft2 = new DoubleFFT_2D(f1.length, f1[0].length);
				fft2.complexForward(newF);
				fft2.complexForward(newF1);
				Dftresult r = dftregistration(convertToComplex(newF1), convertToComplex(newF), (int) gp.upFactor);
				fxx[i] = r.getOutput()[3];
				fyy[i] = r.getOutput()[2];
			}
			double avFxx = getMean(fxx);
			double avFyy = getMean(fyy);
			ArrayList<double[]> xxx = new ArrayList<>();
			ArrayList<double[]> yyy = new ArrayList<>();

			double max = getMax(threed_data[4]);
			for (int i = 1; i < Math.ceil(max / gp.cycle) + 1; i++) {
				double[] Gxx1 = zeros1col(gp.burst);
				double[] Gyy1 = zeros1col(gp.burst);
				double[] Gxx2 = zeros1col(gp.burst);
				double[] Gyy2 = zeros1col(gp.burst);

				for (int j = 0; j < gp.burst; j++) {

					if (i != 1) {
						int frameAbs1 = (int) (2 * (i - 1) * gp.burst + j);

						double[][] g1 = normalise_dtype(wlArray[frameAbs1], true, 4096);
						double[][] newG1 = realToComplex(g1);
						double[][] newF1 = realToComplex(f1);

						DoubleFFT_2D fft2 = new DoubleFFT_2D(f1.length, f1[0].length);
						fft2.complexForward(newG1);
						fft2.complexForward(newF1);

						Dftresult r = dftregistration(convertToComplex(newF1), convertToComplex(newG1),
								(int) gp.upFactor);

						Gxx1[j] = r.getOutput()[3];
						Gyy1[j] = r.getOutput()[2];
					}
					int frameAbs2 = (int) ((2 * i - 1) * gp.burst + j);
					double[][] g2 = normalise_dtype(wlArray[frameAbs2], true, 4096);
					double[][] newG2 = realToComplex(g2);
					double[][] newF1 = realToComplex(f1);

					DoubleFFT_2D fft2 = new DoubleFFT_2D(f1.length, f1[0].length);
					fft2.complexForward(newG2);
					fft2.complexForward(newF1);
					Dftresult r = dftregistration(convertToComplex(newF1), convertToComplex(newG2), (int) gp.upFactor);
					Gxx2[j] = r.getOutput()[3];
					Gyy2[j] = r.getOutput()[2];

				}
				double avGxx1 = 0;
				double avGyy1 = 0;
				if (i == 1) {
					avGxx1 = avFxx;
					avGyy1 = avFyy;
				} else {
					avGxx1 = getMean(Gxx1);
					avGyy1 = getMean(Gyy1);
				}
				double avGxx2 = getMean(Gxx2);
				double avGyy2 = getMean(Gyy2);

				double[] ary = linspace(0, gp.cycle, (int) gp.cycle);
				double[] xx = interp(ary, new double[] { 0, gp.cycle }, new double[] { avGxx1, avGxx2 });
				double[] yy = interp(ary, new double[] { 0, gp.cycle }, new double[] { avGyy1, avGyy2 });
				xxx.add(xx);
				yyy.add(yy);

				for (int j = 1; j < gp.cycle + 1; j++) {

					double frameNumber = (i - 1) * gp.cycle + j;
					ArrayList<Integer> locs = new ArrayList<>();
					for (int k = 0; k < threed_data[4].length; k++) {
						if (threed_data[4][k] == frameNumber) {
							locs.add(k);
							if (k + 1 < threed_data[4].length && threed_data[4][k + 1] > frameNumber) {
								break;
							}
						}
					}
					if (locs.size() == 0) {
						continue;
					}

					double[][] data_to_correct = new double[locs.size()][5];
					for (int k = 0; k < locs.size(); k++) {
						data_to_correct[k] = data_arr[locs.get(k)];
					}
					double[] corr_x = new double[data_to_correct.length];
//					if (paras.flip_x) {
//						for (int k = 0; k < corr_x.length; k++) {
//							corr_x[k] = data_to_correct[k][0] - xx[j - 1] * gp.px_size;
//						}
//					} else {
//						for (int k = 0; k < corr_x.length; k++) {
//							corr_x[k] = data_to_correct[k][0] + xx[j - 1] * gp.px_size;
//						}
//					}
//					double[] corr_y = new double[data_to_correct.length];
//
//					if (paras.flip_y) {
//						for (int k = 0; k < corr_y.length; k++) {
//							corr_y[k] = data_to_correct[k][1] - yy[j - 1] * gp.px_size;
//						}
//					} else {
//						for (int k = 0; k < corr_y.length; k++) {
//							corr_y[k] = data_to_correct[k][1] + yy[j - 1] * gp.px_size;
//						}
//					}

					for (int k = 0; k < corr_x.length; k++) {
						corr_x[k] = data_to_correct[k][0] + xx[j - 1] * gp.px_size;
					}

					double[] corr_y = new double[data_to_correct.length];

					for (int k = 0; k < corr_y.length; k++) {
						corr_y[k] = data_to_correct[k][1] + yy[j - 1] * gp.px_size;
					}

					double[][] corr_x_reshape = reshape(corr_x.length, 1, corr_x);
					double[][] corr_y_reshape = reshape(corr_y.length, 1, corr_y);
//                    for(int k = 0; k < corr_y.length; k ++){
//                        corr_y_reshape[k][0] = corr_y[k] ;
//                    }
//                    double[][] slice_data = new double[data_to_correct.length][3];
//                    for(int k = 0; k < data_to_correct.length; k ++){
//                        for(int f = 2,a = 0 ; f < 5; f ++, a ++){
//                            slice_data[k][a] = data_to_correct[j][f];
//                        }
//                    }
//                    System.out.println("j: " + j + " row: " +data_to_correct.length + " col: " + data_to_correct[0].length);
					double[][] slice_data = arraySliceCol2d(data_to_correct, 2);
					double[][] corrected = new double[slice_data.length][slice_data[0].length + corr_x_reshape[0].length
							+ corr_y_reshape[0].length];
					for (int k = 0; k < corrected.length; k++) {
						corrected[k][0] = corr_x_reshape[k][0];
						corrected[k][1] = corr_y_reshape[k][0];
						corrected[k][2] = slice_data[k][0];
						corrected[k][3] = slice_data[k][1];
						corrected[k][4] = slice_data[k][2];

					}
					for (int k = 0; k < corrected.length; k++) {
						data_corrected.add(corrected[k]);
					}
				}
				System.out.println(i);
			}
			data_corrected.remove(0);
			ArrayList<Double> xxxConcatenate = concatenate(xxx);
			double[][] reshape_xxx = reshape(arrayListSize(xxx), 1, toArrayMul(xxxConcatenate, gp.px_size));
			ArrayList<Double> yyyConcatenate = concatenate(yyy);
			double[][] reshape_yyy = reshape(arrayListSize(yyy), 1, toArrayMul(yyyConcatenate, gp.px_size));

			drift_by_frame = convertIntoArrayList(concatenateAxis1(reshape_xxx, reshape_yyy));
		} else if (paras.wl_occasional && paras.correction_twice && paras.getAverage_drift() == "Average_image") {
//            int[][][] wlArray = loadtifFile(wl_path);
			double[][][] wl_img = normalise_dtype(wlArray, true, 4096);
			int num_burst = (int) Math.floor(wl_img.length / gp.burst);
			System.out.println("num_burst: " + num_burst);

			ArrayList<double[][]> wl_avg = new ArrayList<>();
			for (int bur = 0; bur < num_burst; bur++) {
				int upperIndex = (int) (bur * gp.burst);
				int lowerIndex = (int) ((bur + 1) * gp.burst);
				double[][][] img_burst = new double[lowerIndex - upperIndex][][];

				for (int i = upperIndex; i < lowerIndex; i++) {
					img_burst[i - upperIndex] = wl_img[i];
				}
				double[][] burst_avg = meanAxis0(img_burst);
				wl_avg.add(burst_avg);
			}
			System.out
					.println("wl_avg: " + wl_avg.size() + ", " + wl_avg.get(0).length + ", " + wl_avg.get(0)[0].length);
			double[][] f1 = wl_avg.get(0);
			System.out.println("F1: " + f1[0][0] + " " + f1[0][1]);
			System.out.println();
			double[] Gxx = zeros1col(num_burst);
			double[] Gyy = zeros1col(num_burst);

			int num_cycle = (int) (Gxx.length / 2);
			double[] Gxx1 = zeros1col(num_cycle);
			double[] Gyy1 = zeros1col(num_cycle);
			double[] Gxx2 = zeros1col(num_cycle);
			double[] Gyy2 = zeros1col(num_cycle);

			ArrayList<double[]> xxInter = new ArrayList<>();
			ArrayList<double[]> yyInter = new ArrayList<>();

			for (int i = 1; i < num_burst; i++) {
				double[][] g = wl_avg.get(i);
				double[][] newF1 = realToComplex(f1);
				double[][] newG = realToComplex(g);

				DoubleFFT_2D fft2 = new DoubleFFT_2D(f1.length, f1[0].length);
				fft2.complexForward(newF1);
				fft2.complexForward(newG);
				Complex[][] cf1 = convertToComplex(newF1);
				Complex[][] cg = convertToComplex(newG);
				Dftresult r = dftregistration(cf1, cg, (int) gp.upFactor);

				Gxx[i] = r.getOutput()[3];
				Gyy[i] = r.getOutput()[2];
			}
//            for(int i = 0; i < 4; i++){
//                System.out.println("gxx: " + Gxx.get(i) + " size: " + Gxx.size());
//                System.out.println("gyy: " + Gyy.get(i) + " size: " + Gyy.size());
//                System.out.println();
//            }

			for (int i = 0; i < num_cycle; i++) {
				Gxx1[i] = Gxx[i * 2];
				Gxx2[i] = Gxx[i * 2 + 1];
				Gyy1[i] = Gyy[i * 2];
				Gyy2[i] = Gyy[i * 2 + 1];
				double[] ary = linspace(0, gp.cycle, (int) gp.cycle);
				double[] xx_i = interp(ary, new double[] { 0, gp.cycle }, new double[] { Gxx1[i], Gxx2[i] });
				double[] yy_i = interp(ary, new double[] { 0, gp.cycle }, new double[] { Gyy1[i], Gyy2[i] });

				xxInter.add(xx_i);
				yyInter.add(yy_i);
			}
			ArrayList<Double> xx = concatenate(xxInter);
			try (BufferedWriter writer = new BufferedWriter(new FileWriter("xx.txt"))) {
				for (int i = 0; i < xx.size(); i++) {
					writer.write(String.valueOf(xx.get(i)));
					writer.newLine();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			ArrayList<Double> yy = concatenate(yyInter);

//            for(int i = 0; i < 4; i++){
//                System.out.println("xx: " + xx.get(i) + " size: " + xx.size());
//                System.out.println("yy: " + yy.get(i) + " size: " + yy.size());
//                System.out.println();
//            }

			ArrayList<Double> corr_x_list = new ArrayList<>();
			ArrayList<Double> corr_y_list = new ArrayList<>();

			for (int i = 0; i < threed_data[4].length; i++) {
				double frm = threed_data[4][i];
				double corr_x;
//				if (paras.flip_x) {
//					corr_x = threed_data[0][i] - xx.get((int) (frm - 1)) * gp.px_size;
//				} else {
//					corr_x = threed_data[0][i] + xx.get((int) (frm - 1)) * gp.px_size;
////                    if(920<i && i <923){
////                        System.out.println("i: "+i + " frm: " + frm + "\nxx: " + xx.get((int) (frm-1)) + "\npx_size: " + gp.px_size + "\nthread_data: " + threed_data[0][i] + "corr_x: "+ corr_x + "\n\n");
////                    }
//				}
//				double corr_y;
//				if (paras.flip_y) {
//					corr_y = threed_data[1][i] - yy.get((int) (frm - 1)) * gp.px_size;
//				} else {
//					corr_y = threed_data[1][i] + yy.get((int) (frm - 1)) * gp.px_size;
////                    if(920<i && i <923){
////                        System.out.println("i: "+i + " frm: " + frm + "\nyy: " + yy.get((int) (frm-1)) + "\npx_size: " + gp.px_size + "\nthread_data: " + threed_data[1][i] + "corr_y: "+ corr_y + "\n\n");
////                    }
//				}

				corr_x = threed_data[0][i] + xx.get((int) (frm - 1)) * gp.px_size;
//                    if(920<i && i <923){
//                        System.out.println("i: "+i + " frm: " + frm + "\nxx: " + xx.get((int) (frm-1)) + "\npx_size: " + gp.px_size + "\nthread_data: " + threed_data[0][i] + "corr_x: "+ corr_x + "\n\n");
//                    }

				double corr_y;

				corr_y = threed_data[1][i] + yy.get((int) (frm - 1)) * gp.px_size;
//                    if(920<i && i <923){
//                        System.out.println("i: "+i + " frm: " + frm + "\nyy: " + yy.get((int) (frm-1)) + "\npx_size: " + gp.px_size + "\nthread_data: " + threed_data[1][i] + "corr_y: "+ corr_y + "\n\n");
//                    }

				corr_x_list.add(corr_x);
				corr_y_list.add(corr_y);
			}
			ArrayList<double[]> xy = arrayT(corr_x_list, corr_y_list);
			double[][] inter_data_corrected = arraySliceCol2d(data_arr, 2);
			data_corrected = concatenateAxis1(xy, inter_data_corrected);

			arrayMultiplicaion(xx, gp.px_size);
			arrayMultiplicaion(yy, gp.px_size);
			double[][] xx_reshape = reshape(xx.size(), 1, toArray(xx));
			double[][] yy_reshape = reshape(yy.size(), 1, toArray(yy));
			drift_by_frame = convertIntoArrayList(concatenateAxis1(xx_reshape, yy_reshape));
		}

		double[][] drift_by_loc_inter = new double[data_corrected.size()][data_corrected.get(0).length];
		// IJ.log("drift loc inter length " + drift_by_loc_inter.length);
		// IJ.log("data_corrected.size() " + data_corrected.size());
		// IJ.log("data_corrected.get(0).length " + data_corrected.get(0).length);

		for (int i = 0; i < data_corrected.size(); i++) {
			for (int j = 0; j < data_corrected.get(0).length; j++) {
				drift_by_loc_inter[i][j] = data_corrected.get(i)[j] - data_arr[i][j];
			}
		}
		double[][] drift_by_loc = arraySliceCol2dInverse(drift_by_loc_inter, 2);
		// IJ.log("drift loc length " + drift_by_loc.length);

		return new DCresult(data_corrected, drift_by_loc, drift_by_frame);

	}
	// algorithm ends

	private static JFreeChart createChartPanel(ArrayList<ArrayList<Double>> drift_by_frame) {

		XYSeriesCollection dataset = new XYSeriesCollection();
		XYSeries series = new XYSeries("Trajectory");

		for (ArrayList<Double> point : drift_by_frame) {
			series.add(point.get(0), point.get(1));
		}

		dataset.addSeries(series);
		JFreeChart chart = ChartFactory.createXYLineChart("Trajectory Plot", "X", "Y", dataset,
				PlotOrientation.VERTICAL, true, true, false);

		XYPlot plot = chart.getXYPlot();
		XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
		plot.setBackgroundPaint(Color.WHITE);

//        for (int i = 0; i < dataset.getSeriesCount() - 1; i++) {
////            renderer.setSeriesStroke(i, new BasicStroke(0.5f));
//            renderer.setSeriesShapesVisible(i, false);
////            renderer.setSeriesPaint(i, getColorForValue(i, drift_by_frame.size()));
//        }
		renderer.setSeriesShapesVisible(0, false);
		renderer.setSeriesStroke(0, new BasicStroke(2.0f));
		NumberAxis domainAxis = (NumberAxis) plot.getDomainAxis();
		domainAxis.setTickUnit(new NumberTickUnit(100));
		NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
		rangeAxis.setTickUnit(new NumberTickUnit(50));
//        IJ.log("series: " + dataset.getSeriesCount());

		renderer = new XYLineAndShapeRenderer(true, false) {
			@Override
			public Paint getItemPaint(int row, int col) {
				XYSeries series = dataset.getSeries(row);
				int itemCount = series.getItemCount();
				float ratio = (float) col / (float) itemCount;
				Color startColor = Color.BLUE;
				Color endColor = Color.GREEN;
				int red = (int) (startColor.getRed() * (1 - ratio) + endColor.getRed() * ratio);
				int green = (int) (startColor.getGreen() * (1 - ratio) + endColor.getGreen() * ratio);
				int blue = (int) (startColor.getBlue() * (1 - ratio) + endColor.getBlue() * ratio);
				return new Color(red, green, blue);
			}
		};
		plot.setRenderer(renderer);

		int minValue = 0;
		int maxValue = drift_by_frame.size();
		// IJ.log(String.valueOf(drift_by_frame.size()));

		LookupPaintScale paintScale = new LookupPaintScale(minValue, maxValue, Color.BLUE);
		paintScale.add(minValue, Color.BLUE);
		double interval = (maxValue - minValue) / 100.0; // Divide the range into four segments

		for (int i = 1; i < 100; i++) {
			paintScale.add(minValue + interval * i, mixColors(Color.BLUE, Color.GREEN, (100 - i) * (0.01)));
		}
//        paintScale.add(minValue + 2 * interval, mixColors(Color.BLUE, Color.GREEN, 0.5));
//        paintScale.add(minValue + 3 * interval, mixColors(Color.BLUE, Color.GREEN, 0.25));
		paintScale.add(maxValue, Color.GREEN);

		NumberAxis scaleAxis = new NumberAxis("");
		scaleAxis.setAxisLinePaint(Color.white);
		scaleAxis.setTickMarkPaint(Color.white);
		scaleAxis.setRange(minValue, maxValue);

		TickUnits customUnits = new TickUnits();
		customUnits.add(new NumberTickUnit(maxValue - minValue));
		scaleAxis.setStandardTickUnits(customUnits);

		PaintScaleLegend legend = new PaintScaleLegend(paintScale, scaleAxis);
		legend.setAxisOffset(5.0);
		legend.setMargin(new RectangleInsets(5, 5, 5, 5));
		legend.setFrame(new BlockBorder(Color.white));
		legend.setPadding(new RectangleInsets(10, 10, 10, 10));
		legend.setStripWidth(10.0);
		legend.setPosition(RectangleEdge.RIGHT);

		chart.addSubtitle(legend);

//        plot.setRenderer(renderer);
		return chart;
	}

	private static Color mixColors(Color c1, Color c2, double weight) {
		double r = weight * c1.getRed() + (1.0 - weight) * c2.getRed();
		double g = weight * c1.getGreen() + (1.0 - weight) * c2.getGreen();
		double b = weight * c1.getBlue() + (1.0 - weight) * c2.getBlue();
		return new Color((int) r, (int) g, (int) b);
	}

	private void saveData(ArrayList<ArrayList<Double>> drift_by_frame, String savePath, MemoryPeakResults r,
			String threed_memory) {
		String savePath2 = savePath + threed_memory + "_Drift_by_Frame.csv";
		System.out.print("Path " + threed_memory + savePath2);
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(savePath2))) {
			writer.write("X drift (nm),Y drift (nm)\n");
			for (int n = 0; n < drift_by_frame.size(); n++) {
				List<Double> row = drift_by_frame.get(n);
				StringBuilder rowString = new StringBuilder();

				for (int i = 0; i < row.size(); i++) {
					rowString.append(row.get(i));
					if (i < row.size() - 1) {
						rowString.append(",");
					}
				}

				writer.write(rowString.toString() + "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static ArrayList<ArrayList<Double>> zoom(double[][] f, double upfac) {
		int srcWidth = f[0].length;
		int srcHeight = f.length;

		int targetWidth = (int) Math.round(srcWidth * upfac);
		int targetHeight = (int) Math.round(srcHeight * upfac);

		ArrayList<ArrayList<Double>> f_zoom = new ArrayList<>();

		for (int y = 0; y < targetHeight; y++) {
			ArrayList<Double> row = new ArrayList<>();
			for (int x = 0; x < targetWidth; x++) {
				int gx = (int) Math.round(x / upfac);
				int gy = (int) Math.round(y / upfac);

				// Clamp coordinates to avoid out-of-bounds access
				gx = Math.min(gx, srcWidth - 1);
				gy = Math.min(gy, srcHeight - 1);

				row.add(f[gy][gx]);
			}
			f_zoom.add(row);
		}
		return f_zoom;
	}

	private static double[][] convert(ArrayList<ArrayList<Double>> list) {
		// Number of rows is the size of the outer list
		int rows = list.size();

		// Find the maximum column size by iterating over all inner lists
		int maxCols = list.get(0).size();

		double[][] array = new double[rows][maxCols];

		for (int i = 0; i < rows; i++) {
			ArrayList<Double> innerList = list.get(i);
			for (int j = 0; j < innerList.size(); j++) {
				array[i][j] = innerList.get(j);
			}
		}

		return array;
	}

	private static ArrayList<ArrayList<Double>> shift_img(double[][] f, double xx, double yy) {
		int upfac = 10;
		int pad_num_x = (int) (Math.ceil(Math.abs(upfac * xx)));
		int pad_num_y = (int) (Math.ceil(Math.abs(upfac * yy)));

		int f_x = (int) (f[0].length * upfac);
		int f_y = (int) (f.length * upfac);
		double fmean = getMean2D(f);
		ArrayList<ArrayList<Double>> f_zoom = zoom(f, upfac);
		ArrayList<ArrayList<Double>> x_pad = new ArrayList<>();
		ArrayList<ArrayList<Double>> y_pad = new ArrayList<>();

		ArrayList<ArrayList<Double>> f_padx = new ArrayList<>();
		ArrayList<ArrayList<Double>> f_pady = new ArrayList<>();

		ArrayList<ArrayList<Double>> f_cutx = new ArrayList<>();
		ArrayList<ArrayList<Double>> f_cuty = new ArrayList<>();

		if (pad_num_x != 0) {
			x_pad = ones(f_y, pad_num_x);
			arrayMultiplicaion2d(x_pad, fmean);
		}
		if (pad_num_y != 0) {
			y_pad = ones(pad_num_y, f_x);
			arrayMultiplicaion2d(y_pad, fmean);
		}

		if (xx < 0) {
			f_padx = concatenateAxis1(f_zoom, x_pad);
			f_cutx = arraySliceCol2dInverse(f_padx, pad_num_x);

		} else if (xx == 0) {
			f_cutx = new ArrayList<>(f_zoom);

		} else if (xx > 0) {
			f_padx = concatenateAxis1(x_pad, f_zoom);
			int f_padx_size = f_padx.get(0).size();
			f_cutx = arraySliceCol2d(f_padx, (f_padx_size - pad_num_x));
		}

		if (yy < 0) {
			f_pady = concatenate2DAxis0(f_cutx, y_pad);
			f_cuty = arraySliceRow2dInverse(f_pady, pad_num_y);
		} else if (yy == 0) {
			f_cuty = new ArrayList<>(f_cutx);
		} else if (yy > 0) {
			f_pady = concatenate2DAxis0(y_pad, f_cutx);
			f_cuty = arraySliceRow2d(f_pady, (f_cutx.size() - pad_num_y));
		}
		ArrayList<ArrayList<Double>> result = zoom(convert(f_cuty), 1.0 / upfac);
		return result;
	}

	private static void align_wl(String wl_path, String selectedTitle, DC_Paras paras, DC_GeneralParas generalParas)
			throws Exception {
		int[][][] wlArray;
		File file = new File(wl_path);
		String filename;
		// String filename = file.getName();
		ArrayList<double[][]> fs = new ArrayList<>();
		if (!memoryWL) {
//            IJ.log("load from file" + wl_path);
			wlArray = loadtifFile(wl_path, false);
			filename = file.getName();
		} else {
//            IJ.log("load from window" + selectedTitle);
			wlArray = loadtifFile(selectedTitle, true);
			filename = selectedTitle;
		}
		double[][][] wl_img = normalise_dtype(wlArray, true, 4096);
		String address = saving_directory + filename;
		if (!paras.group_burst) {
			double[][] f0 = wl_img[0];
			fs.add(f0);

			for (int i = 1; i < wl_img.length; i++) {
				double[][] g = wl_img[i];
				double[][] newG = realToComplex(g);
				double[][] newF0 = realToComplex(f0);

				DoubleFFT_2D fft2 = new DoubleFFT_2D(f0.length, f0[0].length);
				fft2.complexForward(newG);
				fft2.complexForward(newF0);

				Dftresult r = dftregistration(convertToComplex(newF0), convertToComplex(newG),
						(int) generalParas.upFactor);
				double xx = r.getOutput()[3];
				double yy = r.getOutput()[2];
				fs.add(convert(shift_img(g, xx, yy)));
			}
			for (double[][] f : fs) {
				for (double[] ff : f) {
					for (int i = 0; i < ff.length; i++) {
						ff[i] = Math.floor(ff[i] * 65535);
					}
				}
			}
			saveAsMultipageTIFF(address, fs);
		}
		if (paras.group_burst) {
			int num_bursts = (int) Math.floor(wl_img.length / generalParas.burst);
			ArrayList<double[][]> grouped = new ArrayList<>();
			double[][] group;
			for (int i = 0; i < num_bursts; i++) {
				if ((i + 1) * generalParas.burst < wl_img.length) {
					int upperIndex = (int) (i * generalParas.burst);
					int lowerIndex = (int) ((i + 1) * generalParas.burst);

					double[][][] inter = new double[lowerIndex - upperIndex][][];
					for (int j = upperIndex; j < lowerIndex; j++) {
						inter[j - upperIndex] = wl_img[j];
					}
					group = meanAxis0(inter);
				} else {
					int upperIndex = (int) (i * generalParas.burst);
					int lowerIndex = wl_img.length;

					double[][][] inter = new double[lowerIndex - upperIndex][wl_img[0].length][wl_img[0][0].length];
					for (int j = upperIndex; j < lowerIndex; j++) {
						inter[j - upperIndex] = wl_img[j];
					}
					group = meanAxis0(inter);
				}
				grouped.add(group);
			}
			double[][] f0 = grouped.get(0);
			fs.add(f0);

			for (int i = 1; i < grouped.size(); i++) {
				double[][] g = grouped.get(i);
				double[][] newG = realToComplex(g);
				double[][] newF0 = realToComplex(f0);

				DoubleFFT_2D fft2 = new DoubleFFT_2D(f0.length, f0[0].length);
				fft2.complexForward(newF0);
				fft2.complexForward(newG);

				Dftresult r = dftregistration(convertToComplex(newF0), convertToComplex(newG),
						(int) generalParas.upFactor);
				double xx = r.getOutput()[3];
				double yy = r.getOutput()[2];
				ArrayList<ArrayList<Double>> inter = shift_img(g, xx, yy);
				fs.add(convert(inter));
			}
			for (double[][] f : fs) {
				for (double[] ff : f) {
					for (int i = 0; i < ff.length; i++) {
						ff[i] = Math.floor(ff[i] * 65535);
					}
				}
			}
			saveAsMultipageTIFF(address, fs);
		}

//        Gson gson = new Gson();
//        String json = gson.toJson(fs);
//
//        try (FileWriter filee = new FileWriter("fsdata.json")) {
//            filee.write(json);
//        }
	}

	private static void saveAsMultipageTIFF(String filename, ArrayList<double[][]> fs) throws Exception {

		String outputFilename = filename.substring(0, filename.length() - 4) + "_corr.tif";
		new File(outputFilename);

//        try (ImageOutputStream ios = ImageIO.createImageOutputStream(outputFile)) {
//            BufferedImage image = convertDoubleArrayToBufferedImage(fs.get(0));
//            ImageIO.write(image, "TIFF", ios);
//
//            // If more than one image, append to the same file
//            if (fs.size() > 1) {
//                for (int i = 1; i < fs.size(); i++) {
//                    BufferedImage image1 = convertDoubleArrayToBufferedImage(fs.get(i));
//                    ImageIO.write(image1, "TIFF", ios);
//                }
//            }
//        }
		int maxWidth = 0;
		int maxHeight = 0;
		for (double[][] slice : fs) {
			if (slice.length > maxHeight) {
				maxHeight = slice.length;
			}
			for (double[] row : slice) {
				if (row.length > maxWidth) {
					maxWidth = row.length;
				}
			}
		}

		ImageStack stack = new ImageStack(maxWidth, maxHeight);

		for (double[][] f : fs) {
			double[][] paddedSlice = new double[maxHeight][maxWidth];

			for (int y = 0; y < f.length; y++) {
				System.arraycopy(f[y], 0, paddedSlice[y], 0, f[y].length);
			}
			FloatProcessor fp = new FloatProcessor(maxWidth, maxHeight);
			for (int y = 0; y < maxHeight; y++) {
				for (int x = 0; x < maxWidth; x++) {
					fp.putPixelValue(x, y, (float) paddedSlice[y][x]);
				}
			}

			stack.addSlice(fp);
		}

		ImagePlus imp = new ImagePlus("Output", stack);
		ij.IJ.saveAsTiff(imp, outputFilename);
		IJ.log("corrected.tif file saved");
	}

//    private static BufferedImage convertDoubleArrayToBufferedImage(double[][] data) {
//        int width = data[0].length;
//        int height = data.length;
//        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
//
//        for (int y = 0; y < height; y++) {
//            for (int x = 0; x < width; x++) {
//                int value = (int) data[y][x];
//                image.setRGB(x, y, value);
//            }
//        }
//
//        return image;
//    }

	private static double compute00(Complex[][] buf1ft) {
		double sum = 0.0;
		for (int i = 0; i < buf1ft.length; i++) {
			for (int j = 0; j < buf1ft[i].length; j++) {
				Complex z = buf1ft[i][j];
				double absValueSquared = z.abs() * z.abs();
				sum += absValueSquared;
			}
		}
		return sum;
	}

	private static void arrayMultiplicaion(ArrayList<Double> xx, double factor) {
		xx.replaceAll(aDouble -> aDouble * factor);
	}

	private static void arrayMultiplicaion2d(ArrayList<ArrayList<Double>> xx, double factor) {
		for (ArrayList<Double> x : xx) {
			x.replaceAll(aDouble -> aDouble * factor);
		}
	}

	private static ArrayList<Double> concatenate(ArrayList<double[]> list) {
		ArrayList<Double> result = new ArrayList<>();
		for (double[] ary : list) {
			for (double i : ary) {
				result.add(i);
			}

		}
		return result;
	}

	private static double[][] arrayListToArray(ArrayList<double[]> arr) {
		double[][] result = new double[arr.size()][arr.get(0).length];
		for (int i = 0; i < result.length; i++) {
			for (int j = 0; j < result[0].length; j++) {
				result[i][j] = arr.get(i)[j];
			}
		}
		return result;
	}

	private static double[][] arraySliceCol2d(double[][] data_arr, int col) {
		double[][] inter_data_corrected = new double[data_arr.length][data_arr[0].length - col];
		for (int i = 0; i < data_arr.length; i++) {
			for (int j = col, k = 0; j < data_arr[0].length; j++, k++) {
				inter_data_corrected[i][k] = data_arr[i][j];
			}
		}
		return inter_data_corrected;
	}

	// [:,:col]
	private static ArrayList<ArrayList<Double>> arraySliceCol2d(ArrayList<ArrayList<Double>> data_arr, int col) {
		if (data_arr.get(0).size() < col) {
			throw new IllegalArgumentException("column number should larger than the total column number");
		}
		for (ArrayList<Double> d : data_arr) {
			while (d.size() > col) {
				d.remove(col);
			}
		}
		return data_arr;
	}

	// [:row,:]
	public static ArrayList<ArrayList<Double>> arraySliceRow2d(ArrayList<ArrayList<Double>> data_arr, int row) {
		if (data_arr.size() < row) {
			throw new IllegalArgumentException("row number should larger than the total row number");
		}
		while (data_arr.size() > row) {
			data_arr.remove(row);
		}
		return data_arr;
	}

	// [:,col:]
	private static ArrayList<ArrayList<Double>> arraySliceCol2dInverse(ArrayList<ArrayList<Double>> data_arr, int col) {
		if (data_arr.get(0).size() < col) {
			throw new IllegalArgumentException("column number should larger than the total column number");
		}
		for (ArrayList<Double> d : data_arr) {
			int size = d.size();
			while (d.size() > size - col) {
				d.remove(0);
			}
		}

		return data_arr;
	}

	// [row:,:]
	private static ArrayList<ArrayList<Double>> arraySliceRow2dInverse(ArrayList<ArrayList<Double>> data_arr, int row) {
		if (data_arr.size() < row) {
			throw new IllegalArgumentException("row number should larger than the total row number");
		}
		int size = data_arr.size();
		while (data_arr.size() > size - row) {
			data_arr.remove(0);
		}
		return data_arr;
	}

	private static double[][] arraySliceCol2dInverse(double[][] data_arr, int col) {
		double[][] inter_data_corrected = new double[data_arr.length][col];
		for (int i = 0; i < data_arr.length; i++) {
			for (int j = 0; j < col; j++) {
				inter_data_corrected[i][j] = data_arr[i][j];
			}
		}
		return inter_data_corrected;
	}

	/**
	 * concatenate with axia=1 is not allowed on 1d array
	 * 
	 * @param data [[1,2,3,4],[5,6,7,8]]
	 * @param arr  [[9,10,11,12],[13,14,15,16]]
	 * @return [[1,2,3,4,9,10,11,12],[5,6,7,8,13,14,15,16]]
	 */
	private static ArrayList<double[]> concatenateAxis1(ArrayList<double[]> data, double[][] arr) {
		ArrayList<double[]> returnList = new ArrayList<double[]>();

		for (int i = 0; i < data.size(); i++) {
			double[] concatenatedArray = new double[data.get(i).length + arr[i].length];
			System.arraycopy(data.get(i), 0, concatenatedArray, 0, data.get(i).length);
			System.arraycopy(arr[i], 0, concatenatedArray, data.get(i).length, arr[i].length);
			returnList.add(concatenatedArray);
		}
		return returnList;
	}

	private static ArrayList<ArrayList<Double>> concatenateAxis1(ArrayList<ArrayList<Double>> data,
			ArrayList<ArrayList<Double>> arr) {
		ArrayList<ArrayList<Double>> returnList = new ArrayList<>();

		for (int i = 0; i < data.size(); i++) {
			ArrayList<Double> inter = new ArrayList<>();
			for (Double d : data.get(i)) {
				inter.add(d);
			}
			for (Double d : arr.get(i)) {
				inter.add(d);

			}
			returnList.add(inter);
		}
		return returnList;
	}

	private static ArrayList<ArrayList<Double>> concatenate2DAxis0(ArrayList<ArrayList<Double>> a,
			ArrayList<ArrayList<Double>> b) {
		ArrayList<ArrayList<Double>> returnList = new ArrayList<>();
		for (ArrayList<Double> aa : a) {
			returnList.add(aa);
		}
		for (ArrayList<Double> bb : b) {
			returnList.add(bb);
		}
		return returnList;
	}

	private static ArrayList<double[]> concatenateAxis1(double[][] data, double[][] arr) {
		ArrayList<double[]> returnList = new ArrayList<double[]>();

		for (int i = 0; i < data.length; i++) {
			double[] concatenatedArray = new double[data[i].length + arr[i].length];
			System.arraycopy(data[i], 0, concatenatedArray, 0, data[i].length);
			System.arraycopy(arr[i], 0, concatenatedArray, data[i].length, arr[i].length);
			returnList.add(concatenatedArray);
		}
		return returnList;
	}

	private static ArrayList<ArrayList<Double>> convertIntoArrayList(ArrayList<double[]> arr) {
		ArrayList<ArrayList<Double>> result = new ArrayList<>();
		for (double[] ary : arr) {
			ArrayList<Double> inter = new ArrayList<>();
			for (double d : ary) {
				inter.add(d);
			}
			result.add(inter);
		}
		return result;
	}

	private static double[] toArray(ArrayList<Double> ary) {
		double[] result = new double[ary.size()];
		for (int i = 0; i < result.length; i++) {
			result[i] = ary.get(i);
		}
		return result;
	}

	private static double[] toArrayMul(ArrayList<Double> ary, double f) {
		double[] result = new double[ary.size()];
		for (int i = 0; i < result.length; i++) {
			result[i] = ary.get(i) * f;
		}
		return result;
	}

	/**
	 *
	 *
	 * @param a assume a is [1,2,3,4]
	 * @param b assume b is [5,6,7,8]
	 * @return return result will be [[1,5],[2,6],[3,7],[4,8]] this method will
	 *         return a arraylist, all its element will be a single java array
	 */
	private static ArrayList<double[]> arrayT(ArrayList<Double> a, ArrayList<Double> b) {
		if (a.size() != b.size()) {
			throw new IllegalArgumentException("two input arrayList should have same size");
		}
		ArrayList<double[]> result = new ArrayList<>();
		for (int i = 0; i < a.size(); i++) {
			double[] inter = new double[2];
			inter[0] = a.get(i);
			inter[1] = b.get(i);
			result.add(inter);
		}
		return result;
	}

	// this return 2d array list
	private static ArrayList<ArrayList<Double>> arrayListT(ArrayList<Double> a, ArrayList<Double> b) {
		if (a.size() != b.size()) {
			throw new IllegalArgumentException("two input arrayList should have same size");
		}
		ArrayList<ArrayList<Double>> result = new ArrayList<>();
		for (int i = 0; i < a.size(); i++) {
			ArrayList<Double> inter = new ArrayList<>();
			inter.add(a.get(i));
			inter.add(b.get(i));
			result.add(inter);

		}
		return result;
	}

	private static double[] interp(double[] x, double[] xp, double[] fp) {
		double[] results = new double[x.length];
//        DecimalFormat df = new DecimalFormat("#.########");

		for (int i = 0; i < x.length; i++) {
//            results[i] = Double.parseDouble(df.format(interpolate(x[i], xp, fp)));
			results[i] = interpolate(x[i], xp, fp);
		}

		return results;
	}

	public static double getMax(double[] array) {
		double max = 0;
		for (double v : array) {
			if (v > max) {
				max = v;
			}
		}
		return max;
	}

	private static double interpolate(double xi, double[] xp, double[] fp) {
		if (xi <= xp[0])
			return fp[0];
		if (xi >= xp[xp.length - 1])
			return fp[fp.length - 1];

		for (int i = 0; i < xp.length - 1; i++) {
			if (xi == xp[i]) {
				return fp[i];
			}
			if (xi < xp[i + 1]) {
				return fp[i] + (xi - xp[i]) * (fp[i + 1] - fp[i]) / (xp[i + 1] - xp[i]);
			}
		}

		throw new AssertionError("Unexpected interpolation error.");
	}

	private static double[] linspace(double start, double end, int numPoints) {
		// Ensure number of points is at least 2
		if (numPoints < 2) {
			throw new IllegalArgumentException("numPoints must be at least 2.");
		}

		double[] result = new double[numPoints];
		double step = (end - start) / (numPoints - 1);
//        DecimalFormat df = new DecimalFormat("#.########");

		for (int i = 0; i < numPoints; i++) {
			result[i] = start + i * step;
//            result[i] = (Double.parseDouble(df.format(start + i * step)));
		}

		return result;
	}

	private static double[][] meanAxis0(double[][][] img_burst) {
		int depth = img_burst.length;
		int rows = img_burst[0].length;
		int cols = img_burst[0][0].length;

		double[][] meanResult = new double[rows][cols];

		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < cols; j++) {
				double sum = 0;
				for (int d = 0; d < depth; d++) {
					sum += img_burst[d][i][j];
				}
				meanResult[i][j] = sum / depth;
			}
		}
		return meanResult;
	}

	private static int arrayListSize(ArrayList<double[]> arr) {
		int size = 0;
		for (double[] ary : arr) {
			size += ary.length;
		}
		return size;
	}

	private static double[][] reshape(int row, int col, double[] arr) {
		if (row * col != arr.length) {
			throw new IllegalArgumentException(
					"Cannot reshape arrayList with " + arr.length + " elements into shape (" + row + ", " + col + ")");
		}

		double[][] result = new double[row][col];
		int count = 0;
		for (int i = 0; i < row; i++) {
			for (int j = 0; j < col; j++) {
				result[i][j] = arr[count];
				count++;
			}
		}
		return result;

	}

	private static ArrayList<ArrayList<Double>> ones(double row, double col) {
		ArrayList<ArrayList<Double>> zeros = new ArrayList<>();
		for (int i = 0; i < row; i++) {
			ArrayList<Double> al = new ArrayList<>();
			for (int j = 0; j < col; j++) {
				al.add((double) 1);
			}
			zeros.add(al);
		}
		return zeros;
	}

	private static double[] zeros1col(double row) {
		double[] zeros = new double[(int) row];
		for (int i = 0; i < row; i++) {
			zeros[i] = 0;
		}
		return zeros;
	}

	private static double getMean2D(double[][] list2D) {
		double total = 0;
		int count = 0;

		for (double[] list : list2D) {
			for (double num : list) {
				total += num;
				count++;
			}
		}

		return total / count;
	}

	private static double getMean(double[] list) {
		double total = 0;
		int count = 0;

		for (double num : list) {
			total += num;
			count++;
		}
		return total / count;
	}

//	private static void reformatComplex(Complex[][] c) {
//		DecimalFormat df = new DecimalFormat("#.########");
//
//		for (int i = 0; i < c.length; i++) {
//			for (int j = 0; j < c[i].length; j++) {
//				double real = Double.parseDouble(df.format(c[i][j].getReal()));
//				double imaginary = Double.parseDouble(df.format(c[i][j].getImaginary()));
//				c[i][j] = new Complex(real, imaginary);
//			}
//		}
//	}

	private static int[] arange(int start, int end) {
		int[] array = new int[end - start];
		for (int i = 0; i < end - start; i++) {
			array[i] = start + i;
		}
		return array;
	}

	private static double[] arange(double start, double end) {
		double[] array = new double[(int) Math.ceil(end - start)];
		for (int i = 0; i < end - start; i++) {
			array[i] = start + i;
		}
		return array;
	}

	private static double[][] absoluteValue(Complex[][] complexArray) {
		int rows = complexArray.length;
		int cols = complexArray[0].length;
		double[][] absoluteArray = new double[rows][cols];

		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < cols; j++) {
				double r = complexArray[i][j].getReal();
				double im = complexArray[i][j].getImaginary();
				absoluteArray[i][j] = r * r + im * im;
			}
		}
		return absoluteArray;
	}

	private static double[][] convertToInterleaved(Complex[][] complexData) {
		int rows = complexData.length;
		int cols = complexData[0].length;
		double[][] interleavedData = new double[rows][2 * cols];
		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < cols; j++) {
				interleavedData[i][2 * j] = complexData[i][j].getReal();
				interleavedData[i][2 * j + 1] = complexData[i][j].getImaginary();
			}
		}
		return interleavedData;
	}

	private static Complex[][] convertToComplex(double[][] interleavedData) {
		int rows = interleavedData.length;
		int cols = interleavedData[0].length / 2; // Divide by 2 since each complex number is represented by 2 doubles
		Complex[][] complexData = new Complex[rows][cols];
		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < cols; j++) {
				double realPart = interleavedData[i][2 * j];
				double imaginaryPart = interleavedData[i][2 * j + 1];
				complexData[i][j] = new Complex(realPart, imaginaryPart);
			}
		}
		return complexData;
	}

	// check data type for 3d array
	private static String checkDtype(int[][][] array) {
		int height = array.length;
		int width = array[0].length;
		int depth = array[0][0].length;
		int maxVal = Integer.MIN_VALUE;
		int minVal = Integer.MAX_VALUE;
		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				for (int k = 0; k < depth; k++) {
					maxVal = Math.max(maxVal, array[i][j][k]);
					minVal = Math.min(minVal, array[i][j][k]);
				}
			}
		}
		if (minVal > 0) {
			if (maxVal < 256) {
				return "uint8";
			}
			if (maxVal < 65536 & minVal > 255) {
				return "uint16";
			}
		} else {
			return "other";
		}
		return "other"; // default return value
	}

	// check data type for 2d array
	private static String checkDtype(int[][] array) {
		int height = array.length;
		int width = array[0].length;
		int maxVal = Integer.MIN_VALUE;
		int minVal = Integer.MAX_VALUE;
		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				maxVal = Math.max(maxVal, array[i][j]);
				minVal = Math.min(minVal, array[i][j]);
			}
		}
		if (minVal > 0) {
			if (maxVal < 256) {
				return "uint8";
			}
			if (maxVal < 65536 & minVal > 255) {
				return "uint16";
			}
		} else {
			return "other";
		}

		return "other"; // default return value
	}

}
