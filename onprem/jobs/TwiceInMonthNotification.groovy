//2020

import com.atlassian.mail.server.SMTPMailServer;
import com.atlassian.mail.Email;
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.jql.parser.JqlQueryParser
import com.atlassian.jira.issue.search.SearchProvider
import com.atlassian.jira.jql.query.IssueIdCollector
import com.atlassian.jira.issue.search.SearchQuery
import com.atlassian.jira.issue.Issue
import groovy.json.JsonSlurper;
import sun.net.www.protocol.https.HttpsURLConnectionImpl;
import groovy.json.internal.LazyMap;
import jira.customLog.Logger

final public String sendPost(String url,String bodyText){    
    def connection = url.toURL().openConnection() as HttpsURLConnectionImpl;
    connection.setReadTimeout(30000);
    connection.setRequestMethod("POST");
    connection.setDoOutput(true);
    connection.getOutputStream().write(bodyText.getBytes("UTF-8"));
    String line;
    try {
        connection.connect();
    line = new BufferedReader(new InputStreamReader(connection.getInputStream())).readLine();  
    } catch (Exception e){
        log.error(e);
    } finally {
     connection.disconnect();
    }
    return line;
}

final public String getDataByK(String numberK){
    def query = """query={people{count,list(k_number:$numberK){
    			employeenumberint,employeenamerus,parentsFullInfo{
                teamlead{employeenamerus,employeenumber,email}
                director{employeenamerus,employeenumber,email}}}}}"""
    String response = sendPost('/api/v2',query)
    return response;
}

final public String getDataByEmail(String email) {
    def query = """query={people{count,list(email:"$email"){
    			employeenumberint,employeenamerus,parentsFullInfo{
                teamlead{employeenamerus,employeenumber,email}
                director{employeenamerus,employeenumber,email}}}}}"""
    String response = sendPost('/api/v2',query)
    return response;
}

def user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()

def jqlQueryParser = ComponentAccessor.getComponent(JqlQueryParser)
def searchProvider = ComponentAccessor.getComponent(SearchProvider)

def query = jqlQueryParser.parseQuery("""project = Test """);
IssueIdCollector collector = new IssueIdCollector()
SearchQuery searchQuery = SearchQuery.create(query, user);
searchProvider.search(searchQuery, collector)
List<Issue> issues = collector.getIssueIds().collect() { ComponentAccessor.getIssueManager().getIssueObject(it as Long) } as List; 

def issuesRG = [:]  

LazyMap<String, String> lazymap = new LazyMap<String, String>()
def issuesDirector = [:]	
def numKField = ComponentAccessor.getCustomFieldManager().getCustomFieldObject("")

for(int i = 0; i < issues.size(); i++) {
    def numKValue = numKField.getValue(issues.get(i))
    def response = getDataByK((String)(numKValue.getAt(0)))	
    if(response) {
    	LazyMap employeeData = new JsonSlurper().parseText(response) as LazyMap	
    	def teamleadMail = employeeData?.getAt("data")?.getAt("people")?.getAt("list")?.getAt("parentsFullInfo")?.getAt("teamlead")?.getAt("email")?.getAt(0) 
		def directorMail = employeeData?.getAt("data")?.getAt("people")?.getAt("list")?.getAt("parentsFullInfo")?.getAt("director")?.getAt("email")?.getAt(0)  
        if(teamleadMail != null) {
    		if(teamleadMail.contains(";")) {
   				int index = teamleadMail.indexOf(";");
   				teamleadMail = teamleadMail.substring(0, index)
                issuesRG[teamleadMail] = []	
			}else {
                issuesRG[teamleadMail] = []	
			}       
        }
        if(directorMail != null) {
    		issuesDirector[directorMail] = []
        }
    }
}

