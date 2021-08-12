package de.culture4life.luca.document.provider.baercode;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import de.culture4life.luca.document.DocumentParsingException;

/**
 * Baercode bundle data model
 */
class BaercodeBundle {

    private static final long HOURS_BEFORE_REFRESH = TimeUnit.HOURS.toMillis(12);
    private final CoseMessage coseMessage;
    private final long date;

    private final HashMap<String, BaercodeKey> keysMap = new HashMap<>();

    public BaercodeBundle(@NonNull byte[] data) throws IOException {
        coseMessage = new CoseMessage(data);
        JsonNode bundleItems = coseMessage.getCoseSignMessage();
        byte[] bytes = bundleItems.get(2).binaryValue();
        JsonNode bundleData = CoseMessage.MAPPER.readTree(bytes);
        JsonNode keys = bundleData.get("Keys");
        date = bundleData.get("Date").longValue() * 1000;
        for (Iterator<JsonNode> it = keys.elements(); it.hasNext(); ) {
            JsonNode keyDataItem = it.next();
            BaercodeKey baercodeKey = BaercodeKey.from(keyDataItem);
            this.keysMap.put(baercodeKey.getKeyId(), baercodeKey);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static BaercodeBundle getTestBundle() throws IOException {
        try {
            return loadBundleFrom("src/test/java/de/culture4life/luca/document/provider/baercode/bundle.cose");
        } catch (NoSuchFileException e) {
            return loadBundleFrom("bundle.cose");
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @NonNull
    private static BaercodeBundle loadBundleFrom(String filePath) throws IOException {
        return new BaercodeBundle(Files.readAllBytes(Paths.get(filePath)));
    }

    public BaercodeKey getKey(@NonNull String keyId) {
        return keysMap.get(keyId);
    }

    /**
     * The bundle data is refreshed once every hour. Normally, new keys in the bundle will only
     * affect the following day.
     *
     * @return true if the bundle is already expired and should be updated
     */
    public boolean isExpired() {
        return System.currentTimeMillis() > date + HOURS_BEFORE_REFRESH;
    }

    /**
     * Verify the signature of the bundle
     */
    public void verify(@NonNull PublicKey publicKey) throws DocumentParsingException {
        try {
            if (!coseMessage.verify(publicKey)) {
                throw new DocumentParsingException("Baercode signature is not valid");
            }
        } catch (IOException | GeneralSecurityException e) {
            throw new DocumentParsingException("Exception while parsing Bundle", e);
        }
    }

}
