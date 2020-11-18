package com.rarchives.ripme.ripper;

import java.net.URL;

public class DownloadItem {
    public final URL url;
    public final long sourceCreatedTimeSeconds;
    
    public DownloadItem(URL url, long sourceCreatedTimeSeconds) {
        this.url = url;
        this.sourceCreatedTimeSeconds = sourceCreatedTimeSeconds;
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
