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

import java.awt.AWTEvent;
import java.awt.Checkbox;
import java.awt.Choice;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;
import ij.IJ;
import ij.Macro;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import ij.plugin.frame.Recorder;
import uk.ac.cam.dhpsfu.analysis.ArrayUtils;
import uk.ac.cam.dhpsfu.analysis.FileIndex;
import uk.ac.cam.dhpsfu.analysis.PeakResultDHPSFU;
import uk.ac.cam.dhpsfu.analysis.Read3DFileCalib;
import uk.ac.sussex.gdsc.core.ij.ImageJUtils;
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

public class LoadLocalisationFIle implements PlugIn, DialogListener {
	private static final String TITLE = "Load File Localisations";
	private static String fileType = "Peakfit";
	// Data paths
	private static String dataPath = ""; // Data path
	private static String name;
	// private boolean saveToFile = true;
	private String savingFormat = "xls";

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
	private String manualFormat = ".csv";
	private List<Panel> fieldPanels = new ArrayList<>();
	private ArrayList<Panel> choicePanels = new ArrayList<>(); // Store panels containing choice components

	private List<Choice> choices = new ArrayList<>();

	private int skipLines;
	private List<TextField> customTextFields = new ArrayList<>();

	@Override
	public void run(String arg) {

		String macroOptions = Macro.getOptions();
		if (macroOptions != null) {
			//IJ.log("Received options from Macro: " + macroOptions);
			parseArguments(macroOptions);
		} else if (arg != null && !arg.isEmpty()) {
			parseArguments(arg);
		} else {
			//IJ.log("No arguments received.");
		}

		if (showDialog()) {
			Load();
		}

	}

	private void parseArguments(String arg) {
		String[] params = arg.split(" ");
		for (String param : params) {
			String[] keyVal = param.split("=");
			if (keyVal.length == 2) {
				switch (keyVal[0]) {
				case "File_directory":
					dataPath = keyVal[1];
					break;
				case "Data_format":
					fileType = keyVal[1];
					break;
				case "File_format":
					savingFormat = keyVal[1];
					break;
				case "Pixel_size":
					pxSize = Double.parseDouble(keyVal[1]);
					break;
				case "Manually_set_column_index":
					ifManualIndex = Boolean.parseBoolean(keyVal[1]);
					break;
				case "Manual_input_format":
					manualFormat = keyVal[1];
					break;
				case "Header":
					skipLines = Integer.parseInt(keyVal[1]);
					break;
				case "Frame":
					frameIndex = Integer.parseInt(keyVal[1]);
					break;
				case "X":
					xIndex = Integer.parseInt(keyVal[1]);
					break;
				case "Y":
					yIndex = Integer.parseInt(keyVal[1]);
					break;
				case "Z":
					zIndex = Integer.parseInt(keyVal[1]);
					break;
				case "Intensity":
					intensityIndex = Integer.parseInt(keyVal[1]);
					break;
				case "Precision":
					precisionIndex = Integer.parseInt(keyVal[1]);
					break;

				}
			}
			//IJ.log("Set " + keyVal[0] + " to " + keyVal[1]);
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
			t.setValues("Precision", precisions);
			t.show("Loaded localisations"); // need to change table name
		}
	}

