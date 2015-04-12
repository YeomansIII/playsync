from django.db import models
from django.contrib.auth.models import User

# Create your models here.
class Listener(models.Model):
    user = models.OneToOneField(User)
    gcm_id = models.CharField(max_length=3000)
