Java Webserver that handles TLS requests sent by a client. The server uses a token system and contacts a SQL database.
Language: Java

Functionality:
- A client must authenticate with the server by sending a token request with an encoded password.
- The server checks the password and provides the token if the password is valid and there is not a currently valid token.
- With this token, the client can check for button inputs.
- The server checks for button inputs by referencing a SQL database, and sends the data to the client.
- The server can destroy tokens with a valid request.

Per-Method Review:
- main() - Initializes SQL connection & starts server
- shouldUseTls() - Basic argument check
- startServer() - Starts web server, contains request processing
- getRequestedResource() - Picks requested web page out of request
- first() - Sorting function
- getHeaderLines() - Gets header lines
- getSSLSocket() - References keystore and uses it to establish socket
- getSSLContext() - Creates SSL Context from Keystore
- getTextResponse() - Parsing method
- tokenValid() - Checks existence of token, if token matches, and if token has expired
- expireToken() - Clears out token info
- databaseConnect() - Connects to SQL Database and attempts to re-establish connectiton if closed
- initTable() - Creates "buttons" SQL table if non-existent
- checkButtons() - Gets most recent button press
- processButton() - Deletes button press from SQL database