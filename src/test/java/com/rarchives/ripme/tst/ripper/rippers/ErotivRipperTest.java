package com.rarchives.ripme.tst.ripper.rippers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.net.URL;

import com.rarchives.ripme.ripper.rippers.ErotivRipper;
import org.junit.jupiter.api.Test;

public class ErotivRipperTest extends RippersTest {
    @Test
    public void testGetGID() throws IOException {
        URL url = new URL("https://erotiv.io/e/1568314255");
        ErotivRipper ripper = new ErotivRipper(url);
        assertEquals("1568314255", ripper.getGID(url));
    }

    @Test
    public void testRip() throws IOException {
        URL url = new URL("https://erotiv.io/e/1568314255");
        ErotivRipper ripper = new ErotivRipper(url);
        testRipper(ripper);
    }

    @Test
    public void testGetURLsFromPage() throws IOException {
        URL url = new URL("https://erotiv.io/e/1568314255");
        ErotivRipper ripper = new ErotivRipper(url);
        assertEquals(1, ripper.getURLsFromPage(ripper.getFirstPage()).size());
    }
}
