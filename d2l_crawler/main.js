/**
  d2l_crawler
*/
// Desire 2 Learn Classlist Crawler as of 2012/11/26

// Loading Node.js modules
var argv = require('optimist').argv
  , jsdom = require('jsdom')
  , async = require('async')
  , fs = require("fs")
  , jquery = fs.readFileSync("./src/jquery.js").toString()
  , request = require('request');

// Setting Default variables
var base = 'https://sjsu.desire2learn.com'
  , login = base + '/d2l/lp/auth/login/login.d2l'
  , logout = base + '/d2l/logout'
  , viewprofileImage = base + '/d2l/common/viewprofileimage.d2l?'
  , ou = argv.ou || 123813
  , classlist = base + '/d2l/lms/classlist/classlist.d2l?ou=' + ou
  , waitTime = argv.wait || 3000
  , imgdir = '../img/' + ou + '/cropped/'
  , mongodb_uri = process.env.FACEMATCHMETA || argv.mongo || 'http://127.0.0.1:1337'
  , auth = {
    userName:argv.username,
    password:argv.password,
    Login:'Login'
  };

function findUser(username, callback) {
  request(mongodb_uri+'/username/'+username, function(err, res, body){
    if (err) {
      console.log("Failed to access meta server");
      callback();
    } else {
      console.log("Returned from server:", body);
      callback(JSON.parse(body));
    }
  });
}

function test () {
  console.log("Testing Connection with MongoDB");

  async.waterfall([
    function(callback){
      findUser('Ken.Nullemura', function (prof) {
        if (prof) {
          console.log("Fail");
        } else {
          console.log("Ok");
        }
        callback(null);
      });
    },
    function(callback){
      findUser('Harnika.Sahni', function (prof) {
        console.log(prof.username, prof.email);
        callback(null);
      })
    }
    
  ], function(err, result) {
    console.log("All Test Done...");
    process.exit(0);
  })

}