	private boolean showDialog() {

		GenericDialog gd = new GenericDialog(TITLE);

		gd.addMessage("File directory:");
		gd.addFileField("File_directory", dataPath); // Use updated dataPath
		gd.addChoice("Data_format", new String[] { "Peakfit", "DHPSFU" }, fileType); // Use updated fileType
		gd.addChoice("File_format", new String[] { ".xls", ".3d" }, savingFormat); // Use updated savingFormat
		gd.addNumericField("Pixel_size_(nm)", pxSize, 1); // Use updated pxSize
	    gd.addCheckbox("Manually_set_column_index", ifManualIndex); // Use updated ifManualIndex
	    
		String[] formats3 = { ".3d", ".csv", ".xls" };
		addChoicePanel(gd, "Manual_input_format", formats3, manualFormat, choices);
		addFieldPanel(gd, "Header",  skipLines);
		addFieldPanel(gd, "Frame", frameIndex);
		addFieldPanel(gd, "X", xIndex);
		addFieldPanel(gd, "Y", yIndex);
		addFieldPanel(gd, "Z", zIndex);
		addFieldPanel(gd, "Intensity", intensityIndex);
		addFieldPanel(gd, "Precision", precisionIndex);

		togglePanelsVisibility(gd, false);

		gd.addDialogListener(this);

		String html = "<html>" + "<h2>Instruction about Load Localisation File Plugin</h2>"
				+ "*** For loading localisations from local file directory ***<br>" + "<br>"
				+ " Available pre-defined formats are GDSC PeakFit output file (.xls) for 2D localisations and ViSP format (.3d) for 3D localisations. <br> "
				+ "Any other format can be loaded by specifying custom columns. <br>" + " <br>"
				+ " Data format: Peakfit (GDSC PeakFit output file, .xls)  or DHPSFU (.3d). <br>"
				+ " File format: Choose the corresponding file extension: .xls or .3d <br>"
				+ " Pixel size (nm):  Camera pixel size in nm.    <br>" + "  <br>"
				+ "  When specifying custom columns, indexing starts from 0. -1 means that the data does not contain such a column.  <br>"
				+ "<br>" + "</font>";
		gd.addHelp(html);
		gd.showDialog();
		if (gd.wasCanceled()) {
			return false;
		}

		if (gd.wasOKed()) {
			dataPath = gd.getNextString();
			IJ.log("Load localisations from: " + dataPath);
			final File file = new File(dataPath);
			if (!file.exists()) {
				IJ.error(TITLE, "File does not exist.");
				return false;
			}

//		preferences.put("defaultDirectory", dataPath);
//		try {
//			preferences.flush();
//		} catch (BackingStoreException e) {
//			e.printStackTrace();
//		}
			name = extractFileName(dataPath);
			fileType = gd.getNextChoice();
			savingFormat = gd.getNextChoice();
			pxSize = gd.getNextNumber();
			Checkbox checkbox = (Checkbox) gd.getCheckboxes().get(0);
	        ifManualIndex = checkbox.getState(); 

			StringBuilder command = new StringBuilder();
			command.append("run(\"Load File Localisations\", ");
			command.append("\"File_directory=").append(dataPath).append(" ");
			command.append("Data_format=").append(fileType).append(" ");
			command.append("File_format=").append(savingFormat).append(" ");
			command.append("Pixel_size=").append(pxSize).append(" ");
			command.append("Manually_set_column_index=").append(ifManualIndex).append("");

			if (!ifManualIndex) {
				fileIndex = getColumnIndex(fileType);
				// IJ.log("Type is: " + fileType);
				if (fileType.equals("Peakfit")) {
					skipLines = 9;
				} else if (fileType.equals("DHPSFU")) {
					skipLines = 0;
				}
			} else {
				manualFormat = choices.get(0).getSelectedItem();

				skipLines = Integer.parseInt(customTextFields.get(0).getText());
				frameIndex = Integer.parseInt(customTextFields.get(1).getText());
	            xIndex = Integer.parseInt(customTextFields.get(2).getText());
	            yIndex = Integer.parseInt(customTextFields.get(3).getText());
	            zIndex = Integer.parseInt(customTextFields.get(4).getText());
	            intensityIndex = Integer.parseInt(customTextFields.get(5).getText());
	            precisionIndex = Integer.parseInt(customTextFields.get(6).getText());

		        if (fileIndex == null) {
		        	fileIndex = new FileIndex(frameIndex, xIndex, yIndex, zIndex, intensityIndex, precisionIndex);
		            // Replace with actual initialization if necessary
		        }

		        fileIndex.setFrameIndex(frameIndex);
		        fileIndex.setxIndex(xIndex);
		        fileIndex.setyIndex(yIndex);
		        fileIndex.setzIndex(zIndex);
		        fileIndex.setIntensityIndex(intensityIndex);
		        fileIndex.setPrecisionIndex(precisionIndex);

				// Include these parameters only if manual index is true
		        command.append(" Manual_input_format=").append(manualFormat).append(" ");
				command.append("Header=").append(skipLines).append(" ");
				command.append("Frame=").append(frameIndex).append(" ");
				command.append("X=").append(xIndex).append(" ");
				command.append("Y=").append(yIndex).append(" ");
				command.append("Z=").append(zIndex).append(" ");
				command.append("Intensity=").append(intensityIndex).append(" ");
				command.append("Precision=").append(precisionIndex).append("");
			}

			command.append("\");");

			if (Recorder.record) {
				Recorder.recordString(command.toString());
			}
		}
		return true;
	} // End of shoeDialog

	private String extractFileName(String filePath) {
		Path path = Paths.get(filePath);
		String fileNameWithExtension = path.getFileName().toString();

		// Remove the last extension (assuming format like 'file.ext.xls')
		int lastDotIndex = fileNameWithExtension.lastIndexOf('.');
		if (lastDotIndex > 0) {
			fileNameWithExtension = fileNameWithExtension.substring(0, lastDotIndex);
		}
		return fileNameWithExtension;
	}

	private void addFieldPanel(GenericDialog gd, String fieldName, int initialValue) {
	    Panel panel = new Panel(new GridLayout(1, 2)); // Set GridLayout to each panel
	    Label label = new Label(fieldName + ":");
	    TextField textField = new TextField(String.valueOf(initialValue), 10); // Set a fixed size for fields

	    panel.add(label);
	    panel.add(textField);
	    gd.addPanel(panel);
	    fieldPanels.add(panel);
	    customTextFields.add(textField); // Store the text field for later retrieval
	}

