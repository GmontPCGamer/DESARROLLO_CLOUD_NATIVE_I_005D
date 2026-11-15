#!/usr/bin/env python3
"""Alinea rutas públicas, CORS y redirects Entra usando sesiones CLI existentes."""
import json
import os
import subprocess
import tempfile
from pathlib import Path

REGION=os.environ.get('AWS_REGION','us-east-1')
API=os.environ.get('NEXOTECH_API_ID','ourd5f7qr1')
INSTANCE=os.environ.get('INSTANCE_ID','i-0e3ea93394a16686d')
CLIENT=os.environ.get('AZURE_CLIENT_ID','f7d7e5dd-430c-4adb-9348-9ecd974b220c')
def aws(*args):
    result=subprocess.check_output(['aws',*args,'--region',REGION,'--output','json'],text=True)
    return json.loads(result) if result.strip() else None

def run():
    api=aws('apigatewayv2','get-api','--api-id',API)
    origin=api['ApiEndpoint']
    ip=aws('ec2','describe-instances','--instance-ids',INSTANCE)['Reservations'][0]['Instances'][0]['PublicIpAddress']
    integrations=aws('apigatewayv2','get-integrations','--api-id',API).get('Items',[])
    routes=aws('apigatewayv2','get-routes','--api-id',API).get('Items',[])
    for prefix,method in [('web','GET')]:
        for suffix,uri_suffix in [('/{proxy+}','/{proxy}'),('','/index.html' if prefix=='web' else '/')]:
            uri='http://'+ip+':80/'+prefix+uri_suffix
            integration=next((i for i in integrations if i.get('IntegrationUri')==uri),None)
            if integration is None:
                integration=aws('apigatewayv2','create-integration','--api-id',API,'--integration-type','HTTP_PROXY',
                    '--integration-method',method,'--integration-uri',uri,'--payload-format-version','1.0')
                integrations.append(integration)
            key=method+' /'+prefix+suffix
            if not any(r['RouteKey']==key for r in routes):
                aws('apigatewayv2','create-route','--api-id',API,'--route-key',key,
                    '--target','integrations/'+integration['IntegrationId'],'--authorization-type','NONE')
    cors=api.get('CorsConfiguration',{})
    cors['AllowOrigins']=list(dict.fromkeys([x for x in cors.get('AllowOrigins',[]) if not x.endswith('.trycloudflare.com')]+[origin,'http://localhost:4200']))
    cors.update(AllowMethods=['GET','POST','PUT','PATCH','DELETE','OPTIONS'],AllowHeaders=['authorization','content-type'],AllowCredentials=False,MaxAge=3600)
    aws('apigatewayv2','update-api','--api-id',API,'--cors-configuration',json.dumps(cors))
    app=json.loads(subprocess.check_output(['az','ad','app','show','--id',CLIENT,'--output','json'],text=True))
    redirects=app.get('spa',{}).get('redirectUris',[])
    callback=origin+'/dev/web/auth'
    if callback not in redirects:redirects.append(callback)
    payload={'spa':{'redirectUris':redirects},'web':{'logoutUrl':origin+'/dev/web/'}}
    with tempfile.TemporaryDirectory(prefix='nexotech-entra-') as directory:
        body=Path(directory)/'redirects.json';body.write_text(json.dumps(payload))
        subprocess.run(['az','rest','--method','patch','--url','https://graph.microsoft.com/v1.0/applications/'+app['id'],
            '--body','@'+str(body),'--only-show-errors'],check=True)
    print(json.dumps({'web':origin+'/dev/web/'},ensure_ascii=False))
if __name__=='__main__':run()
