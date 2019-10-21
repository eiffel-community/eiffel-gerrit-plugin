package com.ericsson.gerrit.plugins.eiffel.git;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Appender;
import org.apache.log4j.LogManager;
import org.apache.log4j.spi.LoggingEvent;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

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

    /*
     * https://github.com/google/guice/issues/1115 Change the code to get the injects via the
     * constrcutor instead for easiser testing
     */

    @Mock
    private Appender mockAppender;
    @Captor
    private ArgumentCaptor<LoggingEvent> captorLoggingEvent;


    @Before
    public void setup() {
        org.apache.log4j.Logger rootLogger = LogManager.getRootLogger();
        rootLogger.addAppender(mockAppender);
    }

    @After
    public void tearDown() {
        org.apache.log4j.Logger rootLogger = LogManager.getRootLogger();
        rootLogger.removeAppender(mockAppender);

    }

    @Test
    public void testFetchingParentsShas() throws Exception {
        String parent1Sha = "Parent1 sha";
        String parent2Sha = "Parent2 sha";

        CommitsCollection commitsCollection = mock(CommitsCollection.class);
        ProjectsCollection projectsCollection = mock(ProjectsCollection.class);
        ProjectResource projectResource = mock(ProjectResource.class);

        CommitResource commitResource = mock(CommitResource.class);
        RevCommit revCommit = mock(RevCommit.class);
        RevCommit parent1 = mock(RevCommit.class);
        RevCommit parent2 = mock(RevCommit.class);

        when(parent1.getName()).thenReturn(parent1Sha);
        when(parent2.getName()).thenReturn(parent2Sha);
        RevCommit[] parents = (RevCommit[]) Arrays.asList(parent1, parent2).toArray();
        when(revCommit.getParents()).thenReturn(parents);

        when(commitResource.getCommit()).thenReturn(revCommit);
        when(commitsCollection.parse(any(ProjectResource.class), any(IdString.class))).thenReturn(
                commitResource);

        when(projectsCollection.parse(any(String.class), anyBoolean())).thenReturn(projectResource);

        CommitInformation commitInformation = new CommitInformation(commitsCollection,
                projectsCollection);
        String commitId = "sha hash";
        String projectName = "projectName";
        List<String> expectedParentsSha = Arrays.asList(parent1Sha, parent2Sha);

        List<String> actualParentSha = commitInformation.getParents(commitId, projectName);

        assertEquals(expectedParentsSha, actualParentSha);
        verify(mockAppender, times(0)).doAppend(captorLoggingEvent.capture());
    }

    @Test
    public void testFetchingCommitNotFound() throws Exception {

        CommitsCollection commitsCollection = mock(CommitsCollection.class);
        ProjectsCollection projectsCollection = mock(ProjectsCollection.class);
        ProjectResource projectResource = mock(ProjectResource.class);

        when(commitsCollection.parse(any(ProjectResource.class), any(IdString.class))).thenThrow(
                ResourceNotFoundException.class);
        when(projectsCollection.parse(any(String.class), anyBoolean())).thenReturn(projectResource);

        CommitInformation commitInformation = new CommitInformation(commitsCollection,
                projectsCollection);
        List<String> expectedParentsSha = Arrays.asList();
        String commitId = "not found hash";
        String projectName = "projectName";

        List<String> actualParentSha = commitInformation.getParents(commitId, projectName);

        assertEquals(expectedParentsSha, actualParentSha);
        verify(mockAppender, times(1)).doAppend(captorLoggingEvent.capture());
    }

    @Test
    public void testFetchingIOException() throws Exception {
        CommitsCollection commitsCollection = mock(CommitsCollection.class);
        ProjectsCollection projectsCollection = mock(ProjectsCollection.class);
        ProjectResource projectResource = mock(ProjectResource.class);

        when(commitsCollection.parse(any(ProjectResource.class), any(IdString.class))).thenThrow(
                IOException.class);
        when(projectsCollection.parse(any(String.class), anyBoolean())).thenReturn(projectResource);

        CommitInformation commitInformation = new CommitInformation(commitsCollection,
                projectsCollection);
        List<String> expectedParentsSha = Arrays.asList();
        String commitId = "sha hash";
        String projectName = "projectName";

        List<String> actualParentSha = commitInformation.getParents(commitId, projectName);

        assertEquals(expectedParentsSha, actualParentSha);
        verify(mockAppender, times(1)).doAppend(captorLoggingEvent.capture());
    }

    @Test()
    public void testFetchingProjectNotFound() throws Exception {
        CommitsCollection commitsCollection = mock(CommitsCollection.class);
        ProjectsCollection projectsCollection = mock(ProjectsCollection.class);

        when(projectsCollection.parse(any(String.class), anyBoolean())).thenThrow(
                UnprocessableEntityException.class);

        CommitInformation commitInformation = new CommitInformation(commitsCollection,
                projectsCollection);
        String commitId = "sha hash";
        String projectName = "notFoundProject";
        List<String> expectedParentsSha = Arrays.asList();

        List<String> actualParentSha = commitInformation.getParents(commitId, projectName);

        assertEquals(expectedParentsSha, actualParentSha);
        verify(mockAppender, times(1)).doAppend(captorLoggingEvent.capture());
    }

}
