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

import com.rarchives.ripme.utils.Http;

public class YoupornRipper extends AbstractSingleFileRipper {
    public YoupornRipper(URL url) throws IOException {
        super(url);
    }

    @Override
    public String getHost() {
        return "youporn";
    }

    @Override
    public String getDomain() {
        return "youporn.com";
    }

    @Override
    public boolean canRip(URL url) {
        Pattern p = Pattern.compile("^https?://[wm.]*youporn\\.com/watch/[0-9]+.*$");
        Matcher m = p.matcher(url.toExternalForm());
        return m.matches();
    }

    @Override
    public Document getFirstPage() throws IOException {
        return Http.url(this.url).get();
    }

    @Override
    public List<DownloadItem> getURLsFromPage(Document doc) throws MalformedURLException {
        List<DownloadItem> results = new ArrayList<>();
        Elements videos = doc.select("video");

        Element video = videos.get(0);
        results.add(new DownloadItem(video.attr("src")));
        return results;
    }


    @Override
    public String getGID(URL url) throws MalformedURLException {
        Pattern p = Pattern.compile("^https?://[wm.]*youporn\\.com/watch/([0-9]+).*$");
        Matcher m = p.matcher(url.toExternalForm());
        if (m.matches()) {
            return m.group(1);
        }

        throw new MalformedURLException(
                "Expected youporn format:"
                        + "youporn.com/watch/####"
                        + " Got: " + url);
    }

    @Override
    public void downloadURL(DownloadItem downloadItem, int index) {
        addURLToDownload(downloadItem, getPrefix(index));
    }
}