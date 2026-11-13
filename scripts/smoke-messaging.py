#!/usr/bin/env python3
"""Prueba local del recorrido HTTP -> RabbitMQ/notificaciones + Kafka/ofertas.
Requiere scripts/start-all.sh local. JWT_LOCAL_SECRET sólo se lee del entorno.
"""
import base64
import hashlib
import hmac
import json
import os
import time
import uuid
import urllib.error
import urllib.request

BASE = 'http://127.0.0.1:8080/api'
def b64(value):
    return base64.urlsafe_b64encode(value).rstrip(b'=').decode()
secret = base64.b64decode(os.environ.get('JWT_LOCAL_SECRET',
    'c2VjcmV0by1sb2NhbC1wYXJhLWRlc2Fycm9sbG8tZGVtby1jbGF2ZS0xMjM0NTY3ODkwYWJjZGVm'))
subject = 'smoke-' + str(uuid.uuid4())
parts = b64(json.dumps({'alg':'HS256','typ':'JWT'}).encode()) + '.' + b64(json.dumps({
    'sub':subject,'roles':['ADMIN'],'name':'Prueba de integración','iat':int(time.time()),'exp':int(time.time())+300}).encode())
token = parts + '.' + b64(hmac.new(secret, parts.encode(), hashlib.sha256).digest())

def call(path, body=None, authenticated=True, method=None):
    headers = {'Content-Type':'application/json'}
    if authenticated:
        headers['Authorization'] = 'Bearer ' + token
    req = urllib.request.Request(BASE+path, data=json.dumps(body).encode() if body is not None else None,
        headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=15) as response:
            return response.status, json.load(response)
    except urllib.error.HTTPError as error:
        return error.code, None

def eventually(predicate):
    until = time.monotonic()+45
    while time.monotonic()<until:
        if predicate():
            return
        time.sleep(.5)
    raise AssertionError('El mensaje no se reflejó antes del plazo de 45 segundos')

assert call('/public/health', authenticated=False)[0] == 200
product = 1
before = next((x['reviewCount'] for x in call('/public/offers')[1] if x['productId']==product),0)
body = {'rating':5,'comment':'Prueba automática RabbitMQ + Kafka '+subject}
assert call('/products/1/reviews', body, authenticated=False)[0] == 401
assert call('/products/1/reviews', {'rating':6,'comment':'Inválida'})[0] == 400
status, review = call('/products/1/reviews', body)
assert status == 200, status
assert call('/products/1/reviews', body)[0] == 409

def rabbit_received():
    status, notices = call('/notifications')
    return status==200 and any(n['type']=='REVIEW' and 'producto #1' in n['message'] for n in notices)
def kafka_received():
    status, offers = call('/public/offers')
    return status==200 and any(o['productId']==product and o['reviewCount']==before+1 for o in offers)
eventually(rabbit_received)
eventually(kafka_received)
print(json.dumps({'result':'PASS','rabbitNotification':True,'kafkaOffer':True,
    'unauthorized':401,'invalidRating':400,'duplicateReview':409,'productId':product},ensure_ascii=False))

