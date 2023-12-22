package org.gaussx.androfono;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.filters.LowPassFS;
public class AudioProcessor {

    private static final int SAMPLE_RATE = 44100;
    private static final int CUTOFF_FREQUENCY = 10; // Hz
    private static final double RC = 1.0 / (2.0 * Math.PI * CUTOFF_FREQUENCY);

    private static double previousFilteredValue = 0;

    public static void processAudioBuffer(byte[] buffer, int bytesRead) {
        for (int i = 0; i < bytesRead; i += 2) {
            // Combina i byte adiacenti per formare un valore short
            short sample = (short) ((buffer[i + 1] << 8) | (buffer[i] & 0xFF));

            // Normalizza il valore a [-1, 1]
            double normalizedSample = sample / 32768.0;

            // Applica il filtro passa-basso
            double filteredValue = previousFilteredValue + (normalizedSample - previousFilteredValue) * RC;
            previousFilteredValue = filteredValue;

            // Denormalizza il valore e scrivi i byte nel buffer
            short outputSample = (short) (filteredValue * 32768.0);
            buffer[i] = (byte) outputSample;
            buffer[i + 1] = (byte) (outputSample >> 8);
        }
    }
}