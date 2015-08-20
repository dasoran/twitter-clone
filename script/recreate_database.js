var exec = require('child_process').exec;
var fs = require('fs');
//var elasticsearch = require('elasticsearch');
//var client = new elasticsearch.Client({
//    host: 'localhost:9200',
//      log: 'trace'
//});


exec('curl -XDELETE "http://localhost:9200/*"', {maxBuffer: 400*1024}, function (error, stdout, stderr) {
  if(stdout !== null){
      console.log('stdout: ' + stdout);
  }
  if (error !== null) {
    console.log('Exec error: ' + error);
  }


  var data = fs.readFileSync('settings-and-mapping.json', 'utf8');
  exec("curl -XPOST 'http://localhost:9200/twitter-clone' -d '" + data + "'", {maxBuffer: 400*1024}, function (error, stdout, stderr) {
    if(stdout !== null){
        console.log('stdout: ' + stdout);
    }
    if (error !== null) {
      console.log('Exec error: ' + error);
    }

    var data = fs.readFileSync('user.json', 'utf8');
    exec("curl -XPOST 'http://localhost:9200/_bulk' -d '" + data.replace(/'/g, "'\\''") + "'", {maxBuffer: 400*1024}, function (error, stdout, stderr) {
      if(stdout !== null){
        var response = JSON.parse(stdout);
        if (response.errors == true) {
          console.log('stdout: ' + stdout);
        } else {
          console.log('user created');
        }
        //console.log('stdout: ' + stdout);
      }
      if (error !== null) {
        console.log('Exec error: ' + error);
      }


      var data = fs.readFileSync('tweet.json', 'utf8');
      exec("curl -XPOST 'http://localhost:9200/_bulk' -d '" + data.replace(/'/g, "'\\''")  + "'", {maxBuffer: 400*1024}, function (error, stdout, stderr) {
        if(stdout !== null){
          var response = JSON.parse(stdout);
          if (response.errors == true) {
            console.log('stdout: ' + stdout);
          } else {
            console.log('tweet created');
          }
        }
        if (error !== null) {
          console.log('Exec error: ' + error);
        }

        var data = fs.readFileSync('tweet2.json', 'utf8');
        exec("curl -XPOST 'http://localhost:9200/_bulk' -d '" + data.replace(/'/g, "'\\''")  + "'", {maxBuffer: 400*1024}, function (error, stdout, stderr) {
          if(stdout !== null){
            var response = JSON.parse(stdout);
            if (response.errors == true) {
              console.log('stdout: ' + stdout);
            } else {
              console.log('tweet2 created');
            }
          }
          if (error !== null) {
            console.log('Exec error: ' + error);
          }

          var data = fs.readFileSync('tweet3.json', 'utf8');
          exec("curl -XPOST 'http://localhost:9200/_bulk' -d '" + data.replace(/'/g, "'\\''")  + "'", {maxBuffer: 400*1024}, function (error, stdout, stderr) {
            if(stdout !== null){
              var response = JSON.parse(stdout);
              if (response.errors == true) {
                console.log('stdout: ' + stdout);
              } else {
                console.log('tweet3 created');
              }
            }
            if (error !== null) {
              console.log('Exec error: ' + error);
            }
          });

            var data = fs.readFileSync('tweet4.json', 'utf8');
            exec("curl -XPOST 'http://localhost:9200/_bulk' -d '" + data.replace(/'/g, "'\\''")  + "'", {maxBuffer: 400*1024}, function (error, stdout, stderr) {
              if(stdout !== null){
                var response = JSON.parse(stdout);
                if (response.errors == true) {
                  console.log('stdout: ' + stdout);
                } else {
                  console.log('tweet4 created');
                }
              }
              if (error !== null) {
                console.log('Exec error: ' + error);
              }
            });



        });
      });
    });
  });
});
