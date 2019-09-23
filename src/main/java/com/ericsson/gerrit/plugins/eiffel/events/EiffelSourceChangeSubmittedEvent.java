package com.ericsson.gerrit.plugins.eiffel.events;

import com.google.gerrit.server.data.ChangeAttribute;
import com.google.gerrit.server.data.PatchSetAttribute;
import com.google.gerrit.server.events.ChangeMergedEvent;
import com.google.gson.JsonObject;

public class EiffelSourceChangeSubmittedEvent extends EiffelEvent {
    private ChangeMergedEvent gerritEvent;

    public EiffelSourceChangeSubmittedEvent(ChangeMergedEvent gerritEvent) {
        this.gerritEvent = gerritEvent;

        addGitIdentifier();
    }

    private void addGitIdentifier() {
        String commitId = parseCommitId();

        JsonObject gitIdentifier = new JsonObject();
        gitIdentifier.addProperty("commitId", commitId);

        JsonObject data = new JsonObject();
        data.add("gitIdentifier", gitIdentifier);

        eiffelEvent.add("data", data);
    }

    private String parseCommitId() {
        ChangeAttribute changeAttribute = gerritEvent.change.get();
        String changeId = changeAttribute.id;
        PatchSetAttribute patchSetAttribute = gerritEvent.patchSet.get();
        String patchSetId = patchSetAttribute.revision;
        return changeId;
    }
}
