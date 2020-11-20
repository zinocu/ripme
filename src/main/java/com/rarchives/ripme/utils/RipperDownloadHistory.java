package com.rarchives.ripme.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import org.apache.log4j.Logger;

public class RipperDownloadHistory {
    protected static final Logger LOGGER = Logger.getLogger(RipperDownloadHistory.class);
    private final Map<String, RipperHistoryItem> historyItems = Collections.synchronizedMap(new HashMap<String, RipperHistoryItem>());
    private final File urlHistoryFile;
    private boolean hasCreatedFile;
    
    public RipperDownloadHistory(String urlHistoryFileLocation) {
        this.urlHistoryFile = new File(urlHistoryFileLocation).getAbsoluteFile();
    }
    
    public boolean ensureCreatedHistoryFile() {
        if (hasCreatedFile) {
            return true;
        }
        
        try {
            // if file doesn't exist, then create it
            if (!urlHistoryFile.exists()) {
                if (!urlHistoryFile.getParentFile().exists()) {
                    urlHistoryFile.getParentFile().mkdirs();
                }
                
                boolean couldMakeDir = urlHistoryFile.createNewFile();
                if (!couldMakeDir) {
                    LOGGER.error("Couldn't url history file");
                    return false;
                }
            }
        }
        catch (IOException ex) {
            LOGGER.debug("Unable to write URL history file: " + urlHistoryFile.getAbsolutePath());
            ex.printStackTrace();
            return false;
        }
        
        hasCreatedFile = true;
        return true;
    }
    
    public void loadHistoryFile() {
        if (!ensureCreatedHistoryFile()) {
            return;
        }
        
        try (Scanner scanner = new Scanner(urlHistoryFile)) {
            while (scanner.hasNextLine()) {
                final String line = scanner.nextLine();
                RipperHistoryItem item = RipperHistoryItem.fromLine(line);
                historyItems.put(item.downloadUrl, item);
            }
        }
        catch (FileNotFoundException e) {}
    }
    
    public void saveDownloadedURL(RipperHistoryItem item) {
        if (!hasCreatedFile && !ensureCreatedHistoryFile()) {
            return;
        }
        
        BufferedWriter bw = null;
        FileWriter fw = null;
        try {
            if (!urlHistoryFile.canWrite()) {
                LOGGER.error("Can't write to url history file: " + urlHistoryFile);
                return;
            }
            
            historyItems.put(item.downloadUrl, item);
            
            fw = new FileWriter(urlHistoryFile.getAbsoluteFile(), true);
            bw = new BufferedWriter(fw);
            bw.write(item.toString() + "\n");
        } catch (IOException e) {
            LOGGER.debug("Unable to write URL history file");
            e.printStackTrace();
        } finally {
            try {
                if (bw != null)
                    bw.close();
                if (fw != null)
                    fw.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
    
    public boolean hasDownloadedURL(String url) {
        return historyItems.containsKey(url);
    }
    
    public static class RipperHistoryItem {
        public final String downloadUrl;
        public final long sourceCreatedTimeSeconds;
        public final String ripperAlbumTitle;
        public final String filePath;
        public final long downloadTimeSeconds;
        
        public RipperHistoryItem(String downloadUrl, long sourceCreatedTimeSeconds, String ripperAlbumTitle, String filePath, long downloadTimeSeconds) {
            this.downloadUrl = downloadUrl;
            this.sourceCreatedTimeSeconds = sourceCreatedTimeSeconds;
            this.ripperAlbumTitle = ripperAlbumTitle;
            this.filePath = filePath;
            this.downloadTimeSeconds = downloadTimeSeconds;
        }
        
        public static RipperHistoryItem fromLine(String line) {
            String downloadUrl = "";
            long sourceCreatedTimeSeconds = 0;
            String ripperAlbumTitle = "";
            String filePath = "";
            long downloadTimeSeconds = 0;
            
            String[] data = line.split("::::");
            downloadUrl = data[0];
            if (data.length > 1) {
                sourceCreatedTimeSeconds = Long.parseLong(data[1]);
            }
            if (data.length > 2) {
                ripperAlbumTitle = data[2];
            }
            if (data.length > 3) {
                filePath = data[3];
            }
            if (data.length > 4) {
                downloadTimeSeconds = Long.parseLong(data[4]);
            }
            
            return new RipperHistoryItem(downloadUrl, sourceCreatedTimeSeconds, ripperAlbumTitle, filePath, downloadTimeSeconds);
        }
        
        @Override
        public String toString() {
            return this.downloadUrl + "::::" +
                this.sourceCreatedTimeSeconds + "::::" +
                this.filePath + "::::" +
                this.downloadTimeSeconds;
        }
    }
}
