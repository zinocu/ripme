package com.rarchives.ripme.ripper;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;
import com.rarchives.ripme.ui.RipStatusMessage;
import com.rarchives.ripme.ui.RipStatusMessage.STATUS;
import com.rarchives.ripme.utils.Utils;

/**
 * Simplified ripper, designed for ripping from sites by parsing JSON.
 */
public abstract class AbstractJSONRipper extends AbstractRipper {
    protected AbstractJSONRipper(URL url) throws IOException {
        super(url);
    }

    protected abstract String getDomain();
    @Override
    public abstract String getHost();

    protected abstract JSONObject getFirstPage() throws IOException;
    protected JSONObject getNextPage(JSONObject doc) throws IOException {
        throw new IOException("getNextPage not implemented");
    }
    protected abstract List<DownloadItem> getURLsFromJSON(JSONObject json) throws IOException;
    protected abstract void downloadURL(DownloadItem downloadItem, int index);
    private DownloadThreadPool getThreadPool() {
        return null;
    }

    protected boolean keepSortOrder() {
        return true;
    }

    @Override
    public boolean canRip(URL url) {
        return url.getHost().endsWith(getDomain());
    }

    @Override
    public URL sanitizeURL(URL url) throws MalformedURLException {
        return url;
    }

    @Override
    public void rip() throws IOException {
        int index = 0;
        LOGGER.info("Retrieving " + this.url);
        sendUpdate(STATUS.LOADING_RESOURCE, this.url.toExternalForm());
        JSONObject json = getFirstPage();

        while (json != null) {
            List<DownloadItem> downloadItems = getURLsFromJSON(json);
            
            if (alreadyDownloadedUrls >= Utils.getConfigInteger("history.end_rip_after_already_seen", 1000000000) && !isThisATest()) {
                 sendUpdate(STATUS.DOWNLOAD_COMPLETE, "Already seen the last " + alreadyDownloadedUrls + " images ending rip");
                 break;
            }
            
            // Remove all but 1 image
            if (isThisATest()) {
                while (downloadItems.size() > 1) {
                    downloadItems.remove(1);
                }
            }

            if (downloadItems.isEmpty() && !hasASAPRipping()) {
                throw new IOException("No images found at " + this.url);
            }

            for (DownloadItem downloadItem : downloadItems) {
                if (isStopped()) {
                    break;
                }
                
                index += 1;
                LOGGER.debug("Found image url #" + index+ ": " + downloadItem);
                downloadURL(downloadItem, index);
            }

            if (isStopped() || isThisATest()) {
                break;
            }

            try {
                sendUpdate(STATUS.LOADING_RESOURCE, "next page");
                json = getNextPage(json);
            } catch (IOException e) {
                LOGGER.info("Can't get next page: " + e.getMessage());
                break;
            }
        }

        // If they're using a thread pool, wait for it.
        if (getThreadPool() != null) {
            LOGGER.debug("Waiting for threadpool " + getThreadPool().getClass().getName());
            getThreadPool().waitForThreads();
        }
        waitForThreads();
    }

    protected String getPrefix(int index) {
        String prefix = "";
        if (keepSortOrder() && Utils.getConfigBoolean("download.save_order", true)) {
            prefix = String.format("%03d_", index);
        }
        return prefix;
    }
    
    /*
     * ------ Methods copied from AlbumRipper ------
     */
    
    protected boolean allowDuplicates() {
        return false;
    }

    @Override
    /**
     * Returns total amount of files attempted.
     */
    public int getCount() {
        return itemsCompleted.size() + itemsErrored.size();
    }

    @Override
    /**
     * Queues multiple URLs of single images to download from a single Album URL
     */
    public boolean addURLToDownload(DownloadItem downloadItem, File saveAs, String referrer, Map<String,String> cookies, Boolean getFileExtFromMIME) {
        // Only download one file if this is a test.
        if (super.isThisATest() &&
                (itemsPending.size() > 0 || itemsCompleted.size() > 0 || itemsErrored.size() > 0)) {
            stop();
            return false;
        }
        if (!allowDuplicates()
                && ( itemsPending.containsKey(downloadItem)
                  || itemsCompleted.containsKey(downloadItem)
                  || itemsErrored.containsKey(downloadItem) )) {
            // Item is already downloaded/downloading, skip it.
            LOGGER.info("[!] Skipping " + downloadItem.url + " -- already attempted: " + Utils.removeCWD(saveAs));
            return false;
        }
        if (Utils.getConfigBoolean("urls_only.save", false)) {
            // Output URL to file
            String urlFile = this.workingDir + File.separator + "urls.txt";
            try (FileWriter fw = new FileWriter(urlFile, true)) {
                fw.write(downloadItem.url.toExternalForm());
                fw.write(System.lineSeparator());
                itemsCompleted.put(downloadItem, new File(urlFile));
            } catch (IOException e) {
                LOGGER.error("Error while writing to " + urlFile, e);
            }
        }
        else {
            itemsPending.put(downloadItem, saveAs);
            DownloadFileThread dft = new DownloadFileThread(downloadItem,  saveAs,  this, getFileExtFromMIME);
            if (referrer != null) {
                dft.setReferrer(referrer);
            }
            if (cookies != null) {
                dft.setCookies(cookies);
            }
            threadPool.addThread(dft);
        }

        return true;
    }

