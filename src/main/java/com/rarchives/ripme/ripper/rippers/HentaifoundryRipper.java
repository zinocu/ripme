package com.rarchives.ripme.ripper.rippers;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.rarchives.ripme.utils.Utils;
import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.rarchives.ripme.ripper.AbstractHTMLRipper;
import com.rarchives.ripme.ripper.DownloadItem;
import com.rarchives.ripme.utils.Http;

public class HentaifoundryRipper extends AbstractHTMLRipper {

    private Map<String,String> cookies = new HashMap<>();
    public HentaifoundryRipper(URL url) throws IOException {
        super(url);
    }

    @Override
    public String getHost() {
        return "hentai-foundry";
    }
    @Override
    public String getDomain() {
        return "hentai-foundry.com";
    }

    @Override
    public String getGID(URL url) throws MalformedURLException {
        Pattern p = Pattern.compile("^.*hentai-foundry\\.com/(pictures|stories)/user/([a-zA-Z0-9\\-_]+).*$");
        Matcher m = p.matcher(url.toExternalForm());
        if (m.matches()) {
            return m.group(2);
        }
        throw new MalformedURLException(
                "Expected hentai-foundry.com gallery format: "
                        + "hentai-foundry.com/pictures/user/USERNAME"
                        + " Got: " + url);
    }

    @Override
    public Document getFirstPage() throws IOException {
        Response resp;
        Document doc;

        resp = Http.url("https://www.hentai-foundry.com/?enterAgree=1&size=1500")
                .referrer("https://www.hentai-foundry.com/")
                .cookies(cookies)
                .response();
        // The only cookie that seems to matter in getting around the age wall is the phpsession cookie
        cookies.putAll(resp.cookies());

        doc = resp.parse();
        String csrf_token = doc.select("input[name=YII_CSRF_TOKEN]")
                               .first().attr("value");
        if (csrf_token != null) {
            Map<String,String> data = new HashMap<>();
            data.put("YII_CSRF_TOKEN"  , csrf_token);
            data.put("rating_nudity"   , "3");
            data.put("rating_violence" , "3");
            data.put("rating_profanity", "3");
            data.put("rating_racism"   , "3");
            data.put("rating_sex"      , "3");
            data.put("rating_spoilers" , "3");
            data.put("rating_yaoi"     , "1");
            data.put("rating_yuri"     , "1");
            data.put("rating_teen"     , "1");
            data.put("rating_guro"     , "1");
            data.put("rating_furry"    , "1");
            data.put("rating_beast"    , "1");
            data.put("rating_male"     , "1");
            data.put("rating_female"   , "1");
            data.put("rating_futa"     , "1");
            data.put("rating_other"    , "1");
            data.put("rating_scat"     , "1");
            data.put("rating_incest"   , "1");
            data.put("rating_rape"     , "1");
            data.put("filter_media"    , "A");
            data.put("filter_order"    , Utils.getConfigString("hentai-foundry.filter_order","date_old"));
            data.put("filter_type"     , "0");

            resp = Http.url("https://www.hentai-foundry.com/site/filters")
                       .referrer("https://www.hentai-foundry.com/")
                       .cookies(cookies)
                       .data(data)
                       .method(Method.POST)
                       .response();
            cookies.putAll(resp.cookies());
        }
        else {
            LOGGER.info("unable to find csrf_token and set filter");
        }

        resp = Http.url(url)
                .referrer("https://www.hentai-foundry.com/")
                .cookies(cookies)
                .response();
        cookies.putAll(resp.cookies());
        return resp.parse();
    }

    @Override
    public Document getNextPage(Document doc) throws IOException {
        if (!doc.select("li.next.hidden").isEmpty()) {
            // Last page
            throw new IOException("No more pages");
        }
        Elements els = doc.select("li.next > a");
        Element first = els.first();
        try {
            String nextURL = first.attr("href");
            nextURL = "https://www.hentai-foundry.com" + nextURL;
            return Http.url(nextURL)
                    .referrer(url)
                    .cookies(cookies)
                    .get();
        } catch (NullPointerException e) {
            throw new IOException("No more pages");
        }
    }

    @Override
    public List<DownloadItem> getURLsFromPage(Document doc) throws MalformedURLException {
        List<DownloadItem> imageURLs = new ArrayList<>();
        // this if is for ripping pdf stories
        if (url.toExternalForm().contains("/stories/")) {
            for (Element pdflink : doc.select("a.pdfLink")) {
                LOGGER.info("grabbing " + "https://www.hentai-foundry.com" + pdflink.attr("href"));
                String link = "https://www.hentai-foundry.com" + pdflink.attr("href");
                imageURLs.add(new DownloadItem(link));
            }
            return imageURLs;
        }
        Pattern imgRegex = Pattern.compile(".*/user/([a-zA-Z0-9\\-_]+)/(\\d+)/.*");
        for (Element thumb : doc.select("div.thumb_square > a.thumbLink")) {
            if (isStopped()) {
                break;
            }
            Matcher imgMatcher = imgRegex.matcher(thumb.attr("href"));
            if (!imgMatcher.matches()) {
                LOGGER.info("Couldn't find user & image ID in " + thumb.attr("href"));
                continue;
            }
            Document imagePage;
            try {

                LOGGER.info("grabbing " + "https://www.hentai-foundry.com" + thumb.attr("href"));
                imagePage = Http.url("https://www.hentai-foundry.com" + thumb.attr("href")).cookies(cookies).get();
            }

            catch (IOException e) {
                LOGGER.debug(e.getMessage());
                LOGGER.debug("Warning: imagePage is null!");
                imagePage = null;
            }
            // This is here for when the image is resized to a thumbnail because ripme doesn't report a screensize
            if (imagePage.select("div.boxbody > img.center").attr("src").contains("thumbs.")) {
                String link = "https:" + imagePage.select("div.boxbody > img.center").attr("onclick").replace("this.src=", "").replace("'", "").replace("; $(#resize_message).hide();", "");
                imageURLs.add(new DownloadItem(link));
            }
            else {
                String link = "https:" + imagePage.select("div.boxbody > img.center").attr("src");
                imageURLs.add(new DownloadItem(link));
            }
        }
        return imageURLs;
    }

    @Override
    public void downloadURL(DownloadItem downloadItem, int index) {
        // When downloading pdfs you *NEED* to end the cookies with the request or you just get the consent page
        if (url.toExternalForm().endsWith(".pdf")) {
            addURLToDownload(downloadItem, getPrefix(index), "", this.url.toExternalForm(), cookies);
        } else {
//            If hentai-foundry.use_prefix is false the ripper will not add a numbered prefix to any images
            if (Utils.getConfigBoolean("hentai-foundry.use_prefix", true)) {
                addURLToDownload(downloadItem, getPrefix(index));
            } else {
                addURLToDownload(downloadItem, "");
            }
        }
    }

}
