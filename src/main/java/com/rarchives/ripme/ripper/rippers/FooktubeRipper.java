package com.rarchives.ripme.ripper.rippers;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.rarchives.ripme.ripper.AbstractSingleFileRipper;
import com.rarchives.ripme.ripper.DownloadItem;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.rarchives.ripme.ripper.VideoRipper;
import com.rarchives.ripme.utils.Http;

public class FooktubeRipper extends AbstractSingleFileRipper {

    private static final String HOST = "mulemax";

    public FooktubeRipper(URL url) throws IOException {
        super(url);
    }

    @Override
    public String getHost() {
        return "mulemax";
    }

    @Override
    public String getDomain() {
        return "mulemax.com";
    }

    @Override
    public Document getFirstPage() throws IOException {
        return Http.url(url).get();
    }

    @Override
    public boolean canRip(URL url) {
        Pattern p = Pattern.compile("^https?://.*fooktube\\.com/video/(.*)/.*$");
        Matcher m = p.matcher(url.toExternalForm());
        return m.matches();
    }

    @Override
    public URL sanitizeURL(URL url) throws MalformedURLException {
        return url;
    }

    @Override
    public String getGID(URL url) throws MalformedURLException {
        Pattern p = Pattern.compile("^https?://.*fooktube\\.com/video/(.*)/(.*)$");
        Matcher m = p.matcher(url.toExternalForm());
        if (m.matches()) {
            return m.group(2);
        }

        throw new MalformedURLException(
                "Expected fooktube format:"
                        + "fooktube.com/video/####"
                        + " Got: " + url);
    }

    @Override
    public List<DownloadItem> getURLsFromPage(Document doc) throws MalformedURLException {
        List<DownloadItem> result = new ArrayList<>();
        String link = doc.select(".video-js > source").attr("src");
        result.add(new DownloadItem(link));
        return result;
    }

    @Override
    public void downloadURL(DownloadItem downloadItem, int index) {
        addURLToDownload(downloadItem, getPrefix(index), "", "mulemax.com", null);
    }
}