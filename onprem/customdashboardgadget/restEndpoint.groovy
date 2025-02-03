import com.onresolve.scriptrunner.runner.rest.common.CustomEndpointDelegate
import groovy.json.JsonBuilder
import groovy.transform.BaseScript
import com.onresolve.scriptrunner.db.DatabaseUtil;
import javax.ws.rs.core.MultivaluedMap
import javax.ws.rs.core.Response
import com.google.gson.Gson
import org.apache.commons.lang3.StringEscapeUtils

@BaseScript CustomEndpointDelegate delegate

List r = new ArrayList()

loadLogs(httpMethod: "GET", groups: ["jira-administrators"]) { MultivaluedMap queryParams, String body ->  
 
String a = queryParams.get("from").toString()
    a = a.replace("[","")
	a = a.replace("]","")
    
int from = a.toInteger()
    
DatabaseUtil.withSql('') { sql ->
    r = sql.rows("SELECT *FROM ORDER BY `id` DESC LIMIT " + from + ", " + 30 + ";");
}
List<String> keys = new ArrayList<String>()
List<String> logMessages = new ArrayList<String>()
List<String> statuses = new ArrayList<String>()
List<String> states = new ArrayList<String>()
List<String> issuesKeys = new ArrayList<String>()
List<String> dates = new ArrayList<String>()
List<String> times = new ArrayList<String>()
List<String> subsystemName = new ArrayList<String>()

for(int i = 0; i < r.size(); i++) {
    keys.add(r.get(i).getAt("id").toString())
    logMessages.add(r.get(i).getAt("log_message").toString())
    statuses.add(r.get(i).getAt("status").toString())
    states.add(r.get(i).getAt("state").toString())
    issuesKeys.add(r.get(i).getAt("issue_key").toString())
    dates.add(r.get(i).getAt("date").toString())
    times.add(r.get(i).getAt("Time").toString())
    subsystemName.add(r.get(i).getAt("subsystem_name").toString());
}

def rt = [:]
rt=[
    	id:keys,
    	message:logMessages,
    	status:statuses,
    	issueKey:issuesKeys,
    	data:dates,
    	time:times,
    	state: states,
    	subsystemname:subsystemName
	]

String answer = new JsonBuilder(rt).toString()

    return Response.ok(new JsonBuilder(rt).toString()).build();
}
