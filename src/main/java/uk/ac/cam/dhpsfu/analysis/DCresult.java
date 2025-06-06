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

import java.util.ArrayList;

public class DCresult {
    ArrayList<double[]> data_corrected;
    double[][] drift_by_loc;
    ArrayList<ArrayList<Double>> drift_by_frame;

    public DCresult(ArrayList<double[]> data_corrected, double[][] drift_by_loc, ArrayList<ArrayList<Double>> drift_by_frame) {
        this.data_corrected = data_corrected;
        this.drift_by_frame = drift_by_frame;
        this.drift_by_loc = drift_by_loc;
    }

	public ArrayList<double[]> getData_corrected() {
        return data_corrected;
    }

    public ArrayList<ArrayList<Double>> getDrift_by_frame() {
        return drift_by_frame;
    }

    public double[][] getDrift_by_loc() {
        return drift_by_loc;
    }

    public void setData_corrected(ArrayList<double[]> data_corrected) {
        this.data_corrected = data_corrected;
    }

    public void setDrift_by_frame(ArrayList<ArrayList<Double>> drift_by_frame) {
        this.drift_by_frame = drift_by_frame;
    }

    public void setDrift_by_loc(double[][] drift_by_loc) {
        this.drift_by_loc = drift_by_loc;
    }
}
