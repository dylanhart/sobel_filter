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
        dialog.addChoice("Should Threshold?", new String[] {"Yes", "No"}, "Yes");
        dialog.showDialog();
        if (!dialog.wasOKed()) return;

        final int bucketCount = (int) dialog.getNextNumber();
        final boolean shouldThreshold = dialog.getNextChoiceIndex() == 0;

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
        double magAvg = 0;

        for (int i = 0; i < length; i++) {
            double x = xDelta.getf(i);
            double y = yDelta.getf(i);

            magData[i] = Math.sqrt(x * x + y * y);
            magAvg += magData[i] / length;

            phaseData[i] = (Math.atan2(y, x) + 2 * Math.PI);
            if (phaseData[i] >= 2 * Math.PI) phaseData[i] -= 2 * Math.PI;
        }

        final double threshold = (shouldThreshold ? magAvg : 0);
        IJ.log("Edgeness Threshold: " + threshold);

        for (int i = 0; i < length; i++) {
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
            double sum = 0;
            this.max = this.min = buckets[0];
            for (double value : buckets) {
                if (value > this.max) this.max = value;
                if (value < this.min) this.min = value;
                sum += value;
            }
            this.mean = sum / buckets.length;

            double devSum = 0;
            for (double value : buckets) {
                devSum += Math.pow(Math.abs(value - this.mean), 2);
            }
            this.stdDev = Math.sqrt(devSum / (buckets.length - 1));

            this.longPixelCount = pixelCount;
            this.histYMax = (int) this.max;
        }

        @Override
        public long[] getHistogram() {
            return buckets;
        }
    }
}
