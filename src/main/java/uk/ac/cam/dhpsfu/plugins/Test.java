


package uk.ac.cam.dhpsfu.plugins;

import ij.IJ;
import ij.plugin.PlugIn;
import ij.gui.GenericDialog;
import org.apache.commons.math3.complex.Complex;
import org.checkerframework.checker.units.qual.A;
import org.checkerframework.checker.units.qual.C;
import org.jfree.data.xy.XYDataset;
import org.jtransforms.fft.DoubleFFT_2D;

import java.awt.Component;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import uk.ac.cam.dhpsfu.analysis.Dftresult;

import javax.swing.*;
import java.awt.*;

public class Test  {
    public static void main(String[] args) {
        Complex c = new Complex(1,5);
        long start = System.currentTimeMillis();
        System.out.println(c.conjugate());
        long end = System.currentTimeMillis();
        System.out.println(end-start);
        start = System.currentTimeMillis();
        System.out.println(new Complex(c.getReal(), -c.getImaginary()));
        end = System.currentTimeMillis();
        System.out.println(end-start);
        start = System.currentTimeMillis();
        double[] cc = new double[2];
        cc[0] = 1;
        cc[1] = 5;
        System.out.println(Arrays.toString(new double[]{cc[0], -cc[1]}));
        end = System.currentTimeMillis();
        System.out.println(end-start);

//        XYSeries series = new XYSeries("My Line Graph");
//        series.add(1, 3);
//        series.add(2, 5);
//        series.add(3, 10);
//
//        XYDataset dataset = new XYSeriesCollection(series);
//        JFreeChart chart = ChartFactory.createXYLineChart(
//                "My Line Chart",
//                "X Axis", "Y Axis",
//                dataset);
//        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
//        XYPlot plot = chart.getXYPlot();
//        for (int i = 0; i < 3 - 1; i++) {
//            renderer.setSeriesStroke(i, new BasicStroke(0.5f));
//            renderer.setSeriesShapesVisible(i, false);
//            renderer.setSeriesPaint(i, Color.blue);
//        }
//
//        renderer.setSeriesShapesVisible(0, false);
//        plot.setRenderer(renderer);
//        ChartPanel chartPanel = new ChartPanel(chart);
//        JFrame frame = new JFrame("Line Chart");
//        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//
//        frame.add(chartPanel);
//        frame.pack();
//        frame.setVisible(true);

        double[][] a = {
                {1,2,3,4,5},
                {6,7,8,9,10},
                {11,12,13,14,15},
                {16,17,18,19,20},
                {21,22,23,24,25}
        };
//        System.out.println(Arrays.deepToString(DriftCorrection.arraySliceCol2dInverse(a, 2)));
//        double[][] complexData = new double[a.length][2 * a[0].length];
//        for (int i = 0; i < a.length; i++) {
//            for (int j = 0; j < a[0].length; j++) {
//                complexData[i][2 * j] = a[i][j];     // real part
//                complexData[i][2 * j + 1] = 0.0;       // imaginary part set to 0
//            }
//        }
//        DoubleFFT_2D fft2 = new DoubleFFT_2D(a.length, a[0].length);
//        fft2.complexForward(complexData);
//
//        Complex[][] result = new Complex[a.length][a[0].length];
//        for (int i = 0; i < a.length; i++) {
//            for (int j = 0; j < a[0].length; j++) {
//                double real = complexData[i][2 * j];
//                double imag = complexData[i][2 * j + 1];
//                result[i][j] = new Complex(real,imag);
//            }
//        }
//        System.out.println(Arrays.deepToString(result));
//        Complex a = new Complex(1, 2);
//        System.out.println(a.multiply(a.conjugate()));
//        double[] corr_x = new double[]{1,2};
//        double[] corr_y = new double[]{3,4};
//        double[][] data_to_correct = new double[][]{
//                {5,6,7,8,9},
//                {10,11,12,13,14}
//        };
//
//        double[][] corr_x_reshape = new double[corr_x.length][1]; // 2,1
//        for(int j = 0; j < corr_x.length; j ++){
//            corr_x_reshape[j][0] = corr_x[j] ;
//        }
//        double[][] corr_y_reshape = new double[corr_y.length][1]; // 2,1
//        for(int j = 0; j < corr_y.length; j ++){
//            corr_y_reshape[j][0] = corr_y[j] ;
//        }
//        double[][] slice_data = new double[data_to_correct.length][3];// 2,3
//        for(int j = 0; j < data_to_correct.length; j ++){
//            for(int k = 2,a = 0 ; k < 5; k ++, a ++){
//                slice_data[j][a] = data_to_correct[j][k];
//            }
//        }
//        double[][] corrected = new double[slice_data.length][slice_data[0].length +
//                corr_x_reshape[0].length + corr_y_reshape[0].length];
//        for(int j = 0; j < corrected.length; j++){
//            corrected[j][0] = corr_x_reshape[j][0];
//            corrected[j][1] = corr_y_reshape[j][0];
//            corrected[j][2] = slice_data[j][0];
//            corrected[j][3] = slice_data[j][1];
//            corrected[j][4] = slice_data[j][2];
//
//        }
//        System.out.println(Arrays.deepToString(corr_x_reshape));
//        System.out.println(Arrays.deepToString(corr_y_reshape));
//        System.out.println(Arrays.deepToString(slice_data));
//        System.out.println(Arrays.deepToString(corrected));
//        System.out.println(test_Resize[0][0]);

//        double[] xxx = new double[]{1,2,3,4,5};
//        double[] yyy = new double[]{6,7,8,9,0};
//        ArrayList<ArrayList<Double>> drift = new ArrayList<>();
////        for(int i = 0; i < xxx.length; i++){
////            drift.add(new ArrayList<Double>());
////        }
//
//        for(int i  = 0; i < xxx.length; i ++){
//            ArrayList<Double> a = new ArrayList<>();
//            a.add(xxx[i]);
//            a.add(yyy[i]);
//            drift.add(a);
//
//        }
//        DecimalFormat df = new DecimalFormat("#.#####");
//        System.out.println(Double.parseDouble(df.format(0.010019)));
//        System.out.println(Arrays.toString(DriftCorrection.linspace(0, 10, 5)));

//        double[] xp = {0.5, 1.4, 2.2, 3.7, 4.6};
//        double[] fp = {3.1, 4.5, 3.9, 5.2, 4.8};
//        double[] x = {0.7, 2.5, 4.0, 5.0};
//        ArrayList<double[]> res = new ArrayList<>();
//        res.add(x);
//        res.add(fp);
//        res.add(xp);
//
//        System.out.println(Arrays.toString(res.toArray()));


        double[][] arrr = {
                {1,2,3},
                {4,5,6}
        };
        double[] arr1 = {1,3,7,10};
        double[] arr2 = {2,4,8,9};
        double[] arr3 = {28,33,41,49};
        double[][] arr4 = {
                {1,2,3,4,5},
                {6,7,8,9,10},
                {11,12,13,14,15},
                {16,17,18,19,20}
        };
        double[][] arr5 = {
                {1,2,3},
                {4,5,6},
                {7,8,9}
        };
//        System.out.println(DriftCorrection.zoom(arr5,3));
//        System.out.println(DriftCorrection.zoom(arrr,10).size());
        double[] result = DriftCorrection.interp(arr1,arr2,arr3);
//        System.out.println(Arrays.toString(result));
        ArrayList<Double> ar1 = new ArrayList<>();
        ArrayList<Double> ar2 = new ArrayList<>();
        ArrayList<Double> ar6 = new ArrayList<>();
        ArrayList<Double> ar7 = new ArrayList<>();
        ArrayList<double[]> ar3 = new ArrayList<>();
        ArrayList<ArrayList<Double>> ar4 = new ArrayList<>();
        ar4.add(ar1);
        ar4.add(ar2);

        ArrayList<ArrayList<Double>> ar5 = new ArrayList<>();
        ArrayList<ArrayList<Double>> ar8 = new ArrayList<>();

//        System.out.println(DriftCorrection.concatenateAxis1(ar4,ar5));

        ar3.add(arr1);
        ar3.add(arr2);
        ar3.add(arr3);

        ar1.add(1.0);
//        ar1.add(3.0);ar1.add(7.0);ar1.add(10.0);ar1.add(88.0);ar1.add(190.0);
        ar6.add(44.0);
//        ar6.add(984.0);ar6.add(325.0);ar6.add(53.0);ar6.add(78.0);ar6.add(487.0);
        ar7.add(12.0);ar7.add(34.0);ar7.add(49.0);ar7.add(98.0);ar7.add(764.0);ar7.add(239.0);
        ar2.add(2.0);ar2.add(4.0);ar2.add(21.0);ar2.add(32.0);ar2.add(47.0);ar2.add(210.0);
        ar5.add(ar2);
        ar5.add(ar7);
        ar8.add(ar1);
        ar8.add(ar6);

//        System.out.println(DriftCorrection.concatenateAxis1(ar5,ar8));
//        System.out.println(ar4);
//        System.out.println(DriftCorrection.arraySliceCol2dInverse(ar4,2));
//        System.out.println(DriftCorrection.arraySliceCol2d(ar4,2));
//        System.out.println(DriftCorrection.arraySliceRow2dInverse(ar4,1));
//        System.out.println(DriftCorrection.arraySliceRow2dInverse(ar4,1));


//        System.out.println(DriftCorrection.concatenateAxis1(ar4,ar5));
//        System.out.println(Arrays.deepToString(DriftCorrection.arrayT(ar1,ar2).toArray()));
        DriftCorrection.arrayMultiplicaion(ar1,10);
//        System.out.println(Arrays.deepToString(DriftCorrection.reshape(arr1.length, 1, arr1)));
//        System.out.println(Arrays.deepToString(DriftCorrection.reshape(DriftCorrection.arrayListSize(ar3), 1, ar3)));
//        System.out.println(ar3);
//        System.out.println(DriftCorrection.convertIntoArrayList(ar3));
//        System.out.println(Arrays.toString(DriftCorrection.toArray(ar1)));
//        System.out.println(Arrays.deepToString(DriftCorrection.arrayListToArray(ar3)));

//        System.out.println(Arrays.toString(interpAlter(arr1,arr2,arr3)));
    }

//    public static ArrayList<double[]> concatArr(ArrayList<double[]> data, double[][] arr){
//        ArrayList<double[]> returnList = new ArrayList<double[]>();
//
//        for (int i = 0; i < data.size(); i++) {
//            double[] concatenatedArray = new double[data.get(i).length + arr[i].length];
//            System.arraycopy(data.get(i), 0, concatenatedArray, 0, data.get(i).length);
//            System.arraycopy(arr[i], 0, concatenatedArray, data.get(i).length, arr[i].length);
//            returnList.add(concatenatedArray);
//        }
//        return returnList;
//    }
















//
//
//
//    double[][] f1 = normalise_dtype(wlArray[0], true, 4096);
//    ArrayList<Double> fxx = zeros1col(gp.burst);
//    ArrayList<Double> fyy = zeros1col(gp.burst);
//
//    for(int i = 1; i < gp.burst; i++ ){
//        double[][] f = normalise_dtype(wlArray[i], true, 4096);
//        double[][] newF = realToComplex(f);
//        double[][] newF1 = realToComplex(f1);
//
//        DoubleFFT_2D fft2 = new DoubleFFT_2D(f1.length, f1[0].length);
//        fft2.complexForward(newF);
//        fft2.complexForward(newF1);
//        Dftresult r = dftregistration(convertToComplex(newF1),convertToComplex(newF),(int) gp.upFactor);
//        fxx.set(i,r.getOutput()[3]);
//        fyy.set(i,r.getOutput()[2]);
//    }
//    double avFxx = getMean(fxx);
//    double avFyy = getMean(fyy);
//    ArrayList<double[]> xxx = new ArrayList<>();
//    ArrayList<double[]> yyy = new ArrayList<>();
//
//    double max = getMax(threed_data[4]);
//    for(int i = 1; i < Math.ceil(max / gp.cycle) +1; i ++){
//        ArrayList<Double> Gxx1 = zeros1col(gp.burst);
//        ArrayList<Double> Gyy1 = zeros1col(gp.burst);
//        ArrayList<Double> Gxx2 = zeros1col(gp.burst);
//        ArrayList<Double> Gyy2 = zeros1col(gp.burst);
//
//        for(int j = 0; j < gp.burst; j++) {
//            if (i != 1) {
//                int frameAbs1 = (int) (2 * (i - 1) * gp.burst + j);
//                double[][] g1 = normalise_dtype(wlArray[frameAbs1], true, 4096);
//                double[][] newG1 = realToComplex(g1);
//                double[][] newF1 = realToComplex(f1);
//
//                DoubleFFT_2D fft2 = new DoubleFFT_2D(f1.length, f1[0].length);
//                fft2.complexForward(newG1);
//                fft2.complexForward(newF1);
//                Dftresult r = dftregistration(convertToComplex(newF1), convertToComplex(newG1), (int) gp.upFactor);
//                Gxx1.set(j, r.getOutput()[3]);
//                Gyy1.set(j, r.getOutput()[2]);
//            }
//            int frameAbs2 = (int) ((2 * i - 1) * gp.burst + j);
//            double[][] g2 = normalise_dtype(wlArray[frameAbs2], true, 4096);
//            double[][] newG2 = realToComplex(g2);
//            double[][] newF1 = realToComplex(f1);
//
//            DoubleFFT_2D fft2 = new DoubleFFT_2D(f1.length, f1[0].length);
//            fft2.complexForward(newG2);
//            fft2.complexForward(newF1);
//            Dftresult r = dftregistration(convertToComplex(newF1), convertToComplex(newG2), (int) gp.upFactor);
//            Gxx2.set(j, r.getOutput()[3]);
//            Gyy2.set(j, r.getOutput()[2]);
//        }
//        double avGxx1 = 0;
//        double avGyy1 = 0;
//        if(i == 1){
//            avGxx1 = avFxx;
//            avGyy1 = avFyy;
//        }else{
//            avGxx1 = getMean(Gxx1);
//            avGyy1 = getMean(Gyy1);
//        }
//        double avGxx2 = getMean(Gxx2);
//        double avGyy2 = getMean(Gyy2);
//
//        double[] ary = linspace(0, gp.cycle,(int) gp.cycle);
//        double[] xx = interp(ary, new double[]{0,gp.cycle}, new double[]{avGxx1, avGxx2});
//        double[] yy = interp(ary, new double[]{0,gp.cycle}, new double[]{avGyy1, avGyy2});
//        xxx.add(xx);
//        yyy.add(yy);
//
//        for(int j =1; j < gp.cycle+1; j ++){
//            double frameNumber = (i - 1) * gp.cycle + j;
//            ArrayList<Integer> locs = new ArrayList<>();
//            for(int k = 0; k < threed_data[4].length; k ++){
//                if(threed_data[4][k] == frameNumber){
//                    locs.add(k);
//                    if( k+1 < threed_data[4].length && threed_data[4][k+1] > frameNumber ){
//                        break;
//                    }
//                }
//            }
//            if(locs.size() == 0){
//                continue;
//            }
//
//            double[][] data_to_correct = new double[locs.size()][5];
//            for(int k = 0; k < locs.size(); k++){
//                data_to_correct[k] = data_arr[locs.get(k)];
//            }
//
//            double[] corr_x = new double[data_to_correct.length];
//            if(paras.flip_x){
//                for(int k = 0; k < corr_x.length; k ++){
//                    corr_x[k] = data_to_correct[k][0] - xx[j-1] * gp.px_size;
//                }
//            }else {
//                for(int k = 0; k < corr_x.length; k ++){
//                    corr_x[k] = data_to_correct[k][0] + xx[j-1] * gp.px_size;
//                }
//            }
//            double[] corr_y = new double[data_to_correct.length];
//
//            if(paras.flip_y){
//                for(int k = 0; k < corr_y.length; k ++){
//                    corr_y[k] = data_to_correct[k][1] - yy[j-1] * gp.px_size;
//                }
//            }else{
//                for(int k = 0; k < corr_y.length; k ++){
//                    corr_y[k] = data_to_correct[k][1] + yy[j-1] * gp.px_size;
//                }
//            }
//            double[][] corr_x_reshape = reshape(corr_x.length,1,corr_x);
//            double[][] corr_y_reshape = reshape(corr_y.length,1,corr_y);
////                    for(int k = 0; k < corr_y.length; k ++){
////                        corr_y_reshape[k][0] = corr_y[k] ;
////                    }
////                    double[][] slice_data = new double[data_to_correct.length][3];
////                    for(int k = 0; k < data_to_correct.length; k ++){
////                        for(int f = 2,a = 0 ; f < 5; f ++, a ++){
////                            slice_data[k][a] = data_to_correct[j][f];
////                        }
////                    }
////                    System.out.println("j: " + j + " row: " +data_to_correct.length + " col: " + data_to_correct[0].length);
//            double[][] slice_data = arraySliceCol2d(data_to_correct,2);
//            double[][] corrected = new double[slice_data.length][slice_data[0].length +
//                    corr_x_reshape[0].length + corr_y_reshape[0].length];
//            for(int k = 0; k < corrected.length; k++){
//                corrected[k][0] = corr_x_reshape[k][0];
//                corrected[k][1] = corr_y_reshape[k][0];
//                corrected[k][2] = slice_data[k][0];
//                corrected[k][3] = slice_data[k][1];
//                corrected[k][4] = slice_data[k][2];
//
//            }
//            for(int k = 0; k < corrected.length; k ++){
//                data_corrected.add(corrected[k]);
//            }
//        }
//        System.out.println(i);
//    }
//            data_corrected.remove(0);
//    ArrayList<Double> xxxConcatenate = concatenate(xxx);
//    double[][] reshape_xxx = reshape(arrayListSize(xxx),1,toArrayMul(xxxConcatenate, gp.px_size));
//    ArrayList<Double> yyyConcatenate = concatenate(yyy);
//    double[][] reshape_yyy = reshape(arrayListSize(yyy),1,toArrayMul(yyyConcatenate, gp.px_size));
//
//    drift_by_frame = convertIntoArrayList(concatenateAxis1(reshape_xxx,reshape_yyy));

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

















    public static double[] interpAlter(double[] x_interp, double[] x_known, double[] y_known){
        double[]returnArr = new double[x_interp.length];
        int current=0;


        for(double x: x_interp){
            int leftIndex = -1;
            int rightIndex = -1;

            for (int i = 0; i < x_known.length; i++) {
                if (x_known[i] <= x) {
                    leftIndex = i;
                } else {
                    rightIndex = i;
                    break;
                }
            }

            if (leftIndex == -1) {
                leftIndex = 0;
                rightIndex= 1;
            }
            else if(rightIndex == -1){
                leftIndex = x_known.length-2;
                rightIndex= x_known.length-1;
            }
            else{
//                System.out.println(leftIndex + ", " + rightIndex + "\n" );
            }
            double y_interp = y_known[leftIndex] + (y_known[rightIndex]-y_known[leftIndex]) * (x-x_known[leftIndex])/(x_known[rightIndex]-x_known[leftIndex]);
            returnArr[current] = y_interp;
            current += 1;
        }

        return returnArr;
    }



}