    @Override
    public boolean addURLToDownload(DownloadItem downloadItem, File saveAs) {
        return addURLToDownload(downloadItem, saveAs, null, null, false);
    }

    /**
     * Queues image to be downloaded and saved.
     * Uses filename from URL to decide filename.
     * @param downloadItem
     *      URL to download
     * @return
     *      True on success
     */
    protected boolean addURLToDownload(DownloadItem downloadItem) {
        // Use empty prefix and empty subdirectory
        return addURLToDownload(downloadItem, "", "");
    }

    @Override
    /**
     * Cleans up & tells user about successful download
     */
    public void downloadCompleted(DownloadItem downloadItem, File saveAs) {
        if (observer == null) {
            return;
        }
        try {
            String path = Utils.removeCWD(saveAs);
            RipStatusMessage msg = new RipStatusMessage(STATUS.DOWNLOAD_COMPLETE, path);
            itemsPending.remove(downloadItem);
            itemsCompleted.put(downloadItem, saveAs);
            observer.update(this, msg);

            checkIfComplete();
        } catch (Exception e) {
            LOGGER.error("Exception while updating observer: ", e);
        }
    }

    @Override
    /**
     * Cleans up & tells user about failed download.
     */
    public void downloadErrored(DownloadItem downloadItem, String reason) {
        if (observer == null) {
            return;
        }
        itemsPending.remove(downloadItem);
        itemsErrored.put(downloadItem, reason);
        observer.update(this, new RipStatusMessage(STATUS.DOWNLOAD_ERRORED, downloadItem.url + " : " + reason));

        checkIfComplete();
    }

    @Override
    /**
     * Tells user that a single file in the album they wish to download has
     * already been downloaded in the past.
     */
    public void downloadExists(DownloadItem downloadItem, File file) {
        if (observer == null) {
            return;
        }

        itemsPending.remove(downloadItem);
        itemsCompleted.put(downloadItem, file);
        observer.update(this, new RipStatusMessage(STATUS.DOWNLOAD_WARN, downloadItem.url + " already saved as " + file.getAbsolutePath()));

        checkIfComplete();
    }

    /**
     * Notifies observers and updates state if all files have been ripped.
     */
    @Override
    protected void checkIfComplete() {
        if (observer == null) {
            return;
        }
        if (itemsPending.isEmpty()) {
            super.checkIfComplete();
        }
    }

    /**
     * Sets directory to save all ripped files to.
     * @param url
     *      URL to define how the working directory should be saved.
     * @throws
     *      IOException
     */
    @Override
    public void setWorkingDir(URL url) throws IOException {
        String path = Utils.getWorkingDirectory().getCanonicalPath();
        if (!path.endsWith(File.separator)) {
            path += File.separator;
        }
        String title;
        if (Utils.getConfigBoolean("album_titles.save", true)) {
            title = getAlbumTitle(this.url);
        } else {
            title = super.getAlbumTitle(this.url);
        }
        LOGGER.debug("Using album title '" + title + "'");

        title = Utils.filesystemSafe(title);
        path += title;
        path = Utils.getOriginalDirectory(path) + File.separator;   // check for case sensitive (unix only)

        this.workingDir = new File(path);
        if (!this.workingDir.exists()) {
            LOGGER.info("[+] Creating directory: " + Utils.removeCWD(this.workingDir));
            this.workingDir.mkdirs();
        }
        LOGGER.debug("Set working directory to: " + this.workingDir);
    }

    /**
     * @return
     *      Integer between 0 and 100 defining the progress of the album rip.
     */
    @Override
    public int getCompletionPercentage() {
        double total = itemsPending.size()  + itemsErrored.size() + itemsCompleted.size();
        return (int) (100 * ( (total - itemsPending.size()) / total));
    }

    /**
     * @return
     *      Human-readable information on the status of the current rip.
     */
    @Override
    public String getStatusText() {
        StringBuilder sb = new StringBuilder();
        sb.append(getCompletionPercentage())
          .append("% ")
          .append("- Pending: "  ).append(itemsPending.size())
          .append(", Completed: ").append(itemsCompleted.size())
          .append(", Errored: "  ).append(itemsErrored.size());
        return sb.toString();
    }

    
}
