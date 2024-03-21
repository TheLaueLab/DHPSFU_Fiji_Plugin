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
