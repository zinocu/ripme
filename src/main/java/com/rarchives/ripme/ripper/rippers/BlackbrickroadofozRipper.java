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

public class BlackbrickroadofozRipper extends AbstractHTMLRipper {

    public BlackbrickroadofozRipper(URL url) throws IOException {
        super(url);
    }

    @Override
    public String getHost() {
        return "blackbrickroadofoz";
    }

    @Override
    public String getDomain() {
        return "blackbrickroadofoz.com";
    }

    @Override
    public String getGID(URL url) throws MalformedURLException {
        Pattern p = Pattern.compile("https?://www.blackbrickroadofoz.com/comic/([a-zA-Z0-9_-]*)/?$");
        Matcher m = p.matcher(url.toExternalForm());
        if (m.matches()) {
            return m.group(1);
        }
        throw new MalformedURLException("Expected blackbrickroadofoz URL format: " +
                "www.blackbrickroadofoz.com/comic/PAGE - got " + url + " instead");
    }

    @Override
    public Document getFirstPage() throws IOException {
        // "url" is an instance field of the superclass
        return Http.url(url).get();
    }

    @Override
    public Document getNextPage(Document doc) throws IOException {
        sleep(1000);
        Element elem = doc.select("div[id=topnav] > nav.cc-nav > a.cc-next").first();
        if (elem == null) {
            throw new IOException("No more pages");
        }
        String nextPage = elem.attr("href");
        return Http.url(nextPage).get();

    }

    @Override
    public List<DownloadItem> getURLsFromPage(Document doc) throws MalformedURLException {
        List<DownloadItem> result = new ArrayList<>();
        Element elem = doc.select("div[id=cc-comicbody] > a > img[id=cc-comic]").first();
        // The site doesn't return properly encoded urls we replace all spaces ( ) with %20
        String link = elem.attr("src").replaceAll(" ", "%20");
        result.add(new DownloadItem(new URL(link), 0));

        return result;
    }

    @Override
    public void downloadURL(DownloadItem downloadItem, int index) {
        addURLToDownload(downloadItem, getPrefix(index));
    }
}
