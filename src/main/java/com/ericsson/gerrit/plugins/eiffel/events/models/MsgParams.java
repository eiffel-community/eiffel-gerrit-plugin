package com.ericsson.gerrit.plugins.eiffel.events.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class MsgParams {
    @SerializedName("meta")
    @Expose
    public Meta meta;
}
