package com.icuxika.commons.utils.console;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.util.stream.Stream;

public class ConsoleProgressDownloader {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsoleProgressDownloader.class);
    private final StringBuilder progressBarBuilder = new StringBuilder();
    private final char incomplete = '░'; // U+2591 \u2591
    private final char complete = '█'; // U+2588 \u2588

    private int printPercentage = 0;

    public static void downloadFile(URL fileUrl, Path filePath) {
        new ConsoleProgressDownloader(fileUrl, filePath);
        System.out.println();
    }

    private ConsoleProgressDownloader(URL fileUrl, Path filePath) {
        Stream.generate(() -> incomplete).limit(100).forEach(progressBarBuilder::append);
        try (
                ReadableByteChannel rbc = new RBCWrapper(Channels.newChannel(fileUrl.openStream()), contentLength(fileUrl), this);
                FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        } catch (Exception e) {
            LOGGER.error("Downloading failed: " + e.getMessage());
        }
    }

    public void rbcProgressCallback(RBCWrapper rbc, double progress) {
        if (((int) progress) >= printPercentage) {
            printPercentage += 10;
            int intProgress = (int) progress;
            progressBarBuilder.replace(0, intProgress, String.valueOf(complete).repeat(intProgress));
            System.out.print("\r" + String.format("Download progress: %.2f / %.2fM " + progressBarBuilder, toMB(rbc.readSoFar), toMB(rbc.expectedSize)));
            System.out.flush();
        }
    }

    private double toMB(long sizeInBytes) {
        return (double) sizeInBytes / (1024 * 1024);
    }

    private int contentLength(URL url) {
        HttpURLConnection connection;
        int contentLength = -1;

        try {
            HttpURLConnection.setFollowRedirects(false);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            contentLength = connection.getContentLength();
        } catch (Exception e) {
        }
        return contentLength;
    }

    private static final class RBCWrapper implements ReadableByteChannel {
        private long readSoFar;
        private final long expectedSize;
        private final ReadableByteChannel rbc;
        private final ConsoleProgressDownloader progressDownloader;

        RBCWrapper(ReadableByteChannel rbc, long expectedSize, ConsoleProgressDownloader progressDownloader) {
            this.progressDownloader = progressDownloader;
            this.expectedSize = expectedSize;
            this.rbc = rbc;
        }

        public boolean isOpen() {
            return rbc.isOpen();
        }

        public void close() throws IOException {
            rbc.close();
        }

        public int read(ByteBuffer bb) throws IOException {
            int n;
            double progress;

            if ((n = rbc.read(bb)) > 0) {
                readSoFar += n;
                progress = expectedSize > 0 ? (double) readSoFar / (double) expectedSize * 100.0 : -1.0;
                progressDownloader.rbcProgressCallback(this, progress);
            }
            return n;
        }
    }
}
