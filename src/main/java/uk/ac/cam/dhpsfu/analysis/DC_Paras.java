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

public class DC_Paras {

	public boolean wl_occasional;
	public boolean correction_twice;
	public boolean flip_x;
	public boolean flip_y;
	public boolean average_drift;
	public boolean group_burst;
	public boolean save_DC_WL;

	public DC_Paras(boolean wl_occasional, boolean correction_twice, boolean flip_x, boolean flip_y,
			boolean average_drift, boolean group_burst, boolean save_DC_WL) {
		this.wl_occasional = wl_occasional;
		this.correction_twice = correction_twice;
		this.flip_x = flip_x;
		this.flip_y = flip_y;
		this.average_drift = average_drift;
		this.group_burst = group_burst;
		this.save_DC_WL = save_DC_WL;
	}

	public boolean getWl_occasional() {
		return wl_occasional;
	}

	public void setWl_occasional(boolean wl_occasional) {
		this.wl_occasional = wl_occasional;
	}

	public boolean getCorrectionTwice() {
		return correction_twice;
	}

	public void setCorrection_twice(boolean correction_twice) {
		this.correction_twice = correction_twice;
	}

	public boolean getFlip_x() {
		return flip_x;
	}

	public void setFlip_x(boolean flip_x) {
		this.flip_x = flip_x;
	}

	public boolean getFlip_y() {
		return flip_y;
	}

	public void setFlip_y(boolean flip_y) {
		this.flip_y = flip_y;
	}

	public boolean getAverage_drift() {
		return average_drift;
	}

	public void setAverage_drift(boolean average_drift) {
		this.average_drift = average_drift;
	}

	public boolean getGroup_burst() {
		return group_burst;
	}

	public void setGroup_burst(boolean group_burst) {
		this.group_burst = group_burst;
	}

	public boolean getSave_DC_WL() {
		return save_DC_WL;
	}

	public void setSave_DC_WL(boolean save_DC_WL) {
		this.save_DC_WL = save_DC_WL;
	}
}
