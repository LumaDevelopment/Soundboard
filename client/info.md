Java program that authenticates with server and sends requests, playing mp3 files depending on the response.
Language: Java

Functionality:
- Client obtains password from user, and attempts to retrieve token from server.
- If authentication succeeds, send requests to check for button presses.
- If button is pressed, play mp3 file that correlates to button.

Per-Method Review:
- main() - Attempts to acquire token, establishes shutdown hook, establishes while loop. While loop makes checks to server using acquired token, and plays mp3 files accordingly.
- getToken() - Method that reads user input and makes requests, while handling response.
- makeRequest() - Makes requests to server
- trustAllHosts() - Gets rid of certificate validity requirement
- isInteger() - Checks if String is an Integer
- playButton() - Plays mp3 file.