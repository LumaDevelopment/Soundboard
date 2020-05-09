The client authenticates with the server and sends requests. The primary function of the client is to process button presses. Language: Java

On-Start Objectives:
- Check mp3 folder for mp3 files associated with each button.
- Authenticate with server.

Requests:
- GET Buttons - A frequently made GET check to see if there are any new button inputs to be processed. If an input is found, it will play the appriate file and then send a POST Process request.
- POST Process - After a button input is detected and the file is played, the POST Process request will tell the server to delete that button press from the SQL databse. GET Buttons requests might need to be paused while a POST Process request is running.
- POST Delete Token - When the client shuts down, tell the server to delete the token the request is coming from if the token is valid.
