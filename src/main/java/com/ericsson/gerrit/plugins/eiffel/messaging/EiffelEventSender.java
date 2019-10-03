package com.ericsson.gerrit.plugins.eiffel.messaging;

import com.ericsson.gerrit.plugins.eiffel.configuration.EiffelPluginConfiguration;
import com.ericsson.gerrit.plugins.eiffel.events.EiffelEvent;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class EiffelEventSender {

    private final Gson gson = new Gson();
    private final EiffelPluginConfiguration pluginConfig;

    public EiffelEventSender(EiffelPluginConfiguration pluginConfig) {
        this.pluginConfig = pluginConfig;
    }

    public void send(EiffelEvent eiffelEvent) {
        JsonObject eiffelEventJson = gson.toJsonTree(eiffelEvent).getAsJsonObject();
        System.out.println("Event to send::: " + eiffelEventJson.toString());
        System.out.println("REMReM URL ::: " + pluginConfig.getRemremPublishURL());
        // TODO Implement event sending using REMReM details from pluginconfig.
    }


}
