package uk.ac.cam.dhpsfu.plugins;

import com.google.gson.Gson;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.linear.Array2DRowFieldMatrix;
import org.apache.commons.math3.linear.FieldMatrix;
import org.checkerframework.checker.units.qual.A;
import org.checkerframework.checker.units.qual.C;
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
import uk.ac.sussex.gdsc.core.ij.ImageJUtils;
import uk.ac.sussex.gdsc.core.ij.gui.ExtendedGenericDialog;
import uk.ac.sussex.gdsc.core.utils.SimpleArrayUtils;
import uk.ac.sussex.gdsc.core.utils.TextUtils;
import uk.ac.sussex.gdsc.smlm.ij.plugins.ResultsManager;

import java.awt.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.prefs.Preferences;

import uk.ac.sussex.gdsc.smlm.ij.plugins.SmlmUsageTracker;
import uk.ac.sussex.gdsc.smlm.results.MemoryPeakResults;
import uk.ac.sussex.gdsc.smlm.results.PeakResult;


import java.awt.Checkbox;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;

import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.FloatProcessor;


public class DriftCorrection implements PlugIn {
    private static final String TITLE = "Drift_correction";
    private static boolean memory3D = false;
    private static boolean memoryWL = false;

    private double px_size;
    private double upFactor;
    private double burst;
    private double cycle;
    private static String WL_path;
    private static String threed_path;
    private static String saving_directory = "";

    private String wlName = "";
    private String threedName = "";
    private static String selectedTitle = "";


    private static String threed_memory; //the name that you want to store in the fiji memory

//    private boolean wl_occasional;
//    private boolean correction_twice;
//    private boolean flip_x;
//    private boolean flip_y;
//    private boolean average_drift;
//    private boolean group_burst;
//    private boolean save_dc_wl;
    public static DC_GeneralParas dc_generalParas = new DC_GeneralParas(271,100,20,500);
    public static DC_Paras dc_paras = new DC_Paras(true,true,false,false,false,true,true);



    private Checkbox checkbox;
    private TextField tf;

    private Button btn;


    public boolean showDialog() {
        ExtendedGenericDialog gd = new ExtendedGenericDialog(TITLE);
        gd.addCheckbox("Load .3d file from ImageJ memory", memory3D);
        Checkbox load_3D_file = gd.getLastCheckbox();
        memory3D = load_3D_file.getState();
        Preferences preferences = Preferences.userNodeForPackage(DriftCorrection.class);

        // 3d file input
        gd.addMessage("select .3d File from ImageJ memory:");
        ResultManager.addInput(gd, "--Please Select--", ResultManager.InputSource.MEMORY);
//        String defaultDirectory1 = preferences.get("defaultDirectory", "");
        gd.addFilenameField(".3d_File_directory", "--Please Select--");
        gd.addStringField("File_name", "3D");
//        IJ.log(defaultDirectory1);

        // white light image input
        gd.addCheckbox("Load WL image from opened window", memoryWL);
        Checkbox wl_image = gd.getLastCheckbox();
        memoryWL = wl_image.getState();
        gd.addMessage("White Light image:");
        String[] imageTitles = WindowManager.getImageTitles();
        if(imageTitles.length != 0 ){
            gd.addChoice("Select opened image", imageTitles,"--Please Select--");
        }else{
            gd.addChoice("Select opened image", new String[]{"No Image Opened"},"No Image Opened");
        }

//        String defaultDirectory2 = preferences.get("defaultDirectory", "");
        gd.addFilenameField(".tif_File_directory", "--Please Select--");
//        IJ.log(defaultDirectory2);

        //parameters
        Checkbox white_light_occasional = gd.addAndGetCheckbox("White Light occasional", dc_paras.wl_occasional);
        Checkbox correction_twice = gd.addAndGetCheckbox("Drift correction twice", dc_paras.correction_twice);
        Checkbox flip_x = gd.addAndGetCheckbox("Flip the field of view in x ", dc_paras.flip_x);
        Checkbox flip_y = gd.addAndGetCheckbox("Flip the field of view in y", dc_paras.flip_y);
        Checkbox average_drift = gd.addAndGetCheckbox("Average the drift", dc_paras.average_drift);
        Checkbox group_burst = gd.addAndGetCheckbox("Group burst", dc_paras.group_burst);



        gd.addNumericField("Pixel size(nm)", dc_generalParas.px_size,1);
        gd.addNumericField("Upsampling factor", dc_generalParas.upFactor);
        gd.addNumericField("Number of WL frames taken in each burst", dc_generalParas.burst);
        gd.addNumericField("Number of SR frames in each cycle", dc_generalParas.cycle);

        gd.addMessage("File Output:");
        Checkbox save_DC_WL = gd.addAndGetCheckbox("Save drift corrected WL images", dc_paras.save_DC_WL);
        gd.addDirectoryField("Save_directory", "");

        showInstruction();
        gd.showDialog();

        if (gd.wasCanceled()) {
            return false;
        }
//        wlName = ResultsManager.getInputSource(gd);
        threedName = ResultsManager.getInputSource(gd);
        memory3D = load_3D_file.getState();
        IJ.log("3d: "+ memory3D);
        memoryWL = wl_image.getState();
        IJ.log("wl: "+ memoryWL);

        threed_path = gd.getNextString();
        threed_memory = gd.getNextString();
        selectedTitle = gd.getNextChoice();
        IJ.log(selectedTitle);
        WL_path = gd.getNextString();
        saving_directory = gd.getNextString();



        //update parameters
        dc_paras.setWl_occasional(white_light_occasional.getState());
        dc_paras.setCorrection_twice(correction_twice.getState());
        dc_paras.setFlip_x(flip_x.getState());
        dc_paras.setFlip_y(flip_y.getState());
        dc_paras.setAverage_drift(average_drift.getState());
        dc_paras.setGroup_burst(group_burst.getState());
        dc_paras.setSave_DC_WL(save_DC_WL.getState());

        px_size = gd.getNextNumber();
        upFactor = gd.getNextNumber();
        burst = gd.getNextNumber();
        cycle = gd.getNextNumber();

        dc_generalParas.setPxSize(px_size);
        dc_generalParas.setUpFactor(upFactor);
        dc_generalParas.setBurst(burst);
        dc_generalParas.setCycle(cycle);





//        load_3D_file.addItemListener(e -> {
//            memory1 = load_3D_file.getState();
//            Label file_directory_3d = null;
//
//            Preferences preferences = Preferences.userNodeForPackage(DriftCorrection.class);
//            if(memory1){
//                gd.addMessage("select .3d File from ImageJ memory:");
//                ResultManager.addInput(gd, "", ResultManager.InputSource.MEMORY);
//
//            }else{
//                String defaultDirectory = preferences.get("defaultDirectory", "");
//                gd.addFilenameField(".3d_File_directory", defaultDirectory);
//            }
//            gd.showDialog();
//            gd.pack();
//            Component[] cpn = gd.getComponents();
//            for(Component component : cpn){
//                IJ.log(component.getClass().getSimpleName());
//            }
//        });






        //


        return true;

    }
    public void showInstruction(){
        IJ.log(" -------- Brief Instruction about Drift Correction Plugin ---------");
        IJ.log(" ");
        IJ.log("    *** If \"Load .3d file from ImageJ memory\" is ticked:");
        IJ.log("            -Program will use file selected in \".3d file directory\"");
        IJ.log("            -Else will use saved file in \"Input\" field");
        IJ.log(" ");
        IJ.log("    *** If \"Load WL image from opened window\" is ticked:");
        IJ.log("            -Program will use image selected in \".tif file directory\"");
        IJ.log("            -Else will use saved image in \"Select opened image\" field");
        IJ.log(" ");
        IJ.log("    Once you select \"OK\", it will takes 10-20 seconds to calculate the result");
        IJ.log("    (* Program running time depends on your input data size *)");

    }

