package uk.ac.cam.dhpsfu.plugins;

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

import ij.IJ;
import ij.Prefs;
import ij.gui.GenericDialog;
import ij.gui.YesNoCancelDialog;
import ij.io.OpenDialog;
import ij.plugin.PlugIn;
import ij.plugin.frame.Recorder;
import ij.util.Java2;
import java.awt.Choice;
import java.awt.EventQueue;
import java.awt.Label;
import java.awt.Panel;
import java.awt.Rectangle;
import java.awt.event.ItemListener;
import java.awt.geom.Rectangle2D;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

import javax.swing.JFileChooser;
import org.apache.commons.lang3.StringUtils;
import uk.ac.sussex.gdsc.core.ij.ImageJPluginLoggerHelper;
import uk.ac.sussex.gdsc.core.ij.ImageJUtils;
import uk.ac.sussex.gdsc.core.ij.SimpleImageJTrackProgress;
import uk.ac.sussex.gdsc.core.ij.gui.ExtendedGenericDialog;
import uk.ac.sussex.gdsc.core.ij.gui.ExtendedGenericDialog.OptionListener;
import uk.ac.sussex.gdsc.core.ij.gui.MultiDialog;
import uk.ac.sussex.gdsc.core.ij.process.LutHelper;
import uk.ac.sussex.gdsc.core.utils.BitFlagUtils;
import uk.ac.sussex.gdsc.core.utils.FileUtils;
import uk.ac.sussex.gdsc.core.utils.LocalList;
import uk.ac.sussex.gdsc.core.utils.MathUtils;
import uk.ac.sussex.gdsc.core.utils.TextUtils;
import uk.ac.sussex.gdsc.core.utils.ValidationUtils;
import uk.ac.sussex.gdsc.smlm.data.config.CalibrationProtos.CameraType;
import uk.ac.sussex.gdsc.smlm.data.config.CalibrationWriter;
import uk.ac.sussex.gdsc.smlm.data.config.ResultsProtos.ResultsFileSettings;
import uk.ac.sussex.gdsc.smlm.data.config.ResultsProtos.ResultsImageMode;
import uk.ac.sussex.gdsc.smlm.data.config.ResultsProtos.ResultsImageSettings;
import uk.ac.sussex.gdsc.smlm.data.config.ResultsProtos.ResultsImageSizeMode;
import uk.ac.sussex.gdsc.smlm.data.config.ResultsProtos.ResultsImageType;
import uk.ac.sussex.gdsc.smlm.data.config.ResultsProtos.ResultsInMemorySettings;
import uk.ac.sussex.gdsc.smlm.data.config.ResultsProtos.ResultsSettings;
import uk.ac.sussex.gdsc.smlm.data.config.ResultsProtos.ResultsSettings.Builder;
import uk.ac.sussex.gdsc.smlm.data.config.ResultsProtos.ResultsTableFormat;
import uk.ac.sussex.gdsc.smlm.data.config.ResultsProtos.ResultsTableSettings;
import uk.ac.sussex.gdsc.smlm.data.config.ResultsProtosHelper;
import uk.ac.sussex.gdsc.smlm.data.config.UnitProtos.DistanceUnit;
import uk.ac.sussex.gdsc.smlm.data.config.UnitProtos.IntensityUnit;
import uk.ac.sussex.gdsc.smlm.ij.IJImageSource;
import uk.ac.sussex.gdsc.smlm.ij.gui.PeakResultTableModel;
import uk.ac.sussex.gdsc.smlm.ij.gui.PeakResultTableModelFrame;
import uk.ac.sussex.gdsc.smlm.ij.plugins.HelpUrls;
import uk.ac.sussex.gdsc.smlm.ij.plugins.ParameterUtils;
import uk.ac.sussex.gdsc.smlm.ij.plugins.PeakFit;
import uk.ac.sussex.gdsc.smlm.ij.plugins.ResultsManager;
import uk.ac.sussex.gdsc.smlm.ij.plugins.SmlmUsageTracker;
import uk.ac.sussex.gdsc.smlm.ij.plugins.SummariseResults;
import uk.ac.sussex.gdsc.smlm.ij.results.ImageJImagePeakResults;
import uk.ac.sussex.gdsc.smlm.ij.results.ImageJTablePeakResults;
import uk.ac.sussex.gdsc.smlm.ij.results.ImagePeakResultsFactory;
import uk.ac.sussex.gdsc.smlm.ij.settings.SettingsManager;
import uk.ac.sussex.gdsc.smlm.results.ExtendedPeakResult;
import uk.ac.sussex.gdsc.smlm.results.FixedPeakResultList;
import uk.ac.sussex.gdsc.smlm.results.MemoryPeakResults;
import uk.ac.sussex.gdsc.smlm.results.PeakResults;
import uk.ac.sussex.gdsc.smlm.results.PeakResultsList;
import uk.ac.sussex.gdsc.smlm.results.PeakResultsReader;
import uk.ac.sussex.gdsc.smlm.results.ResultOption;
import uk.ac.sussex.gdsc.smlm.results.TextFilePeakResults;
import uk.ac.sussex.gdsc.smlm.results.count.Counter;
import uk.ac.sussex.gdsc.smlm.results.procedures.PeakResultProcedureX;
import uk.ac.sussex.gdsc.smlm.results.procedures.StandardResultProcedure;

/**
 * Opens peaks results and displays/converts them.
 */
public class ResultManager implements PlugIn {
	/** Use this to add extra options to the dialog. */
	static final int FLAG_EXTRA_OPTIONS = 0x00000001;
	/** Use this to add the results directory to the file results dialog. */
	static final int FLAG_RESULTS_DIRECTORY = 0x00000002;
	/** Use this to add the results file to the file results dialog. */
	static final int FLAG_RESULTS_FILE = 0x00000004;
	/** Use this to avoid adding the section header to the dialog. */
	static final int FLAG_NO_SECTION_HEADER = 0x00000008;
	/** Use this to add a choice of table format to the dialog. */
	static final int FLAG_TABLE_FORMAT = 0x00000010;
	/** Use this to remove the None option from the results image options. */
	static final int FLAG_IMAGE_REMOVE_NONE = 0x00000020;
	/** Use this to avoid adding the LUT option to the results image options. */
	static final int FLAG_IMAGE_NO_LUT = 0x00000040;

	private static final String TITLE = "Results Manager";
	private static final Logger logger = ImageJPluginLoggerHelper.getLogger(ResultsManager.class);

	private static final AtomicReference<List<String>> LAST_SELECTED = new AtomicReference<>();

	/** An empty array of load options. */
	private static final LoadOption[] EMPTY_LOAD_OPTIONS = new LoadOption[0];

	/** The name of the input field. */
	private static final String INPUT_NAME = "Input";

	/** The input file. */
	static final String INPUT_FILE = "File";

	/** The input memory. */
	static final String INPUT_MEMORY = "Memory";

	/** The input none. */
	static final String INPUT_NONE = "[None]";

	private ResultsSettings.Builder resultsSettings = ResultsSettings.newBuilder();
	private boolean extraOptions;
	private static String fileType = "Peakfit";
	private static String dataPath = "E:/Fiji_sampledata/"; // Data path

	/** Flag set to true when the input dialog selected to load from file input. */
	private boolean fileInput;

	private String omDirectory;
	private File[] omFiles;

	/** The plugin settings. */
	private Settings settings;

	/**
	 * Contains the settings that are the re-usable state of the plugin.
	 */
	private static class Settings {
		/**
		 * The last settings used by the plugin. This should be updated after plugin
		 * execution.
		 */
		private static final AtomicReference<Settings> INSTANCE = new AtomicReference<>(new Settings());

		String inputOption;
		String inputFilename;
		double inputNmPerPixel;
		double inputGain;
		double inputExposureTime;

		Settings() {
			// Set defaults
			inputOption = "";
			inputFilename = Prefs.get("DHPSFU.Localisations", "");
			inputNmPerPixel = Prefs.get("DHPSFU.pxSize", 210);
			inputGain = Prefs.get("DHPSFU.Gain", 250);
			inputExposureTime = Prefs.get("DHPSFU.ExposureTime", 20);
		}

		Settings(Settings source) {
			inputOption = source.inputOption;
			inputFilename = source.inputFilename;
			inputNmPerPixel = source.inputNmPerPixel;
			inputGain = source.inputGain;
			inputExposureTime = source.inputExposureTime;
		}

		Settings copy() {
			return new Settings(this);
		}

		/**
		 * Load a copy of the settings.
		 *
		 * @return the settings
		 */
		static Settings load() {
			return INSTANCE.get().copy();
		}

		/**
		 * Save the settings.
		 */
		void save() {
			INSTANCE.set(this);
			Prefs.set("DHPSFU.Localisations", inputFilename);
			Prefs.set("DHPSFU.pxSize", inputNmPerPixel);
			Prefs.set("DHPSFU.Gain", inputExposureTime);
			Prefs.set("DHPSFU.ExposureTime", inputGain);
		}
	}

