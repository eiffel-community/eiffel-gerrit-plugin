package com.ericsson.gerrit.plugins.eiffel.events.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class KeyValue {
    @SerializedName("key")
    @Expose
    public String key;
    
    @SerializedName("value")
    @Expose
    public String value;
}
