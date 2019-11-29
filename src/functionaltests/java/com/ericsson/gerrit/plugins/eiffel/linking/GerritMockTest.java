package com.ericsson.gerrit.plugins.eiffel.linking;

import static org.junit.Assert.assertEquals;
import static org.powermock.api.mockito.PowerMockito.mock;

import java.io.IOException;

import org.junit.Test;

import com.ericsson.gerrit.plugins.eiffel.git.CommitInformation;
import com.google.gerrit.extensions.restapi.ResourceNotFoundException;

public class GerritMockTest {

    private static final String USER_1 = "user1";
    private static final String USER_2 = "user2";
    private static final String BRANCH_1 = "branch1";
    private static final String BRANCH_2 = "branch2";

    @Test
    public void oneUser() {
        final GerritMock gerritMock = new GerritMock();
        gerritMock.createBranch(BRANCH_1);
        final GitCommit base = gerritMock.getHead(BRANCH_1);

        final String changeId = gerritMock.createNewChange(USER_1, BRANCH_1);
        final GitCommit update = gerritMock.getCommit(changeId);
        assertEquals("Not correct parrent", base.sha, update.parentSha);

        final GitCommit newPatchSet = gerritMock.createNewPatchSet(changeId);
        assertEquals("Parent changed when creating new patchset", base.sha, newPatchSet.parentSha);

        final GitCommit merged = gerritMock.submit(changeId);
        assertEquals("The submitted commit does not have correct parent", base.sha,
                merged.parentSha);
        assertEquals("The branch is not updated correctly", merged.sha,
                gerritMock.getHead(BRANCH_1).sha);
    }

    @Test
    public void oneUserTwoBranches() {
        final GerritMock gerritMock = new GerritMock();
        gerritMock.createBranch(BRANCH_1);
        gerritMock.createBranch(BRANCH_2);
        final GitCommit base = gerritMock.getHead(BRANCH_1);
        final GitCommit base2 = gerritMock.getHead(BRANCH_2);

        final String changeId = gerritMock.createNewChange(USER_1, BRANCH_1);
        assertEquals("Lookup did not return correct changId", changeId,
                gerritMock.getChangeId(USER_1, BRANCH_1));

        final GitCommit update = gerritMock.getCommit(changeId);
        assertEquals("Not correct parrent", base.sha, update.parentSha);

        final String changeId2 = gerritMock.createNewChange(USER_1, BRANCH_2);
        assertEquals("Lookup did not return correct changId", changeId2,
                gerritMock.getChangeId(USER_1, BRANCH_2));

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
                gerritMock.getHead(BRANCH_1).sha);

        final GitCommit merged2 = gerritMock.submit(changeId2);
        assertEquals("The submitted commit does not have correct parent", base2.sha,
                merged2.parentSha);
        assertEquals("The branch is not updated correctly", merged2.sha,
                gerritMock.getHead(BRANCH_2).sha);
    }

    @Test
    public void twoUsers() {
        final GerritMock gerritMock = new GerritMock();
        gerritMock.createBranch(BRANCH_1);
        final GitCommit base = gerritMock.getHead(BRANCH_1);

        final String changeId = gerritMock.createNewChange(USER_1, BRANCH_1);
        final GitCommit update = gerritMock.getCommit(changeId);
        assertEquals("Not correct parrent", base.sha, update.parentSha);

        final String changeId2 = gerritMock.createNewChange(USER_2, BRANCH_1);
        assertEquals("Lookup did not return correct changId", changeId2,
                gerritMock.getChangeId(USER_2, BRANCH_1));

        final GitCommit update2 = gerritMock.getCommit(changeId2);
        assertEquals("Not correct parrent", base.sha, update2.parentSha);

        final GitCommit merged2 = gerritMock.submit(changeId2);
        assertEquals("The submitted commit does not have correct parent", base.sha,
                merged2.parentSha);
        assertEquals("The branch is not updated correctly", merged2.sha,
                gerritMock.getHead(BRANCH_1).sha);

        final GitCommit newPatchSet = gerritMock.createNewPatchSet(changeId);
        assertEquals("Parent changed when creating new patchset", base.sha, newPatchSet.parentSha);

        final GitCommit rebase = gerritMock.rebase(changeId);
        assertEquals("Rebase did not return the correct commit", rebase.parentSha, merged2.sha);

        final GitCommit merged = gerritMock.submit(changeId);
        assertEquals("The submitted commit does not have correct parent", merged2.sha,
                merged.parentSha);
        assertEquals("The branch is not updated correctly", merged.sha,
                gerritMock.getHead(BRANCH_1).sha);
    }

    /**
     * Test that you cannot create a second change with a user and branch without submit first
     */
    @Test(expected = AssertionError.class)
    public void createWithoutSubmitFail() {
        final GerritMock gerritMock = new GerritMock();

        gerritMock.createBranch(BRANCH_1);

        gerritMock.createNewChange(USER_1, BRANCH_1);
        gerritMock.createNewChange(USER_1, BRANCH_1);
    }

    /**
     * Test that submitting before rebaseing causes an error
     */
    @Test(expected = AssertionError.class)
    public void submitBeforeRebaseFail() {
        final GerritMock gerritMock = new GerritMock();

        gerritMock.createBranch(BRANCH_1);

        final String changeUser1 = gerritMock.createNewChange(USER_1, BRANCH_1);
        final String changeUser2 = gerritMock.createNewChange(USER_2, BRANCH_1);
        gerritMock.submit(changeUser2);
        gerritMock.submit(changeUser1);
    }

    @Test
    public void createTwoChages() {
        final GerritMock gerritMock = new GerritMock();

        gerritMock.createBranch(BRANCH_1);

        final String user = "user";
        final String change1 = gerritMock.createNewChange(user, BRANCH_1);
        assertEquals("Lookup did not return correct changId", change1,
                gerritMock.getChangeId(user, BRANCH_1));
        gerritMock.submit(change1);
        final String change2 = gerritMock.createNewChange(user, BRANCH_1);
        assertEquals("Lookup did not return correct changId", change2,
                gerritMock.getChangeId(user, BRANCH_1));

    }

    @Test
    public void setCollectionsExpectation() throws ResourceNotFoundException, IOException {

        final GerritMock gerritMock = new GerritMock();
        gerritMock.createBranch(BRANCH_1);
        final String changeId = gerritMock.createNewChange(USER_1, BRANCH_1);
        final GitCommit commit = gerritMock.getCommit(changeId);

        final CommitInformation commitInformation = mock(CommitInformation.class);

        gerritMock.setExpectionsFor(commitInformation, changeId, "some-project");

        final String actualParent = commitInformation.getParentsSHAs(commit.sha, "some-project").get(0);
        final String expectedParent = commit.parentSha;
        assertEquals("The expectation did not return the correct parent", expectedParent,
                actualParent);
    }
}
