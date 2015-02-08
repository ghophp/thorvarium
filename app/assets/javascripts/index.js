$(window).ready(function(){

  var ws = new WebSocket($("body").data("ws-url"));
  ws.onmessage(function(event) {
    var message = JSON.parse(event.data);
    switch (message.type) {
      case "message":
        $("#board tbody").append("<tr><td>" + message.uid + "</td><td>" + message.msg + "</td></tr>");
        break;
      default:
        console.log(message);
    }
  });

  $("#msgform").submit(function(event){

    event.preventDefault();
    console.log($("#msgtext").val());

    ws.send(JSON.stringify({msg: $("#msgtext").val()}));
    $("#msgtext").val("");

  });
});