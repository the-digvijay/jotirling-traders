# ज्योतिर्लिंग ट्रेडर्स — आधी हे वाचा

ही React + Spring Boot app आहे. Mobile-friendly आहे आणि supported browser मध्ये install करता येते. ZIP extract करा; ZIPच्या आतून run करू नका.

## 1. पहिल्यांदा local trial

PC वर Java 17, Maven आणि Node.js 20+ installed हवेत. VS Code मध्ये `jotirling-traders` folder उघडा. Terminal मध्ये `java -version`, `mvn -version`, `node -v` चालले पाहिजेत.

पहिला terminal:

```powershell
cd backend
mvn spring-boot:run "-Dspring-boot.run.profiles=demo"
```

`Started Application` दिसल्यावर terminal चालूच ठेवा.

दुसरा terminal — projectच्या main folderपासून:

```powershell
cd frontend
npm install
npm run dev
```

Browser मध्ये http://localhost:5175 उघडा.

**दुकानदाराचे trial login:**

- Email: `admin@jotirling.local`
- Password: `DemoShop@123`

पहिल्या trialमध्ये bannerवर आधारित 18 sample products आहेत. त्यांचे prices, sizes आणि stock उदाहरणे आहेत; दुकानाचे वास्तविक rates समजू नका. Trial database PCवर `backend/data/` मध्ये टिकतो. पुन्हा run केल्यावर orders राहतात.

## 2. स्वतः पूर्ण flow तपासा

1. साध्या browser windowमध्ये admin login ठेवा.
2. Incognito / Private windowमध्ये त्याच URLवर `Sign in / नोंदणी` → Register करा. हा स्वतंत्र customer आहे; स्वतः email आणि किमान 10 अक्षरांचा password ठेवा.
3. Customer catalogueमध्ये product → Add to cart → Cart → Send order.
4. Admin windowमध्ये Orders उघडा. Order आणि Notificationsमध्ये message साधारण 10 secondsमध्ये दिसतो; Refresh लगेच करू शकता.
5. Admin `Confirm / order book करा` दाबा. Customerला CONFIRMED status आणि notification दिसते.
6. Admin READY करा. Pickup order असेल तर DELIVERED; delivery order असेल तर DISPATCHED → DELIVERED.
7. Admin Productsमध्ये stock पाहा. Confirmवेळी reserve, deliveredवेळी actual stock कमी.
8. Purchasesमध्ये product, supplier, invoice reference, quantity, cost, date भरून Receive करा. Stock वाढतो.
9. Notificationsमध्ये `Enable alert sound` ऐच्छिक. Browser notificationsसाठी permission द्या. VAPID नसल्यास बंद appवर push येणार नाही.

## 3. वास्तविक MySQL database वापरा

Trial backend बंद करा. MySQL चालू ठेवा. `backend` terminalमध्ये:

```powershell
$env:DB_USERNAME="root"
$env:DB_PASSWORD="TUMCHA_MYSQL_PASSWORD"
$env:ADMIN_EMAIL="TUMCHA_ADMIN_EMAIL"
$env:ADMIN_PASSWORD="TUMCHA_UNIQUE_LONG_PASSWORD"
mvn spring-boot:run
```

वरील placeholder values स्वतः बदला. Admin password किमान 10 अक्षरे असावा. `jotirling_traders` database तयार होतो; permission नसेल तर MySQLमध्ये `CREATE DATABASE jotirling_traders;` करा. `.env` आपोआप Javaमध्ये load होत नाही; terminalचे environment variables वापरा.

या normal modeमध्ये sample products नसतात. Admin login → Productsमध्ये वास्तविक items/rates भरा → Purchasesमध्ये वास्तविक received stock नोंदवा. Passwords GitHubवर upload करू नका. Admin account पहिल्या startupवेळी बनतो; नंतर environment password बदलल्याने existing accountचा password बदलत नाही.

## 4. दुकान आणि customerचे वेगळ्या ठिकाणचे फोन

`localhost` link फक्त त्या PCवर चालतो. सर्वांना वापरता यावे म्हणून frontend आणि backend online host करावे लागतात. Phone app बंद असताना pushसाठी HTTPS, VAPID keys आणि notification permission लागते. `DEPLOYMENT.md` मध्ये configuration दिले आहे. हा ZIP online deployment केलेला नाही.

## समस्या

- Backend connection error: पहिला terminal चालू आहे का आणि port8082 आहे का पाहा.
- Frontend: port5175 वापरा. दुसऱ्या frontend appचा port5175 असेल तर ती बंद करा.
- MySQL `Access denied`: DB username/password तपासा. Demoसाठी MySQL लागत नाही.
- Maven command missing: Maven install करून VS Code पुन्हा उघडा.
- Admin password incorrect: trialमध्ये वरील exact password; normalमध्ये पहिल्यांदा दिलेला password. Demo आणि MySQL हे वेगळे databases आहेत.

ही पहिली functional आवृत्ती आहे: online payment, GST tax invoice आणि WhatsApp/SMS integration यात नाहीत. Print हे order summary आहे. पुढील बदल दुकानाच्या actual कामानुसार करू शकतो.
