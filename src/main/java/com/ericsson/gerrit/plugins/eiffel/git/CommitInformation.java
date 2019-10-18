package com.ericsson.gerrit.plugins.eiffel.git;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.gerrit.plugins.eiffel.events.generators.EiffelSourceChangeSubmittedEventGenerator;
import com.google.gerrit.extensions.common.CommitInfo;
import com.google.gerrit.extensions.restapi.IdString;
import com.google.gerrit.extensions.restapi.ResourceNotFoundException;
import com.google.gerrit.extensions.restapi.UnprocessableEntityException;
import com.google.gerrit.server.project.CommitResource;
import com.google.gerrit.server.project.GetCommit;
import com.google.gerrit.server.project.ProjectResource;
import com.google.inject.Inject;

/**
 * A class to fetch information of a commit from the repository.
 *
 * <strong>Note:</strong> This class uses injects. To use this class the caller must inject it,
 * otherwise the injection framework cannot instantiate this class properly.
 *
 */
public class CommitInformation {
    private static final Logger LOGGER = LoggerFactory.getLogger(
            EiffelSourceChangeSubmittedEventGenerator.class);

    @Inject
    private com.google.gerrit.server.project.CommitsCollection commitsCollection;

    @Inject
    private com.google.gerrit.server.project.ProjectsCollection projectsCollection;

    /**
     * Fetches a list of parents SHA from a commit. An empty list will be returned if any error
     * occurs.
     *
     * @param commitId    The SHA of the commit to operate on
     * @param projectName The name of the project which the commits resides in
     * @return A list of parents SHAs
     */
    public List<String> getParents(final String commitId, final String projectName) {
        List<CommitInfo> parents = getParentsList(commitId, projectName);
        List<String> parentsSha = parents.stream()
                                         .map(commitInfo -> commitInfo.commit)
                                         .collect(Collectors.toList());
        return parentsSha;
    }

    /**
     * Will return a list of parents for a given commit. If the collections cannot find the project
     * or commit, an empty list will be returned.
     */
    private List<CommitInfo> getParentsList(final String commitId, final String projectName) {
        List<CommitInfo> parents = Collections.emptyList();

        try {
            parents = getParentsFromCommit(commitId, projectName);
        } catch (UnprocessableEntityException e) {
            LOGGER.error(String.format("Cannot find or load the project %s", projectName), e);
        } catch (ResourceNotFoundException e) {
            LOGGER.error(String.format("Cannot find the commit %s", commitId), e);
        } catch (IOException e) {
            LOGGER.error(
                    String.format("Error finding the parents for %s in %s", commitId, projectName),
                    e);
        }
        return parents;
    }

    private List<CommitInfo> getParentsFromCommit(final String commitId, final String projectName)
            throws UnprocessableEntityException, IOException, ResourceNotFoundException {

        ProjectResource projectResource = projectsCollection.parse(projectName, true);
        CommitResource commitResource = commitsCollection.parse(projectResource,
                IdString.fromDecoded(commitId));

        // CommitInfo parses the data into an easier data format
        GetCommit getCommit = new GetCommit();
        CommitInfo commitInfo = getCommit.apply(commitResource);

        return commitInfo.parents;
    }

}
