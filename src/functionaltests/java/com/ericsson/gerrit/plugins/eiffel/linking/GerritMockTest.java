package com.ericsson.gerrit.plugins.eiffel.linking;

import static org.junit.Assert.assertEquals;
import static org.powermock.api.mockito.PowerMockito.mock;

import java.io.IOException;

import org.junit.Test;

import com.ericsson.gerrit.plugins.eiffel.git.CommitInformation;
import com.google.gerrit.extensions.restapi.ResourceNotFoundException;

public class GerritMockTest {

    @Test
    public void oneUser() {
        final GerritMock gerritMock = new GerritMock();
        final String branch = "branch";
        final String user1 = "user1";
        gerritMock.createBranch(branch);
        final GitCommit base = gerritMock.getHead(branch);

        final String changeId = gerritMock.createNewChange(user1, branch);
        final GitCommit update = gerritMock.getCommit(changeId);
        assertEquals("Not correct parrent", base.sha, update.parentSha);

        final GitCommit newPatchSet = gerritMock.createNewPatchSet(changeId);
        assertEquals("Parent changed when creating new patchset", base.sha, newPatchSet.parentSha);

        final GitCommit merged = gerritMock.submit(changeId);
        assertEquals("The submitted commit does not have correct parent", base.sha,
                merged.parentSha);
        assertEquals("The branch is not updated correctly", merged.sha,
                gerritMock.getHead(branch).sha);
    }

    @Test
    public void oneUserTwoBranches() {
        final GerritMock gerritMock = new GerritMock();
        final String branch1 = "branch1";
        final String branch2 = "branch2";
        final String user1 = "user1";
        gerritMock.createBranch(branch1);
        gerritMock.createBranch(branch2);
        final GitCommit base = gerritMock.getHead(branch1);
        final GitCommit base2 = gerritMock.getHead(branch2);

        final String changeId = gerritMock.createNewChange(user1, branch1);
        assertEquals("Lookup did not return correct changId", changeId,
                gerritMock.getChangeId(user1, branch1));

        final GitCommit update = gerritMock.getCommit(changeId);
        assertEquals("Not correct parrent", base.sha, update.parentSha);

        final String changeId2 = gerritMock.createNewChange(user1, branch2);
        assertEquals("Lookup did not return correct changId", changeId2,
                gerritMock.getChangeId(user1, branch2));

        final GitCommit update2 = gerritMock.getCommit(changeId2);
        assertEquals("Not correct parrent", base2.sha, update2.parentSha);

        final GitCommit newPatchSet = gerritMock.createNewPatchSet(changeId);
        assertEquals("Parent changed when creating new patchset", base.sha, newPatchSet.parentSha);

        final GitCommit newPatchSet2 = gerritMock.createNewPatchSet(changeId2);
        assertEquals("Parent changed when creating new patchset", base2.sha,
                newPatchSet2.parentSha);

        final GitCommit merged = gerritMock.submit(changeId);
        assertEquals("The submitted commit does not have correct parent", base.sha,
                merged.parentSha);
        assertEquals("The branch is not updated correctly", merged.sha,
                gerritMock.getHead(branch1).sha);

        final GitCommit merged2 = gerritMock.submit(changeId2);
        assertEquals("The submitted commit does not have correct parent", base2.sha,
                merged2.parentSha);
        assertEquals("The branch is not updated correctly", merged2.sha,
                gerritMock.getHead(branch2).sha);
    }

    @Test
    public void twoUsers() {
        final GerritMock gerritMock = new GerritMock();
        final String branch = "branch1";
        final String user1 = "user1";
        final String user2 = "user2";
        gerritMock.createBranch(branch);
        final GitCommit base = gerritMock.getHead(branch);

        final String changeId = gerritMock.createNewChange(user1, branch);
        final GitCommit update = gerritMock.getCommit(changeId);
        assertEquals("Not correct parrent", base.sha, update.parentSha);

        final String changeId2 = gerritMock.createNewChange(user2, branch);
        assertEquals("Lookup did not return correct changId", changeId2,
                gerritMock.getChangeId(user2, branch));

        final GitCommit update2 = gerritMock.getCommit(changeId2);
        assertEquals("Not correct parrent", base.sha, update2.parentSha);

        final GitCommit merged2 = gerritMock.submit(changeId2);
        assertEquals("The submitted commit does not have correct parent", base.sha,
                merged2.parentSha);
        assertEquals("The branch is not updated correctly", merged2.sha,
                gerritMock.getHead(branch).sha);

        final GitCommit newPatchSet = gerritMock.createNewPatchSet(changeId);
        assertEquals("Parent changed when creating new patchset", base.sha, newPatchSet.parentSha);

        final GitCommit rebase = gerritMock.rebase(changeId);
        assertEquals("Rebase did not return the correct commit", rebase.parentSha, merged2.sha);

        final GitCommit merged = gerritMock.submit(changeId);
        assertEquals("The submitted commit does not have correct parent", merged2.sha,
                merged.parentSha);
        assertEquals("The branch is not updated correctly", merged.sha,
                gerritMock.getHead(branch).sha);
    }

    /**
     * Test that you cannot create a second change with a user and branch without submit first
     */
    @Test(expected = AssertionError.class)
    public void createWithoutSubmitFail() {
        final GerritMock gerritMock = new GerritMock();

        final String branch = "branch";
        gerritMock.createBranch(branch);

        gerritMock.createNewChange("user", branch);
        gerritMock.createNewChange("user", branch);
    }

    /**
     * Test that submitting before rebaseing causes an error
     */
    @Test(expected = AssertionError.class)
    public void submitBeforeRebaseFail() {
        final GerritMock gerritMock = new GerritMock();

        final String branch = "branch";
        gerritMock.createBranch(branch);

        final String changeUser1 = gerritMock.createNewChange("user1", branch);
        final String changeUser2 = gerritMock.createNewChange("user2", branch);
        gerritMock.submit(changeUser2);
        gerritMock.submit(changeUser1);
    }

    @Test
    public void createTwoChages() {
        final GerritMock gerritMock = new GerritMock();

        final String branch = "branch";
        gerritMock.createBranch(branch);

        final String user = "user";
        final String change1 = gerritMock.createNewChange(user, branch);
        assertEquals("Lookup did not return correct changId", change1,
                gerritMock.getChangeId(user, branch));
        gerritMock.submit(change1);
        final String change2 = gerritMock.createNewChange(user, branch);
        assertEquals("Lookup did not return correct changId", change2,
                gerritMock.getChangeId(user, branch));

    }

    @Test
    public void setCollectionsExpectation() throws ResourceNotFoundException, IOException {

        final GerritMock gerritMock = new GerritMock();
        final String branch = "branch";
        gerritMock.createBranch(branch);
        final String changeId = gerritMock.createNewChange("user", branch);
        final GitCommit commit = gerritMock.getCommit(changeId);

        final CommitInformation commitInformation = mock(CommitInformation.class);

        gerritMock.setExpectionsFor(commitInformation, changeId, "some-project");

        final String actualParent = commitInformation.getParentsSHAs(commit.sha, "some-project").get(0);
        final String expectedParent = commit.parentSha;
        assertEquals("The expectation did not return the correct parent", expectedParent,
                actualParent);
    }
}
