//2020
import com.onresolve.jira.groovy.user.FieldBehaviours;
import groovy.transform.BaseScript;
import com.onresolve.jira.groovy.user.FormField

@BaseScript FieldBehaviours fieldBehaviours

import com.atlassian.jira.issue.search.SearchProvider
import com.atlassian.jira.issue.search.SearchQuery
import com.atlassian.jira.jql.parser.JqlQueryParser 
import com.atlassian.jira.jql.query.IssueIdCollector
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.Issue
import java.util.Date

def List getIssuesByJQL(String jql) {

    def jqlQueryParser = ComponentAccessor.getComponent(JqlQueryParser)
    def searchProvider = ComponentAccessor.getComponent(SearchProvider)
    def issueManager = ComponentAccessor.getIssueManager()
    def user = ComponentAccessor.getUserManager().getUserByKey("system_user")
    def query = jqlQueryParser.parseQuery(jql)
    def iManager = ComponentAccessor.getIssueManager();
    IssueIdCollector collector = new IssueIdCollector()
    SearchQuery searchQuery = SearchQuery.create(query, user)
    searchProvider.search(searchQuery, collector)
    return collector.getIssueIds().collect { iManager.getIssueObject(it as Long) }
}

def reasonOfAbsentionId = ""
def reasonOfAbsention = getFieldById(reasonOfAbsentionId) 

def issue = getUnderlyingIssue()

def numK

def issueTypeId = ""
if(issue?.getIssueTypeId() == issueTypeId) { 
	numK = issue?.getCustomFieldValue(ComponentAccessor.getCustomFieldManager().
                                    getCustomFieldObject(""))

	if(numK.getClass() == java.util.Collections$UnmodifiableSet){
    	numK = numK.asType(List).join(" ")
	}
}else{
	numK = getFieldById("").getValue() 
}

if(numK != "0") {

FormField periodSince = getFieldById("") 
FormField periodFor = getFieldById("") 

if(reasonOfAbsention?.getFormValue() in [""]) {
    periodSince.setRequired(true)
    periodFor.setRequired(true) 
}else {
    periodFor.setHelpText("")
    periodSince.setRequired(false)
    periodFor.setRequired(false)
}
if(reasonOfAbsention.getFormValue() == "") { 
    getFieldById("").setHidden(false)
    getFieldById("").setFormValue("")
    periodFor.setHelpText("")
}else if (getFieldScreen().getId() == "") {	
    periodFor.setHelpText("")
    getFieldById("").setHidden(false)
}else {
    periodFor.setHelpText("")
    getFieldById("").setHidden(true)
}

if(reasonOfAbsention?.getFormValue() == "") { 
    validate("", numK, periodSince, periodFor)
}else if(reasonOfAbsention?.getFormValue() == "") { 
    validate("", numK, periodSince, periodFor)
}else if(getFieldById("").getFormValue() != "") { 
 	validate("", numK, periodSince, periodFor)   
}
}

public final void validate(def issueType, def numK, FormField periodSince, FormField periodFor) {
    
	if(periodSince?.getValue() && periodFor?.getValue()) {
   		Date periodSinceValue = (Date)periodSince?.getValue()
   		Date periodForValue = (Date)periodFor?.getValue()
    
   		if(periodSinceValue && periodForValue) {
      		String periodSinceForJQL = periodSinceValue.format('yyyy-MM-dd')
      		String periodForForJQL = periodForValue.format('yyyy-MM-dd')
        
     		def query = "project = '' AND issuetype = '$issueType' AND resolution was not \"Won't Fix\" "
         		query+= " AND '' = '$numK' AND ('Period since' >= $periodSinceForJQL AND 'Period since' <= $periodForForJQL OR 'Period for' >= $periodSinceForJQL AND 'Period for' <= $periodForForJQL"
         		query+=" OR 'Period since' >= $periodForForJQL AND 'Period for' <= $periodForForJQL OR 'Period since' <= $periodForForJQL AND 'Period for' >= $periodForForJQL)"

       		List<Issue> issues = getIssuesByJQL(query)
            
            if(issues.size() == 0) {
                getFieldById("").clearError()
            } else {
            
       			def message = """There are overlapping dates in the tasks: """;
       			def baseUrl = ""
       			for (def iss in issues)
       			{
          			def key = iss.getKey()
          			if(issues.get(issues.size()-1) != iss) {
             			message+="""<a href= "$baseUrl/browse/${iss.key}"> ${key}, </a>"""  
          			}else {
              			message+="""<a href= "$baseUrl/browse/${iss.key}"> ${key}.</a>"""  
          			}
       			}
			
            	if(getFieldScreen().getId() == "" || getFieldScreen().getId() == "") { //Экран создания отпуска 
       				message+="""</br> You can't create request. </br>""" 
               		getFieldById("").setError(message)
            	}else {
                	message+="""</br> If you click the confirm button, the ${issueType} ticket will not be created. </br> A connection will be formed between this request and"""
        		
                	for (def iss in issues) {
          				def key = iss.getKey()
          				if(issues.get(issues.size()-1) != iss) {
              				message+="""<a href= "$baseUrl/browse/${iss.key}"> ${key},</a>"""  
          				} else {
             				message+="""<a href= "$baseUrl/browse/${iss.key}"> ${key}.</a>""" 
          				}
      				}
                	getFieldById("").clearError()
            	}
            
       			(issues) ? periodFor.setHelpText(message) : periodFor.setHelpText("")
    		}
        }
	}
}
