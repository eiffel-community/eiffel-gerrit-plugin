package com.ericsson.gerrit.plugins.eiffel.events.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Author {
    @SerializedName("name")
    @Expose
    public String name;
    
    @SerializedName("email")
    @Expose
    public String email;
    
    @SerializedName("id")
    @Expose
    public String id;
    
    @SerializedName("group")
    @Expose
    public String group;
}
