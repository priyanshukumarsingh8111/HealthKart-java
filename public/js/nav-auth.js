// Landing page: show the logged-in user in the navbar
(function () {
  function updateNav() {
    const u = window.HK && HK.user();
    const btn = document.querySelector('.login-btn');
    if (!btn) return;
    if (u) {
      const displayName = HK.esc((u.name || 'User').split(' ')[0]);
      const isDoc = u.role === 'doctor';
      const rolePrefix = isDoc ? '🩺 Dr. ' : '👤 ';
      const targetUrl = isDoc
        ? (window.HK ? HK.pageUrl('doctor-dashboard.html') : 'pages/doctor-dashboard.html')
        : (window.HK ? HK.pageUrl('my-health.html') : 'pages/my-health.html');
      
      const parent = btn.parentElement;
      if (parent) {
        btn.outerHTML = `
          <div style="display:inline-flex;align-items:center;gap:8px;">
            <a href="${targetUrl}" class="login-btn" style="background:#0b9f6e;color:white;display:inline-flex;align-items:center;gap:6px;">
              ${rolePrefix}${displayName}
            </a>
            <a href="#" id="homeLogoutBtn" class="btn sm ghost" title="Logout" style="padding:10px 12px;border-radius:9px;color:#0b9f6e;border:1px solid #b9dfd0;background:white;">
              <i class="fa-solid fa-power-off"></i>
            </a>
          </div>
        `;
        const lb = document.getElementById('homeLogoutBtn');
        if (lb) lb.addEventListener('click', (e) => { e.preventDefault(); HK.logout(); });
      }
    }
    if (window.HK && HK.updateCartCount) HK.updateCartCount();
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', updateNav);
  } else {
    updateNav();
  }
})();
