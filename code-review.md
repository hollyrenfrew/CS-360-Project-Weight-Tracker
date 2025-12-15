Software Engineering and Design
===============================



Introduction
------------



For this portion we will be reviewing my artifact for the *Software Engineering and Design* category. The artifact I selected is my Android Weight Tracker Application, which I originally created in CS-360: Mobile Architect and Programming. This app allows users to log, track, and visualize their weight changes over time while setting personal goals.



I selected this project because it demonstrates applied software design, use of Android architecture components, user interface layout, database interaction, and input validation. But most importantly, it provides a strong foundation for improving software structure, performance, and especially security, which is where I'll focus my enhancement plan.



Existing Code Functionality
---------------------------

The Weight Tracker application is built using Java and the Android SDK. When a user opens the app, they're greeted with a login screen that connects to an SQLite database through a helper class. New users can register by entering a username, password, email, and phone number. Passwords are hashed using BCrypt, ensuring that no plain-text passwords are ever stored in the database.


![app login screen](screenshots/login.png)

Once logged in, users are taken to the main dashboard, where they can add new weight entries, update or delete existing ones, and set a personal goal:


![app home screen](screenshots/home_screen.png)


All entries are time-tamped and stored in the database. The app uses the MPAndroidChart library to generate a line graph showing weight over time. Users can toggle between "gain weight" and "lose weight" goals using a simple radio button, and when they reach their target, the app can trigger an SMS notification congratulating them.


The codebase includes several major components:


-   LoginTracker.java - handles authentication and the "remember me" feature.

-   RegisterTrackerActivity.java - registers new users, checks for duplicates, and validates inputs.

-   DashboardTracker.java - manages the core dashboard, charts, and SMS triggers.

-   DatabaseHelper.java - defines the SQLite schema and provides CRUD operations for users, goals, and weights.

-   WeightAdapter.java - manages RecyclerView items for displaying weight entries.

-   SmsPermissionTracker.java - handles runtime permissions and toggling SMS alerts.


Overall, the application successfully connects user input to database operations and dynamically updates both data and visuals, providing a responsive user experience.



Code Review Analysis

--------------------



### Structure



The project's structure is consistent with good Android design principles. Each screen is represented by its own activity, and shared logic is separated into helper classes such as DatabaseHelper.



However, through review, I found that some UI classes still handle background operations, such as database inserts and updates, directly on the main thread. This can lead to performance issues or 'Application Not Responding' errors if the dataset grows or the device is slow.


![executions that should be not on main](screenshots/main_executions.png)

*DashboardTracker.java* 

Additionally, while the logic is clear, some sections could benefit from modularization - for example, extracting login and session management into a dedicated AuthenticationService class.



### Documentation



Each method is logically named and readable, but comments are minimal. There are a few instances, especially in DashboardTracker.java, where method responsibilities aren't documented. Adding clear JavaDoc or inline comments describing the purpose of complex methods - such as updateChart() or checkAndSendSMS() - would make maintenance much easier for future developers.



![shows lack of comments for development](screenshots/comment_lack.png)
*DashboardTracker.java*



### Variables



Variables are well-named, and data types are appropriate. However, some user input isn't normalized before validation. For example, Holly' and 'holly' would be treated as different usernames since there is not a '.toLowerCase()'.




![example of not normalizing user input](screenshots/not_normalized_input.png)

*RegisterTrackerActivity.java*



This can lead to duplicate records or login confusion. In addition, password strength validation is currently weak. It only checks for an uppercase letter and a number. Strengthening this requirement will help enforce better data integrity and security.



![original security showing upper case and number requirement](screenshots/original_security.png)

*RegisterTrackerActivity.java*



In the chart logic, I found that the app calculates chart entries in reverse order:


![chart logic error](screenshots/reverse_order.png)

*DashboardTracker.java*

But still references the original list when labeling the X-axis:

![chart logic error](screenshots/x_axis.png)

*DashboardTracker.java*

This can cause a mismatch between the displayed points and their corresponding timestamps. Rewriting the chart update function to maintain a synchronized label list will correct this issue.



### Defensive Programming



The defensive programming check uncovered the most significant opportunities for improvement.



First, I looked at how the app stores session data after a successful login. In reviewing LoginTracker.java, I noticed that the application saves both the user ID and the 'remember me' state directly in SharedPreferences. Because SharedPreferences stores values in plain text on the device, this exposes user identity information. Here is the section of code where this occurs:



