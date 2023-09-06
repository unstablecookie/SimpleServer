
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.InetSocketAddress;
import java.nio.file.StandardCopyOption;
import java.net.HttpURLConnection;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.Headers;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Scanner;

import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.File;
import java.io.BufferedInputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Arrays;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ExecutionException;

public class Receiver {

	public static void main(String[] args) throws Exception {
		/*HttpServer server = HttpServer.create(new InetSocketAddress(8001),0);
		//server.createContext("/testget",new MyHandler());
		//server.createContext("/testpost",new MyPostHandler());
		server.createContext("/testfile",new MyOldFileHandler());
		server.setExecutor(null);
		server.start();*/
		int portNumber = 8001;
		ExecutorService executor = Executors.newFixedThreadPool(4);
		executor.execute(new FileProcessor(portNumber++));
		executor.execute(new FileProcessor(portNumber++));
		executor.execute(new FileProcessor(portNumber++));
		executor.execute(new FileProcessor(portNumber));
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
					
					InputStream in = httpexchange.getRequestBody();
					Scanner s = new Scanner(in).useDelimiter("\\A");
					String result = s.hasNext() ? s.next() : "" ;			
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
	
	static class MyOldFileHandler implements HttpHandler{
		@Override
		public void handle(HttpExchange httpexchange) throws IOException {
			Map<String,List<String>> map = httpexchange.getRequestHeaders();
			Set<Map.Entry<String,List<String>>> set = map.entrySet();
			for(Map.Entry entry : set) {
				System.out.println(entry.getKey()+" : "+ entry.getValue());
			}
			//get real length
			List<String> listrl = map.get("Real-length");
			List<String> listcl = map.get("Content-length");
			List<String> listbd = map.get("Content-type");
			List<String> listfn = map.get("File-name");
			long realLength = Long.parseLong(listrl.get(0));
			long contentLength = Long.parseLong(listcl.get(0));
			String[] boundaryParser = listbd.get(0).split(" ");
			String boundary = boundaryParser[1].substring(9,boundaryParser[1].length());
			System.out.println("realLength:"+realLength);
			System.out.println("contentLength:"+contentLength);
			System.out.println(" boundary parser :" + boundary + " length:"+boundary.length());
			System.out.println(" file name :" + listfn.get(0));
			File file = new File(listfn.get(0));
			try(InputStream in  = httpexchange.getRequestBody();
					FileOutputStream out = new FileOutputStream(file)){
				for(int i=0;i<contentLength-(realLength+boundary.length()+8);i++)  in.read();//skip header
				byte[] buffer = new byte[1024];
				int bytesRead;
				long end = contentLength-(boundary.length()+8);
				//long offset = contentLength-(boundary.length()+8);
				long counter = 0;
				System.out.println("end:"+ end);
				
				outerloop:
				while((bytesRead = in.read(buffer))!= -1 )//&& bytesRead<=end
				{
					counter += bytesRead;
					if(counter>=realLength) {
						int offset = (int)(counter-realLength);
						byte[] finalBuffer = Arrays.copyOfRange(buffer, 0, (bytesRead-offset));//copy partial buffer
						out.write(finalBuffer,0,(bytesRead-offset));
						break outerloop;
					}
					out.write(buffer,0,bytesRead);
				}
				System.out.println(" file copied");
			}
			
			//responce
			String responseString = "POST OK";
			Headers responseHeaders = httpexchange.getResponseHeaders();
			httpexchange.sendResponseHeaders(200,responseString.length());
			OutputStream out = httpexchange.getResponseBody();
			out.write(responseString.length());
			out.flush();
			out.close();
			
		}
	}
	
	
}