	/**
	 * Specify the results input source to be added to a dialog.
	 */
	enum InputSource {
	//@formatter:off
    /** File input. If specified then an addition field is added to the dialog for the filename. */
    FILE{ @Override
     String getName() { return "File"; }},

    /** Memory input. */
    MEMORY{ @Override
     String getName() { return "Memory"; }},

    /** Memory input with at least one result spanning frames (linked using ID). */
    MEMORY_MULTI_FRAME{ @Override
     String getName() { return "Memory (Multi-Frame)"; }},

    /** Memory input with no results spanning frames (linked using ID). */
    MEMORY_SINGLE_FRAME{ @Override
     String getName() { return "Memory (Single-Frame)"; }},

    /** Memory input with identified results (ID above zero). */
    MEMORY_CLUSTERED{ @Override
     String getName() { return "Memory (Id)"; }},

    /** Memory input with categorised results (category above zero). */
    MEMORY_CATEGORY{ @Override
     String getName() { return "Memory (Category)"; }},

    /** No input. */
    NONE{ @Override
     String getName() { return "None"; }};
    //@formatter:on

		@Override
		public String toString() {
			return getName();
		}

		/**
		 * Gets the name.
		 *
		 * @return the name
		 */
		abstract String getName();
	}

	/**
	 * Class that allows the current results held in memory to be listed.
	 */
	static class MemoryResultsList extends ArrayList<String> {
		private static final long serialVersionUID = 20190719L;

		private final Map<String, String> nameMap = new HashMap<>();

		/**
		 * Instantiates a new memory results list.
		 *
		 * @param filter the filter
		 */
		MemoryResultsList(Predicate<MemoryPeakResults> filter) {
			final Collection<MemoryPeakResults> allResults = MemoryPeakResults.getAllResults();
			for (final MemoryPeakResults results : allResults) {
				if (filter.test(results)) {
					final String name = results.getName();
					add(name);
					nameMap.put(name, name);
				}
			}
		}

		/**
		 * Gets the display converter to map the name to a display name.
		 *
		 * @return the display converter
		 */
		Function<String, String> getDisplayConverter() {
			return nameMap::get;
		}
	}

	/**
	 * Specifies an option for the loading of results.
	 */
	interface LoadOption {
		// Marker interface
	}

	/**
	 * Specifies a filename load option.
	 */
	static class FilenameLoadOption implements LoadOption {
		/** The filename. */
		private final String filename;

		/**
		 * Create a new instance.
		 *
		 * @param filename the filename
		 */
		FilenameLoadOption(String filename) {
			this.filename = filename;
		}

		/**
		 * Gets the filename.
		 *
		 * @return the filename
		 */
		String getFilename() {
			return filename;
		}
	}

	@Override
	public void run(String arg) {
		extraOptions = ImageJUtils.isExtraOptions();
		SmlmUsageTracker.recordPlugin(this.getClass(), arg);

		if (StringUtils.startsWith(arg, "clear")) {
			runClearMemory(arg);
			return;
		}

		if (!showDialog()) {
			return;
		}

		final MemoryPeakResults results = loadResults(settings.inputOption);

		if (MemoryPeakResults.isEmpty(results)) {
			IJ.error(TITLE, "No results could be loaded");
			IJ.showStatus("");
			return;
		}

		IJ.showStatus("Loaded " + TextUtils.pleural(results.size(), "result"));

		final Rectangle bounds = results.getBounds(true);
		final boolean showDeviations = resultsSettings.getShowDeviations() && canShowDeviations(results);
		final boolean showEndFrame = canShowEndFrame(results);
		final boolean showId = canShowId(results);
		final boolean showCategory = canShowCategory(results);

		// Display the configured output
		final PeakResultsList outputList = new PeakResultsList();

		outputList.copySettings(results);

		final int tableFormat = resultsSettings.getResultsTableSettings().getResultsTableFormatValue();
		if (tableFormat == ResultsTableFormat.IMAGEJ_VALUE) {
			addImageJTableResults(outputList, resultsSettings.getResultsTableSettings(), showDeviations, showEndFrame,
					results.is3D(), showId, showCategory);
		} else if (tableFormat == ResultsTableFormat.INTERACTIVE_VALUE) {
			showInteractiveTable(results, resultsSettings.getResultsTableSettings());
		}

		addImageResults(outputList, resultsSettings.getResultsImageSettings(), bounds,
				(extraOptions) ? FLAG_EXTRA_OPTIONS : 0);
		addFileResults(outputList, showDeviations, showEndFrame, showId, showCategory);

		final ResultsFileSettings resultsFileSettings = resultsSettings.getResultsFileSettings();
		saveFile(resultsFileSettings, results, fileType, dataPath);

		if (outputList.numberOfOutputs() == 0) {
			// This occurs when only using the interactive table
			// IJ.showStatus("Processed " + TextUtils.pleural(results.size(), "result"));
			ImageJUtils.log("No output shown.");
			return;
		}

		// Reduce to single object for speed
		final PeakResults output = (outputList.numberOfOutputs() == 1) ? outputList.toArray()[0] : outputList;

		output.begin();
		// Note: We could add a batch iterator to the MemoryPeakResults.
		// However the speed increase will be marginal as the main time
		// taken is in processing the outputs.

		// Process in batches to provide progress
		final Counter progress = new Counter();
		final int totalProgress = results.size();
		final int batchSize = Math.max(100, totalProgress / 10);
		final FixedPeakResultList batch = new FixedPeakResultList(batchSize);
		IJ.showProgress(0);
		results.forEach((PeakResultProcedureX) result -> {
			batch.add(result);
			if (batch.size == batchSize) {
				if (IJ.escapePressed()) {
					batch.clear();
					return true;
				}
				output.addAll(batch.results);
				batch.clear();
				IJ.showProgress(progress.incrementAndGet(batchSize), totalProgress);
			}
			return false;
		});

		// Will be empty if interrupted
		if (batch.isNotEmpty()) {
			output.addAll(batch.toArray());
		}
		IJ.showProgress(1);
		output.end();
		if (output.size() == results.size()) {
			IJ.showStatus("Processed " + TextUtils.pleural(results.size(), "result"));
		} else {
			IJ.showStatus(String.format("A %d/%s", output.size(), TextUtils.pleural(results.size(), "result")));
		}
	}

	/**
	 * Creates a MultiDialog listing all the results held in memory.
	 *
	 * @param title the dialog title
	 * @return the dialog
	 */
	static MultiDialog createMultiDialog(String title) {
		return createMultiDialog(title, results -> true);
	}

	/**
	 * Creates a MultiDialog listing all the results held in memory.
	 *
	 * @param title  the dialog title
	 * @param filter the filter to select results
	 * @return the dialog
	 */
	static MultiDialog createMultiDialog(String title, Predicate<MemoryPeakResults> filter) {
		final MemoryResultsList items = new MemoryResultsList(filter);
		final MultiDialog md = new MultiDialog(title, items);
		md.setDisplayConverter(items.getDisplayConverter());
		return md;
	}

	private static void runClearMemory(String arg) {
		if (MemoryPeakResults.isMemoryEmpty()) {
			IJ.error(TITLE, "There are no fitting results in memory");
			IJ.showStatus("");
			return;
		}

		Collection<MemoryPeakResults> allResults;
		boolean removeAll = false;
		String helpKey = "clear-memory-results";
		if (arg.contains("multi")) {
			helpKey += "-multi";
			final MultiDialog md = createMultiDialog(TITLE);
			md.setSelected(LAST_SELECTED.get());
			md.setHelpUrl(HelpUrls.getUrl(helpKey));
			md.showDialog();
			if (md.wasCancelled()) {
				return;
			}
			final List<String> selected = md.getSelectedResults();
			if (selected.isEmpty()) {
				return;
			}
			LAST_SELECTED.set(selected);
			allResults = new ArrayList<>(selected.size());
			for (final String name : selected) {
				final MemoryPeakResults r = MemoryPeakResults.getResults(name);
				if (r != null) {
					allResults.add(r);
				}
			}
		} else {
			removeAll = true;
			allResults = MemoryPeakResults.getAllResults();
		}
		if (allResults.isEmpty()) {
			return;
		}

		long memorySize = 0;
		int size = 0;
		for (final MemoryPeakResults results : allResults) {
			memorySize += MemoryPeakResults.estimateMemorySize(results);
			size += results.size();
		}
		final String memory = TextUtils.bytesToString(memorySize);
		final String count = TextUtils.pleural(size, "result");
		final String sets = TextUtils.pleural(allResults.size(), "set");

		final GenericDialog gd = new GenericDialog(TITLE);

		gd.addMessage(String.format("Do you want to remove %s from memory (%s, %s)?", count, sets, memory));
		gd.addHelp(HelpUrls.getUrl(helpKey));
		gd.showDialog();
		if (gd.wasCanceled()) {
			return;
		}

		if (removeAll) {
			MemoryPeakResults.clearMemory();
		} else {
			for (final MemoryPeakResults results : allResults) {
				MemoryPeakResults.removeResults(results.getName());
			}
		}

		SummariseResults.clearSummaryTable();
		ImageJUtils.log("Cleared %s (%s, %s)", count, sets, memory);
	}

