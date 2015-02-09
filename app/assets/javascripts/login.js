var ws = null;
$(window).ready(function(){

  ws = new WebSocket($("body").data("ws-url"));
  ws.onmessage = function(event) {
    console.log(event);
  };

  $("#msgform").submit(function(event){

    event.preventDefault();
    console.log($("#msgtext").val());

    ws.send(JSON.stringify({msg: $("#msgtext").val()}));
    $("#msgtext").val("");

  });
});