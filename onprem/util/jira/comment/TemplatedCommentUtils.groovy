package utils.jira.comment

/**
 Purpose: Utils class for creating templated comments
 Usage: import into your code and call public methods
 Preconditions: none
 Environment: JIRA Software 7.13.2, JSD 3.16.2, ScriptRunner v. 5.8.0
 Script type: Filesystem
 */

import com.atlassian.jira.bc.issue.comment.CommentService
import com.atlassian.jira.bc.issue.comment.CommentService.CommentCreateValidationResult
import com.atlassian.jira.bc.issue.comment.CommentService.CommentParameters
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.util.json.JSONObject
import groovy.text.SimpleTemplateEngine
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class TemplatedCommentUtils {

    static final Logger log = LoggerFactory.getLogger(this.getClass())

    /**
     Last parameter is optional. Add it only if you want to evalute your message as a template
     */
    static void addComment(ApplicationUser author, Issue issue, boolean isInternal,
                           boolean dispatchEvent, String commentTemplate, Map templateParams = [:]) {
        CommentService commentService = ComponentAccessor.getComponent(CommentService)
        String commentBody = templateParams.isEmpty()
                ? commentTemplate
                : evaluate(commentTemplate, templateParams)
        CommentParameters commentParams = CommentParameters.builder()
                .body(commentBody)
                .author(author)
                .issue(issue)
                .commentProperties(newCommentProperties(isInternal))
                .build()
        CommentCreateValidationResult createValidationResult = commentService.validateCommentCreate(author, commentParams)
        if (createValidationResult.isValid()) {
            commentService.create(author, createValidationResult, dispatchEvent)
        } else {
            throw new IllegalArgumentException("Issue with key $issue.key doesn't exist or you don't have permissions to comment on it. FullError list: $createValidationResult.errorCollection")
        }
    }

    private static Map newCommentProperties(boolean isInternal) {
        JSONObject jsonObject = new JSONObject(['internal': isInternal])
        ['sd.public.comment': jsonObject]
    }

    private static String evaluate(String template, Map templateParams) {
        SimpleTemplateEngine engine = new SimpleTemplateEngine()
        Writable writable = engine.createTemplate(template).make(templateParams)
        writable.toString()
    }
}
