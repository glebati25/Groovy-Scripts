package utils.jira.entity

/**
 Purpose: Utility class for managing Issue Properties
 Usage: import into your code and call static utility methods
 Preconditions: none
 Environment: JIRA Software 7.13.2, JSD 3.16.2, ScriptRunner v. 5.8.0
 Script type: Filesystem
 Author: Gleb Yudenok
 */

import com.atlassian.jira.bc.issue.properties.IssuePropertyService
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.entity.property.EntityProperty
import com.atlassian.jira.entity.property.EntityPropertyService.PropertyInput
import com.atlassian.jira.entity.property.EntityPropertyService.SetPropertyValidationResult
import com.atlassian.jira.entity.property.EntityPropertyService.PropertyResult
import com.atlassian.jira.entity.property.EntityPropertyService.DeletePropertyValidationResult
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.user.ApplicationUser
import groovy.util.logging.Slf4j
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Slf4j
class IssuePropertyUtils {

    static final IssuePropertyService issuePropertyService = ComponentAccessor.getComponentOfType(IssuePropertyService)

    static void setProperty(ApplicationUser actor, Issue issue, String json, String key) {
        PropertyInput propertyInput = new PropertyInput(json, key)
        SetPropertyValidationResult result = issuePropertyService.validateSetProperty(actor, issue.id, propertyInput)
        if (result.isValid()) {
            issuePropertyService.setProperty(actor, result)
        } else {
            throw new IllegalArgumentException("Could not set issue property with key $key for issue $key. Error list: $result.errorCollection")
        }
    }

    static EntityProperty getProperty(ApplicationUser actor, Issue issue, String key) {
        PropertyResult result = issuePropertyService.getProperty(actor, issue.id, key)
        if (result.isValid()) {
            return result.entityProperty.getOrNull()
        } else {
            throw new IllegalArgumentException("Property with key $key for issue $issue.key doesn't exist or you don't have access to it. Error list: $result.errorCollection")
        }
    }

    static void deleteProperty(ApplicationUser actor, Issue issue, String key) {
        DeletePropertyValidationResult result = issuePropertyService.validateDeleteProperty(actor, issue.id, key)
        if (result.isValid()) {
            issuePropertyService.deleteProperty(actor, result)
        } else {
            throw new IllegalArgumentException("Property with key $key for issue $issue.key doesn't exist or you don't have access to it. Error list: $result.errorCollection")
        }
    }
}
