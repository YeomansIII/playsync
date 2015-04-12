# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models, migrations


class Migration(migrations.Migration):

    dependencies = [
        ('playsync', '0001_initial'),
    ]

    operations = [
        migrations.AlterField(
            model_name='currentplaying',
            name='playid',
            field=models.CharField(default=b'01E2BF', max_length=6, serialize=False, primary_key=True),
        ),
    ]
