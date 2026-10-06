import React, {useRef, useState, useEffect} from 'react';
import {api, money, today} from './api';
import {openReceipt} from './documents';
const eligible = status => ['CONFIRMED','PREPARING','READY','DISPATCHED','DELIVERED'].includes(status);
const empty = () => ({amount:'',method:'CASH',reference:'',notes:'',receivedDate:today()});
export default function OrderPayments({order,admin,accountId,busy,run,refresh}) {
 const storageKey=`jt_pending_payment_${accountId}_${order.id}`;
 const [restored]=useState(()=>{try{return JSON.parse(sessionStorage.getItem(storageKey)||'null');}catch{return null;}});
 const [form,setForm]=useState(()=>restored||empty()),[locked,setLocked]=useState(!!restored),[message,setMessage]=useState('');
 const pending=useRef(restored);
 function remember(payload){try{if(payload)sessionStorage.setItem(storageKey,JSON.stringify(payload));else sessionStorage.removeItem(storageKey);}catch{}}

 const payment=order.payment||{paid:0,balance:0,status:'NOT_DUE',history:[]};
 useEffect(()=>{if(pending.current&&payment.history.some(p=>p.requestId===pending.current.requestId)){
 pending.current=null;remember(null);setLocked(false);setForm(empty());setMessage('Payment saved. Historyमध्ये नोंद आहे.');
 }},[payment.history]);
 async function record(e) {
  e.preventDefault();if(busy)return;
  if(!pending.current){pending.current={...form,amount:form.amount,requestId:crypto.randomUUID()};remember(pending.current);}
  setLocked(true);setMessage('');let failure;
  const result=await run(async()=>{try{return await api.post(`/admin/orders/${order.id}/payments`,pending.current);}catch(err){failure=err;throw err;}},'Payment recorded; customer inbox updated');
  if(result){pending.current=null;remember(null);setLocked(false);setForm(empty());await refresh();}
  else if(failure?.response?.status>=400&&failure?.response?.status<500){pending.current=null;remember(null);setLocked(false);setMessage('Payment स्वीकारले नाही. तपशील तपासून पुन्हा नोंदवा.');}
  else setMessage('Result uncertain. History तपासा; Retry त्याच requestची पुनरावृत्ती करेल.');
 }
 async function voidEntry(p){
  if(busy||locked)return;
  const reason=window.prompt('फक्त चुकीची payment नोंद रद्द करा. याने प्रत्यक्ष refund होत नाही. कारण लिहा:');
  if(!reason?.trim())return;
  if(reason.trim().length>500){setMessage('कारण 500 अक्षरांपेक्षा कमी लिहा.');return;}
  const result=await run(()=>api.post(`/admin/orders/${order.id}/payments/${p.id}/void`,{reason:reason.trim()}),'Incorrect entry voided; balance updated');
  if(result)await refresh();
 }
 return <section className="paymentbox" style={{borderTop:'1px solid #ddd',paddingTop:16,marginTop:16}}>
  <div className="sectiontitle"><h3>Payment / पैसे मिळाल्याची नोंद</h3><span className="status">{payment.status.replaceAll('_',' ')}</span></div>
  <div className="formrow"><p>भरलेले: <strong>{money(payment.paid)}</strong></p><p>बाकी: <strong>{money(payment.balance)}</strong></p></div>
  {payment.status==='NOT_DUE'&&<p className="muted">Pending / cancelled / rejected orderची रक्कम देय खात्यात धरलेली नाही.</p>}
  {admin&&eligible(order.status)&&Number(payment.balance)>0&&<form className="no-print" onSubmit={record}>
   <p className="muted">प्रत्यक्ष पैसे मिळाल्याची खात्री झाल्यावरच नोंदवा. ही नोंद आहे; पैसे transfer होत नाहीत.</p>
   <fieldset disabled={busy||locked}><div className="formrow">
    <label className="field"><span>मिळालेली रक्कम ₹</span><input required type="number" min="0.01" max={payment.balance} step="0.01" value={form.amount} onChange={e=>setForm({...form,amount:e.target.value})}/></label>
    <label className="field"><span>Payment method</span><select value={form.method} onChange={e=>setForm({...form,method:e.target.value,reference:''})}><option value="CASH">Cash</option><option value="UPI">UPI</option><option value="BANK">Bank transfer</option></select></label>
    <label className="field"><span>{form.method==='CASH'?'Receipt reference — optional':'Transaction reference / UTR'}</span><input required={form.method!=='CASH'} maxLength={100} value={form.reference} onChange={e=>setForm({...form,reference:e.target.value})}/></label>
   </div><div className="formrow">
    <label className="field"><span>पैसे मिळाल्याची तारीख</span><input required type="date" max={today()} value={form.receivedDate} onChange={e=>setForm({...form,receivedDate:e.target.value})}/></label>
    <label className="field"><span>Notes — optional</span><input maxLength={500} value={form.notes} onChange={e=>setForm({...form,notes:e.target.value})}/></label>
   </div></fieldset>
   <button className="btn gold" disabled={busy}>{locked?'Retry same payment':'Record received payment'}</button>
  </form>}
  {message&&<p className="muted" role="status">{message}</p>}
  {payment.history.length>0&&<div className="tablewrap"><table><thead><tr><th>Date / receipt</th><th>Method / reference</th><th>Amount</th><th>Entry</th><th className="no-print">Receipt</th>{admin&&<th className="no-print"></th>}</tr></thead><tbody>{payment.history.map(p=><tr key={p.id}>
   <td>{p.receivedDate}<small>Receipt #{p.id} · {p.recordedBy}</small>{p.notes&&<small>{p.notes}</small>}</td>
   <td>{p.method}<small>{p.reference||'—'}</small></td><td>{money(p.amount)}</td>
   <td>{p.voided?<><strong>VOID</strong><small>{p.voidReason}</small><small>{p.voidedBy} · {new Date(p.voidedAt).toLocaleString('en-IN')}</small></>: 'Recorded'}</td>
   <td className="no-print"><button className="btn outline" onClick={()=>{try{openReceipt(order,p);}catch(e){setMessage(e.message);}}}>{p.voided?'Void record / PDF':'Receipt / Save PDF'}</button></td>
   {admin&&<td className="no-print">{!p.voided&&<button className="textbtn" disabled={busy||locked} onClick={()=>voidEntry(p)}>Void incorrect entry</button>}</td>}
  </tr>)}</tbody></table></div>}
  {!payment.history.length&&<p className="muted">अजून payment नोंदवलेली नाही.</p>}
 </section>;
}
export function PaymentLedger({orders,admin,onOpenOrders}) {
 const [documentError,setDocumentError]=useState('');
 const rows=orders.filter(o=>eligible(o.status));const groups=new Map();
 const cents=n=>Math.round(Number(n||0)*100);
 for(const o of rows){const g=groups.get(o.customerId)||{id:o.customerId,name:o.customerName,phone:o.customerPhone,total:0,paid:0,balance:0,count:0};g.total+=cents(o.total);g.paid+=cents(o.payment?.paid);g.balance+=cents(o.payment?.balance);g.count++;groups.set(g.id,g);}
 const totals=[...groups.values()].reduce((a,g)=>({total:a.total+g.total,paid:a.paid+g.paid,balance:a.balance+g.balance}),{total:0,paid:0,balance:0});
 const receipts=orders.flatMap(o=>(o.payment?.history||[]).map(p=>({...p,orderId:o.id,customer:o.customerName}))).sort((a,b)=>b.id-a.id);
 return <>
  <div className="stats">{[['Confirmed order value',totals.total],['Payments received',totals.paid],['Outstanding / बाकी',totals.balance]].map(([label,value])=><section className="stat" key={label}><small>{label}</small><strong>{money(value/100)}</strong></section>)}</div>
  <section className="panel"><h2>{admin?'Customer उधारी / बाकी खाते':'माझी बाकी रक्कम'}</h2><p className="muted">Confirmed आणि पुढील statusesमधील orders धरल्या आहेत. Pending / cancelled / rejected धरलेल्या नाहीत. Void entries received totalमध्ये धरलेल्या नाहीत.</p>
   <div className="tablewrap"><table><thead><tr><th>Customer</th><th>Orders</th><th>Order value</th><th>Paid</th><th>Balance</th></tr></thead><tbody>{[...groups.values()].map(g=><tr key={g.id}><td>{g.name}{admin&&<small>{g.phone}</small>}</td><td>{g.count}</td><td>{money(g.total/100)}</td><td>{money(g.paid/100)}</td><td><strong>{money(g.balance/100)}</strong></td></tr>)}</tbody></table></div>
   {!groups.size&&<p className="muted">अजून confirmed orders नाहीत.</p>}
   <button className="btn gold" onClick={onOpenOrders}>Ordersमध्ये payment नोंद / तपशील पाहा →</button>
  </section>
  <section className="panel"><h3>Payment history</h3>{documentError&&<p className="alert error" role="alert">{documentError}</p>}<div className="tablewrap"><table><thead><tr><th>Receipt / order</th><th>Date</th>{admin&&<th>Customer</th>}<th>Method / reference</th><th>Amount</th><th>Status</th><th>Receipt</th></tr></thead><tbody>{receipts.map(p=><tr key={p.id}><td>#{p.id} · Order #{p.orderId}</td><td>{p.receivedDate}</td>{admin&&<td>{p.customer}</td>}<td>{p.method}<small>{p.reference||'—'}</small></td><td>{money(p.amount)}</td><td>{p.voided?'VOID':'Recorded'}</td><td><button className="btn outline" onClick={()=>{try{setDocumentError('');openReceipt(orders.find(o=>o.id===p.orderId),p);}catch(e){setDocumentError(e.message);}}}>Receipt / Save PDF</button></td></tr>)}</tbody></table></div>{!receipts.length&&<p className="muted">No payment entries yet.</p>}</section>
 </>;
}
