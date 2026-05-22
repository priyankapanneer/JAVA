/* ===== EXAMS VIEW ===== */
async function renderExamsView(container, user) {
  const isAdmin = user.role === 'admin';
  try {
    const res = isAdmin ? await Api.getExams() : await Api.getActiveExams();
    const exams = res.data;

    if (isAdmin) {
      container.innerHTML = `
        <div class="page-header">
          <div><h2>Manage Exams</h2><p>${exams.length} exam(s) total</p></div>
          <button class="btn-primary" id="create-exam-btn" onclick="showCreateExamModal()">${ICONS.plus} Create Exam</button>
        </div>
        <div class="exam-grid" id="exam-grid">
          ${exams.length === 0 ? emptyState(ICONS.exam,'No exams yet','Create your first exam using the button above.') : exams.map(adminExamCard).join('')}
        </div>`;
    } else {
      container.innerHTML = `
        <div class="page-header">
          <div><h2>Available Exams</h2><p>${exams.length} active exam(s) available</p></div>
        </div>
        <div class="exam-grid" id="exam-grid">
          ${exams.length === 0 ? emptyState(ICONS.exam,'No exams available','Check back later for available exams.') : exams.map(studentExamCard).join('')}
        </div>`;
    }
  } catch (e) {
    container.innerHTML = `<div class="error-msg">Failed to load exams: ${e.message}</div>`;
  }
}

function adminExamCard(e) {
  return `<div class="exam-card" id="exam-card-${e.id}">
    <div class="exam-card-header">
      <div class="exam-card-title">${e.title}</div>
      ${badgeStatus(e.status)}
    </div>
    <div class="exam-card-desc">${e.description || 'No description provided.'}</div>
    <div class="exam-meta">
      <span class="exam-meta-item">${ICONS.timer} ${e.duration} min</span>
      <span class="exam-meta-item">${ICONS.exam} ${e.questionCount} Qs</span>
      <span class="exam-meta-item">🏆 ${e.totalMarks} marks</span>
      <span class="exam-meta-item">✅ Pass: ${e.passingMarks}</span>
    </div>
    <div class="exam-card-actions">
      <button class="btn-secondary btn-sm" onclick="showManageQuestionsModal(${e.id},'${escHtml(e.title)}')">${ICONS.edit} Questions</button>
      <button class="btn-secondary btn-sm" onclick="showEditExamModal(${e.id})">${ICONS.edit} Edit</button>
      <button class="btn-secondary btn-sm" onclick="viewExamResults(${e.id},'${escHtml(e.title)}')">${ICONS.eye} Results</button>
      <button class="btn-danger btn-sm" onclick="deleteExam(${e.id})">${ICONS.trash}</button>
    </div>
    <div style="font-size:.75rem;color:var(--text-3)">Created ${formatDate(e.createdAt)} by ${e.createdByName}</div>
  </div>`;
}

function studentExamCard(e) {
  return `<div class="exam-card">
    <div class="exam-card-header">
      <div class="exam-card-title">${e.title}</div>
      ${badgeStatus(e.status)}
    </div>
    <div class="exam-card-desc">${e.description || 'No description provided.'}</div>
    <div class="exam-meta">
      <span class="exam-meta-item">${ICONS.timer} ${e.duration} min</span>
      <span class="exam-meta-item">${ICONS.exam} ${e.questionCount} Questions</span>
      <span class="exam-meta-item">🏆 ${e.totalMarks} marks</span>
      <span class="exam-meta-item">✅ Pass: ${e.passingMarks} marks</span>
    </div>
    <div class="exam-card-actions">
      <button class="btn-primary" onclick="navigateTo('exam-take',{examId:${e.id}})">${ICONS.play} Start Exam</button>
    </div>
  </div>`;
}

function escHtml(s) { return (s||'').replace(/'/g,"\\'").replace(/"/g,'&quot;'); }

