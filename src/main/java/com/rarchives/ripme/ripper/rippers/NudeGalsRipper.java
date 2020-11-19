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

public class NudeGalsRipper extends AbstractHTMLRipper {
    // Current HTML document
    private Document albumDoc = null;

    public NudeGalsRipper(URL url) throws IOException {
        super(url);
    }

    @Override
    public String getHost() {
        return "Nude-Gals";
    }

    @Override
    public String getDomain() {
        return "nude-gals.com";
    }

    @Override
    public String getGID(URL url) throws MalformedURLException {
        Pattern p;
        Matcher m;

        p = Pattern.compile("^.*nude-gals\\.com/photoshoot\\.php\\?photoshoot_id=(\\d+)$");
        m = p.matcher(url.toExternalForm());
        if (m.matches()) {
            return m.group(1);
        }

        throw new MalformedURLException(
                "Expected nude-gals.com gallery format: "
                        + "nude-gals.com/photoshoot.php?phtoshoot_id=####"
                        + " Got: " + url);
    }

    @Override
    public Document getFirstPage() throws IOException {
        if (albumDoc == null) {
            albumDoc = Http.url(url).get();
        }
        return albumDoc;
    }

    @Override
    public List<DownloadItem> getURLsFromPage(Document doc) throws MalformedURLException {
        List<DownloadItem> imageURLs = new ArrayList<>();

        Elements thumbs = doc.select("img.thumbnail");
        for (Element thumb : thumbs) {
            String link = thumb.attr("src").replaceAll("thumbs/th_", "");
            String imgSrc = "http://nude-gals.com/" + link;
            imageURLs.add(new DownloadItem(imgSrc));
        }

        return imageURLs;
    }

    @Override
    public void downloadURL(DownloadItem downloadItem, int index) {
        // Send referrer when downloading images
        addURLToDownload(downloadItem, getPrefix(index), "", this.url.toExternalForm(), null);
    }
}