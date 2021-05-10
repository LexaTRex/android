package de.culture4life.luca.crypto;

import java.util.ArrayList;
import java.util.Collection;

import androidx.annotation.NonNull;

/**
 * Collection of {@link TraceIdWrapper}.
 */
public class TraceIdWrapperList extends ArrayList<TraceIdWrapper> {

    public TraceIdWrapperList() {
    }

    public TraceIdWrapperList(@NonNull Collection<? extends TraceIdWrapper> traceIdWrappers) {
        super(traceIdWrappers);
    }

}
