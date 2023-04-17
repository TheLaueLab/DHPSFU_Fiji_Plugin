package uk.ac.sussex.gdsc.smlm.ij.example.plugins;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GUI;
import ij.gui.GenericDialog;
import ij.gui.Overlay;
import ij.gui.PointRoi;
import ij.plugin.PlugIn;
import ij.text.TextPanel;
import ij.text.TextWindow;
import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Choice;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Panel;
import java.awt.Point;
import java.awt.TextField;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import uk.ac.sussex.gdsc.core.ij.ImageAdapter;
import uk.ac.sussex.gdsc.core.ij.gui.ExtendedGenericDialog;
import uk.ac.sussex.gdsc.core.ij.gui.OffsetPointRoi;
import uk.ac.sussex.gdsc.smlm.data.config.UnitHelper;
import uk.ac.sussex.gdsc.smlm.data.config.UnitProtos.DistanceUnit;
import uk.ac.sussex.gdsc.smlm.ij.example.analysis.Localisations;
import uk.ac.sussex.gdsc.smlm.results.MemoryPeakResults;
import uk.ac.sussex.gdsc.smlm.results.PeakResult;
import uk.ac.sussex.gdsc.smlm.results.procedures.XyResultProcedure;
import uk.ac.sussex.gdsc.smlm.results.procedures.XyzrResultProcedure;

/**
 * Overlay localisation results on the source image.
 */
public class OverlayResults implements PlugIn {
  private static final String TITLE = "Overlay Results";

  /**
   * The ROI colors.
   *
   * <p>This is Paul Tol's Vibrant colour scheme (color-blind safe). It has been reordered for
   * contrast between the initial colors. See https://personal.sron.nl/~pault/.
   */
  static final int[][] COLORS = {{0, 119, 187}, // Blue
      {238, 119, 51}, // Orange
      {0, 153, 136}, // Teal
      {204, 51, 17}, // Red
      {51, 187, 238}, // Cyan
      {238, 51, 119}, // Magenta
  };

  /** The ROI options for each named dataset. */
  static final ConcurrentHashMap<String, RoiOptions> ROI_OPTIONS = new ConcurrentHashMap<>();
  /** The option to show the table. */
  static final AtomicBoolean SHOW_TABLE = new AtomicBoolean(true);

  /**
   * Options for the Overlay ROI.
   */
  private static class RoiOptions {
    /** The default instance. */
    static final RoiOptions INSTANCE = new RoiOptions();

    /** The point type. */
    private int pointType = PointRoi.CIRCLE;
    /** The color. */
    private Color color;

    /**
     * Sets the point type.
     *
     * @param pointType the point type
     * @return a reference to this object (for chaining)
     */
    RoiOptions setPointType(int pointType) {
      this.pointType = pointType;
      return this;
    }

    /**
     * Gets the point type.
     *
     * @return the point type
     */
    int getPointType() {
      return pointType;
    }

    /**
     * Sets the color.
     *
     * @param color the color
     * @return a reference to this object (for chaining)
     */
    RoiOptions setColor(Color color) {
      this.color = color;
      return this;
    }

    /**
     * Apply the settings to the ROI.
     *
     * @param roi the roi
     */
    void apply(PointRoi roi) {
      roi.setPointType(pointType);
      roi.setStrokeColor(color);
    }
  }

  /**
   * Worker to respond to selection events and extract selected results.
   */
  private static class SelectionWorker {
    /** The action for the selected results. */
    private final Consumer<List<MemoryPeakResults>> action;
    /** Flag set to true when a selection task is running. */
    private final AtomicBoolean running = new AtomicBoolean();
    /** Cache of sorted results. */
    private final Map<String, MemoryPeakResults> cache = new HashMap<>();
    /** Lock object for synchronization. */
    private final Object lock = new Object();
    /** Currently selected frame. Only update when holding the lock. */
    private int currentFrame;
    /** Currently selected results. Only update when holding the lock. */
    private List<String> currentResults;
    /** Queued frame. Only update when holding the lock. */
    private int nextFrame = 1;
    /** Queued results. Only update when holding the lock. */
    private List<String> nextResults = Collections.emptyList();

