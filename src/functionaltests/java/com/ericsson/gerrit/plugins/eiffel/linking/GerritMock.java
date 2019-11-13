package com.ericsson.gerrit.plugins.eiffel.linking;

import java.util.UUID;

public class GerritMock {

    private GitCommit head;

    public GerritMock() {
        head = getNewCommit();
    }

    public GitCommit newCommit() {
        return getNewCommit(head);
    }

    public GitCommit getCommit() {
        return head;
    }

    public GitCommit merge(GitCommit update) {
        assert update.parentSha.equals(head.sha);
        head = update;
        return update;
    }

    public GitCommit rebase(GitCommit update) {
        return getNewCommit(head);

    }

    private GitCommit getNewCommit() {
        GitCommit commit = new GitCommit();
        commit.sha = UUID.randomUUID().toString();
        return commit;
    }

    private GitCommit getNewCommit(GitCommit parentCommit) {
        GitCommit commit = getNewCommit();
        commit.parentSha = parentCommit.sha;
        return commit;
    }

}
