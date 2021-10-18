package client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import bench.BenchRegistry;
import util.PeerQueue;
import util.Util;
import vista.VistaCliente;

public class Peer {
	
	private int peerId;
	private int numFiles;
	private ArrayList<String> fileNames;
	private String directory;
	private String address;
	private int port;
	private PeerQueue<Connection> peerQueue;
	private int numThreads = 4;
	public ServerSocket serverSocket;
        private VistaCliente vista;
	
	public Peer(String directory, ArrayList<String> fileNames, int numFiles, String address, int port ) throws IOException{
		this.directory = directory;
		this.fileNames = fileNames;
		this.numFiles = numFiles;
		this.address = address;
		this.port = port;
		
		peerQueue = new PeerQueue<Connection>();
	}
        
        
	
	//getters
		public int getPeerId(){
			return peerId;
		}
		
		public int getNumFiles(){
			return numFiles;
		}
		
		public ArrayList<String> getFileNames(){
			return fileNames;
		}
		
		public String getDirectory(){
			return directory;
		}
		
		public String getAddress(){
			return address;
		}
		
		public int getPort(){
			return port;
		}
		
		//setters
		public void setPeerId(int peerId){
			this.peerId = peerId;
		}
		
		public void setNumFiles(int numFiles){
			this.numFiles = numFiles;
		}
		
		public void setFileNames(ArrayList<String> fileNames){
			this.fileNames.addAll(fileNames);
		}
		
		public void addFileName(String fileName){
			this.fileNames.add(fileName);
		}
		
		public void setDirectory(String directory){
			this.directory = directory;
		}
		
		public void setAddress(String address){
			this.address = address;
		}
		
		public void setPort(int port){
			this.port = port;
		}
    
    public void register(Socket socket) throws IOException {
    	
    	//System.out.println("Connecting to the server...");
    	long start = System.currentTimeMillis();
    	DataOutputStream dOut = new DataOutputStream(socket.getOutputStream());
    	
    	//Opción de registro en el servidor  (new peer)
    	dOut.writeByte(0);
    	dOut.flush();
    
    	//Número de archivos
    	dOut.writeByte(1);
    	dOut.writeInt(numFiles);
    	dOut.flush(); 
    	//Nombres de archivos
    	dOut.writeByte(2);
    	for(String str : fileNames)
    		dOut.writeUTF(str);
    	dOut.flush();
    	dOut.writeByte(3);
    	dOut.writeUTF(directory);
    	dOut.flush();
    	dOut.writeByte(4);
    	dOut.writeUTF(address);
    	dOut.flush();
    	dOut.writeByte(5);
    	dOut.writeInt(port);
    	dOut.flush();
    	dOut.writeByte(-1);
    	dOut.flush();
    	
    	
    	//Lectura del Id único del servidor
    	DataInputStream dIn = new DataInputStream(socket.getInputStream());
    	this.peerId = dIn.readInt();
    	
    	dOut.close();
    	dIn.close();
    	socket.close();
    	
    	System.out.println("Corriendo como Peer " + peerId + "! " + "tomó " + (System.currentTimeMillis() - start) + "ms para el registro");
    	//System.out.println("Took " + (System.currentTimeMillis() - start) + " ms to register in the server.");
    	//System.out.println((System.currentTimeMillis() - start) + " ms");
    	if(BenchRegistry.times != null) BenchRegistry.times.add((System.currentTimeMillis() - start));
	}

    public String[] lookup(String fileName, Socket socket, int count) throws IOException{
    	DataOutputStream dOut = new DataOutputStream(socket.getOutputStream());
    	
    	//Opcion de buscar un archivo
    	dOut.writeByte(1);
    	
    	String [] peerAddress = new String[0];
    	
    	//Nombre del archivo
    	dOut.writeUTF(fileName);
    	dOut.flush();
    	//System.out.println("Reading from the server...");
    	
    	System.out.println("Peer " + peerId + " - buscando el archivo. (" + count + ")");
    	
    	//Lectura de la dirección del peer que tiene el archivo
    	DataInputStream dIn = new DataInputStream(socket.getInputStream());
    	byte found = dIn.readByte();
    	
    	if(found == 1){
    		int qt = dIn.readInt();
    		
    		peerAddress = new String[qt];
    		
    		for(int i = 0; i < qt; i++){
    			try{
    			 peerAddress[i] = dIn.readUTF();
    			}catch (EOFException e){
    				i--;
    			}
    			String paddress[] = peerAddress[i].split(":");
    			System.out.println("Peer " + paddress[2] + " - " + paddress[0] +":" + paddress[1] + " tiene el archivo " + fileName + "! - Buscado por el Peer " + peerId);
    		}
    	} else if(found == 0){
    		System.out.println("Archivo no encontrado en el sistema");
    		peerAddress = new String[0];
    	}
    	
    	dOut.close();
    	dIn.close();
    	socket.close();
    	return peerAddress;
    }
    
    public void server() throws IOException{
    	
    	try {
    		serverSocket = new ServerSocket(port);
    	} catch(Exception e){
    		return;
    	}
		
		while(true){
			Socket socket = serverSocket.accept();
			synchronized(peerQueue){
				peerQueue.add(new Connection(socket,directory));
			}
			/*try {
				Thread.sleep(2);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}*/
		}
		
	}
    
    public void income() throws IOException{
		
		ExecutorService executor = Executors.newFixedThreadPool(numThreads);

		while(true){
			if(peerQueue.peek() == null){
				try {
					Thread.sleep(1);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				continue;
			}
			synchronized(peerQueue){
				//System.out.println("Added to executor");
				Connection c = peerQueue.poll();
				Server s = new Server(c.getSocket(), c.getDirectory());
				executor.execute(s);
			}
		}
		
	}
    
    public void download(String peerAddress, int port, String fileName, int i)  throws IOException {
    	Socket socket = new Socket(peerAddress, port);
    	DataOutputStream dOut = new DataOutputStream(socket.getOutputStream());
    	dOut.writeUTF(fileName);
        InputStream in = socket.getInputStream();
        
        String folder = directory + "/";
        File f = new File(folder);
        Boolean created = false;
        if (!f.exists()){
        	try {
        		created = f.mkdirs();
        	}catch (Exception e){
        		System.out.println("No se ha podido crear la carpeta, ¡el archivo se guardará en el directorio actual!");
        	}
        }else {
        	created = true;
        }
        
        if(i != -1) fileName = fileName + i;
        
        OutputStream out = (created) ? new FileOutputStream(f.toString() + "/" + fileName) : new FileOutputStream(fileName);
        Util.copy(in, out);
        System.out.println("Archivo " + fileName + " recibido del peer " + peerAddress + ":" + port);
        dOut.close();
        out.close();
        in.close();
        socket.close();
    }
    
    
}

	