    /**
     * Create an instance
     *
     * @param action the action for the selected results
     */
    SelectionWorker(Consumer<List<MemoryPeakResults>> action) {
      this.action = Objects.requireNonNull(action);
    }

    void setFrame(int frame) {
      synchronized (lock) {
        nextFrame = frame;
      }
      update();
    }

    void setResults(List<String> results) {
      synchronized (lock) {
        nextResults = results;
      }
      update();
    }

    /**
     * Force a refresh of the overlay.
     */
    void refresh() {
      synchronized (lock) {
        // Change settings so an update will be processed
        currentFrame = 0;
      }
      update();
    }

    private void update() {
      if (running.compareAndSet(false, true)) {
        ForkJoinPool.commonPool().execute(this::run);
      }
    }

    private void run() {
      try {
        for (;;) {
          int frame;
          List<String> results;
          synchronized (lock) {
            // Check for an update
            frame = nextFrame;
            results = nextResults;
            if (frame == currentFrame && results.equals(currentResults)) {
              // No change
              break;
            }
            // Update the most recently processed values
            currentFrame = frame;
            currentResults = results;
          }
          selectResults(frame, results);
        }
      } finally {
        // Clear the running flag to allow further updates
        running.set(false);
      }
    }

    /**
     * Select the results and send the selection to the output action.
     *
     * @param frame the frame
     * @param resultNames the result names
     */
    private void selectResults(int frame, List<String> resultNames) {
      final List<MemoryPeakResults> data = new ArrayList<>();
      for (final String name : resultNames) {
        // Use a cache of sorted results for a binary search
        final MemoryPeakResults results = cache.computeIfAbsent(name, key -> {
          MemoryPeakResults r = MemoryPeakResults.getResults(name);
          if (r != null) {
            r = r.copy();
            // Sort by frame then (x, y) position.
            // The sort on (x, y) positions uses descending order as the results
            // for the frame are used in descending order.
            r.sort(SelectionWorker::compare);
            return r;
          }
          return null;
        });
        if (results == null) {
          // Results have been cleared from memory (or renamed).
          logError("Results not available", name);
          continue;
        }

        final MemoryPeakResults subset = new MemoryPeakResults();
        subset.copySettings(results);
        for (int i = binarySearch(results, frame); i >= 0; i--) {
          final PeakResult r = results.get(i);
          if (frame != r.getFrame()) {
            break;
          }
          subset.add(r);
        }
        data.add(subset);
      }
      action.accept(data);
    }

    /**
     * Compare the results by frame (ascending), x (descending) then y (descending).
     *
     * @param o1 First result
     * @param o2 Second result
     * @return the comparison result
     */
    private static int compare(PeakResult o1, PeakResult o2) {
      final int f1 = o1.getFrame();
      final int f2 = o2.getFrame();
      if (f1 < f2) {
        return -1;
      }
      if (f1 > f2) {
        return 1;
      }
      final float x1 = o1.getXPosition();
      final float x2 = o2.getXPosition();
      if (x1 > x2) {
        return -1;
      }
      if (x1 < x2) {
        return 1;
      }
      return Float.compare(o2.getYPosition(), o1.getYPosition());
    }

    private static void logError(String msg, String name) {
      IJ.log(String.format("%s Error: %s for results '%s'", TITLE, msg, name));
    }

    /**
     * Conduct a binary search for the specified frame. For convenience this returns the index of
     * the last matching result, or else -1.
     *
     * @param results the results
     * @param frame the frame
     * @return the index
     */
    private static int binarySearch(MemoryPeakResults results, int frame) {
      int lo = 0;
      int hi = results.size() - 1;
      while (lo <= hi) {
        int mid = (lo + hi) >>> 1;
        final int t = results.get(mid).getFrame();
        if (t < frame) {
          lo = mid + 1;
        } else if (t > frame) {
          hi = mid - 1;
        } else {
          // Match. Find the last index.
          while (mid < hi && results.get(mid + 1).getFrame() == frame) {
            mid++;
          }
          return mid;
        }
      }
      return -1;
    }
  }

  /**
   * Worker to show results. Provides a simple mechanism to turn on/off output of results.
   */
  private interface OutputWorker extends Consumer<List<MemoryPeakResults>> {
    /**
     * Start display of the results.
     */
    void show();

