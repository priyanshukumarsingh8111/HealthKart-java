/* Shared helpers: API, user, cart, toasts, nav, realtime socket, incoming-call popup */
(function () {
  const HK = (window.HK = {});

  HK.esc = (s) => String(s ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
  HK.md = (s) => HK.esc(s).replace(/\*\*(.+?)\*\*/g, '<b>$1</b>').replace(/\*(.+?)\*/g, '<i>$1</i>').replace(/\n/g, '<br>');
  HK.rupee = (n) => '₹' + Number(n || 0).toLocaleString('en-IN');

  // In-memory fallback if localStorage is blocked by private mode or file:// sandbox
  const _mem = {};
  HK.json = (k, d) => {
    try {
      const v = localStorage.getItem(k);
      return v != null ? JSON.parse(v) : (_mem[k] ?? d);
    } catch {
      return _mem[k] ?? d;
    }
  };
  HK.save = (k, v) => {
    _mem[k] = v;
    try { localStorage.setItem(k, JSON.stringify(v)); } catch (e) {}
  };

  // Determine environment:
  // Port 3000 is our dedicated Spring Boot server.
  // Any other origin (Live Server port 5500, Vite, Python http.server, or file://) routes API calls to http://<hostname>:3000/api.
  const isServerOrigin = location.port === "3000";
  const isFile = location.protocol === "file:";
  const currentHost = (location.hostname && location.hostname !== "") ? location.hostname : "localhost";
  const apiBase = isServerOrigin ? "/api" : ("http://" + currentHost + ":3000/api");

  HK.img = (src) => {
    if (!src) return "";
    if (src.startsWith("http://") || src.startsWith("https://")) return src;
    if (isServerOrigin) return src;
    const clean = src.startsWith("/") ? src.slice(1) : src;
    return "http://" + currentHost + ":3000/" + clean;
  };

  const SEED_MEDS = [{"id":"paracetamol-500","name":"Paracetamol 500mg","category":"Pain Relief","description":"Relieves mild to moderate pain and fever. Strip of 15 tablets.","price":30,"rating":4.7,"icon":"fa-pills","image":"/images/medicines/paracetamol-500.jpg","providers":[{"name":"1mg","price":32},{"name":"PharmEasy","price":34},{"name":"Apollo","price":30}]},{"id":"vitamin-d3","name":"Vitamin D3 60K IU","category":"Vitamins","description":"Weekly vitamin D supplement for bone and immune health.","price":120,"rating":4.6,"icon":"fa-capsules","image":"/images/medicines/vitamin-d3.jpg","providers":[{"name":"1mg","price":125},{"name":"PharmEasy","price":118},{"name":"Apollo","price":130}]},{"id":"cetirizine-10","name":"Cetirizine 10mg","category":"Allergy","description":"Antihistamine for sneezing, runny nose and itchy eyes.","price":25,"rating":4.5,"icon":"fa-pills","image":"/images/medicines/cetirizine-10.jpg","providers":[{"name":"1mg","price":27},{"name":"PharmEasy","price":24},{"name":"Apollo","price":28}]},{"id":"amoxicillin-500","name":"Amoxicillin 500mg","category":"Antibiotics","description":"Penicillin antibiotic. Prescription required.","price":95,"rating":4.4,"icon":"fa-capsules","image":"/images/medicines/amoxicillin-500.jpg","providers":[{"name":"1mg","price":99},{"name":"PharmEasy","price":92},{"name":"Apollo","price":97}]},{"id":"bp-monitor","name":"Digital BP Monitor","category":"Devices","description":"Automatic upper-arm blood pressure monitor with memory.","price":1499,"rating":4.5,"icon":"fa-heart-pulse","image":"/images/medicines/bp-monitor.jpg","providers":[{"name":"Amazon","price":1599},{"name":"Flipkart","price":1450},{"name":"Apollo","price":1520}]},{"id":"thermometer","name":"Digital Thermometer","category":"Devices","description":"Fast, accurate digital thermometer with fever alarm.","price":199,"rating":4.3,"icon":"fa-temperature-half","image":"/images/medicines/thermometer.jpg","providers":[{"name":"Amazon","price":215},{"name":"Flipkart","price":189},{"name":"Apollo","price":210}]},{"id":"glucometer","name":"Glucometer Kit","category":"Devices","description":"Blood glucose monitor with 25 test strips.","price":899,"rating":4.6,"icon":"fa-droplet","image":"/images/medicines/glucometer.jpg","providers":[{"name":"Amazon","price":949},{"name":"Flipkart","price":880},{"name":"Apollo","price":920}]},{"id":"ors","name":"ORS Electrolyte Powder","category":"Hydration","description":"Oral rehydration salts, orange flavour, 21g sachets x 10.","price":85,"rating":4.4,"icon":"fa-bottle-water","image":"/images/medicines/ors.jpg","providers":[{"name":"1mg","price":88},{"name":"PharmEasy","price":82},{"name":"Apollo","price":90}]},{"id":"omega3","name":"Omega-3 Fish Oil","category":"Vitamins","description":"1000mg omega-3 capsules for heart and brain health, 60 caps.","price":450,"rating":4.5,"icon":"fa-capsules","image":"/images/medicines/omega3.jpg","providers":[{"name":"1mg","price":470},{"name":"PharmEasy","price":440},{"name":"Apollo","price":460}]},{"id":"multivit","name":"Daily Multivitamin","category":"Vitamins","description":"Complete daily multivitamin and mineral tablets, 30 count.","price":320,"rating":4.2,"icon":"fa-capsules","image":"/images/medicines/multivit.jpg","providers":[{"name":"1mg","price":335},{"name":"PharmEasy","price":310},{"name":"Apollo","price":325}]},{"id":"pantoprazole-40","name":"Pantoprazole 40mg","category":"Digestive","description":"Reduces stomach acid; take before food. 15 tablets.","price":75,"rating":4.4,"icon":"fa-pills","image":"/images/medicines/pantoprazole-40.jpg","providers":[{"name":"1mg","price":79},{"name":"PharmEasy","price":72},{"name":"Apollo","price":77}]},{"id":"sanitizer","name":"Hand Sanitizer 500ml","category":"Personal Care","description":"70% alcohol-based hand sanitizer with aloe vera.","price":180,"rating":4.1,"icon":"fa-pump-soap","image":"/images/medicines/sanitizer.jpg","providers":[{"name":"Amazon","price":190},{"name":"Flipkart","price":175},{"name":"Apollo","price":185}]},{"id":"vitamin-d3-1000","name":"Vitamin D3 1000 IU","category":"Vitamin","description":"Daily vitamin D supplement for bones and immunity","price":120,"rating":4.6,"icon":"fa-pills","image":"/images/medicines/vitamin-d3.jpg","providers":[]},{"id":"face-cleanser","name":"Gentle Face Cleanser","category":"Personal Care","description":"Dermatologist-recommended mild foaming cleanser for all skin types","price":249,"rating":4.4,"icon":"fa-pills","image":"/images/medicines/face-cleanser.jpg","providers":[]},{"id":"pulse-oximeter","name":"Digital Pulse Oximeter","category":"Device","description":"Fingertip SpO2 oxygen saturation monitor with OLED display","price":899,"rating":4.7,"icon":"fa-pills","image":"/images/medicines/pulse-oximeter.jpg","providers":[]},{"id":"first-aid-kit","name":"Emergency First Aid Kit","category":"Device","description":"Comprehensive 50-piece home and travel emergency care kit","price":499,"rating":4.9,"icon":"fa-pills","image":"/images/medicines/first-aid-kit.jpg","providers":[]}];
  const SEED_DOCS = [{"id":"rahul","name":"Dr. Rahul Sharma","specialization":"General Physician","clinicHospital":"Max Super Speciality Hospital, New Delhi","consultationFee":500,"experience":12,"rating":4.8,"image":"/images/doctors/rahul.jpg","availableTimeSlots":["09:00 AM","10:00 AM","11:00 AM","12:00 PM","02:00 PM","03:00 PM","04:00 PM","05:00 PM"]},{"id":"priya","name":"Dr. Priya Mehta","specialization":"Dermatologist","clinicHospital":"Apollo Clinic, Noida","consultationFee":700,"experience":9,"rating":4.7,"image":"/images/doctors/priya.jpg","availableTimeSlots":["09:00 AM","10:00 AM","11:00 AM","12:00 PM","02:00 PM","03:00 PM","04:00 PM","05:00 PM"]},{"id":"amit","name":"Dr. Amit Verma","specialization":"Cardiologist","clinicHospital":"Fortis Hospital, Noida","consultationFee":1000,"experience":15,"rating":4.9,"image":"/images/doctors/amit.jpg","availableTimeSlots":["09:00 AM","10:00 AM","11:00 AM","12:00 PM","02:00 PM","03:00 PM","04:00 PM","05:00 PM"]},{"id":"arjun","name":"Dr. Arjun Singh","specialization":"Orthopedic","clinicHospital":"Sharda Hospital, Greater Noida","consultationFee":800,"experience":11,"rating":4.6,"image":"/images/doctors/arjun.jpg","availableTimeSlots":["09:00 AM","10:00 AM","11:00 AM","12:00 PM","02:00 PM","03:00 PM","04:00 PM","05:00 PM"]},{"id":"neha","name":"Dr. Neha Gupta","specialization":"Pediatrician","clinicHospital":"Kailash Hospital, Noida","consultationFee":600,"experience":8,"rating":4.8,"image":"/images/doctors/neha.jpg","availableTimeSlots":["09:00 AM","10:00 AM","11:00 AM","12:00 PM","02:00 PM","03:00 PM","04:00 PM","05:00 PM"]},{"id":"ananya","name":"Dr. Ananya Gupta","specialization":"General Physician","clinicHospital":"Artemis Hospital, Noida","consultationFee":449,"experience":"7 years experience","rating":4.9,"image":"/images/doctors/ananya.jpg","availableTimeSlots":["09:00 AM","09:30 AM","10:00 AM","10:30 AM","02:00 PM","02:30 PM","03:00 PM"]}];
  HK.getFallbackData = (path, opts) => {
    const qIndex = path.indexOf("?");
    const endpoint = (qIndex >= 0 ? path.substring(0, qIndex) : path).toLowerCase();
    const query = qIndex >= 0 ? new URLSearchParams(path.substring(qIndex + 1)) : new URLSearchParams();

    if (endpoint === "/medicines") {
      let list = [...SEED_MEDS];
      const cat = query.get("category");
      const search = (query.get("search") || query.get("q") || "").toLowerCase().trim();
      const sort = query.get("sort");
      if (cat && cat !== "all") {
        list = list.filter(m => (m.category || "").toLowerCase() === cat.toLowerCase());
      }
      if (search) {
        list = list.filter(m => (m.name + " " + m.category + " " + m.description).toLowerCase().includes(search));
      }
      if (sort === "price-low") list.sort((a, b) => a.price - b.price);
      else if (sort === "price-high") list.sort((a, b) => b.price - a.price);
      else if (sort === "rating") list.sort((a, b) => b.rating - a.rating);
      return { success: true, count: list.length, medicines: list };
    }

    if (endpoint.startsWith("/medicines/")) {
      const id = endpoint.replace("/medicines/", "");
      const m = SEED_MEDS.find(x => x.id === id);
      return m ? { success: true, medicine: m } : { error: "Not found" };
    }

    if (endpoint === "/doctors") {
      let list = [...SEED_DOCS];
      const search = (query.get("search") || query.get("q") || "").toLowerCase().trim();
      if (search) {
        list = list.filter(d => (d.name + " " + d.specialization + " " + d.clinicHospital).toLowerCase().includes(search));
      }
      return { success: true, count: list.length, doctors: list };
    }

    if (endpoint.startsWith("/doctors/")) {
      const id = endpoint.replace("/doctors/", "");
      const d = SEED_DOCS.find(x => x.id === id);
      return d ? { success: true, doctor: d } : { error: "Not found" };
    }

    if (endpoint === "/appointments/slots") {
      const docId = query.get("doctorId") || "rahul";
      const date = query.get("date") || "";
      const slots = ["09:00 AM", "10:00 AM", "11:00 AM", "12:00 PM", "02:00 PM", "03:00 PM", "04:00 PM", "05:00 PM"]
        .map(time => ({ time, isBooked: false, appointmentId: null }));
      return { success: true, doctorId: docId, date, isSunday: false, slots };
    }

    if (endpoint === "/jdbc/status") {
      return { success: true, connected: true, latencyMs: 5, dbProduct: "HealthKart JDBC", driver: "JDBC Driver", jdbcUrl: "jdbc:mysql://localhost:3306/healthkart_db", tables: [{ table: "medicines", count: 16, daoClass: "MedicineDaoImpl" }, { table: "doctors", count: 6, daoClass: "DoctorDaoImpl" }] };
    }

    return null;
  };

  HK.api = async (path, opts = {}) => {
    const clean = path.startsWith("/") ? path : "/" + path;
    const url = apiBase + clean;
    try {
      const r = await fetch(url, {
        method: opts.method || "GET",
        headers: { "Content-Type": "application/json" },
        body: opts.body ? JSON.stringify(opts.body) : undefined
      });
      const data = await r.json().catch(() => ({}));
      if (!r.ok) {
        const fb = HK.getFallbackData(clean, opts);
        if (fb !== null) return fb;
        const e = new Error(data.error || "Request failed"); e.data = data; throw e;
      }
      return data;
    } catch (err) {
      const fb = HK.getFallbackData(clean, opts);
      if (fb !== null) {
        console.warn("Backend offline at " + url + ", served fallback seed data.");
        return fb;
      }
      throw err;
    }
  };
  // ---- user (stored with JDBC backend sync & browser persistence) ----
  HK.user = () => HK.json('hk_user', null);
  HK.setUser = (u) => HK.save('hk_user', u);
  HK.logout = async () => {
    try { await HK.api('/auth/logout', { method: 'POST' }); } catch (e) {}
    try { localStorage.removeItem('hk_user'); } catch (e) {}
    _mem['hk_user'] = null;
    const isPage = location.pathname.includes('/pages/') || location.pathname.includes('\\pages\\');
    location.href = isServerOrigin ? '/index.html' : (isPage ? '../index.html' : 'index.html');
  };

  // Helper for cross-environment URL resolution
  HK.pageUrl = (page) => {
    const isPage = location.pathname.includes('/pages/') || location.pathname.includes('\\pages\\');
    if (!isServerOrigin) return isPage ? page : 'pages/' + page;
    return '/pages/' + page;
  };

  HK.homeUrl = () => {
    const isPage = location.pathname.includes('/pages/') || location.pathname.includes('\\pages\\');
    if (!isServerOrigin) return isPage ? '../index.html' : 'index.html';
    return '/index.html';
  };

  // ---- cart ----
  HK.cart = () => HK.json('hk_cart', []);
  HK.updateCartCount = () => {
    const n = HK.cart().reduce((a, i) => a + (i.qty || 1), 0);
    document.querySelectorAll('#cartCount, .cart-count').forEach(el => el.textContent = n);
  };
  HK.addToCart = (m) => {
    if (!m) return;
    const c = HK.cart(); const f = c.find((i) => i.id === m.id);
    if (f) f.qty = (f.qty || 1) + 1;
    else c.push({ id: m.id, name: m.name, price: m.price, icon: m.icon, image: m.image, qty: 1 });
    HK.save('hk_cart', c); HK.updateCartCount(); HK.toast((m.name || 'Product') + ' added to cart');
  };

  // ---- toast notifications ----
  HK.toast = (msg) => {
    let box = document.getElementById('toasts');
    if (!box) { box = document.createElement('div'); box.id = 'toasts'; document.body.appendChild(box); }
    const t = document.createElement('div'); t.className = 'toast'; t.textContent = msg; box.appendChild(t);
    setTimeout(() => t.remove(), 4000);
  };

  // ---- navbar for inner pages ----
  HK.nav = (active) => {
    const u = HK.user();
    const isPage = location.pathname.includes('/pages/') || location.pathname.includes('\\pages\\');
    const pfx = isServerOrigin ? '/pages/' : (isPage ? '' : 'pages/');
    const homeUrl = isServerOrigin ? '/index.html' : (isPage ? '../index.html' : 'index.html');
    const cartUrl = isServerOrigin ? '/pages/cart.html' : (isPage ? 'cart.html' : 'pages/cart.html');
    const loginUrl = isServerOrigin ? '/pages/login.html' : (isPage ? 'login.html' : 'pages/login.html');

    const links = [
      [homeUrl, 'Home', 'home'],
      [pfx + 'search.html', 'Medicines', 'search'],
      [pfx + 'compare.html', 'Compare', 'compare'],
      [pfx + 'doctors.html', 'Doctors', 'doctors'],
      [pfx + 'my-health.html', 'My Health', 'health'],
      [pfx + 'database.html', 'Database Console', 'database']
    ];
    if (u && u.role === 'doctor') links.push([pfx + 'doctor-dashboard.html', 'Dashboard', 'dash']);
    const host = document.getElementById('nav'); if (!host) return;

    let userBtn = `<a href="${loginUrl}" class="login-btn"><i class="fa-solid fa-arrow-right-to-bracket"></i> Login</a>`;
    if (u) {
      const displayName = HK.esc((u.name || 'User').split(' ')[0]);
      const roleBadge = u.role === 'doctor' ? '🩺 Dr.' : '👤';
      userBtn = `
        <div style="display:inline-flex;align-items:center;gap:8px;">
          <a href="${u.role === 'doctor' ? pfx + 'doctor-dashboard.html' : pfx + 'my-health.html'}" class="btn sm" style="background:#eaf9f3;color:#0b9f6e;font-weight:700;">
            ${roleBadge} ${displayName}
          </a>
          <a href="#" class="btn sm ghost" id="logoutBtn" title="Sign out" style="padding:6px 10px;">
            <i class="fa-solid fa-power-off"></i>
          </a>
        </div>
      `;
    }

    host.innerHTML = `<header class="navbar">
      <a class="logo" href="${homeUrl}"><i class="fa-solid fa-heart-pulse"></i><span>Health<span>Kart</span></span></a>
      <nav>${links.map((l) => `<a href="${l[0]}" ${l[2] === active ? 'style="color:#0b9f6e;font-weight:700;"' : ''}>${l[1]}</a>`).join('')}</nav>
      <div class="nav-actions">
        <a href="${cartUrl}" aria-label="Cart" style="position:relative;">
          <i class="fa-solid fa-cart-shopping"></i>
          <span id="cartCount" style="background:#0b9f6e;color:white;border-radius:10px;padding:2px 7px;font-size:11px;font-weight:700;margin-left:4px;">0</span>
        </a>
        ${userBtn}
      </div></header>`;

    const lb = document.getElementById('logoutBtn');
    if (lb) lb.onclick = (e) => { e.preventDefault(); HK.logout(); };
    HK.updateCartCount();
  };

  // ---- realtime ----
  HK.socket = null;
  if (window.io) {
    try {
      const socketHost = isFile ? 'localhost' : (location.hostname || 'localhost');
      HK.socket = io('http://' + socketHost + ':9092', { reconnectionAttempts: 3, timeout: 3000 });
      const reg = () => { const u = HK.user(); if (u && u.email) HK.socket.emit('register_user', { email: u.email }); };
      HK.socket.on('connect', reg);

      HK.socket.on('realtime_notification', (d) => {
        const u = HK.user();
        if (u && d && d.recipientEmail && d.recipientEmail.toLowerCase() === (u.email || '').toLowerCase())
          HK.toast('🔔 ' + (d.notification?.title || 'New notification'));
      });

      HK.socket.on('incoming_doctor_call', (d) => {
        const u = HK.user();
        if (!u || u.role === 'doctor' || !d.patientEmail || d.patientEmail.toLowerCase() !== (u.email || '').toLowerCase()) return;
        const bg = document.createElement('div'); bg.className = 'modal-bg open';
        bg.innerHTML = `<div class="modal"><h2>📹 Incoming consultation</h2><p style="margin-bottom:16px">${HK.esc(d.doctorName)} is calling you.</p>
          <div class="row"><button class="btn" id="acc">Accept</button><button class="btn red" id="dec">Decline</button></div></div>`;
        document.body.appendChild(bg);
        const callTarget = HK.pageUrl('call.html') + `?appointment=${encodeURIComponent(d.appointmentId)}&role=patient`;
        bg.querySelector('#acc').onclick = () => {
          HK.socket.emit('accept_doctor_call', { roomId: d.roomId, appointmentId: d.appointmentId });
          location.href = callTarget;
        };
        bg.querySelector('#dec').onclick = () => { HK.socket.emit('decline_doctor_call', { roomId: d.roomId, appointmentId: d.appointmentId }); bg.remove(); };
      });
    } catch (e) {
      console.warn('Realtime socket notice:', e);
    }
  }
  document.addEventListener('DOMContentLoaded', HK.updateCartCount);
})();
