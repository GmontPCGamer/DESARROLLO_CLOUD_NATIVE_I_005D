#!/usr/bin/env python3
"""Ejecutar por SSM en EC2 después de source /opt/nexotech/env.sh.
Prueba los transportes cloud con datos sintéticos, sin sustituir una sesión Entra.
"""
import base64
import datetime
import json
import os
import subprocess
import time
import urllib.parse
import urllib.request
import uuid

def read_offers():
    with urllib.request.urlopen('http://127.0.0.1:8080/api/public/offers',timeout=5) as response:
        return json.load(response)

def poll(predicate):
    end=time.monotonic()+90
    while time.monotonic()<end:
        try:
            if predicate(): return
        except (OSError,ValueError):
            pass
        time.sleep(1)
    raise AssertionError('La comprobación cloud no terminó en 90 segundos')

subject='smoke-cloud-'+str(uuid.uuid4())
previous=read_offers()
count=next((o['reviewCount'] for o in previous if o['productId']==1),0)
notice={'userId':subject,'title':'Prueba de integración cloud','message':'RabbitMQ y Kafka activos','type':'REVIEW'}
vhost=urllib.parse.quote(os.environ.get('SPRING_RABBITMQ_VIRTUAL_HOST','/'),safe='')
url='http://'+os.environ['SPRING_RABBITMQ_HOST']+':15672/api/exchanges/'+vhost+'/nexotech.notifications.exchange/publish'
credentials=os.environ['SPRING_RABBITMQ_USERNAME']+':'+os.environ['SPRING_RABBITMQ_PASSWORD']
body={'properties':{'content_type':'application/json','delivery_mode':2},'routing_key':'notification.user',
      'payload':json.dumps(notice),'payload_encoding':'string'}
request=urllib.request.Request(url,data=json.dumps(body).encode(),headers={
    'Content-Type':'application/json','Authorization':'Basic '+base64.b64encode(credentials.encode()).decode()})
with urllib.request.urlopen(request,timeout=10) as response:
    assert json.load(response)['routed']

event={'eventId':subject,'reviewId':990001,'productId':1,'rating':5,
       'createdAt':datetime.datetime.now(datetime.timezone.utc).isoformat()}
line='1:'+json.dumps(event)+'\n'
subprocess.run(['docker','compose','-p','nexotech-ep3','-f','/opt/nexotech/current/docker-compose.yml','exec','-T','kafka1','kafka-console-producer',
    '--bootstrap-server','kafka1:29092','--topic','nexotech.review-created',
    '--property','parse.key=true','--property','key.separator=:',
    '--producer-property','acks=all'],input=line+line,text=True,check=True,capture_output=True,timeout=30)

def rabbit_ack():
    logs=subprocess.check_output(['journalctl','-u','nexotech-notification-service','--since','5 minutes ago','--no-pager'],text=True)
    return any(subject in line and 'ACK aviso persistido' in line for line in logs.splitlines())
def kafka_offer():
    return any(o['productId']==1 and o['reviewCount']==count+1 for o in read_offers())
poll(rabbit_ack)
poll(kafka_offer)
print(json.dumps({'result':'PASS','rabbitPersistedAndAcked':True,'kafkaConsumed':True,'duplicateCountedOnce':True}))
