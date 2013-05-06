var formidable = require('formidable')
  , http = require('http')
  , util = require('util')
  , request = require('request')
  , execSync = require('execSync')
  , async = require('async')
  , gm = require('gm')
  , fs = require('fs');

var port = 8080
  , fmworker = 'http://localhost:5000/findmatch/'
  , fmmeta = 'http://facematchmeta.rs.af.cm/username/'
  , fmresult = []
  , fmfound = {};

var rmDir = function (dirPath) {
    try {
      var files = fs.readdirSync(dirPath);
    } catch (e) {
      return;
    }
    if (files.length > 0) for (var i = 0; i < files.length; i++) {
      var filePath = dirPath + '/' + files[i];
      if (fs.statSync(filePath).isFile()) fs.unlinkSync(filePath);
      else rmDir(filePath);
    }
  };

var getCropped = function (dirPath) {
    var cropped = [];
    try {
      var files = fs.readdirSync(dirPath);
    } catch (e) {
      return;
    }
    if (files.length > 0) for (var i = 0; i < files.length; i++) {
      if (files[i].match(/_crop\.jpg$/)) {
        cropped.push(files[i]);
      }
    }
    return cropped;
  }

function _merge(obj1, obj2) {
  var obj3 = {};
  for (var attrname in obj1) {
    obj3[attrname] = obj1[attrname];
  }
  for (var attrname in obj2) {
    obj3[attrname] = obj2[attrname];
  }
  return obj3;
}

var fmqueue = async.queue(function (task, callback) {
  console.log("Task: "+task.cropped);
  request({
    url: fmworker + task.cropped,
    json: true
  }, function (e, response, fmjson) {
    if (!e && response.statusCode == 200) {
      console.log(fmjson.matches);
      var matches = fmjson.matches.replace(/\d_crop\.jpg$/, '').match(/\/([^\/]+)$/);
      console.log(matches[1]);
      if (matches[1]) {
        request({
          url: fmmeta + matches[1],
          json: true
        }, function (e, response, json) {
          if (json) {
            fmresult.push(_merge(fmjson, json))
          }
          callback();
        });
      }

    } else {
      callback(e);
    }

  });
}, 2);



http.createServer(function (req, res) {
  //console.log(req);
  if (req.url == '/match' && req.method.toLowerCase() == 'post') {
    console.log("/match request ", req.headers);
    console.log("Cleaning tmp directory");

    // parse a file upload
    var form = new formidable.IncomingForm();
    form.uploadDir = '../img/tmp';
    form.keepExtensions = true;

    fmqueue.drain = function () {
      console.log("Finished Result");
      var result = JSON.stringify(fmresult);
      console.log(result);
      res.end(result);
    };

    form.parse(req, function (err, fields, files) {
      res.writeHead(200, {
        'content-type': 'application/json'
      });

      if (err) {
        res.end(JSON.stringify({
          'error': err
        }));
      } else {

        var path = files.upload.path;
        console.log("File path: " + path);
        // The bottleneck needs to be solved.
        // var facecrop = execSync.stdout('java FaceRecognition haarcascade_frontalface_alt.xml ' + form.uploadDir);
        // var cropped = getCropped(form.uploadDir);

        // console.log(cropped);
        fmresult = [], fmfound = {};
        
        gm(path).size(function (err, size){
          if (!err)
            console.log('width: ' + size.width, 'height: ' + size.height);
            //        for (var i = 0; i < cropped.length; i++) {
            fmqueue.push({
              cropped: path.replace(/^\.\.\/img\/tmp\//, '')
            }, function (err) {
              console.log('Finished: ' + path);
              fs.unlink(path);
            });

  //        }
        })

      }
    });

    return;
  }
  
  if (req.url == '/clean' && req.method.toLowerCase() == 'get') {
    rmDir('../img/tmp');
    res.writeHead(200, {
      'content-type': 'text/html'
    });
    res.end('Cleaned Tmp');
    return;
  }

  // show a file upload form
  res.writeHead(200, {
    'content-type': 'text/html'
  });

  fs.readFile('./template/form.html', function(err, html){
    res.end(html);
  })
  
}).listen(port);
console.log("Listening to port: ", port);
