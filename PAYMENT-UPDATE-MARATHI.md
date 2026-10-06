# Payment tracking update — ज्योतिर्लिंग ट्रेडर्स

## चालू projectमध्ये update कसे लावायचे

1. Backend आणि frontend terminalsमध्ये Ctrl+C दाबून बंद करा.
2. तुमच्या चालू `jotirling-traders` folderची backup copy ठेवा. विशेषतः `backend/data` folder जपून ठेवा. MySQL वापरत असल्यास database backup घ्या.
3. ही update ZIP वेगळ्या folderमध्ये Extract करा.
4. त्यातील `frontend`, `backend` आणि `tools` folders तुमच्या चालू `jotirling-traders` folderमध्ये copy करा. Windowsने विचारल्यास **Replace the files in the destination** निवडा. जुन्या data folderला delete/replace करू नका. ZIPमध्ये database नाही.
5. Backendच्या terminalमध्ये, आधी demo चालवत असशील तर:
```powershell
cd backend
mvn spring-boot:run "-Dspring-boot.run.profiles=demo"
```
Normal MySQL वापरत असशील तर तुझे DB/admin environment variables कायम ठेवून `mvn spring-boot:run` कर. Profile बदलू नकोस; त्याने वेगळा database उघडू शकतो.
6. दुसऱ्या terminalमध्ये project rootपासून:
```powershell
cd frontend
npm run dev
```
नवीन npm dependency जोडलेली नाही. `Started Application` दिसू दे. Browserमध्ये Ctrl+Shift+R दाब. Demo login: admin@jotirling.local / DemoShop@123; तुझ्या existing admin credentials वेगळ्या असल्यास त्याच वापर.

## Payment नोंदवण्याची पद्धत

1. Admin → Orders उघड. Pending order आधी Confirm कर.
2. त्या orderमध्ये **Payment / पैसे मिळाल्याची नोंद** दिसेल.
3. प्रत्यक्ष मिळालेली amount, CASH / UPI / BANK, received date आणि optional note भरा.
4. UPI/Bank निवडल्यास खरा Transaction reference / UTR भरा. Reference स्वतः बनवू नका; प्रत्यक्ष transactionचा वापरा.
5. **Record received payment** दाब. ग्राहकाला app inbox notification जाते.
6. उदा. order ₹1,000, received ₹400 → PAID ₹400, BALANCE ₹600, PARTIAL. उरलेले ₹600 नोंदवल्यावर PAID आणि balance ₹0.
7. Sidebarमधील **Payments** pageवर प्रत्येक customerचे confirmed order value, भरलेले पैसे, बाकी आणि receipt history दिसेल. Customerला स्वतःचाच account दिसतो.
8. WhatsApp buttonच्या messageमध्ये order statusसोबत paid/balance दिसेल. WhatsAppमध्ये Send स्वतः दाबायचा आहे.

## योग्य अर्थ

- Order Delivered आणि payment Paid वेगवेगळे आहेत. Delivery केल्याने payment आपोआप नोंदवले जात नाही.
- Pending / cancelled / rejected orders देय रकमेत धरलेल्या नाहीत. Confirmed आणि त्यापुढील orders देय आहेत.
- Balanceपेक्षा जास्त payment स्वीकारले जात नाही. एक UPI/Bank transaction reference एका active receiptमध्येच वापरता येतो.
- एका receiptचा transaction दोन ordersमध्ये split करणे या versionमध्ये नाही. Cashसाठी reference optional आहे.
- चुकीची नोंद झाल्यास **Void incorrect entry** दाबून कारण लिहा. Amount इतिहासात VOID म्हणून राहते; balance पुन्हा मोजला जातो. हे correction आहे, प्रत्यक्ष refund नाही. योग्य payment वेगळ्या receiptमध्ये नोंदवा.
- भरलेले पैसे असलेल्या orderला Cancel करता येत नाही. Real refunds/returns या updateमध्ये नाहीत. योग्य payment लपवण्यासाठी Void वापरू नका.
- Network error आला तर history पाहा; Retry त्याच request IDने चालतो. त्याच tabमधील pending payment refresh/navigationनंतरही जपली जाते; backendवर save झालेली आढळल्यास pending request clear होते.
- ही manual received-payment नोंद आहे. कोणतेही bank/UPI पैसे transfer होत नाहीत. Print order summary tax invoice/payment receipt नाही.

## Developer verification

Backend source Java17वर compile झाला. Frontend production build पास झाला. Live HTTP checks: pending-payment/role restrictions, partial/full payment, UUID replay, transaction-reference duplicate rejection, overpayment validation, audited void, customer notice/ownership, delivery, concurrent requests आणि backend restartनंतर persistence — सर्व पास.
Production MySQL आणि physical browser/device UI या updateसाठी इथे तपासलेले नाहीत. वापरापूर्वी तुमच्या databaseवर दोन sample payments तपासा.

Optional automated HTTP regression (Python3.9+, Java17): backendमध्ये `mvn package` करा, मग project rootपासून `python tools/check-payments.py` चालवा. हे स्वतंत्र temporary H2 database आणि port18082 वापरते; वास्तविक data बदलत नाही.
