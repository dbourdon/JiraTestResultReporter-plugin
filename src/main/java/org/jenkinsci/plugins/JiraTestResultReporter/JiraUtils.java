/**
 Copyright 2015 Andrei Tuicu

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
package org.jenkinsci.plugins.JiraTestResultReporter;

import com.atlassian.jira.rest.client.api.IssueRestClient;
import com.atlassian.jira.rest.client.api.RestClientException;
import com.atlassian.jira.rest.client.api.domain.BasicIssue;
import com.atlassian.jira.rest.client.api.domain.input.IssueInput;
import com.atlassian.jira.rest.client.api.domain.input.IssueInputBuilder;
import com.atlassian.jira.rest.client.api.domain.util.ErrorCollection;
import com.atlassian.util.concurrent.Promise;
import hudson.EnvVars;
import hudson.model.Job;
import hudson.tasks.test.TestResult;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.JiraTestResultReporter.config.AbstractFields;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by tuicu.
 */
public class JiraUtils {
    private static final Logger LOGGER = Logger.getLogger("JiraIssuePlugin.log");

    /**
     * Constructs the URL for an issue, given the server url and the issue key
     * @param serverURL
     * @param issueKey
     * @return
     */
    public static String getIssueURL(String serverURL, String issueKey) {
        return serverURL + (serverURL.charAt(serverURL.length() - 1) == '/' ? "" : "/") + "browse/" + issueKey;
    }

    public static void log(String message) {
        LOGGER.log(Level.INFO, message);
    }

    public static void logError(String message, Exception e) {
        LOGGER.log(Level.SEVERE, message, e);
    }

    public static void logError(String message) {
        LOGGER.log(Level.SEVERE, message);
    }

    public static void logWarning(String message) {
        LOGGER.log(Level.WARNING, message);
    }

    public static void logWarning(String message, Exception e) { LOGGER.log(Level.WARNING, message, e);}
    /**
     * Static getter for the JiraTestDataPublisherDescriptor singleton instance
     * @return
     */
    public static JiraTestDataPublisher.JiraTestDataPublisherDescriptor getJiraDescriptor() {
        return (JiraTestDataPublisher.JiraTestDataPublisherDescriptor) Jenkins.getInstance().getDescriptor(JiraTestDataPublisher.class);
    }

    /**
     * Form a single string from the messages returned in a RestClientException
     * @param e a RestClientException
     * @param newLine string representing the new line
     * @return
     */
    public static String getErrorMessage(RestClientException e, String newLine) {
        StringBuilder errorMessages = new StringBuilder();
        for (ErrorCollection errorCollection : e.getErrorCollections()) {
            if (errorMessages.length() != 0) {
                errorMessages.append(newLine);
            }
            errorMessages.append("Error ").append(errorCollection.getStatus());
            for (String message : errorCollection.getErrorMessages()) {
                errorMessages.append(newLine).append(message);
            }

            for (Map.Entry<String, String> entry : errorCollection.getErrors().entrySet()) {
                errorMessages.append(newLine).append(entry.getValue());
            }
        }
        return errorMessages.toString();
    }

    public static String createIssueInput(Job project, TestResult test, EnvVars envVars) {
        final IssueRestClient issueClient = JiraUtils.getJiraDescriptor().getRestClient().getIssueClient();
        final IssueInputBuilder newIssueBuilder = new IssueInputBuilder(
                JobConfigMapping.getInstance().getProjectKey(project),
                JobConfigMapping.getInstance().getIssueType(project));
        //first use the templates and then override them if other configs exist
        for(AbstractFields f : JiraTestDataPublisher.JiraTestDataPublisherDescriptor.templates) {
            newIssueBuilder.setFieldInput(f.getFieldInput(test, envVars));
        }
        for (AbstractFields f : JobConfigMapping.getInstance().getConfig(project)) {
            newIssueBuilder.setFieldInput(f.getFieldInput(test, envVars));
        }
        IssueInput issueInput = newIssueBuilder.build();
        Promise<BasicIssue> issuePromise = issueClient.createIssue(issueInput);
        return issuePromise.claim().getKey();
    }

}
