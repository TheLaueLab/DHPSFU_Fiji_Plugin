/*
 * Copyright (C) 2023 GDSC
 *
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
 */

package uk.ac.sussex.gdsc.smlm.ij.example.analysis;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.ac.sussex.gdsc.smlm.data.config.UnitProtos.DistanceUnit;
import uk.ac.sussex.gdsc.smlm.data.config.UnitProtos.IntensityUnit;
import uk.ac.sussex.gdsc.smlm.results.MemoryPeakResults;
import uk.ac.sussex.gdsc.smlm.results.procedures.StandardResultProcedure;

/**
 * Demonstrate using the JUnit 5 test framework to assert expected results.
 */
class LocalisationsTest {

  /**
   * Test the created random localisations are calibrated in pixels and photons.
   * An object is used to obtain all the results in non-native units. The test
   * then accesses all the native data using the forEach method of the results and checks
   * the conversion.
   *
   * <p>Note there are many {@code forEach} method to obtain data in the {@link MemoryPeakResults}.
   * The {@link StandardResultProcedure} provides commonly used options to get data into
   * arrays.
   */
  @Test
  void canCreateRandomResults() {
    MemoryPeakResults results = Localisations.createRandomResults("Test");
    Assertions.assertEquals(DistanceUnit.PIXEL, results.getDistanceUnit(), "distance unit");
    Assertions.assertEquals(IntensityUnit.PHOTON, results.getIntensityUnit(), "intensity unit");

    // Check the calibration allows unit conversion
    double gain = results.getGain();
    double nmPerPixel = results.getNmPerPixel();
    Assertions.assertNotEquals(1, gain, "gain == 1");
    Assertions.assertNotEquals(1, nmPerPixel, "nm/pixel == 1");

    // Use a procedure to obtain the results in (different) desired units
    StandardResultProcedure p = new StandardResultProcedure(results, DistanceUnit.NM, IntensityUnit.COUNT);
    // This method will obtain the {Background, Intensity, X, Y, Z} in the configured
    // units and save them in arrays
    p.getBixyz();

    // Here we check scaling was done correctly: px to nm; and photons to counts

    // The consumer used in the forEach method does not know the current index so
    // we maintain it in an array that can be updated within the consumer.
    int[] index = {0};
    results.forEachNative((b, i, x, y, z) -> {
      int j = index[0]++;
      Assertions.assertEquals((float) (b * gain), p.background[j]);
      Assertions.assertEquals((float) (i * gain), p.intensity[j]);
      Assertions.assertEquals((float) (x * nmPerPixel), p.x[j]);
      Assertions.assertEquals((float) (y * nmPerPixel), p.y[j]);
      Assertions.assertEquals((float) (z * nmPerPixel), p.z[j]);
    });
  }
}
