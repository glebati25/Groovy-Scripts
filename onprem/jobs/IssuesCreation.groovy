//2020

import groovy.json.JsonSlurper;
import groovy.json.internal.LazyMap;
import groovy.util.logging.Slf4j
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.event.type.EventDispatchOption
import java.sql.Timestamp
import com.atlassian.crowd.embedded.api.Group
import sun.net.www.protocol.https.HttpsURLConnectionImpl;
import jira.customLog.Logger

def operators = new JsonSlurper().parse("".toURL()) as ArrayList

def String setJobTitle(def jobTitle) { 
        if(jobTitle.equals("") || jobTitle.equals("") || jobTitle.equals("")
           || jobTitle.equals("")) {
        	jobTitle = ""	
        } else if(jobTitle.equals("")) {
            jobTitle = ""
        } else {
            Logger.warn("job title is undefined")
        }
        return jobTitle
    }

 	def String setLocation(def location) {
        location = placeValidation(location)
        return location
	}

	def String setDateTime(def dateTime) {
        String tmp = dateTime
        dateTime = tmp.substring(0,10) 
        String s = (String)dateTime
        List<String> d = Arrays.asList(s.split("-"));
        String ss = d.get(2) + "/" + d.get(1) + "/" + d.get(0);
        dateTime = ss
        return dateTime
	}

def placeValidation(def place) {
    	def checkboxCode
    
    	switch(place) {
            case "" : checkboxCode = ""
            	break;
            case "" : checkboxCode = ""
            	break;
        	default : 
            	String message = "" + place;
            	Logger.error(message)
        		return "11111111" 
        		break
        	return checkboxCode
    	}
	}

// the project key under which the issue will get created
final String projectKey = ""				

// the issue type for the new issue
final String issueTypeName = ""

def issueService = ComponentAccessor.issueService
def constantsManager = ComponentAccessor.constantsManager
def creator = ComponentAccessor.getUserManager().getUserByKey("system_user")

def project = ComponentAccessor.projectManager.getProjectObjByKey(projectKey)

def issueType = constantsManager.allIssueTypeObjects.findByName(issueTypeName)

