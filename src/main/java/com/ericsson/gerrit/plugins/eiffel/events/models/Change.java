package com.ericsson.gerrit.plugins.eiffel.events.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Change {
    @SerializedName("insertions")
    @Expose
    public int insertions;

    @SerializedName("deletions")
    @Expose
    public int deletions;

    @SerializedName("tracker")
    @Expose
    public String tracker;

    @SerializedName("details")
    @Expose
    public String details;

    @SerializedName("id")
    @Expose
    public String id;
}