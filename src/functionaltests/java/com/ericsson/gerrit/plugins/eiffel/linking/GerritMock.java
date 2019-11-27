package com.ericsson.gerrit.plugins.eiffel.linking;

import static org.powermock.api.mockito.PowerMockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.ericsson.gerrit.plugins.eiffel.git.CommitInformation;
import com.google.gerrit.extensions.restapi.ResourceNotFoundException;

/**
 * This class will very simply simulate Gerrit when creating new changes and submitting them.
 */
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

    /**
     * Initializes a branch with an commit
     *
     * @param branch a branch name to use during test
     */
    public void createBranch(String branch) {
        branches.put(branch, getNewCommit());
    }

    /**
     * Get the "HEAD" commit from a branch
     *
     * @param branch name of branch to get commit for
     * @return the commit the branch points to
     */
    public GitCommit getHead(String branch) {
        assert branches.containsKey(branch);
        return branches.get(branch);
    }

    /**
     * Fetches the current commit for a changeId
     *
     * @param changeId the change id to fetch the commit for
     * @return the current commit of the changeId
     */
    public GitCommit getCommit(String changeId) {
        return commits.get(changeId);
    }

    /**
     * Simulates creating a new branch in Gerrit. Will generate a changeId and create a new commit
     * based on HEAD for the given branch. The commit will have the HEAD commit as parent.
     *
     * As the function requires both <code>user</code> and <code>branch</code> many scenarios can be
     * tested:
     * <ul>
     * <li>Many users on the same branch</li>
     * <li>One user working on many branches</li>
     * </ul>
     *
     * @param user   Name of the user to create the change for
     * @param branch Name of branch this will
     * @return a changeId
     */
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

    /**
     * Simulates creating a new patchset on the given change. The method will create a new commit
     * with the same parent as before.
     *
     * @param changeId The changeId for the change to be updated
     * @return the new commit
     */
    public GitCommit createNewPatchSet(String changeId) {
        GitCommit oldCommit = commits.get(changeId);
        GitCommit newCommit = getNewCommit();

        // As we have not rebased the change the startig point has not changed.
        newCommit.parentSha = oldCommit.parentSha;
        commits.put(changeId, newCommit);
        return newCommit;
    }

    /**
     * Returns the changeId for the given user and branch
     *
     * @param user   Name of user
     * @param branch Name of branch
     * @return the changeId
     */
    public String getChangeId(String user, String branch) {
        String lookupKey = getLookupKey(user, branch);
        return changeIdsLookup.get(lookupKey);
    }

    /**
     * Simulates rebasing a change in Gerrit. It will create a new commit with branch HEAD as parent
     * and set that commit as current commit for this change.
     *
     * @param changeId the changeId of the change
     * @return the new commit
     */
    public GitCommit rebase(String changeId) {
        String branch = changeInfos.get(changeId).branch;
        GitCommit head = getHead(branch);
        GitCommit rebased = getNewCommit(head);
        commits.put(changeId, rebased);
        return rebased;

    }

    /**
     * Simulates submitting a change in Gerrit. Will update branch head to the change commit and
     * then remove the connection of user and branch to the change. The test implementation only
     * supports one change per user and branch.
     *
     * @param changeId
     * @return
     */
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

    /**
     * Will set an expectation on the given CommitInformation to check for the correct commit and
     * project. The expectation will then return the parent SHA for the asked commit.
     *
     * @param commitInformation The CommitInformation to set expectation on
     * @param changeId          the current change containing commit SHA and parent SHA used in the
     *                          expectation
     * @param projectName       project name used in the expectation
     * @throws ResourceNotFoundException
     * @throws IOException
     */
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

    private class ChangeInfo {
        public String branch;
        public String user;
    }

}
