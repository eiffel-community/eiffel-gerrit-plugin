package com.ericsson.gerrit.plugins.eiffel.events.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Meta {
    @SerializedName("type")
    @Expose
    public String type;
    
    @SerializedName("version")
    @Expose
    public String version;
    
    @SerializedName("source")
    @Expose
    public Source source;
}