	private static boolean canShowDeviations(MemoryPeakResults results) {
		return results.hasDeviations();
	}

	private static boolean canShowEndFrame(MemoryPeakResults results) {
		return results.hasEndFrame();
	}

	private static boolean canShowId(MemoryPeakResults results) {
		return results.hasId();
	}

	private static boolean canShowCategory(MemoryPeakResults results) {
		return results.hasCategory();
	}

	/**
	 * Adds the table results.
	 *
	 * @param resultsList     the results list
	 * @param resultsSettings the results settings
	 * @param showDeviations  the show deviations
	 * @param showEndFrame    the show end frame
	 * @param showZ           the show Z
	 * @param showId          the show id
	 * @param showCategory    the show category
	 * @return the IJ table peak results
	 */
	static ImageJTablePeakResults addTableResults(PeakResultsList resultsList, ResultsTableSettings resultsSettings,
			boolean showDeviations, boolean showEndFrame, boolean showZ, boolean showId, boolean showCategory) {
		if (resultsSettings.getShowTable()) {
			return addImageJTableResults(resultsList, resultsSettings, showDeviations, showEndFrame, showZ, showId,
					showCategory);
		}
		return null;
	}

	private static ImageJTablePeakResults addImageJTableResults(PeakResultsList resultsList,
			ResultsTableSettings resultsSettings, boolean showDeviations, boolean showEndFrame, boolean showZ,
			boolean showId, boolean showCategory) {
		final ImageJTablePeakResults r = new ImageJTablePeakResults(showDeviations);
		r.setDistanceUnit(resultsSettings.getDistanceUnit());
		r.setIntensityUnit(resultsSettings.getIntensityUnit());
		r.setAngleUnit(resultsSettings.getAngleUnit());
		r.setShowPrecision(resultsSettings.getShowPrecision());
		if (resultsSettings.getShowPrecision()) {
			r.setComputePrecision(true);
		}
		r.setShowEndFrame(showEndFrame);
		r.setRoundingPrecision(resultsSettings.getRoundingPrecision());
		r.setShowZ(showZ);
		r.setShowFittingData(resultsSettings.getShowFittingData());
		r.setShowNoiseData(resultsSettings.getShowNoiseData());
		r.setShowId(showId);
		r.setShowCategory(showCategory);
		resultsList.addOutput(r);
		return r;
	}

	/**
	 * Show interactive table.
	 *
	 * @param results              the results
	 * @param resultsTableSettings the results table settings
	 */
	static void showInteractiveTable(MemoryPeakResults results, ResultsTableSettings resultsTableSettings) {
		final PeakResultTableModel model = new PeakResultTableModel(results, true, resultsTableSettings);
		final PeakResultTableModelFrame frame = new PeakResultTableModelFrame(model);
		frame.setTitle(results.getName());
		frame.setVisible(true);
		// Save changes to the interactive table settings
		model.addSettingsUpdatedAction(s -> {
			// Load the latest settings and save the table options
			final ResultsSettings.Builder resultsSettings = SettingsManager.readResultsSettings(0).toBuilder();
			resultsSettings.setResultsTableSettings(s);
			SettingsManager.writeSettings(resultsSettings.build());
		});
	}

	/**
	 * Adds the image results.
	 *
	 * <p>
	 * Note: This always uses {@link ResultsImageMode#IMAGE_ADD}.
	 *
	 * @param resultsList     the results list
	 * @param resultsSettings the results settings
	 * @param bounds          the bounds
	 * @param flags           the flags
	 */
	static void addImageResults(PeakResultsList resultsList, ResultsImageSettings resultsSettings, Rectangle bounds,
			int flags) {
		if (resultsSettings.getImageTypeValue() > 0) {
			final ResultsImageSettings.Builder builder = resultsSettings.toBuilder();
			builder.setImageMode(ResultsImageMode.IMAGE_ADD);
			final ImageJImagePeakResults image = ImagePeakResultsFactory.createPeakResultsImage(builder,
					resultsList.getName(), bounds, resultsList.getNmPerPixel());
			// Rolling window size is set by the factory method. Unset it if not using extra
			// options.
			if (!BitFlagUtils.anySet(flags, FLAG_EXTRA_OPTIONS)) {
				image.setRollingWindowSize(0);
			}
			image.setRepaintDelay(2000);
			resultsList.addOutput(image);
		}
	}

	private void addFileResults(PeakResultsList resultsList, boolean showDeviations, boolean showEndFrame,
			boolean showId, boolean showCategory) {
		final ResultsFileSettings resultsFileSettings = this.resultsSettings.getResultsFileSettings();
		if (!TextUtils.isNullOrEmpty(resultsFileSettings.getResultsFilename())) {
			// Remove extension
			final String resultsFilename = FileUtils.replaceExtension(resultsFileSettings.getResultsFilename(),
					ResultsProtosHelper.getExtension(resultsFileSettings.getFileFormat()));

			if (fileInput && settings.inputFilename.equals(resultsFilename)) {
				IJ.log(TITLE + ": Input and output files are the same, skipping output ...");
				return;
			}

			// Update
			this.resultsSettings.getResultsFileSettingsBuilder().setResultsFilename(resultsFilename);
			SettingsManager.writeSettings(this.resultsSettings.build());

			addFileResults(resultsList, resultsFileSettings, resultsFilename, showDeviations, showEndFrame, showId,
					showCategory, fileType);
		}
	}

	/**
	 * Adds the file results.
	 *
	 * @param resultsList     the results list
	 * @param resultsSettings the results settings
	 * @param resultsFilename the results filename
	 * @param showDeviations  the show deviations
	 * @param showEndFrame    the show end frame
	 * @param showId          the show id
	 * @param showCategory    the show category
	 * @return the peak results
	 */
	static PeakResults addFileResults(PeakResultsList resultsList, ResultsFileSettings resultsSettings,
			String resultsFilename, boolean showDeviations, boolean showEndFrame, boolean showId, boolean showCategory,
			String fileType) {
		if (resultsSettings.getFileFormatValue() > 0 && resultsFilename != null) {
			final File file = new File(resultsFilename);
			final File parent = file.getParentFile();
			if (parent != null && parent.exists()) {
				PeakResults results;
				switch (fileType) {
				case "Peakfit":
					final TextFilePeakResults f = new TextFilePeakResults(dataPath, showDeviations, showEndFrame,
							showId, resultsSettings.getShowPrecision(), showCategory);
					f.setDistanceUnit(resultsSettings.getDistanceUnit());
					f.setIntensityUnit(resultsSettings.getIntensityUnit());
					f.setAngleUnit(resultsSettings.getAngleUnit());
					f.setComputePrecision(true);
					results = f;
					break;
				case "DHPSFU":
					final ImageJTablePeakResults c = new ImageJTablePeakResults(showDeviations);
					c.setDistanceUnit(resultsSettings.getDistanceUnit());
					c.setIntensityUnit(resultsSettings.getIntensityUnit());
					results = c;
					break;
				default:
					throw new IllegalArgumentException(
							"File format does not exist: " + resultsSettings.getFileFormat());
				}
				resultsList.addOutput(results);
				return results;
			}
		}
		return null;
	}

