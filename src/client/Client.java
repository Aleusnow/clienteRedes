
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author Amelia Wolf
 */
package client;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

import util.Util;
import vista.VistaCliente;

public class Client {
    
    private VistaCliente vista; 

    public Client(VistaCliente vista) {
        this.vista = vista;
    }

    public Client() {
    }
    

    public void conectarClient(String dir, String port) throws IOException {

        if (dir == null) {
            vista.entrada("Debe ser java client/Client folder port");
            return;
        }

        //informacion del server
        String serverAddress = "localhost";
        int serverPort = 6756;

        File folder = new File(dir);
        int option;
        String fileName = null;

        if (!folder.isDirectory()) {
            vista.entrada("Pon un nombre de directorio valido");
            return;
        }

        //Util.getExternalIP();
        String address = InetAddress.getLocalHost().getHostAddress();

        int portLocal = 3434;
        if (port.length() == 4) {
            try {
                portLocal = Integer.parseInt(port);
            } catch (Exception e) {
                vista.entrada("Pon un numero de puerto valido");
            }
        }
        
        ArrayList<String> fileNames = Util.listFilesForFolder(folder);
        final Peer peer = new Peer(dir, fileNames, fileNames.size(), address, portLocal);
        Socket socket = null;
        try {
            socket = new Socket(serverAddress, serverPort);
        } catch (IOException e) {
            vista.entrada("No hay ninguna instancia de servidor en funcionamiento. Inicia una primero!");
            return;
        }
        peer.register(socket);

        new Thread() {
            public void run() {
                try {
                    peer.server();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();

        new Thread() {
            public void run() {
                try {
                    peer.income();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();

        String[] peerAddress = new String[0];

        Scanner scanner = new Scanner(System.in);
        while (true) {
            vista.entrada("\n\nSelecciona la opcion:");
            vista.entrada("1 - Buscar un archivo");
            vista.entrada("2 - Descargar un archivo");

            option = scanner.nextInt();
            int optpeer;

            if (option == 1) {
                System.out.println("Introduzca el nombre del archivo:");
                fileName = scanner.next();
                peerAddress = peer.lookup(fileName, new Socket(serverAddress, serverPort), 1);
            } else if (option == 2) {
                if (peerAddress.length == 0) {
                    vista.entrada("Busca primero el peer");
                } else if (peerAddress.length == 1 && Integer.parseInt(peerAddress[0].split(":")[2]) == peer.getPeerId()) {
                    vista.entrada("Este peer ya tiene el archivo, por lo tanto no lo descarga.");
                } else if (peerAddress.length == 1) {
                    String[] addrport = peerAddress[0].split(":");
                    vista.entrada("Descargando desde el peer " + addrport[2] + ": " + addrport[0] + ":" + addrport[1]);
                    peer.download(addrport[0], Integer.parseInt(addrport[1]), fileName, -1);
                } else {
                    vista.entrada("Selecciona de que peer deseas descargar el archivo:");
                    for (int i = 0; i < peerAddress.length; i++) {
                        String[] addrport = peerAddress[i].split(":");
                        System.out.println((i + 1) + " - " + addrport[0] + ":" + addrport[1]);
                    }
                    optpeer = scanner.nextInt();
                    while (optpeer > peerAddress.length || optpeer < 1) {
                        vista.entrada("Selecciona una opcion valida:");
                        optpeer = scanner.nextInt();
                    }
                    String[] addrport = peerAddress[optpeer - 1].split(":");
                    peer.download(addrport[0], Integer.parseInt(addrport[1]), fileName, -1);
                }
            } else {
                scanner.close();
                vista.entrada("Peer desconectado!");
                return;
            }

        }
    }
}
