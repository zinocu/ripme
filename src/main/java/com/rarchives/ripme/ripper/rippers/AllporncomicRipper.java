package com.rarchives.ripme.ripper.rippers;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.rarchives.ripme.ripper.AbstractHTMLRipper;
import com.rarchives.ripme.ripper.DownloadItem;
import com.rarchives.ripme.utils.Http;

public class AllporncomicRipper extends AbstractHTMLRipper {
    // This ripper instance is guaranteed to only be ripping a chapter when getURLsFromPage() is called
    // Each chapter only has one upload date, so store it here instead of retrieving multiple times
    private long retrievedUploadTime;
    private boolean hasRetrievedTime;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MM.dd.yyyy");
    
    public AllporncomicRipper(URL url) throws IOException {
        super(url);
    }

    @Override
    public String getHost() {
        return "allporncomic";
    }

    @Override
    public String getDomain() {
        return "allporncomic.com";
    }

    @Override
    public String getGID(URL url) throws MalformedURLException {
        Pattern p = Pattern.compile("https?://allporncomic.com/porncomic/([a-zA-Z0-9_\\-]+)/([a-zA-Z0-9_\\-]+)/?$");
        Matcher m = p.matcher(url.toExternalForm());
        if (m.matches()) {
            return m.group(1) + "_" + m.group(2);
        }
        p = Pattern.compile("^https?://allporncomic.com/porncomic/([a-zA-Z0-9_\\-]+)/?$");
        m = p.matcher(url.toExternalForm());
        if (m.matches()) {
            return m.group(1);
        }
        throw new MalformedURLException("Expected allporncomic URL format: " +
                "allporncomic.com/TITLE/CHAPTER - got " + url + " instead");
    }

    @Override
    public Document getFirstPage() throws IOException {
        // "url" is an instance field of the superclass
        return Http.url(url).get();
    }

    @Override
    public List<DownloadItem> getURLsFromPage(Document doc) throws MalformedURLException {
        List<DownloadItem> result = new ArrayList<>();
        
        long uploadTime = retrievedUploadTime;
        if (shouldGetUploadTime && !hasRetrievedTime) {
            String urlLocation = url.toExternalForm();
            int cutoffIndex = urlLocation.lastIndexOf("chapter-");
            uploadTime = retrievedUploadTime = getUploadTimeFromURL(new URL(urlLocation.substring(0, cutoffIndex)));
            hasRetrievedTime = true;
        }
        
        for (Element el : doc.select(".wp-manga-chapter-img")) {
            result.add(new DownloadItem(new URL(el.attr("data-src")), uploadTime));
        }
        
        return result;
    }
    
    protected long getUploadTimeFromURL(URL urlWithTime) {
        try {
            Document doc = Http.url(urlWithTime).get();
            String dateText = doc.select(".wp-manga-chapter > .chapter-release-date").text();
            Date date = dateFormat.parse(dateText);
            return date.getTime() / 1000;
        } catch (Exception e) {
            LOGGER.error("Could not get upload date for " + urlWithTime);
            e.printStackTrace();
        }
        
        return 0;
    }
    
    @Override
    public boolean hasQueueSupport() {
        return true;
    }

    @Override
    public boolean pageContainsAlbums(URL url) {
        Pattern pa = Pattern.compile("^https?://allporncomic.com/porncomic/([a-zA-Z0-9_\\-]+)/?$");
        Matcher ma = pa.matcher(url.toExternalForm());
        return ma.matches();
    }

    @Override
    public List<String> getAlbumsToQueue(Document doc) {
        List<String> urlsToAddToQueue = new ArrayList<>();
        for (Element elem : doc.select(".wp-manga-chapter > a")) {
            urlsToAddToQueue.add(elem.attr("href"));
        }
        return urlsToAddToQueue;
    }

    @Override
    public void downloadURL(DownloadItem downloadItem, int index) {
        addURLToDownload(downloadItem, getPrefix(index));
    }
}
