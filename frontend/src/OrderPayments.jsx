import React,{useState} from 'react';
import {money} from './api';
import {openReceipt} from './documents';
const eligible=status=>['CONFIRMED','DELIVERED'].includes(status);
export default function OrderPayments({order}){
 const [message,M]=useState('');
 const p=order.payment||{paid:0,balance:0,status:'NOT_DUE',history:[]};
 return <section className="paymentbox" style={{borderTop:'1px solid #ddd',paddingTop:16,marginTop:16}}>
 <h3>Payment / भरलेले आणि बाकी</h3><div className="formrow"><p>Order रक्कम: <strong>{money(order.total)}</strong></p><p>जमा झालेले: <strong>{money(p.paid)}</strong></p><p>बाकी: <strong>{money(p.balance)}</strong></p></div>
 {p.status==='NOT_DUE'&&<p>Pending / cancelled / rejected orderची रक्कम देय खात्यात धरलेली नाही.</p>}
 {message&&<p role="status">{message}</p>}
 {p.history.length>0?<div className="tablewrap"><table><thead><tr><th>तारीख / Receipt</th><th>UPI / Reference</th><th>रक्कम</th><th>Status</th><th>Receipt</th></tr></thead><tbody>{p.history.map(r=><tr key={r.id}><td>{r.receivedDate}<small>Receipt #{r.id}</small></td><td>{r.method}<small>{r.reference}</small></td><td>{money(r.amount)}</td><td>{r.voided?'VOID':'Verified / जमा'}</td><td><button className="btn outline" onClick={()=>{try{openReceipt(order,r);}catch(e){M(e.message);}}}>Receipt / Save PDF</button></td></tr>)}</tbody></table></div>:<p>अजून verified payment नाही.</p>}
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
   <button className="btn gold" onClick={onOpenOrders}>Ordersमध्ये भरलेले व बाकी पाहा →</button>
  </section>
  <section className="panel"><h3>Payment history</h3>{documentError&&<p className="alert error" role="alert">{documentError}</p>}<div className="tablewrap"><table><thead><tr><th>Receipt / order</th><th>Date</th>{admin&&<th>Customer</th>}<th>Method / reference</th><th>Amount</th><th>Status</th><th>Receipt</th></tr></thead><tbody>{receipts.map(p=><tr key={p.id}><td>#{p.id} · Order #{p.orderId}</td><td>{p.receivedDate}</td>{admin&&<td>{p.customer}</td>}<td>{p.method}<small>{p.reference||'—'}</small></td><td>{money(p.amount)}</td><td>{p.voided?'VOID':'Recorded'}</td><td><button className="btn outline" onClick={()=>{try{setDocumentError('');openReceipt(orders.find(o=>o.id===p.orderId),p);}catch(e){setDocumentError(e.message);}}}>Receipt / Save PDF</button></td></tr>)}</tbody></table></div>{!receipts.length&&<p className="muted">No payment entries yet.</p>}</section>
 </>;
}
