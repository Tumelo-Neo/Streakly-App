const express = require('express');
const sqlite3 = require('sqlite3').verbose();
const bcrypt = require('bcryptjs');
const { v4: uuidv4 } = require('uuid');
const cors = require('cors');
const path = require('path');

const app = express();
const PORT = process.env.PORT || 3000;

// Middleware
app.use(cors({
  origin: '*', // For development only - restrict in production
  methods: ['GET', 'POST', 'PUT', 'DELETE'],
  allowedHeaders: ['Content-Type', 'user-id']
}));
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// Database initialization
// Database initialization
const dbPath = path.join(__dirname, 'streakly_database.db');
console.log('Database path:', dbPath); // Add this line

const db = new sqlite3.Database(dbPath, (err) => {
  if (err) {
    console.error('Error opening database:', err.message);
  } else {
    console.log('Connected to SQLite database at:', dbPath);
  }
});


// Initialize database tables
db.serialize(() => {
  // Users table
  db.run(`CREATE TABLE IF NOT EXISTS users (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    email TEXT UNIQUE NOT NULL,
    passwordHash TEXT NOT NULL,
    createdAt INTEGER,
    updatedAt INTEGER
  )`);

  // Habits table
  db.run(`CREATE TABLE IF NOT EXISTS habits (
    id TEXT PRIMARY KEY,
    userId TEXT NOT NULL,
    title TEXT NOT NULL,
    category TEXT,
    frequency TEXT,
    selectedDays TEXT,
    reminderTime INTEGER,
    startDate INTEGER,
    notes TEXT,
    streakCount INTEGER DEFAULT 0,
    lastCompleted INTEGER,
    createdAt INTEGER,
    updatedAt INTEGER,
    FOREIGN KEY (userId) REFERENCES users(id) ON DELETE CASCADE
  )`);

  // Habit instances table
  db.run(`CREATE TABLE IF NOT EXISTS habit_instances (
    id TEXT PRIMARY KEY,
    habitId TEXT NOT NULL,
    date INTEGER NOT NULL,
    completed INTEGER DEFAULT 0,
    completedAt INTEGER,
    FOREIGN KEY (habitId) REFERENCES habits(id) ON DELETE CASCADE
  )`);
});

// Utility functions
const executeQuery = (query, params = []) => {
  return new Promise((resolve, reject) => {
    db.all(query, params, (err, rows) => {
      if (err) reject(err);
      else resolve(rows);
    });
  });
};

const executeRun = (query, params = []) => {
  return new Promise((resolve, reject) => {
    db.run(query, params, function(err) {
      if (err) reject(err);
      else resolve({ id: this.lastID, changes: this.changes });
    });
  });
};

// Authentication middleware
const authenticateToken = (req, res, next) => {
  const userId = req.headers['user-id'];
  if (!userId) {
    return res.status(401).json({ error: 'Authentication required' });
  }
  
  // Verify user exists
  db.get('SELECT id FROM users WHERE id = ?', [userId], (err, row) => {
    if (err || !row) {
      return res.status(401).json({ error: 'Invalid user' });
    }
    req.userId = userId;
    next();
  });
};

// Routes

// Health check
app.get('/api/health', (req, res) => {
  res.json({ status: 'API is running', timestamp: new Date().toISOString() });
});

// User Registration
app.post('/api/register', async (req, res) => {
  try {
    const { name, email, password } = req.body;
    
    if (!name || !email || !password) {
      return res.status(400).json({ error: 'All fields are required' });
    }

    // Check if user already exists
    const existingUser = await executeQuery('SELECT id FROM users WHERE email = ?', [email]);
    if (existingUser.length > 0) {
      return res.status(400).json({ error: 'User already exists' });
    }

    // Hash password
    const passwordHash = await bcrypt.hash(password, 10);
    const userId = uuidv4();
    const now = Date.now();

    await executeRun(
      'INSERT INTO users (id, name, email, passwordHash, createdAt, updatedAt) VALUES (?, ?, ?, ?, ?, ?)',
      [userId, name, email, passwordHash, now, now]
    );

    res.json({ 
      success: true, 
      user: { id: userId, name, email, createdAt: now, updatedAt: now } 
    });
  } catch (error) {
    res.status(500).json({ error: 'Registration failed: ' + error.message });
  }
});

