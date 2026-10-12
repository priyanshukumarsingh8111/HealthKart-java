// Landing page: home search + featured doctors & medicines showcase
(function () {
  const input = document.getElementById('homeSearch');
  const btn = document.getElementById('homeSearchBtn');
  const searchUrl = window.HK ? HK.pageUrl('search.html') : 'pages/search.html';
  const doctorsUrl = window.HK ? HK.pageUrl('doctors.html') : 'pages/doctors.html';
  const compareUrl = window.HK ? HK.pageUrl('compare.html') : 'pages/compare.html';

  const go = (q) => (location.href = searchUrl + (q ? '?q=' + encodeURIComponent(q) : ''));
  if (btn) btn.addEventListener('click', (e) => { e.preventDefault(); go(input ? input.value.trim() : ''); });
  if (input) input.addEventListener('keydown', (e) => { if (e.key === 'Enter') go(input.value.trim()); });
  document.querySelectorAll('.quick-links a').forEach((a) =>
    a.addEventListener('click', (e) => { e.preventDefault(); go(a.textContent.trim()); }));

  // Load Featured Doctors
  const docContainer = document.getElementById('featuredDoctors');
  if (docContainer && window.HK) {
    HK.api('/doctors').then((r) => {
      const topDocs = (r.doctors || []).slice(0, 3);
      docContainer.innerHTML = topDocs.map((d) => `
        <div class="card doctor-card">
          <div class="doctor-media">
            ${d.image ? `<img src="${HK.img ? HK.img(d.image) : d.image}" alt="${HK.esc(d.name)}" loading="lazy" onerror="this.style.display='none';this.nextElementSibling.style.display='flex';">` : ''}
            <div class="media-fallback" style="${d.image ? 'display:none;' : ''}"><i class="fa-solid fa-user-doctor"></i></div>
          </div>
          <span class="tag">${HK.esc(d.specialization)}</span>
          <h3>${HK.esc(d.name)}</h3>
          <p>${HK.esc(d.clinicHospital)}<br>${d.experience ? d.experience + ' yrs experience · ' : ''}<span style="color:#eab308;font-weight:700;">★</span> ${d.rating || '-'}</p>
          <div class="price">${HK.rupee(d.consultationFee)}</div>
          <a href="${doctorsUrl}?doctor=${encodeURIComponent(d.id)}" class="btn" style="display:block;text-align:center;">Book Consultation</a>
        </div>
      `).join('');
    }).catch(() => {});
  }

  // Load Popular Medicines
  const medContainer = document.getElementById('featuredMedicines');
  if (medContainer && window.HK) {
    HK.api('/medicines').then((r) => {
      const topMeds = (r.medicines || []).slice(0, 4);
      medContainer.innerHTML = topMeds.map((m) => `
        <div class="card">
          <div class="medicine-media">
            ${m.image ? `<img src="${HK.img ? HK.img(m.image) : m.image}" alt="${HK.esc(m.name)}" loading="lazy" onerror="this.style.display='none';this.nextElementSibling.style.display='flex';">` : ''}
            <div class="media-fallback" style="${m.image ? 'display:none;' : ''}"><i class="fa-solid ${HK.esc(m.icon || 'fa-pills')}"></i></div>
          </div>
          <span class="tag">${HK.esc(m.category)}</span>
          <h3>${HK.esc(m.name)}</h3>
          <p>${HK.esc(m.description)}</p>
          <div class="price">${HK.rupee(m.price)} <small style="font-size:12px;color:#eab308;font-weight:normal;">★ ${m.rating}</small></div>
          <div class="row">
            <button class="btn sm" data-add="${m.id}">Add to cart</button>
            <a href="${compareUrl}" class="btn sm ghost">Compare</a>
          </div>
        </div>
      `).join('');

      medContainer.querySelectorAll('[data-add]').forEach((b) => {
        b.onclick = () => {
          const found = topMeds.find((x) => x.id === b.dataset.add);
          if (found) HK.addToCart(found);
        };
      });
    }).catch(() => {});
  }
})();
