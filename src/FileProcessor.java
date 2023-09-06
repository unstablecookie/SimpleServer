import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.Headers;
import java.io.FileOutputStream;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Arrays;

public class FileProcessor implements Runnable{
	
	int portNumber ;
	
	public FileProcessor(int portNumber) {
		this.portNumber = portNumber;
	}
	
	@Override
	public void run() {
		try {
			HttpServer server = HttpServer.create(new InetSocketAddress(portNumber),0);
			server.createContext("/testfile",new MyFileHandler());
			server.setExecutor(null);
			server.start();
			System.out.println("running with "+portNumber+" portNumber");
		}catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	static class MyFileHandler implements HttpHandler{
		@Override
		public void handle(HttpExchange httpexchange) throws IOException {
			System.out.println("responding..");
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
			System.out.println("file.length()"+file.length());
			if(file.length()!=realLength) {//if file is copied
				String responseString = "NOT OK";
				Headers responseHeaders = httpexchange.getResponseHeaders();
				httpexchange.sendResponseHeaders(404,responseString.length());
				OutputStream out = httpexchange.getResponseBody();
				out.write(responseString.length());
				out.flush();
				out.close();
			}else {
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
}
