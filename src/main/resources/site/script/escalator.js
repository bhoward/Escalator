var Escalator={}

Escalator.getResults = function(id, spec) {
  var srcId = "src" + id
  var resId = "res" + id
  var input = document.getElementById(srcId).value
  var output = document.getElementById(resId)
  
  var ajax
  if (window.XMLHttpRequest) {
    ajax = new XMLHttpRequest()
  } else {
    ajax = new ActiveXObject("Microsoft.XMLHTTP")
  }
  
  ajax.onreadystatechange = function() {
    if (ajax.readyState == 4 && ajax.status == 200) {
      output.innerHTML = ajax.responseText
    }
  }
  
  ajax.open("POST", "/console/" + (spec ? "test" : "example"), true)
  ajax.setRequestHeader("Content-Type", "application/x-www-form-urlencoded")
  ajax.send("input=" + encodeURIComponent(input))
}

// Ctrl-Enter runs the code
Escalator.handleKey = function(e, id, spec) {
  var event = e || window.event
  var key = event.keyCode
  var ctrl = (event.ctrlKey == 1)
  
  if (key == 13 && ctrl) {
    Escalator.getResults(id, spec)
    return false
  }
  
  return true
}
