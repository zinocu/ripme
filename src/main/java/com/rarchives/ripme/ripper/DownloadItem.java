package com.rarchives.ripme.ripper;

import java.net.MalformedURLException;
import java.net.URL;

public class DownloadItem {
    public final URL url;
    public final long sourceCreatedTimeSeconds;

    public DownloadItem(URL url, long sourceCreatedTimeSeconds) {
        this.url = url;
        this.sourceCreatedTimeSeconds = sourceCreatedTimeSeconds;
    }
    
    public DownloadItem(String url, long sourceCreatedTimeSeconds) throws MalformedURLException {
        this.url = new URL(url);
        this.sourceCreatedTimeSeconds = sourceCreatedTimeSeconds;
    }

    public DownloadItem(String url) throws MalformedURLException {
        this.url = new URL(url);
        this.sourceCreatedTimeSeconds = 0;
    }

    public DownloadItem(URL url) {
        this.url = url;
        this.sourceCreatedTimeSeconds = 0;
    }
    
    @Override
    public String toString() {
        return url.toString() + " - " + sourceCreatedTimeSeconds;
    }
    
    @Override
    public int hashCode() {
        return url.hashCode() ^ (int)sourceCreatedTimeSeconds;
    }
    
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof DownloadItem)) {
            return false;
        }
        
        DownloadItem item = (DownloadItem) o;
        return item.url.equals(this.url) && item.sourceCreatedTimeSeconds == this.sourceCreatedTimeSeconds;
    }
}
