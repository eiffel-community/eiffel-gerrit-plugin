package com.ericsson.gerrit.plugins.eiffel.linking;

import static org.junit.Assert.*;

import org.junit.Test;

public class GerritMockTest {

    @Test
    public void test() {
        GerritMock gerritMock = new GerritMock();
        GitCommit base = gerritMock.getCommit();
        GitCommit update1 = gerritMock.newCommit();
        GitCommit update2 = gerritMock.newCommit();
        assertEquals(base.sha, update1.parentSha);
        assertEquals(base.sha, update2.parentSha);

        GitCommit update2Merged = gerritMock.merge(update2);
        assertEquals(base.sha, update2Merged.parentSha);
        assertEquals(update2Merged.sha, gerritMock.getCommit().sha);

        GitCommit update1Rebased = gerritMock.rebase(update1);
        assertEquals(update2Merged.sha, update1Rebased.parentSha);

        GitCommit update1Merged = gerritMock.merge(update1Rebased);
        assertEquals(update2Merged.sha, update1Merged.parentSha);
        assertEquals(update1Merged.sha, gerritMock.getCommit().sha);


    }

}