if '--all-flows' in __import__('sys').argv:
    initial = call('/admin/analytics/sales')[1]
    assert call('/admin/kafka/topics',authenticated=False)[0] == 401
    assert call('/cart/items',{'productId':2,'quantity':1})[0] == 200
    status, checkout = call('/orders',{'region':'Metropolitana','commune':'Santiago','paymentMethod':'SIMULATED_CARD'})
    assert status == 200, ('checkout',status)
    def analytics_received():
        status, summary = call('/admin/analytics/sales')
        return status==200 and summary['orderCount']==initial['orderCount']+1 and summary['units']==initial['units']+1
    eventually(analytics_received)
    eventually(lambda:any(n['type']=='ORDER' for n in call('/notifications')[1]))
    topic='demo.smoke-'+str(uuid.uuid4())[:8]
    assert call('/admin/kafka/topics',{'name':topic,'partitions':3,'replicas':2,'config':{'retention.ms':'3600000'}})[0] == 200
    eventually(lambda:any(t['name']==topic for t in call('/admin/kafka/topics')[1]))
    assert call('/admin/kafka/topics/'+topic,{'partitions':4,'config':{'retention.ms':'7200000'}},method='PATCH')[0]==200
    assert call('/admin/kafka/topics/'+topic+'/config')[1]['retention.ms']=='7200000'
    assert call('/admin/kafka/topics/nexotech.review-created',method='DELETE')[0]==400
    assert call('/admin/kafka/topics/'+topic,method='DELETE')[0]==200
    eventually(lambda:not any(t['name']==topic for t in call('/admin/kafka/topics')[1]))
    assert any(g['groupId']=='sales-analytics-v1' for g in call('/admin/kafka/groups')[1])
    print('PASS: compra -> Kafka -> analítica; notificación Rabbit; administración de topics y grupos con token')

# Opción de prueba de reconstrucción: sólo actúa sobre el proceso local registrado.
if '--restart-offers' in __import__('sys').argv:
    from pathlib import Path
    import signal
    import subprocess
    root = Path(__file__).resolve().parent.parent
    pidfile = root / '.run/pids/offer-service.pid'
    pid = int(pidfile.read_text().strip())
    command = subprocess.check_output(['ps','-p',str(pid),'-o','command='],text=True)
    expected = str(root / 'services/offer-service/target/offer-service-0.0.1-SNAPSHOT.jar')
    assert expected in command, 'El PID no corresponde a este servicio'
    previous = call('/public/offers')[1]
    os.kill(pid, signal.SIGTERM)
    for _ in range(100):
        try:
            os.kill(pid,0)
        except ProcessLookupError:
            break
        time.sleep(.1)
    with (root / '.run/logs/offer-service.log').open('ab') as log:
        env = dict(os.environ,SPRING_PROFILES_ACTIVE='local')
        process = subprocess.Popen(['java','-Xmx192m','-jar',expected],cwd=root,
            stdout=log,stderr=log,stdin=subprocess.DEVNULL,start_new_session=True,env=env)
    pidfile.write_text(str(process.pid))
    def replayed():
        try:
            status, current = call('/public/offers')
            return status==200 and current==previous
        except (urllib.error.URLError, TimeoutError):
            return False
    eventually(replayed)
    print('PASS: ofertas reconstruidas desde Kafka tras reiniciar offer-service')

if '--restart-analytics' in __import__('sys').argv:
    from pathlib import Path
    import signal
    import subprocess
    root=Path(__file__).resolve().parent.parent
    pidfile=root/'.run/pids/sales-analytics-service.pid'
    pid=int(pidfile.read_text())
    jar=str(root/'services/sales-analytics-service/target/sales-analytics-service-0.0.1-SNAPSHOT.jar')
    assert jar in subprocess.check_output(['ps','-p',str(pid),'-o','command='],text=True)
    expected=call('/admin/analytics/sales')[1]
    os.kill(pid,signal.SIGTERM)
    for _ in range(100):
        try:os.kill(pid,0)
        except ProcessLookupError:break
        time.sleep(.1)
    with (root/'.run/logs/sales-analytics-service.log').open('ab') as log:
        process=subprocess.Popen(['java','-Xmx192m','-jar',jar],cwd=root,stdout=log,stderr=log,
            stdin=subprocess.DEVNULL,start_new_session=True,env=dict(os.environ,SPRING_PROFILES_ACTIVE='local'))
    pidfile.write_text(str(process.pid))
    def restored():
        try:
            status,value=call('/admin/analytics/sales')
            return status==200 and value==expected
        except (OSError,ValueError):return False
    eventually(restored)
    print('PASS: analítica persistente y consumer group estable tras reiniciar')