/* CREATE EXAM MODAL */
function showCreateExamModal() {
  openModal('Create New Exam', `
    <div class="form-group"><label class="form-label">Title *</label>
      <input type="text" id="ex-title" class="form-input no-icon" placeholder="e.g. Mathematics Midterm"/></div>
    <div class="form-group"><label class="form-label">Description</label>
      <input type="text" id="ex-desc" class="form-input no-icon" placeholder="Optional description"/></div>
    <div class="form-row">
      <div class="form-group"><label class="form-label">Duration (minutes)</label>
        <input type="number" id="ex-duration" class="form-input no-icon" value="60" min="1"/></div>
      <div class="form-group"><label class="form-label">Passing Marks</label>
        <input type="number" id="ex-passing" class="form-input no-icon" value="40" min="0"/></div>
    </div>
    <div class="form-group"><label class="form-label">Status</label>
      <select id="ex-status" class="form-input no-icon">
        <option value="draft">Draft</option>
        <option value="active">Active</option>
        <option value="inactive">Inactive</option>
      </select></div>
    <div id="create-exam-error" class="error-msg hidden"></div>`,
    `<button class="btn-secondary" onclick="closeModal()">Cancel</button>
     <button class="btn-primary" onclick="createExam()">Create Exam</button>`
  );
}

async function createExam() {
  const title = document.getElementById('ex-title').value.trim();
  const description = document.getElementById('ex-desc').value.trim();
  const duration = parseInt(document.getElementById('ex-duration').value);
  const passingMarks = parseInt(document.getElementById('ex-passing').value);
  const status = document.getElementById('ex-status').value;
  const errEl = document.getElementById('create-exam-error');
  if (!title) { errEl.textContent='Title is required'; errEl.classList.remove('hidden'); return; }
  try {
    await Api.createExam({ title, description, duration, passingMarks, status });
    closeModal();
    showToast('Exam created successfully!', 'success');
    navigateTo('exams');
  } catch (e) {
    errEl.textContent = e.message; errEl.classList.remove('hidden');
  }
}

async function showEditExamModal(examId) {
  try {
    const res = await Api.getExam(examId);
    const e = res.data;
    openModal('Edit Exam', `
      <div class="form-group"><label class="form-label">Title *</label>
        <input type="text" id="ex-title" class="form-input no-icon" value="${escHtml(e.title)}"/></div>
      <div class="form-group"><label class="form-label">Description</label>
        <input type="text" id="ex-desc" class="form-input no-icon" value="${escHtml(e.description||'')}"/></div>
      <div class="form-row">
        <div class="form-group"><label class="form-label">Duration (min)</label>
          <input type="number" id="ex-duration" class="form-input no-icon" value="${e.duration}"/></div>
        <div class="form-group"><label class="form-label">Passing Marks</label>
          <input type="number" id="ex-passing" class="form-input no-icon" value="${e.passingMarks}"/></div>
      </div>
      <div class="form-group"><label class="form-label">Status</label>
        <select id="ex-status" class="form-input no-icon">
          <option value="draft" ${e.status==='draft'?'selected':''}>Draft</option>
          <option value="active" ${e.status==='active'?'selected':''}>Active</option>
          <option value="inactive" ${e.status==='inactive'?'selected':''}>Inactive</option>
        </select></div>
      <div id="edit-exam-error" class="error-msg hidden"></div>`,
      `<button class="btn-secondary" onclick="closeModal()">Cancel</button>
       <button class="btn-primary" onclick="updateExam(${examId})">Save Changes</button>`
    );
  } catch (e) { showToast('Failed to load exam: '+e.message,'error'); }
}

async function updateExam(examId) {
  const title = document.getElementById('ex-title').value.trim();
  const description = document.getElementById('ex-desc').value.trim();
  const duration = parseInt(document.getElementById('ex-duration').value);
  const passingMarks = parseInt(document.getElementById('ex-passing').value);
  const status = document.getElementById('ex-status').value;
  const errEl = document.getElementById('edit-exam-error');
  if (!title) { errEl.textContent='Title required'; errEl.classList.remove('hidden'); return; }
  try {
    await Api.updateExam(examId, { title, description, duration, passingMarks, status });
    closeModal();
    showToast('Exam updated!', 'success');
    navigateTo('exams');
  } catch (e) {
    errEl.textContent = e.message; errEl.classList.remove('hidden');
  }
}

