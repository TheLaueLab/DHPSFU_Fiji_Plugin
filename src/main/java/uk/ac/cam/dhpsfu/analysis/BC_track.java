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



import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.tuple.Triple;

public class BC_track {

    public List<Double> x;
    public List<Double> y;
    public List<Double> z;
    public List<Integer> frame;
    public List<Double> intensity;
    public List<Double> distances;

    public BC_track() {
        x = new ArrayList<>();
        y = new ArrayList<>();
        z = new ArrayList<>();
        frame = new ArrayList<>();
        intensity = new ArrayList<>();
        distances = new ArrayList<>();
    }

 // Add a position to the track
    public void addPosition(double newX, double newY, double newZ, int newFrame, double newIntensity) {
        double distance = calcAdjustedDistance(newX, newY, newZ, newFrame, this); // Calculate the distance here

        x.add(newX);
        y.add(newY);
        z.add(newZ);
        frame.add(newFrame);
        intensity.add(newIntensity);
        distances.add(distance);
    }

    /*
    public static double calcAdjustedDistance(double newPositionX, double newPositionY, double newPositionZ, int newFrame, BC_track track) {
        if (track.x.isEmpty()) {
            // Handle the case where the list is empty (no previous positions)
            return 0.0; // You can return a default value or handle it as needed
        }

        double deltaX = newPositionX - track.x.get(track.x.size() - 1);
        double deltaY = newPositionY - track.y.get(track.y.size() - 1);
        double deltaZ = newPositionZ - track.z.get(track.z.size() - 1);
        System.out.println("X:" + deltaX + " Y: " + deltaY + " Z:" + deltaZ);
        double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ) /
                         Math.sqrt(Math.abs(newFrame - track.frame.get(track.frame.size() - 1)));

        return distance;
    }
    */
    public static double calcAdjustedDistance(double newPositionX, double newPositionY, double newPositionZ, int newFrame, BC_track track) {
        if (track.getX_list().isEmpty()) {
            // Handle the case where the list is empty (no previous positions)
            return 0.0; // You can return a default value or handle it as needed
        }

        double deltaX = newPositionX - track.getX_list().get(track.getX_list().size() - 1);
        double deltaY = newPositionY - track.getY_list().get(track.getY_list().size() - 1);
        double deltaZ = newPositionZ - track.getZ_list().get(track.getZ_list().size() - 1);
        //System.out.println("X:" + deltaX + " Y: " + deltaY + " Z:" + deltaZ);
        
        double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ) /
                         Math.sqrt(Math.abs(newFrame - track.getFrame_list().get(track.getFrame_list().size() - 1)));

        return distance;
    }
    
    public double getAverageX() {
        double sumX = 0.0;
        for (double xPos : x) {
            sumX += xPos;
        }
        return sumX / x.size();
    }

    public double getAverageY() {
        double sumY = 0.0;
        for (double yPos : y) {
            sumY += yPos;
        }
        return sumY / y.size();
    }

    public double getAverageZ() {
        double sumZ = 0.0;
        for (double zPos : z) {
            sumZ += zPos;
        }
        return sumZ / z.size();
    }
    
    // Other methods...

    public double getAverageI() {
        if (frame.size() >= 3) {
            double sumIntensity = 0.0;
            for (int i = 1; i < frame.size() - 1; i++) {
                sumIntensity += intensity.get(i);
            }
            return sumIntensity / (frame.size() - 2);
        } else {
            double sumIntensity = 0.0;
            for (double intensityVal : intensity) {
                sumIntensity += intensityVal;
            }
            return sumIntensity / intensity.size();
        }
    }

    public int getDeltaFrames() {
        if (frame.isEmpty()) {
            return 0; // Return 0 if there are no frames
        } else {
            return frame.get(frame.size() - 1) - frame.get(0);
        }
    }

    

    public List<Double> getX_list(){
        return x;
    }
    public List<Double> getY_list(){
        return y;
    }

    public List<Double> getZ_list(){
        return z;
    }
    
    public List<Integer> getFrame_list(){
        return frame;
    }
    
    public List<Double> getI_list(){
        return intensity;
    }
    public List<Double> getDist_list(){
        return distances;
    }
    
 


}