    /**
     * Stop display of the results.
     */
    void close();
  }

  /**
   * Worker to overlay results on an image.
   */
  private static class OverlayWorker implements OutputWorker {
    /** The image. */
    private final ImagePlus imp;
    /** The original overlay. */
    private final Overlay originalOverlay;
    /** Flag to indicate the overlay should be displayed. */
    private boolean enabled;

    /**
     * A simple wrapper around resizable arrays of (x,y) values.
     */
    private static final class XYList {
      /** The size. */
      private int size;
      /** The x values. */
      private float[] x = new float[11];
      /** The y values. */
      private float[] y = new float[11];

      /**
       * Adds the pair (a, b) to the list.
       *
       * @param a value a
       * @param b value b
       */
      void add(float a, float b) {
        final int s = size;
        if (s == x.length) {
          resize();
        }
        x[s] = a;
        y[s] = b;
        size = s + 1;
      }

      private void resize() {
        x = Arrays.copyOf(x, (int) Math.min(size * 2L, Integer.MAX_VALUE));
        y = Arrays.copyOf(y, x.length);
      }

      /**
       * Get the number of elements in the list.
       *
       * @return the size
       */
      int size() {
        return size;
      }

      /**
       * Gets a reference to the X data.
       *
       * @return the x data
       */
      float[] getX() {
        return x;
      }

      /**
       * Gets a reference to the Y data.
       *
       * @return the y data
       */
      float[] getY() {
        return y;
      }

      /**
       * Clear the list.
       */
      void clear() {
        size = 0;
      }
    }

    /**
     * Create an instance.
     *
     * @param imp the image
     */
    OverlayWorker(ImagePlus imp) {
      this.imp = imp;
      originalOverlay = imp.getOverlay();
    }

    @Override
    public void accept(List<MemoryPeakResults> data) {
      if (!enabled) {
        return;
      }
      final Overlay overlay = new Overlay();
      final XYList xy = new XYList();
      data.forEach(results -> {
        xy.clear();
        results.forEach(DistanceUnit.PIXEL, (XyResultProcedure) xy::add);
        // Coords are copied in PolygonRoi so we can use the array references from the XY list
        final PointRoi roi = new OffsetPointRoi(xy.getX(), xy.getY(), xy.size());
        getRoiOptions(results.getName()).apply(roi);
        overlay.add(roi);
      });
      imp.setOverlay(overlay);
    }

    /**
     * Gets the ROI options for the selected key. This will return the same options for the key even
     * when it is deselected and then reselected.
     *
     * @param key the key
     * @return the ROI options
     */
    private static RoiOptions getRoiOptions(String key) {
      return ROI_OPTIONS.computeIfAbsent(key, k -> {
        // Get the next colour, or use the default
        final int i = ROI_OPTIONS.size();
        if (i < COLORS.length) {
          final int[] rgb = COLORS[i];
          return new RoiOptions().setColor(new Color(rgb[0], rgb[1], rgb[2]));
        }
        return RoiOptions.INSTANCE;
      });
    }

    @Override
    public void show() {
      enabled = true;
    }

    @Override
    public void close() {
      enabled = false;
      imp.setOverlay(originalOverlay);
    }
  }

  /**
   * Worker to show results in a table.
   */
  private static class TableWorker implements OutputWorker {
    /** The result table. */
    private TextWindow tw;
    /** The saved location. */
    private Point location;

    @Override
    public void show() {
      tw = new TextWindow(TITLE + " Data", "Name\tFrame\tx\ty\tz\tIntensity\tunits", "", 600, 300);
      tw.addWindowListener(new WindowAdapter() {
        @Override
        public void windowClosing(WindowEvent e) {
          // Table was manually closed. Save this as a preference.
          SHOW_TABLE.set(false);
          // This ensures the location is saved
          close();
        }
      });
      if (location != null) {
        tw.setLocation(location);
      }
    }

    /**
     * Sets the initial display location.
     *
     * @param location the location
     */
    void setLocation(Point location) {
      this.location = location;
    }

    @Override
    public void close() {
      if (tw != null) {
        location = tw.getLocation();
        tw.close();
        tw = null;
      }
    }

