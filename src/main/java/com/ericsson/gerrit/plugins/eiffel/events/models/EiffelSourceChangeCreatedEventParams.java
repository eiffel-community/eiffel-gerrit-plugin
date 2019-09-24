package com.ericsson.gerrit.plugins.eiffel.events.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class EiffelSourceChangeCreatedEventParams {
    @SerializedName("data")
    @Expose
    public EiffelSourceChangeCreatedEventData data;
    
    @SerializedName("links")
    @Expose
    public Links links;
}
