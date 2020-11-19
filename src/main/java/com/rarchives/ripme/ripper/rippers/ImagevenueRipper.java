package com.rarchives.ripme.ripper.rippers;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.rarchives.ripme.ripper.AbstractHTMLRipper;
import com.rarchives.ripme.ripper.DownloadItem;
import com.rarchives.ripme.ripper.DownloadThreadPool;
import com.rarchives.ripme.utils.Http;
import com.rarchives.ripme.utils.Utils;

public class ImagevenueRipper extends AbstractHTMLRipper {

    // Thread pool for finding direct image links from "image" pages (html)
    private DownloadThreadPool imagevenueThreadPool = new DownloadThreadPool("imagevenue");
    @Override
    public DownloadThreadPool getThreadPool() {
        return imagevenueThreadPool;
    }

    public ImagevenueRipper(URL url) throws IOException {
        super(url);
    }

    @Override
    public String getHost() {
        return "imagevenue";
    }
    @Override
    public String getDomain() {
        return "imagevenue.com";
    }

    @Override
    public String getGID(URL url) throws MalformedURLException {
        Pattern p;
        Matcher m;

        p = Pattern.compile("^https?://.*imagevenue.com/galshow.php\\?gal=([a-zA-Z0-9\\-_]+).*$");
        m = p.matcher(url.toExternalForm());
        if (m.matches()) {
            return m.group(1);
        }

        throw new MalformedURLException(
                "Expected imagevenue gallery format: "
                        + "http://...imagevenue.com/galshow.php?gal=gallery_...."
                        + " Got: " + url);
    }

    @Override
    public Document getFirstPage() throws IOException {
        return Http.url(url).get();
    }

    public List<DownloadItem> getURLsFromPage(Document doc) throws MalformedURLException {
        List<DownloadItem> imageURLs = new ArrayList<>();
        for (Element thumb : doc.select("a[target=_blank]")) {
            imageURLs.add(new DownloadItem(thumb.attr("href")));
        }
        return imageURLs;
    }

    public void downloadURL(DownloadItem downloadItem, int index) {
        ImagevenueImageThread t = new ImagevenueImageThread(downloadItem, index);
        imagevenueThreadPool.addThread(t);
    }

    /**
     * Helper class to find and download images found on "image" pages
     *
     * Handles case when site has IP-banned the user.
     */
    private class ImagevenueImageThread extends Thread {
        private DownloadItem downloadItem;
        private int index;

        ImagevenueImageThread(DownloadItem downloadItem, int index) {
            super();
            this.downloadItem = downloadItem;
            this.index = index;
        }

        @Override
        public void run() {
            fetchImage();
        }

        private void fetchImage() {
            try {
                Document doc = Http.url(downloadItem.url)
                                   .retries(3)
                                   .get();
                // Find image
                Elements images = doc.select("a > img");
                if (images.isEmpty()) {
                    LOGGER.warn("Image not found at " + this.downloadItem);
                    return;
                }
                Element image = images.first();
                String imgsrc = image.attr("src");
                imgsrc = "http://" + this.downloadItem.url.getHost() + "/" + imgsrc;
                // Provide prefix and let the AbstractRipper "guess" the filename
                String prefix = "";
                if (Utils.getConfigBoolean("download.save_order", true)) {
                    prefix = String.format("%03d_", index);
                }
                addURLToDownload(new DownloadItem(new URL(imgsrc)), prefix);
            } catch (IOException e) {
                LOGGER.error("[!] Exception while loading/parsing " + this.downloadItem.url, e);
            }
        }
    }
}