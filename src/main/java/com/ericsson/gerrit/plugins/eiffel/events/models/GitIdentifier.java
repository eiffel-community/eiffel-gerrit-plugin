package com.ericsson.gerrit.plugins.eiffel.events.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class GitIdentifier {
    @SerializedName("commitId")
    @Expose
    public String commitId;
    
    @SerializedName("repoUri")
    @Expose
    public String repoUri;
    
    @SerializedName("branch")
    @Expose
    public String branch;
    
    @SerializedName("repoName")
    @Expose
    public String repoName;
}
