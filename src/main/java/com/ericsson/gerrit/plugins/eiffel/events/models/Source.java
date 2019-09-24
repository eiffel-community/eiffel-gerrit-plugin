package com.ericsson.gerrit.plugins.eiffel.events.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Source {
    @SerializedName("domainId")
    @Expose
    public String domainId;
    
    @SerializedName("host")
    @Expose
    public String host;
    
    @SerializedName("name")
    @Expose
    public String name;
    
    @SerializedName("uri")
    @Expose
    public String uri;
}
