from django.conf.urls import include, url
from apiv1 import views

urlpatterns = [
    # Examples:
    # url(r'^$', 'playsync.views.home', name='home'),
    # url(r'^blog/', include('blog.urls')),

    url(r'^mylistenerid/(?P<gcmId>.+)$', views.mylistenerid, name="mylistenerid"),
    url(r'^initiate/(?P<trackPlay>.+)/(?P<requested_listener_id>.+)/(?P<myId>.+)$', views.initiate, name='initiate'),
    url(r'^isready/(?P<playId>.+)$', views.is_ready, name="is_ready"),
    url(r'^ready/(?P<playId>.+)$', views.ready, name="ready"),
]
