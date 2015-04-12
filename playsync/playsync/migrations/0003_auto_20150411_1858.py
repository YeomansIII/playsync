# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models, migrations


class Migration(migrations.Migration):

    dependencies = [
        ('playsync', '0002_auto_20150411_1847'),
    ]

    operations = [
        migrations.AlterField(
            model_name='currentplaying',
            name='playid',
            field=models.CharField(max_length=6, serialize=False, primary_key=True),
        ),
    ]
