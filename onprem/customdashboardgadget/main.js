if(document.getElementById("pageNum").innerHTML=="1") {
	document.getElementById("previous").hidden=true;
}else {
 	document.getElementById("previous").hidden=false;
}

$.ajax({
      	url: '/rest/scriptrunner/latest/custom/loadLogs?from='+0+'&to='+30,
     	  type: "GET",
      	dataType: "json",
      	async: false,
      	success: function(data){
        	fillTable(data);
        }          
    });


function fillTable(data) {
	  var id = data.id;
  	var message = data.message;
  	var date = data.data;
  	var issueKey = data.issueKey;
  	var status = data.status;
  	var state = data.state;
  	var subsystemname = data.subsystemname;
  	var time = data.time;
    var url = ""
  
  	for(var i = 0; i < id.length; i++) {
  		$('#tableBody').append("<tr id=" + id[i] +"><td class='line'>" + id[i] + "</td>" +
                           "<td class='line'>" + message[i] + "</td>" +
                           "<td class='line'>" + date[i] + "</td>" +
                           "<td class='line'>" + subsystemname[i] + "</td>" +
                           "<td class='line'>" + time[i] + "</td>" +
                           "<td class='line'>" + state[i] + "</td>" +
                           "<td class='line'>" + status[i] + "</td>" +
                           "<td id = " + issueKey[i] + " class='line'> <a href='" + url + "'" + issueKey[i] + "'>" + issueKey[i] + "</a></td>" +
                           "</tr>");
      	if(state[i] == "INFO") {
        	document.getElementById(id[i]).style.color="green";
        }else if(state[i] == "CRITICAL") {
         	document.getElementById(id[i]).style.color="red";
        }else if(state[i] == "WARNING") {
          	document.getElementById(id[i]).style.color="blue";
        }
    }  
	for(var i = 0; i < document.getElementsByTagName("td").length; i++) {
    	if(document.getElementsByTagName("td")[i].textContent == "null") {
      		document.getElementsByTagName("td")[i].textContent="";
    	}
  	}
  
  	for(var i = 0; i < document.getElementsByTagName("a").length; i++) {
    	if(document.getElementsByTagName("a")[i].textContent == "null") {
      		document.getElementsByTagName("a")[i].textContent="";
    	}
  	}
}

function goNext() {  
$('#tableBody').empty();
  	var pageNum = parseInt(document.getElementById("pageNum").innerHTML);
  	var pageFrom = pageNum * 30;
  	pageNum++;
  	document.getElementById("pageNum").innerHTML = pageNum;      
  
	 $.ajax({
      	url: '/rest/scriptrunner/latest/custom/loadLogs?from='+ pageFrom,
     	type: "GET",
      	dataType: "json",
      	async: false,
      	success: function(data){
        	fillTable(data);
        }          
    });
  if(document.getElementById("pageNum").innerHTML=="1") {
		document.getElementById("previous").hidden=true;
  }else {
 		document.getElementById("previous").hidden=false;
  }
}

function goBack() {  
$('#tableBody').empty();
  	var pageNum = parseInt(document.getElementById("pageNum").innerHTML);
  	pageNum-=2;
  
  	var pageFrom = pageNum * 30; 
  	pageNum++;
  	document.getElementById("pageNum").innerHTML = pageNum;      
  
	 $.ajax({
      	url: '/rest/scriptrunner/latest/custom/loadLogs?from='+ pageFrom,
     	type: "GET",
      	dataType: "json",
      	async: false,
      	success: function(data){
        	fillTable(data);
        }          
    });
  
  if(document.getElementById("pageNum").innerHTML=="1") {
		document.getElementById("previous").hidden=true;
  }else {
 		document.getElementById("previous").hidden=false;
  }
}
