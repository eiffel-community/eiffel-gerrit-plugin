package com.ericsson.gerrit.plugins.eiffel.events.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class EiffelSourceChangeSubmittedEventEventParams {
    @SerializedName("data")
    @Expose
    public EiffelSourceChangeSubmittedEventData data;
    
    @SerializedName("links")
    @Expose
    public Links links;
}
