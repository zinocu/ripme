package com.rarchives.ripme.ripper.rippers;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.rarchives.ripme.ripper.AbstractHTMLRipper;
import com.rarchives.ripme.ripper.DownloadItem;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.rarchives.ripme.ripper.DownloadThreadPool;
import com.rarchives.ripme.utils.Http;
import com.rarchives.ripme.utils.Utils;

public class PornhubRipper extends AbstractHTMLRipper {
    // All sleep times are in milliseconds
    private static final int IMAGE_SLEEP_TIME    = 1000;

    private static final String DOMAIN = "pornhub.com", HOST = "Pornhub";

    // Thread pool for finding direct image links from "image" pages (html)
    private DownloadThreadPool pornhubThreadPool = new DownloadThreadPool("pornhub");

    public PornhubRipper(URL url) throws IOException {
        super(url);
    }

    @Override
    public String getHost() {
        return HOST;
    }

    @Override
    protected String getDomain() {
        return DOMAIN;
    }

    @Override
    protected Document getFirstPage() throws IOException {
        return Http.url(url).referrer(url).get();
    }

    @Override
    public Document getNextPage(Document page) throws IOException {
        Elements nextPageLink = page.select("li.page_next > a");
        if (nextPageLink.isEmpty()){
            throw new IOException("No more pages");
        } else {
            URL nextURL = new URL(this.url, nextPageLink.first().attr("href"));
            return Http.url(nextURL).get();
        }
    }

    @Override
    protected List<DownloadItem> getURLsFromPage(Document page) throws MalformedURLException {
        List<DownloadItem> pageURLs = new ArrayList<>();
        // Find thumbnails
        Elements thumbs = page.select(".photoBlockBox li");
        // Iterate over thumbnail images on page
        for (Element thumb : thumbs) {
            String imagePage = thumb.select(".photoAlbumListBlock > a")
                    .first().attr("href");
            String fullURL = "https://pornhub.com" + imagePage;
            pageURLs.add(new DownloadItem(fullURL));
        }
        return pageURLs;
    }

    @Override
    protected void downloadURL(DownloadItem downloadItem, int index) {
        PornhubImageThread t = new PornhubImageThread(downloadItem, index, this.workingDir);
        pornhubThreadPool.addThread(t);
        try {
            Thread.sleep(IMAGE_SLEEP_TIME);
        } catch (InterruptedException e) {
            LOGGER.warn("Interrupted while waiting to load next image", e);
        }
    }

    public URL sanitizeURL(URL url) throws MalformedURLException {
        // always start on the first page of an album
        // (strip the options after the '?')
        String u = url.toExternalForm();
        if (u.contains("?")) {
            u = u.substring(0, u.indexOf("?"));
            return new URL(u);
        } else {
            return url;
        }
    }

    @Override
    public String getGID(URL url) throws MalformedURLException {
        Pattern p;
        Matcher m;

        p = Pattern.compile("^.*pornhub\\.com/album/([0-9]+).*$");
        m = p.matcher(url.toExternalForm());
        if (m.matches()) {
            return m.group(1);
        }

        throw new MalformedURLException(
                "Expected pornhub.com album format: "
                        + "http://www.pornhub.com/album/####"
                        + " Got: " + url);
    }

    @Override
    public DownloadThreadPool getThreadPool(){
        return pornhubThreadPool;
    }

    public boolean canRip(URL url) {
        return url.getHost().endsWith(DOMAIN) && url.getPath().startsWith("/album");
    }

    /**
     * Helper class to find and download images found on "image" pages
     *
     * Handles case when site has IP-banned the user.
     */
    private class PornhubImageThread extends Thread {
        private DownloadItem downloadItem;
        private int index;

        PornhubImageThread(DownloadItem downloadItem, int index, File workingDir) {
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
                Document doc = Http.url(this.downloadItem.url)
                                   .referrer(this.downloadItem.url)
                                   .get();

                // Find image
                Elements images = doc.select("#photoImageSection img");
                Element image = images.first();
                String imgsrc = image.attr("src");
                LOGGER.info("Found URL " + imgsrc + " via " + images.get(0));

                // Provide prefix and let the AbstractRipper "guess" the filename
                String prefix = "";
                if (Utils.getConfigBoolean("download.save_order", true)) {
                    prefix = String.format("%03d_", index);
                }

                URL imgurl = new URL(downloadItem.url, imgsrc);
                addURLToDownload(new DownloadItem(imgurl), prefix);

            } catch (IOException e) {
                LOGGER.error("[!] Exception while loading/parsing " + this.downloadItem, e);
            }
        }
    }
}