	private boolean showDialog() {
		final ExtendedGenericDialog gd = new ExtendedGenericDialog(TITLE);
		gd.addHelp(HelpUrls.getUrl("results-manager"));

		settings = Settings.load();
		resultsSettings = SettingsManager.readResultsSettings(0).toBuilder();

		gd.addMessage("Manage localisation results in memory.");

		gd.addMessage("Select the Localisation Results");
		addInput(gd, INPUT_NAME, settings.inputOption, InputSource.MEMORY);

		addTableResultsOptions(gd, resultsSettings, FLAG_TABLE_FORMAT);
		addImageResultsOptions(gd, resultsSettings);
		addFileResultsOptions(gd, resultsSettings, 0);
		addInMemoryResultsOptions(gd, resultsSettings);

		gd.showDialog();

		if (gd.wasCanceled()) {
			return false;
		}

		settings.inputOption = ResultsManager.getInputSource(gd);
		// settings.inputFilename = gd.getNextString();

		resultsSettings.getResultsTableSettingsBuilder().setResultsTableFormatValue(gd.getNextChoiceIndex());
		resultsSettings.getResultsImageSettingsBuilder().setImageTypeValue(gd.getNextChoiceIndex());

		int index = gd.getNextChoiceIndex();
		resultsSettings.getResultsFileSettingsBuilder().setFileFormatValue(index);

		String[] formats = { "None", "DHPSFU", "Peakfit" };
		fileType = formats[index];
		if (fileType == "None") {
			ImageJUtils.log("Localisations are not saved.");
		}
		else {
		// ImageJUtils.log("Diaglog File type is: " + fileType);
		dataPath = gd.getNextString();

		// Check if file exists
		final File file = new File(dataPath);
		if (file.exists()) {
			final YesNoCancelDialog d = new YesNoCancelDialog(IJ.getInstance(), TITLE,
					"Overwrite existing file?\n" + dataPath);
			if (!d.yesPressed()) {
				return false;
			}
		}
		}
		// ImageJUtils.log("Diaglog File path is: " + dataPath);
		resultsSettings.getResultsFileSettingsBuilder().setResultsFilename(dataPath);
		resultsSettings.getResultsInMemorySettingsBuilder().setInMemory(gd.getNextBoolean());

		gd.collectOptions();

		// Check arguments
		try {
			final ResultsImageSettings.Builder imageSettings = resultsSettings.getResultsImageSettingsBuilder();
			if (imageSettings.getImageTypeValue() > 0) {
				if (imageSettings.getImageType() == ResultsImageType.DRAW_INTENSITY_AVERAGE_PRECISION
						|| imageSettings.getImageType() == ResultsImageType.DRAW_LOCALISATIONS_AVERAGE_PRECISION) {
					ParameterUtils.isAboveZero("Image precision", imageSettings.getAveragePrecision());
				}
				if (imageSettings.getImageSizeMode() == ResultsImageSizeMode.SCALED) {
					ParameterUtils.isAboveZero("Image scale", imageSettings.getScale());
				} else if (imageSettings.getImageSizeMode() == ResultsImageSizeMode.IMAGE_SIZE) {
					ParameterUtils.isAboveZero("Image size", imageSettings.getImageSize());
				} else if (imageSettings.getImageSizeMode() == ResultsImageSizeMode.PIXEL_SIZE) {
					ParameterUtils.isAboveZero("Image pixel size", imageSettings.getPixelSize());
				}
			}
			if (extraOptions) {
				ParameterUtils.isPositive("Image rolling window", imageSettings.getRollingWindowSize());
			}
		} catch (final IllegalArgumentException ex) {
			IJ.error(TITLE, ex.getMessage());
			return false;
		}

		settings.save();
		SettingsManager.writeSettings(resultsSettings.build());
//    ImageJUtils.log("Diaglog File type is: " + fileType);
//    ImageJUtils.log("Diaglog File path is: " + dataPath);
		return true;
	}

	/**
	 * Adds the file results options.
	 *
	 * @param gd              the dialog
	 * @param resultsSettings the results settings
	 * @param flags           the flags
	 */
	static void addFileResultsOptions(final ExtendedGenericDialog gd, final Builder resultsSettings, final int flags) {
		if (BitFlagUtils.anyNotSet(flags, FLAG_NO_SECTION_HEADER)) {
			gd.addMessage("--- File output ---");
		}

		final ResultsFileSettings.Builder fileSettings = resultsSettings.getResultsFileSettingsBuilder();
		String[] formats = { "None", "DHPSFU(.3d)", "Peakfit(.xls)" };
		gd.addChoice("Results_format", formats, 0, new OptionListener<Integer>() {
			@Override
			public boolean collectOptions(Integer field) {
				fileSettings.setFileFormatValue(field);
				return collectOptions(false);
			}

			@Override
			public boolean collectOptions() {
				return collectOptions(true);
			}

			private boolean collectOptions(boolean silent) {
				// final ResultsFileFormat resultsFileFormat = fileSettings.getFileFormat();
//            if (!ResultsProtosHelper.isGdsc(resultsFileFormat)) {
//              return false;
//            }
				final int selectedIndex = fileSettings.getFileFormatValue();
				// int selectedIndex = gd.getNextChoiceIndex();
				final ExtendedGenericDialog egd = new ExtendedGenericDialog(TITLE, null);

				if (formats[selectedIndex].equals("DHPSFU(.3d)")) {
					egd.addChoice("File_distance_unit", SettingsManager.getDistanceUnitNames(),
							fileSettings.getDistanceUnitValue());
					egd.addChoice("File_intensity_unit", SettingsManager.getIntensityUnitNames(),
							fileSettings.getIntensityUnitValue());

				} else {

					egd.addChoice("File_distance_unit", SettingsManager.getDistanceUnitNames(),
							fileSettings.getDistanceUnitValue());
					egd.addChoice("File_intensity_unit", SettingsManager.getIntensityUnitNames(),
							fileSettings.getIntensityUnitValue());
					egd.addChoice("File_angle_unit", SettingsManager.getAngleUnitNames(),
							fileSettings.getAngleUnitValue());
					egd.addCheckbox("File_show_precision", fileSettings.getShowPrecision());
					egd.addCheckbox("Show_deviations", resultsSettings.getShowDeviations());
				}

				egd.setSilent(silent);
				egd.showDialog(true, gd);
				if (egd.wasCanceled()) {
					return false;
				}

				fileSettings.setDistanceUnitValue(egd.getNextChoiceIndex());
				fileSettings.setIntensityUnitValue(egd.getNextChoiceIndex());

				if (!formats[selectedIndex].equals("DHPSFU")) {
					fileSettings.setAngleUnitValue(egd.getNextChoiceIndex());
					fileSettings.setShowPrecision(egd.getNextBoolean());
					resultsSettings.setShowDeviations(egd.getNextBoolean());
				}
				return true;
			}
		});

		Preferences prefs = Preferences.userNodeForPackage(ResultManager.class);
		String defaultDirectory = prefs.get("defaultDirectory", "");
		gd.addFilenameField("File_directory", defaultDirectory);
	}

	/**
	 * Adds the table results options.
	 *
	 * @param gd              the dialog
	 * @param resultsSettings the results settings
	 */
	static void addTableResultsOptions(final ExtendedGenericDialog gd, final Builder resultsSettings) {
		addTableResultsOptions(gd, resultsSettings, 0);
	}

	/**
	 * Adds the table results options.
	 *
	 * @param gd              the dialog
	 * @param resultsSettings the results settings
	 * @param flags           the flags
	 */
	static void addTableResultsOptions(final ExtendedGenericDialog gd, final Builder resultsSettings, final int flags) {
		if (BitFlagUtils.anyNotSet(flags, FLAG_NO_SECTION_HEADER)) {
			gd.addMessage("--- Table output ---");
		}
		final ResultsTableSettings.Builder tableSettings = resultsSettings.getResultsTableSettingsBuilder();
		if (BitFlagUtils.anySet(flags, FLAG_TABLE_FORMAT)) {
			gd.addChoice("Table", SettingsManager.getResultsTableFormatNames(),
					tableSettings.getResultsTableFormatValue(), new OptionListener<Integer>() {
						@Override
						public boolean collectOptions(Integer field) {
							tableSettings.setResultsTableFormatValue(field);
							return collectOptions(false);
						}

						@Override
						public boolean collectOptions() {
							return collectOptions(true);
						}

						private boolean collectOptions(boolean silent) {
							if (tableSettings.getResultsTableFormatValue() <= 0) {
								return false;
							}
							final ExtendedGenericDialog egd = new ExtendedGenericDialog(TITLE, null);
							egd.addChoice("Table_distance_unit", SettingsManager.getDistanceUnitNames(),
									tableSettings.getDistanceUnitValue());
							egd.addChoice("Table_intensity_unit", SettingsManager.getIntensityUnitNames(),
									tableSettings.getIntensityUnitValue());
							egd.addChoice("Table_angle_unit", SettingsManager.getAngleUnitNames(),
									tableSettings.getAngleUnitValue());
							egd.addCheckbox("Table_show_fitting_data", tableSettings.getShowFittingData());
							egd.addCheckbox("Table_show_noise_data", tableSettings.getShowNoiseData());
							egd.addCheckbox("Table_show_precision", tableSettings.getShowPrecision());
							egd.addSlider("Table_precision", 0, 10, tableSettings.getRoundingPrecision());
							egd.setSilent(silent);
							egd.showDialog(true, gd);
							if (egd.wasCanceled()) {
								return false;
							}
							tableSettings.setDistanceUnitValue(egd.getNextChoiceIndex());
							tableSettings.setIntensityUnitValue(egd.getNextChoiceIndex());
							tableSettings.setAngleUnitValue(egd.getNextChoiceIndex());
							tableSettings.setShowFittingData(egd.getNextBoolean());
							tableSettings.setShowNoiseData(egd.getNextBoolean());
							tableSettings.setShowPrecision(egd.getNextBoolean());
							tableSettings.setRoundingPrecision((int) egd.getNextNumber());
							return true;
						}
					});
		} else {
			gd.addCheckbox("Show_results_table", tableSettings.getShowTable(), new OptionListener<Boolean>() {

				@Override
				public boolean collectOptions(Boolean field) {
					tableSettings.setShowTable(field);
					return collectOptions(false);
				}

				@Override
				public boolean collectOptions() {
					return collectOptions(true);
				}

				private boolean collectOptions(boolean silent) {
					if (!tableSettings.getShowTable()) {
						return false;
					}
					final ExtendedGenericDialog egd = new ExtendedGenericDialog(TITLE, null);
					egd.addChoice("Table_distance_unit", SettingsManager.getDistanceUnitNames(),
							tableSettings.getDistanceUnitValue());
					egd.addChoice("Table_intensity_unit", SettingsManager.getIntensityUnitNames(),
							tableSettings.getIntensityUnitValue());
					egd.addChoice("Table_angle_unit", SettingsManager.getAngleUnitNames(),
							tableSettings.getAngleUnitValue());
					egd.addCheckbox("Table_show_fitting_data", tableSettings.getShowFittingData());
					egd.addCheckbox("Table_show_noise_data", tableSettings.getShowNoiseData());
					egd.addCheckbox("Table_show_precision", tableSettings.getShowPrecision());
					egd.addSlider("Table_precision", 0, 10, tableSettings.getRoundingPrecision());
					egd.setSilent(silent);
					egd.showDialog(true, gd);
					if (egd.wasCanceled()) {
						return false;
					}
					tableSettings.setDistanceUnitValue(egd.getNextChoiceIndex());
					tableSettings.setIntensityUnitValue(egd.getNextChoiceIndex());
					tableSettings.setAngleUnitValue(egd.getNextChoiceIndex());
					tableSettings.setShowFittingData(egd.getNextBoolean());
					tableSettings.setShowNoiseData(egd.getNextBoolean());
					tableSettings.setShowPrecision(egd.getNextBoolean());
					tableSettings.setRoundingPrecision((int) egd.getNextNumber());
					return true;
				}

			});
		}
	}

