package utils.jira.cf

/**
 Purpose: Utils class for operations with custom fields
 Usage: import into your code
 Preconditions: none
 Environment: JIRA Software 7.13.2, JSD 3.16.2, ScriptRunner v. 5.8.0
 Script type: Filesystem
 */

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.event.type.EventDispatchOption
import com.atlassian.jira.issue.CustomFieldManager
import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.issue.customfields.option.Option
import com.atlassian.jira.issue.customfields.option.Options
import com.atlassian.jira.issue.fields.CustomField
import com.atlassian.jira.issue.fields.config.FieldConfigScheme
import com.atlassian.jira.user.ApplicationUser
import groovy.util.logging.Slf4j

@Slf4j
class CustomFieldUtils {

    static final CustomFieldManager customFieldManager = ComponentAccessor.customFieldManager

    static Option getCfOptionForAssociatedProject(final CustomField customField,
                                                  final String value,
                                                  final long associatedProjectId) {
        List<FieldConfigScheme> configSchemes = customField.configurationSchemes
        FieldConfigScheme scheme = configSchemes.find {
            FieldConfigScheme scheme -> associatedProjectId in scheme.associatedProjectIds
        }
        scheme = scheme ?: configSchemes[0]
        if (!scheme) {
            log.error('Could not retrieve any configuration schemes for field with id {}.', customField.id)
            return
        }
        Options options = ComponentAccessor.optionsManager.getOptions(scheme.oneAndOnlyConfig)
        options.find { it.value == value }
    }

    static void setSingleUserPickerCf(ApplicationUser actor, MutableIssue issue, CustomField cf, String username) {
        ApplicationUser newValue = username
                ? ComponentAccessor.userManager.getUserByName(username)
                : null
        issue.setCustomFieldValue(cf, newValue)
        ComponentAccessor.issueManager.updateIssue(actor, issue, EventDispatchOption.DO_NOT_DISPATCH, false)
    }

    /**
     * Allows you to update multiple custom fields at once. Example usage:
     *
     * // ... some code where you retrieve the actor and issue arguments ...
     * Map<Long, Object> mapping = [10001: 'newValue1', 10002: 'newValue2'] // where 10001 and 10002 are cf ids
     * CustomFieldUtils.bulkUpdateCustomFields(actor, issue, mapping)
     */
    static void bulkUpdateCustomFields(ApplicationUser actor, MutableIssue issue, Map<Long, Object> updateMapping) {
        updateMapping.each { Long key, Object value ->
            CustomField cf = customFieldManager.getCustomFieldObject(key)
            if (cf) {
                issue.setCustomFieldValue(cf, value)
            } else {
                log.error('Could not retrieve custom field with id {}.', key)
            }
        }
        ComponentAccessor.issueManager.updateIssue(actor, issue, EventDispatchOption.DO_NOT_DISPATCH, false)
    }
}
