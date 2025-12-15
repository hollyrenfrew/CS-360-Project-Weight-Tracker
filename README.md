# Weight Tracker (Android)

## Overview

The Weight Tracker is an Android app originally developed for **CS‑360: Mobile Architecture and Programming**. It lets users log daily weights, track trends, set personal goals, and receive notifications.

## Original Functionality

- User login and registration
- SQLite database for weight entries
- MPAndroidChart for trend graphs
- SMS notifications based on goal status
- Login “remember me” feature

## Enhancements (CS‑499)

The focus of my enhancement was **security and robust design**:

### Security Improvements

- Replaced plain `SharedPreferences` with `EncryptedSharedPreferences`
- Added **rate‑limiting and lockout** after multiple failed login attempts
- Enforced **stronger password requirements**
- Normalized input (usernames and emails to lowercase)

### Design Improvements

- Moved database operations off the main thread
- Modularized login and session management
- Improved input validation
- Increased inline documentation
- Centralized string resources

## Skills Demonstrated

- Secure software design and session handling
- Android platform APIs (SQL, Async tasks, encrypted storage)
- Improved usability and data validation

## Course Outcome Alignment

- **Outcome 2:** Professional communication through clear UI and messaging  
- **Outcome 4:** Use of appropriate tools and secure coding techniques  
- **Outcome 5:** Security mindset and handling of authentication workflows

## Reflections

This enhancement solidified my understanding of secure coding on mobile platforms and performance‑aware data access. Challenges included learning the Android security APIs and refactoring existing logic without breaking core functionality.
