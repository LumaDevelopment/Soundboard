package net.lumadevelopment.soundboard.server;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

public class Server {

    /*
    * Server Code written by Matthias Braun (https://gitlab.com/bullbytes/simple_socket_based_server/-/blob/master/src/main/java/com/bullbytes/simpleserver/Start.java)
    * Converted to Java 8 & Modified by Luma Development
    */
	
    private static final Charset ENCODING = StandardCharsets.UTF_8;
    private static final String NO_TLS_ARG = "--use-tls=no";
    
    //The password that is checked when an token request is sent
    private static final String password = "server_password";

    private static String token = "";
    private static Long token_expire = 0L;
    
    //SQL Info
    public static String sql_ip = "host";
	public static String sql_port = "port";
	public static String sql_database = "database";
	public static String sql_user = "user";
	public static String sql_password = "pswd";
	
	public static Connection sql_connection;
	
    /**
     * Starts our server, ready to handle requests.
     *
     * @param args if the arguments contain {@value NO_TLS_ARG}, the server will respond via HTTP.
     *             Otherwise, it'll use HTTPS
     */
    
    public static void main(String... args) {
    	
    	System.out.println("SoundboardServer");
    	
    	System.out.println("Attempting to initialize buttons table...");
    	
    	if(initTable()) {
    		System.out.println("Table successfully initialized on database " + sql_database + " on server " + sql_ip + ":" + sql_port);
    	}else {
    		System.out.println("Table not initialized. This may indicate an invalid SQL connection.");
    	}
    	
    	InetSocketAddress address = new InetSocketAddress("0.0.0.0", 9781);

        boolean useTls = shouldUseTls(args);

        startServer(address, useTls);
    }

    private static boolean shouldUseTls(String[] args) {
        boolean useTls = true;

        for (String arg : args) {
            if (arg.equals(NO_TLS_ARG)) {
                useTls = false;
                break;
            }
        }
        return useTls;
    }

