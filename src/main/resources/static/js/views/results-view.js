/* ===== RESULTS VIEW ===== */
async function renderResultsView(container, user) {
  try {
    const res = await Api.getResults();
    const results = res.data;
    const isAdmin = user.role === 'admin';

    container.innerHTML = `
      <div class="page-header">
        <div><h2>${isAdmin ? 'All Results' : 'My Results'}</h2>
          <p>${results.length} result(s)</p></div>
        ${isAdmin ? `<button class="btn-secondary" onclick="downloadCsv('/api/export/results')">${ICONS.download} Export CSV</button>` : ''}
      </div>
        : `<div class="table-wrap"><table>
          <thead><tr>
            ${isAdmin ? '<th>Student</th>' : ''}
            <th>Exam</th><th>Score</th><th>%</th><th>Status</th><th>Time Taken</th>${isAdmin ? '<th>Cheat Warnings</th>' : ''}<th>Date</th><th></th>
          </tr></thead>
          <tbody>
            ${results.map(r => `
              <tr>
                ${isAdmin ? `<td><strong style="color:var(--text)">${r.fullName}</strong><br><span style="font-size:.75rem;color:var(--text-3)">@${r.username}</span></td>` : ''}
                <td style="color:var(--text);font-weight:600">${r.examTitle}</td>
                <td><strong>${r.score}</strong>/${r.totalMarks}</td>
                <td>${pct(r.score, r.totalMarks)}%</td>
                <td>${badgeResult(r.passed)}</td>
                <td style="font-size:.82rem">${formatDuration(r.timeTaken)}</td>
                ${isAdmin ? `<td>${r.cheatCount > 0 ? `<span class="badge badge-fail">⚠ ${r.cheatCount}</span>` : '<span style="color:var(--text-3);font-size:.8rem">Clean</span>'}</td>` : ''}
                <td style="font-size:.78rem">${formatDateTime(r.submittedAt)}</td>
                <td><button class="btn-icon" title="View Detail" onclick="navigateTo('result-detail',{resultId:${r.id}})">${ICONS.eye}</button></td>
              </tr>`).join('')}
          </tbody></table></div>`}`;
  } catch (e) {
    container.innerHTML = `<div class="error-msg">Failed to load results: ${e.message}</div>`;
  }
}

/* ===== RESULT DETAIL VIEW ===== */
async function renderResultDetailView(container, resultId) {
  try {
    const res = await Api.getResult(resultId);
    const r = res.data;
    const percentage = r.totalMarks > 0 ? (r.score / r.totalMarks * 100).toFixed(1) : 0;

    container.innerHTML = `
      <div class="page-header">
        <div><h2>Exam Result</h2><p>${r.examTitle}</p></div>
        <button class="btn-secondary" onclick="navigateTo('results')">&larr; Back to Results</button>
      </div>

      <div style="display:grid;grid-template-columns:1fr 2fr;gap:20px;margin-bottom:28px;flex-wrap:wrap">
        <!-- Score Card -->
        <div class="card" style="text-align:center;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:12px">
          <div class="result-score-circle ${r.passed?'pass':'fail'}">
            <div class="score-pct" style="color:${r.passed?'#10B981':'#EF4444'}">${percentage}%</div>
            <div class="score-label">${r.passed?'PASSED':'FAILED'}</div>
          </div>
          <div style="font-size:1.5rem;font-weight:800;color:var(--text)">${r.score} / ${r.totalMarks}</div>
          <div style="color:var(--text-3);font-size:.85rem">Passing marks: ${r.passingMarks}</div>
          ${badgeResult(r.passed)}
        </div>

        <!-- Details -->
        <div class="card">
          <div class="card-title" style="margin-bottom:16px">Result Details</div>
          <div style="display:grid;grid-template-columns:1fr 1fr;gap:14px">
            ${detailRow('Student', r.fullName)}
            ${detailRow('Exam', r.examTitle)}
            ${detailRow('Correct Answers', `${r.correctAnswers} / ${r.totalQuestions}`)}
            ${detailRow('Wrong Answers', r.totalQuestions - r.correctAnswers)}
            ${detailRow('Time Taken', formatDuration(r.timeTaken))}
            ${detailRow('Submitted', formatDateTime(r.submittedAt))}
          </div>
          <div style="margin-top:18px">
            <div style="font-size:.78rem;color:var(--text-3);margin-bottom:6px;font-weight:600">SCORE BREAKDOWN</div>
            <div class="progress-bar">
              <div class="progress-fill" style="width:${percentage}%;background:linear-gradient(90deg,${r.passed?'#10B981':'#EF4444'},${r.passed?'#34D399':'#F87171'})"></div>
            </div>
            <div style="display:flex;justify-content:space-between;font-size:.75rem;color:var(--text-3);margin-top:4px">
              <span>0</span><span style="color:${r.passed?'#10B981':'#EF4444'};font-weight:600">${percentage}%</span><span>100%</span>
            </div>
          </div>
        </div>
      </div>`;
  } catch (e) {
    container.innerHTML = `<div class="error-msg">Failed to load result: ${e.message}</div>`;
  }
}

function detailRow(label, value) {
  return `<div>
    <div style="font-size:.74rem;font-weight:600;color:var(--text-3);text-transform:uppercase;letter-spacing:.04em;margin-bottom:4px">${label}</div>
    <div style="font-size:.95rem;color:var(--text);font-weight:500">${value}</div>
  </div>`;
}