	private void addImageResultsOptions(final ExtendedGenericDialog gd, final Builder resultsSettings) {
		addImageResultsOptions(gd, resultsSettings, (extraOptions) ? FLAG_EXTRA_OPTIONS : 0);
	}

	/**
	 * Adds the image results options.
	 *
	 * <p>
	 * Note: If using the option {@link #FLAG_IMAGE_REMOVE_NONE} then the integer
	 * value returned from the dialog choice should be offset by 1.
	 *
	 * <pre>
	 * ExtendedGenericDialog gd;
	 * ResultsSettings.Builder settings;
	 * ResultsManager.addImageResultsOptions(gd, settings, ResultsManager.FLAG_IMAGE_REMOVE_NONE);
	 * // ...
	 * settings.getResultsImageSettings().setImageTypeValue(gd.getNextChoiceIndex() + 1);
	 * </pre>
	 *
	 * @param gd              the dialog
	 * @param resultsSettings the results settings
	 * @param flags           the flags
	 */
	static void addImageResultsOptions(final ExtendedGenericDialog gd, final Builder resultsSettings, final int flags) {
		if (BitFlagUtils.anyNotSet(flags, FLAG_NO_SECTION_HEADER)) {
			gd.addMessage("--- Image output ---");
		}
		final ResultsImageSettings.Builder imageSettings = resultsSettings.getResultsImageSettingsBuilder();
		final EnumSet<ResultsImageType> requirePrecision = EnumSet.of(
				ResultsImageType.DRAW_LOCALISATIONS_AVERAGE_PRECISION,
				ResultsImageType.DRAW_INTENSITY_AVERAGE_PRECISION);
		final EnumSet<ResultsImageType> requireWeighted = EnumSet.of(ResultsImageType.DRAW_LOCALISATIONS,
				ResultsImageType.DRAW_INTENSITY, ResultsImageType.DRAW_FRAME_NUMBER, ResultsImageType.DRAW_FIT_ERROR);
		String[] names = SettingsManager.getResultsImageTypeNames();
		final int offset = BitFlagUtils.areSet(flags, FLAG_IMAGE_REMOVE_NONE) ? 1 : 0;
		if (offset != 0) {
			names = Arrays.copyOfRange(names, 1, names.length);
		}

		gd.addChoice("Image", names, imageSettings.getImageTypeValue() - offset, new OptionListener<Integer>() {
			@Override
			public boolean collectOptions(Integer field) {
				imageSettings.setImageTypeValue(field + offset);
				return collectOptions(false);
			}

			@Override
			public boolean collectOptions() {
				return collectOptions(true);
			}

			private boolean collectOptions(boolean silent) {
				final ResultsImageType resultsImage = imageSettings.getImageType();
				if (resultsImage.getNumber() <= 0) {
					return false;
				}
				final boolean isExtraOptions = BitFlagUtils.anySet(flags, FLAG_EXTRA_OPTIONS);
				final ExtendedGenericDialog egd = new ExtendedGenericDialog(TITLE, null);
				if (requireWeighted.contains(resultsImage)) {
					egd.addCheckbox("Weighted", imageSettings.getWeighted());
				}
				egd.addCheckbox("Equalised", imageSettings.getEqualised());
				if (requirePrecision.contains(resultsImage)) {
					egd.addSlider("Image_precision (nm)", 5, 30, imageSettings.getAveragePrecision());
				}
				egd.addChoice("Image_size_mode", SettingsManager.getResultsImageSizeModeNames(),
						imageSettings.getImageSizeModeValue());
				final Choice choice = egd.getLastChoice();

				// Selectively show the fields based on the image size mode
				final Label[] labels = new Label[3];
				final Panel[] panels = new Panel[3];
				// Adds a Label and Panel
				egd.addSlider("Image_scale", 1, 15, imageSettings.getScale());
				labels[0] = egd.getLabel();
				panels[0] = egd.getLastPanel();
				// Adds a Label and Panel (panel contains a label)
				egd.addNumericField("Image_size", imageSettings.getImageSize(), 0, 6, "px");
				labels[1] = egd.getLabel();
				panels[1] = egd.getLastPanel();
				// Adds a Label and Panel
				egd.addSlider("Image_pixel_size (nm)", 5, 30, imageSettings.getPixelSize());
				labels[2] = egd.getLabel();
				panels[2] = egd.getLastPanel();

				// Setting each to visible = false does not work without manually revalidating.
				// The alternative is to remove the components and add back the one to draw.
				// This requires storing the constraints of the component when first added
				// to the grid layout or controlling a dedicated display panel.
				// Here the first option is used to simplify supporting any underlying layout.
				// It relies on the layout manager to process and ignore the invisible
				// components.

				final ItemListener l = e -> {
					for (int i = 0; i < panels.length; i++) {
						labels[i].setVisible(false);
						panels[i].setVisible(false);
					}
					final int index = choice.getSelectedIndex();
					labels[index].setVisible(true);
					panels[index].setVisible(true);
					labels[index].revalidate();
					panels[index].revalidate();
				};
				l.itemStateChanged(null);
				choice.addItemListener(l);

				if (isExtraOptions) {
					egd.addNumericField("Image_window", imageSettings.getRollingWindowSize(), 0);
				}
				if (BitFlagUtils.anyNotSet(flags, FLAG_IMAGE_NO_LUT)) {
					egd.addChoice("LUT", LutHelper.getLutNames(), imageSettings.getLutName());
				}
				egd.setSilent(silent);
				egd.showDialog(true, gd);
				if (egd.wasCanceled()) {
					return false;
				}
				if (requireWeighted.contains(resultsImage)) {
					imageSettings.setWeighted(egd.getNextBoolean());
				}
				imageSettings.setEqualised(egd.getNextBoolean());
				if (requirePrecision.contains(resultsImage)) {
					imageSettings.setAveragePrecision(egd.getNextNumber());
				}
				imageSettings.setImageSizeModeValue(egd.getNextChoiceIndex());
				imageSettings.setScale(egd.getNextNumber());
				imageSettings.setImageSize((int) egd.getNextNumber());
				imageSettings.setPixelSize(egd.getNextNumber());
				if (isExtraOptions) {
					imageSettings.setRollingWindowSize((int) egd.getNextNumber());
				}
				if (BitFlagUtils.anyNotSet(flags, FLAG_IMAGE_NO_LUT)) {
					imageSettings.setLutName(egd.getNextChoice());
				}
				return true;
			}
		});
	}

	/**
	 * Adds the in memory results options.
	 *
	 * @param gd              the dialog
	 * @param resultsSettings the results settings
	 */
	static void addInMemoryResultsOptions(final ExtendedGenericDialog gd, final Builder resultsSettings) {
		addInMemoryResultsOptions(gd, resultsSettings, 0);
	}

	/**
	 * Adds the in memory results options.
	 *
	 * @param gd              the dialog
	 * @param resultsSettings the results settings
	 * @param flags           the flags
	 */
	static void addInMemoryResultsOptions(final ExtendedGenericDialog gd, final Builder resultsSettings, int flags) {
		if (BitFlagUtils.anyNotSet(flags, FLAG_NO_SECTION_HEADER)) {
			gd.addMessage("--- Memory output ---");
		}
		final ResultsInMemorySettings.Builder memorySettings = resultsSettings.getResultsInMemorySettingsBuilder();
		gd.addCheckbox("Save_to_memory", memorySettings.getInMemory());
	}

