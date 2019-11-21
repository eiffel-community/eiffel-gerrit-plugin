package com.ericsson.gerrit.plugins.eiffel.linking;

import static org.powermock.api.mockito.PowerMockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.ericsson.gerrit.plugins.eiffel.git.CommitInformation;
import com.google.gerrit.extensions.restapi.ResourceNotFoundException;

public class GerritMock {

    private Map<String, GitCommit> branches;
    private Map<String, String> changeIdsLookup;
    private Map<String, GitCommit> commits;
    private Map<String, ChangeInfo> changeInfos;

    public GerritMock() {
        branches = new HashMap<String, GitCommit>();
        changeIdsLookup = new HashMap<String, String>();
        commits = new HashMap<String, GitCommit>();
        changeInfos = new HashMap<String, ChangeInfo>();
    }

    public void createBranch(String branch) {
        branches.put(branch, getNewCommit());
    }

    public GitCommit getHead(String branch) {
        assert branches.containsKey(branch);
        return branches.get(branch);
    }

    public GitCommit getCommit(String changeId) {
        return commits.get(changeId);
    }

    public String createNewChange(String user, String branch) {
        String lookupKey = getLookupKey(user, branch);
        String changeId = "change-id-" + changeIdsLookup.size();

        assert !changeIdsLookup.containsKey(lookupKey) : lookupKey
                + " already exists. Forgot to submit it?";
        changeIdsLookup.put(lookupKey, changeId);
        ChangeInfo changeInfo = new ChangeInfo();
        changeInfo.user = user;
        changeInfo.branch = branch;
        changeInfos.put(changeId, changeInfo);

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
        return changeIdsLookup.get(lookupKey);
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
        assert submittedCommit.parentSha.equals(getHead(branch).sha) : changeId
                + " is not ready for submit. Reabase it first!";

        branches.put(branch, submittedCommit);

        ChangeInfo changeInfo = changeInfos.get(changeId);
        String lookupKey = getLookupKey(changeInfo.user, changeInfo.branch);
        changeIdsLookup.remove(lookupKey);
        return submittedCommit;
    }

    public void setExpectionsFor(CommitInformation commitInformation, String changeId,
            String projectName)
            throws ResourceNotFoundException, IOException {
        GitCommit commit = getCommit(changeId);
        when(commitInformation.getParentsSHAs(commit.sha, projectName)).thenReturn(
                Arrays.asList(commit.parentSha));
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
        public String branch;
        public String user;
    }

}
