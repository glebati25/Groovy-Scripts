package utils.jira.issuetype

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.issuetype.IssueType

class IssueTypeUtils {
    static String getIssueTypeIdByName(String name) {
        Collection<IssueType> issueTypes = ComponentAccessor.constantsManager.allIssueTypeObjects
        IssueType type = issueTypes.find { it.name == name }
        type.id
    }
}
