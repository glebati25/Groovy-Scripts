package utils.jira.workflow

import com.atlassian.jira.bc.issue.IssueService
import com.atlassian.jira.bc.issue.IssueService.TransitionValidationResult
import com.atlassian.jira.bc.issue.IssueService.IssueResult
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.IssueInputParameters
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.workflow.TransitionOptions
import groovy.util.logging.Slf4j

@Slf4j
class TransitionUtils {

    static void transitionIssue(ApplicationUser actor, Issue issue, int targetActionId, boolean skipChecks = false) {
        transitionIssue(actor, issue?.id, targetActionId, skipChecks)
    }

    static void transitionIssue(ApplicationUser actor, Long issueId, int targetActionId, boolean skipChecks = false) {
        IssueService issueService = ComponentAccessor.issueService

        IssueInputParameters newIssueInputParameters = issueService.newIssueInputParameters()
        TransitionOptions.Builder builder = new TransitionOptions.Builder()
        TransitionOptions transitionOptions
        if (skipChecks) {
            transitionOptions = builder
                .skipPermissions()
                .setAutomaticTransition()
                .skipValidators()
                .skipConditions()
                .build()
            newIssueInputParameters.skipScreenCheck()
        } else {
            transitionOptions = builder.build()
        }

        TransitionValidationResult transitionValidationResult = issueService.validateTransition(
            actor, issueId, targetActionId, newIssueInputParameters, transitionOptions
        )
        if (transitionValidationResult.valid) {
            IssueResult result = issueService.transition(actor, transitionValidationResult)
            if (!result.valid) {
                log.warn('Issue with id {}: Transition result is invalid, although it does not mean that transition failed: {}',
                    issueId, transitionValidationResult.errorCollection)
            }
        } else {
            throw new IllegalStateException("Issue with id: $issueId: Transition validation failed. $transitionValidationResult.errorCollection")
        }
    }

}