    /**
     * Checks if the table is visible.
     *
     * @return true if visible
     */
    boolean isVisible() {
      return tw != null && tw.isVisible();
    }

    @Override
    public void accept(List<MemoryPeakResults> data) {
      if (!isVisible()) {
        return;
      }
      final TextPanel tp = tw.getTextPanel();
      tp.clear();
      final StringBuilder sb = new StringBuilder(256);
      data.forEach(results -> {
        final String unit = UnitHelper.getShortName(results.getIntensityUnit());
        results.forEach(DistanceUnit.PIXEL, (XyzrResultProcedure) (x, y, z, r) -> {
          sb.setLength(0);
          sb.append(results.getName()).append('\t').append(r.getFrame()).append('\t').append(x)
              .append('\t').append(y).append('\t').append(z).append('\t').append(r.getIntensity())
              .append('\t').append(unit);
          tp.appendWithoutUpdate(sb.toString());
          // Auto-column width computation occurs at max 10 lines
          if (tp.getLineCount() == 10) {
            tp.updateDisplay();
          }
        });
      });
      tp.updateDisplay();
    }
  }

  /**
   * Shows a list window allowing multiple items to be selected.
   */
  private static class SelectionWindow extends Frame {
    private static final long serialVersionUID = 20230413L;

    /** The list of selected items. */
    private JList<String> list;

    private Button done;
    private Button settings;

    private transient Function<String, String> displayConverter = Function.identity();
    private transient Runnable settingsAction;

    private transient LocalKeyAdapter keyAdapter = new LocalKeyAdapter();

    /**
     * Class to respond to key press events in the window.
     */
    private class LocalKeyAdapter extends KeyAdapter {
      @Override
      public void keyPressed(KeyEvent event) {
        final int keyCode = event.getKeyCode();
        IJ.setKeyDown(keyCode);
        if (keyCode == KeyEvent.VK_ENTER) {
          final Object source = event.getSource();
          if (source == done) {
            close();
          } else if (source == settings) {
            runSettingsAction();
          }
        } else if (keyCode == KeyEvent.VK_ESCAPE) {
          close();
          IJ.resetEscape();
        } else if (keyCode == KeyEvent.VK_W
            && (event.getModifiers() & Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()) != 0) {
          close();
        }
      }
    }

    /**
     * Create an instance.
     *
     * @param title the frame title
     * @param results the results
     */
    SelectionWindow(String title, List<String> results) {
      super(title);
      addKeyListener(keyAdapter);
      add(buildPanel(results));
      this.addKeyListener(keyAdapter);
      pack();
      WindowManager.addWindow(this);
    }

    @Override
    public void processWindowEvent(WindowEvent e) {
      // Relay to window listeners
      super.processWindowEvent(e);
      if (e.getID() == WindowEvent.WINDOW_CLOSING) {
        // Release all resources when closing
        WindowManager.removeWindow(this);
        dispose();
      }
    }

    /**
     * Close the window (set visible to false and release resources).
     */
    void close() {
      processWindowEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
    }

    /**
     * Sets the display converter. This is used to convert the text in the list of items into the
     * text displayed in the dialog. It can be used to decorate the underlying text items.
     *
     * @param displayConverter the display converter
     */
    void setDisplayConverter(Function<String, String> displayConverter) {
      this.displayConverter = displayConverter;
    }

    /**
     * Sets the action to perform when the settings button is clicked.
     *
     * @param action the settings action
     */
    void setSettingsAction(Runnable action) {
      settingsAction = action;
    }

    /**
     * Sets the selection listener called when the selection changes. This will clear any previous
     * selection listeners.
     *
     * @param action the new selection listener
     */
    void setSelectionListener(Consumer<List<String>> action) {
      if (list != null) {
        // Only support one selection listener
        Arrays.stream(list.getListSelectionListeners()).forEach(list::removeListSelectionListener);
        list.addListSelectionListener(e -> {
          if (e.getValueIsAdjusting()) {
            return;
          }
          action.accept(list.getSelectedValuesList());
        });
      }
    }

    /**
     * Gets the selection.
     *
     * @return the selection
     */
    List<String> getSelection() {
      return list != null ? list.getSelectedValuesList() : null;
    }