for(int i = 0; i < issues.size(); i++) {
    def numKValue = numKField.getValue(issues.get(i))
    def response = getDataByK((String)(numKValue.getAt(0)))	
    if(response) {
    	LazyMap employeeData = new JsonSlurper().parseText(response) as LazyMap
    	def teamleadMail = employeeData?.getAt("data")?.getAt("people")?.getAt("list")?.getAt("parentsFullInfo")?.getAt("teamlead")?.getAt("email")?.getAt(0)	
    	def directorMail = employeeData?.getAt("data")?.getAt("people")?.getAt("list")?.getAt("parentsFullInfo")?.getAt("director")?.getAt("email")?.getAt(0)	
        if(teamleadMail != null) {
        	if(teamleadMail.contains(";")) {
   				int index = teamleadMail.indexOf(";");
   				teamleadMail = teamleadMail.substring(0, index)
                issuesRG[teamleadMail].add(issues.get(i)) 
			}else {
    			issuesRG[teamleadMail].add(issues.get(i)) 
			}
        }
    }
}

SMTPMailServer smtp = ComponentAccessor.getMailServerManager().getDefaultSMTPMailServer()	
def summary = ""	

def bodyRg = ""	

String link = "https://jira.atlassian.net/browse/"	
SMTPMailServer mailServer = ComponentAccessor.getMailServerManager().getDefaultSMTPMailServer();

for(int i = 0; i < issuesRG.keySet().size(); i++) { 	
    for(int j = 0; j < issuesRG.getAt(issuesRG.keySet().getAt(i)).size(); j++) {	
		bodyRg += "\n" + link + (String)issuesRG.getAt(issuesRG.keySet().getAt(i)).get(j)	
    }
    
	if (mailServer) {      
        //issuesRG.keySet().getAt(i) email РГ
        Email email = new Email((String)issuesRG.keySet().getAt(i).replace(";", ","));
       	email.setSubject(summary);
       	email.setBody((String)bodyRg);
       	mailServer.send(email);
    }
    bodyRg = ""
}

for(int i = 0; i < issuesDirector.size(); i++) {
    for(int j = 0; j < issuesRG.size(); j++) {
        def response = getDataByEmail(issuesRG.keySet().getAt(j))	
        if(response) {
        	def leadOfRgEmail = new JsonSlurper().parseText(response)?.
            	getAt("data")?.getAt("people")?.getAt("list")?.getAt("parentsFullInfo")?.getAt("teamlead")?.getAt("email")?.getAt(0)  
        	if(issuesDirector.keySet().getAt(i) == leadOfRgEmail) {
            	issuesDirector[issuesDirector.keySet().getAt(i)].add([issuesRG.keySet().getAt(j), issuesRG.getAt(issuesRG.keySet().getAt(j))])
        	}
        }
    }    
}

def bodyDirector = ""

for(int i = 0; i < issuesDirector.size(); i++) {
    for(int j = 0; j < issuesDirector[issuesDirector.keySet().getAt(i)].size(); j++) {
     	def response = getDataByEmail(issuesDirector[issuesDirector.keySet().getAt(i)].getAt(j).getAt(0))
        if(response) {
        	def rgFIO = new JsonSlurper().parseText(response).
            getAt("data")?.getAt("people")?.getAt("list")?.getAt("employeenamerus")?.getAt(0)
        	bodyDirector += "\nРГ: " + rgFIO + "\n"//issuesDirector[issuesDirector.keySet().getAt(i)].getAt(j).get(0) + "<br>"
        	for(int k = 0; k < issuesDirector[issuesDirector.keySet().getAt(i)].getAt(j).get(1).size(); k++) {
    			bodyDirector += link + issuesDirector[issuesDirector.keySet().getAt(i)].
                getAt(j).get(1).get(k) + "\n"
        	}
        }
    }
    
	if (mailServer) {      
        Email email = new Email((String)issuesDirector.keySet().getAt(i).replace(";", ","));
       	email.setSubject(summary);
       	email.setBody((String)bodyDirector);
       	mailServer.send(email);
    }
    
    bodyDirector = ""
}