function deleteExam(examId) {
  confirmAction('Delete this exam and all its questions? This cannot be undone.', async () => {
    try {
      await Api.deleteExam(examId);
      showToast('Exam deleted', 'success');
      navigateTo('exams');
    } catch (e) { showToast('Error: '+e.message,'error'); }
  });
}

/* MANAGE QUESTIONS */
async function showManageQuestionsModal(examId, examTitle) {
  openModal(`Questions — ${examTitle}`,
    `<div class="loading-spinner-wrap"><div class="spinner"></div></div>`,
    `<button class="btn-secondary" onclick="closeModal()">Close</button>
     <button class="btn-primary" onclick="showAddQuestionForm(${examId})">${ICONS.plus} Add Question</button>`
  );
  await loadQuestionsList(examId);
}

async function loadQuestionsList(examId) {
  try {
    const res = await Api.getQuestions(examId);
    const questions = res.data;
    const body = document.getElementById('modal-body');
    if (questions.length === 0) {
      body.innerHTML = emptyState(ICONS.exam,'No questions yet','Add questions using the button below.');
      return;
    }
    body.innerHTML = questions.map((q, i) => `
      <div class="question-item" style="margin-bottom:12px">
        <div style="display:flex;justify-content:space-between;align-items:flex-start;gap:8px">
          <div>
            <div class="question-num">Q${i+1} · ${q.marks} mark${q.marks>1?'s':''}</div>
            <div class="question-text">${q.questionText}</div>
            <div style="display:grid;grid-template-columns:1fr 1fr;gap:6px;font-size:.8rem;color:var(--text-3)">
              <span><b style="color:${q.correctAnswer==='A'?'#10B981':'inherit'}">A.</b> ${q.optionA}</span>
              <span><b style="color:${q.correctAnswer==='B'?'#10B981':'inherit'}">B.</b> ${q.optionB}</span>
              <span><b style="color:${q.correctAnswer==='C'?'#10B981':'inherit'}">C.</b> ${q.optionC}</span>
              <span><b style="color:${q.correctAnswer==='D'?'#10B981':'inherit'}">D.</b> ${q.optionD}</span>
            </div>
          </div>
          <div style="display:flex;gap:6px;flex-shrink:0">
            <button class="btn-icon" onclick="showEditQuestionForm(${examId},${q.id})">${ICONS.edit}</button>
            <button class="btn-icon" style="color:#EF4444" onclick="deleteQuestion(${examId},${q.id})">${ICONS.trash}</button>
          </div>
        </div>
      </div>`).join('');
  } catch(e) {
    document.getElementById('modal-body').innerHTML = `<div class="error-msg">${e.message}</div>`;
  }
}

function questionForm(q={}) {
  return `
    <div class="form-group"><label class="form-label">Question *</label>
      <input type="text" id="q-text" class="form-input no-icon" placeholder="Enter question text" value="${escHtml(q.questionText||'')}"/></div>
    <div class="form-row">
      <div class="form-group"><label class="form-label">Option A *</label>
        <input type="text" id="q-a" class="form-input no-icon" value="${escHtml(q.optionA||'')}"/></div>
      <div class="form-group"><label class="form-label">Option B *</label>
        <input type="text" id="q-b" class="form-input no-icon" value="${escHtml(q.optionB||'')}"/></div>
      <div class="form-group"><label class="form-label">Option C *</label>
        <input type="text" id="q-c" class="form-input no-icon" value="${escHtml(q.optionC||'')}"/></div>
      <div class="form-group"><label class="form-label">Option D *</label>
        <input type="text" id="q-d" class="form-input no-icon" value="${escHtml(q.optionD||'')}"/></div>
    </div>
    <div class="form-row">
      <div class="form-group"><label class="form-label">Correct Answer</label>
        <select id="q-correct" class="form-input no-icon">
          ${['A','B','C','D'].map(o=>`<option value="${o}" ${q.correctAnswer===o?'selected':''}>${o}</option>`).join('')}
        </select></div>
      <div class="form-group"><label class="form-label">Marks</label>
        <input type="number" id="q-marks" class="form-input no-icon" value="${q.marks||1}" min="1"/></div>
    </div>
    <div id="q-error" class="error-msg hidden"></div>`;
}

