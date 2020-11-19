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
import com.rarchives.ripme.utils.Http;

public class VidbleRipper extends AbstractHTMLRipper {

    public VidbleRipper(URL url) throws IOException {
        super(url);
    }

    @Override
    public String getHost() {
        return "vidble";
    }
    @Override
    public String getDomain() {
        return "vidble.com";
    }

    @Override
    public String getGID(URL url) throws MalformedURLException {
        Pattern p; Matcher m;

        p = Pattern.compile("^.*vidble.com/album/([a-zA-Z0-9_\\-]+).*$");
        m = p.matcher(url.toExternalForm());
        if (m.matches()) {
            return m.group(1);
        }
        throw new MalformedURLException(
                "Expected vidble.com album format: "
                        + "vidble.com/album/####"
                        + " Got: " + url);
    }

    @Override
    public Document getFirstPage() throws IOException {
        return Http.url(url).get();
    }

    @Override
    public List<DownloadItem> getURLsFromPage(Document doc) throws MalformedURLException {
        return getURLsFromPageStatic(doc);
    }

    private static List<DownloadItem> getURLsFromPageStatic(Document doc) throws MalformedURLException {
        List<DownloadItem> imageURLs = new ArrayList<>();
        Elements els = doc.select("#ContentPlaceHolder1_divContent");
        Elements imgs = els.select("img");
        for (Element img : imgs) {
            String src = img.absUrl("src");
            src = src.replaceAll("_[a-zA-Z]{3,5}", "");

            if (!src.equals("")) {
                imageURLs.add(new DownloadItem(src));
            }
        }
        return imageURLs;
   }

    @Override
    public void downloadURL(DownloadItem downloadItem, int index) {
        addURLToDownload(downloadItem, getPrefix(index));
    }

    public static List<DownloadItem> getURLsFromPage(URL url) throws IOException {
        Document doc = Http.url(url).get();
        return getURLsFromPageStatic(doc);
    }
}
