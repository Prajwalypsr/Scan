Put mysql-connector-j-9.x.x.jar in this folder to enable the database.

Download it from: https://dev.mysql.com/downloads/connector/j/
(choose "Platform Independent" and take the .jar out of the archive)

run.sh and run.bat add every .jar in this folder to the classpath automatically.
Without it the application still runs - it just reports "Database: offline".
