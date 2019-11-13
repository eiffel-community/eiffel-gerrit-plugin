package com.ericsson.gerrit.plugins.eiffel.linking;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GerritMock {

    private Map<String, GitCommit> branches;
    private Map<String, String> changeIds;
    private Map<String, GitCommit> commits;
    private Map<String, ChangeInfo> changeInfos;

    public GerritMock() {
        branches = new HashMap<String, GitCommit>();
        changeIds = new HashMap<String, String>();
        commits = new HashMap<String, GitCommit>();
        changeInfos = new HashMap<String, ChangeInfo>();
    }

    public void createBranch(String branch) {
        branches.put(branch, getNewCommit());
    }

    public GitCommit getHead(String branch) {
        return branches.get(branch);
    }

    public GitCommit getCommit(String changeId) {
        return commits.get(changeId);
    }

    public String createNewChange(String user, String branch) {
        String lookupKey = getLookupKey(user, branch);
        String changeId = "change-id-" + changeIds.size();

        assert !changeIds.containsKey(lookupKey);
        changeIds.put(lookupKey, changeId);
        changeInfos.put(changeId, new ChangeInfo(user, branch));

        GitCommit branchHead = getHead(branch);
        GitCommit changeCommit = getNewCommit(branchHead);
        commits.put(changeId, changeCommit);
        return changeId;
    }

    public GitCommit createNewPatchSet(String changeId) {
        GitCommit oldCommit = commits.get(changeId);
        GitCommit newCommit = getNewCommit();

        // As we have not rebased the change the startig point has not changed.
        newCommit.parentSha = oldCommit.parentSha;
        commits.put(changeId, newCommit);
        return newCommit;
    }

    public String getChangeId(String user, String branch) {
        String lookupKey = getLookupKey(user, branch);
        return changeIds.get(lookupKey);
    }

    public GitCommit rebase(String changeId) {
        String branch = changeInfos.get(changeId).branch;
        GitCommit head = getHead(branch);
        GitCommit rebased = getNewCommit(head);
        commits.put(changeId, rebased);
        return rebased;

    }

    public GitCommit submit(String changeId) {
        GitCommit submittedCommit = getCommit(changeId);
        String branch = changeInfos.get(changeId).branch;

        // Ensure its rebased
        assert submittedCommit.parentSha.equals(getHead(branch).sha);

        branches.put(branch, submittedCommit);
        return submittedCommit;
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


    private String getLookupKey(String user, String branch) {
        return user + branch;
    }

    class ChangeInfo {
        String branch;
        String user;

        public ChangeInfo(String user, String branch) {
            this.user = user;
            this.branch = branch;
        }
    }

}
