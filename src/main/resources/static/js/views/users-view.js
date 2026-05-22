/* ===== USERS VIEW (Admin only) ===== */
async function renderUsersView(container) {
  if (currentUser.role !== 'admin') {
    container.innerHTML = `<div class="error-msg">Access denied.</div>`;
    return;
  }
  try {
    const res = await Api.getUsers();
    const users = res.data;
    const students = users.filter(u => u.role === 'student');
    const admins = users.filter(u => u.role === 'admin');

    container.innerHTML = `
      <div class="page-header">
        <div><h2>Manage Students</h2><p>${students.length} student(s) · ${admins.length} admin(s)</p></div>
      </div>
      <div class="stats-grid" style="margin-bottom:24px">
        <div class="stat-card cyan">
          <div class="stat-icon cyan">${ICONS.users}</div>
          <div class="stat-value">${students.length}</div>
          <div class="stat-label">Total Students</div>
        </div>
        <div class="stat-card purple">
          <div class="stat-icon purple">${ICONS.users}</div>
          <div class="stat-value">${admins.length}</div>
          <div class="stat-label">Admins</div>
        </div>
        <div class="stat-card green">
          <div class="stat-icon green">${ICONS.users}</div>
          <div class="stat-value">${users.length}</div>
          <div class="stat-label">Total Users</div>
        </div>
      </div>
      ${users.length === 0
        ? emptyState(ICONS.users,'No users found','')
        : `<div class="table-wrap"><table>
            <thead><tr><th>#</th><th>Name</th><th>Username</th><th>Email</th><th>Role</th><th>Joined</th><th>Actions</th></tr></thead>
            <tbody>
              ${users.map((u, i) => `
                <tr>
                  <td style="color:var(--text-3)">${i + 1}</td>
                  <td><strong style="color:var(--text)">${u.fullName}</strong></td>
                  <td style="font-family:monospace;font-size:.85rem">@${u.username}</td>
                  <td style="font-size:.85rem;color:var(--text-3)">${u.email}</td>
                  <td>${badgeRole(u.role)}</td>
                  <td style="font-size:.78rem">${formatDate(u.createdAt)}</td>
                  <td>
                    ${u.role !== 'admin'
                      ? `<button class="btn-danger btn-sm" onclick="deleteUser(${u.id},'${escHtml(u.fullName)}')">${ICONS.trash} Remove</button>`
                      : `<span style="font-size:.78rem;color:var(--text-3)">Protected</span>`}
                  </td>
                </tr>`).join('')}
            </tbody>
          </table></div>`}`;
  } catch (e) {
    container.innerHTML = `<div class="error-msg">Failed to load users: ${e.message}</div>`;
  }
}

function deleteUser(userId, name) {
  confirmAction(`Remove student "<strong>${name}</strong>"? This cannot be undone.`, async () => {
    try {
      await Api.deleteUser(userId);
      showToast('User removed', 'success');
      navigateTo('users');
    } catch (e) { showToast('Error: '+e.message, 'error'); }
  });
}

function escHtml(s) { return (s||'').replace(/'/g,"\\'").replace(/"/g,'&quot;'); }