    /**
     * Builds the main panel for the dialog.
     *
     * @param results the results
     * @return the panel
     */
    private Panel buildPanel(List<String> results) {
      final Panel p = new Panel();
      final BorderLayout layout = new BorderLayout();
      layout.setVgap(3);
      p.setLayout(layout);
      p.add(buildResultsList(results), BorderLayout.CENTER, 0);
      p.add(buildButtonPanel(), BorderLayout.SOUTH, 1);
      return p;
    }

    /**
     * Builds the results list component for the dialog.
     *
     * @param results the results
     * @return the component
     */
    private Component buildResultsList(List<String> results) {
      list = new JList<>(results.toArray(new String[0]));
      list.setCellRenderer(new DefaultListCellRenderer() {
        private static final long serialVersionUID = 1L;

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
            boolean isSelected, boolean cellHasFocus) {
          return super.getListCellRendererComponent(list,
              SelectionWindow.this.mapToDisplay(value.toString()), index, isSelected, cellHasFocus);
        }
      });
      list.addKeyListener(keyAdapter);
      return list;
    }

    /**
     * Map the text in the list to display text.
     *
     * @param value the value
     * @return the display value
     */
    private String mapToDisplay(String value) {
      return displayConverter.apply(value);
    }

    /**
     * Builds the button panel for the dialog.
     *
     * @return the panel
     */
    private Panel buildButtonPanel() {
      final Panel buttons = new Panel();
      buttons.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 0));
      settings = new Button("Settings");
      settings.addActionListener(this::actionPerformed);
      settings.addKeyListener(keyAdapter);
      buttons.add(settings);
      done = new Button("Done");
      done.addActionListener(this::actionPerformed);
      done.addKeyListener(keyAdapter);
      buttons.add(done);
      return buttons;
    }

    /**
     * Respond to button events.
     *
     * @param event the event
     */
    private void actionPerformed(ActionEvent event) {
      final Object source = event.getSource();
      if (source == done) {
        close();
      } else if (source == settings) {
        runSettingsAction();
      }
    }

    private void runSettingsAction() {
      if (settingsAction != null) {
        // Do not run the settings action on the dispatch thread (as it shows a dialog
        // which will halt the thread). Run on a new thread.
        ForkJoinPool.commonPool().submit(settingsAction);
      }
    }

    /**
     * Custom support for Serializable.
     *
     * @param in the object input stream
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws ClassNotFoundException the class not found exception
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
      in.defaultReadObject();
      // Create all instance fields
      displayConverter = Function.identity();
      keyAdapter = new LocalKeyAdapter();
    }
  }

  @Override
  public void run(String arg) {
    if (MemoryPeakResults.isMemoryEmpty()) {
      IJ.error(TITLE, "There are no fitting results in memory");
      return;
    }
    final ImagePlus imp = WindowManager.getCurrentImage();
    if (!showDialog(imp)) {
      return;
    }

    // Consumers for selected results
    final OverlayWorker overlay = new OverlayWorker(imp);
    final TableWorker table = new TableWorker();

    // Worker to select the results and pass them to the consumers
    final SelectionWorker selectionWorker = new SelectionWorker(overlay.andThen(table));

    // Create the window of available results
    final SelectionWindow selectionWindow = new SelectionWindow(TITLE, listResults());
    selectionWindow.setDisplayConverter(OverlayResults::getDisplayName);
    // Use this window to collect settings changes
    selectionWindow.setSettingsAction(() -> {
      if (updateSettings(selectionWindow.getSelection(), table)) {
        selectionWorker.refresh();
      }
    });

    // Place windows next to the input image
    final Point p = imp.getWindow().getLocation();
    p.x += imp.getWindow().getWidth();
    selectionWindow.setLocation(p);
    selectionWindow.setVisible(true);
    p.x -= imp.getWindow().getWidth();
    p.y += imp.getWindow().getHeight();
    table.setLocation(p);

    // Show results
    overlay.show();
    if (SHOW_TABLE.get()) {
      table.show();
    }

    // Push changes to the selected datasets to the worker
    selectionWindow.setSelectionListener(selectionWorker::setResults);

    // Push changes to the selected image to the worker.
    // We cannot register with only the target image, we have to register
    // to all images.
    final ImageAdapter ia = new ImageAdapter() {
      private final int id = imp.getID();

      @Override
      public void imageUpdated(ImagePlus imp) {
        // If this is a change to the target image then refresh
        if (sameImage(imp)) {
          selectionWorker.setFrame(imp.getT());
        }
      }

      @Override
      public void imageClosed(ImagePlus imp) {
        if (sameImage(imp)) {
          selectionWindow.close();
        }
      }

      private boolean sameImage(ImagePlus imp) {
        return imp != null && id == imp.getID();
      }
    };
    ImagePlus.addImageListener(ia);

    // De-register when the selection window is closed. Without this
    // the memory for the plugin objects is not released as the
    // ImageAdaptor is registered to a static instance.
    selectionWindow.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        ImagePlus.removeImageListener(ia);
        table.close();
        overlay.close();
      }
    });
  }

  /**
   * Show a dialog to collect plugin settings for the image.
   *
   * @param imp the image
   * @return true if successful
   */
  private static boolean showDialog(ImagePlus imp) {
    if (imp == null) {
      IJ.noImage();
      return false;
    }
    // If there is 1 frame this may be a mistake
    if (imp.getNFrames() == 1) {
      final GenericDialog gd = new GenericDialog(TITLE);
      // Option to convert stack to a hyperstack of frames
      if (imp.getNDimensions() == 3) {
        gd.addMessage("Stack image contains only 1 frame.\n \n"
            + "Convert stack to frames, or continue without conversion?");
        gd.enableYesNoCancel("Convert", "Continue");
        gd.showDialog();
        if (gd.wasCanceled()) {
          return false;
        }
        if (gd.wasOKed()) {
          // Set CZT with T using the stack size
          imp.setDimensions(1, 1, imp.getStackSize());
        }
      } else {
        gd.addMessage("Image contains only 1 frame.\n \nContinue with overlay?");
        gd.hideCancelButton();
        gd.enableYesNoCancel();
        gd.showDialog();
        if (!gd.wasOKed()) {
          return false;
        }
      }
    }
    return true;
  }

  /**
   * List the results held in memory. Filters the results to those that can be converted to pixels.
   * Without a pixel calibration the overlay positions cannot be created in the correct location.
   *
   * @return the list
   */
  private static List<String> listResults() {
    return MemoryPeakResults.getAllResults().stream()
        .filter(r -> r.getCalibrationReader().getDistanceConverterSafe(DistanceUnit.PIXEL)
            .to() == DistanceUnit.PIXEL)
        .map(MemoryPeakResults::getName).collect(Collectors.toList());
  }

  /**
   * Gets the display name of the named dataset.
   *
   * @param name the dataset name
   * @return the display name
   */
  private static String getDisplayName(String name) {
    final MemoryPeakResults memoryResults = MemoryPeakResults.getResults(name);
    if (memoryResults != null) {
      final StringBuilder sb = new StringBuilder(name.length() + 15);
      sb.append(memoryResults.getName()).append(" [").append(memoryResults.size()).append(']');
      return sb.toString();
    }
    return name;
  }

  /**
   * Update the output settings.
   *
   * <p>This method will show/hide the specified table.
   *
   * @param selection the selection names
   * @param table the table
   * @return true if settings changed
   */
  private static boolean updateSettings(List<String> names, TableWorker table) {
    final ExtendedGenericDialog gd = new ExtendedGenericDialog(TITLE);
    final boolean showTable = table.isVisible();
    gd.addCheckbox("Show_table", showTable);

    // Update the ROI options.
    if (!names.isEmpty()) {
      gd.addMessage("Set ROI color to empty to reset");
      final ArrayList<TextField> textFields = new ArrayList<>(names.size());
      final ArrayList<Choice> choiceFields = new ArrayList<>(names.size());
      // Note: This only lists the current datasets in their specified order
      for (int i = 0; i < names.size(); i++) {
        final String name = names.get(i);
        final RoiOptions roiOptions = ROI_OPTIONS.getOrDefault(name, RoiOptions.INSTANCE);
        textFields.add(gd.addAndGetStringField(name + "_color",
            String.format("#%06x", roiOptions.color.getRGB() & 0xffffff)));
        choiceFields
            .add(gd.addAndGetChoice(name + "_point", PointRoi.types, roiOptions.getPointType()));
      }
      gd.addAndGetButton("Reset ROI options", e -> {
        ROI_OPTIONS.clear();
        textFields.forEach(f -> f.setText(""));
        choiceFields.forEach(f -> f.select(RoiOptions.INSTANCE.getPointType()));
      });
    }

    gd.showDialog();
    if (gd.wasCanceled()) {
      return false;
    }

    final boolean newShowTable = gd.getNextBoolean();
    boolean updated = showTable != newShowTable;
    // Save preference in global settings
    SHOW_TABLE.set(newShowTable);
    if (updated) {
      // Table visibility changed
      if (newShowTable) {
        table.show();
      } else {
        table.close();
      }
    }

    for (int i = 0; i < names.size(); i++) {
      final String name = names.get(i);
      final String rgb = gd.getNextString();
      final int pointType = gd.getNextChoiceIndex();
      if (rgb.isEmpty()) {
        // Reset
        ROI_OPTIONS.remove(name);
      } else {
        // Note that if this method runs in the ForkJoinPool then exceptions are consumed so
        // explicitly display them
        int v;
        try {
          v = Integer.decode(rgb);
        } catch (final NumberFormatException ignored) {
          IJ.error(TITLE, "Invalid color: " + rgb);
          break;
        }
        ROI_OPTIONS.put(name, new RoiOptions().setColor(new Color(v)).setPointType(pointType));
      }
      updated = true;
    }
    // Signal a change occurred
    return updated;
  }

  /**
   * Main method for debugging.
   *
   * <p>For debugging, it is convenient to have a method that starts ImageJ and calls the plugin,
   * e.g. after setting breakpoints.
   *
   * @param args unused
   * @throws URISyntaxException if the URL cannot be converted to a URI
   */
  public static void main(String[] args) throws URISyntaxException {
    // Set the base directory for plugins
    // see: https://stackoverflow.com/a/7060464/1207769
    final Class<OverlayResults> clazz = OverlayResults.class;
    final java.net.URL url = clazz.getProtectionDomain().getCodeSource().getLocation();
    final File file = new File(url.toURI());
    // Note: This returns the base path. ImageJ will find plugins in here that have an
    // underscore in the name. But it will not search recursively through the
    // package structure to find plugins. Adding this at least puts it on ImageJ's
    // classpath so plugins not satisfying these requirements can be loaded.
    System.setProperty("plugins.dir", file.getAbsolutePath());

    // Start ImageJ and exit when closed
    final ImageJ imagej = new ImageJ();
    imagej.exitWhenQuitting(true);

    // If this is in a sub-package or has no underscore then manually add the plugin
    final String packageName = clazz.getName().replace(clazz.getSimpleName(), "");
    if (!packageName.isEmpty() || clazz.getSimpleName().indexOf('_') < 0) {
      // Add a spacer
      ij.Menus.installPlugin("", ij.Menus.PLUGINS_MENU, "-", "", IJ.getInstance());
      ij.Menus.installPlugin(clazz.getName(), ij.Menus.PLUGINS_MENU,
          clazz.getSimpleName().replace('_', ' '), "", IJ.getInstance());
    }

    // Initialise for testing, e.g. create some random datasets
    MemoryPeakResults.addResults(Localisations.createRandomResults("Random1"));
    MemoryPeakResults.addResults(Localisations.createRandomResults("Random2"));
    MemoryPeakResults.addResults(Localisations.createRandomResults("Random3"));
    MemoryPeakResults.addResults(Localisations.createRandomResults("Random4"));
    MemoryPeakResults.addResults(Localisations.createRandomResults("Random5"));

    // Used the known limits of the random data
    final ImagePlus imp = IJ.createImage("Dummy", "8-bit", 100, 50, 1, 1, 10);
    imp.show();
    imp.getCanvas().zoomIn(0, 0);
    imp.getCanvas().zoomIn(0, 0);
    imp.getCanvas().zoomIn(0, 0);
    imp.getCanvas().zoomIn(0, 0);
    GUI.center(imp.getWindow());

    // Run the plugin
    IJ.runPlugIn(clazz.getName(), "");
  }
}