	private void addChoicePanel(GenericDialog gd, String label, String[] items, String defaultItem, List<Choice> choices) {
	    Panel choicePanel = new Panel(new GridLayout(1, 2)); // Creating a new panel for each choice
	    Choice choice = new Choice();
	    for (String item : items) {
	        choice.add(item);
	    }
	    choice.select(defaultItem);
	    choicePanel.add(new Label(label));
	    choicePanel.add(choice);
	    gd.addPanel(choicePanel);
	    choicePanels.add(choicePanel); // Add the panel to choicePanels for visibility control
	    choices.add(choice); // Add the choice to the list for value retrieval
	}

	@Override
	public boolean dialogItemChanged(GenericDialog gd, AWTEvent e) {
	    Choice dataFormatChoice = (Choice) gd.getChoices().get(0);
	    Choice fileFormatChoice = (Choice) gd.getChoices().get(1);

	    String selectedDataFormat = dataFormatChoice.getSelectedItem();

	    if ("Peakfit".equals(selectedDataFormat)) {
	        fileFormatChoice.select(".xls");
	    } else if ("DHPSFU".equals(selectedDataFormat)) {
	        fileFormatChoice.select(".3d");
	    }
	    Checkbox checkbox = (Checkbox) gd.getCheckboxes().get(0);
	    boolean manualIndexState = checkbox.getState();
	    //IJ.log("Checkbox state during dialog: " + manualIndexState); // Log state during dialog
	    togglePanelsVisibility(gd, manualIndexState);
	    return true;
	}

	private void togglePanelsVisibility(GenericDialog gd, boolean visible) {
		for (Panel panel : fieldPanels) {
			panel.setVisible(visible);
		}
		for (Panel panel : choicePanels) {
			panel.setVisible(visible);
		}
		gd.pack(); // Adjust the dialog to accommodate the new layout
	}

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
			}
//			else if (savingFormat == ".csv") {
//				try {
//					data = importer.readCSVDoubleFile(Paths.get(dataPath), skipLines);
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//			}
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
			}
			// else if (savingFormat == ".csv") {
//				try {
//					data = importer.readCSVDoubleFile(Paths.get(dataPath), skipLines);
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//			}
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
			int skipLines, String manualFormat,double pxSize) {
		SplittableRandom rng = new SplittableRandom();
		Read3DFileCalib importer = new Read3DFileCalib();
		double[][] data = null;

		MemoryPeakResults results = new MemoryPeakResults();
		results.setName(name);
		if (fileType == "Peakfit") {
			if (manualFormat == ".xls") {
				try {
					data = importer.readCSVDouble(Paths.get(dataPath), skipLines);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (manualFormat == ".csv") {

				try {
					data = importer.readCSVDoubleFile(Paths.get(dataPath), skipLines);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			final String msg = "Data is " + TextUtils.pleural(data != null ? 1 : 0, "");
			IJ.showStatus(msg);
			//ImageJUtils.log(msg);
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
						if (x[1]/pxSize < 0.5) {	
						parameters[PeakResultDHPSFU.X] = (float) x[i];
					} else {
						parameters[PeakResultDHPSFU.X] = (float) ((float) x[i]/pxSize);
						}
					}
					if (y != null) {
						if (x[1]/pxSize < 0.5) {	
							parameters[PeakResultDHPSFU.Y] = (float) y[i];
						} else {
							parameters[PeakResultDHPSFU.Y] = (float) ((float) y[i]/pxSize);
							}
					}
					if (intensity != null) {
						parameters[PeakResultDHPSFU.INTENSITY] = (float) intensity[i];
					}
					if (precision != null) {
						if (x[1]/pxSize < 0.5) {	
							parameters[PeakResultDHPSFU.PRECISION] = (float) precision[i];
						} else {
							parameters[PeakResultDHPSFU.PRECISION] = (float) ((float) precision[i]/pxSize);
							}
						
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
			//IJ.log("x1= " + x[1]);
			
			cw.setDistanceUnit(DistanceUnit.PIXEL);
			cw.setTimeUnit(TimeUnit.FRAME);
			cw.setExposureTime(50);
			cw.setNmPerPixel(100);
			cw.setCountPerPhoton(45);
			cw.getBuilder().getCameraCalibrationBuilder().setCameraType(CameraType.EMCCD).setBias(100)
					.setQuantumEfficiency(0.95).setReadNoise(1.6);
			results.setCalibration(cw.getCalibration());
			return results;

		} else if (manualFormat == ".3d") {
			try {
				data = importer.readCSVDouble(Paths.get(dataPath), skipLines);
			} catch (IOException e) {
				e.printStackTrace();
			}
			final String msg = "Data is " + TextUtils.pleural(data != null ? 1 : 0, "");
			//IJ.showStatus(msg);
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
			localisations = LoadLocalisationsManual(dataPath, fileIndex, fileType, skipLines, manualFormat,pxSize);
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

	public static void main(String[] args) {
		new LoadLocalisationFIle().run(
				"fileType=DHPSFU savingFormat=.3d pxSize=100 ifManualIndex=false dataPath=C:/Users/yw525/Documents/cell2_2nM_slice7_corr.3d");
	}
}
