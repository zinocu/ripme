package com.rarchives.ripme.ripper.rippers;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.rarchives.ripme.ripper.AbstractHTMLRipper;
import com.rarchives.ripme.ripper.DownloadItem;
import com.rarchives.ripme.utils.Http;
import java.util.HashMap;

public class AerisdiesRipper extends AbstractHTMLRipper {
    private Map<String,String> cookies = new HashMap<>();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MM.dd.yyyy");

    public AerisdiesRipper(URL url) throws IOException {
        super(url);
    }

    @Override
    public String getHost() {
        return "aerisdies";
    }
    @Override
    public String getDomain() {
        return "aerisdies.com";
    }

    @Override
    public String getGID(URL url) throws MalformedURLException {
        Pattern p = Pattern.compile("^https?://(?:www\\.)?aerisdies.com/html/lb/[a-z]*_(\\d+)_\\d\\.html");
        Matcher m = p.matcher(url.toExternalForm());
        if (m.matches()) {
            return m.group(1);
        }
        throw new MalformedURLException("Expected URL format: http://www.aerisdies.com/html/lb/albumDIG, got: " + url);

    }

    @Override
    public String getAlbumTitle(URL url) throws MalformedURLException {
        try {
            Element el = getFirstPage().select(".headtext").first();
            if (el == null) {
                throw new IOException("Unable to get album title");
            }
            String title = el.text();
            return getHost() + "_" + getGID(url) + "_" + title.trim();
        } catch (IOException e) {
            // Fall back to default album naming convention
            LOGGER.info("Unable to find title at " + url);
        }
        return super.getAlbumTitle(url);
    }

    @Override
    public Document getFirstPage() throws IOException {
        return Http.url(url).get();
    }

    @Override
    public List<DownloadItem> getURLsFromPage(Document page) throws MalformedURLException {
        List<DownloadItem> imageURLs = new ArrayList<>();
        Elements albumElements = page.select("div.imgbox > a");
            for (Element imagePageLink : albumElements) {
                String imagePageUrl = imagePageLink.attr("href");
                long uploadTime = 0;
                if (shouldGetUploadTime) {
                    uploadTime = getUploadTimeFromURL(new URL(imagePageUrl));
                }
                
                String imageUrl = imagePageLink.select("img").attr("src");
                imageUrl = imageUrl.replaceAll("thumbnails", "images");
                imageUrl = imageUrl.replaceAll("../../", "");
                imageUrl = imageUrl.replaceAll("gif", "jpg");
                imageURLs.add(new DownloadItem(new URL("http://www.aerisdies.com/" + imageUrl), uploadTime));
            }
        return imageURLs;
    }
    
    protected long getUploadTimeFromURL(URL urlWithTime) {
        try {
            Document doc = Http.url(urlWithTime).get();
            Elements elements = doc.getElementsContainingOwnText("Added On");
            if (elements.size() > 0) {
                String dateText = elements.first().nextElementSibling().text();
                Date date = dateFormat.parse(dateText);
                return date.getTime() / 1000;
            }
        } catch (Exception e) {
            LOGGER.error("Could not get upload date for " + urlWithTime);
            e.printStackTrace();
        }
        
        return 0;
    }

    @Override
    public void downloadURL(DownloadItem downloadItem, int index) {
        addURLToDownload(downloadItem, getPrefix(index), "", this.url.toExternalForm(), cookies);
    }

    @Override
    public String getPrefix(int index) {
        return String.format("%03d_", index);
    }
}
