//2020

import com.atlassian.jira.bc.issue.search.SearchService
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.IssueManager
import com.atlassian.jira.web.bean.PagerFilter
import com.atlassian.jira.issue.search.SearchResults
import com.atlassian.jira.issue.search.SearchException
import com.atlassian.jira.jql.builder.JqlQueryBuilder
import com.atlassian.query.Query
import com.atlassian.jira.jql.query.IssueIdCollector
import com.atlassian.jira.issue.search.SearchQuery
import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.jql.parser.JqlQueryParser
import com.atlassian.jira.issue.search.SearchProvider
import groovy.json.JsonSlurper
import groovy.json.internal.LazyMap;
import com.atlassian.jira.bc.issue.IssueService
import com.atlassian.jira.issue.IssueInputParametersImpl
import sun.net.www.protocol.https.HttpsURLConnectionImpl;
import com.atlassian.jira.event.type.EventDispatchOption
import com.atlassian.jira.issue.index.IssueIndexingService;
import com.atlassian.jira.issue.index.IssueIndexingService
import jira.customLog.Logger

Logger.info("Daytime check for dismissal", "Dismissal check")

def jqlQueryParser = ComponentAccessor.getComponent(JqlQueryParser);
def searchProvider = ComponentAccessor.getComponent(SearchProvider);
def issueManager = ComponentAccessor.getIssueManager();
def user = ComponentAccessor.getUserManager().getUserByName("system_user");

def query = jqlQueryParser.parseQuery("");

def iManager = ComponentAccessor.getIssueManager();
IssueIdCollector collector = new IssueIdCollector();
SearchQuery searchQuery = SearchQuery.create(query, user);
searchProvider.search(searchQuery, collector);
List<MutableIssue> issues = collector.getIssueIds().collect { iManager.getIssueObject(it as Long) } as List;
                                                                         

String url = ("") //employee database
boolean flag = false
List<MutableIssue> operators

	operators = (new JsonSlurper().parse(url.toURL())) as List

    for(def i : operators) {  
        String s = "[" + i.getAt("employeenumber") + "]"
        for(def j : issues) {      
            if(s == (String)j.getCustomFieldValue(
                ComponentAccessor.getCustomFieldManager().getCustomFieldObject(""))){
                issues.remove(j)
                break
            }
        }     
	}  	
   
for(int i = 0; i < issues.size(); i++) {
    if(issues.get(i) == null) {
        continue
    }
    
    Logger.info("Employee was fired", "Dismissal check", issues.get(i).getKey())
    
    IssueService issueService = ComponentAccessor.getIssueService()
	
    String numberK = issues.get(i).getCustomFieldValue(ComponentAccessor.getCustomFieldManager().getCustomFieldObject(""))

    if(numberK == null) {
        continue
    }
    
    numberK = numberK.substring(1, (int)((int)numberK.length() - 1))
   
    def urlDate = ""+ numberK + "){enddate}}"; //employee database
    def connection = urlDate.toURL().openConnection() as HttpsURLConnectionImpl;
 
    connection.setRequestMethod("POST");
    connection.setDoOutput(true);

    String line;
   	try {
        connection.connect();
        line = new BufferedReader(new InputStreamReader(connection.getInputStream())).readLine()
        if(line == null || line == "") {
            Logger.info("the date of dismissal is unknown...", "Dismissal check", issues.get(i).getKey())
            continue
        }
        def operator = new JsonSlurper().parseText(line) as LazyMap
     	String str = operator.get("data")
        
        if(str == null || str == "" || str == "Employee=[]}" || str == "{Employee=[{enddate=null}]}") {
            Logger.info("the date of dismissal is unknown...", "Dismissal check", issues.get(i).getKey())     	
            continue
        }
        
        Logger.info("dismissal date is known, date set...", "Dismissal check", issues.get(i).getKey()) 
        
        List<String> rightData = str.split("=").toList()
        List<String> commaSplit = rightData.get(2).split(",").toList()
        List<String> clearDate = commaSplit.get(0).replace("}]}", "").split(" ").toList()
    
        List<String> rightFormat = clearDate.get(0).split("-").toList() 
        
        int year = rightFormat.get(0).toInteger()
        int month = rightFormat.get(1).toInteger()
        int day = rightFormat.get(2).toInteger()
        Date date = new Date();
        date.setYear(year - 1900)
        date.setMonth(month - 1)
        date.setDate(day)
        
        def option = ComponentAccessor.getOptionsManager().findByOptionId()
        issues.get(i).setCustomFieldValue(ComponentAccessor.getCustomFieldManager().getCustomFieldObject(""), option)
        issues.get(i).setCustomFieldValue(ComponentAccessor.getCustomFieldManager().getCustomFieldObject(""), date.toTimestamp())
        ComponentAccessor.getIssueManager().updateIssue(user, issues.get(i), EventDispatchOption.DO_NOT_DISPATCH, false)
        def issueIndexingService = ComponentAccessor.getComponent(IssueIndexingService);
		issueIndexingService.reIndex(issues.get(i));
        
    } catch (Exception e){
        Logger.critical(e.getMessage(), "Dismissal check", issues.get(i).getKey());
    } finally {
   		connection.disconnect();
    } 
    
		Logger.info("The request should close", "Dismissal check", issues.get(i).getKey())
		def actionId = "" // change this to the step that you want the issues to be transitioned to
		def transitionValidationResult
		def transitionResult
		def customFieldManager = ComponentAccessor.getCustomFieldManager()

 		transitionValidationResult = issueService.validateTransition(user, issues.get(i).getId(), actionId, new IssueInputParametersImpl())
        
		Logger.critical(transitionValidationResult.errorCollection.getErrorMessages().toString(), "Dismissal check", issues.get(i).getKey())
 		if (transitionValidationResult.isValid()) {
 			transitionResult = issueService.transition(user, transitionValidationResult)
 		}
}
