from django.http import HttpResponse
from playsync.models import CurrentPlaying
from django.contrib.auth.models import User
from accounts.models import Listener

import json, uuid
from datetime import datetime, timedelta

SERVER = 'gcm.googleapis.com'
PORT = 5235
USERNAME = "853482743730"
API_KEY = "AIzaSyDwjcMgrT6eITolk3TkcRBlkYrddVJLZPQ"
REGISTRATION_ID = "Registration Id of the target device"

def usergen():
    from base64 import b32encode
    from hashlib import sha1
    from random import random
    username = b32encode(sha1(str(random())).digest()).lower()[:6]
    print("\n\nusername: "+username)
    return username

# Create your views here.
def mylistenerid(request, gcmId):
    listener = Listener.objects.filter(gcm_id=gcmId)

    if listener:
        listener = listener[0]
        jsons = {
            'success': True,
            'id': listener.user.username
        }
    else:
        new_user = User.objects.create(username=(str(uuid.uuid4().get_hex().upper()[0:6])))
        new_user.save()
        listener = Listener.objects.create(user=new_user, gcm_id=gcmId)
        listener.save()
        jsons = {
            'success': True,
            'id': listener.user.username
        }

    return HttpResponse(json.dumps(jsons))


def is_ready(request, playId):
    curPlay = CurrentPlaying.objects.filter(playid__exact=playId)[0]

    if curPlay:
        if curPlay.ready:
            jsons = {
                'success': True,
                'ready': True,
                'track': str(curPlay.track),
                'start': str(curPlay.start_time),
                'now': str(datetime.utcnow().time())
            }
        else:
            jsons = {
                'success': True,
                'ready': False
            }
    else:
        jsons = {
            'success': False,
            'error': "invalid playID"
        }

    return HttpResponse(json.dumps(jsons))

def ready(request, playId):
    curPlay = CurrentPlaying.objects.filter(playid__exact=playId)[0]


    if curPlay:
        if not curPlay.ready:
            curPlay.ready = True
            dt = datetime.utcnow()
            dt = dt + timedelta(0,10)
            curPlay.start_time = dt.time()
            curPlay.save()
        jsons = {
            'success': True,
            'track': str(curPlay.track),
            'start': str(curPlay.start_time),
            'now': str(datetime.utcnow().time())
        }
    else:
        jsons = {
            'success': False,
            'error': "invalid playID"
        }

    return HttpResponse(json.dumps(jsons))

def initiate(request, trackPlay, requested_listener_id, myId):
    requestedL = Listener.objects.filter(user__username__exact=requested_listener_id)
    initiatingL = Listener.objects.filter(user__username__exact=myId)

    if requestedL and initiatingL:
        requestedL = requestedL[0]
        initiatingL = initiatingL[0]

        cp = CurrentPlaying.objects.filter(initiating=initiatingL, requested=requestedL, track=trackPlay)
        if not cp:
            cp = CurrentPlaying.objects.create(ready=False,playid=(str(uuid.uuid4().get_hex().upper()[0:6])), initiating=initiatingL, requested=requestedL, track=trackPlay)
            cp.save()
        else:
            cp = cp[0]

        jsons = {
            'success': True,
            'playId': str(cp.playid)
        }

        from gcm import *

        gcm = GCM(API_KEY)
        data = {'message': initiatingL.user.username + ' wants to listen to a song with you!', 'pId': cp.playid}

        reg_id = requestedL.gcm_id

        gcm.plaintext_request(registration_id=reg_id, data=data)

    else:
        jsons = {
            'success': False,
            'error': "requested user does not exist"
        }

    return HttpResponse(json.dumps(jsons))
