package com.ericsson.gerrit.plugins.eiffel.events;

import com.ericsson.gerrit.plugins.eiffel.events.models.EiffelSourceChangeCreatedEventParams;
import com.ericsson.gerrit.plugins.eiffel.events.models.EiffelSourceChangeSubmittedEventEventParams;
import com.ericsson.gerrit.plugins.eiffel.events.models.MsgParams;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * This Eiffel Event model represents the EiffelSourceChangeCreatedEvent and is populated with
 * information from the patchset-created gerrit event.
 *
 */
public class EiffelSourceChangeCreatedEvent {
    @SerializedName("msgParams")
    @Expose
    public MsgParams msgParams;
    
    @SerializedName("eventParams")
    @Expose
    public EiffelSourceChangeCreatedEventParams eventParams;
}