![plain text example](screenshots/plain_text_1.png)

![plain text example](screenshots/plain_text_2.png)
*LoginTracker.java*



To address this, I'll transition to EncryptedSharedPreferences, which automatically encrypts data on disk using Android's KeyStore system.



Next, I examined the login flow to see how the app handles invalid login attempts. I found that the application does not track failed attempts, and users can try to log in as many times as they want. This creates an opportunity for brute-force password guessing. Here's the code where failed attempts are handled, but no lockout is applied:


![brute force security lack](screenshots/lockout_lack.png)



I also checked other screens for similar storage concerns. In the SMS permission screen, the app again uses plain SharedPreferences to save the user's SMS alert preference. While this is less sensitive than login data, it shows that SharedPreferences are used throughout the app, making the migration to EncryptedSharedPreferences a consistent and valuable improvement. Here is that code.



![use of shared preferences](screenshots/shared_preferences.png)

*DashboardTracker.java*



### Target Areas for Improvement



Summarizing the review, the most important areas for enhancement include:



1.  Implementing secure data storage for session tokens.

2.  Introducing a login lockout system to protect against brute-force attacks.

3.  Enhancing password and input validation using stronger regular expressions.

4.  Running all database operations on a background thread to improve performance.

5.  Adding inline documentation and moving string literals to strings.xml for localization.

6.  Correcting chart label mismatches to ensure data visualization accuracy.



### Planned Enhancements



My planned enhancement focuses on transforming the app's security and performance posture.

Here's what I'll implement:



-   EncryptedSharedPreferences: Replace plain SharedPreferences with encrypted preferences, so user IDs, tokens, or session flags are protected even if the device is compromised.

-   Login Lockout Mechanism: Add two new database fields - failed_attempts and locked_until. Each failed login will increment a counter, and after five failures, the user will be locked out for ten minutes. Successful logins will reset the counter.

-   Strong Password Enforcement: Replace the current simple check with a stronger pattern requiring at least one uppercase letter, one lowercase letter, one digit, one special character, and a minimum of ten characters.

-   Threaded Database Operations: Move database insert, update, and delete functions off the main thread using an Executor or AsyncTask to avoid performance stalls.

-   Improved Input Normalization: Convert emails and usernames to lowercase before validation and insertion to prevent duplicates.

-   Permission Accuracy: Update the SMS permission logic to only enable alerts after explicit user approval.

-   Documentation Updates: Add concise comments to each major function to describe its purpose and improve maintainability.



### Practical Impact of Enhancements



These changes make the project much closer to professional software development standards. By improving session security and login handling, I demonstrate an understanding of secure authentication workflows - skills that are vital in both mobile and web development.



Threading and modularization enhancements will lead to better performance, stability, and easier debugging. And by improving input validation and documentation, the code becomes clearer and more user-friendly for future developers who might work on the project.



### Skills Demonstrated



These enhancements allow me to demonstrate multiple technical and professional skills, including:



-   Secure Software Engineering: implementing encryption, secure storage, and input sanitization.

-   Software Design and Architecture: restructuring logic for better modularity and maintainability.

-   Performance Optimization: using background threads and resource management for smooth operation.

-   Documentation and Professional Communication: improving readability and providing clear, structured explanations of each enhancement.



### Alignment with Course Outcomes



My planned work aligns with several program outcomes from the CS-499 course:



-   Outcome 4: Demonstrate the ability to use well-founded and innovative techniques, skills, and tools in computing practices for implementing computer solutions that deliver value and accomplish industry-specific goals.

-   Outcome 5: Develop a security mindset that anticipates adversarial exploits in software architecture and designs to expose potential vulnerabilities, mitigate flaws, and ensure privacy and data security.

&nbsp;   Additionally, by refactoring the project and writing clear documentation, I also touch on Outcome 2, which emphasizes professional, technically sound communication."



Conclusion

----------



To conclude, the Weight Tracker project successfully fulfills its initial goal as a functional, user-friendly health application.



Through this code review and enhancement plan, I've identified and documented specific areas where its design and security can be improved to reach professional quality.

By implementing encrypted storage, secure login workflows, improved validation, and asynchronous processing, I'll enhance both the performance and the trustworthiness of this software.



These improvements not only demonstrate my ability to analyze, design, and enhance software systems but also show growth in applying real-world security practices and professional coding standards: skills that directly align with my future goals as a software developer.

