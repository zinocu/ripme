package com.rarchives.ripme.ripper.rippers;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.rarchives.ripme.ripper.AbstractHTMLRipper;
import com.rarchives.ripme.ripper.DownloadItem;
import com.rarchives.ripme.utils.Http;

public class ManganeloRipper extends AbstractHTMLRipper {

    public ManganeloRipper(URL url) throws IOException {
        super(url);
    }

    @Override
    public String getHost() {
        return "manganelo";
    }

    @Override
    public String getDomain() {
        return "manganelo.com";
    }

    @Override
    public String getGID(URL url) throws MalformedURLException {
        Pattern p = Pattern.compile("https?://manganelo.com/manga/([\\S]+)/?$");
        Matcher m = p.matcher(url.toExternalForm());
        if (m.matches()) {
            return m.group(1);
        }

        p = Pattern.compile("https?://manganelo.com/chapter/([\\S]+)/([\\S_\\-]+)/?$");
        m = p.matcher(url.toExternalForm());
        if (m.matches()) {
            return m.group(1);
        }
        throw new MalformedURLException("Expected manganelo URL format: " +
                "/manganelo.com/manga/ID - got " + url + " instead");
    }

    @Override
    public Document getFirstPage() throws IOException {
        // "url" is an instance field of the superclass
        return Http.url(url).get();
    }

    @Override
    public Document getNextPage(Document doc) throws IOException {
        Element elem = doc.select("div.btn-navigation-chap > a.back").first();
        if (elem == null) {
            throw new IOException("No more pages");
        } else {
            return Http.url(elem.attr("href")).get();
        }
    }

    private List<DownloadItem> getURLsFromChap(String url) {
        List<DownloadItem> result = new ArrayList<>();
        try {
            Document doc = Http.url(url).get();

            return getURLsFromChap(doc);
        } catch (IOException e) {
            return null;
        }

    }

    private List<DownloadItem> getURLsFromChap(Document doc) throws MalformedURLException {
        LOGGER.debug("Getting urls from " + doc.location());
        List<DownloadItem> result = new ArrayList<>();
        for (Element el : doc.select(".vung-doc > img")) {
            result.add(new DownloadItem(el.attr("src")));
        }
        return result;
    }

    @Override
    public List<DownloadItem> getURLsFromPage(Document doc) throws MalformedURLException {
        List<DownloadItem> result = new ArrayList<>();
        List<String> urlsToGrab = new ArrayList<>();
        if (url.toExternalForm().contains("/manga/")) {
            for (Element el : doc.select("div.chapter-list > div.row > span > a")) {
                urlsToGrab.add(el.attr("href"));
            }
            Collections.reverse(urlsToGrab);

            for (String url : urlsToGrab) {
                result.addAll(getURLsFromChap(url));
            }
        } else if (url.toExternalForm().contains("/chapter/")) {
            result.addAll(getURLsFromChap(doc));
        }
        return result;
    }

    @Override
    public void downloadURL(DownloadItem downloadItem, int index) {
        addURLToDownload(downloadItem, getPrefix(index));
    }
}
