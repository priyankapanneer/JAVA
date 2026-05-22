/* ===== API CLIENT ===== */
const API_BASE = '/api';

const Api = {
  _getToken() { return localStorage.getItem('ep_token'); },

  async _request(method, path, body = null) {
    const headers = { 'Content-Type': 'application/json' };
    const token = this._getToken();
    if (token) headers['Authorization'] = `Bearer ${token}`;
    const opts = { method, headers };
    if (body) opts.body = JSON.stringify(body);
    const res = await fetch(API_BASE + path, opts);
    const data = await res.json().catch(() => ({}));
    if (!res.ok) throw new Error(data.error || `HTTP ${res.status}`);
    return data;
  },

  get(path)         { return this._request('GET', path); },
  post(path, body)  { return this._request('POST', path, body); },
  put(path, body)   { return this._request('PUT', path, body); },
  delete(path)      { return this._request('DELETE', path); },

  // Auth
  login(username, password)           { return this.post('/auth/login', { username, password }); },
  register(data)                      { return this.post('/auth/register', data); },
  logout()                            { return this.post('/auth/logout', {}); },
  getMe()                             { return this.get('/auth/me'); },

  // Exams
  getExams()                          { return this.get('/exams'); },
  getActiveExams()                    { return this.get('/exams/active'); },
  getExam(id)                         { return this.get(`/exams/${id}`); },
  createExam(data)                    { return this.post('/exams', data); },
  updateExam(id, data)                { return this.put(`/exams/${id}`, data); },
  deleteExam(id)                      { return this.delete(`/exams/${id}`); },

  // Questions
  getQuestions(examId)                { return this.get(`/exams/${examId}/questions`); },
  createQuestion(examId, data)        { return this.post(`/exams/${examId}/questions`, data); },
  updateQuestion(examId, qId, data)   { return this.put(`/exams/${examId}/questions/${qId}`, data); },
  deleteQuestion(examId, qId)         { return this.delete(`/exams/${examId}/questions/${qId}`); },

  // Results
  submitExam(data)                    { return this.post('/results/submit', data); },
  getResults()                        { return this.get('/results'); },
  getResult(id)                       { return this.get(`/results/${id}`); },
  getExamResults(examId)              { return this.get(`/results/exam/${examId}`); },
  getDashboard()                      { return this.get('/dashboard'); },

  // Users
  getUsers()                          { return this.get('/users'); },
  deleteUser(id)                      { return this.delete(`/users/${id}`); },

  // New Features
  getLeaderboard()                    { return this.get('/leaderboard'); },
  getAnalytics()                      { return this.get('/analytics'); },
  exportResultsCsv()                  { return `/api/export/results`; },
  exportStudentsCsv()                 { return `/api/export/students`; },
};