	/**
	 * Add a list of input sources to the generic dialog. The choice field will be
	 * named 'input'. If a file input option is selected then a field will be added
	 * name 'Input_file'.
	 *
	 * <p>
	 * If the source is a memory source then it will not be added if it is empty. If
	 * not empty then a summary of the number of localisation is added as a message
	 * to the dialog.
	 *
	 * @param gd          the dialog
	 * @param inputOption the input option
	 * @param inputs      the inputs
	 */
	static void addInput(ExtendedGenericDialog gd, String inputOption, InputSource... inputs) {
		addInput(gd, INPUT_NAME, inputOption, inputs);
	}

	/**
	 * Add a list of input sources to the generic dialog. The choice field will be
	 * named inputName. If a file input option is selected then a field will be
	 * added name 'Input_file'.
	 *
	 * <p>
	 * If the source is a memory source then it will not be added if it is empty. If
	 * not empty then a summary of the number of localisation is added as a message
	 * to the dialog.
	 *
	 * @param gd          the dialog
	 * @param inputName   the input name
	 * @param inputOption the input option
	 * @param inputs      the inputs
	 */
	static void addInput(ExtendedGenericDialog gd, String inputName, String inputOption, InputSource... inputs) {
		addInput(gd, inputName, inputOption, EMPTY_LOAD_OPTIONS, inputs);
	}

	/**
	 * Add a list of input sources to the generic dialog. The choice field will be
	 * named inputName. If a file input option is selected then a field will be
	 * added name 'Input_file'.
	 *
	 * <p>
	 * If the source is a memory source then it will not be added if it is empty. If
	 * not empty then a summary of the number of localisation is added as a message
	 * to the dialog.
	 *
	 * @param gd           the dialog
	 * @param inputName    the input name
	 * @param inputOption  the input option
	 * @param extraOptions the extra options
	 * @param inputs       the inputs
	 */
	static void addInput(ExtendedGenericDialog gd, String inputName, String inputOption, LoadOption[] extraOptions,
			InputSource... inputs) {
		final Set<String> source = new LinkedHashSet<>();
		String filename = null;
		for (final InputSource input : inputs) {
			addInputSource(source, input);
			if (input == InputSource.FILE) {
				filename = findFileName(extraOptions);
			}
		}
		if (source.isEmpty()) {
			addInputSource(source, InputSource.NONE);
		}

		addInputSourceToDialog(gd, inputName, inputOption, source, filename);
	}

	/**
	 * Add a list of input sources to the generic dialog. The choice field will be
	 * named inputName. If the file input option is true then a field will be added
	 * name 'Input_file'.
	 *
	 * @param gd          the dialog
	 * @param inputName   the input name
	 * @param inputOption The option to select by default
	 * @param source      the source
	 * @param filename    the filename
	 */
	private static void addInputSourceToDialog(final ExtendedGenericDialog gd, String inputName, String inputOption,
			Set<String> source, String filename) {
		final String[] options = source.toArray(new String[0]);
		// Find the option
		inputOption = removeFormatting(inputOption);

		int optionIndex = 0;
		for (int i = 0; i < options.length; i++) {
			final String name = removeFormatting(options[i]);
			if (name.equals(inputOption)) {
				optionIndex = i;
				break;
			}
		}
		final Choice choice = gd.addAndGetChoice(inputName, options, options[optionIndex]);
		if (filename != null) {
			gd.addFilenameField("Input_file", filename);

			// Add a listener to the choice to enable the file input field.
			// Currently we hide the filename field and pack the dialog.
			// We may wish to just disable the fields and leave them there.
			// This could be a user configured option in a global GDSC settings class.
			if (ImageJUtils.isShowGenericDialog()) {
				final Label l = gd.getLastLabel();
				final Panel p = gd.getLastPanel();
				final ItemListener listener = event -> {
					final boolean enable = INPUT_FILE.equals(choice.getSelectedItem());
					if (enable != l.isVisible()) {
						l.setVisible(enable);
						p.setVisible(enable);
						gd.pack();
					}
				};

				// Run once to set up
				listener.itemStateChanged(null);

				choice.addItemListener(listener);
			}
		}
	}

	/**
	 * Remove the extra information added to a name for use in dialogs.
	 *
	 * @param name the formatted name
	 * @return The name
	 */
	private static String removeFormatting(String name) {
		final int index = name.lastIndexOf('[');
		if (index > 0) {
			name = name.substring(0, index - 1);
		}
		return name;
	}

	/**
	 * Add an input source the list. If the source is a memory source then it will
	 * not be added if it is empty. If not empty then a summary of the number of
	 * localisation is added as a message to the dialog.
	 *
	 * @param source the source
	 * @param input  the input
	 */
	private static void addInputSource(Set<String> source, InputSource input) {
		switch (input) {
		case FILE:
			source.add(INPUT_FILE);
			break;

		case MEMORY:
		case MEMORY_MULTI_FRAME:
		case MEMORY_SINGLE_FRAME:
		case MEMORY_CLUSTERED:
		case MEMORY_CATEGORY:
			for (final String name : MemoryPeakResults.getResultNames()) {
				addInputSource(source, MemoryPeakResults.getResults(name), input);
			}
			break;

		default:
			ValidationUtils.checkArgument(input == InputSource.NONE, input);
			source.add(INPUT_NONE);
			break;
		}
	}

	/**
	 * Add a memory input source to the list.
	 *
	 * @param source        the source
	 * @param memoryResults the memory results
	 * @param input         MEMORY_MULTI_FRAME : Select only those results with at
	 *                      least one result spanning frames, MEMORY_CLUSTERED :
	 *                      Select only those results which have at least some IDs
	 *                      above zero (allowing zero to be a valid cluster Id for
	 *                      no cluster)
	 */
	private static void addInputSource(Set<String> source, MemoryPeakResults memoryResults, InputSource input) {
		if (memoryResults.size() > 0) {
			switch (input) {
			case MEMORY_MULTI_FRAME:
				if (!isMultiFrame(memoryResults)) {
					return;
				}
				break;
			case MEMORY_SINGLE_FRAME:
				if (isMultiFrame(memoryResults)) {
					return;
				}
				break;
			case MEMORY_CLUSTERED:
				if (!hasId(memoryResults)) {
					return;
				}
				break;
			case MEMORY_CATEGORY:
				if (!hasCategory(memoryResults)) {
					return;
				}
				break;
			default:
			}
			source.add(getName(memoryResults));
		}
	}

	/**
	 * Get the name of the results for use in dialogs.
	 *
	 * @param memoryResults the memory results
	 * @return The name
	 */
	static String getName(MemoryPeakResults memoryResults) {
		return memoryResults.getName() + " [" + memoryResults.size() + "]";
	}

	/**
	 * Load results from memory using a name from a dialog.
	 *
	 * @param name the name
	 * @return The results
	 */
	private static MemoryPeakResults loadMemoryResults(String name) {
		return MemoryPeakResults.getResults(removeFormatting(name));
	}

	/**
	 * Check for multi-frame results.
	 *
	 * @param memoryResults the memory results
	 * @return True if at least one result spanning frames
	 */
	static boolean isMultiFrame(MemoryPeakResults memoryResults) {
		return memoryResults.forEach((PeakResultProcedureX) result -> result.getFrame() < result.getEndFrame());
	}

	/**
	 * Check for any IDs above zero.
	 *
	 * @param memoryResults the memory results
	 * @return True if any results have IDs above zero
	 */
	static boolean hasId(MemoryPeakResults memoryResults) {
		return memoryResults.forEach((PeakResultProcedureX) result -> result.getId() > 0);
	}

	/**
	 * Check for all IDs above zero.
	 *
	 * @param memoryResults the memory results
	 * @return True if all results have IDs above zero
	 */
	static boolean isId(MemoryPeakResults memoryResults) {
		return memoryResults.forEach((PeakResultProcedureX) result -> result.getId() <= 0);
	}

	/**
	 * Check for any categories above zero.
	 *
	 * @param memoryResults the memory results
	 * @return True if any results have categories above zero
	 */
	static boolean hasCategory(MemoryPeakResults memoryResults) {
		return memoryResults.forEach((PeakResultProcedureX) result -> result.getCategory() > 0);
	}

	/**
	 * All results must be an ExtendedPeakResult.
	 *
	 * @param memoryResults the memory results
	 * @return True if all are an ExtendedPeakResult
	 */
	static boolean isExtended(MemoryPeakResults memoryResults) {
		return memoryResults.forEach((PeakResultProcedureX) result -> !(result instanceof ExtendedPeakResult));
	}

	/**
	 * Gets the name of the next input source from the dialog.
	 *
	 * @param gd the dialog
	 * @return the input source
	 */
	static String getInputSource(GenericDialog gd) {
		final String source = gd.getNextChoice();
		return removeFormatting(source);
	}

