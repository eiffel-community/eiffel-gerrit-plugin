package state;

import java.io.File;

import com.ericsson.gerrit.plugins.eiffel.events.EiffelEvent;
import com.ericsson.gerrit.plugins.eiffel.events.EiffelSourceChangeCreatedEvent;
import com.ericsson.gerrit.plugins.eiffel.events.EiffelSourceChangeSubmittedEvent;

public class StateFactory {
    public static StateAccessor getStateAccessor(File pluginDir, EiffelEvent eiffelEvent) {
        try {
            switch (eiffelEvent.msgParams.meta.type) {
            case "EiffelSourceChangeCreatedEvent":
                EiffelSourceChangeCreatedEvent eiffelSourceChangeCreatedEvent = (EiffelSourceChangeCreatedEvent) eiffelEvent;
                String project = eiffelSourceChangeCreatedEvent.eventParams.data.gitIdentifier.repoName;
                String branch = eiffelSourceChangeCreatedEvent.eventParams.data.gitIdentifier.branch;
                String changeId = eiffelSourceChangeCreatedEvent.eventParams.data.change.id;

                return new SourceChangeCreatedStateAccessor(pluginDir, project, branch, changeId);
            case "EiffelSourceChangeSubmittedEvent":
                EiffelSourceChangeSubmittedEvent eiffelSourceChangeSubmittedEvent = (EiffelSourceChangeSubmittedEvent) eiffelEvent;
                String project1 = eiffelSourceChangeSubmittedEvent.eventParams.data.gitIdentifier.repoName;
                String branch1 = eiffelSourceChangeSubmittedEvent.eventParams.data.gitIdentifier.branch;

                return new SourceChangeSubmittedStateAccessor(pluginDir, project1, branch1);
            }
        } catch (Exception e) {
            // TODO Change this
            return null;
        }
        return null;
    }

}
