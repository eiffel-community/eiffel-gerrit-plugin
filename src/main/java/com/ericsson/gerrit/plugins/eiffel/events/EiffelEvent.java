package com.ericsson.gerrit.plugins.eiffel.events;

import com.google.gson.JsonObject;

public abstract class EiffelEvent {
    protected JsonObject eiffelEvent = new JsonObject();

    /**
     * Sends an eiffel event
     *
     * @throws Exception    If send fails
     */
    public void send() throws Exception{
        //TODO Send eiffel event and throw exception if error occured
    }
}