	/**
	 * Load the results from the named input option. If the results are not empty
	 * then a check can be made for calibration, and data using the specified units.
	 * If the calibration cannot be obtained or the units are incorrect then the
	 * null will be returned.
	 *
	 * @param inputOption      the input option
	 * @param checkCalibration Set to true to ensure the results have a valid
	 *                         calibration
	 * @param distanceUnit     the required distance unit for the results
	 * @return the results
	 */
	static MemoryPeakResults loadInputResults(String inputOption, boolean checkCalibration, DistanceUnit distanceUnit) {
		return loadInputResults(inputOption, checkCalibration, distanceUnit, null);
	}

	/**
	 * Load the results from the named input option. If the results are not empty
	 * then a check can be made for calibration, and data using the specified units.
	 * If the calibration cannot be obtained or the units are incorrect then the
	 * null will be returned.
	 *
	 * @param inputOption      the input option
	 * @param checkCalibration Set to true to ensure the results have a valid
	 *                         calibration
	 * @param intensityUnit    the required intensity unit for the results
	 * @return the results
	 */
	static MemoryPeakResults loadInputResults(String inputOption, boolean checkCalibration,
			IntensityUnit intensityUnit) {
		return loadInputResults(inputOption, checkCalibration, null, intensityUnit);
	}

	/**
	 * Load the results from the named input option. If the results are not empty
	 * then a check can be made for calibration, and data using the specified units.
	 * If the calibration cannot be obtained or the units are incorrect then the
	 * null will be returned.
	 *
	 * @param inputOption      the input option
	 * @param checkCalibration Set to true to ensure the results have a valid
	 *                         calibration
	 * @param distanceUnit     the required distance unit for the results
	 * @param intensityUnit    the required intensity unit for the results
	 * @param extraOptions     the extra options
	 * @return the results
	 */
	static MemoryPeakResults loadInputResults(String inputOption, boolean checkCalibration, DistanceUnit distanceUnit,
			IntensityUnit intensityUnit, LoadOption... extraOptions) {
		MemoryPeakResults results;
		if (INPUT_NONE.equals(inputOption)) {
			// Do nothing
			return null;
		} else if (INPUT_FILE.equals(inputOption)) {
			IJ.showStatus("Reading results file ...");
			final PeakResultsReader reader = new PeakResultsReader(findFileName(extraOptions));
			IJ.showStatus("Reading " + reader.getFormat() + " results file ...");
			final ResultOption[] options = reader.getOptions();
			if (options.length != 0) {
				collectOptions(reader, options);
			}
			reader.setTracker(SimpleImageJTrackProgress.getInstance());
			results = reader.getResults();
			reader.getTracker().progress(1.0);

			// If the name contains a .tif suffix then create an image source
			if (results != null && results.size() > 0 && results.getName() != null && results.getName().contains(".tif")
					&& results.getSource() == null) {
				final int index = results.getName().indexOf(".tif");
				results.setSource(new IJImageSource(results.getName().substring(0, index)));
			}
		} else {
			results = loadMemoryResults(inputOption);
		}

		try {
			if (results == null) {
				return null;
			}
			if (results.isEmpty()) {
				return results;
			}

			if (checkCalibration && !checkCalibration(results)) {
				return null;
			}
			if (distanceUnit != null && results.getDistanceUnit() != distanceUnit) {
				ImageJUtils.log("Incorrect distance unit: " + results.getDistanceUnit());
				return null;
			}
			if (intensityUnit != null && results.getIntensityUnit() != intensityUnit) {
				ImageJUtils.log("Incorrect intensity unit: " + results.getDistanceUnit());
				return null;
			}
		} finally {
			IJ.showStatus("");
		}
		return results;
	}

	/**
	 * Find the file name.
	 *
	 * @param extraOptions the extra options
	 * @return the filename (or empty)
	 */
	private static String findFileName(LoadOption... extraOptions) {
		for (final LoadOption option : extraOptions) {
			if (option instanceof FilenameLoadOption) {
				return ((FilenameLoadOption) option).getFilename();
			}
		}
		return "";
	}

	private static void collectOptions(PeakResultsReader reader, ResultOption[] options) {
		final GenericDialog gd = new GenericDialog(TITLE);
		gd.addMessage("Options required for file format: " + reader.getFormat().getName());
		for (final ResultOption option : options) {
			if (option.hasValues()) {
				final String[] items = new String[option.values.length];
				for (int i = 0; i < items.length; i++) {
					items[i] = option.values[i].toString();
				}
				gd.addChoice(getOptionName(option), items, option.getValue().toString());
			} else if (option.getValue() instanceof Number) {
				final Number n = (Number) option.getValue();
				if (n.doubleValue() == n.intValue()) {
					gd.addNumericField(getOptionName(option), n.intValue(), 0);
				} else {
					final String value = n.toString();
					int sig = 0;
					int index = value.indexOf('.');
					if (index != -1) {
						// There is a decimal point. Count the digits after it
						while (++index < value.length()) {
							if (!Character.isDigit(value.charAt(index))) {
								// A non-digit character after the decimal point is for scientific notation
								sig = -sig;
								break;
							}
							sig++;
						}
					}
					gd.addNumericField(getOptionName(option), n.doubleValue(), sig);
				}
			} else if (option.getValue() instanceof String) {
				gd.addStringField(getOptionName(option), (String) option.getValue());
			} else if (option.getValue() instanceof Boolean) {
				gd.addCheckbox(getOptionName(option), (Boolean) option.getValue());
			} else {
				IJ.log(TITLE + ": Unsupported reader option: " + option.name + "=" + option.getValue().toString());
			}
		}
		gd.showDialog();
		if (gd.wasCanceled()) {
			return;
		}
		try {
			for (final ResultOption option : options) {
				if (option.hasValues()) {
					option.setValue(option.values[gd.getNextChoiceIndex()]);
				} else if (option.getValue() instanceof Number) {
					final double d = gd.getNextNumber();
					// Convert to the correct type using the String value constructor for the number
					option.setValue(
							option.getValue().getClass().getConstructor(String.class).newInstance(Double.toString(d)));
				} else if (option.getValue() instanceof String) {
					option.setValue(gd.getNextString());
				} else if (option.getValue() instanceof Boolean) {
					option.setValue(gd.getNextBoolean());
				}
			}
			reader.setOptions(options);
		} catch (final Exception ex) {
			// This can occur if the options are not valid
			IJ.log(TITLE + ": Failed to configure reader options: " + ex.getMessage());
		}
	}

	private static String getOptionName(ResultOption option) {
		return option.name.replace(' ', '_');
	}

	/**
	 * Check the calibration of the results exists, if not then prompt for it with a
	 * dialog.
	 *
	 * <p>
	 * Missing calibration is written to the Logger for the class.
	 *
	 * @param results The results
	 * @return True if OK; false if calibration dialog cancelled
	 */
	static boolean checkCalibration(MemoryPeakResults results) {
		// Check for Calibration
		final String msg = (results.hasCalibration()) ? "partially calibrated" : "uncalibrated";
		final CalibrationWriter calibration = results.getCalibrationWriterSafe();

		final Settings settings = Settings.load();
		boolean missing = isEssentialCalibrationMissing(calibration, settings);

		if (missing) {
			logger.info(() -> "Results are " + msg + ". Requesting input.");
			final Rectangle2D.Float dataBounds = results.getDataBounds(null);

			final ExtendedGenericDialog gd = new ExtendedGenericDialog(TITLE);
			ImageJUtils.addMessage(gd, "Results are %s.\nData bounds = (%s,%s) to (%s,%s)", msg,
					MathUtils.rounded(dataBounds.x), MathUtils.rounded(dataBounds.y),
					MathUtils.rounded(dataBounds.y + dataBounds.getWidth()),
					MathUtils.rounded(dataBounds.x + dataBounds.getHeight()));
			gd.addChoice("Calibration_distance_unit", SettingsManager.getDistanceUnitNames(),
					calibration.getDistanceUnitValue());
			gd.addChoice("Calibration_intensity_unit", SettingsManager.getIntensityUnitNames(),
					calibration.getIntensityUnitValue());
			gd.addNumericField("Calibration (nm/px)", calibration.getNmPerPixel(), 2);
			gd.addNumericField("Exposure_time (ms)", calibration.getExposureTime(), 2);
			PeakFit.addCameraOptions(gd, calibration);
			gd.showDialog();
			if (gd.wasCanceled()) {
				return false;
			}
			calibration.setDistanceUnit(SettingsManager.getDistanceUnitValues()[gd.getNextChoiceIndex()]);
			calibration.setIntensityUnit(SettingsManager.getIntensityUnitValues()[gd.getNextChoiceIndex()]);
			calibration.setNmPerPixel(Math.abs(gd.getNextNumber()));
			calibration.setExposureTime(Math.abs(gd.getNextNumber()));
			calibration.setCameraType(SettingsManager.getCameraTypeValues()[gd.getNextChoiceIndex()]);

			gd.collectOptions();

			if (calibration.getCameraType() == CameraType.SCMOS) {
				calibration.clearGlobalCameraSettings();
			}

			missing = isEssentialCalibrationMissing(calibration, settings);

			// Save for next time ...
			settings.inputNmPerPixel = calibration.getNmPerPixel();
			settings.inputExposureTime = calibration.getExposureTime();
			if (calibration.isCcdCamera()) {
				settings.inputGain = calibration.getCountPerPhoton();
			}
			settings.save();

			results.setCalibration(calibration.getCalibration());
		}

		// Only OK if nothing is missing
		return !missing;
	}