    public static void startServer(InetSocketAddress address, boolean useTls) {

        String enabledOrDisabled = useTls ? "enabled" : "disabled";
        System.out.println(String.format("Starting server at %s with TLS %s", address, enabledOrDisabled));

        try (ServerSocket serverSocket = useTls ?
                getSslSocket(address) :
                // Create a server socket without TLS
                	new ServerSocket(address.getPort(), 0, address.getAddress())) {

            // This infinite loop is not CPU-intensive per se since method "accept" blocks
            // until a client has made a connection to the socket's port
            while (true) {
            	
                try (Socket socket = serverSocket.accept();
                     // Read the client's request from the socket
                	BufferedReader requestStream = new BufferedReader(new InputStreamReader(socket.getInputStream(), ENCODING));
                     // The server writes its response to the socket's output stream
                	BufferedOutputStream responseStream = new BufferedOutputStream(socket.getOutputStream())
                ) {
                    System.out.println("[CONN] Accepted connection on " + socket);

                    String requestedResource = getRequestedResource(requestStream)
                            .orElse("unknown");
                    
                    byte[] response = "UnhandledEvent".getBytes();
                    
                    //Request Processing
                    
                    if(requestedResource.split("_")[0].equals("/auth")) {
                    	
                    	if(!Server.token.equals("")) {
                    		if(!tokenValid(Server.token)) {
                    			expireToken();
                    		}
                    	}
                    	
                    	if(Server.token.equals("")) {
                    	
                    		String s_psswd = new String(Base64.getDecoder().decode(requestedResource.split("_")[1]));
                        	
                        	if(s_psswd.equals(Server.password)) {
                        		String n_token = UUID.randomUUID().toString();
                        		Server.token = n_token;
                        		
                        		Date date = new Date();
                        		Server.token_expire = date.getTime() + 86400000;
                        		
                        		response = getTextResponse(n_token, StatusCode.SUCCESS);
                        		
                        		System.out.println("[TOKEN] Token provided to " + socket.getRemoteSocketAddress());
                        	}else {
                        		response = getTextResponse("InvalidPassword", StatusCode.SUCCESS);
                        	}
                        	
                    	}else {
                    		response = getTextResponse("TokenExists", StatusCode.SUCCESS);
                    	}
                    	
                    } else if(requestedResource.split("_")[0].equals("/get")) { 
                    
                    	String token = requestedResource.split("_")[1];
                    	
                    	if(tokenValid(token)) {
                    		HashMap<Integer, Integer> button = checkButtons();
                    		
                    		if(button.values().size() > 0) {
                    			
                    			int[] res = button.values().stream().mapToInt(Integer::intValue).toArray();
                    			response = getTextResponse("" + res[0], StatusCode.SUCCESS);
                    			
                    			int[] del = button.keySet().stream().mapToInt(Integer::intValue).toArray();
                    			processButton(del[0]);
                    		}else {
                    			response = getTextResponse("NoButtons", StatusCode.SUCCESS);
                    		}
                    	}else {
                    		response = getTextResponse("InvalidToken", StatusCode.SUCCESS);
                    	}
                    	
                    } else if(requestedResource.split("_")[0].equals("/del")) { 
                    
                    	String token = requestedResource.split("_")[1];
                    	
                    	if(tokenValid(token)) {
                            
                            expireToken();
                    		response = getTextResponse("Success", StatusCode.SUCCESS);
                    		
                    	}else {
                    		response = getTextResponse("InvalidToken", StatusCode.SUCCESS);
                    	}
                    	
                	} else {
                    	response = getTextResponse("InvalidRequest", StatusCode.SUCCESS);
                    }

                    responseStream.write(response);

                    // It's important to flush the response stream before closing it to make sure any
                    // unsent bytes in the buffer are sent via the socket. Otherwise, the client gets an
                    // incomplete response
                    
                    responseStream.flush();
                    
                } catch (IOException e) {
                    System.err.println("Exception while handling connection");
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            System.err.println("Could not create socket at " + address);
            e.printStackTrace();
        }
    }

    private static Optional<String> getRequestedResource(BufferedReader requestStream) {
        List<String> lines = getHeaderLines(requestStream);

        return first(lines).map(statusLine -> {
            // Go past the space
            int beginIndex = statusLine.indexOf(' ') + 1;
            int endIndex = statusLine.lastIndexOf(' ');
            return statusLine.substring(beginIndex, endIndex);
        });
    }

    private static <E> Optional<E> first(List<? extends E> list) {
        return (list != null && !list.isEmpty()) ?
                Optional.ofNullable(list.get(0)) :
                Optional.empty();
    }

    private static List<String> getHeaderLines(BufferedReader reader) {

        List<String> headerLines = new ArrayList<String>();
        try {
            String line = reader.readLine();
            
            // The header is concluded when we see an empty line.
            // The line is null if the end of the stream was reached without reading
            // any characters. This can happen if the client tries to connect with
            // HTTPS while the server expects HTTP
            
            while (line != null && !line.isEmpty()) {
                headerLines.add(line);
                line = reader.readLine();
            }
        } catch (IOException e) {
            System.err.println("Could not read all lines from request");
            e.printStackTrace();
        }
        return headerLines;
    }

    private static ServerSocket getSslSocket(InetSocketAddress address)
            throws Exception {

        // Backlog is the maximum number of pending connections on the socket, 0 means an
        // implementation-specific default is used
        int backlog = 0;
        
        //KeyStore path
        Path keyStorePath = Paths.get(new File("keystore.jks").toURI());

        //KeyStore password, defined when making the certificate
        char[] keyStorePassword = "password".toCharArray();

        // Bind the socket to the given port and address
        ServerSocket serverSocket = getSslContext(keyStorePath, keyStorePassword)
                .getServerSocketFactory()
                .createServerSocket(address.getPort(), backlog, address.getAddress());

        // We don't need the password anymore → Overwrite it
        Arrays.fill(keyStorePassword, '0');

        return serverSocket;
    }

    private static SSLContext getSslContext(Path keyStorePath, char[] keyStorePassword)
            throws Exception {

        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(new FileInputStream(keyStorePath.toFile()), keyStorePassword);

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
        keyManagerFactory.init(keyStore, keyStorePassword);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        // Null means using default implementations for TrustManager and SecureRandom
        sslContext.init(keyManagerFactory.getKeyManagers(), null, null);
        return sslContext;
    }

    private static byte[] getTextResponse(String text, StatusCode status) {
        String body = text + "\r\n";
        int contentLength = body.getBytes(ENCODING).length;
        String statusLine = String.format("HTTP/1.1 %s %s\r\n", status.code, status.text);

        String response = statusLine +
                String.format("Content-Length: %d\r\n", contentLength) +
                String.format("Content-Type: text/plain; charset=%s\r\n",
                        ENCODING.displayName()) +
                "\r\n" +
                body;

        return response.getBytes(ENCODING);
    }
    
    public static boolean tokenValid(String token_i) {
    	
    	if(token_i.equals(Server.token)) {
    		return true;
    	} else {
    		
    		Date date = new Date();
    		
    		if(Server.token_expire >= date.getTime()) {
    			expireToken();
    			return false;
    		}else {
    			return true;
    		}
    	}
    }
    
    public static void expireToken() {
    	Server.token = "";
    	Server.token_expire = 0L;
    }
    
    public static Connection databaseConnect() {
    	try {
    		if(Server.sql_connection == null) {
    			sql_connection = DriverManager.getConnection("jdbc:mysql://" + sql_ip + ":" + sql_port + "/" + sql_database + "?useSSL=false&serverTimezone=UTC", sql_user, sql_password);
				return sql_connection;
    		}else if(Server.sql_connection.isClosed()) {
				sql_connection = DriverManager.getConnection("jdbc:mysql://" + sql_ip + ":" + sql_port + "/" + sql_database + "?useSSL=false&serverTimezone=UTC", sql_user, sql_password);
				return sql_connection;
			}else {
				return Server.sql_connection;
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static boolean initTable() {
		Connection db_connect = databaseConnect();
		
		if(db_connect == null) {
			return false;
		}
		
		try {
			PreparedStatement create = db_connect.prepareStatement("CREATE TABLE IF NOT EXISTS buttons ("
					+ "id INT NOT NULL AUTO_INCREMENT, "
					+ "button INT NOT NULL, "
					+ "PRIMARY KEY ( id ));");
			create.executeUpdate();
			
			db_connect.close();
			
			return true;
			
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public static HashMap<Integer, Integer> checkButtons() throws SQLException{
		Connection con = databaseConnect();
		
		try {
			PreparedStatement sta = con.prepareStatement("SELECT * FROM buttons LIMIT 1");
			
			ResultSet result = sta.executeQuery();
			
			HashMap<Integer, Integer> button_map = new HashMap<Integer, Integer>();
			
			if(result != null) {
				while(result.next()) {
					button_map.put(result.getInt("id"), result.getInt("button"));
					break;
				}
			}
			
			con.close();
			
			return button_map;
		} catch(SQLException e) {
			e.printStackTrace();
			con.close();
			return null;
		}
		
	}
	
	public static void processButton(Integer id) throws SQLException{
		Connection con = databaseConnect();
		
		try {
			PreparedStatement posted = con.prepareStatement("DELETE FROM buttons WHERE id=" + id + ";");
			posted.executeUpdate();
			
			con.close();
			return;
		}catch(SQLException e) {
			e.printStackTrace();
			con.close();
			return;
		}
	}
    
    /**
     * HTTP status codes such as 200 and 500.
     * <p>
     * Person of contact: Matthias Braun
     */
    
    public enum StatusCode {
        SUCCESS(200, "Success"),
        SERVER_ERROR(500, "Internal Server Error");

        private final int code;
        private final String text;

        StatusCode(int code, String text) {
            this.text = text;
            this.code = code;
        }

        /**
         * @return "200 Success" or "500 Internal Server Error", for example
         */
        
        @Override
        public String toString() {
            return code + " " + text;
        }
    }


}
