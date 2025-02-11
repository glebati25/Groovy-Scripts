package utils.jira.jql

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.search.DocumentWithId
import com.atlassian.jira.issue.search.SearchQuery
import com.atlassian.jira.issue.search.SearchResults
import com.atlassian.jira.jql.parser.JqlParseException
import com.atlassian.jira.jql.parser.JqlQueryParser
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.user.util.UserManager
import com.atlassian.query.Query
import groovy.util.logging.Slf4j
import com.atlassian.jira.issue.search.SearchProvider
import com.atlassian.jira.web.bean.PagerFilter

@Slf4j
class JqlUtils {

    private final static SearchProvider searchProvider = ComponentAccessor.getComponent(SearchProvider)
    private final static UserManager userManager = ComponentAccessor.getComponent(UserManager)
    private final static JqlQueryParser jqlQueryParser = ComponentAccessor.getComponent(JqlQueryParser)

    static final String PAGE_SIZE = 100
    
    /**
     * Searches for lucene documents and applies a function the each of them, all with pagination
     * @param jql - valid jql
     * @param documentFieldIds - field ids for indexed document
     * @param apply - closure that expects Document as argument
     */
    static void searchAndApply(final String jql, final Set<String> documentFieldIds, final Closure<Void> apply) {
        if (apply == null) {
            log.error('Passed null as function to apply')
            throw new IllegalArgumentException('Passed null as function to apply')
        }

        Query query
        try {
            query = jqlQueryParser.parseQuery(jql)
        } catch (JqlParseException jpe) {
            log.error('Failed to parse jql {}', jql)
            throw new IllegalArgumentException("Failed to parse jql ${jql}")
        }
        
        ApplicationUser admin = userManager.getUserByName(SystemAdminUsers.PORTAL_ROBOT)
        SearchQuery searchQuery = SearchQuery.create(query, admin)
        SearchResults<DocumentWithId> results = searchProvider.search(
                searchQuery, PagerFilter.newPageAlignedFilter(0, PAGE_SIZE), documentFieldIds)
        int nextStartIndex = PAGE_SIZE - 1

        while (results.total > 0) {
            results.results.each { DocumentWithId documentWithId ->
                apply.call(documentWithId.document)
            }

            results = searchProvider.search(searchQuery,
                PagerFilter.newPageAlignedFilter(nextStartIndex, PAGE_SIZE),
                documentFieldIds)
            nextStartIndex += PAGE_SIZE - 1
        }
    }
}