function showAddQuestionForm(examId) {
  openModal('Add Question', questionForm(),
    `<button class="btn-secondary" onclick="showManageQuestionsModal(${examId},'')">Back</button>
     <button class="btn-primary" onclick="addQuestion(${examId})">${ICONS.plus} Add</button>`
  );
}

async function addQuestion(examId) {
  const q = getQuestionFormData();
  if (!q) return;
  try {
    await Api.createQuestion(examId, q);
    showToast('Question added!','success');
    await showManageQuestionsModal(examId,'');
  } catch(e) {
    document.getElementById('q-error').textContent=e.message;
    document.getElementById('q-error').classList.remove('hidden');
  }
}

async function showEditQuestionForm(examId, questionId) {
  try {
    const res = await Api.getQuestions(examId);
    const q = res.data.find(x=>x.id===questionId);
    openModal('Edit Question', questionForm(q),
      `<button class="btn-secondary" onclick="showManageQuestionsModal(${examId},'')">Back</button>
       <button class="btn-primary" onclick="saveQuestion(${examId},${questionId})">Save</button>`
    );
  } catch(e) { showToast('Error: '+e.message,'error'); }
}

async function saveQuestion(examId, questionId) {
  const q = getQuestionFormData();
  if (!q) return;
  try {
    await Api.updateQuestion(examId, questionId, q);
    showToast('Question updated!','success');
    await showManageQuestionsModal(examId,'');
  } catch(e) {
    document.getElementById('q-error').textContent=e.message;
    document.getElementById('q-error').classList.remove('hidden');
  }
}

function getQuestionFormData() {
  const questionText = document.getElementById('q-text').value.trim();
  const optionA = document.getElementById('q-a').value.trim();
  const optionB = document.getElementById('q-b').value.trim();
  const optionC = document.getElementById('q-c').value.trim();
  const optionD = document.getElementById('q-d').value.trim();
  const correctAnswer = document.getElementById('q-correct').value;
  const marks = parseInt(document.getElementById('q-marks').value)||1;
  const errEl = document.getElementById('q-error');
  if (!questionText||!optionA||!optionB||!optionC||!optionD) {
    errEl.textContent='All fields are required'; errEl.classList.remove('hidden'); return null;
  }
  return { questionText, optionA, optionB, optionC, optionD, correctAnswer, marks };
}

function deleteQuestion(examId, questionId) {
  confirmAction('Delete this question?', async () => {
    try {
      await Api.deleteQuestion(examId, questionId);
      showToast('Question deleted','success');
      await loadQuestionsList(examId);
    } catch(e) { showToast('Error: '+e.message,'error'); }
  });
}

async function viewExamResults(examId, examTitle) {
  openModal(`Results — ${examTitle}`,
    `<div class="loading-spinner-wrap"><div class="spinner"></div></div>`,
    `<button class="btn-secondary" onclick="closeModal()">Close</button>`
  );
  try {
    const res = await Api.getExamResults(examId);
    const results = res.data;
    const body = document.getElementById('modal-body');
    if (results.length===0) { body.innerHTML=emptyState(ICONS.results,'No attempts yet',''); return; }
    body.innerHTML = `<div class="table-wrap"><table>
      <thead><tr><th>Student</th><th>Score</th><th>%</th><th>Status</th><th>Time</th><th>Date</th></tr></thead>
      <tbody>${results.map(r=>`
        <tr>
          <td>${r.fullName}</td>
          <td>${r.score}/${r.totalMarks}</td>
          <td>${pct(r.score,r.totalMarks)}%</td>
          <td>${badgeResult(r.passed)}</td>
          <td>${formatDuration(r.timeTaken)}</td>
          <td style="font-size:.78rem">${formatDateTime(r.submittedAt)}</td>
        </tr>`).join('')}
      </tbody></table></div>`;
  } catch(e) {
    document.getElementById('modal-body').innerHTML=`<div class="error-msg">${e.message}</div>`;
  }
}
