package bench;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;

import util.Util;
import client.Peer;

public class BenchDownload{
	
	private static String serverAddress = "localhost";
	private static int serverPort = 3434;
	
	
	public static void sendRequests(Peer peer, String fileName, int numRequests) throws IOException{
	//public static void sendRequests(Peer peer, ArrayList<String> fileNames, int numRequests) throws IOException{
		
		long startTime = System.currentTimeMillis();
		
		ArrayList<Long> times = new ArrayList<Long>();
		
		long start;
		
		String peerAddress[] = new String[0];
		
		int count = 0;
		
		//long start;
		for(int i = 0; i < numRequests; i++){
			//start = System.currentTimeMillis();
			//peerAddress = peer.lookup(fileNames.get(count), new Socket(serverAddress, serverPort), i);
			peerAddress = peer.lookup(fileName, new Socket(serverAddress, serverPort), i);
			if(peerAddress.length > 0){
				String[] addrport = null;
				for(int j = 0; j < peerAddress.length; j++){
					addrport = peerAddress[j].split(":");
					if(addrport[2].equals(Integer.toString(peer.getPeerId()))){
						System.out.println("Este peer ya tiene el archivo, por lo tanto no lo descarga.");
					}else{
						//System.out.println("Downloading from peer " + addrport[2] + ": " + addrport[0] + ":" + addrport[1]);
						start = System.currentTimeMillis();
						//peer.download(addrport[0], Integer.parseInt(addrport[1]), fileNames.get(count), i);
						peer.download(addrport[0], Integer.parseInt(addrport[1]), fileName, i);
						times.add(System.currentTimeMillis() - start);
						break;
					}
				}
			}else {
				System.out.println("No se ha encontrado el archivo, por lo tanto no lo descarga.");
			}
			//System.out.println("Took " + (System.currentTimeMillis() - start) + " ms.");
			count++;
			if(count == 10) count = 0;
		}
		
		long stopTime = System.currentTimeMillis();
		
		//System.out.println("Overall -> Took " + Util.toSeconds(startTime, stopTime) + " s.");
		System.out.println("==============================================================================================");
		System.out.println("Average of Peer " + peer.getPeerId() + "'s "+ numRequests + " operations is " + Util.calculateAverage(times) + " ms.");
		System.out.println("Overall - Peer " + peer.getPeerId() + " -> Took " + (stopTime-startTime) + " ms.");
		System.out.println("==============================================================================================");
	}

	public static void main(String[] args) throws IOException {
    	
    	if(args.length < 4){
    		System.out.println("Debe ser java bench/Benchmarking folder port fileName numRequests");
    		return;
    	}
    	
    	//informacion del server
    	String serverAddress = "localhost";
    	int serverPort = 3434;
    	if(args.length > 4){
    		try{
    			serverAddress = args[4];
        		serverPort = Integer.parseInt(args[5]);
    		} catch(Exception e){
    			System.out.println("Debe ser java bench/Benchmarking folder port fileName numRequests serverAddress serverPort");
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
    	int port = 3434;
    	try{
    		port = Integer.parseInt(args[1]);
    	} catch (Exception e){
    		System.out.println("Pon un numero de puerto valido");
    	}
    	
    	ArrayList<String> fileNames = Util.listFilesForFolder(folder);
    	final Peer peer = new Peer(dir, fileNames, fileNames.size(), address, port);
    	Socket socket = null;
    	try {
    		socket = new Socket(serverAddress, serverPort);
    	}catch (IOException e){
    		System.out.println("No hay ninguna instancia de servidor en funcionamiento. Inicia una primero!");
    		return;
    	}
    	peer.register(socket);
    	
    	try {
			Thread.sleep(2);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
    	
    	new Thread(){
    		public void run(){
    			try {
					peer.server();
				} catch (IOException e) {
					e.printStackTrace();
				}
    		}
    	}.start();
    	
    	new Thread(){
    		public void run(){
    			try {
					peer.income();
				} catch (IOException e) {
					e.printStackTrace();
				}
    		}
    	}.start();
    	
    	String fileName = args[2];
    	//File folderDownload= new File(fileName);
    	//ArrayList<String> fileNamesDownload = Util.listFilesForFolder(folderDownload);
    	
    	int numRequests = 100;
    	
    	try{
    		numRequests = Integer.parseInt(args[3]);
    	} catch (Exception e){
    		System.out.println("Pon un numero de puerto valido");
    	}
    	
    	//sendRequests(peer, fileNamesDownload, numRequests);
    	sendRequests(peer, fileName, numRequests);
    }
}