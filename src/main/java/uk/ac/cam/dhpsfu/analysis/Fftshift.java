/*-
 * #%L
 * Double Helix PSF SMLM analysis tool.
 * %%
 * Copyright (C) 2023 - 2024 Laue Lab
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
package uk.ac.cam.dhpsfu.analysis;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;
import org.jtransforms.fft.DoubleFFT_2D;

import java.util.Arrays;

public class Fftshift {
    public static void main(String[] args) {
        // Example 2D array
//        int[][] image = {
//                {1, 2, 3, 4,21},
//                {5, 6, 7, 8,22},
//                {9, 10, 11, 12,23},
//                {13, 14, 15, 16,24},
//                {17, 18, 19, 20,25}
//        };
//
//        // Perform fftshift on the 2D array
//        int[][] shiftedImage = fftShift(image);
//
//        // Print the shifted array
//        for (int[] row : shiftedImage) {
//            for (int value : row) {
//                System.out.print(value + " ");
//            }
//            System.out.println();
//        }
        int[] abc = {0,1,2,3,4};
        int[] ifftshift1D = ifftShift1D(abc);
        System.out.println(Arrays.toString(ifftshift1D));
    }

    // for 2d array
    public static int[][] fftShift2D(int[][] array){
        return fftShift2D(array,false);

    }
    public static double[][] fftShift2D(double[][] array){
        return fftShift2D(array,false);

    }
    public static Complex[][] fftShift2D(Complex[][] array){
        return fftShift2D(array,false);

    }

    public static int[][] ifftShift2D(int[][] array){
        return fftShift2D(array,true);

    }
    public static double[][] ifftShift2D(double[][] array){
        return fftShift2D(array,true);

    }
    public static Complex[][] ifftShift2D(Complex[][] array){
        return fftShift2D(array,true);

    }

    // for 1d array
    public static int[] ifftShift1D(int[] array){
        return fftShift(array,true);

    }
    public static double[] ifftShift1D(double[] array){
        return fftShift(array,true);

    }
    public static Complex[] ifftShift1D(Complex[] array){
        return fftShift(array,true);

    }

    public static int[] fftShift1D(int[] array){
        return fftShift(array,false);

    }
    public static double[] fftShift1D(double[] array){
        return fftShift(array,false);

    }
    public static Complex[] fftShift1D(Complex[] array){
        return fftShift(array,false);

    }


    // Function to perform fftshift on a 2D array
    public static int[][] fftShift2D(int[][] array, boolean inverse) {
        int rows = array.length;
        int cols = array[0].length;
        int midRow;
        int midCol;

        if(inverse){
            midRow = (int) Math.floor((double) (rows) / 2);
//            System.out.println(midRow);
            midCol = (int) Math.floor((double) (cols) / 2);
//            System.out.println(midCol);
        }else {
            midRow = (int) Math.ceil((double) (rows) / 2);
//            System.out.println(midRow);
            midCol = (int) Math.ceil((double) (cols) / 2);
//            System.out.println(midCol);
        }
        int currentIndex = 0;


        int[][] shiftedArray = new int[rows][cols];

        for(int i = midRow, j = 0; i < rows; i++, j++){
            System.arraycopy(array[i], 0, shiftedArray[j],0, cols);
            currentIndex = j;
//            System.out.println(Arrays.toString(shiftedArray[j]));
        }
//        System.out.println();
        for(int i = currentIndex+1, j = 0; j <midRow; i++, j++){
            System.arraycopy(array[j],0,shiftedArray[i],0,cols);
//            System.out.println(Arrays.toString(shiftedArray[i]));
        }
//        System.out.println(Arrays.deepToString(shiftedArray));
//        System.out.println();

        for(int i = 0; i < rows; i++){
            int[]tem = new int[cols];
            System.arraycopy(shiftedArray[i],0,tem,0,cols);
            int currentIndexCol = 0;
            for(int j = midCol, k = 0; j < cols;j++, k++){
                shiftedArray[i][k] = tem[k + midCol];
                currentIndexCol = k;
            }
            for(int j = currentIndexCol+1,k = 0; k < midCol; j++, k++){
                shiftedArray[i][j] = tem[k];
            }
//            System.out.println(Arrays.toString(shiftedArray[i]));
        }

        return shiftedArray;
    }
    public static double[][] fftShift2D(double[][] array, boolean inverse) {
        int rows = array.length;
        int cols = array[0].length;
        int midRow;
        int midCol;

        if(inverse){
            midRow = (int) Math.floor((double) (rows) / 2);
//            System.out.println(midRow);
            midCol = (int) Math.floor((double) (cols) / 2);
//            System.out.println(midCol);
        }else {
            midRow = (int) Math.ceil((double) (rows) / 2);
//            System.out.println(midRow);
            midCol = (int) Math.ceil((double) (cols) / 2);
//            System.out.println(midCol);
        }
        int currentIndex = 0;


        double[][] shiftedArray = new double[rows][cols];

        for(int i = midRow, j = 0; i < rows; i++, j++){
            System.arraycopy(array[i], 0, shiftedArray[j],0, cols);
            currentIndex = j;
//            System.out.println(Arrays.toString(shiftedArray[j]));
        }
//        System.out.println();
        for(int i = currentIndex+1, j = 0; j <midRow; i++, j++){
            System.arraycopy(array[j],0,shiftedArray[i],0,cols);
//            System.out.println(Arrays.toString(shiftedArray[i]));
        }
//        System.out.println(Arrays.deepToString(shiftedArray));
//        System.out.println();

        for(int i = 0; i < rows; i++){
            double[]tem = new double[cols];
            System.arraycopy(shiftedArray[i],0,tem,0,cols);
            int currentIndexCol = 0;
            for(int j = midCol, k = 0; j < cols;j++, k++){
                shiftedArray[i][k] = tem[k + midCol];
                currentIndexCol = k;
            }
            for(int j = currentIndexCol+1,k = 0; k < midCol; j++, k++){
                shiftedArray[i][j] = tem[k];
            }
//            System.out.println(Arrays.toString(shiftedArray[i]));
        }

        return shiftedArray;
    }
    public static Complex[][] fftShift2D(Complex[][] array, boolean inverse) {
        int rows = array.length;
        int cols = array[0].length;
        int midRow;
        int midCol;

        if(inverse){
            midRow = (int) Math.floor((double) (rows) / 2);
//            System.out.println(midRow);
            midCol = (int) Math.floor((double) (cols) / 2);
//            System.out.println(midCol);
        }else {
            midRow = (int) Math.ceil((double) (rows) / 2);
//            System.out.println(midRow);
            midCol = (int) Math.ceil((double) (cols) / 2);
//            System.out.println(midCol);
        }
        int currentIndex = 0;


        Complex[][] shiftedArray = new Complex[rows][cols];

        for(int i = midRow, j = 0; i < rows; i++, j++){
            System.arraycopy(array[i], 0, shiftedArray[j],0, cols);
            currentIndex = j;
//            System.out.println(Arrays.toString(shiftedArray[j]));
        }
//        System.out.println();
        for(int i = currentIndex+1, j = 0; j <midRow; i++, j++){
            System.arraycopy(array[j],0,shiftedArray[i],0,cols);
//            System.out.println(Arrays.toString(shiftedArray[i]));
        }
//        System.out.println(Arrays.deepToString(shiftedArray));
//        System.out.println();

        for(int i = 0; i < rows; i++){
            Complex[]tem = new Complex[cols];
            System.arraycopy(shiftedArray[i],0,tem,0,cols);
            int currentIndexCol = 0;
            for(int j = midCol, k = 0; j < cols;j++, k++){
                shiftedArray[i][k] = tem[k + midCol];
                currentIndexCol = k;
            }
            for(int j = currentIndexCol+1,k = 0; k < midCol; j++, k++){
                shiftedArray[i][j] = tem[k];
            }
//            System.out.println(Arrays.toString(shiftedArray[i]));
        }

        return shiftedArray;
    }

    public static int[] fftShift(int[] array, boolean inverse) {
//        int rows = array.length;
        int cols = array.length;
//        int midRow;
        int midCol;

        if(inverse){
//            midRow = (int) Math.floor((double) (rows) / 2);
//            System.out.println(midRow);
            midCol = (int) Math.floor((double) (cols) / 2);
//            System.out.println(midCol);
        }else {
//            midRow = (int) Math.ceil((double) (rows) / 2);
//            System.out.println(midRow);
            midCol = (int) Math.ceil((double) (cols) / 2);
//            System.out.println(midCol);
        }
        int currentIndex = 0;


//        int[][] shiftedArray = new int[rows][cols];
        int[] shiftedArray = new int[cols];
        System.arraycopy(array,0,shiftedArray,0,cols);
        int currentIndexCol = 0;
        for(int j = midCol, k = 0; j < cols;j++, k++){
            shiftedArray[k] = array[k + midCol];
            currentIndexCol = k;
        }
        for(int j = currentIndexCol+1,k = 0; k < midCol; j++, k++){
            shiftedArray[j] = array[k];
        }
//            System.out.println(Arrays.toString(shiftedArray[i]));

        return shiftedArray;
    }

    public static double[] fftShift(double[] array, boolean inverse) {
//        int rows = array.length;
        int cols = array.length;
//        int midRow;
        int midCol;

        if(inverse){
//            midRow = (int) Math.floor((double) (rows) / 2);
//            System.out.println(midRow);
            midCol = (int) Math.floor((double) (cols) / 2);
//            System.out.println(midCol);
        }else {
//            midRow = (int) Math.ceil((double) (rows) / 2);
//            System.out.println(midRow);
            midCol = (int) Math.ceil((double) (cols) / 2);
//            System.out.println(midCol);
        }
        int currentIndex = 0;


//        int[][] shiftedArray = new int[rows][cols];
        double[] shiftedArray = new double[cols];
        System.arraycopy(array,0,shiftedArray,0,cols);
        int currentIndexCol = 0;
        for(int j = midCol, k = 0; j < cols;j++, k++){
            shiftedArray[k] = array[k + midCol];
            currentIndexCol = k;
        }
        for(int j = currentIndexCol+1,k = 0; k < midCol; j++, k++){
            shiftedArray[j] = array[k];
        }
//            System.out.println(Arrays.toString(shiftedArray[i]));

        return shiftedArray;
    }

    public static Complex[] fftShift(Complex[] array, boolean inverse) {
//        int rows = array.length;
        int cols = array.length;
//        int midRow;
        int midCol;

        if(inverse){
//            midRow = (int) Math.floor((double) (rows) / 2);
//            System.out.println(midRow);
            midCol = (int) Math.floor((double) (cols) / 2);
//            System.out.println(midCol);
        }else {
//            midRow = (int) Math.ceil((double) (rows) / 2);
//            System.out.println(midRow);
            midCol = (int) Math.ceil((double) (cols) / 2);
//            System.out.println(midCol);
        }
        int currentIndex = 0;


//        int[][] shiftedArray = new int[rows][cols];
        Complex[] shiftedArray = new Complex[cols];
        System.arraycopy(array,0,shiftedArray,0,cols);
        int currentIndexCol = 0;
        for(int j = midCol, k = 0; j < cols;j++, k++){
            shiftedArray[k] = array[k + midCol];
            currentIndexCol = k;
        }
        for(int j = currentIndexCol+1,k = 0; k < midCol; j++, k++){
            shiftedArray[j] = array[k];
        }
//            System.out.println(Arrays.toString(shiftedArray[i]));

        return shiftedArray;
    }




}
