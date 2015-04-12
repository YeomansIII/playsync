# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models, migrations


class Migration(migrations.Migration):

    dependencies = [
        ('accounts', '0001_initial'),
    ]

    operations = [
        migrations.CreateModel(
            name='CurrentPlaying',
            fields=[
                ('playid', models.CharField(default=b'EC2028', max_length=6, serialize=False, primary_key=True)),
                ('ready', models.BooleanField(default=False)),
                ('track', models.CharField(max_length=200)),
                ('start_time', models.TimeField(null=True)),
                ('initiating', models.ForeignKey(related_name='initiating_listener', to='accounts.Listener')),
                ('requested', models.ForeignKey(related_name='requesting_listener', to='accounts.Listener')),
            ],
        ),
    ]
