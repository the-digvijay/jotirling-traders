from pathlib import Path
import subprocess,time,urllib.request,urllib.error,http.cookiejar,json,uuid,concurrent.futures,tempfile,sys,os
project=Path(__file__).resolve().parents[1]
jar=Path(sys.argv[1]).resolve() if len(sys.argv)>1 else project/'backend/target/traders-1.0.0.jar'
if not jar.is_file():raise SystemExit('Build backend first: cd backend; mvn package')
temporary=tempfile.TemporaryDirectory(prefix='jotirling-payment-check-')
base=Path(temporary.name)
env={**os.environ,'ADMIN_EMAIL':'admin@jotirling.local','ADMIN_PASSWORD':'DemoShop@123','FRONTEND_ORIGIN':'http://localhost:5175','COOKIE_SECURE':'false','VAPID_PUBLIC_KEY':'','VAPID_PRIVATE_KEY':''}
log=open(base/'server.log','w')
p=subprocess.Popen(['java','-jar',str(jar),'--spring.profiles.active=demo','--spring.datasource.url=jdbc:h2:file:'+str(base/'test-data')+';MODE=MySQL','--server.port=18082'],cwd=base,stdout=log,stderr=subprocess.STDOUT,env=env)
def client():return urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
def req(c,path,body=None,method=None,status=200):
 h={'X-App-Request':'1','Origin':'http://localhost:5175'}
 if body is not None:h['Content-Type']='application/json'
 r=urllib.request.Request('http://localhost:18082/api'+path,data=json.dumps(body).encode() if body is not None else None,method=method or ('POST' if body is not None else 'GET'),headers=h)
 try:
  with c.open(r,timeout=12) as response:b=response.read();assert response.status==status;return json.loads(b) if b else None
 except urllib.error.HTTPError as e:
  assert e.code==status,(e.code,status,e.read().decode());return None
