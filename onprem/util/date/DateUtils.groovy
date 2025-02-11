/**
 Purpose: Sets customfield values from Insight attributes selected in Dynamic Forms
 Usage: Add as postfunction in place where multiple consecutive insight postfunctions-setters were used
 Preconditions: The checkbox for updating an issue shouldn't be set if the next post-function is the same script
 Enviroment: JIRA Software 8.13.3, JSD 4.13.3, ScriptRunner v. 6.17.0, Insight
 Script type: Filesystem
 Author: Gleb Yudenok
 */

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.event.type.EventDispatchOption
import com.atlassian.jira.issue.CustomFieldManager
import com.atlassian.jira.issue.IssueManager
import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.issue.customfields.CustomFieldType
import com.atlassian.jira.issue.customfields.impl.RenderableTextCFType
import com.atlassian.jira.issue.customfields.impl.TextAreaCFType
import com.atlassian.jira.issue.customfields.impl.UserCFType
import com.atlassian.jira.issue.customfields.impl.MultiUserCFType
@WithPlugin('com.atlassian.servicedesk')
import com.atlassian.servicedesk.internal.customfields.participants.ParticipantsCFType
import com.atlassian.jira.issue.fields.CustomField
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.user.util.UserManager
import com.onresolve.scriptrunner.parameters.annotation.Checkbox
import com.onresolve.scriptrunner.parameters.annotation.CustomFieldPicker
import com.onresolve.scriptrunner.parameters.annotation.ShortTextInput
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectFacade
import com.onresolve.scriptrunner.runner.customisers.WithPlugin
@WithPlugin('com.riadalabs.jira.plugins.insight')
import com.riadalabs.jira.plugins.insight.services.model.ObjectAttributeBean
import com.riadalabs.jira.plugins.insight.services.model.ObjectBean
import com.riadalabs.jira.plugins.insight.services.jira.customfield.DefaultObjectCustomField
import groovy.transform.Field
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import com.atlassian.jira.security.JiraAuthenticationContext

import static com.atlassian.jira.issue.IssueFieldConstants.ASSIGNEE
import static com.atlassian.jira.issue.IssueFieldConstants.REPORTER

@Field final Logger log = LoggerFactory.getLogger(this.class)
@Field final String LOG_PREFIX = 'Postfunction:Set customfield values from insight attributes'
@Field final String SEPARATOR = ','
@Field final String ALT_ATTRS_SEPARATOR = ':'
@Field final List<String> SYSTEM_FIELDS = [ASSIGNEE, REPORTER]
@Field final String AUTOMATIC_ASSIGN_KEY = 'AUTOMATIC-ASSIGN'
@Field final ObjectFacade objectFacade = ComponentAccessor.getOSGiComponentInstanceOfType(ObjectFacade)
final IssueManager issueManager = ComponentAccessor.issueManager

MutableIssue mutableIssue = issue as MutableIssue

@CustomFieldPicker(label = 'Поле с типом Insight Object/s', description = 'Значения будут браться из атрибутов данного поля', placeholder='Выберите поле')
CustomField SOURCE_INSIGHT_FIELD
@ShortTextInput(label = 'Список ID атрибутов', description = 'Атрибуты поля Insight, перечисленные через запятую без пробелов. При необходимости указать альтернативные атрибуты в рамках заполняемого поля используйте двоеточие для разделителя.')
String ATTRIBUTE_IDS
@ShortTextInput(label = 'Список ID настраиваемых полей', description = 'Поля, которые будут заполнены значениями из атрибутов. Перечисляются через запятую без пробелов. Для системных полей используйте assignee, reporter')
String TARGET_FIELD_IDS
@Checkbox(label = 'Обновить запрос', description = 'Выполняет обновление задачи. Следует ставить, если последующие постфункции на этом переходе используют поля изменяемые в текущей постфункции или если данная постфункция находится после реиндекса.')
Boolean UPDATE_ISSUE

final boolean someConfigsAreNotSet = !SOURCE_INSIGHT_FIELD || !ATTRIBUTE_IDS || !TARGET_FIELD_IDS
if (someConfigsAreNotSet) {
    log.error('{}:{} Config error! Some fields were not selected in workflow configuration.', LOG_PREFIX, mutableIssue)
    return
}

JiraAuthenticationContext jiraAuthenticationContext = ComponentAccessor.jiraAuthenticationContext
ApplicationUser user = jiraAuthenticationContext.loggedInUser

