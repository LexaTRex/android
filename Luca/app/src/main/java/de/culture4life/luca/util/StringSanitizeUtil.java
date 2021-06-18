package de.culture4life.luca.util;

import androidx.annotation.NonNull;

public final class StringSanitizeUtil {

    /**
     * @param s String to be sanitized
     * @return String with special characters replaced by a space. Regular expression is taken from
     *         the backend code.
     */
    public static String sanitize(@NonNull String s) {
        return s.replaceAll("[^\\w +.:@£À-ÿāăąćĉċčđēėęěĝğģĥħĩīįİıĵķĸĺļłńņōőœŗřśŝšţŦũūŭůűųŵŷźżžơưếệ-]", " ");
    }

}