def numberKField = ComponentAccessor.getCustomFieldManager().getCustomFieldObject("")
def nameField = ComponentAccessor.getCustomFieldManager().getCustomFieldObject("") 
def noReleaseDateField = ComponentAccessor.getCustomFieldManager().getCustomFieldObject("")
def employeePositionField = ComponentAccessor.getCustomFieldManager().getCustomFieldObject("")
def projectCodeField = ComponentAccessor.getCustomFieldManager().getCustomFieldObject("")
def teamLeadField = ComponentAccessor.getCustomFieldManager().getCustomFieldObject("")
def superviserField = ComponentAccessor.getCustomFieldManager().getCustomFieldObject("")
def placeField = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectsByName("")[0]
def group = ComponentAccessor.getCustomFieldManager().getCustomFieldObject("")

	for(int i = 0; i < operators.size(); i++) {
        def numberK = operators.get(i).getAt("NumberK")
        
        if(((String)numberK).isInteger()) {
    		numberK = ((String)numberK).toInteger()
        }else {
            Logger.warn("Incorrect number");
            continue
        }
        
    	def nameRus = operators.get(i).getAt("NameRUS")
    	def projectCode = operators.get(i).getAt("ProjectCode")
    	def jobTitle = setJobTitle(operators.get(i).getAt("JobTitle"))
    	def supervisorName = operators.get(i).getAt("SupervisorName")
    	def teamLeadName = operators.get(i).getAt("TeamleadName")
    	def supervisorMail = operators.get(i).getAt("SupervisorMail")
    	def teamLeadMail = operators.get(i).getAt("TeamleadMail")
    	def location = setLocation(operators.get(i).getAt("Location"))
    	def dateTime = setDateTime(operators.get(i).getAt("DateTime"))
        
        
        // the summary of the new issue
		  final String summary = " " + nameRus + " " + dateTime
        
      if(location == null || location == "11111111") {
          continue
      }
        
      def issueInputParameters = issueService.newIssueInputParameters()
    	issueInputParameters.setProjectId(project.id)
      issueInputParameters.setIssueTypeId(issueType.id)
      issueInputParameters.setReporterId(creator.getKey())
      issueInputParameters.setSummary(summary)
    	
        issueInputParameters.addCustomFieldValue(numberKField.getId(), (String)numberK)
        issueInputParameters.addCustomFieldValue(nameField.getId(), (String)nameRus)
        issueInputParameters.addCustomFieldValue(noReleaseDateField.getId(), (String)dateTime)
        issueInputParameters.addCustomFieldValue(employeePositionField.getId(), (String)jobTitle)
        issueInputParameters.addCustomFieldValue(projectCodeField.getId(), (String)projectCode)
        issueInputParameters.addCustomFieldValue(teamLeadField.getId(), (String)teamLeadName)
        issueInputParameters.addCustomFieldValue(superviserField.getId(), (String)supervisorName)
		issueInputParameters.addCustomFieldValue(placeField.getId(), (String)location)
      
        def validationResult = issueService.validateCreate(creator, issueInputParameters)
       
		if(validationResult.isValid()){
			def result = issueService.create(creator, validationResult)
            Logger.info("")
		} else {
            Logger.error(validationResult.getErrorCollection().toString())
            continue
		}
        
        def issue = validationResult.getIssue()
        
        Date d = (Date)issue.getCustomFieldValue(ComponentAccessor.getCustomFieldManager().getCustomFieldObject(""));
		
        Calendar calendar = Calendar.getInstance()
        calendar.setTime(d)
        if(calendar.get(Calendar.DAY_OF_WEEK) == 5 || calendar.get(Calendar.DAY_OF_WEEK) == 6) {
            d = d + 4
        } else 
        	d = d + 2
          
		Timestamp ts = new Timestamp(d.getTime());
		issue.setDueDate(ts)
        
		ComponentAccessor.getIssueManager().updateIssue(ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser(), 
                                                issue, EventDispatchOption.DO_NOT_DISPATCH, false)
		
        ApplicationUser watcher
        def watcherManager = ComponentAccessor.getWatcherManager()     
        JsonSlurper slurper = new JsonSlurper()

		def jsonData = slurper.parseText(sendPost(
    		"/api/v2?query={people{list(projectCode:$projectCode){employeenumberint,employeenamerus,email,status,positions,location{id,city{country_id}}}}}")) as LazyMap
        def locationByNumberK = slurper.parseText(sendPost(
    		/api/v2?query={people{list(k_number:$numberK){location{id,city{country_id}}}}}")) as LazyMap
        
		def data = jsonData.getAt("data")
		def people = data.getAt("people")
		def list = people.getAt("list") as ArrayList
		String location_id = (String)(locationByNumberK.getAt("data").getAt("people").getAt("list").getAt("location").getAt("id"))

		for(int j = 0; j < list.size(); j++) {
    		String position = list.get(j).getAt("positions")		
    		if((location_id).equals("["+((String)(list.get(j).getAt("location").getAt("id")))+"]")) {
    			if(position.toLowerCase().contains("team lead") || position.toLowerCase().contains("supervisor")) {
        			def email = list.get(j).getAt("email")
                    
        			if(((String)email).equals("") || ((String)email) == null) {
            			continue		
                    }else {
                        watcher = ComponentAccessor.getUserManager().getUserByName((((String)(email)).split("@")[0]).substring(1))
       					watcherManager.startWatching(watcher, issue)
                    }	
    			}
    		}
		}
        
        ApplicationUser supervisorWatcher
		ApplicationUser teamleadWatcher
        
        if(supervisorMail.equals("") || supervisorMail == null || teamLeadMail.equals("") || teamLeadMail == null)
        {
            if(supervisorMail == null || supervisorMail != "") {
                if(teamLeadMail == "" || teamLeadMail == null) {
                    continue
                }else {
                    teamleadWatcher = ComponentAccessor.getUserManager().getUserByName(((String)(teamLeadMail)).split("@")[0])
       				watcherManager.startWatching(teamleadWatcher, issue)
                }
            }
            
            if(teamLeadMail.equals("") || teamLeadMail == null) {
                if(supervisorMail == null || supervisorMail == "") {
                    continue
                }else {   
        			supervisorWatcher = ComponentAccessor.getUserManager().getUserByName(((String)(supervisorMail)).split("@")[0])
        			watcherManager.startWatching(supervisorWatcher, issue)
        		}
            } 
        }else {
            teamleadWatcher = ComponentAccessor.getUserManager().getUserByName(((String)(teamLeadMail)).split("@")[0])
       		watcherManager.startWatching(teamleadWatcher, issue)
            supervisorWatcher = ComponentAccessor.getUserManager().getUserByName(((String)(supervisorMail)).split("@")[0])
        	watcherManager.startWatching(supervisorWatcher, issue)
        }
	}
          
final public String sendPost(String url){    
    def connection = url.toURL().openConnection() as HttpsURLConnectionImpl;
    connection.setReadTimeout(30000);
    connection.setRequestMethod("POST");
    connection.setDoOutput(true);
    //connection.getOutputStream().write(bodyText.getBytes("UTF-8"));
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
