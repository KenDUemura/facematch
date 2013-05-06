#!/usr/bin/env python

import time
import sys
import os
import json
import argparse

from flask import Flask, Request, request, Response, render_template, g, session, flash, \
     redirect, url_for, abort, jsonify
from flaskext.openid import OpenID
from pyfaces import pyfaces

application = app = Flask('wsgi')
parser = argparse.ArgumentParser(description='Run facematch server')
parser.add_argument('--port', dest='port', action='store', type=int, help='Specify port number default 5000')

@app.route('/')
def welcome():
    return 'welcome '

@app.route('/env')
def env():
    return os.environ.get("VCAP_SERVICES", "{}")

@app.route('/findmatch/<imagepath>', methods=['POST', 'GET'])
def findmatch(imagepath):
    error = None
    if Request.method == 'POST':
#        if valid_login(request.form['username'],
#                       request.form['password']):
#            return log_the_user_in(request.form['username'])
#        else:
#            error = 'Invalid username/password'
        return 'post'

    try:
        start = time.time()
        imgname= "../img/tmp/" + imagepath
        print "findmatch request for: '%s'" % imgname
        if not os.path.exists(imgname):
            result['matches']=None
            return jsonify(**result)
        
        dirname="../img/123813/cropped"
        egfaces=12
        thrshld=8
        pyf=pyfaces.PyFaces(imgname,dirname,egfaces,thrshld)
        result=pyf.findmatch()
        result['src']=imgname
        end = time.time()
        result['time']=end-start
        print "%s" % result
        return jsonify(**result)
    except Exception,detail:
        print detail.args
        print "usage:python pyfacesdemo imgname dirname numofeigenfaces threshold "

    return 'Error'

#@app.route('/mongo')
#def mongotest():
#    from pymongo import Connection
#    uri = mongodb_uri()
#    conn = Connection(uri)
#    coll = conn.db['ts']
#    coll.insert(dict(now=int(time.time())))
#    last_few = [str(x['now']) for x in coll.find(sort=[("_id", -1)], limit=10)]
#    body = "\n".join(last_few)
#    return Response(body, content_type="text/plain;charset=UTF-8")

#def mongodb_uri():
#    local = os.environ.get("MONGODB", None)
#    if local:
#        return local
#    services = json.loads(os.environ.get("VCAP_SERVICES", "{}"))
#    if services:
#        creds = services['mongodb-1.8'][0]['credentials']
#        uri = "mongodb://%s:%s@%s:%d/%s" % (
#            creds['username'],
#            creds['password'],
#            creds['hostname'],
#            creds['port'],
#            creds['db'])
#        print >> sys.stderr, uri
#        return uri
#    else:
#        raise Exception, "No services configured"
    

if __name__ == '__main__':
    args = parser.parse_args()
    app.run(debug=True,host='0.0.0.0',port=args.port)
