import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.HistogramWindow;
import ij.plugin.filter.PlugInFilter;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;

import java.util.Arrays;

public class SobelEdgeness_ implements PlugInFilter {

    private final static int BUCKET_COUNT = 360;
    private final static double THRESHOLD = 0;

    @Override
    public int setup(String arg, ImagePlus imp) {
        return DOES_8G | DOES_16 | DOES_32 | NO_CHANGES | NO_UNDO;
    }

    @Override
    public void run(ImageProcessor ip) {
        GenericDialog dialog = new GenericDialog("Sobel Edgeness");
        dialog.addNumericField("Bucket Count", BUCKET_COUNT, 0);
        dialog.addNumericField("Threshold", THRESHOLD, 0);
        dialog.showDialog();
        if (!dialog.wasOKed()) return;

        final int bucketCount = (int) dialog.getNextNumber();
        final double threshold = dialog.getNextNumber();

        FloatProcessor fp = ip.convertToFloatProcessor();

        FloatProcessor xDelta = (FloatProcessor) fp.duplicate();
        xDelta.convolve3x3(new int[] {1, 0, -1, 2, 0, -2, 1, 0, -1});

        FloatProcessor yDelta = (FloatProcessor) fp.duplicate();
        yDelta.convolve3x3(new int[] {1, 2, 1, 0, 0, 0, -1, -2, -1});

        int length = fp.getWidth() * fp.getHeight();

        double[] buckets = new double[bucketCount];
        final double bucketWidth = (2*Math.PI / bucketCount);

        double[] magData = new double[length];
        double[] phaseData = new double[length];

        for (int i = 0; i < length; i++) {
            double x = xDelta.getf(i);
            double y = yDelta.getf(i);

            magData[i] = Math.sqrt(x*x + y*y);
            phaseData[i] = (Math.atan2(y, x) + 2*Math.PI);
            if (phaseData[i] >= 2*Math.PI) phaseData[i] -= 2*Math.PI;

            if (magData[i] >= threshold) {
                int bucket = (int) Math.floor(phaseData[i] / bucketWidth);
                buckets[bucket] += magData[i];
            }
        }

        double min = buckets[0], max = buckets[0];
        for (int bucket = 1; bucket < bucketCount; bucket++) {
            if (buckets[bucket] < min) min = buckets[bucket];
            if (buckets[bucket] > max) max = buckets[bucket];
        }

        new HistogramWindow("Sobel Edgeness Histogram", WindowManager.getCurrentImage(), new StatsOverride(buckets, length));

        IJ.log("Raw Sobel Edgeness Buckets:");
        IJ.log(Arrays.toString(buckets));
    }

    private static class StatsOverride extends ImageStatistics {
        private long[] buckets;

        StatsOverride(double[] buckets, long pixelCount) {
            this.buckets = new long[buckets.length];
            this.histMin = 0;
            this.histMax = 360; // or 2*Math.PI

            // convert array to longs because that's what the built in histogram uses
            for (int i = 0; i < buckets.length; i++) {
                this.buckets[i] = (long) buckets[i];
            }

            // calculate statistics
            this.mean = Arrays.stream(buckets).average().orElse(0);
            this.max = Arrays.stream(buckets).max().orElse(0);
            this.min = Arrays.stream(buckets).min().orElse(0);
            // sqrt(sum(squared diff to mean) / N-1)
            this.stdDev = Math.sqrt(
                    Arrays.stream(buckets)
                        .map(x -> Math.abs(x - this.mean))
                        .map(x -> Math.pow(x, 2))
                        .sum() / (buckets.length - 1)
            );
            this.longPixelCount = pixelCount;
        }

        @Override
        public long[] getHistogram() {
            return buckets;
        }
    }
}
