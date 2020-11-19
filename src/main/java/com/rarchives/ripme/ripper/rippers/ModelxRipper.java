package com.rarchives.ripme.ripper.rippers;

import com.rarchives.ripme.ripper.AbstractHTMLRipper;
import com.rarchives.ripme.ripper.DownloadItem;
import com.rarchives.ripme.utils.Http;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ModelxRipper extends AbstractHTMLRipper {

    public ModelxRipper(URL url) throws IOException {
        super(url);
    }

    @Override
    public String getHost() {
        return "modelx";
    }

    @Override
    public String getDomain() {
        return "modelx.org";
    }

    @Override
    public String getGID(URL url) throws MalformedURLException {
        Pattern p = Pattern.compile("^.*modelx.org/.*/(.+)$");
        Matcher m = p.matcher(url.toExternalForm());

        if (m.matches()) {
            return m.group(1);
        }

        throw new MalformedURLException("Expected URL format: http://www.modelx.org/[category (one or more)]/xxxxx got: " + url);
    }

    @Override
    public Document getFirstPage() throws IOException {
        return Http.url(url).get();
    }

    @Override
    public List<DownloadItem> getURLsFromPage(Document page) throws MalformedURLException {
        List<DownloadItem> result = new ArrayList<>();

        for (Element el : page.select(".gallery-icon > a")) {
            result.add(new DownloadItem(el.attr("href")));
        }

        return result;
    }

    @Override
    public void downloadURL(DownloadItem downloadItem, int index) {
        addURLToDownload(downloadItem, getPrefix(index));
    }
}