try {
    ApplicationUser admin = ComponentAccessor.userManager.getUserByKey(SystemAdminUsers.PORTAL_ROBOT)
    jiraAuthenticationContext.loggedInUser = admin

    List<String> attrIdsStringed = ATTRIBUTE_IDS.split(SEPARATOR)
    List<String> fieldIds = TARGET_FIELD_IDS.split(SEPARATOR)

    if (!validateInputIds(attrIdsStringed, fieldIds)) {
        log.error('{}:{} Config error! Number of attribute ids does not match number of field ids, or some ids are not numbers. Attribute ids: {}, field ids: {}',
            LOG_PREFIX, mutableIssue, attrIdsStringed, fieldIds)
        return
    }

    ObjectBean objectBean = SOURCE_INSIGHT_FIELD.getValue(mutableIssue)?.find()
    if (!objectBean) {
        log.info('{}:{} Custom field with name {} has no value',
            LOG_PREFIX, mutableIssue, SOURCE_INSIGHT_FIELD.name)
        return
    }

    Map<Integer, List<String>> attrToField = mapAttrsToFields(attrIdsStringed, fieldIds)
    Map<Integer, Object> values = objectBean.objectAttributeBeans
        .findAll { ObjectAttributeBean attr -> attrToField[attr.objectTypeAttributeId] != null }
        .collectEntries { ObjectAttributeBean attr -> [attr.objectTypeAttributeId, attr.objectAttributeValueBeans] }

    @Field UserManager userManager = ComponentAccessor.userManager
    @Field CustomFieldManager customFieldManager = ComponentAccessor.customFieldManager

    values.each { Integer attrId, Object value ->
        attrToField[attrId].each { String fieldId ->
            setFieldValue(fieldId, value, mutableIssue)
        }
    }

    if (UPDATE_ISSUE) {
        issueManager.updateIssue(admin, mutableIssue, EventDispatchOption.DO_NOT_DISPATCH, false)
    }
} catch (any) {
    log.error('{}:{} Error with setting fields form attributes of an insight customfield.', LOG_PREFIX, mutableIssue, any.message)
} finally {
    jiraAuthenticationContext.loggedInUser = user
}

private Map<Integer, List<String>> mapAttrsToFields(List<String> attrIds, List<String> fieldIds) {
    Map<Integer, List<String>> mapping = [:]
    fieldIds.eachWithIndex { String fieldId, int index ->
        String attrEntry = attrIds[index]
        List<Integer> attrs = attrEntry.contains(ALT_ATTRS_SEPARATOR)
            ? attrEntry.split(ALT_ATTRS_SEPARATOR).collect { Integer.parseInt(it) }
            : [Integer.parseInt(attrEntry)]

        attrs.each { Integer attrId ->
            List<String> currentFieldIds = mapping[attrId]
            mapping[attrId] = currentFieldIds ? currentFieldIds + [fieldId] : [fieldId]
        }
    }
    mapping
}

private boolean validateInputIds(List<String> attrIds, List<String> fieldIds) {
    boolean sizesAreEqual = attrIds.size() == fieldIds.size()
    boolean fieldEntriesAreNumberOrSystem = fieldIds.every { it.isNumber() || it in SYSTEM_FIELDS }
    boolean attrEntriesAreNumbers = attrIds.every { String id ->
        id.isNumber() ||
            id.split(ALT_ATTRS_SEPARATOR).with { String[] alternativeAttrs ->
                alternativeAttrs.size() > 1 && alternativeAttrs.every { it.isNumber() }
            }
    }
    sizesAreEqual && fieldEntriesAreNumberOrSystem && attrEntriesAreNumbers
}

private void setFieldValue(String fieldId, Object value, MutableIssue issue) {
    switch (fieldId) {
        case REPORTER:
            issue.reporter = userManager.getUserByKey(value?.find()?.value as String)
            break
        case ASSIGNEE:
            issue.assignee = userManager.getUserByKey(value?.find()?.value as String)
            break
        default:
            CustomField cf = customFieldManager.getCustomFieldObject("customfield_$fieldId")
            if (cf) {
                Object finalValue = getValueByType(cf.customFieldType, value)
                issue.setCustomFieldValue(cf, finalValue)
            } else {
                log.warn('{}:{} Custom field with id {} was not found.', LOG_PREFIX, issue, fieldId)
            }
    }
}

private Object getValueByType(CustomFieldType customFieldType, Object value) {
    switch (customFieldType) {
        case TextAreaCFType:
        case RenderableTextCFType:
            return value?.find()?.value.toString()
        case UserCFType:
            return userManager.getUserByKey(value?.find()?.value as String)
        case MultiUserCFType:
        case ParticipantsCFType:
            return value?.findAll()?.value?.findResults{ String userKey ->
                userKey && userKey instanceof String
                ? userManager.getUserByKey(userKey as String)
                : null
            }
        case DefaultObjectCustomField:
            Integer fieldValue = value?.find()?.value
            return fieldValue ? [objectFacade.loadObjectBean(fieldValue)] : null
        default:
            return value?.find()?.value
    }
}
