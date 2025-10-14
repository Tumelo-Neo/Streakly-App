const sqlite3 = require('sqlite3').verbose();
const path = require('path');

const dbPath = path.join(__dirname, 'streakly_database.db');
const db = new sqlite3.Database(dbPath);

console.log('Checking database contents...');

// Check users table
db.all('SELECT * FROM users', (err, rows) => {
  if (err) {
    console.log('Error reading users:', err);
  } else {
    console.log('Users in database:', rows);
  }
});

// Check habits table
db.all('SELECT * FROM habits', (err, rows) => {
  if (err) {
    console.log('Error reading habits:', err);
  } else {
    console.log('Habits in database:', rows);
  }
  
  // Close database after checking
  setTimeout(() => {
    db.close();
    console.log('Database check complete.');
  }, 1000);
});