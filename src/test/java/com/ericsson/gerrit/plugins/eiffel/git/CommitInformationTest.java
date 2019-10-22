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
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.log4j.Appender;
import org.apache.log4j.LogManager;
import org.apache.log4j.spi.LoggingEvent;
import org.assertj.core.util.Lists;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
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

    @Rule
    public TestName name = new TestName();
    @Mock
    private Appender mockAppender;
    @Captor
    private ArgumentCaptor<LoggingEvent> captorLoggingEvent;
    private List<Appender> otherAppenders = Lists.emptyList();

    @Before
    public void setup() {
        org.apache.log4j.Logger rootLogger = LogManager.getRootLogger();
        rootLogger.addAppender(mockAppender);
    }


    @After
    public void tearDown() {
        org.apache.log4j.Logger rootLogger = LogManager.getRootLogger();
        rootLogger.removeAppender(mockAppender);

        restoreStdoutAppenders(rootLogger);
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

        // Verify we have not logged
        verify(mockAppender, times(0)).doAppend(captorLoggingEvent.capture());

    }

    @Test
    public void testFetchingCommitNotFound() throws Exception {
        removeStdoutAppenders();

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
        removeStdoutAppenders();

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

    @Test
    public void testFetchingProjectNotFound() throws Exception {
        removeStdoutAppenders();

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

    private void restoreStdoutAppenders(org.apache.log4j.Logger rootLogger) {
        for (Appender appender : otherAppenders) {
            rootLogger.addAppender((Appender) appender);
        }
        otherAppenders.clear();
    }


    private void removeStdoutAppenders() {
        org.apache.log4j.Logger rootLogger = LogManager.getRootLogger();

        Enumeration<Appender> enumeration = rootLogger.getAllAppenders();
        /*
         * A note on mockito and equals: Testing equality with mock objects depends on the context.
         * When removing a the mock from the root logger mockito will call an '=='. Here mockito
         * interprets it as another context and thus does not call '=='.
         */
        otherAppenders = Collections.list(enumeration)
                                    .stream()
                                    .filter(appender -> appender != mockAppender)
                                    .collect(Collectors.toList());

        otherAppenders.forEach(appender -> rootLogger.removeAppender(appender));
    }

}
