package uk.ac.cam.dhpsfu.analysis;

public class ArrayUtils {
	public static double[] getColumn(double[][] data, int columnIndex) {
        int numRows = data.length;
        double[] column = new double[numRows];

        for (int i = 0; i < numRows; i++) {
            column[i] = data[i][columnIndex];
        }

        return column;
    }

}
