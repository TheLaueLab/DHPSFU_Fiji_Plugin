package uk.ac.cam.dhpsfu.analysis;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;


import java.util.ArrayList;
import java.util.List;


public class Read3DFileCalib {
	 public List<List<String>> readCSV(Path filePath, int skipLines) throws IOException {
	        List<String> lines = Files.readAllLines(filePath);

	        if (lines.isEmpty()) {
	            throw new IllegalArgumentException("The CSV file must have more than 8 lines.");
	        }

	        List<List<String>> data = new ArrayList<>();

	        for (int i = 8; i < lines.size(); i++) {
	            String line = lines.get(i);
	            String[] values = line.split("\t");
	            List<String> row = new ArrayList<>();
	            for (String value : values) {
	                row.add(value);
	            }
	            data.add(row);
	        }

	        return data;
	   
	    }
	 
	 public List<List<String>> readCSVfile(Path filePath, int skipLines) throws IOException {
	        List<String> lines = Files.readAllLines(filePath);

	        if (lines.isEmpty()) {
	            throw new IllegalArgumentException("The CSV file must have more than 8 lines.");
	        }

	        List<List<String>> data = new ArrayList<>();

	        for (int i = 8; i < lines.size(); i++) {
	            String line = lines.get(i);
	            String[] values = line.split(",");
	            List<String> row = new ArrayList<>();
	            for (String value : values) {
	                row.add(value);
	            }
	            data.add(row);
	        }

	        return data;
	   
	    }
	 
	   public double[][] readCSVDouble(Path filePath, int linesToSkip) throws IOException {
	        List<String> lines = Files.readAllLines(filePath);

	        if (lines.size() <= linesToSkip) {
	            throw new IllegalArgumentException("The CSV file must have more lines than the number to skip.");
	        }

	        List<List<Double>> data = new ArrayList<>();

	        for (int i = linesToSkip; i < lines.size(); i++) {
	            String line = lines.get(i);
	            String[] values = line.split("\t");
	            List<Double> row = new ArrayList<>();
	            for (String value : values) {
	                try {
	                    row.add(Double.parseDouble(value));
	                } catch (NumberFormatException e) {
	                    System.err.println("Invalid number format: " + value);
	                    row.add(null);
	                }
	            }
	            data.add(row);
	        }

	        int numRows = data.size();
	        int numCols = data.get(0).size();
	        double[][] dataArray = new double[numRows][numCols];

	        for (int i = 0; i < numRows; i++) {
	            for (int j = 0; j < numCols; j++) {
	                dataArray[i][j] = data.get(i).get(j);
	            }
	        }

	        return dataArray;
	    }
	   
	   public double[][] readCSVDoubleFile(Path filePath, int linesToSkip) throws IOException {
	        List<String> lines = Files.readAllLines(filePath);

	        if (lines.size() <= linesToSkip) {
	            throw new IllegalArgumentException("The CSV file must have more lines than the number to skip.");
	        }

	        List<List<Double>> data = new ArrayList<>();

	        for (int i = linesToSkip; i < lines.size(); i++) {
	            String line = lines.get(i);
	            String[] values = line.split(",");
	            List<Double> row = new ArrayList<>();
	            for (String value : values) {
	                try {
	                    row.add(Double.parseDouble(value));
	                } catch (NumberFormatException e) {
	                    System.err.println("Invalid number format: " + value);
	                    row.add(null);
	                }
	            }
	            data.add(row);
	        }

	        int numRows = data.size();
	        int numCols = data.get(0).size();
	        double[][] dataArray = new double[numRows][numCols];

	        for (int i = 0; i < numRows; i++) {
	            for (int j = 0; j < numCols; j++) {
	                dataArray[i][j] = data.get(i).get(j);
	            }
	        }

	        return dataArray;
	    }
	 
	

}
