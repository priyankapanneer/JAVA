/* ===== LEADERBOARD VIEW ===== */
async function renderLeaderboardView(container) {
  try {
    const res = await Api.getLeaderboard();
    const rows = res.data || [];

    const medals = ['🥇', '🥈', '🥉'];

    container.innerHTML = `
      <div class="page-header">
        <div>
          <h2>🏆 Leaderboard</h2>
          <p>Top students ranked by average score across all exams.</p>
        </div>
        <div style="display:flex;gap:8px;align-items:center">
          <span class="chip">Auto-refreshes every 30s</span>
        </div>
      </div>
      ${rows.length === 0
        ? emptyState(ICONS.trophy, 'No data yet', 'Results will appear here after students take exams.')
        : `
        <div class="leaderboard-wrap">
          ${rows.slice(0, 3).length > 0 ? `
          <div class="podium-row">
            ${rows[1] ? `<div class="podium-card podium-silver">
              <div class="podium-medal">🥈</div>
              <div class="podium-avatar">${(rows[1].fullName||'?')[0]}</div>
              <div class="podium-name">${rows[1].fullName}</div>
              <div class="podium-score">${rows[1].avgScore}%</div>
              <div class="podium-base silver-base">2nd</div>
            </div>` : '<div class="podium-card"></div>'}
            ${rows[0] ? `<div class="podium-card podium-gold">
              <div class="podium-crown">👑</div>
              <div class="podium-medal">🥇</div>
              <div class="podium-avatar gold">${(rows[0].fullName||'?')[0]}</div>
              <div class="podium-name">${rows[0].fullName}</div>
              <div class="podium-score">${rows[0].avgScore}%</div>
              <div class="podium-base gold-base">1st</div>
            </div>` : ''}
            ${rows[2] ? `<div class="podium-card podium-bronze">
              <div class="podium-medal">🥉</div>
              <div class="podium-avatar">${(rows[2].fullName||'?')[0]}</div>
              <div class="podium-name">${rows[2].fullName}</div>
              <div class="podium-score">${rows[2].avgScore}%</div>
              <div class="podium-base bronze-base">3rd</div>
            </div>` : '<div class="podium-card"></div>'}
          </div>` : ''}

          <div class="table-wrap" style="margin-top:28px">
            <table>
              <thead>
                <tr>
                  <th>Rank</th>
                  <th>Student</th>
                  <th>Avg Score</th>
                  <th>Best Score</th>
                  <th>Exams Taken</th>
                  <th>Passed</th>
                  <th>Pass Rate</th>
                </tr>
              </thead>
              <tbody>
                ${rows.map(r => {
                  const passRate = r.totalAttempts > 0 ? ((r.totalPassed / r.totalAttempts) * 100).toFixed(0) : 0;
                  const medal = r.rank <= 3 ? medals[r.rank - 1] : '';
                  return `<tr class="${r.rank <= 3 ? 'top-rank-row' : ''}">
                    <td><span class="rank-badge ${r.rank <= 3 ? 'rank-top' : ''}">${medal || '#' + r.rank}</span></td>
                    <td>
                      <div style="display:flex;align-items:center;gap:10px">
                        <div class="lb-avatar">${(r.fullName||'?')[0]}</div>
                        <div>
                          <div style="font-weight:600;color:var(--text)">${r.fullName}</div>
                          <div style="font-size:.75rem;color:var(--text-3)">@${r.username}</div>
                        </div>
                      </div>
                    </td>
                    <td>
                      <div class="score-bar-wrap">
                        <div class="score-bar-track"><div class="score-bar-fill" style="width:${r.avgScore}%"></div></div>
                        <span style="font-weight:700;color:var(--text)">${r.avgScore}%</span>
                      </div>
                    </td>
                    <td><strong style="color:var(--success)">${r.bestScore.toFixed(1)}%</strong></td>
                    <td style="color:var(--text)">${r.totalAttempts}</td>
                    <td><span class="badge badge-pass">${r.totalPassed}</span></td>
                    <td>
                      <div class="progress-bar" style="width:80px;display:inline-block">
                        <div class="progress-fill" style="width:${passRate}%"></div>
                      </div>
                      <span style="font-size:.8rem;color:var(--text-3);margin-left:6px">${passRate}%</span>
                    </td>
                  </tr>`;
                }).join('')}
              </tbody>
            </table>
          </div>
        </div>`}
    `;

    // Auto-refresh every 30 seconds
    if (currentView === 'leaderboard') {
      setTimeout(() => { if (currentView === 'leaderboard') renderLeaderboardView(container); }, 30000);
    }
  } catch (e) {
    container.innerHTML = `<div class="error-msg">Failed to load leaderboard: ${e.message}</div>`;
  }
}
