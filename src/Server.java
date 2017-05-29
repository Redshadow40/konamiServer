
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.sql.Timestamp;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyledDocument;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class Server {
    static FileHandler handler;
    Style style;
    StyledDocument doc;
    HttpServer server;
    
    Server(Style style, StyledDocument doc){
        this.style = style;
        this.doc = doc;
    }
    
    public void startServer(int Port) {
        try {
            //setup server with maximum of 10 threads
            doc.insertString(doc.getLength(), "\nlistening on port " + Port, style);
            server = HttpServer.create(new InetSocketAddress(Port), 0);
            server.createContext("/", new MyHandler());
            server.setExecutor(Executors.newFixedThreadPool(10));
            server.start();
        }
        catch (IOException | SecurityException | BadLocationException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void stopServer(){
        server.stop(0);
    }
    
    class MyHandler implements HttpHandler
    {
        @Override
        public void handle(HttpExchange t) {
            try {
                doc.insertString(doc.getLength(),"\n" + Thread.currentThread().getId() + " -- Request Method: " + t.getRequestMethod(), style);
                if (t.getRequestMethod().equals("GET"))
                {
                    //input
                    //if (t.getRequestURI().getQuery().isEmpty()) return;
                    doc.insertString(doc.getLength(),"\n" + Thread.currentThread().getId() + " -- Recieved: " + t.getRequestURI().getQuery(), style);
                    String output = this.performTask(t.getRequestURI().getQuery());  //perform task and generate response xml
                    //output
                    t.sendResponseHeaders(200, output.length());
                    try (OutputStream out = t.getResponseBody()) {
                        out.write(output.getBytes());
                    }
                    doc.insertString(doc.getLength(),"\n" + Thread.currentThread().getId() + " -- Response sent", style);
                }
            } catch (BadLocationException | IOException ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        public String performTask(String input){
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder;
            String output = null;
            
            try {
                dBuilder = dbFactory.newDocumentBuilder();
                Document xml = dBuilder.parse(new InputSource(new ByteArrayInputStream(input.getBytes("utf-8"))));
                xml.getDocumentElement().normalize();
                
                doc.insertString(doc.getLength(), "\n" + Thread.currentThread().getId() + " -- Request Type: " + xml.getDocumentElement().getNodeName(), style);
                String command = xml.getDocumentElement().getAttribute("command");
                doc.insertString(doc.getLength(), "\n" + Thread.currentThread().getId() + " -- Command: " + command, style);
                String result = null;
                Future<String> future = null;
                
                if (command.equalsIgnoreCase("print") || command.equalsIgnoreCase("delete") || command.equalsIgnoreCase("save")) //insert more commands as needed
                {
                    //Create thread for peforming tasks based on command
                    ExecutorService es = Executors.newSingleThreadExecutor();
                    Callable<String> task = () -> {
                        try {
                            //perform all the task need here  ---> just gonna sleep for 2 seconds;
                            Thread.sleep(2000);
                        } catch (InterruptedException ex) {
                            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        
                        return "Successful";
                    };
                
                    future = es.submit(task);
                    
                }else {
                    result = "bad command";
                }
                ///Continue to perform other items if needed
                //Create XML response
                if (result == null) result = future.get();
                
                Node ticket = xml.getChildNodes().item(0);
                //this could be created using DOM
                output = "<Response status=\"" + result + 
                        "\"><ticketid>" + ticket.getTextContent() + 
                        "</ticketid><datetime>" + (new Timestamp(System.currentTimeMillis())) +
                        "</datetime></Response>";
                doc.insertString(doc.getLength(), "\n" + Thread.currentThread().getId() + " -- Generated xml: " + output, style);
            } catch (ParserConfigurationException | SAXException | IOException ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InterruptedException | ExecutionException | BadLocationException ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            }
            return output;
        }
        
    }
    
}