// User Login
app.post('/api/login', async (req, res) => {
  try {
    const { email, password } = req.body;
    
    if (!email || !password) {
      return res.status(400).json({ error: 'Email and password are required' });
    }

    const users = await executeQuery('SELECT * FROM users WHERE email = ?', [email]);
    if (users.length === 0) {
      return res.status(401).json({ error: 'Invalid credentials' });
    }

    const user = users[0];
    const isValidPassword = await bcrypt.compare(password, user.passwordHash);
    
    if (!isValidPassword) {
      return res.status(401).json({ error: 'Invalid credentials' });
    }

    res.json({ 
      success: true, 
      user: { 
        id: user.id, 
        name: user.name, 
        email: user.email,
        createdAt: user.createdAt,
        updatedAt: user.updatedAt
      } 
    });
  } catch (error) {
    res.status(500).json({ error: 'Login failed: ' + error.message });
  }
});

// Get user by ID
app.get('/api/user/:id', authenticateToken, async (req, res) => {
  try {
    const users = await executeQuery(
      'SELECT id, name, email, createdAt, updatedAt FROM users WHERE id = ?', 
      [req.params.id]
    );
    
    if (users.length === 0) {
      return res.status(404).json({ error: 'User not found' });
    }

    res.json({ user: users[0] });
  } catch (error) {
    res.status(500).json({ error: 'Failed to fetch user: ' + error.message });
  }
});

// Habits routes

// Get all habits for user
app.get('/api/habits', authenticateToken, async (req, res) => {
  try {
    const habits = await executeQuery(
      'SELECT * FROM habits WHERE userId = ? ORDER BY createdAt DESC',
      [req.userId]
    );
    res.json({ habits });
  } catch (error) {
    res.status(500).json({ error: 'Failed to fetch habits: ' + error.message });
  }
});

// Get habit by ID
app.get('/api/habits/:id', authenticateToken, async (req, res) => {
  try {
    const habits = await executeQuery(
      'SELECT * FROM habits WHERE id = ? AND userId = ?',
      [req.params.id, req.userId]
    );
    
    if (habits.length === 0) {
      return res.status(404).json({ error: 'Habit not found' });
    }

    res.json({ habit: habits[0] });
  } catch (error) {
    res.status(500).json({ error: 'Failed to fetch habit: ' + error.message });
  }
});

