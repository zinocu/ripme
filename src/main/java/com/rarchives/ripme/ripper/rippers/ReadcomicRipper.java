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

public class ReadcomicRipper extends  ViewcomicRipper {

    public ReadcomicRipper(URL url) throws IOException {
        super(url);
    }

    @Override
    public String getHost() {
        return "read-comic";
    }

    @Override
    public String getDomain() {
        return "read-comic.com";
    }



    @Override
    public String getGID(URL url) throws MalformedURLException {
        Pattern p = Pattern.compile("https?://read-comic.com/([a-zA-Z1-9_-]*)/?$");
        Matcher m = p.matcher(url.toExternalForm());
        if (m.matches()) {
            return m.group(1);
        }
        throw new MalformedURLException("Expected view-comic URL format: " +
                "read-comic.com/COMIC_NAME - got " + url + " instead");
    }

    @Override
    public List<DownloadItem> getURLsFromPage(Document doc) throws MalformedURLException {
        List<DownloadItem> result = new ArrayList<>();
        for (Element el : doc.select("div.pinbin-copy > a > img")) {
            result.add(new DownloadItem(el.attr("src")));
        }
        return result;
    }

}
