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
package uk.ac.cam.dhpsfu.analysis;

public class arrayFormatTransfer {
	public static void main(String[] args) {
	}

	// Function to print the array in Python array format
	public static void printPythonArray(int[][] array) {
		System.out.print("[");
		for (int i = 0; i < array.length; i++) {
			System.out.print("[");
			for (int j = 0; j < array[i].length; j++) {
				System.out.print(array[i][j]);
				if (j < array[i].length - 1) {
					System.out.print(", ");
				}
			}
			System.out.print("]");
			if (i < array.length - 1) {
				System.out.print(", ");
			}
			System.out.println();
		}
		System.out.println("]");
	}

	public static int[][] convertToJavaArray(String pythonArray) {
		String[] rowStrings = pythonArray.substring(2, pythonArray.length() - 2).split("\\]\\s*\n\\s*\\[");

		// Initialize the 2D array in Java
		int[][] javaArray = new int[rowStrings.length][];

		// Iterate over the row strings
		for (int i = 0; i < rowStrings.length; i++) {
			// Remove leading and trailing spaces and split the row string into individual
			// number strings
			String[] numberStrings = rowStrings[i].trim().split("\\s+");

			// Initialize the current row in the Java array
			javaArray[i] = new int[numberStrings.length];

			// Iterate over the number strings
			for (int j = 0; j < numberStrings.length; j++) {
				// Convert the number string into an integer and store it in the Java array
				javaArray[i][j] = Integer.parseInt(numberStrings[j]);
			}
		}

		return javaArray;
	}
}
