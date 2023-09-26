package com.icuxika.commons.utils.console;

import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ConsoleProgressDownloaderTest {
    @Test
    void downloadFile() {
        try {
            URL url = new URI("https://download.oracle.com/graalvm/21/latest/graalvm-jdk-21_windows-x64_bin.zip").toURL();
            Path target = Path.of(System.getProperty("user.home")).resolve("Downloads").resolve("temp").resolve("result.zip");
            ConsoleProgressDownloader.downloadFile(url, target);
            assertTrue(Files.exists(target), "File does not exist");
        } catch (MalformedURLException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

}