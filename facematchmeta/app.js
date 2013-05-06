var express = require('express');
var mongodb = require('mongodb');

run = function(client) {
  var app = express();
  
  app.use( express.bodyParser() );
  app.use( express.cookieParser() );
  app.use( app.router );
  app.use(function(err, req, res, next){
    console.error(err.stack);
    res.send(500, 'Something broke!');
    res.end();
  });
  
  var students = new mongodb.Collection(client, 'students');
//  var surveys = new mongodb.Collection(client, 'surveys');
//  var responses = new mongodb.Collection(client,'responses');
//  var summaries = new mongodb.Collection(client,'summaries');

  app.get('/',function(req,res){
    students.find( {}, {} ).sort({_id:-1}).limit(200).toArray( function(err,docs) {
      if ( err ) { throw new Error(err); }
      else if (!docs.length) {
        console.log("Empty Result: ");
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.write(JSON.stringify({}));
      } else {
        console.log("Returning: ", docs);
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.write(JSON.stringify(docs));
      }
      res.end();
    });
  });
  
  app.post('/',function(req,res){
    /* strip leading and trailing spaces, and collapse multiple spaces
       and then break into an array */
    console.log("Posting JSON", req.body);
    students.insert( req.body, { safe: true }, function(err,objects) {
      if ( err ) { throw new Error(err); }
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.write(JSON.stringify( {'result':'ok'} ));
      res.end();
    });
  });
  
  app.get('/class/:ou',function(req,res){
    students.find({ classes: req.params.ou }).toArray( function(err,doc) {
      if ( err ) { throw new Error(err); }
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.write(JSON.stringify( doc ));
      res.end();
    });
  });
  
  app.get('/username/:username',function(req,res){
    students.findOne({ username: req.params.username }, function(err,doc) {
      if ( err ) { throw new Error(err); }
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.write(JSON.stringify( doc ));
      res.end();
    });
  });
  
  app.get('/respond/:id',function(req,res){
    var id = new client.bson_serializer.ObjectID(req.params.id);
    students.findOne( { _id: id }, function(err,doc) {
      if ( err ) { throw new Error(err); }
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.write(JSON.stringify( doc ));
      res.end();
    });
  });
  
  app.get('/purge', function(req,res){
    students.remove({}, function(err,removed){
      if (err) { throw new Error(err); }
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.write(JSON.stringify( {'removed':removed} ));
      res.end();
    })
  });
  
//  app.post('/respond/:id',function(req,res){
//    var survey_id = new client.bson_serializer.ObjectID(req.params.id);
//    var response = { survey_id: survey_id, choices: req.body.choices }
//    responses.insert( response, function(err,objects) {
//      if ( err ) { throw new Error(err); }
//      var count = req.body.choices.length;
//      req.body.choices.forEach( function(choice) {
//        summaries.update( { survey_id: survey_id, choice: choice }, {$inc: { 'responses' : 1 }}, { safe: true, upsert: true }, function() {
//          if ( --count == 0 ) {
//            res.redirect("/results/" + req.params.id );  
//          }
//        });
//      });
//    });
//  });
//  
//  app.get('/results/:id',function(req,res){
//    var id = new client.bson_serializer.ObjectID(req.params.id);
//    surveys.findOne( { _id: id }, function(err,survey) {
//      summaries.find( { survey_id: id }).toArray(function(err,docs) {
//        if ( err ) { throw new Error(err); }
//        res.header('Cache-Control','private');
//        res.render('results.ejs', { id: req.params.id, survey: survey, responses: docs } );
//      });
//    });
//  });


  var port = process.env.VCAP_APP_PORT || process.env.PORT || 1337;
  app.listen(port);
  console.log('Server listing on port '+ port);

};

if ( process.env.VCAP_SERVICES ) {
  var service_type = "mongodb-1.8";
  var json = JSON.parse(process.env.VCAP_SERVICES);
  var credentials = json[service_type][0]["credentials"];
  var server = new mongodb.Server( credentials["host"], credentials["port"]);
  new mongodb.Db( credentials["db"], server, {} ).open( function(err,client) {
    client.authenticate( credentials["username"], credentials["password"], function(err,replies) { 
      run(client);
    });
  });
} else {
  var server = new mongodb.Server("127.0.0.1",27017,{});
  new mongodb.Db( "facematchmeta", server, {} ).open( function(err,client) {
    if ( err ) { throw err; }
    run(client);
  });
}