    @Override
    public void run(String arg) {
        SmlmUsageTracker.recordPlugin(this.getClass(), arg);
        if(showDialog()){
            showInstruction();
//            IJ.log(selectedTitle);
            if(saving_directory.equals("") && dc_paras.save_DC_WL){
                IJ.error("Please select a place to save your corrected white light image ");
            }else {
                load();
                DCresult dCresult = drift_correction(threed_path, threedName, selectedTitle, WL_path, dc_generalParas, dc_paras);
                ArrayList<double[]> result = dCresult.getData_corrected();
                MemoryPeakResults r = loadResult(arrayListToArray(result), "corrected_result").getResults();
                if (r.size() > 0) {
                    MemoryPeakResults.addResults(r);
                }
                ArrayList<ArrayList<Double>> dbf = dCresult.getDrift_by_frame();
                double[][] dbl = dCresult.getDrift_by_loc();
//            writeCorrected(result,"DataCorrected.txt");
//            writedbf(dbf,"dfb.txt");
//            writedbl(dbl,"dbl.txt");


                viewLoadedResult(r, "corrected_result");
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
                        IJ.log("--- Saving corrected white light image ---");
                        align_wl(WL_path, selectedTitle, dc_paras, dc_generalParas);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
//            checkResult(dCresult.getDrift_by_frame(),"Drift by frame");
//            checkResult(dCresult.getDrift_by_loc(),"Drift by loc");
//            plotTrajectory(dCresult.getDrift_by_frame());

//            IJ.log("drift by frame: " + dbf.size() + ", " + dbf.get(0).size() );
//            IJ.log("drift by loc: " + dbl.length + e", " + dbl[0].length );
//            for(int i = 0; i < 4; i ++){
//                IJ.log(String.valueOf(dbf.get(i).get(0)));
//            }

        }
    }
    //arr[0] = =[x,y,z,intensity,frame]

    public static void writedbf(ArrayList<ArrayList<Double>> a,String filename){
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
            String ext = ((FileNameExtensionFilter)fileChooser.getFileFilter()).getExtensions()[0];

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
    public static void writeCorrected(ArrayList<double[]> a,String filename){
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            for (int i = 0; i < a.size(); i++) {
                writer.write(a.get(i)[0] + "," + a.get(i)[1] + "," +a.get(i)[2] + "," + a.get(i)[3] + "," +a.get(i)[4]);
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void writedbl(double[][] a,String filename){
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            for (int i = 0; i < a.length; i++) {
                writer.write(a[i][0] + "," + a[i][1]);
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static LoadedResult loadResult(double[][]arr, String fileName){
        MemoryPeakResults results = new MemoryPeakResults();
        results.setName(fileName);
//        IJ.log(fileName);

        double[][] returnArray= null;

        if(arr!= null){
            double[] x = ArrayUtils.getColumn(arr,0);
            double[] y = ArrayUtils.getColumn(arr,1);
            double[] z = ArrayUtils.getColumn(arr,2);
            double[] intensity = ArrayUtils.getColumn(arr,3);
            double[] frame = ArrayUtils.getColumn(arr,4);
            returnArray = new double[][]{x, y, z, intensity, frame};

            IJ.log("size" + x.length);

            results.begin();
            for(int i = 0; i < arr.length; i++){
                float[] parameters = new float[5];

                //The value of X in the ith row
                parameters[0] = (float) x[i];

                //The value of Y in the ith row
                parameters[1] = (float) y[i];

                //The value of Z in the ith row
                parameters[2] = (float) z[i];

                //The value of intensity in the ith row
                parameters[3] = (float) intensity[i];

                //The value of frame in the ith row
                parameters[4] = (float) frame[i];

                PeakResult r = new PeakResult((int) parameters[4], parameters[0],parameters[1],parameters[3]);
                r.setZPosition(parameters[2]);
                results.add(r);
            }

        }
        results.end();
        results.sort();

        LoadedResult r = new LoadedResult(results,returnArray);
//        IJ.log("result "+results.get(1).getZPosition());
        return r;

    }


    /**
     * load the .3d file and add the loaded data into imageJ memory
     * @param filePath the .3d file path
     * @param fileName the name of the loaded data which stores in imageJ memory
     * @return
     */
    public static LoadedResult load3DFile(String filePath, String fileName){
        Read3DFileCalib importer = new Read3DFileCalib();
        double [][] data = null;
        double[][] returnArray = null;

//        MemoryPeakResults results = new MemoryPeakResults();
//        results.setName(fileName);
//        IJ.log(fileName);

        String fileExtension = getFileExtension(filePath);
        if(fileExtension.equals(".3d")){
            try{
                data = importer.readCSVDouble(Paths.get(filePath),0);
            }catch (IOException e){
                e.printStackTrace();
            }

        }else{
            IJ.error(TITLE,"You must select a .3d file");
        }

       return loadResult(data,fileName);
    }

    /**
     * load the 3d file and show the data using result table
     */
    public static void load(){
        MemoryPeakResults threed_result = load3DFile(threed_path,threed_memory).getResults();
        if(threed_result == null){
            return;
        }
        if (threed_result.isEmpty()) {
            IJ.error(TITLE, "No localisations could be loaded");
            return;
        }

        // Create the in-memory results
        if (threed_result.size() > 0) {
            MemoryPeakResults.addResults(threed_result);
        }

        final String msg = "Loaded " + TextUtils.pleural(threed_result.size(), "lines");
        IJ.showStatus(msg);
        ImageJUtils.log(msg);

        viewLoadedResult(threed_result,"Loaded_3d_file");

    }
    public static double[][] resultToArray(MemoryPeakResults results){
        if (MemoryPeakResults.isEmpty(results)) {
            IJ.error(TITLE, "No results could be loaded");
//            return new double[][]{{}};
        }
        int size = results.size();
        double[][] r = new double[5][size];

        for(int i = 0; i < size; i++){
            r[0][i] = results.get(i).getXPosition();
            r[1][i] = results.get(i).getYPosition();
            r[2][i] = results.get(i).getZPosition();
            r[3][i] = results.get(i).getIntensity();
            r[4][i] = results.get(i).getFrame();
        }
        return r;

    }

    /**
     *
     * @param results the data that you want to show in the table
     */
    public static void viewLoadedResult(MemoryPeakResults results,String windowTitle){
        if (MemoryPeakResults.isEmpty(results)) {
            IJ.error(TITLE, "No results could be loaded");
            return;
        }
        int size = results.size();
        int[] frame = new int[size];
        float[] x =new float[size];
        float[] y = new float[size];
        float[] z = new float[size];
        float[] intensity = new float[size];

        for(int i = 0; i < size; i++){
            frame[i] = results.get(i).getFrame();
            x[i] = results.get(i).getXPosition();
            y[i] = results.get(i).getYPosition();
            z[i] = results.get(i).getZPosition();
            intensity[i] = results.get(i).getIntensity();
        }

        if(frame == null){
            IJ.log("abcd"+ frame.length);
        }

        ResultsTable t = new ResultsTable();
        t.setValues("Frame", SimpleArrayUtils.toDouble(frame));
        t.setValues("Frame", new double[]{1, 2, 3, 4, 5});
        t.setValues("X", SimpleArrayUtils.toDouble(x));
        t.setValues("Y", SimpleArrayUtils.toDouble(y));
        t.setValues("Z", SimpleArrayUtils.toDouble(z));
        t.setValues("Intensity", SimpleArrayUtils.toDouble(intensity));
        t.show(windowTitle);

    }
    private  static void  checkResult(ArrayList<ArrayList<Double>> r,String title){
        ResultsTable t = new ResultsTable();
        double[] x = new double[r.size()];
        double[] y = new double[r.size()];
        for(int i =0 ;i < r.size(); i++){
            x[i] = r.get(i).get(0);
            y[i] = r.get(i).get(1);
        }
        t.setValues("X",x);
        t.setValues("Y",y);
        t.setPrecision(8);
        t.show(title);
    }
    private  static void  checkResult(double[][] r,String title){
        ResultsTable t = new ResultsTable();
        double[] x = new double[r.length];
        double[] y = new double[r.length];
        for(int i =0 ;i < r.length; i++){
            x[i] = r[i][0];
            y[i] = r[i][1];
        }
        t.setValues("X",x);
        t.setValues("Y",y);
        t.setPrecision(8);
        t.show(title);
    }
    public static String getFileExtension(String filePath){
        int lastIndex = filePath.lastIndexOf(".");
        if (lastIndex == -1) {
            return ""; // empty extension
        }
        return filePath.substring(lastIndex);
    }

    public static int[][][] loadtifFile(String wl_fpath, boolean window){
        ImagePlus imp;
        if(!getFileExtension(wl_fpath).equals(".tif")){
            IJ.error(TITLE,"You must select a .tif file");
        }
        if(window){
            imp = WindowManager.getImage(wl_fpath);
        }else{
            imp = new ImagePlus(wl_fpath);
        }

        int width = imp.getWidth();
        int height = imp.getHeight();
        int slices = imp.getStackSize();
//        imp.show();

        // Create a 3D array to hold the pixel data
        int[][][] array3D = new int[slices][width][height];

        // Iterate through the slices and populate the 3D array
        for (int slice = 1; slice <= slices; slice++) {
            ImageProcessor ip = imp.getStack().getProcessor(slice);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    array3D[slice-1][y][x] = ip.getPixel(x, y);
                }
            }
        }
        return array3D;
    }

    public static Complex[][] ftPad(Complex[][] imFT, int[] outsize){

        // the shape of this 2d array
        int row = imFT.length;
        int col = imFT[0].length;
        int[] nin = {row,col};

        //fft shift for imFT
        imFT = Fftshift.fftShift2D(imFT);
        int[] center = {imFT.length/2, imFT[0].length/2};

        //initialise imFTout, each element in this array is zero
        //numpy.zeros()
        Complex[][] imFTout = new Complex[outsize[0]][outsize[1]];
        for(int i = 0; i < imFTout.length; i++){
            for(int j = 0; j < imFTout[0].length; j++){
                imFTout[i][j] = new Complex(0,0);
            }
        }
        int[] centerOut = {imFTout.length/2, imFTout[0].length/2};

        int[] cenout_cen = new int[centerOut.length];
        for(int i = 0; i < cenout_cen.length; i++){
            cenout_cen[i] = centerOut[i]-center[i];
        }
        int im_max1 = Math.max(cenout_cen[0] + 1, 1) - 1;
        int im_min1 = Math.min(cenout_cen[0] + nin[0], outsize[0]);

        int im_max2 = Math.max(cenout_cen[1] + 1, 1) - 1;
        int im_min2 = Math.min(cenout_cen[1] + nin[1], outsize[1]);

        int im_max3 = Math.max(-cenout_cen[0] + 1, 1) - 1;
        int im_min3 = Math.min(-cenout_cen[0] + outsize[0], nin[0]);

        int im_max4 = Math.max(-cenout_cen[1] + 1, 1) - 1;
        int im_min4 = Math.min(-cenout_cen[1] + outsize[1], nin[1]);

        // copy the relevant region from imFT to imFTout
        for (int i = im_max1; i < im_min1; i++) {
            for (int j = im_max2; j < im_min2; j++) {
                imFTout[i][j] = imFT[im_max3 + (i - im_max1)][im_max4 + (j - im_max2)];
            }
        }

        // perform inverse shift and scale
        imFTout = Fftshift.ifftShift2D(imFTout);
        DecimalFormat df = new DecimalFormat("#.########");

        for (int i = 0; i < imFTout.length; i++) {
            for (int j = 0; j < imFTout[i].length; j++) {
                imFTout[i][j] = imFTout[i][j].multiply((double) outsize[0] * outsize[1] / (nin[0] * nin[1]));
                double real = Double.parseDouble(df.format(imFTout[i][j].getReal()));
                double imaginary = Double.parseDouble(df.format(imFTout[i][j].getImaginary()));
                imFTout[i][j] = new Complex(real, imaginary);
            }
        }
        return imFTout;
    }
    public static Complex[][] dftups(Complex[][] inn, int nor, int noc, int usfac, int roff, int coff){
        int nr = inn.length;
        int nc = inn[0].length;

        double[] fftpara = new double[nc];
        for(int i = 0; i < nc; i++){
            fftpara[i] = i;
        }
        fftpara = Fftshift.ifftShift1D(fftpara);
        //kernc_1
        Complex[] kernc_1 = new Complex[nc];
        for (int i = 0; i < nc; i++) {
            kernc_1[i] = new Complex(0, -2 * Math.PI / (nc * usfac)).multiply(fftpara[i] - Math.floor((double) nc / 2));
        }
        //reshape kernc_1
        Complex[][] reshaped_kernc_1 = new Complex[1][kernc_1.length];
        System.arraycopy(kernc_1, 0, reshaped_kernc_1[0], 0, kernc_1.length);

        //kernc_2
        double[] kernc_2 = new double[noc];
        for (int i = 0; i < noc; i++) {
            kernc_2[i] = i-coff;
        }
        //reshape kernc_2
        double[][] reshaped_kernc_2 = reshape(kernc_2.length,1,kernc_2);


        //multiplication between Complex and double
        int rows = reshaped_kernc_2.length;  // Assuming 2D array
        int cols = reshaped_kernc_1[0].length;  // Assuming non-empty 2D array

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
        //exp
        for (int i = 0; i < result.getRowDimension(); i++) {
            for (int j = 0; j < result.getColumnDimension(); j++) {
                Complex value = result.getEntry(i, j);
                double realPart = value.getReal();
                double imaginaryPart = value.getImaginary();
                double expRealPart = Math.exp(realPart);
                Complex expComplex = new Complex(expRealPart * Math.cos(imaginaryPart), expRealPart * Math.sin(imaginaryPart));
                result.setEntry(i, j, expComplex);
            }
        }
        //kernc
        FieldMatrix<Complex> kerncM = result.transpose();
        Complex[][] kernc = matrixToArray(kerncM);

        //kernr_1
        double[] arange0nor = new double[nor];
        for(int i = 0; i < nor; i++){
            arange0nor[i] = i;
        }
        Complex[] kernr_1 = new Complex[nor];
        for (int i = 0; i < nor; i++) {
            kernr_1[i] = new Complex(0, -2 * Math.PI / (nr * usfac)).multiply(arange0nor[i] - roff);
        }
        //reshape kernr_1
        Complex[][] reshaped_kernr_1 = new Complex[1][kernr_1.length];
        System.arraycopy(kernr_1, 0, reshaped_kernr_1[0], 0, kernr_1.length);

        //kernr_2
        double[] arange0nr = new double[nr];
        for(int i = 0; i < nr; i++){
            arange0nr[i] = i;
        }
        arange0nr = Fftshift.ifftShift1D(arange0nr);
        double[] kernr_2 = new double[nr];
        for(int i = 0; i < nr; i++){
            kernr_2[i] = arange0nr[i] - Math.floor((double) nr/2);
        }
        //reshape kernr_2
        double[][] reshaped_kernr_2 = reshape(kernr_2.length,1,kernr_2);

        //multiplication between Complex and double
        int nrrows = reshaped_kernr_2.length;  // Assuming 2D array
        int nrcols = reshaped_kernr_1[0].length;  // Assuming non-empty 2D array

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
        //exp
        for (int i = 0; i < nrresult.getRowDimension(); i++) {
            for (int j = 0; j < nrresult.getColumnDimension(); j++) {
                Complex value = nrresult.getEntry(i, j);
                double realPart = value.getReal();
                double imaginaryPart = value.getImaginary();
                double expRealPart = Math.exp(realPart);
                Complex expComplex = new Complex(expRealPart * Math.cos(imaginaryPart), expRealPart * Math.sin(imaginaryPart));
                nrresult.setEntry(i, j, expComplex);
            }
        }
        //kernc
        FieldMatrix<Complex> kernrM = nrresult.transpose();

        //
        Complex[][] kernr = matrixToArray(kernrM);

        // dot(kernr,inn)
        int finalrow1 = kernr.length;  // Assuming 2D array
        int finalcol1 = inn[0].length;  // Assuming non-empty 2D array

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

        //dot(interResult,kernc)
        int r = interResult.length;
        int c = kernc[0].length;

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

        reformatComplex(out);
        return out;

    }

    public static Complex[][] matrixToArray(FieldMatrix<Complex> matrix) {
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

    public static Dftresult dftregistration(Complex[][] buf1ft, Complex[][] buf2ft, int usfac) {
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

        Complex ccMax = new Complex(0,0);
        double ccmax = 0;


        if (usfac == 0) {
//            ccMax = new Complex(0, 0);
            for (int i = 0; i < buf1ft.length; i++) {
                for (int j = 0; j < buf1ft[i].length; j++) {
                    Complex conj = buf2ft[i][j].conjugate(); // Take the conjugate of buf2ft[i][j]
                    Complex product = buf1ft[i][j].multiply(conj); // Multiply with buf1ft[i][j]
                    ccMax = ccMax.add(product); // Add to the sum
                }
            }
            row_shift = 0;
            col_shift = 0;


        } else if (usfac == 1) {
            Complex[][] cc = new Complex[buf1ft.length][buf1ft[0].length];
            for (int i = 0; i < buf1ft.length; i++) {
                for (int j = 0; j < buf1ft[i].length; j++) {
                    cc[i][j] = buf1ft[i][j].multiply((buf2ft[i][j]).conjugate());
                }
            }
            double[][] doubleComplex = convertToInterleaved(cc);
            DoubleFFT_2D fft2 = new DoubleFFT_2D(doubleComplex.length, doubleComplex[0].length/2);
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

            ccMax = cc[(int) row_shift][(int)col_shift].multiply(nc * nr);
            row_shift = Nr[(int)row_shift];
            col_shift = Nc[(int)col_shift];


        } else if (usfac > 1) {
            Complex[][] interCC = new Complex[buf1ft.length][buf1ft[0].length];
            for (int i = 0; i < buf1ft.length; i++) {
                for (int j = 0; j < buf1ft[i].length; j++) {
                    interCC[i][j] = buf1ft[i][j].multiply((buf2ft[i][j]).conjugate());
                }
            }
            interCC = ftPad(interCC, new int[]{2 * nr, 2 * nc});
            double[][] doubleComplex = convertToInterleaved(interCC);
            DoubleFFT_2D fft2 = new DoubleFFT_2D(doubleComplex.length, doubleComplex[0].length/2);
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
                        interCC1[i][j] = buf2ft[i][j].multiply((buf1ft[i][j]).conjugate());
                    }
                }
                row_shift = (double) (Math.round(row_shift * usfac)) / usfac;
                col_shift = (double) (Math.round(col_shift * usfac)) / usfac;
                double dftshift = (Math.ceil(usfac * 1.5)) / 2;
                //np.fix
                dftshift = (dftshift < 0) ? Math.ceil(dftshift) : Math.floor(dftshift);

//                Complex[][] newCC = new Complex[interCC1.length][interCC1[0].length];

                Complex[][] interCC11 = dftups(interCC1, (int) Math.ceil(usfac * 1.5), (int) Math.ceil(usfac * 1.5), usfac, (int) (dftshift - row_shift * usfac), (int) (dftshift - col_shift * usfac));
                Complex[][] newCC = new Complex[interCC11.length][interCC11[0].length];
                for (int i = 0; i < newCC.length; i++) {
                    for (int j = 0; j < newCC[i].length; j++) {
                        newCC[i][j] = interCC11[i][j].conjugate();
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

                row_shift = row_shift + (double) (rloc)/usfac;
                col_shift = col_shift + (double) (cloc)/usfac;

            }
        }

        if(nr == 1){
            row_shift = 0;
        }

        if(nc == 1){
            col_shift = 0;
        }
        double rg00 = compute00(buf1ft);
        double rf00 = compute00(buf2ft);
        double error = 0;
        double diffphase;
        if(Objects.equals(ccMax, new Complex(0, 0))){
            //cc max is double
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
        }else{
            // ccmax is complex
            error = 1 - (ccMax.abs() * ccMax.abs()) / (rg00 * rf00);
            error = Math.sqrt(Math.abs(error));
            diffphase = Math.atan2(ccMax.getImaginary(), ccMax.getReal());
        }
        double[] output = {error,diffphase,row_shift,col_shift};
        Complex[][] greg = new Complex[buf2ft.length][buf2ft[0].length];
        if(usfac > 0){
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
        DecimalFormat df = new DecimalFormat("#.########");

        for (int i = 0; i < greg.length; i++) {
            for (int j = 0; j < greg[i].length; j++) {
                double real = Double.parseDouble(df.format(greg[i][j].getReal()));
                double imaginary = Double.parseDouble(df.format(greg[i][j].getImaginary()));
                greg[i][j] = new Complex(real, imaginary);
            }
        }

        return new Dftresult(output,greg);

    }
    public static double[][][] division3dArray(int[][][] input_array, int factor){
        double[][][] out = new double[input_array.length][input_array[0].length][input_array[0][0].length];
        DecimalFormat df = new DecimalFormat("#.########");
        for(int i = 0; i < input_array.length; i++){
            for(int j = 0; j < input_array[0].length; j ++){
                for(int k = 0; k < input_array[0][0].length; k++){
                    out[i][j][k] = Double.parseDouble(df.format((double) input_array[i][j][k] / factor));
                }
            }
        }
        return out;
    }

    public static double[][] division2dArray(int[][] input_array, int factor){
        double[][] out = new double[input_array.length][input_array[0].length];
        DecimalFormat df = new DecimalFormat("#.########");
        for(int i = 0; i < input_array.length; i++){
            for(int j = 0; j < input_array[0].length; j ++){
                out[i][j] = Double.parseDouble(df.format((double) input_array[i][j] / factor));
            }
        }
        return out;
    }
    public static int findMax3dArray(int[][][] input_array){
        int maxVal = 0;
        for(int i = 0; i < input_array.length; i++){
            for(int j = 0; j < input_array[0].length; j ++){
                for(int k = 0; k < input_array[0][0].length; k++){
                    maxVal = Math.max(maxVal, input_array[i][j][k]);
                }
            }
        }
        return maxVal;
    }

    public static int findMax2dArray(int[][] input_array){
        int maxVal = 0;
        for(int i = 0; i < input_array.length; i++){
            for(int j = 0; j < input_array[0].length; j ++){
                maxVal = Math.max(maxVal, input_array[i][j]);
            }
        }
        return maxVal;
    }

    //normalise data type for 3d array
    public static double[][][] normalise_dtype(int[][][] input_array, boolean auto_range, int range_max){
        double[][][] out = new double[input_array.length][input_array[0].length][input_array[0][0].length];
        if(!auto_range){
            out = division3dArray(input_array,range_max);
        }else{
            if(checkDtype(input_array).equals("uint16")){
                out = division3dArray(input_array,65535);
            } else if (checkDtype(input_array).equals("uint8")) {
                out = division3dArray(input_array,255);
            }else{
                int max = findMax3dArray(input_array);
                out = division3dArray(input_array,max);
            }

        }
        return out;
    }

    //normalise data type for 2d array
    public static double[][] normalise_dtype(int[][] input_array, boolean auto_range, int range_max){
        double[][] out = new double[input_array.length][input_array[0].length];
        if(!auto_range){
            out = division2dArray(input_array,range_max);
        }else{
            if(checkDtype(input_array).equals("uint16")){
                out = division2dArray(input_array,65535);
            } else if (checkDtype(input_array).equals("uint8")) {
                out = division2dArray(input_array,255);
            }else{
                int max = findMax2dArray(input_array);
                out = division2dArray(input_array,max);
            }

        }
        return out;
    }
    public static double[][] realToComplex(double[][] a){
        double[][] complexData = new double[a.length][2 * a[0].length];
        for (int i = 0; i < a.length; i++) {
            for (int j = 0; j < a[0].length; j++) {
                complexData[i][2 * j] = a[i][j];     // real part
                complexData[i][2 * j + 1] = 0.0;       // imaginary part set to 0
            }
        }
        return complexData;

    }

    public static DCresult drift_correction(String threed_path, String threed_name, String selectedTitle, String wl_path, DC_GeneralParas gp, DC_Paras paras){

        double[][] threed_data;
        if(!memory3D){
            IJ.log("load from file" + threed_path);
            threed_data = load3DFile(threed_path,threed_name).getResultArray();
        }else{
            IJ.log("load from memory" + threed_name);
            MemoryPeakResults r = ResultsManager.loadInputResults(threed_name,false,null,null);
            threed_data = resultToArray(r);
        }
        int[][][] wlArray;
        if(!memoryWL){
            IJ.log("load from file" + wl_path);
            wlArray = loadtifFile(wl_path,false);

        }else{
            IJ.log("load from window" + selectedTitle);
            wlArray = loadtifFile(selectedTitle,true);

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
        //in data_arr, one column contains all xyz intensity frame
        // data_arr[i] = [x,y,z,intensity,frame]
        double[][] data_arr = new double[threed_data[0].length][5];
        for(int i = 0; i < threed_data[0].length; i ++){
            data_arr[i] = new double[] {threed_data[0][i],threed_data[1][i],threed_data[2][i],threed_data[3][i],threed_data[4][i]};
        }

        // find the position of first frame
        //sometimes there's multiple first frame, with different xyz value
        ArrayList<Integer> locs1 = new ArrayList<Integer>();
        double min = 0;
        for(int i =0; i < threed_data[4].length; i ++){
            if (min > threed_data[4][i]){
                min = threed_data[4][i];
                locs1.add(i);
            }
        }
        ArrayList<double[]> data_corrected = new ArrayList<>();
        ArrayList<ArrayList<Double>> drift_by_frame = new ArrayList<>();
//        double[][] data_corrected = new double[locs1.size()][5];
        for(int i = 0; i < locs1.size(); i ++){
            data_corrected.add( data_arr[locs1.get(i)]);
        }

        if(!paras.wl_occasional) {
//            int[][][] wlArray = loadtifFile(wl_path);
            double[][] f = normalise_dtype(wlArray[0], true, 4096);


            ArrayList<double[]> shifts = new ArrayList<>();
            ArrayList<Double> xxx = new ArrayList<>();
            ArrayList<Double> yyy = new ArrayList<>();

            double maxVal = getMax(threed_data[4]);

            for(int i =1 ; i < maxVal; i++){
                // get the current frame
                ArrayList<Integer> locs = new ArrayList<>();
                for(int j = 0; j < threed_data[4].length; j ++){
                    if(threed_data[4][j] == i){
                        locs.add(j);
                        if(threed_data[4][j+1] > i){
                            break;
                        }
                    }
                }
                double[][] g = normalise_dtype(wlArray[i-1],true,4096);
                double[][] newG = realToComplex(g);
                double[][] newF = realToComplex(f);

                DoubleFFT_2D fft2 = new DoubleFFT_2D(newG.length, newG[0].length);
                fft2.complexForward(newG);
                fft2.complexForward(newF);

                Dftresult r = dftregistration(convertToComplex(newF),convertToComplex(newG),(int) gp.upFactor);
                shifts.add(new double[]{i,r.getOutput()[3],r.getOutput()[2]});
                double xx = r.getOutput()[3] * gp.px_size;
                double yy = r.getOutput()[2] * gp.px_size;
                xxx.add(xx);
                yyy.add(yy);
                //TODO: check the data type of corr_x and corr_y
                //TODO: corr_X and cprr_y are arrays of doubles
                double[][] data_to_correct = new double[locs.size()][5];
                for(int j = 0; j < locs.size(); j++){
                    data_to_correct[j] = data_arr[locs.get(j)];
                }
                double[] corr_x = new double[data_to_correct.length];

                if(paras.flip_x){
                    for(int j = 0; j < corr_x.length; j ++){
                        corr_x[j] = data_to_correct[j][0] - xx;
                    }
                }else {
                    for(int j = 0; j < corr_x.length; j ++){
                        corr_x[j] = data_to_correct[j][0] + xx;
                    }
                }
                double[] corr_y = new double[data_to_correct.length];

                if(paras.flip_y){
                    for(int j = 0; j < corr_y.length; j ++){
                        corr_y[j] = data_to_correct[j][1] - yy;
                    }
                }else{
                    for(int j = 0; j < corr_y.length; j ++){
                        corr_y[j] = data_to_correct[j][1] + yy;
                    }
                }
                double[][] corr_x_reshape = reshape(corr_x.length,1,corr_x);
                double[][] corr_y_reshape = reshape(corr_y.length,1,corr_y);
                //TODO: here slice_data is data_to_correct[:,2:]
                //TODO: total 5 cols and get each element from the third column(2)
                //TODO: get the last 3 columns each row
                //TODO: this part of code is extract to a single function
//                double[][] slice_data = new double[data_to_correct.length][3];
//                for(int j = 0; j < data_to_correct.length; j ++){
//                    for(int k = 2,a = 0 ; k < 5; k ++, a ++){
//                        slice_data[j][a] = data_to_correct[j][k];
//                    }
//                }
                double[][] slice_data = arraySliceCol2d(data_to_correct,2);
                double[][] corrected = new double[slice_data.length][slice_data[0].length +
                                                        corr_x_reshape[0].length + corr_y_reshape[0].length];
                for(int j = 0; j < corrected.length; j++){
                    corrected[j][0] = corr_x_reshape[j][0];
                    corrected[j][1] = corr_y_reshape[j][0];
                    corrected[j][2] = slice_data[j][0];
                    corrected[j][3] = slice_data[j][1];
                    corrected[j][4] = slice_data[j][2];

                }
                for(int j = 0; j < corrected.length; j ++){
                    data_corrected.add(corrected[j]);
                }
            }
            for(int j = 0; j < xxx.size(); j ++){
                ArrayList<Double> a = new ArrayList<>();
                a.add(xxx.get(j));
                a.add(yyy.get(j));
                drift_by_frame.add(a);
            }

        } else if (paras.wl_occasional && !paras.correction_twice && paras.average_drift) {
//            int[][][] wlArray = loadtifFile(wl_path);
            double[][] f1 = normalise_dtype(wlArray[0], true, 4096);
            ArrayList<Double> fxx = zeros1col(gp.burst);
            ArrayList<Double> fyy = zeros1col(gp.burst);

            for(int i = 1; i < gp.burst; i ++ ){
                double[][] f = normalise_dtype(wlArray[i], true, 4096);
                double[][] newF = realToComplex(f);
                double[][] newF1 = realToComplex(f1);

                DoubleFFT_2D fft2 = new DoubleFFT_2D(f1.length, f1[0].length);
                fft2.complexForward(newF);
                fft2.complexForward(newF1);
                Dftresult r = dftregistration(convertToComplex(newF1),convertToComplex(newF),(int) gp.upFactor);
                fxx.set(i,r.getOutput()[3]);
                fyy.set(i,r.getOutput()[2]);
            }
            double avFxx = getMean(fxx);
            double avFyy = getMean(fyy);

            double max = getMax(threed_data[4]);
//            for (double[] doubles : data_arr) {
//                if (doubles[4] > max) {
//                    max = doubles[4];
//                }
//            }
            ArrayList<Double> avGxx = zeros1col(Math.ceil(max / gp.cycle) +1 );
            avGxx.add(avFxx);
            ArrayList<Double> avGyy = zeros1col(Math.ceil(max / gp.cycle) +1 );
            avGyy.add(avFyy);

            ArrayList<double[]> xxx = new ArrayList<>();
            ArrayList<double[]> yyy = new ArrayList<>();

            for(int i = 1; i < Math.ceil(max / gp.cycle) +1 ; i ++){
                ArrayList<Double> Gxx = zeros1col(gp.burst);
                ArrayList<Double> Gyy = zeros1col(gp.burst);

                for(int j =0; j < gp.burst; j++){
                    int frameAbs = (int) (i * gp.burst + j);
                    if (frameAbs > wlArray.length){
                        frameAbs = (int) ((i * gp.burst + j) - gp.burst);
                    }

                    double[][] g = normalise_dtype(wlArray[frameAbs], true, 4096);
                    double[][] newG = realToComplex(g);
                    double[][] newF1 = realToComplex(f1);

                    DoubleFFT_2D fft2 = new DoubleFFT_2D(f1.length, f1[0].length);
                    fft2.complexForward(newG);
                    fft2.complexForward(newF1);
                    Dftresult r = dftregistration(convertToComplex(newF1),convertToComplex(newG),(int) gp.upFactor);
                    Gxx.set(j,r.getOutput()[3]);
                    Gyy.set(j,r.getOutput()[2]);
                }
                avGxx.set(i,getMean(Gxx));
                avGyy.set(i,getMean(Gyy));
                double[] ary = linspace(0, gp.cycle,(int) gp.cycle);
                double[] xx = interp(ary, new double[]{0,gp.cycle}, new double[]{avGxx.get(i-1), avGxx.get(i)});
                double[] yy = interp(ary, new double[]{0,gp.cycle}, new double[]{avGyy.get(i-1), avGyy.get(i)});
                xxx.add(xx);
                yyy.add(yy);

                for(int j = 1; j < gp.cycle+1; j++){
                    double frameNumber = (i - 1) * gp.cycle + j;
                    ArrayList<Integer> locs = new ArrayList<>();
                    for(int k = 0; k < threed_data[4].length; k ++){
                        if(threed_data[4][k] == frameNumber){
                            locs.add(k);
                            if(threed_data[4][k+1] > frameNumber){
                                break;
                            }
                        }
                    }
                    double[][] data_to_correct = new double[locs.size()][5];
                    for(int k = 0; k < locs.size(); k++){
                        data_to_correct[k] = data_arr[locs.get(k)];
                    }
                    //TODO: check the data type of corr_x and corr_y
                    double[] corr_x = new double[data_to_correct.length];
                    if(paras.flip_x){
                        for(int k = 0; k < corr_x.length; k ++){
                            corr_x[k] = data_to_correct[k][0] - xx[j-1] * gp.px_size;
                        }
                    }else {
                        for(int k = 0; k < corr_x.length; k ++){
                            corr_x[k] = data_to_correct[k][0] + xx[j-1] * gp.px_size;
                        }
                    }
                    double[] corr_y = new double[data_to_correct.length];

                    if(paras.flip_y){
                        for(int k = 0; k < corr_y.length; k ++){
                            corr_y[k] = data_to_correct[k][1] - yy[j-1] * gp.px_size;
                        }
                    }else{
                        for(int k = 0; k < corr_y.length; k ++){
                            corr_y[k] = data_to_correct[k][1] + yy[j-1] * gp.px_size;
                        }
                    }
                    double[][] corr_x_reshape = reshape(corr_x.length,1,corr_x);
                    double[][] corr_y_reshape = reshape(corr_y.length,1,corr_y);
                    double[][] slice_data = new double[data_to_correct.length][3];
                    for(int k = 0; k < data_to_correct.length; k ++){
                        for(int f = 2,a = 0 ; f < 5; f ++, a ++){
                            slice_data[k][a] = data_to_correct[j][f];
                        }
                    }
                    double[][] corrected = new double[slice_data.length][slice_data[0].length +
                            corr_x_reshape[0].length + corr_y_reshape[0].length];
                    for(int k = 0; k < corrected.length; k++){
                        corrected[k][0] = corr_x_reshape[k][0];
                        corrected[k][1] = corr_y_reshape[k][0];
                        corrected[k][2] = slice_data[k][0];
                        corrected[k][3] = slice_data[k][1];
                        corrected[k][4] = slice_data[k][2];

                    }
                    for(int k = 0; k < corrected.length; k ++){
                        data_corrected.add(corrected[k]);
                    }
                }
                data_corrected.remove(0);
                ArrayList<Double> interX = new ArrayList<>();
                //;put all value into one array
                for(double[] xArr : xxx){
                    for (double v : xArr) {
                        interX.add(v * gp.px_size);
                    }
                }

                ArrayList<Double> interY = new ArrayList<>();
                //;put all value into one array
                for(double[] yArr : yyy){
                    for (double v : yArr) {
                        interY.add(v * gp.px_size);
                    }
                }
                //write drift_by_frame
                for(int k = 0; k < interX.size(); k ++){
                    ArrayList<Double> subList = new ArrayList<>();
                    subList.add(interX.get(k));
                    subList.add(interY.get(k));
                    drift_by_frame.add(subList);
                }
            }
        } else if (paras.wl_occasional && !paras.correction_twice && !paras.average_drift) {
//            int[][][] wlArray = loadtifFile(wl_path);
            double[][][]wl_img = normalise_dtype(wlArray,true,4096);
            int num_burst = (int) Math.floor(wl_img.length / gp.burst);

            ArrayList<double[][]> wl_avg = new ArrayList<>();
            for(int bur = 0; bur < num_burst; bur++){
                int upperIndex =(int) (bur * gp.burst);
                int lowerIndex = (int) ((bur + 1) * gp.burst );
                double[][][] img_burst = new double[lowerIndex - upperIndex][][];

                for(int i = upperIndex; i < lowerIndex; i++){
                    img_burst[i - upperIndex] = wl_img[i];
                }
                double[][] burst_avg = meanAxis0(img_burst);
                wl_avg.add(burst_avg);
            }
            double last_loc = getMax(threed_data[4]);
            if(last_loc/ gp.cycle > num_burst){
                //add the last item in this list to this list
                wl_avg.add(wl_avg.get(wl_avg.size()-1));
                num_burst += 1;
            }

            double[][] f1 = wl_avg.get(0);
            ArrayList<Double> Gxx = zeros1col(num_burst);
            ArrayList<Double> Gyy = zeros1col(num_burst);

            ArrayList<double[]> xxInter = new ArrayList<>();
            ArrayList<double[]> yyInter = new ArrayList<>();

            for(int i = 1; i < num_burst; i ++){
                double[][] g = wl_avg.get(i);
                double[][] newF1 = realToComplex(f1);
                double[][] newG = realToComplex(g);

                DoubleFFT_2D fft2 = new DoubleFFT_2D(f1.length, f1[0].length);
                fft2.complexForward(newF1);
                fft2.complexForward(newG);
                Dftresult r = dftregistration(convertToComplex(newF1),convertToComplex(newG),(int) gp.upFactor);

                Gxx.set(i, r.getOutput()[3]);
                Gyy.set(i, r.getOutput()[2]);
                double[] ary = linspace(0, gp.cycle,(int) gp.cycle);
                double[] xx_1 = interp(ary, new double[]{0,gp.cycle}, new double[]{Gxx.get(i-1), Gxx.get(i)});
                double[] yy_1 = interp(ary, new double[]{0,gp.cycle}, new double[]{Gyy.get(i-1), Gyy.get(i)});

                xxInter.add(xx_1);
                yyInter.add(yy_1);
            }
            ArrayList<Double> xx = concatenate(xxInter);
            ArrayList<Double> yy = concatenate(yyInter);

            ArrayList<Double> corr_x_list = new ArrayList<>();
            ArrayList<Double> corr_y_list = new ArrayList<>();

            for(int i =0; i < data_arr[4].length; i ++){
                double frm = data_arr[4][i];
                double corr_x;
                if(paras.flip_x){
                    //data_Arr[1] is all y value
                    corr_x = threed_data[0][i] - xx.get((int) (frm-1))* gp.px_size;
                }else {
                    corr_x = threed_data[0][i] + xx.get((int) (frm-1))* gp.px_size;
                }
                double corr_y;

                if(paras.flip_y){
                    //data_Arr[1] is all y value
                    corr_y = threed_data[1][i] - yy.get((int) (frm-1))* gp.px_size;
                }else{
                    corr_y = threed_data[1][i] + yy.get((int) (frm-1))* gp.px_size;
                }
                corr_x_list.add(corr_x);
                corr_y_list.add(corr_y);
            }
            ArrayList<double[]> xy = arrayT(corr_x_list,corr_y_list);
            double[][] inter_data_corrected  = arraySliceCol2d(data_arr,2);
            data_corrected = concatenateAxis1(xy,inter_data_corrected);
            arrayMultiplicaion(xx,gp.px_size);
            arrayMultiplicaion(yy,gp.px_size);
            drift_by_frame = arrayListT(xx,yy);

        } else if (paras.wl_occasional && paras.correction_twice && paras.average_drift) {
//            int[][][] wlArray = loadtifFile(wl_path);
            double[][] f1 = normalise_dtype(wlArray[0], true, 4096);
            ArrayList<Double> fxx = zeros1col(gp.burst);
            ArrayList<Double> fyy = zeros1col(gp.burst);

            for(int i = 1; i < gp.burst; i ++ ){
                double[][] f = normalise_dtype(wlArray[i], true, 4096);
                double[][] newF = realToComplex(f);
                double[][] newF1 = realToComplex(f1);

                DoubleFFT_2D fft2 = new DoubleFFT_2D(f1.length, f1[0].length);
                fft2.complexForward(newF);
                fft2.complexForward(newF1);
                Dftresult r = dftregistration(convertToComplex(newF1),convertToComplex(newF),(int) gp.upFactor);
                fxx.set(i,r.getOutput()[3]);
                fyy.set(i,r.getOutput()[2]);
            }
            double avFxx = getMean(fxx);
            double avFyy = getMean(fyy);
            ArrayList<double[]> xxx = new ArrayList<>();
            ArrayList<double[]> yyy = new ArrayList<>();

            double max = getMax(threed_data[4]);
            for(int i =1; i < Math.ceil(max / gp.cycle) +1; i ++){
                ArrayList<Double> Gxx1 = zeros1col(gp.burst);
                ArrayList<Double> Gyy1 = zeros1col(gp.burst);
                ArrayList<Double> Gxx2 = zeros1col(gp.burst);
                ArrayList<Double> Gyy2 = zeros1col(gp.burst);

                for(int j = 0; j < gp.burst; j++) {
                    if (i != 1) {
                        int frameAbs1 = (int) (2 * (i - 1) * gp.burst + j);
                        double[][] g1 = normalise_dtype(wlArray[frameAbs1], true, 4096);
                        double[][] newG1 = realToComplex(g1);
                        double[][] newF1 = realToComplex(f1);

                        DoubleFFT_2D fft2 = new DoubleFFT_2D(f1.length, f1[0].length);
                        fft2.complexForward(newG1);
                        fft2.complexForward(newF1);
                        Dftresult r = dftregistration(convertToComplex(newF1), convertToComplex(newG1), (int) gp.upFactor);
                        Gxx1.set(j, r.getOutput()[3]);
                        Gyy1.set(j, r.getOutput()[2]);
                    }
                    int frameAbs2 = (int) ((2 * i - 1) * gp.burst + j);
                    double[][] g2 = normalise_dtype(wlArray[frameAbs2], true, 4096);
                    double[][] newG2 = realToComplex(g2);
                    double[][] newF1 = realToComplex(f1);

                    DoubleFFT_2D fft2 = new DoubleFFT_2D(f1.length, f1[0].length);
                    fft2.complexForward(newG2);
                    fft2.complexForward(newF1);
                    Dftresult r = dftregistration(convertToComplex(newF1), convertToComplex(newG2), (int) gp.upFactor);
                    Gxx2.set(j, r.getOutput()[3]);
                    Gyy2.set(j, r.getOutput()[2]);
                }
                double avGxx1 = 0;
                double avGyy1 = 0;
                if(i == 1){
                    avGxx1 = avFxx;
                    avGyy1 = avFyy;
                }else{
                    avGxx1 = getMean(Gxx1);
                    avGyy1 = getMean(Gyy1);
                }
                double avGxx2 = getMean(Gxx2);
                double avGyy2 = getMean(Gyy2);

                double[] ary = linspace(0, gp.cycle,(int) gp.cycle);
                double[] xx = interp(ary, new double[]{0,gp.cycle}, new double[]{avGxx1, avGxx2});
                double[] yy = interp(ary, new double[]{0,gp.cycle}, new double[]{avGyy1, avGyy2});
                xxx.add(xx);
                yyy.add(yy);

                for(int j =1; j < gp.cycle+1; j ++){
                    double frameNumber = (i - 1) * gp.cycle + j;
                    ArrayList<Integer> locs = new ArrayList<>();
                    for(int k = 0; k < threed_data[4].length; k ++){
                        if(threed_data[4][k] == frameNumber){
                            locs.add(k);
                            if(threed_data[4][k+1] > frameNumber){
                                break;
                            }
                        }
                    }
                    double[][] data_to_correct = new double[locs.size()][5];
                    for(int k = 0; k < locs.size(); k++){
                        data_to_correct[k] = data_arr[locs.get(k)];
                    }
                    double[] corr_x = new double[data_to_correct.length];
                    if(paras.flip_x){
                        for(int k = 0; k < corr_x.length; k ++){
                            corr_x[k] = data_to_correct[k][0] - xx[j-1] * gp.px_size;
                        }
                    }else {
                        for(int k = 0; k < corr_x.length; k ++){
                            corr_x[k] = data_to_correct[k][0] + xx[j-1] * gp.px_size;
                        }
                    }
                    double[] corr_y = new double[data_to_correct.length];

                    if(paras.flip_y){
                        for(int k = 0; k < corr_y.length; k ++){
                            corr_y[k] = data_to_correct[k][1] - yy[j-1] * gp.px_size;
                        }
                    }else{
                        for(int k = 0; k < corr_y.length; k ++){
                            corr_y[k] = data_to_correct[k][1] + yy[j-1] * gp.px_size;
                        }
                    }
                    double[][] corr_x_reshape = reshape(corr_x.length,1,corr_x);
                    double[][] corr_y_reshape = reshape(corr_y.length,1,corr_y);
                    for(int k = 0; k < corr_y.length; k ++){
                        corr_y_reshape[k][0] = corr_y[k] ;
                    }
                    double[][] slice_data = new double[data_to_correct.length][3];
                    for(int k = 0; k < data_to_correct.length; k ++){
                        for(int f = 2,a = 0 ; f < 5; f ++, a ++){
                            slice_data[k][a] = data_to_correct[j][f];
                        }
                    }
                    double[][] corrected = new double[slice_data.length][slice_data[0].length +
                            corr_x_reshape[0].length + corr_y_reshape[0].length];
                    for(int k = 0; k < corrected.length; k++){
                        corrected[k][0] = corr_x_reshape[k][0];
                        corrected[k][1] = corr_y_reshape[k][0];
                        corrected[k][2] = slice_data[k][0];
                        corrected[k][3] = slice_data[k][1];
                        corrected[k][4] = slice_data[k][2];

                    }
                    for(int k = 0; k < corrected.length; k ++){
                        data_corrected.add(corrected[k]);
                    }
                }
                data_corrected.remove(0);
                ArrayList<Double> xxxConcatenate = concatenate(xxx);
                double[][] reshape_xxx = reshape(arrayListSize(xxx),1,xxx);
                ArrayList<Double> yyyConcatenate = concatenate(yyy);
                double[][] reshape_yyy = reshape(arrayListSize(yyy),1,yyy);

                drift_by_frame = convertIntoArrayList(concatenateAxis1(reshape_xxx,reshape_yyy));
            }
        } else if (paras.wl_occasional && paras.correction_twice && !paras.average_drift) {
//            int[][][] wlArray = loadtifFile(wl_path);
            double[][][]wl_img = normalise_dtype(wlArray,true,4096);
            int num_burst = (int) Math.floor(wl_img.length / gp.burst);
            System.out.println("num_burst: " + num_burst);

            ArrayList<double[][]> wl_avg = new ArrayList<>();
            for(int bur = 0; bur < num_burst; bur++){
                int upperIndex =(int) (bur * gp.burst);
                int lowerIndex = (int) ((bur + 1) * gp.burst );
                double[][][] img_burst = new double[lowerIndex - upperIndex][][];

                for(int i = upperIndex; i < lowerIndex; i++){
                    img_burst[i - upperIndex] = wl_img[i];
                }
                double[][] burst_avg = meanAxis0(img_burst);
                wl_avg.add(burst_avg);
            }
            System.out.println("wl_avg: " + wl_avg.size() + ", " + wl_avg.get(0).length + ", "+wl_avg.get(0)[0].length);
            double[][] f1 = wl_avg.get(0);
            System.out.println("F1: " + f1[0][0] + " " + f1[0][1] );
            System.out.println();
            ArrayList<Double> Gxx = zeros1col(num_burst);
            ArrayList<Double> Gyy = zeros1col(num_burst);

            int num_cycle = (int) (Gxx.size() / 2);
            ArrayList<Double> Gxx1 = zeros1col(num_cycle);
            ArrayList<Double> Gyy1 = zeros1col(num_cycle);
            ArrayList<Double> Gxx2 = zeros1col(num_cycle);
            ArrayList<Double> Gyy2 = zeros1col(num_cycle);


            ArrayList<double[]> xxInter = new ArrayList<>();
            ArrayList<double[]> yyInter = new ArrayList<>();

            for(int i = 1; i < num_burst; i ++) {
                double[][] g = wl_avg.get(i);
                double[][] newF1 = realToComplex(f1);
                double[][] newG = realToComplex(g);

                DoubleFFT_2D fft2 = new DoubleFFT_2D(f1.length, f1[0].length);
                fft2.complexForward(newF1);
                fft2.complexForward(newG);
                Complex[][] cf1 = convertToComplex(newF1);
                Complex[][] cg = convertToComplex(newG);
                Dftresult r = dftregistration(cf1, cg, (int) gp.upFactor);

                Gxx.set(i, r.getOutput()[3]);
                Gyy.set(i, r.getOutput()[2]);
            }
//            for(int i = 0; i < 4; i++){
//                System.out.println("gxx: " + Gxx.get(i) + " size: " + Gxx.size());
//                System.out.println("gyy: " + Gyy.get(i) + " size: " + Gyy.size());
//                System.out.println();
//            }

            for(int i = 0; i < num_cycle; i ++){
                Gxx1.set(i,Gxx.get(i * 2));
                Gxx2.set(i,Gxx.get(i * 2 + 1));
                Gyy1.set(i,Gyy.get(i * 2));
                Gyy2.set(i,Gyy.get(i * 2 + 1));
                double[] ary = linspace(0, gp.cycle,(int) gp.cycle);
                double[] xx_i = interp(ary, new double[]{0,gp.cycle}, new double[]{Gxx1.get(i),Gxx2.get(i)});
                double[] yy_i = interp(ary, new double[]{0,gp.cycle}, new double[]{Gyy1.get(i),Gyy2.get(i)});

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

            for(int i =0; i < threed_data[4].length; i ++){
                double frm = threed_data[4][i];
                double corr_x;
                if(paras.flip_x){
                    corr_x = threed_data[0][i] - xx.get((int) (frm-1))* gp.px_size;
                }else {
                    corr_x = threed_data[0][i] + xx.get((int) (frm-1))* gp.px_size;
//                    if(920<i && i <923){
//                        System.out.println("i: "+i + " frm: " + frm + "\nxx: " + xx.get((int) (frm-1)) + "\npx_size: " + gp.px_size + "\nthread_data: " + threed_data[0][i] + "corr_x: "+ corr_x + "\n\n");
//                    }
                }
                double corr_y;
                if(paras.flip_y){
                    corr_y = threed_data[1][i] - yy.get((int) (frm-1))* gp.px_size;
                }else{
                    corr_y = threed_data[1][i] + yy.get((int) (frm-1))* gp.px_size;
//                    if(920<i && i <923){
//                        System.out.println("i: "+i + " frm: " + frm + "\nyy: " + yy.get((int) (frm-1)) + "\npx_size: " + gp.px_size + "\nthread_data: " + threed_data[1][i] + "corr_y: "+ corr_y + "\n\n");
//                    }
                }

                corr_x_list.add(corr_x);
                corr_y_list.add(corr_y);
            }
            ArrayList<double[]> xy = arrayT(corr_x_list,corr_y_list);
            double[][] inter_data_corrected  = arraySliceCol2d(data_arr,2);
            data_corrected = concatenateAxis1(xy,inter_data_corrected);

            arrayMultiplicaion(xx,gp.px_size);
            arrayMultiplicaion(yy,gp.px_size);
            double[][] xx_reshape = reshape(xx.size(), 1,toArray(xx));
            double[][] yy_reshape = reshape(yy.size(), 1,toArray(yy));
            drift_by_frame = convertIntoArrayList(concatenateAxis1(xx_reshape,yy_reshape));
        }

        double[][] drift_by_loc_inter = new double[data_corrected.size()][data_corrected.get(0).length];
        for(int i = 0; i < data_corrected.size(); i ++){
            for(int j = 0; j < data_corrected.get(0).length; j ++){
                drift_by_loc_inter[i][j] = data_corrected.get(i)[j] - data_arr[i][j];
            }
        }
        double[][] drift_by_loc = arraySliceCol2dInverse(drift_by_loc_inter,2);

        return new DCresult(data_corrected,drift_by_loc,drift_by_frame);

    }
    // algorithm ends

    public static void plotTrajectory(ArrayList<ArrayList<Double>> driftByFrame) {
        XYSeries series = new XYSeries("Trajectory");

        for (int i = 0; i < driftByFrame.size(); i++) {
            double x = driftByFrame.get(i).get(0);
            double y = driftByFrame.get(i).get(1);
            series.add(x, y);
        }

        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(series);

        JFreeChart chart = ChartFactory.createXYLineChart(
                "Trajectory Plot",
                "X",
                "Y",
                dataset
        );

        XYPlot plot = chart.getXYPlot();
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();

        // Set line color
        renderer.setSeriesPaint(0, Color.BLUE);
        plot.setRenderer(renderer);

        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(new ChartPanel(chart));
        frame.pack();
        frame.setVisible(true);
    }
    private static JFreeChart createChartPanel(ArrayList<ArrayList<Double>> drift_by_frame) {

        XYSeriesCollection dataset = new XYSeriesCollection();
        XYSeries series = new XYSeries("Trajectory");

        for (ArrayList<Double> point : drift_by_frame) {
            series.add(point.get(0), point.get(1));
        }

        dataset.addSeries(series);
        JFreeChart chart = ChartFactory.createXYLineChart("Trajectory Plot", "X", "Y", dataset, PlotOrientation.VERTICAL,true,true,false);

        XYPlot plot = chart.getXYPlot();
        XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
        plot.setBackgroundPaint(Color.WHITE);

//        for (int i = 0; i < dataset.getSeriesCount() - 1; i++) {
////            renderer.setSeriesStroke(i, new BasicStroke(0.5f));
//            renderer.setSeriesShapesVisible(i, false);
////            renderer.setSeriesPaint(i, getColorForValue(i, drift_by_frame.size()));
//        }
        renderer.setSeriesShapesVisible(0, false);
        renderer.setSeriesStroke(0,new BasicStroke(2.0f));
        NumberAxis domainAxis = (NumberAxis) plot.getDomainAxis();
        domainAxis.setTickUnit(new NumberTickUnit(100));
        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis() ;
        rangeAxis.setTickUnit(new NumberTickUnit(50));
        IJ.log("series: " + dataset.getSeriesCount());

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

        LookupPaintScale paintScale = new LookupPaintScale(minValue,maxValue , Color.BLUE);
        paintScale.add(minValue, Color.BLUE);
        double interval = (maxValue - minValue) / 100.0;  // Divide the range into four segments

        for(int i = 1 ;i < 100; i ++){
            paintScale.add(minValue + interval * i, mixColors(Color.BLUE, Color.GREEN, (100-i) * (0.01)));
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
    public static Color mixColors(Color c1, Color c2, double weight) {
        double r = weight * c1.getRed() + (1.0 - weight) * c2.getRed();
        double g = weight * c1.getGreen() + (1.0 - weight) * c2.getGreen();
        double b = weight * c1.getBlue() + (1.0 - weight) * c2.getBlue();
        return new Color((int) r, (int) g, (int) b);
    }

    public static ArrayList<ArrayList<Double>> zoom(double[][] f, double upfac) {
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
    public static double[][] convert(ArrayList<ArrayList<Double>> list) {
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




    public static ArrayList<ArrayList<Double>> shift_img(double[][] f, double xx, double yy){
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


        if(pad_num_x != 0){
            x_pad = ones(f_y,pad_num_x);
            arrayMultiplicaion2d(x_pad,fmean);
        }
        if(pad_num_y != 0){
            y_pad = ones(pad_num_y,f_x);
            arrayMultiplicaion2d(y_pad,fmean);
        }

        if(xx < 0){
            f_padx = concatenateAxis1(f_zoom,x_pad);
            f_cutx = arraySliceCol2dInverse(f_padx,pad_num_x);

        } else if (xx == 0) {
            f_cutx = new ArrayList<>(f_zoom);

        } else if (xx > 0) {
            f_padx = concatenateAxis1(x_pad,f_zoom);
            int f_padx_size = f_padx.get(0).size();
            f_cutx = arraySliceCol2d(f_padx,(f_padx_size - pad_num_x));
        }

        if(yy < 0){
            f_pady = concatenate2DAxis0(f_cutx,y_pad);
            f_cuty = arraySliceRow2dInverse(f_pady,pad_num_y);
        } else if (yy == 0) {
            f_cuty = new ArrayList<>(f_cutx);
        } else if (yy > 0) {
            f_pady = concatenate2DAxis0(y_pad,f_cutx);
            f_cuty = arraySliceRow2d(f_pady,(f_cutx.size() - pad_num_y));

        }
        ArrayList<ArrayList<Double>> result = zoom(convert(f_cuty), 1.0 /upfac);
        return result;
    }
    public static void align_wl(String wl_path, String selectedTitle,DC_Paras paras,DC_GeneralParas generalParas) throws Exception {
        int[][][] wlArray;
        File file = new File(wl_path);
        String filename = file.getName();
        ArrayList<double[][]> fs = new ArrayList<>();
        if (!memoryWL) {
            IJ.log("load from file" + wl_path);
            wlArray = loadtifFile(wl_path, false);

        } else {
            IJ.log("load from window" + selectedTitle);
            wlArray = loadtifFile(selectedTitle, true);

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

                Dftresult r = dftregistration(convertToComplex(newF0), convertToComplex(newG), (int) generalParas.upFactor);
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

                Dftresult r = dftregistration(convertToComplex(newF0), convertToComplex(newG), (int) generalParas.upFactor);
                double xx = r.getOutput()[3];
                double yy = r.getOutput()[2];
                ArrayList<ArrayList<Double>> inter =  shift_img(g, xx, yy);
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

    public static void saveAsMultipageTIFF(String filename, ArrayList<double[][]> fs) throws Exception {
        String outputFilename = filename.substring(0, filename.length() - 4) + "_corr.tif";
        File outputFile = new File(outputFilename);

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
        IJ.log("corrected .tif file saved");
    }

    private static BufferedImage convertDoubleArrayToBufferedImage(double[][] data) {
        int width = data[0].length;
        int height = data.length;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int value = (int) data[y][x];
                image.setRGB(x, y, value);
            }
        }

        return image;
    }




public static double compute00(Complex[][] buf1ft) {
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
    public static void arrayMultiplicaion(ArrayList<Double> xx, double factor){
        xx.replaceAll(aDouble -> aDouble * factor);
    }
    public static void arrayMultiplicaion2d(ArrayList<ArrayList<Double>> xx, double factor){
        for(ArrayList<Double>x : xx){
            x.replaceAll(aDouble -> aDouble * factor);
        }
    }

    public static ArrayList<Double> concatenate(ArrayList<double[]> list){
        ArrayList<Double> result = new ArrayList<>();
        for(double[] ary : list){
            for(double i : ary){
                result.add(i);
            }

        }
        return result;
    }
    public static double[][] arrayListToArray(ArrayList<double[]> arr){
        double[][] result = new double[arr.size()][arr.get(0).length];
        for(int i = 0; i < result.length; i ++){
            for(int j = 0; j < result[0].length; j ++){
                result[i][j] = arr.get(i)[j];
            }
        }
        return result;
    }
    public static double[][] arraySliceCol2d(double[][] data_arr,int col){
        double[][] inter_data_corrected  = new double[data_arr.length][data_arr[0].length - col];
        for(int i = 0; i < data_arr.length; i ++){
            for(int j =col, k = 0; j < 5; j ++, k ++){
                inter_data_corrected[i][k] = data_arr[i][j];
            }
        }
        return inter_data_corrected;
    }
    // [:,:col]
    public static ArrayList<ArrayList<Double>> arraySliceCol2d(ArrayList<ArrayList<Double>> data_arr,int col){
        if(data_arr.get(0).size() < col){
            throw new IllegalArgumentException("column number should larger than the total column number");
        }
        for(ArrayList<Double> d :data_arr ){
            while (d.size() > col){
                d.remove(col);
            }
        }
        return data_arr;
    }
    //[:row,:]
    public static ArrayList<ArrayList<Double>> arraySliceRow2d(ArrayList<ArrayList<Double>> data_arr,int row){
        if(data_arr.size() < row){
            throw new IllegalArgumentException("row number should larger than the total row number");
        }
        while(data_arr.size() > row ){
            data_arr.remove(row);
        }
        return data_arr;
    }
    //[:,col:]
    public static ArrayList<ArrayList<Double>> arraySliceCol2dInverse(ArrayList<ArrayList<Double>> data_arr,int col){
        if(data_arr.get(0).size() < col){
            throw new IllegalArgumentException("column number should larger than the total column number");
        }
        for(ArrayList<Double> d : data_arr){
            int size = d.size();
            while (d.size() > size - col){
                d.remove(0);
            }
        }

        return data_arr;
    }
    //[row:,:]
    public static ArrayList<ArrayList<Double>> arraySliceRow2dInverse(ArrayList<ArrayList<Double>> data_arr,int row){
        if(data_arr.size() < row){
            throw new IllegalArgumentException("row number should larger than the total row number");
        }
        int size = data_arr.size();
        while(data_arr.size() >size - row ){
            data_arr.remove(0);
        }
        return data_arr;
    }
    public static double[][] arraySliceCol2dInverse(double[][] data_arr,int col){
        double[][] inter_data_corrected  = new double[data_arr.length][col];
        for(int i = 0; i < data_arr.length; i ++){
            for(int j = 0; j < col; j ++){
                inter_data_corrected[i][j] = data_arr[i][j];
            }
        }
        return inter_data_corrected;
    }

    /**
     * concatenate with axia=1 is not allowed on 1d array
     * @param data [[1,2,3,4],[5,6,7,8]]
     * @param arr  [[9,10,11,12],[13,14,15,16]]
     * @return  [[1,2,3,4,9,10,11,12],[5,6,7,8,13,14,15,16]]
     */
    public static ArrayList<double[]> concatenateAxis1(ArrayList<double[]> data, double[][] arr){
        ArrayList<double[]> returnList = new ArrayList<double[]>();

        for (int i = 0; i < data.size(); i++) {
            double[] concatenatedArray = new double[data.get(i).length + arr[i].length];
            System.arraycopy(data.get(i), 0, concatenatedArray, 0, data.get(i).length);
            System.arraycopy(arr[i], 0, concatenatedArray, data.get(i).length, arr[i].length);
            returnList.add(concatenatedArray);
        }
        return returnList;
    }
    public static ArrayList<ArrayList<Double>> concatenateAxis1(ArrayList<ArrayList<Double>> data, ArrayList<ArrayList<Double>> arr){
        ArrayList<ArrayList<Double>> returnList = new ArrayList<>();

        for(int i = 0 ; i < data.size(); i ++){
            ArrayList<Double> inter = new ArrayList<>();
            for(Double d : data.get(i)){
                inter.add(d);
            }
            for(Double d : arr.get(i)){
                inter.add(d);

            }
            returnList.add(inter);
        }
        return returnList;
    }
    public static ArrayList<ArrayList<Double>> concatenate2DAxis0(ArrayList<ArrayList<Double>> a , ArrayList<ArrayList<Double>> b){
        ArrayList<ArrayList<Double>> returnList = new ArrayList<>();
        for(ArrayList<Double> aa : a){
            returnList.add(aa);
        }
        for(ArrayList<Double> bb : b){
            returnList.add(bb);
        }
        return returnList;
    }



    public static ArrayList<double[]> concatenateAxis1(double[][] data, double[][] arr){
        ArrayList<double[]> returnList = new ArrayList<double[]>();

        for (int i = 0; i < data.length; i++) {
            double[] concatenatedArray = new double[data[i].length + arr[i].length];
            System.arraycopy(data[i], 0, concatenatedArray, 0, data[i].length);
            System.arraycopy(arr[i], 0, concatenatedArray, data[i].length, arr[i].length);
            returnList.add(concatenatedArray);
        }
        return returnList;
    }

    public static ArrayList<ArrayList<Double>> convertIntoArrayList(ArrayList<double[]> arr){
        ArrayList<ArrayList<Double>> result = new ArrayList<>();
        for(double[] ary : arr){
            ArrayList<Double> inter = new ArrayList<>();
            for(double d : ary) {
                inter.add(d);
            }
            result.add(inter);
        }
        return result;
    }
    public static double[] toArray(ArrayList<Double> ary){
        double[] result = new double[ary.size()];
        for(int i = 0; i < result.length; i ++){
            result[i] = ary.get(i);
        }
        return result;
    }

    /**
     *
     *
     * @param a assume a is [1,2,3,4]
     * @param b assume b is [5,6,7,8]
     * @return return result will be [[1,5],[2,6],[3,7],[4,8]]
     * this method will return a arraylist, all its element will be a single java array
     */
    public static ArrayList<double[]> arrayT(ArrayList<Double> a, ArrayList<Double>b){
        if(a.size() != b.size()){
            throw new IllegalArgumentException("two input arrayList should have same size");
        }
        ArrayList<double[]> result = new ArrayList<>();
        for(int i  = 0; i < a.size(); i++){
            double[] inter = new double[2];
            inter[0] = a.get(i);
            inter[1] = b.get(i);
            result.add(inter);
        }
        return result;
    }
    //this return 2d array list
    public static ArrayList<ArrayList<Double>> arrayListT(ArrayList<Double> a, ArrayList<Double>b){
        if(a.size() != b.size()){
            throw new IllegalArgumentException("two input arrayList should have same size");
        }
        ArrayList<ArrayList<Double>> result = new ArrayList<>();
        for(int i  = 0; i < a.size(); i++){
            ArrayList<Double> inter = new ArrayList<>();
            inter.add(a.get(i));
            inter.add(b.get(i));
            result.add(inter);

        }
        return result;
    }
    public static double[] interp(double[] x, double[] xp, double[] fp) {
        double[] results = new double[x.length];
        DecimalFormat df = new DecimalFormat("#.########");

        for (int i = 0; i < x.length; i++) {
            results[i] = Double.parseDouble(df.format(interpolate(x[i], xp, fp)));
        }

        return results;
    }
    public static double getMax(double[] array){
        double max = 0;
        for (double v : array) {
            if (v > max) {
                max = v;
            }
        }
        return max;
    }

    private static double interpolate(double xi, double[] xp, double[] fp) {
        if (xi <= xp[0]) return fp[0];
        if (xi >= xp[xp.length - 1]) return fp[fp.length - 1];

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

    public static double[] linspace(double start, double end, int numPoints) {
        // Ensure number of points is at least 2
        if (numPoints < 2) {
            throw new IllegalArgumentException("numPoints must be at least 2.");
        }

        double[] result = new double[numPoints];
        double step = (end - start) / (numPoints - 1);
        DecimalFormat df = new DecimalFormat("#.########");

        for (int i = 0; i < numPoints; i++) {

            result[i] = (Double.parseDouble(df.format(start + i * step)));
        }

        return result;
    }
    public static double[][] meanAxis0(double[][][] img_burst) {
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
    public static int arrayListSize(ArrayList<double[]> arr){
        int size = 0;
        for (double[] ary : arr){
            size += ary.length;
        }
        return size;
    }
    public static double[][] reshape(int row, int col, ArrayList<double[]> arr){
        int size = arrayListSize(arr);
        if(row*col != size){
            throw new IllegalArgumentException("Cannot reshape arrayList with " + size +" elements into shape (" +row + ", " + col + ")");
        }
        ArrayList<Double> inter = new ArrayList<>();
        for(double[] ary : arr){
            for(double a : ary){
                inter.add(a);
            }
        }
        double[][] result = new double[row][col];
        int count = 0;
        for(int i = 0; i < row; i ++){
            for(int j = 0; j < col; j ++){
                result[i][j] =  inter.get(count);
                count++;
            }
        }
        return  result;

    }
    public static double[][] reshape(int row, int col, double[] arr){
        if(row*col != arr.length){
            throw new IllegalArgumentException("Cannot reshape arrayList with " + arr.length +" elements into shape (" +row + ", " + col + ")");
        }

        double[][] result = new double[row][col];
        int count = 0;
        for(int i = 0; i < row; i ++){
            for(int j = 0; j < col; j ++){
                result[i][j] =  arr[count];
                count++;
            }
        }
        return  result;

    }
    public static ArrayList<ArrayList<Double>> zeros(double row, double col){
        ArrayList<ArrayList<Double>> zeros = new ArrayList<>();
        for(int i = 0 ;i < row; i ++){
            ArrayList<Double> al = new ArrayList<>();
            for(int j = 0; j < col; j ++){
                al.add((double) 0);
            }
            zeros.add(al);
        }
        return zeros;
    }
    public static ArrayList<ArrayList<Double>> ones(double row, double col){
        ArrayList<ArrayList<Double>> zeros = new ArrayList<>();
        for(int i = 0 ;i < row; i ++){
            ArrayList<Double> al = new ArrayList<>();
            for(int j = 0; j < col; j ++){
                al.add((double) 1);
            }
            zeros.add(al);
        }
        return zeros;
    }
    public static ArrayList<Double> ones1col(double row){
        ArrayList<Double> zeros = new ArrayList<>();
        for(int i = 0 ;i < row; i ++){
            zeros.add((double) 1);
        }
        return zeros;
    }

    public static ArrayList<Double> zeros1col(double row){
        ArrayList<Double> zeros = new ArrayList<>();
        for(int i = 0 ;i < row; i ++){
           zeros.add((double) 0);
        }
        return zeros;
    }
    public static double getMean2D(ArrayList<ArrayList<Double>> list2D) {
        double total = 0;
        int count = 0;

        for(ArrayList<Double> list : list2D) {
            for(double num : list) {
                total += num;
                count++;
            }
        }

        return total / count;
    }
    public static double getMean2D(double[][] list2D) {
        double total = 0;
        int count = 0;

        for(double[] list : list2D) {
            for(double num : list) {
                total += num;
                count++;
            }
        }

        return total / count;
    }
    public static double getMean(ArrayList<Double> list) {
        double total = 0;
        int count = 0;

        for(double num : list) {
            total += num;
            count++;
        }


        return total / count;
    }

    public static void reformatComplex(Complex[][] c){
        DecimalFormat df = new DecimalFormat("#.########");

        for (int i = 0; i < c.length; i++) {
            for (int j = 0; j < c[i].length; j++) {
                double real = Double.parseDouble(df.format(c[i][j].getReal()));
                double imaginary = Double.parseDouble(df.format(c[i][j].getImaginary()));
                c[i][j] = new Complex(real, imaginary);
            }
        }
    }

    public static int[] arange(int start, int end) {
        int[] array = new int[end-start];
        for(int i = 0; i < end-start; i ++){
            array[i] = start + i;
        }
        return array;
    }
    public static double[] arange(double start, double end) {
        double[] array = new double[(int) Math.ceil(end-start)];
        for(int i = 0; i < end-start; i ++){
            array[i] = start + i;
        }
        return array;
    }

    public static double[][] absoluteValue(Complex[][] complexArray) {
        int rows = complexArray.length;
        int cols = complexArray[0].length;
        double[][] absoluteArray = new double[rows][cols];

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                absoluteArray[i][j] = complexArray[i][j].abs();
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

    //check data type for 3d array
    private static String checkDtype(int[][][] array){
        int height = array.length;
        int width = array[0].length;
        int depth = array[0][0].length;

        int maxVal = Integer.MIN_VALUE;
        int minVal = Integer.MAX_VALUE;

        for(int i=0; i<height; i++){
            for(int j=0; j<width; j++){
                for(int k=0; k<depth; k++){
                    maxVal = Math.max(maxVal, array[i][j][k]);
                    minVal = Math.min(minVal,array[i][j][k]);
                }
            }
        }
        if(minVal > 0) {
            if (maxVal < 256) {
                return "uint8";
            }
            if(maxVal < 65536 & minVal > 255){
                return "uint16";
            }
        }else{
            return "other";
        }

        return "other"; //default return value
    }

    //check data type for 2d array
    private static String checkDtype(int[][] array){
        int height = array.length;
        int width = array[0].length;

        int maxVal = Integer.MIN_VALUE;
        int minVal = Integer.MAX_VALUE;
        for(int i=0; i<height; i++){
            for(int j=0; j<width; j++){
                maxVal = Math.max(maxVal, array[i][j]);
                minVal = Math.min(minVal,array[i][j]);
            }
        }
        if(minVal > 0) {
            if (maxVal < 256) {
                return "uint8";
            }
            if(maxVal < 65536 & minVal > 255){
                return "uint16";
            }
        }else{
            return "other";
        }

        return "other"; //default return value
    }

    public static void test() throws Exception {
        String threedPath = "/Users/liusiqi/Downloads/Code/Sampledata/cell2_2nM_slice8_super-res.tif.results.3d";
        String threedName1 = "cell2_2nM_slice8_super-res.tif.results.3d";
        String wlPath = "/Users/liusiqi/Downloads/Code/Sampledata/cell2_2nM_slice8_WL.tif";

//        ArrayList<double[]>result = drift_correction(threedPath,threedName1,selectedTitle,wlPath,dc_generalParas,dc_paras).getData_corrected();
//        writeCorrected(result,"corrected_new.txt");
        align_wl(wlPath,"",dc_paras,dc_generalParas);
//        for(int i = 0; i < 4; i ++){
//            System.out.println(Arrays.toString(result.get(i)));
//        }
    }



    public static void main(String[]args) throws Exception {
//        String wlPath = "/Users/liusiqi/Downloads/Code/Sampledata/cell2_2nM_slice8_WL.tif";
//        int[][][] r = loadtifFile(wlPath);
//        long startTime = System.currentTimeMillis();
//        normalise_dtype(r,true,4096);
//        long endTime = System.currentTimeMillis();
//        long executionTime = endTime - startTime;
//        System.out.println("Execution time: " + executionTime + " milliseconds");
//        double[][][] result = normalise_dtype(r,true,4096);
//        for(int i = 0; i < 4; i ++){
//            for(int j =0; j < 4; j ++){
//                for(int k =0; k < 4;k ++){
//                    System.out.print(result[i][j][k]);
//                }
//                System.out.println();
//            }
//            System.out.println();
//        }



        test();
//        Class<DriftCorrection> clazz = DriftCorrection.class;
//        java.net.URL url = clazz.getProtectionDomain().getCodeSource().getLocation();
//        File file = new File(url.toURI());
//        // Note: This returns the base path. ImageJ will find plugins in here that have an
//        // underscore in the name. But it will not search recursively through the
//        // package structure to find plugins. Adding this at least puts it on ImageJ's
//        // classpath so plugins not satisfying these requirements can be loaded.
//        System.setProperty("plugins.dir", file.getAbsolutePath());
//
//        // Start ImageJ and exit when closed
//        ImageJ imagej = new ImageJ();
//        imagej.exitWhenQuitting(true);
//
//        // If this is in a sub-package or has no underscore then manually add the plugin
//        String packageName = clazz.getName().replace(clazz.getSimpleName(), "");
//        if (!packageName.isEmpty() || clazz.getSimpleName().indexOf('_') < 0) {
//            // Add a spacer
//            ij.Menus.installPlugin("", ij.Menus.PLUGINS_MENU, "-", "", IJ.getInstance());
//            ij.Menus.installPlugin(clazz.getName(),
//                    ij.Menus.PLUGINS_MENU, clazz.getSimpleName().replace('_', ' '), "", IJ.getInstance());
//        }

        // Initialise for testing, e.g. create some random datasets
//        MemoryPeakResults.addResults(getResults(name1, calibPath));
//        MemoryPeakResults.addResults(getResults(name2, dataPath));

        // Run the plugin
//        IJ.runPlugIn(clazz.getName(), "");
        //dftups test
//        Complex[][] array = new Complex[][] {
//                {new Complex(49.99, -2.68), new Complex(15.14, -0.36), new Complex(47.65, 12.7), new Complex(72.06, 24.38), new Complex(23.07, -7.29)},
//                {new Complex(67.12, -21.66), new Complex(79.5, 43.7), new Complex(10.39, -38.83), new Complex(69.55, -13.15), new Complex(72.42, -1.23)},
//                {new Complex(75.2, -11.58), new Complex(16.62, 0.34), new Complex(38.64, 7.48), new Complex(75.86, -19.2), new Complex(68.34, -21.42)}
//        };
//        dftups(array,5,5,3,2,2);
        //dft registrarion test
//        Complex[][] array1 = {
//                { new Complex(3.1, 4.2), new Complex(1.3, 2.7), new Complex(9.0, 0.5), new Complex(5.8, 6.1), new Complex(2.1, 3.6) },
//                { new Complex(4.6, 5.3), new Complex(7.2, 8.9), new Complex(1.0, 4.3), new Complex(3.9, 7.7), new Complex(6.1, 5.0) },
//                { new Complex(2.3, 8.5), new Complex(5.7, 1.1), new Complex(3.4, 6.2), new Complex(7.8, 4.0), new Complex(1.5, 9.3) }
//        };
//        Complex[][] array2 = {
//                { new Complex(6.1, 7.4), new Complex(2.3, 4.5), new Complex(5.2, 1.7), new Complex(3.8, 8.2), new Complex(4.3, 3.9) },
//                { new Complex(8.6, 5.2), new Complex(1.4, 7.8), new Complex(9.1, 0.3), new Complex(5.0, 6.5), new Complex(7.2, 5.7) },
//                { new Complex(3.5, 7.9), new Complex(2.8, 0.6), new Complex(1.7, 5.3), new Complex(8.7, 2.0), new Complex(3.4, 6.8) }
//        };
//        dftregistration(array1,array2,3);
    }

}
