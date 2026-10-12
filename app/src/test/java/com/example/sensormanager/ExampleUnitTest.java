package com.example.sensormanager;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit test for SensorManager calculation and threshold logic.
 */
public class ExampleUnitTest {

    @Test
    public void testVectorMagnitudeCalculation() {
        float x = 3.0f;
        float y = 4.0f;
        float z = 0.0f;

        double magnitude = Math.sqrt(x * x + y * y + z * z);
        assertEquals(5.0, magnitude, 0.001);
    }

    @Test
    public void testEarthGravityMagnitude() {
        float x = 0.0f;
        float y = 0.0f;
        float z = 9.81f;

        double magnitude = Math.sqrt(x * x + y * y + z * z);
        assertEquals(9.81, magnitude, 0.01);
    }

    @Test
    public void testThresholdEvaluation() {
        float threshold = 12.0f;
        double normalMagnitude = 9.81;
        double highMagnitude = 15.2;

        assertTrue(normalMagnitude < threshold);
        assertTrue(highMagnitude >= threshold);
    }
}
