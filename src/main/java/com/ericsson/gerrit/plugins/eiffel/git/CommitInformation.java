/*
   Copyright 2019 Ericsson AB.
   For a full list of individual contributors, please see the commit history.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package com.ericsson.gerrit.plugins.eiffel.git;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jgit.revwalk.RevCommit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gerrit.extensions.restapi.IdString;
import com.google.gerrit.extensions.restapi.ResourceNotFoundException;
import com.google.gerrit.extensions.restapi.UnprocessableEntityException;
import com.google.gerrit.server.permissions.PermissionBackendException;
import com.google.gerrit.server.project.CommitResource;
import com.google.gerrit.server.project.CommitsCollection;
import com.google.gerrit.server.project.ProjectResource;
import com.google.gerrit.server.project.ProjectsCollection;
import com.google.inject.Inject;

/**
 * A class to fetch information of a commit from the repository.
 *
 * <strong>Note:</strong> This class uses injects. To use this class the caller must inject it,
 * otherwise the injection framework cannot instantiate this class properly.
 *
 */
public class CommitInformation {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommitInformation.class);
    private final CommitsCollection commitsCollection;
    private final ProjectsCollection projectsCollection;

    /**
     * Package private constructor with injection for testing reasons. Injecting directly as field
     * variables makes it harder to test
     */
    @Inject
    CommitInformation(final CommitsCollection commitsCollection, final ProjectsCollection projectsCollection) {
        this.commitsCollection = commitsCollection;
        this.projectsCollection = projectsCollection;
    }

    /**
     * Fetches a list of parents SHA from a commit. An empty list will be returned if any error
     * occurs.
     *
     * @param commitId    The SHA of the commit to operate on
     * @param projectName The name of the project which the commits resides in
     * @return A list of parents SHAs
     */
    public List<String> getParentsSHAs(final String commitId, final String projectName) {
        final List<RevCommit> parents = getParents(commitId, projectName);
        final List<String> parentsSHAs = getSHAs(parents);
        return parentsSHAs;
    }

    /**
     * Will return a list of parents for a given commit. If the collections cannot find the project
     * or commit, an empty list will be returned.
     */
    private List<RevCommit> getParents(final String commitId, final String projectName) {
        List<RevCommit> parents = Collections.emptyList();

        try {
            parents = getParentsFromCommit(commitId, projectName);
        } catch (final UnprocessableEntityException | PermissionBackendException e) {
            final String message = String.format("Cannot find or load the project %s", projectName);
            LOGGER.error(message, e);
        } catch (final ResourceNotFoundException e) {
            final String message = String.format("Cannot find the commit %s", commitId);
            LOGGER.error(message, e);
        } catch (final IOException e) {
            final String message = String.format("Error finding the commit for %s in %s", commitId,
                    projectName);
            LOGGER.error(message, e);
        }
        return parents;
    }

    private List<RevCommit> getParentsFromCommit(final String commitId, final String projectName)
       throws UnprocessableEntityException, IOException, ResourceNotFoundException, PermissionBackendException {
        RevCommit[] parents = null;
        try{
        final ProjectResource projectResource = projectsCollection.parse(projectName, true);
        final CommitResource commitResource = commitsCollection.parse(projectResource,
                IdString.fromDecoded(commitId));
        final RevCommit commit = commitResource.getCommit();
         parents = commit.getParents();
          }
        catch (PermissionBackendException e) {
           System.out.println("PermissionBackendException is occured");
        }
           return  Arrays.asList(parents);
       
        
    }

    private List<String> getSHAs(final List<RevCommit> parents) {
        final List<String> parentsSha = parents.stream()
                                         .map(parent -> parent.getName())
                                         .collect(Collectors.toList());
        return parentsSha;
    }

}
