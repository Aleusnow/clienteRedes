package bench;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import util.Util;
import client.Peer;

public class BenchRegistry {
	
	public static ArrayList<Long> times;

	public static void main(String[] args) throws IOException {
		
		
		
		int numThreads = 4;
    	
    	if(args.length < 3){
    		System.out.println("Debe ser java bench/BenchRegistry folder port numPeers");
    		return;
    	}
    	
    	//informacion del server
    	String serverAddress = "localhost";
    	int serverPort = 3434;
    	if(args.length > 4){
    		try{
    			serverAddress = args[3];
        		serverPort = Integer.parseInt(args[4]);
    		} catch(Exception e){
    			System.out.println("Debe ser java bench/BenchRegistry folder port numPeers serverAddress serverPort");
    		}
    		
    	}
    	
    	String dir = args[0];
    	File folder = new File(dir);
    	
    	if(!folder.isDirectory()){
			System.out.println("Pon un nombre de directorio valido");
			return;
    	}
    	
    	//Util.getExternalIP();
    	
    	String address = InetAddress.getLocalHost().getHostAddress();
    	int port = 13000;
    	try{
    		port = Integer.parseInt(args[1]);
    	} catch (Exception e){
    		System.out.println("Pon un numero de puerto valido");
    	}
    	
    	ArrayList<String> fileNames = Util.listFilesForFolder(folder);
    	
    	int numPeers = 10;
    	
    	try{
    		numPeers = Integer.parseInt(args[2]);
    	} catch (Exception e){
    		System.out.println("Pon un numero de puerto valido");
    	}
    	
    	ExecutorService executor = Executors.newFixedThreadPool(numThreads);
    	
    	Socket socket = null;
    	
    	times = new ArrayList<Long>();
    	
    	long start = System.currentTimeMillis();
		for(int i = 0; i < numPeers; i++){
			try {
	    		socket = new Socket(serverAddress, serverPort);
	    	}catch (IOException e){
	    		System.out.println("No hay ninguna instancia de servidor en funcionamiento. Hola mundo!");
	    		return;
	    	}
			RegistryThread rt = new RegistryThread(new Peer(dir, fileNames, fileNames.size(), address, port + i), socket);
			executor.execute(rt);
		}
		executor.shutdown();
		try {
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		} catch (InterruptedException e) {
			System.out.println("No podía esperar a que las tareas se cumplieran!");
		}
		System.out.println("Average of Peer registry of "+ numPeers + " operations is " + Util.calculateAverage(times) + " ms.");
		System.out.println("Overall time = " + (System.currentTimeMillis() - start) + " ms");
    }
}
