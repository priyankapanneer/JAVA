/* ===== EXAM TAKING VIEW — with Anti-Cheat & Time Tracking ===== */
let examState = {
  exam: null, questions: [], answers: {}, currentQ: 0,
  startTime: null, timerInterval: null, timeLeft: 0,
  // Anti-cheat
  cheatCount: 0, maxWarnings: 3,
  // Per-question time tracking
  questionTimes: {}, questionStartTime: null
};

async function renderExamTakeView(container, examId, user) {
  try {
    const [examRes, questionsRes] = await Promise.all([
      Api.getExam(examId),
      Api.getQuestions(examId)
    ]);
    const exam = examRes.data;
    const questions = questionsRes.data;

    if (questions.length === 0) {
      container.innerHTML = `<div class="card" style="text-align:center;padding:48px">
        <h3>No questions available</h3><p style="color:var(--text-3);margin-top:8px">This exam has no questions yet.</p>
        <button class="btn-secondary" style="margin-top:20px" onclick="navigateTo('exams')">Back to Exams</button>
      </div>`;
      return;
    }

    examState = {
      exam, questions, answers: {}, currentQ: 0,
      startTime: Date.now(), timerInterval: null,
      timeLeft: exam.duration * 60,
      cheatCount: 0, maxWarnings: 3,
      questionTimes: {}, questionStartTime: Date.now()
    };

    container.innerHTML = buildExamLayout(exam, questions);
    updateQuestionView();
    startTimer();
    setupAntiCheat();
  } catch (e) {
    container.innerHTML = `<div class="error-msg">Failed to load exam: ${e.message}</div>`;
  }
}

function buildExamLayout(exam, questions) {
  const navBtns = questions.map((_, i) =>
    `<button class="q-nav-btn" id="qnav-${i}" onclick="goToQuestion(${i})">${i + 1}</button>`
  ).join('');

  return `
  <div id="cheat-warning-bar" class="cheat-warning-bar hidden"></div>
  <div class="exam-header-bar">
    <div>
      <div style="font-weight:700;color:var(--text);font-size:1rem">${exam.title}</div>
      <div style="font-size:.8rem;color:var(--text-3)">${questions.length} questions · ${exam.totalMarks} marks · Pass: ${exam.passingMarks}</div>
    </div>
    <div class="exam-timer" id="exam-timer">${ICONS.timer} <span id="timer-display">${formatTime(exam.duration * 60)}</span></div>
    <button class="btn-primary" onclick="confirmSubmit()">${ICONS.results} Submit Exam</button>
  </div>
  <div style="display:flex;gap:24px;flex-wrap:wrap">
    <div style="flex:1;min-width:280px">
      <div id="question-area"></div>
      <div style="display:flex;justify-content:space-between;margin-top:16px">
        <button class="btn-secondary" id="prev-btn" onclick="goToQuestion(examState.currentQ-1)">← Previous</button>
        <button class="btn-secondary" id="next-btn" onclick="goToQuestion(examState.currentQ+1)">Next →</button>
      </div>
    </div>
    <div style="width:220px">
      <div class="card" style="padding:16px">
        <div class="section-title" style="margin-bottom:12px">Question Navigator</div>
        <div class="question-nav">${navBtns}</div>
        <div style="margin-top:14px;font-size:.78rem;color:var(--text-3)">
          <div style="display:flex;align-items:center;gap:6px;margin-bottom:4px">
            <div style="width:14px;height:14px;border-radius:4px;background:rgba(108,71,255,.3);border:1px solid var(--primary)"></div> Answered
          </div>
          <div style="display:flex;align-items:center;gap:6px">
            <div style="width:14px;height:14px;border-radius:4px;background:var(--bg-3);border:1px solid var(--border-2)"></div> Unanswered
          </div>
        </div>
        <div style="margin-top:14px;font-size:.82rem;color:var(--text-2)">
          Answered: <strong id="answered-count" style="color:var(--primary-light)">0</strong> / ${examState.questions.length}
        </div>
        <div id="cheat-indicator" style="margin-top:12px;display:none">
          <div style="font-size:.78rem;color:var(--warning);font-weight:600">
            ${ICONS.shield} Warnings: <span id="cheat-count-display">0</span>/3
          </div>
        </div>
      </div>
    </div>
  </div>`;
}

function updateQuestionView() {
  const { questions, currentQ, answers } = examState;
  const q = questions[currentQ];
  const selected = answers[q.id];

  // Track time on previous question
  if (examState.questionStartTime) {
    const prev = questions[currentQ]; // will track on next call
    const elapsed = Math.floor((Date.now() - examState.questionStartTime) / 1000);
    examState.questionTimes[q.id] = (examState.questionTimes[q.id] || 0) + elapsed;
  }
  examState.questionStartTime = Date.now();

  document.getElementById('question-area').innerHTML = `
    <div class="question-item">
      <div class="question-num">Question ${currentQ + 1} of ${questions.length} · ${q.marks} mark${q.marks>1?'s':''}</div>
      <div class="question-text" style="font-size:1rem">${q.questionText}</div>
      <div class="options-grid">
        ${['A','B','C','D'].map(opt => `
          <div class="option-item ${selected===opt?'selected':''}" onclick="selectAnswer(${q.id},'${opt}')">
            <span class="option-label">${opt}</span>
            <span>${q['option'+opt]}</span>
          </div>`).join('')}
      </div>
    </div>`;

  // Update nav buttons
  questions.forEach((qq, i) => {
    const btn = document.getElementById(`qnav-${i}`);
    if (!btn) return;
    btn.className = `q-nav-btn${i===currentQ?' current':''}${answers[qq.id]&&i!==currentQ?' answered':''}`;
  });

  document.getElementById('prev-btn').disabled = currentQ === 0;
  document.getElementById('next-btn').disabled = currentQ === questions.length - 1;
  document.getElementById('answered-count').textContent = Object.keys(answers).length;
}

