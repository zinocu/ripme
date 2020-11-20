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

import com.rarchives.ripme.ripper.AbstractHTMLRipper;
import com.rarchives.ripme.ripper.DownloadItem;
import com.rarchives.ripme.utils.Http;

public class FemjoyhunterRipper extends AbstractHTMLRipper {

    public FemjoyhunterRipper(URL url) throws IOException {
        super(url);
    }

    @Override
    public String getHost() {
        return "femjoyhunter";
    }

    @Override
    public String getDomain() {
        return "femjoyhunter.com";
    }

    @Override
    public String getGID(URL url) throws MalformedURLException {
        Pattern p = Pattern.compile("https?://www.femjoyhunter.com/([a-zA-Z0-9_-]+)/?");
        Matcher m = p.matcher(url.toExternalForm());
        if (m.matches()) {
            return m.group(1);
        }
        throw new MalformedURLException("Expected femjoyhunter URL format: " +
                "femjoyhunter.com/ID - got " + url + " instead");
    }

    @Override
    public Document getFirstPage() throws IOException {
        // "url" is an instance field of the superclass
        return Http.url(url).get();
    }

    @Override
    public List<DownloadItem> getURLsFromPage(Document doc) throws MalformedURLException {
        List<DownloadItem> result = new ArrayList<>();
        Element container = doc.select("div.list-justified-container").first();
        for (Element el : container.select("a")) {
            result.add(new DownloadItem(el.attr("href")));
        }
        return result;
    }

    @Override
    public void downloadURL(DownloadItem downloadItem, int index) {
        addURLToDownload(downloadItem, getPrefix(index), "", "https://a2h6m3w6.ssl.hwcdn.net/", null);
    }
}