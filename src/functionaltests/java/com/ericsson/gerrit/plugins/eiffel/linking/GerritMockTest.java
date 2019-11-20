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
        GerritMock gerritMock = new GerritMock();
        String branch = "branch";
        String user1 = "user1";
        gerritMock.createBranch(branch);
        GitCommit base = gerritMock.getHead(branch);

        String changeId = gerritMock.createNewChange(user1, branch);
        GitCommit update = gerritMock.getCommit(changeId);
        assertEquals(base.sha, update.parentSha);

        GitCommit newPatchSet = gerritMock.createNewPatchSet(changeId);
        assertEquals(base.sha, newPatchSet.parentSha);

        GitCommit merged = gerritMock.submit(changeId);
        assertEquals(base.sha, merged.parentSha);
        assertEquals(merged.sha, gerritMock.getHead(branch).sha);
    }

    @Test
    public void oneUserTwoBranches() {
        GerritMock gerritMock = new GerritMock();
        String branch1 = "branch1";
        String branch2 = "branch2";
        String user1 = "user1";
        gerritMock.createBranch(branch1);
        gerritMock.createBranch(branch2);
        GitCommit base = gerritMock.getHead(branch1);
        GitCommit base2 = gerritMock.getHead(branch2);

        String changeId = gerritMock.createNewChange(user1, branch1);
        assertEquals(changeId, gerritMock.getChangeId(user1, branch1));

        GitCommit update = gerritMock.getCommit(changeId);
        assertEquals(base.sha, update.parentSha);

        String changeId2 = gerritMock.createNewChange(user1, branch2);
        assertEquals(changeId2, gerritMock.getChangeId(user1, branch2));

        GitCommit update2 = gerritMock.getCommit(changeId2);
        assertEquals(base2.sha, update2.parentSha);

        GitCommit newPatchSet = gerritMock.createNewPatchSet(changeId);
        assertEquals(base.sha, newPatchSet.parentSha);

        GitCommit newPatchSet2 = gerritMock.createNewPatchSet(changeId2);
        assertEquals(base2.sha, newPatchSet2.parentSha);

        GitCommit merged = gerritMock.submit(changeId);
        assertEquals(base.sha, merged.parentSha);
        assertEquals(merged.sha, gerritMock.getHead(branch1).sha);

        GitCommit merged2 = gerritMock.submit(changeId2);
        assertEquals(base2.sha, merged2.parentSha);
        assertEquals(merged2.sha, gerritMock.getHead(branch2).sha);
    }

    @Test
    public void twoUsers() {
        GerritMock gerritMock = new GerritMock();
        String branch = "branch1";
        String user1 = "user1";
        String user2 = "user2";
        gerritMock.createBranch(branch);
        GitCommit base = gerritMock.getHead(branch);

        String changeId = gerritMock.createNewChange(user1, branch);
        GitCommit update = gerritMock.getCommit(changeId);
        assertEquals(base.sha, update.parentSha);

        String changeId2 = gerritMock.createNewChange(user2, branch);
        assertEquals(changeId2, gerritMock.getChangeId(user2, branch));

        GitCommit update2 = gerritMock.getCommit(changeId2);
        assertEquals(base.sha, update2.parentSha);

        GitCommit merged2 = gerritMock.submit(changeId2);
        assertEquals(base.sha, merged2.parentSha);
        assertEquals(merged2.sha, gerritMock.getHead(branch).sha);

        GitCommit newPatchSet = gerritMock.createNewPatchSet(changeId);
        assertEquals(base.sha, newPatchSet.parentSha);

        GitCommit rebase = gerritMock.rebase(changeId);
        assertEquals(rebase.parentSha, merged2.sha);

        GitCommit merged = gerritMock.submit(changeId);
        assertEquals(merged2.sha, merged.parentSha);
        assertEquals(merged.sha, gerritMock.getHead(branch).sha);
    }

    @Test(expected = AssertionError.class)
    public void createWithoutSubmitFail() {
        GerritMock gerritMock = new GerritMock();

        String branch = "branch";
        gerritMock.createBranch(branch);

        gerritMock.createNewChange("user", branch);
        gerritMock.createNewChange("user", branch);
    }

    @Test(expected = AssertionError.class)
    public void submitBeforeRebaseFail() {
        GerritMock gerritMock = new GerritMock();

        String branch = "branch";
        gerritMock.createBranch(branch);

        String changeUser1 = gerritMock.createNewChange("user1", branch);
        String changeUser2 = gerritMock.createNewChange("user2", branch);
        gerritMock.submit(changeUser2);
        gerritMock.submit(changeUser1);
    }

    @Test
    public void createTwoChages() {
        GerritMock gerritMock = new GerritMock();

        String branch = "branch";
        gerritMock.createBranch(branch);

        String user = "user";
        String change1 = gerritMock.createNewChange(user, branch);
        assertEquals(change1, gerritMock.getChangeId(user, branch));
        gerritMock.submit(change1);
        String change2 = gerritMock.createNewChange(user, branch);
        assertEquals(change2, gerritMock.getChangeId(user, branch));

    }

    @Test
    public void setCollectionsExpectation() throws ResourceNotFoundException, IOException {

        GerritMock gerritMock = new GerritMock();
        String branch = "branch";
        gerritMock.createBranch(branch);
        String changeId = gerritMock.createNewChange("user", branch);
        GitCommit commit = gerritMock.getCommit(changeId);

        CommitInformation commitInformation = mock(CommitInformation.class);

        gerritMock.setExpectionsFor(commitInformation, changeId, "some-project");

        String actualParent = commitInformation.getParentsSHAs(commit.sha, "some-project").get(0);
        String expectedParent = commit.parentSha;
        assertEquals(expectedParent, actualParent);
    }
}
