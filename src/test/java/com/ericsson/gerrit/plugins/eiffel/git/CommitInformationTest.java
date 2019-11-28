package com.ericsson.gerrit.plugins.eiffel.git;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.ericsson.gerrit.plugins.eiffel.loghelper.LogHelper;
import com.google.gerrit.extensions.restapi.IdString;
import com.google.gerrit.extensions.restapi.ResourceNotFoundException;
import com.google.gerrit.extensions.restapi.UnprocessableEntityException;
import com.google.gerrit.server.project.CommitResource;
import com.google.gerrit.server.project.CommitsCollection;
import com.google.gerrit.server.project.ProjectResource;
import com.google.gerrit.server.project.ProjectsCollection;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ RevCommit.class })
public class CommitInformationTest {

    private final LogHelper logHelper = new LogHelper();


    @Before
    public void setUp() {
        logHelper.setup();
    }


    @After
    public void tearDown() {
        logHelper.tearDown();
    }

    @Test
    public void testFetchingParentsShas() throws Exception {
        final String parent1Sha = "Parent1 sha";
        final String parent2Sha = "Parent2 sha";

        final CommitsCollection commitsCollection = mock(CommitsCollection.class);
        final ProjectsCollection projectsCollection = mock(ProjectsCollection.class);
        final ProjectResource projectResource = mock(ProjectResource.class);

        final CommitResource commitResource = mock(CommitResource.class);
        final RevCommit revCommit = mock(RevCommit.class);
        final RevCommit parent1 = mock(RevCommit.class);
        final RevCommit parent2 = mock(RevCommit.class);

        when(parent1.getName()).thenReturn(parent1Sha);
        when(parent2.getName()).thenReturn(parent2Sha);
        final RevCommit[] parents = (RevCommit[]) Arrays.asList(parent1, parent2).toArray();
        when(revCommit.getParents()).thenReturn(parents);

        when(commitResource.getCommit()).thenReturn(revCommit);
        when(commitsCollection.parse(any(ProjectResource.class), any(IdString.class))).thenReturn(
                commitResource);

        when(projectsCollection.parse(any(String.class), anyBoolean())).thenReturn(projectResource);

        final CommitInformation commitInformation = new CommitInformation(commitsCollection,
                projectsCollection);
        final String commitId = "sha hash";
        final String projectName = "projectName";
        final List<String> expectedParentsSha = Arrays.asList(parent1Sha, parent2Sha);

        final List<String> actualParentSha = commitInformation.getParentsSHAs(commitId, projectName);

        assertEquals(expectedParentsSha, actualParentSha);

        // Verify we have not logged
        logHelper.verifyLoggerCalledTimes(0);

    }

    @Test
    public void testFetchingCommitNotFound() throws Exception {
        logHelper.removeStdoutAppenders();

        final CommitsCollection commitsCollection = mock(CommitsCollection.class);
        final ProjectsCollection projectsCollection = mock(ProjectsCollection.class);
        final ProjectResource projectResource = mock(ProjectResource.class);

        when(commitsCollection.parse(any(ProjectResource.class), any(IdString.class))).thenThrow(
                ResourceNotFoundException.class);
        when(projectsCollection.parse(any(String.class), anyBoolean())).thenReturn(projectResource);

        final CommitInformation commitInformation = new CommitInformation(commitsCollection,
                projectsCollection);
        final List<String> expectedParentsSha = Arrays.asList();
        final String commitId = "not found hash";
        final String projectName = "projectName";

        final List<String> actualParentSha = commitInformation.getParentsSHAs(commitId, projectName);

        assertEquals(expectedParentsSha, actualParentSha);
        logHelper.verifyLoggerCalledTimes(1);

    }

    @Test
    public void testFetchingIOException() throws Exception {
        logHelper.removeStdoutAppenders();

        final CommitsCollection commitsCollection = mock(CommitsCollection.class);
        final ProjectsCollection projectsCollection = mock(ProjectsCollection.class);
        final ProjectResource projectResource = mock(ProjectResource.class);

        when(commitsCollection.parse(any(ProjectResource.class), any(IdString.class))).thenThrow(
                IOException.class);
        when(projectsCollection.parse(any(String.class), anyBoolean())).thenReturn(projectResource);

        final CommitInformation commitInformation = new CommitInformation(commitsCollection,
                projectsCollection);
        final List<String> expectedParentsSha = Arrays.asList();
        final String commitId = "sha hash";
        final String projectName = "projectName";

        final List<String> actualParentSha = commitInformation.getParentsSHAs(commitId, projectName);

        assertEquals(expectedParentsSha, actualParentSha);
        logHelper.verifyLoggerCalledTimes(1);

    }

    @Test
    public void testFetchingProjectNotFound() throws Exception {
        logHelper.removeStdoutAppenders();

        final CommitsCollection commitsCollection = mock(CommitsCollection.class);
        final ProjectsCollection projectsCollection = mock(ProjectsCollection.class);

        when(projectsCollection.parse(any(String.class), anyBoolean())).thenThrow(
                UnprocessableEntityException.class);

        final CommitInformation commitInformation = new CommitInformation(commitsCollection,
                projectsCollection);
        final String commitId = "sha hash";
        final String projectName = "notFoundProject";
        final List<String> expectedParentsSha = Arrays.asList();

        final List<String> actualParentSha = commitInformation.getParentsSHAs(commitId, projectName);

        assertEquals(expectedParentsSha, actualParentSha);
        logHelper.verifyLoggerCalledTimes(1);
    }
}