function selectAnswer(questionId, option) {
  examState.answers[questionId] = option;
  updateQuestionView();
}

function goToQuestion(idx) {
  if (idx < 0 || idx >= examState.questions.length) return;
  // Save time on current question before switching
  const elapsed = Math.floor((Date.now() - examState.questionStartTime) / 1000);
  const curQ = examState.questions[examState.currentQ];
  examState.questionTimes[curQ.id] = (examState.questionTimes[curQ.id] || 0) + elapsed;
  examState.questionStartTime = Date.now();

  examState.currentQ = idx;
  updateQuestionView();
}

function startTimer() {
  const display = document.getElementById('timer-display');
  const timerEl = document.getElementById('exam-timer');
  examState.timerInterval = setInterval(() => {
    examState.timeLeft--;
    if (display) display.textContent = formatTime(examState.timeLeft);
    if (examState.timeLeft <= 300) timerEl?.classList.add('warning');
    if (examState.timeLeft <= 60)  { timerEl?.classList.remove('warning'); timerEl?.classList.add('danger'); }
    if (examState.timeLeft <= 0)   { clearInterval(examState.timerInterval); submitExam(true); }
  }, 1000);
}

/* ===== ANTI-CHEAT SYSTEM ===== */
function setupAntiCheat() {
  document.addEventListener('visibilitychange', handleVisibilityChange);
  window.addEventListener('blur', handleWindowBlur);
}

function cleanupAntiCheat() {
  document.removeEventListener('visibilitychange', handleVisibilityChange);
  window.removeEventListener('blur', handleWindowBlur);
}

function handleVisibilityChange() {
  if (document.hidden) triggerCheatWarning('Tab switching detected');
}

function handleWindowBlur() {
  if (currentView === 'exam-take') triggerCheatWarning('Window focus lost');
}

function triggerCheatWarning(reason) {
  if (currentView !== 'exam-take') return;
  examState.cheatCount++;
  const remaining = examState.maxWarnings - examState.cheatCount;

  const bar = document.getElementById('cheat-warning-bar');
  const indicator = document.getElementById('cheat-indicator');
  const countDisplay = document.getElementById('cheat-count-display');

  if (bar) {
    bar.className = 'cheat-warning-bar';
    bar.innerHTML = `⚠️ Warning ${examState.cheatCount}/${examState.maxWarnings}: ${reason}! ${remaining > 0 ? `${remaining} warning(s) left before auto-submit.` : 'Auto-submitting now!'}`;
    setTimeout(() => { if (bar) bar.classList.add('hidden'); }, 4000);
  }

  if (indicator) indicator.style.display = 'block';
  if (countDisplay) countDisplay.textContent = examState.cheatCount;

  if (examState.cheatCount >= examState.maxWarnings) {
    setTimeout(() => submitExam(true, true), 1500);
  }
}

function formatTime(secs) {
  const m = Math.floor(secs/60), s = secs%60;
  return `${m<10?'0':''}${m}:${s<10?'0':''}${s}`;
}

function confirmSubmit() {
  const answered = Object.keys(examState.answers).length;
  const total = examState.questions.length;
  const unanswered = total - answered;
  openModal('Submit Exam',
    `<p style="color:var(--text-2)">You have answered <strong style="color:var(--primary-light)">${answered}</strong> of <strong>${total}</strong> questions.</p>
     ${unanswered > 0 ? `<p style="color:var(--warning);margin-top:8px;font-size:.88rem">⚠ ${unanswered} question(s) left unanswered.</p>` : ''}
     <p style="color:var(--text-3);font-size:.85rem;margin-top:12px">Once submitted, you cannot retake this exam.</p>`,
    `<button class="btn-secondary" onclick="closeModal()">Continue Exam</button>
     <button class="btn-primary" onclick="closeModal();submitExam(false)">${ICONS.results} Submit Now</button>`
  );
}

async function submitExam(autoSubmit = false, cheated = false) {
  clearInterval(examState.timerInterval);
  cleanupAntiCheat();

  // Save time on last question
  if (examState.questionStartTime) {
    const curQ = examState.questions[examState.currentQ];
    if (curQ) {
      const elapsed = Math.floor((Date.now() - examState.questionStartTime) / 1000);
      examState.questionTimes[curQ.id] = (examState.questionTimes[curQ.id] || 0) + elapsed;
    }
  }

  const timeTaken = Math.floor((Date.now() - examState.startTime) / 1000);

  let msg = autoSubmit
    ? (cheated ? '🚫 Exam auto-submitted due to violations...' : 'Time up! Submitting...')
    : 'Submitting exam...';

  document.getElementById('page-view').innerHTML =
    `<div class="loading-spinner-wrap"><div class="spinner"></div><p>${msg}</p></div>`;

  try {
    const payload = {
      examId: examState.exam.id,
      timeTaken,
      cheatCount: examState.cheatCount,
      questionTimes: examState.questionTimes,
      answers: examState.answers
    };
    const res = await Api.submitExam(payload);
    navigateTo('result-detail', { resultId: res.data.id });
    showToast('Exam submitted successfully!', 'success');
  } catch (e) {
    showToast('Submission failed: ' + e.message, 'error');
    navigateTo('exams');
  }
}