	/**
	 * Check for essential calibration settings (i.e. not readNoise, bias, emCCD,
	 * amplification).
	 *
	 * @param calibration the calibration
	 * @param settings    the settings
	 * @return true, if calibration is missing
	 */
	private static boolean isEssentialCalibrationMissing(final CalibrationWriter calibration, Settings settings) {
		final LocalList<String> missing = new LocalList<>();
		if (!calibration.hasNmPerPixel()) {
			missing.add("nm/pixel");
			calibration.setNmPerPixel(settings.inputNmPerPixel);
		}
		if (!calibration.hasExposureTime()) {
			missing.add("Exposure time");
			calibration.setExposureTime(settings.inputExposureTime);
		}
		if (!calibration.hasDistanceUnit()) {
			missing.add("Distance unit");
		}
		if (!calibration.hasIntensityUnit()) {
			missing.add("Intensity unit");
		}

		switch (calibration.getCameraType()) {
		case CCD:
		case EMCCD:
			// Count-per-photon is required for CCD camera types
			if (!calibration.hasCountPerPhoton()) {
				missing.add("Count-per-photon");
			}
			calibration.setCountPerPhoton(settings.inputGain);
			break;
		case SCMOS:
			break;
		case CAMERA_TYPE_NA:
		case UNRECOGNIZED:
		default:
			missing.add("Camera type");
			break;
		}

		if (missing.isEmpty()) {
			return false;
		}

		logger.info(() -> {
			final StringBuilder sb = new StringBuilder(128);
			sb.append("Calibration missing: ");
			for (int i = 0; i < missing.size(); i++) {
				if (i != 0) {
					sb.append("; ");
				}
				sb.append(missing.unsafeGet(i));
			}
			return sb.toString();
		});
		return true;
	}

	/**
	 * Load the results from the named input option.
	 *
	 * @param inputOption the input option
	 * @return the memory peak results
	 */
	private MemoryPeakResults loadResults(String inputOption) {
		LoadOption loadOption = null;
		if (INPUT_FILE.equals(inputOption)) {
			fileInput = true;
			loadOption = new FilenameLoadOption(settings.inputFilename);
		}
		// Only check file input results for calibration. Other results are in memory
		// and have already been loaded or created by analysis.
		return loadInputResults(inputOption, fileInput, null, null, loadOption);
	}

	/**
	 * Batch load a set of results files.
	 */
	List<String> batchLoad() {
		// Adapted from ij.io.Opener.openMultiple

		Java2.setSystemLookAndFeel();
		// run JFileChooser in a separate thread to avoid possible thread deadlocks
		try {
			EventQueue.invokeAndWait(() -> {
				final JFileChooser fc = new JFileChooser();
				fc.setMultiSelectionEnabled(true);
				File dir = null;
				final String sdir = OpenDialog.getDefaultDirectory();
				if (sdir != null) {
					dir = new File(sdir);
				}
				if (dir != null) {
					fc.setCurrentDirectory(dir);
				}
				final int returnVal = fc.showOpenDialog(IJ.getInstance());
				if (returnVal != JFileChooser.APPROVE_OPTION) {
					return;
				}
				omFiles = fc.getSelectedFiles();
				if (omFiles.length == 0) { // getSelectedFiles does not work on some JVMs
					omFiles = new File[1];
					omFiles[0] = fc.getSelectedFile();
				}
				omDirectory = fc.getCurrentDirectory().getPath() + File.separator;
			});
		} catch (final Exception ignored) {
			// Ignore
		}
		if (omDirectory == null) {
			return new ArrayList<>();
		}
		OpenDialog.setDefaultDirectory(omDirectory);
		List<String> dataNames = new ArrayList<>();

		for (final File file : omFiles) {
			final String path = omDirectory + file.getName();
			String DataName = load(path);
			dataNames.add(DataName);
		}
		return dataNames;
	}

	private static String load(String path) {
		// Record this as a single load of the results manager.
		// This should support any dialogs that are presented in loadInputResults(...)
		// to get the calibration.
		if (Recorder.record) {
			Recorder.setCommand("Results Manager");
			Recorder.recordOption("input", INPUT_FILE);
			Recorder.recordOption("input_file", path);
			Recorder.recordOption("image",
					SettingsManager.getResultsImageTypeNames()[ResultsImageType.DRAW_NONE_VALUE]);
			Recorder.recordOption("results_file", "[]");
			Recorder.recordOption("save_to_memory");
		}
		final MemoryPeakResults results = loadInputResults(INPUT_FILE, true, null, null, new FilenameLoadOption(path));
		if (MemoryPeakResults.isEmpty(results)) {
			IJ.error(TITLE, "No results could be loaded from " + path);
		} else {
			if (addResultsToMemory(results, path) && Recorder.record) {
				Recorder.saveCommand();
			}
		}
		return results.getName();
	}

	/**
	 * Adds the results loaded from a file to memory. Performs a check for a results
	 * name and if not present will show a dialog to obtain a name.
	 *
	 * @param results  the results
	 * @param filename the filename
	 * @return true, if successful
	 */
	private static boolean addResultsToMemory(final MemoryPeakResults results, String filename) {
		if (TextUtils.isNullOrEmpty(results.getName())) {
			final ExtendedGenericDialog gd = new ExtendedGenericDialog(TITLE);
			gd.addMessage("Results require a name");
			gd.addStringField("Results_name", FileUtils.removeExtension(FileUtils.getName(filename)));
			gd.showDialog();
			if (gd.wasCanceled()) {
				return false;
			}
			final String name = gd.getNextString();
			if (TextUtils.isNullOrEmpty(name)) {
				return false;
			}
			results.setName(name);
		}
		MemoryPeakResults.addResults(results);
		return true;
	}

	private static boolean saveFile(ResultsFileSettings resultsSettings, MemoryPeakResults source, String fileType,
			String dataPath) {
		// Assume the directory exists
		// String resultsFilename;

		Preferences prefs = Preferences.userNodeForPackage(ResultManager.class);
		prefs.put("defaultDirectory", dataPath);
		try {
			prefs.flush();
		} catch (BackingStoreException e) {
			e.printStackTrace();
		}

		Path outputPath;
		PeakResults results;
		switch (fileType) {
		case "None":
			dataPath = " ";
			// ImageJUtils.log("Localisations are not saved.");
			break;

		case "Peakfit":

			final TextFilePeakResults f = new TextFilePeakResults(dataPath, source.hasDeviations(),
					source.hasEndFrame(), source.hasId(), resultsSettings.getShowPrecision());
			f.setDistanceUnit(resultsSettings.getDistanceUnit());
			f.setIntensityUnit(resultsSettings.getIntensityUnit());
			f.setAngleUnit(resultsSettings.getAngleUnit());
			f.setComputePrecision(true);
			results = f;
			results.copySettings(source);
			results.begin();
			results.addAll(source.toArray());
			results.end();
			final String msg = "Saved " + source.getName() + " to " + dataPath;
			IJ.showStatus(msg);
			ImageJUtils.log(msg);
			break;
		case "DHPSFU":
			StandardResultProcedure s = new StandardResultProcedure(source, DistanceUnit.NM, IntensityUnit.PHOTON);
			s.getTxy();
			s.getZ();
			if (s.z[0] == 0.0) {
				IJ.error(TITLE, "Wrong format: File shoule be in peakfit format.");
				break;
			}
			s.getI();
			int[] frame = s.frame;
			float[] x = s.x;
			// System.out.println(x);
			float[] y = s.y;
			float[] z = s.z;
			float[] intensity = s.intensity;

			List<List<Double>> saveResults = new ArrayList<>();
			int numberOfDataPoints = x.length;

			for (int i = 0; i < numberOfDataPoints; i++) {
				List<Double> rowData = new ArrayList<>();
				rowData.add((double) x[i]);
				rowData.add((double) y[i]);
				rowData.add((double) z[i]);
				rowData.add((double) intensity[i]);
				rowData.add((double) frame[i]);
				saveResults.add(rowData);
			}
			ImageJUtils.log("Saved %s to %s", source.getName(), dataPath);
			outputPath = Paths.get(dataPath);
			try (BufferedWriter writer = Files.newBufferedWriter(outputPath)) {
				for (List<Double> row : saveResults) {
					String csvRow = row.stream().map(Object::toString).collect(Collectors.joining("\t"));
					writer.write(csvRow);
					writer.newLine();
				}
			} catch (IOException e) {
				IJ.error(TITLE, "Error writing to file");
				e.printStackTrace();
			}
			break;
		default:
			throw new IllegalArgumentException("Cannot save the file.");
		}
		return true;
	}

}
