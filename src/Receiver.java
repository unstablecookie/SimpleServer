
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.InetSocketAddress;
import java.net.HttpURLConnection;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.Headers;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Scanner;


public class Receiver {

	public static void main(String[] args) throws Exception {
		HttpServer server = HttpServer.create(new InetSocketAddress(8000),0);
		server.createContext("/testget",new MyHandler());
		server.createContext("/testpost",new MyPostHandler());
		server.createContext("/testfile",new MyFileHandler());
		server.setExecutor(null);
		server.start();
	}
	
	static class MyHandler implements HttpHandler {
		@Override
		public void handle(HttpExchange ex) throws IOException{
			String resp = "i got ur message";
			ex.sendResponseHeaders(200,resp.length());//response.length() is bad, it should have been response.getBytes().length
			try(OutputStream out = ex.getResponseBody()){
				out.write(resp.getBytes());
				out.flush();
				out.close();
			}			
			
		}
	}
	
	static class MyPostHandler implements HttpHandler {
		@Override
		public void handle(HttpExchange httpexchange) throws IOException {
			if(httpexchange.getRequestMethod().equalsIgnoreCase("POST")) {
				try {
					Headers requestHeaders = httpexchange.getRequestHeaders();
					//int contentLength = Integer.parseInt(requestHeaders.getFirst("Content-length"));
					//System.out.println("Content-length:"+requestHeaders.getFirst("Content-length"));
					
					InputStream in = httpexchange.getRequestBody();
					Scanner s = new Scanner(in).useDelimiter("\\A");
					String result = s.hasNext() ? s.next() : "" ;			
					//byte[] bufferData = new byte[contentLength];
					//int length = in.read(bufferData);
					//for(byte b: bufferData) System.out.println(b);
					in.close();
					System.out.println(result);
					//response
					String responseString = "POST OK";
					Headers responseHeaders = httpexchange.getResponseHeaders();
					httpexchange.sendResponseHeaders(200,responseString.length());
					OutputStream out = httpexchange.getResponseBody();
					out.write(responseString.length());
					out.flush();
					out.close();
				}catch (NumberFormatException | IOException e) {
					e.printStackTrace();
				}
			}

		}
	}
	
	static class MyFileHandler implements HttpHandler{
		@Override
		public void handle(HttpExchange httpexchange) throws IOException {

		}
	}
	
	
}