checks=[]
def passed(label):checks.append(label);print('PASS: '+label,flush=True)
def receipt(amount,method='CASH',reference='',requestId=None):return {'amount':str(amount),'method':method,'reference':reference,'notes':'HTTP test','receivedDate':'2026-01-01','requestId':requestId or str(uuid.uuid4())}
try:
 pub=client()
 for _ in range(160):
  try:
   catalog=req(pub,'/products')
   if _ in (20,80):print("Startup catalogue count:",len(catalog),flush=True)
   if len(catalog)>=18:break
  except Exception as err:
   if _ in (20,80):print("Startup error:",str(err)[:500],flush=True)
   if p.poll() is not None:raise RuntimeError('Server failed; see server.log')
  time.sleep(.25)
 else:raise RuntimeError('Startup timeout')
 a=client();c=client();x=client();req(a,'/auth/login',{'email':'admin@jotirling.local','password':'DemoShop@123'})
 for cl,label in [(c,'Customer'),(x,'Stranger')]:req(cl,'/auth/register',{'name':label,'email':str(uuid.uuid4())+'@example.com','phone':'9876543210','password':'Customer@123'})
 prod=req(a,'/admin/products',{'sku':str(uuid.uuid4()),'name':'Payment test','category':'WELDING','variant':'','unit':'box','description':'test','price':100,'lowStock':1,'active':True})
 req(a,'/admin/purchases',{'productId':prod['id'],'supplier':'Test supplier','invoiceReference':'Test','quantity':10,'unitCost':50,'receivedDate':'2026-01-01','requestId':str(uuid.uuid4())})
 def order():return req(c,'/orders',{'requestId':str(uuid.uuid4()),'fulfilment':'PICKUP','address':'','notes':'','items':[{'productId':prod['id'],'quantity':1}]})['id']
 def state(id,status):return req(a,'/admin/orders/'+str(id),{'status':status,'notes':''},'PATCH')
 id=order();url=f'/admin/orders/{id}/payments'
 req(a,url,receipt(40),status=400);req(c,url,receipt(40),status=403);state(id,'CONFIRMED');passed('Pending payments and customer writes rejected')
 cash=receipt(40);pay=req(a,url,cash);assert pay['paid']==40 and pay['balance']==60 and pay['status']=='PARTIAL';assert len(pay['history'])==1
 pay=req(a,url,cash);assert len(pay['history'])==1
 req(a,url,{**cash,'amount':'41'},status=409);passed('Partial cash payment, correct balance and idempotent replay')
 req(a,url,receipt(61),status=400);req(a,url,receipt(0),status=400);req(a,url,receipt(10,'UPI',''),status=400)
 req(a,url,{**receipt(1),'receivedDate':'2099-01-01'},status=400);stateurl=f'/admin/orders/{id}';req(a,stateurl,{'status':'CANCELLED','notes':''},'PATCH',400);passed('Overpayment, invalid amount/date/reference and paid cancellation rejected')
 ref='UTR-'+str(uuid.uuid4());pay=req(a,url,receipt(60,'UPI',ref));assert pay['status']=='PAID' and pay['balance']==0
 notice=req(c,'/notifications');assert any('Payment recorded' in n['title'] and n['orderId']==id for n in notice)
 req(x,f'/orders/{id}',status=403);req(x,url,receipt(1),status=403);passed('Full UPI payment, customer notice and account isolation')
 second=order();state(second,'CONFIRMED');req(a,f'/admin/orders/{second}/payments',receipt(60,'UPI',ref),status=409);passed('Duplicate non-cash transaction reference rejected')
 pid=pay['history'][0]['id'];voidurl=url+f'/{pid}/void';pay=req(a,voidurl,{'reason':'Wrong entry'});assert pay['paid']==60 and pay['balance']==40 and pay['history'][0]['voided']
 pay=req(a,voidurl,{'reason':'Retry'});assert len(pay['history'])==2;req(a,f'/admin/orders/{second}/payments/{pid}/void',{'reason':'Wrong order'},status=400)
 req(c,voidurl,{'reason':'Not admin'},status=403);pay=req(a,url,receipt(40));assert pay['status']=='PAID' and len(pay['history'])==3;passed('Audited void, revised balance, correction and role checks')
 state(id,'READY');done=state(id,'DELIVERED');assert done['payment']['paid']==100 and done['payment']['balance']==0;passed('Delivery preserves payment history')
 third=order();state(third,'CONFIRMED');u=f'/admin/orders/{third}/payments'
 def simultaneous(_):
  try:req(a,u,receipt(60));return 200
  except AssertionError as e:
   assert e.args[0][0]==400;return 400
 with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:results=list(pool.map(simultaneous,range(2)))
 assert sorted(results)==[200,400];v=req(c,f'/orders/{third}')['payment'];assert v['paid']==60 and v['balance']==40 and len(v['history'])==1;passed('Concurrent payment requests cannot overpay')
 # Restart against the same database: recorded history must survive.
 p.terminate();p.wait(timeout=8)
 p=subprocess.Popen(['java','-jar',str(jar),'--spring.profiles.active=demo','--spring.datasource.url=jdbc:h2:file:'+str(base/'test-data')+';MODE=MySQL','--server.port=18082'],cwd=base,stdout=log,stderr=subprocess.STDOUT,env=env)
 for _ in range(160):
  try:
   v=req(c,f'/orders/{id}')['payment']
   if v['paid']==100 and len(v['history'])==3:break
  except Exception:time.sleep(.25)
 else:raise RuntimeError('Restart persistence failed')
 passed('Payments persist after backend restart')
 (base/'result.txt').write_text('\n'.join(checks)+'\nAll live HTTP checks passed.\n')
 print('ALL PAYMENT HTTP CHECKS PASSED',flush=True)
finally:
 if p.poll() is None:
  p.terminate()
  try:p.wait(timeout=8)
  except subprocess.TimeoutExpired:p.kill();p.wait()
 log.close()
 if sys.exc_info()[0]:print((base/'server.log').read_text()[-3000:])
 temporary.cleanup()
