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

import com.rarchives.ripme.utils.Http;

public class RulePornRipper extends AbstractSingleFileRipper {

    public RulePornRipper(URL url) throws IOException {
        super(url);
    }

    @Override
    public String getHost() {
        return "ruleporn";
    }

    @Override
    public String getDomain() {
        return "ruleporn.com";
    }

    @Override
    public String getGID(URL url) throws MalformedURLException {
        Pattern p = Pattern.compile("^https://.*ruleporn\\.com/(.*)/$");
        Matcher m = p.matcher(url.toExternalForm());
        if (m.matches()) {
            return m.group(1);
        }
        throw new MalformedURLException(
                "Expected ruleporn.com URL format: " + "ruleporn.com/NAME - got " + url + " instead");
    }

    @Override
    public Document getFirstPage() throws IOException {
        return Http.url(url).get();
    }

    @Override
    public List<DownloadItem> getURLsFromPage(Document doc) throws MalformedURLException {
        List<DownloadItem> result = new ArrayList<>();
        String link = doc.select("source[type=video/mp4]").attr("src");
        result.add(new DownloadItem(link));
        return result;
    }

    @Override
    public void downloadURL(DownloadItem downloadItem, int index) {
        addURLToDownload(downloadItem, getPrefix(index));
    }
}