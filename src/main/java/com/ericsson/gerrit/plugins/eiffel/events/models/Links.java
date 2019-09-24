package com.ericsson.gerrit.plugins.eiffel.events.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Links {
    @SerializedName("type")
    @Expose
    public String type;

    @SerializedName("target")
    @Expose
    public String target;
}
