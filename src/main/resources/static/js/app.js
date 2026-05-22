/* ===== MAIN APP CONTROLLER ===== */

let currentUser = null;
let currentView = null;

async function initApp() {
  const token = localStorage.getItem('ep_token');
  if (!token) { window.location.href = '/index.html'; return; }

  try {
    const res = await Api.getMe();
    currentUser = res.data;
    localStorage.setItem('ep_user', JSON.stringify(currentUser));
    renderSidebar();
    renderTopbar();
    navigateTo('dashboard');
  } catch (e) {
    localStorage.clear();
    window.location.href = '/index.html';
  }
}

function renderSidebar() {
  const isAdmin = currentUser.role === 'admin';
  document.getElementById('sidebar-avatar').textContent = (currentUser.fullName || 'U')[0].toUpperCase();
  document.getElementById('sidebar-name').textContent = currentUser.fullName || currentUser.username;
  document.getElementById('sidebar-role').textContent = currentUser.role;

  const nav = document.getElementById('sidebar-nav');
  const adminItems = `
    <div class="nav-section">
      <div class="nav-section-label">Admin</div>
      <button class="nav-item" data-view="dashboard" onclick="navigateTo('dashboard')">
        ${ICONS.dashboard} Dashboard <span class="nav-indicator"></span>
      </button>
      <button class="nav-item" data-view="exams" onclick="navigateTo('exams')">
        ${ICONS.exam} Manage Exams <span class="nav-indicator"></span>
      </button>
      <button class="nav-item" data-view="results" onclick="navigateTo('results')">
        ${ICONS.results} All Results <span class="nav-indicator"></span>
      </button>
      <button class="nav-item" data-view="users" onclick="navigateTo('users')">
        ${ICONS.users} Students <span class="nav-indicator"></span>
      </button>
    </div>`;
  const studentItems = `
    <div class="nav-section">
      <div class="nav-section-label">Student</div>
      <button class="nav-item" data-view="dashboard" onclick="navigateTo('dashboard')">
        ${ICONS.dashboard} Dashboard <span class="nav-indicator"></span>
      </button>
      <button class="nav-item" data-view="exams" onclick="navigateTo('exams')">
        ${ICONS.exam} Available Exams <span class="nav-indicator"></span>
      </button>
      <button class="nav-item" data-view="results" onclick="navigateTo('results')">
        ${ICONS.results} My Results <span class="nav-indicator"></span>
      </button>
    </div>`;
  nav.innerHTML = isAdmin ? adminItems : studentItems;
}

function renderTopbar() {
  const badge = document.getElementById('topbar-role-badge');
  badge.textContent = currentUser.role === 'admin' ? '⚡ Admin' : '🎓 Student';
  badge.style.background = currentUser.role === 'admin'
    ? 'rgba(168,85,247,0.15)' : 'rgba(6,182,212,0.15)';
  badge.style.color = currentUser.role === 'admin' ? '#A855F7' : '#06B6D4';
  badge.style.borderColor = currentUser.role === 'admin'
    ? 'rgba(168,85,247,0.3)' : 'rgba(6,182,212,0.3)';
}

function navigateTo(view, params = {}) {
  currentView = view;

  // Update sidebar active state
  document.querySelectorAll('.nav-item').forEach(item => {
    item.classList.toggle('active', item.dataset.view === view);
  });

  // Update page title
  const titles = { dashboard:'Dashboard', exams: currentUser?.role === 'admin' ? 'Manage Exams' : 'Available Exams',
    results: currentUser?.role === 'admin' ? 'All Results' : 'My Results',
    users:'Students', 'exam-take':'Take Exam', 'result-detail':'Result Detail' };
  document.getElementById('page-title').textContent = titles[view] || view;

  // Close mobile sidebar
  document.getElementById('sidebar').classList.remove('open');
  document.getElementById('sidebar-overlay').classList.remove('open');

  // Render view
  const pageView = document.getElementById('page-view');
  pageView.innerHTML = `<div class="loading-spinner-wrap"><div class="spinner"></div><p>Loading...</p></div>`;

  const views = {
    dashboard:     () => renderDashboardView(pageView),
    exams:         () => renderExamsView(pageView, currentUser),
    results:       () => renderResultsView(pageView, currentUser),
    users:         () => renderUsersView(pageView),
    'exam-take':   () => renderExamTakeView(pageView, params.examId, currentUser),
    'result-detail': () => renderResultDetailView(pageView, params.resultId),
  };

  (views[view] || (() => { pageView.innerHTML = '<p>View not found.</p>'; }))();
}

async function handleLogout() {
  try { await Api.logout(); } catch (_) {}
  localStorage.clear();
  window.location.href = '/index.html';
}

// Bootstrap
document.addEventListener('DOMContentLoaded', initApp);