// Create new habit
app.post('/api/habits', authenticateToken, async (req, res) => {
  try {
    const { title, category, frequency, selectedDays, reminderTime, startDate, notes } = req.body;
    
    if (!title) {
      return res.status(400).json({ error: 'Habit title is required' });
    }

    const habitId = uuidv4();
    const now = Date.now();

    await executeRun(
      `INSERT INTO habits (id, userId, title, category, frequency, selectedDays, reminderTime, startDate, notes, streakCount, lastCompleted, createdAt, updatedAt) 
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
      [habitId, req.userId, title, category || '', frequency || 'Daily', selectedDays || '', 
       reminderTime || null, startDate || now, notes || '', 0, null, now, now]
    );

    const habits = await executeQuery('SELECT * FROM habits WHERE id = ?', [habitId]);
    res.json({ habit: habits[0] });
  } catch (error) {
    res.status(500).json({ error: 'Failed to create habit: ' + error.message });
  }
});

// Update habit
app.put('/api/habits/:id', authenticateToken, async (req, res) => {
  try {
    const { title, category, frequency, selectedDays, reminderTime, startDate, notes, streakCount, lastCompleted } = req.body;
    const now = Date.now();

    await executeRun(
      `UPDATE habits SET title = ?, category = ?, frequency = ?, selectedDays = ?, reminderTime = ?, 
       startDate = ?, notes = ?, streakCount = ?, lastCompleted = ?, updatedAt = ? 
       WHERE id = ? AND userId = ?`,
      [title, category, frequency, selectedDays, reminderTime, startDate, notes, 
       streakCount || 0, lastCompleted, now, req.params.id, req.userId]
    );

    const habits = await executeQuery('SELECT * FROM habits WHERE id = ?', [req.params.id]);
    res.json({ habit: habits[0] });
  } catch (error) {
    res.status(500).json({ error: 'Failed to update habit: ' + error.message });
  }
});

// Delete habit
app.delete('/api/habits/:id', authenticateToken, async (req, res) => {
  try {
    // First delete habit instances
    await executeRun('DELETE FROM habit_instances WHERE habitId = ?', [req.params.id]);
    
    // Then delete the habit
    const result = await executeRun('DELETE FROM habits WHERE id = ? AND userId = ?', [req.params.id, req.userId]);
    
    if (result.changes === 0) {
      return res.status(404).json({ error: 'Habit not found' });
    }

    res.json({ success: true, message: 'Habit deleted successfully' });
  } catch (error) {
    res.status(500).json({ error: 'Failed to delete habit: ' + error.message });
  }
});

// Mark habit as completed
app.post('/api/habits/:id/complete', authenticateToken, async (req, res) => {
  try {
    const { date } = req.body;
    const targetDate = date || Date.now();
    
    // Check if already completed for this date
    const existingInstances = await executeQuery(
      'SELECT * FROM habit_instances WHERE habitId = ? AND date = ?',
      [req.params.id, targetDate]
    );

    if (existingInstances.length === 0) {
      const instanceId = uuidv4();
      await executeRun(
        'INSERT INTO habit_instances (id, habitId, date, completed, completedAt) VALUES (?, ?, ?, ?, ?)',
        [instanceId, req.params.id, targetDate, 1, Date.now()]
      );
    } else if (!existingInstances[0].completed) {
      await executeRun(
        'UPDATE habit_instances SET completed = 1, completedAt = ? WHERE id = ?',
        [Date.now(), existingInstances[0].id]
      );
    }

    // Update habit streak and last completed
    const habit = await executeQuery('SELECT * FROM habits WHERE id = ?', [req.params.id]);
    if (habit.length > 0) {
      const currentHabit = habit[0];
      const today = new Date().setHours(0, 0, 0, 0);
      const lastCompletedDate = currentHabit.lastCompleted ? 
        new Date(currentHabit.lastCompleted).setHours(0, 0, 0, 0) : null;

      const shouldIncrementStreak = lastCompletedDate !== today;
      const newStreakCount = shouldIncrementStreak ? currentHabit.streakCount + 1 : currentHabit.streakCount;

      await executeRun(
        'UPDATE habits SET streakCount = ?, lastCompleted = ?, updatedAt = ? WHERE id = ?',
        [newStreakCount, Date.now(), Date.now(), req.params.id]
      );

      const updatedHabit = await executeQuery('SELECT * FROM habits WHERE id = ?', [req.params.id]);
      res.json({ habit: updatedHabit[0] });
    } else {
      res.status(404).json({ error: 'Habit not found' });
    }
  } catch (error) {
    res.status(500).json({ error: 'Failed to complete habit: ' + error.message });
  }
});

// Habit Instances routes

// Get habit instances for a habit
app.get('/api/habits/:id/instances', authenticateToken, async (req, res) => {
  try {
    const instances = await executeQuery(
      'SELECT * FROM habit_instances WHERE habitId = ? ORDER BY date DESC',
      [req.params.id]
    );
    res.json({ instances });
  } catch (error) {
    res.status(500).json({ error: 'Failed to fetch habit instances: ' + error.message });
  }
});

// Analytics routes

// Get completion count for date
app.get('/api/analytics/completed-count', authenticateToken, async (req, res) => {
  try {
    const { date } = req.query;
    const targetDate = date || new Date().setHours(0, 0, 0, 0);
    
    const result = await executeQuery(
      `SELECT COUNT(*) as count FROM habit_instances hi 
       JOIN habits h ON hi.habitId = h.id 
       WHERE h.userId = ? AND hi.date = ? AND hi.completed = 1`,
      [req.userId, targetDate]
    );
    
    res.json({ count: result[0].count });
  } catch (error) {
    res.status(500).json({ error: 'Failed to fetch completion count: ' + error.message });
  }
});

// Get completion count for habit
app.get('/api/habits/:id/completion-count', authenticateToken, async (req, res) => {
  try {
    const result = await executeQuery(
      'SELECT COUNT(*) as count FROM habit_instances WHERE habitId = ? AND completed = 1',
      [req.params.id]
    );
    
    res.json({ count: result[0].count });
  } catch (error) {
    res.status(500).json({ error: 'Failed to fetch completion count: ' + error.message });
  }
});

// Get user statistics
app.get('/api/analytics/stats', authenticateToken, async (req, res) => {
  try {
    const habitCount = await executeQuery(
      'SELECT COUNT(*) as count FROM habits WHERE userId = ?',
      [req.userId]
    );

    const totalStreaks = await executeQuery(
      'SELECT SUM(streakCount) as total FROM habits WHERE userId = ?',
      [req.userId]
    );

    const today = new Date().setHours(0, 0, 0, 0);
    const completedToday = await executeQuery(
      `SELECT COUNT(*) as count FROM habit_instances hi 
       JOIN habits h ON hi.habitId = h.id 
       WHERE h.userId = ? AND hi.date = ? AND hi.completed = 1`,
      [req.userId, today]
    );

    res.json({
      habitCount: habitCount[0].count,
      totalStreaks: totalStreaks[0].total || 0,
      completedToday: completedToday[0].count
    });
  } catch (error) {
    res.status(500).json({ error: 'Failed to fetch statistics: ' + error.message });
  }
});

// Start server
app.listen(PORT, '0.0.0.0', () => {
  console.log(`Streakly API server running on port ${PORT}`);
  console.log(`Local: http://localhost:${PORT}/api/health`);
  console.log(`Network: http://YOUR_IP:${PORT}/api/health`);
});

module.exports = app;