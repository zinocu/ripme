package com.rarchives.ripme.ripper;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import org.apache.log4j.Logger;

import com.rarchives.ripme.ui.RipStatusMessage.STATUS;
import com.rarchives.ripme.utils.Utils;

/**
 * Thread for downloading files.
 * Includes retry logic, observer notifications, and other goodies.
 */
class DownloadVideoThread extends Thread {

    private static final Logger logger = Logger.getLogger(DownloadVideoThread.class);

    private DownloadItem downloadItem;
    private File saveAs;
    private String prettySaveAs;
    private AbstractRipper observer;
    private int retries;

    public DownloadVideoThread(DownloadItem downloadItem, File saveAs, AbstractRipper observer) {
        super();
        this.downloadItem = downloadItem;
        this.saveAs = saveAs;
        this.prettySaveAs = Utils.removeCWD(saveAs);
        this.observer = observer;
        this.retries = Utils.getConfigInteger("download.retries", 1);
    }

    /**
     * Attempts to download the file. Retries as needed.
     * Notifies observers upon completion/error/warn.
     */
    public void run() {
        try {
            observer.stopCheck();
        } catch (IOException e) {
            observer.downloadErrored(downloadItem, "Download interrupted");
            return;
        }
        if (saveAs.exists()) {
            if (Utils.getConfigBoolean("file.overwrite", false)) {
                logger.info("[!] Deleting existing file" + prettySaveAs);
                saveAs.delete();
            } else {
                logger.info("[!] Skipping " + downloadItem.url + " -- file already exists: " + prettySaveAs);
                observer.downloadExists(downloadItem, saveAs);
                return;
            }
        }

        int bytesTotal, bytesDownloaded = 0;
        try {
            bytesTotal = getTotalBytes(downloadItem.url);
        } catch (IOException e) {
            logger.error("Failed to get file size at " + downloadItem.url, e);
            observer.downloadErrored(downloadItem, "Failed to get file size of " + downloadItem.url);
            return;
        }
        observer.setBytesTotal(bytesTotal);
        observer.sendUpdate(STATUS.TOTAL_BYTES, bytesTotal);
        logger.debug("Size of file at " + downloadItem.url + " = " + bytesTotal + "b");
        
        URL downloadUrl = downloadItem.url;
        int tries = 0; // Number of attempts to download
        do {
            InputStream bis = null; OutputStream fos = null;
            byte[] data = new byte[1024 * 256];
            int bytesRead;
            try {
                logger.info("    Downloading file: " + downloadUrl + (tries > 0 ? " Retry #" + tries : ""));
                observer.sendUpdate(STATUS.DOWNLOAD_STARTED, downloadUrl.toExternalForm());

                // Setup HTTP request
                HttpURLConnection huc;
                if (downloadUrl.toString().startsWith("https")) {
                    huc = (HttpsURLConnection) downloadUrl.openConnection();
                }
                else {
                    huc = (HttpURLConnection) downloadUrl.openConnection();
                }
                huc.setInstanceFollowRedirects(true);
                huc.setConnectTimeout(0); // Never timeout
                huc.setRequestProperty("accept",  "*/*");
                huc.setRequestProperty("Referer", downloadUrl.toExternalForm()); // Sic
                huc.setRequestProperty("User-agent", AbstractRipper.USER_AGENT);
                tries += 1;
                logger.debug("Request properties: " + huc.getRequestProperties().toString());
                huc.connect();
                // Check status code
                bis = new BufferedInputStream(huc.getInputStream());
                fos = new FileOutputStream(saveAs);
                while ( (bytesRead = bis.read(data)) != -1) {
                    try {
                        observer.stopCheck();
                    } catch (IOException e) {
                        observer.downloadErrored(downloadItem, "Download interrupted");
                        return;
                    }
                    fos.write(data, 0, bytesRead);
                    bytesDownloaded += bytesRead;
                    observer.setBytesCompleted(bytesDownloaded);
                    observer.sendUpdate(STATUS.COMPLETED_BYTES, bytesDownloaded);
                }
                bis.close();
                fos.close();
                break; // Download successful: break out of infinite loop
            } catch (IOException e) {
                logger.error("[!] Exception while downloading file: " + downloadUrl + " - " + e.getMessage(), e);
            } finally {
                // Close any open streams
                try {
                    if (bis != null) { bis.close(); }
                } catch (IOException e) { }
                try {
                    if (fos != null) { fos.close(); }
                } catch (IOException e) { }
            }
            if (tries > this.retries) {
                logger.error("[!] Exceeded maximum retries (" + this.retries + ") for URL " + downloadUrl);
                observer.downloadErrored(downloadItem, "Failed to download " + downloadUrl.toExternalForm());
                return;
            }
        } while (true);
        observer.downloadCompleted(downloadItem, saveAs);
        logger.info("[+] Saved " + downloadUrl + " as " + this.prettySaveAs);
    }

    /**
     * @param url
     *      Target URL
     * @return 
     *      Returns connection length
     */
    private int getTotalBytes(URL url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("HEAD");
        conn.setRequestProperty("accept",  "*/*");
        conn.setRequestProperty("Referer", this.downloadItem.url.toExternalForm()); // Sic
        conn.setRequestProperty("User-agent", AbstractRipper.USER_AGENT);
        return conn.getContentLength();
    }

}