// <p>crawl steps.<br/>
// 1. Login to starts session<br/>
// 2. Parse Classlist page<br/>
// 3. Walk through each student's profile<br/>
// 4. Store Image into img directory<br/>
// 5. Logout<br/>
// 6. Store Metadata into MongoDB</p>
function crawl(callback) {
//  console.log("Authentication: ", auth);

  // Logging in by posting authentication information.
  request.post({url: login, form: auth}, function (err, res, body) {
    if (err) {
      console.log("Failed to login");
      process.exit(1);
    } else {
      console.log("Logged In");
      console.log(res.headers);

      // Get Classlist.
      request(classlist, function (err, res, body) {
        if (err) {
          console.log("Failed to retrieve classlist");
          process.exit(1);
        } else {
          console.log("Parsing Classlist");
          
          var students = [];
            
          jsdom.env({
            html: body,
            done: function (errors, window) {
              if (errors) {
                console.log("jsdom Failed parsing body: \n"+ body + errors);
              } else {
                var table = window.document.getElementById("z_j");
                if (table) {
                  var rows = table.rows;
                  var length = rows.length;
                  for (var i = 3; i < length - 1; i++) {
                    var href = rows[i].cells[1].getElementsByTagName('a')[1].getAttribute('href');
                    var username = rows[i].cells[3].getElementsByTagName('label')[0].innerHTML;
                    students.push({href:href, username:username, classes:ou});
                  }
                  
                  console.log("Set:", students);
                } else {
                  console.log("Failed");
                }
                
              }

              var inputs, scripts, data = [];
              var parseProfile = function (callback) {
                
                var prof = students.shift();
                
                if (prof !== undefined) {
                  
                  findUser(prof.username, function (prevProf) {
                    if ((argv.skip && prevProf)
                    || prof.username.match(/guest/i) 
                    || (prof.username.toLowerCase() === argv.username.toLowerCase()) ) {
                      console.log('Skipping Guest/Own Account');
                      parseProfile(callback);
                      
                    } else {

                      console.log("Retrieving Profile: ", prof);
                    
                      request(base + prof.href, function (err, res, body) {
                        if (err) {
                          console.log("Failed to retrieve profile: ", prof);
                          process.exit(1);
                        } else {
                          console.log("Parsing Profile for: " + prof.username);
                          jsdom.env({
                            html: body,
                            done: function (errors, window) {
                              if (errors) {
                                console.log("Failed to retrieve profile: ", prof);
                                return;
                              }
                              
                              console.log("Retrieved");
                              prof.name = window.document.getElementsByClassName('dhdg_1')[0].innerHTML;
                              // $('#z_n .fcl_w a').html()
                              prof.email = window.document.getElementById('z_n')
                                .getElementsByClassName('fcl_w')[0]
                                .getElementsByTagName('a')[0].innerHTML;
                              
          // Profile with picture
          // "/d2l/img/lp/pixel.gif"
          //
          // If not
          // "/d2l/img/0/Framework.UserProfileBadge.actProfile100.gif?v=9.2.1.143-8"
                              var imgUrl = window.document.getElementById('z_i').getAttribute('src');
                              if (imgUrl && imgUrl.match(/^\/d2l\/img\/lp\/pixel\.gif/)) {
                                console.log("Image: Yes");
                                
                                inputs = window.document.getElementsByTagName('input');
                                scripts = window.document.getElementsByTagName('script');
                                
                                var iLen = inputs.length
                                  , controlMap
                                  , sLen = scripts.length
                                  , lastModified;

                                for (var i = 0; i < iLen; i++) { 
                                  if (inputs[i].getAttribute('name') === 'd2l_controlMap') { 
                                    console.log("Found d2l_controlMap");
                                    try {
                                      controlMap = eval("(" + inputs[i].getAttribute('value') + ");");
                                    } catch (err) {
                                      console.error("ERROR: " + err);
                                      return;
                                    }
                                    break;
                                  }
                                }
                                
                                for (var i = 0; i < sLen; i++) {
                                  if ( matched = scripts[i].innerHTML.match(/'LastModified':(\d+)/) ) {
                                    lastModified = matched[1];
                                    break; 
                                  }
                                }
                                
                                if (controlMap === undefined) {
                                  console.error("ERROR: d2l_controlMap");
                                  return;
                                } else {
          // D2L Keeps image information in d2l_controlMap input value
          //
          // tmp[0].ctl_3[2]
          // [6605, 160260, 100, "11", 29247]
          // https://sjsu.desire2learn.com/d2l/common/viewprofileimage.d2l?
          // oi=6605    # User Id
          // &ui=160260 # ???
          // &s=200     # Image Size in pixel Max 200
          // &lm=634823751847970000 # LastModified
          // &v=11      # ???
          // &cui=29247 # ???
                                  
                                  var param = controlMap[0].ctl_3[2];
                                  param[2] = 200; // Increasing size
                                  var profImageUrl = viewprofileImage
                                    + "oi=" + param[0]
                                    + "&ui=" + param[1]
                                    + "&s=" + param[2];
                                    
                                  if (lastModified)
                                    profImageUrl += "&lm=" + lastModified;
                                    
                                  profImageUrl += "&v=" + param[3]
                                    + "&cui=" + param[4];
                                  
                                  prof.hasImage = 1;
                                  
                                  // Requests image and stream it to the file.
                                  var imgPath = imgdir + prof.username + "_original.JPEG";
                                  console.log("Saving Image: "+profImageUrl+" to "+imgPath);
                                  request(profImageUrl).pipe(fs.createWriteStream(imgPath));
                                  
                                }
                                
                              } else {
                                console.log("Image: No");
                                prof.hasImage = 0;
                              }

                              request.post(mongodb_uri).form(prof);

                              console.log("Parsing next profile in "+ waitTime +"ms...\n");
                              setTimeout(parseProfile(callback), waitTime);
                              
                            }
                          })
                        }
                      })
                    }                    
                  });
                  
                  
                } else {
                  // No more students
                  console.log("All Done");
                  callback();
                }
                
              };
              
              parseProfile(function(){
                // Logout for cleaning session.
                request(logout, function(err, res, data) {
                  if (err) {
                    console.log("Logout Failed");
                    console.log(res.headers);
                    process.exit(1);
                  } else {
                    console.log("Logged Out")
                    console.log(res.headers);
                    callback()
                  }
                })
              
              });
              

              
            }
          })
          
        }
      })

    }
  })

}



if (argv.test) {
  test();
} else {
  
  if (argv.username === undefined || argv.password === undefined) {
    console.log(
"Usage:\n" +
"node main.js --username [USERNAME] --password [PASSWORD]\n"
    );
    process.exit(0);
  }

  console.log("Setting up directory");
  try {
    fs.mkdirSync(imgdir, 0777);
  } catch (err) {
    console.log("Failed to create directory: " + err);
  }
  
  console.log("Starts Crawling");
  crawl(function(){
    console.log("Done")
  });
}
