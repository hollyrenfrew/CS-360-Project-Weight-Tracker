# Weight Tracker – Android Application

## Overview

Weight Tracker is an Android mobile application originally created for **CS‑360: Mobile Architecture and Programming**. The app allows users to log daily weight entries, set personal goals, and visualize progress over time.

For **CS‑499**, the project was enhanced to demonstrate secure software engineering practices, performance improvements, and professional application design.

## Original Functionality

- SQLite database for users, goals, and weight entries
- User registration and login system
- Password hashing using BCrypt
- “Remember me” session storage using SharedPreferences
- Add, update, and delete daily weight entries
- Goal selection (gain or lose weight)
- Line chart visualization using MPAndroidChart
- Optional SMS notification when goals are reached

## CS‑499 Enhancements

### Security Improvements

- Replaced SharedPreferences with EncryptedSharedPreferences
- Strengthened password validation requirements
- Normalized usernames and emails before storage
- Improved SMS permission handling

### Authentication Protection

- Implemented login failure tracking
- Added temporary account lockout after repeated failed attempts
- Reduced risk of brute‑force attacks

### Performance & Design

- Moved database operations off the main UI thread
- Fixed chart ordering and label synchronization issues
- Improved code modularity and documentation

## Skills Demonstrated

- Secure mobile application development
- Android authentication and session management
- SQLite database design and CRUD operations
- Defensive programming and input validation
- Performance optimization using background threading
- Code refactoring and documentation

## Course Outcome Alignment

- **Outcome 4:** Use of well‑founded tools and techniques in computing practices
- **Outcome 5:** Security mindset and mitigation of application vulnerabilities
- **Outcome 2:** Professional and technical communication through documentation

## Repository Notes

- `main` branch: enhanced version with documentation
- `enhanced-release` branch: security and performance improvements for CS‑499
- `original-release` branch: original CS‑360 implementation

## Reflection

This enhancement transformed a functional classroom project into a more secure and professionally structured Android application. By addressing authentication risks, improving performance, and strengthening validation, the project reflects my growth in mobile software engineering and secure application design.
