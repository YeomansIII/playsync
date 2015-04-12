from django.db import models
from accounts.models import Listener
import uuid

class CurrentPlaying(models.Model):
    playid = models.CharField(max_length=6, primary_key=True)
    initiating = models.ForeignKey(Listener, related_name='initiating_listener')
    requested = models.ForeignKey(Listener, related_name='requesting_listener')
    ready = models.BooleanField(default=False)
    track = models.CharField(max_length=200)
    start_time = models.TimeField(null=True)
