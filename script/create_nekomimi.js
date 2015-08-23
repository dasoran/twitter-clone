var exec = require('child_process').exec;
var fs = require('fs');
//var elasticsearch = require('elasticsearch');
//var client = new elasticsearch.Client({
//    host: 'localhost:9200',
//      log: 'trace'
//});


  var data = fs.readFileSync('graph/groupmapping.json', 'utf8');
  exec("curl -XPOST 'http://localhost:9200/nekomimi' -d '" + data + "'", {maxBuffer: 400*1024}, function (error, stdout, stderr) {
    if(stdout !== null){
        console.log('stdout: ' + stdout);
    }
    if (error !== null) {
      console.log('Exec error: ' + error);
    }

  });
