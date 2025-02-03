import com.atlassian.jira.component.ComponentAccessor;
import groovy.json.internal.LazyMap;
import groovy.json.JsonSlurper;

String text = "LazyMap"//text example for searching
 
String buildSkriptRegistry(response, String type, String text){
    def
        name,
        numsScripts = 0,
        scriptPos = 0,
        searchRows = new StringBuilder();
    for(item in response.getAt(type).asType(ArrayList)){
        name=item.getAt("name");
        for(script in item.getAt("scripts").asType(ArrayList)){
            scriptPos++;
            if(script.getAt("inlineScript").toString().contains(text)){
                numsScripts++;
                searchRows.append("""<tr><td>${scriptPos}</td><td>${name}</td><td>${script.getAt("name")}</td><td><details><summary>See</summary>${script.asType(LazyMap).inlineScript}</details></td></tr>""");
            }
        }
    }
    return """<br><br><details><summary>${type} - Found: <strong>${numsScripts}</strong></summary>
    <table class="issue-table"><thead><tr><th>Position</th><th>Title</th><th>Script name</th><th style='width:1100px;'>Code</th></tr></thead><tbody>
    ${searchRows}</tbody></table></details>
    """
}
 
//API
def baseurl = ComponentAccessor.getApplicationProperties().getString("jira.baseurl")
def url = baseurl+'/rest/scriptrunner/latest/canned/com.onresolve.scriptrunner.canned.jira.admin.ScriptRegistry';
def authString = "".bytes.encodeBase64().toString();
def client = new groovyx.net.http.RESTClient(url);
def resp = client.post(
    headers: [
        'Authorization': 'Basic '+authString,
        //'Content-Type': 'application/json',
    ],
    requestContentType : groovyx.net.http.ContentType.JSON,
    body : '{"canned-script":"com.onresolve.scriptrunner.canned.jira.admin.ScriptRegistry"}'
) as groovyx.net.http.HttpResponseDecorator
def searchText = new StringBuilder();
def searchRows = new StringBuilder();
def numsScripts = 0;
LazyMap response = resp.getData() as LazyMap;/* output - [workflows, listeners, fields, endpoints, behaviours, scriptErrors, fragments, jobs] */
//WORKFLOWS
/*workflows - [active, transitions, draft, name]
    tranzitions - [href, name, configuredItems]
        configuredItems - [scripts, description, clazz, name]
            scripts - [scriptFile, lang, description, scriptCompileCtxOptions, inlineScript, name, scriptCompileContext]*/
def workflowName, transitionName, scriptType, scriptName;
for(workflow in response.output.asType(LazyMap).workflows.asType(ArrayList)){
    workflowName = workflow.getAt("name")
    for(transition in workflow.getAt("transitions").asType(ArrayList)){
        transitionName = transition.getAt("name")
        for(itemm in transition.getAt("configuredItems").asType(ArrayList)){
            scriptType = itemm.getAt("name")
            for(script in itemm.getAt("scripts").asType(ArrayList)){
                if(script.asType(LazyMap).inlineScript.toString().contains(text)){
                    numsScripts++;
                    scriptName = script.getAt("name")
                    searchRows.append("""<tr><td>${workflowName}</td><td>${transitionName}</td><td>${scriptType}</td><td>${scriptName}</td><td><details><summary>See</summary>${script.asType(LazyMap).inlineScript}</details></td></tr>""");
                }
            }
        }
    }
}
searchText.append(
    """<details><summary>Workflow - Found: <strong>${numsScripts}</strong></summary>
<table class="issue-table"><thead><tr><th>Workflow name</th><th>Transition name</th><th>Script type</th><th>Script name</th><th style='width:1100px;'>Код</th></tr></thead><tbody>""");
searchText.append(searchRows)
searchText.append("""</tbody></table></details>""");
 
for(type in ['listeners', 'fields', 'endpoints', 'behaviours', 'fragments', 'jobs']){
    searchText.append(buildSkriptRegistry(response.output, type, text))
}
 
searchText.append("""</div>""");
searchText
