package com.rarchives.ripme.ripper.rippers;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Connection.Response;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.rarchives.ripme.ripper.AbstractHTMLRipper;
import com.rarchives.ripme.ripper.DownloadItem;
import com.rarchives.ripme.utils.Http;

/**
 *
 * @author randomcommitter
 */
public class ErotivRipper extends AbstractHTMLRipper {

    boolean rippingProfile;

    public ErotivRipper(URL url) throws IOException {
        super(url);
    }

    @Override
    public String getDomain() {
        return "erotiv.io";
    }

    @Override
    public String getHost() {
        return "erotiv";
    }

    @Override
    public String getGID(URL url) throws MalformedURLException {
        Pattern p = Pattern.compile("^https?://(?:www.)?erotiv.io/e/([0-9]*)/?$");
        Matcher m = p.matcher(url.toExternalForm());
        if (m.matches()) {
            return m.group(1);
        }

        throw new MalformedURLException("erotiv video not found in " + url + ", expected https://erotiv.io/e/id");
    }

    @Override
    public Document getFirstPage() throws IOException {
        Response resp = Http.url(this.url).ignoreContentType().response();

        return resp.parse();
    }

    @Override
    public URL sanitizeURL(URL url) throws MalformedURLException {
        return new URL(url.toExternalForm().replaceAll("https?://www.erotiv.io", "https://erotiv.io"));
    }

    @Override
    public List<DownloadItem> getURLsFromPage(Document doc) throws MalformedURLException {
        List<DownloadItem> results = new ArrayList<>();
        Element el = doc.select("video[id=\"player\"] > source").first();
        if (el.hasAttr("src")) {
            results.add(new DownloadItem("https://erotiv.io" + el.attr("src")));
        }
        return results;
    }

    @Override
    public void downloadURL(DownloadItem downloadItem, int index) {
        addURLToDownload(downloadItem, getPrefix(index));
    }

    @Override
    public boolean hasQueueSupport() {
        return true;
    }

}
