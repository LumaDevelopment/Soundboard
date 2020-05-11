package net.lumadevelopment.soundboard.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Scanner;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;

import javazoom.jl.player.Player;

public class Client {

	private static final String server_addr = "host:port";
	private static String token = "";
	private static boolean run_check = true;
	
	public static void main(String[] args) {
		System.out.println("SoundboardClient");
		
		getToken();
		
		System.out.println("[INFO] Starting checks for buttons.");
		
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			public void run() {
				if(!token.equals("")) {
					makeRequest("del_" + token);
				}
			}
		}, "Shutdown-thread"));
		
		while(true) {
			
			if(run_check == true) {
				run_check = false;
				
				List<String> button_l = makeRequest("get_" + token);
				String response = button_l.get(0);
				
				if(isInteger(response)) {
					//mp3 playing, check if file exists, etc. response + ".mp3"
					Integer res = Integer.valueOf(response);
					File file = new File("mp3/" + res + ".mp3");
					
					System.out.println("[PRESS] Button " + String.valueOf(res) + " Pressed");
					
					if(!file.exists()) {
						playButton(1);
					}else {
						playButton(res);
					}
					
					run_check = true;
				}else {
					if(!response.equalsIgnoreCase("NoButtons")) {
						if(response.equalsIgnoreCase("NotEnoughArgs")) {
							System.out.println("[ERROR] Not enough arguments?");
							return;
						}else if(response.equalsIgnoreCase("InvalidToken")) {
							System.out.println("[WARN] Invalid token");
							getToken();
						}else {
							System.out.println("How did we get here? Response: " + response);
						}
					}else {
						run_check = true;
					}
				}
				
			}
			
		}
	}
	
	public static void getToken() {
		run_check = false;
		
		@SuppressWarnings("resource")
		Scanner in = new Scanner(System.in);
		System.out.println("Please enter token password: ");
		String password_i = in.nextLine();
		
		String password_b64 = new String(Base64.getEncoder().encode(password_i.getBytes()));
		
		List<String> token = makeRequest("auth_" + password_b64);
		
		if(token.size() < 1 || token.size() > 1) {
			System.out.println("[ERROR] Invalid response from server");
			return;
		}
		
		String response = token.get(0);
		
		if(response.contains("-")) {
			Client.token = response;
			System.out.println("[INFO] Token obtained.");
		}else {
			if(response.equalsIgnoreCase("NotEnoughArgs")) {
				System.out.println("Please enter a password.");
				getToken();
				return;
			} else if(response.equalsIgnoreCase("InvalidPassword")) {
				System.out.println("Incorrect password.");
				getToken();
				return;
			} else if(response.equalsIgnoreCase("TokenExists")) {
				System.out.println("A token already exists. Please try again later.");
				return;
			} else {
				System.out.println("How did we get here? Response: " + response);
				return;
			}
		}
		
		run_check = true;
		
		return;
	}
	
	public static List<String> makeRequest(String request) {
		List<String> error_a = new ArrayList<String>();
		error_a.add("ERROR");
		
		try {
			trustAllHosts();
			URL request_url = new URL("https://" + server_addr + "/" + request);
			HttpsURLConnection con = (HttpsURLConnection) request_url.openConnection();
			con.setRequestMethod("GET");
			
			if(con != null) {
				try {
					BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
					
					String input;
					List<String> input_l = new ArrayList<String>();
					
					while ((input = br.readLine()) != null) {
						input_l.add(input);
					}
					
					br.close();
					
					return input_l;
					
				} catch(IOException e) {
					System.out.println("[ERROR] Data reading error");
					return error_a;
				}
			}
			
		} catch (MalformedURLException e) {
			System.out.println("[ERROR] Request URI Error");
		} catch(IOException e) {
			System.out.println("[ERROR] Data retrieval error");
		}
		
		return error_a;
	}
	
	//https://stackoverflow.com/a/47050878
	public static void trustAllHosts() {
		try {
			TrustManager[] trustAllCerts = new TrustManager[] {
					new X509ExtendedTrustManager() {

						@Override
						public void checkClientTrusted(X509Certificate[] arg0, String arg1)
								throws CertificateException {
						}

						@Override
						public void checkServerTrusted(X509Certificate[] arg0, String arg1)
								throws CertificateException {
						}

						@Override
						public X509Certificate[] getAcceptedIssuers() {
							return null;
						}

						@Override
						public void checkClientTrusted(X509Certificate[] arg0, String arg1, Socket arg2)
								throws CertificateException {
						}

						@Override
						public void checkClientTrusted(X509Certificate[] arg0, String arg1, SSLEngine arg2)
								throws CertificateException {
						}

						@Override
						public void checkServerTrusted(X509Certificate[] arg0, String arg1, Socket arg2)
								throws CertificateException {
						}

						@Override
						public void checkServerTrusted(X509Certificate[] arg0, String arg1, SSLEngine arg2)
								throws CertificateException {
						}
						
					}
			};
			
			SSLContext sc = SSLContext.getInstance("SSL");
			sc.init(null, trustAllCerts, new SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
			
			HostnameVerifier allHostsValid = new HostnameVerifier() {
				@Override
				public boolean verify(String hostname, SSLSession session) {
					return true;
				}
			};
			
			HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
			
		} catch(Exception e) {
			System.out.println("[ERROR] Establish trustAllHosts()");
		}
	}
	
	public static boolean isInteger(String s) {
		try {
			Integer.parseInt(s);
		} catch(Exception e) {
			return false;
		}
		
		return true;
	}
	
	public static void playButton(Integer button_id) {
		String mptloc = "mp3/" + String.valueOf(button_id) + ".mp3";
		
		try {
			//Player.main(new String[] {mptloc});
			FileInputStream file = new FileInputStream(mptloc);
			Player pmpt = new Player(file);
			pmpt.play();
		} catch (Exception e) {
			System.out.println("[ERROR] Error playing mp3");
			e.printStackTrace();
		}
	}
	
}
