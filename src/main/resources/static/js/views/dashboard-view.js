/* ===== DASHBOARD VIEW ===== */
async function renderDashboardView(container) {
  try {
    const res = await Api.getDashboard();
    const d = res.data;
    const isAdmin = currentUser.role === 'admin';

    if (isAdmin) {
      container.innerHTML = `
      <div class="page-header">
        <div><h2>Welcome back, ${currentUser.fullName} 👋</h2><p>Here's what's happening on your platform.</p></div>
        <button class="btn-secondary" onclick="navigateTo('analytics')">${ICONS.chart} View Analytics</button>
      </div>
      <div class="stats-grid">
        <div class="stat-card purple">
          <div class="stat-icon purple">${ICONS.users}</div>
          <div class="stat-value" id="stat-students">0</div>
          <div class="stat-label">Total Students</div>
        </div>
        <div class="stat-card cyan">
          <div class="stat-icon cyan">${ICONS.exam}</div>
          <div class="stat-value" id="stat-exams">0</div>
          <div class="stat-label">Total Exams</div>
        </div>
        <div class="stat-card green">
          <div class="stat-icon green">${ICONS.results}</div>
          <div class="stat-value" id="stat-attempts">0</div>
          <div class="stat-label">Total Attempts</div>
        </div>
        <div class="stat-card orange">
          <div class="stat-icon orange">${ICONS.trophy}</div>
          <div class="stat-value" id="stat-passed">0</div>
          <div class="stat-label">Passed (Total)</div>
        </div>
      </div>
      <div class="section-title">Recent Submissions</div>
      ${d.recentResults.length === 0
        ? emptyState(ICONS.results, 'No submissions yet', 'Results will appear here once students take exams.')
        : `<div class="table-wrap">
          <table>
            <thead><tr><th>Student</th><th>Exam</th><th>Score</th><th>Status</th><th>Cheat</th><th>Date</th></tr></thead>
            <tbody>
              ${d.recentResults.map(r => `
                <tr>
                  <td><strong style="color:var(--text)">${r.fullName}</strong><br><span style="font-size:.78rem;color:var(--text-3)">@${r.username}</span></td>
                  <td style="color:var(--text)">${r.examTitle}</td>
                  <td><strong style="color:var(--text)">${r.score}/${r.totalMarks}</strong> <span style="color:var(--text-3);font-size:.8rem">(${pct(r.score,r.totalMarks)}%)</span></td>
                  <td>${badgeResult(r.passed)}</td>
                  <td>${r.cheatCount > 0 ? `<span class="badge badge-fail">⚠ ${r.cheatCount}</span>` : '<span style="color:var(--text-3);font-size:.8rem">—</span>'}</td>
                  <td style="font-size:.8rem">${formatDateTime(r.submittedAt)}</td>
                </tr>`).join('')}
            </tbody>
          </table></div>`}
      `;
      // Animate counters
      animateCounter(document.getElementById('stat-students'), d.totalStudents);
      animateCounter(document.getElementById('stat-exams'), d.totalExams);
      animateCounter(document.getElementById('stat-attempts'), d.totalAttempts);
      const totalPassed = d.recentResults.filter(r=>r.passed).length;
      animateCounter(document.getElementById('stat-passed'), totalPassed);
`;
    } else {
      container.innerHTML = `
      <div class="page-header">
        <div><h2>Hello, ${currentUser.fullName} 🎓</h2><p>Track your progress and take exams.</p></div>
        <button class="btn-primary" onclick="navigateTo('exams')">${ICONS.play} Browse Exams</button>
      </div>
      <div class="stats-grid">
        <div class="stat-card purple">
          <div class="stat-icon purple">${ICONS.exam}</div>
          <div class="stat-value">${d.totalAttempts}</div>
          <div class="stat-label">Exams Taken</div>
        </div>
        <div class="stat-card green">
          <div class="stat-icon green">${ICONS.results}</div>
          <div class="stat-value">${d.totalPassed}</div>
          <div class="stat-label">Passed</div>
        </div>
        <div class="stat-card cyan">
          <div class="stat-icon cyan">${ICONS.dashboard}</div>
          <div class="stat-value">${d.avgScore}%</div>
          <div class="stat-label">Average Score</div>
        </div>
        <div class="stat-card orange">
          <div class="stat-icon orange">${ICONS.results}</div>
          <div class="stat-value">${d.totalAttempts - d.totalPassed}</div>
          <div class="stat-label">Failed</div>
        </div>
      </div>
      <div class="section-title">Recent Results</div>
      ${d.recentResults.length === 0
        ? emptyState(ICONS.exam, 'No exams taken yet', 'Go to "Available Exams" to start your first exam.')
        : `<div class="table-wrap"><table>
            <thead><tr><th>Exam</th><th>Score</th><th>%</th><th>Status</th><th>Time Taken</th><th>Date</th><th></th></tr></thead>
            <tbody>
              ${d.recentResults.map(r => `
                <tr>
                  <td style="color:var(--text);font-weight:600">${r.examTitle}</td>
                  <td>${r.score}/${r.totalMarks}</td>
                  <td>${pct(r.score,r.totalMarks)}%</td>
                  <td>${badgeResult(r.passed)}</td>
                  <td style="font-size:.8rem">${formatDuration(r.timeTaken)}</td>
                  <td style="font-size:.8rem">${formatDateTime(r.submittedAt)}</td>
                  <td><button class="btn-icon" title="View" onclick="navigateTo('result-detail',{resultId:${r.id}})">${ICONS.eye}</button></td>
                </tr>`).join('')}
            </tbody>
          </table></div>`}`;
    }
  } catch (e) {
    container.innerHTML = `<div class="error-msg">Failed to load dashboard: ${e.message}</div>`;
  }
}
