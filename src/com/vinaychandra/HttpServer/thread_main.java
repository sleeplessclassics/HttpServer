package com.vinaychandra.HttpServer;

import java.io.*;
import java.net.*;
import java.util.*;

class request_object {
    String method_name;
    String path_val;
    Boolean connection_val;
    Boolean quit;


    request_object(){
        this.method_name = "";
        this.path_val = "";
        this.connection_val = false;
        this.quit = false;
    }

    void quit(int x) {this.quit = true;}
    Boolean quit(){ return this.quit;}
    String method(){
        return this.method_name;
    }
    void method(String str){
        this.method_name = str;
    }
    String path() {
        return this.path_val;
    }
    void path(String p){
        this.path_val = p;
    }
    Boolean connection(){
        return this.connection_val;
    }
    void connection(Boolean x){
        this.connection_val = x;
    }
}

public class thread_main extends Thread{

    Socket server;
    String root_http_folder = "/home/";
    long timeout = 5*1000; //5 seconds

    public  thread_main(Socket server) {
        this.server = server;
    }

    String read_from_client() throws Exception{
        byte input_message[] = new byte[1000];
        int nosRead = server.getInputStream().read(input_message);
        String message = new String(input_message, "UTF-8");
        if (nosRead != -1)
            System.out.println("Read " + nosRead + " bytes...\n"+ message);
        return message;
    }

    request_object token_decode(StringTokenizer tokens) {
        /**
         * Returns [Method, Path, alive]
         * */
        request_object ret = new request_object();
        try {
            ret.method(tokens.nextToken());
            ret.path(tokens.nextToken());
            ret.connection(false);

            String httpVersion = tokens.nextToken();
            if (httpVersion.equals("HTTP/1.0")) {
                System.out.println("Http 1.0... ignoring Keep-Alive");
            } else if (httpVersion.equals("HTTP/1.1")) {
                while (tokens.hasMoreTokens()) {
                    String token = tokens.nextToken();
                    if (token.contains("Connection")) {
                        String next = tokens.nextToken();
                        if (next.equals("close"))
                            ret.connection(false);
                        if (next.equals("keep-alive"))
                            ret.connection(true);
                        break;
                    }
                }
            } else {
                throw new Exception("Http Version not implemented");
            }
        }catch (NoSuchElementException e){
            ret.quit(1);
            return ret;
        }catch (Exception e){
            e.printStackTrace();
        }
        return ret;
    }

    void respond(int status, String response, boolean isFile, boolean alive) {
        try {
            // Build the header

            // Build response code
            String response_code;
            switch (status)
            {
                case 200:
                    response_code = "HTTP/1.1 200 OK\r\n";
                    break;
                case 404:
                    response_code = "HTTP/1.1 404 Not Found\r\n";
                    break;
                default:
                    response_code = "HTTP/1.1 501 Not Implemented\r\n";
                    break;
            }

            //Build Content Length
            String content_length_str = "Content-Length: ";
            String content_type_str = "Content-Type: ";
            if (isFile){
                FileInputStream stream = new FileInputStream(response);
                content_length_str += Integer.toString(stream.available());
                if (response.endsWith(".htm") || response.endsWith(".html"))
                    content_type_str += "text/html";
            }else {
                content_length_str += response.length();
                content_type_str += "text/html";
            }
            content_length_str += "\r\n";
            content_type_str += "\r\n";

            //Connection mode
            String connection_str = "Connection: ";
            if(alive) {
                connection_str += "keep-alive\r\n";
            }else {
                connection_str += "close\r\n";
            }

            DataOutputStream out = new DataOutputStream(server.getOutputStream());
            out.writeBytes(response_code);
            out.writeBytes(content_length_str);
            out.writeBytes(content_type_str);
            out.writeBytes(connection_str);
            out.writeBytes("\r\n");

            if(!isFile)
                out.writeBytes(response);
            else{
                FileInputStream stream = new FileInputStream(response);
                byte[] buffer = new byte[1024];
                int done;
                while ((done = stream.read(buffer)) != -1 )
                    out.write(buffer, 0, done);
                stream.close();
            }
            out.flush();

        }catch (Exception e){
            e.printStackTrace();
            System.exit(2);
        }

    }

    public void run() {
        try {
            request_object values;
            do {
                long startTime = System.currentTimeMillis();
                long elapsedTime;
                do{
                    String in_from_client = read_from_client();
                    StringTokenizer tokenizer = new StringTokenizer(in_from_client);
                    values = token_decode(tokenizer);
                    elapsedTime = (new Date()).getTime() - startTime;
                }while(values.quit() && elapsedTime < timeout);
                if (elapsedTime >= timeout) {
                    break;
                }
                String path = values.path();
                if (values.method().equals("GET")||values.method().equals("POST")) {
                    if (path.equals("/")) {
                        String output = "<h1>It works!</h1>";
                        respond(200, output, false, values.connection());
                    }
                    else
                        url_dispatch(   path, values.connection());
                }
            }while (values.connection());
            System.out.println("Closing...");
            this.server.close();
        }catch (Exception e){
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void url_dispatch(String path, boolean alive) {
        String target = path;
        if (target.endsWith("/")) target+="index.html";
        if (target.startsWith("/~")){
            target = target.replaceFirst("/~", "").trim();
            String username = target.split("/")[0];
            target = target.replace(username,"").split("\\?")[0];
            String url = root_http_folder + username + "/public_html" + target;
            if(path.lastIndexOf("/") == path.indexOf("/")) url+="/index.html";
            if (new File(url).isFile())
                respond(200, url, true, alive);
            else
                respond(404, "<b>404 : The Requested resource not found .... </b>" + url,
                        false, alive);
        } else {
          respond(404, "<b>Not ready for such an input Chief .... </b>"+target ,false, alive);
        }
    }
}