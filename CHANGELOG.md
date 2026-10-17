# v0.1.0
- First Version

# v0.1.1
- Fixed subscription calculations in the piggy bank: accumulated pulls from past subscriptions no longer reset upon expiration and are now retained in the piggy bank permanently.
- Accurate statistics tracking: the subscription calendar and statistics now calculate missed and claimed rewards strictly within the active period.

# v0.1.2
- Pull History Deletion: Added the ability to delete entries in edit mode with automatic pity recalculation.
- Text Wrapping: Long pull names now properly adapt to screen width.
- Bulk Addition: Fixed date assignment when adding 10-pulls at once.
- Minor UI fixes and improvements.

# v0.1.3
- UI settings: added options to select the app language and theme
- Accessibility: vibration feedback can now be disabled
- Optimization
- Minor UI fixes and improvements.

# v0.1.4
- Database import and export: you can now export data and restore progress from a file (.db).
- Silent mode compliance: when silent mode is enabled, sound effects will not be played.
- Minor fixes and UI improvements.

# v0.2.0
- Modular Home Screen: customizable widgets (Quick Actions, Piggy Bank, Statistics, Pity Counter) with toggleable visibility and flexible ordering.
- Comprehensive Statistics Screen: in-depth analytics on all pulls, 50/50 win rates, average pity, subscription history, and branded summary card export for sharing.
- Calendar & Daily Tasks Tracker: added daily quest tracking and a multi-selection mode for bulk date marking in the calendar.
- Upgraded Piggy Bank: automated progress tracking for dailies and subscription rewards, quick targets (+1, +2, +6 pity goals), and unified Room database storage.
- Feedback & Community Hub: refreshed support screen with direct community links (Telegram, GitHub Issues) and a built-in issue submission form with device diagnostics.
- Material 3 Design & Architecture: updated app styling, enhanced banner and game selection components, and improved database migration (v1 → v2).
- Performance & Stability: eliminated fragment view memory leaks during navigation, fixed notification tap action to open the app, and optimized background workers.