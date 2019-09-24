package com.ericsson.gerrit.plugins.eiffel.events.models;

import java.util.ArrayList;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class EiffelSourceChangeSubmittedEventData {
    @SerializedName("submitter")
    @Expose
    public Submitter submitter;
    
    @SerializedName("gitIdentifier")
    @Expose
    public GitIdentifier gitIdentifier;
    
    @SerializedName("customData")
    @Expose
    public ArrayList<KeyValue> customData;
}
