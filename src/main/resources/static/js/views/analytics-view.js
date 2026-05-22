/* ===== ANALYTICS VIEW (Admin Only) ===== */
async function renderAnalyticsView(container) {
  if (currentUser.role !== 'admin') {
    container.innerHTML = `<div class="error-msg">Admin access required.</div>`;
    return;
  }

  try {
    const res = await Api.getAnalytics();
    const d = res.data;
    const stats = d.examStats || [];

    container.innerHTML = `
      <div class="page-header">
        <div><h2>📊 Analytics</h2><p>Platform-wide performance insights and exam statistics.</p></div>
        <button class="btn-secondary" onclick="downloadCsv('/api/export/results')">
          ${ICONS.download} Export Results CSV
        </button>
      </div>

      <div class="stats-grid" style="margin-bottom:32px">
        <div class="stat-card purple">
          <div class="stat-icon purple">${ICONS.users}</div>
          <div class="stat-value" id="sc-students">0</div>
          <div class="stat-label">Total Students</div>
        </div>
        <div class="stat-card cyan">
          <div class="stat-icon cyan">${ICONS.exam}</div>
          <div class="stat-value" id="sc-exams">0</div>
          <div class="stat-label">Total Exams</div>
        </div>
        <div class="stat-card green">
          <div class="stat-icon green">${ICONS.results}</div>
          <div class="stat-value" id="sc-attempts">0</div>
          <div class="stat-label">Total Attempts</div>
        </div>
        <div class="stat-card orange">
          <div class="stat-icon orange">${ICONS.chart}</div>
          <div class="stat-value">${stats.length > 0 ? (stats.reduce((s,e) => s + (e.totalAttempts > 0 ? e.totalPassed/e.totalAttempts*100 : 0), 0) / Math.max(stats.length,1)).toFixed(1) : 0}%</div>
          <div class="stat-label">Overall Pass Rate</div>
        </div>
      </div>

      ${stats.length === 0 ? emptyState(ICONS.chart, 'No data yet', 'Analytics will appear after students take exams.') : `
      <div style="display:grid;grid-template-columns:1fr 1fr;gap:24px;margin-bottom:28px" class="analytics-chart-grid">
        <div class="card">
          <div class="card-title">Pass vs Fail by Exam</div>
          <div class="card-sub" style="margin-bottom:16px">How many students passed each exam</div>
          <canvas id="chart-passrate" height="220"></canvas>
        </div>
        <div class="card">
          <div class="card-title">Average Score by Exam</div>
          <div class="card-sub" style="margin-bottom:16px">Mean percentage score per exam</div>
          <canvas id="chart-avgscore" height="220"></canvas>
        </div>
      </div>

      <div class="card" style="margin-bottom:28px">
        <div class="card-title" style="margin-bottom:16px">Exam Performance Breakdown</div>
        <div class="table-wrap">
          <table>
            <thead><tr><th>Exam</th><th>Attempts</th><th>Passed</th><th>Failed</th><th>Avg Score</th><th>Avg Time</th></tr></thead>
            <tbody>
              ${stats.map(e => `
                <tr>
                  <td style="font-weight:600;color:var(--text)">${e.title}</td>
                  <td>${e.totalAttempts}</td>
                  <td><span class="badge badge-pass">${e.totalPassed}</span></td>
                  <td><span class="badge badge-fail">${e.totalFailed}</span></td>
                  <td>
                    <div style="display:flex;align-items:center;gap:8px">
                      <div class="progress-bar" style="width:70px"><div class="progress-fill" style="width:${e.avgScore}%"></div></div>
                      <strong style="color:var(--text)">${e.avgScore}%</strong>
                    </div>
                  </td>
                  <td style="font-size:.85rem;color:var(--text-3)">${Math.floor(e.avgTime/60)}m ${e.avgTime%60}s</td>
                </tr>`).join('')}
            </tbody>
          </table>
        </div>
      </div>`}
    `;

    // Animate counters
    animateCounter(document.getElementById('sc-students'), d.totalStudents);
    animateCounter(document.getElementById('sc-exams'), d.totalExams);
    animateCounter(document.getElementById('sc-attempts'), d.totalAttempts);

    // Render charts if data exists
    if (stats.length > 0) {
      setTimeout(() => renderCharts(stats), 100);
    }

  } catch (e) {
    container.innerHTML = `<div class="error-msg">Failed to load analytics: ${e.message}</div>`;
  }
}

function renderCharts(stats) {
  if (typeof Chart === 'undefined') return;

  const labels = stats.map(e => e.title.length > 18 ? e.title.slice(0,18)+'…' : e.title);
  const chartDefaults = {
    plugins: { legend: { labels: { color: '#A8A3C8', font: { family: 'Inter' } } } },
    scales: {
      x: { ticks: { color: '#6B658F' }, grid: { color: 'rgba(255,255,255,0.05)' } },
      y: { ticks: { color: '#6B658F' }, grid: { color: 'rgba(255,255,255,0.05)' } }
    }
  };

  // Pass vs Fail chart
  const ctx1 = document.getElementById('chart-passrate')?.getContext('2d');
  if (ctx1) new Chart(ctx1, {
    type: 'bar',
    data: {
      labels,
      datasets: [
        { label: 'Passed', data: stats.map(e => e.totalPassed), backgroundColor: 'rgba(16,185,129,0.7)', borderColor: '#10B981', borderWidth: 1, borderRadius: 6 },
        { label: 'Failed', data: stats.map(e => e.totalFailed), backgroundColor: 'rgba(239,68,68,0.7)', borderColor: '#EF4444', borderWidth: 1, borderRadius: 6 }
      ]
    },
    options: { responsive: true, ...chartDefaults }
  });

  // Avg score chart
  const ctx2 = document.getElementById('chart-avgscore')?.getContext('2d');
  if (ctx2) new Chart(ctx2, {
    type: 'bar',
    data: {
      labels,
      datasets: [{
        label: 'Avg Score (%)',
        data: stats.map(e => e.avgScore),
        backgroundColor: stats.map(e => e.avgScore >= 70 ? 'rgba(108,71,255,0.7)' : 'rgba(245,158,11,0.7)'),
        borderColor: stats.map(e => e.avgScore >= 70 ? '#6C47FF' : '#F59E0B'),
        borderWidth: 1,
        borderRadius: 6
      }]
    },
    options: { responsive: true, ...chartDefaults, scales: { ...chartDefaults.scales, y: { ...chartDefaults.scales.y, max: 100 } } }
  });
}
