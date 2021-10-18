
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

import util.Util;
import vista.VistaCliente;

public class Client extends Thread {

    private VistaCliente vista;
    private String dir;
    private String port;
    private String serverAddress;
    private int serverPort;
    private Peer peer;
    private String fileName;
    private static String[] peerAddress = new String[0];

    public Client(VistaCliente vista) {
        this.vista = vista;
        
    }

    public Client() {
    }

    public void setDatos(String dir, String port) {
        this.dir = dir;
        this.port = port;
    }

    public void conectarClient() throws IOException {

        if (dir == null) {
            vista.entrada("Debe ser java client/Client folder port");
            return;
        }

        //informacion del server
        serverAddress = "localhost";
        serverPort = 6756;

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
        peer = new Peer(dir, fileNames, fileNames.size(), address, portLocal, this.vista);
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

        vista.conectado(true);
        vista.cambiaTitulo("----CLIENTE " + peer.getPeerId() + "----");
        vista.entrada("\n\nPara descargar:");
        vista.entrada("1 - Buscar un archivo");
        vista.entrada("2 - Descargar el archivo");

    }

    public void buscarArchi() throws IOException {
        vista.limpiarPeer();
        vista.entrada("holi laura");
        if (vista.getTxtArchi().length() > 0) {
            peerAddress = new String[0];
            fileName = vista.getTxtArchi().trim();
            peerAddress = peer.lookup(fileName, new Socket("localhost", 6756), 1);
            if (peerAddress.length > 1) {
                vista.entrada("Selecciona de que peer deseas descargar el archivo:");
                for (int i = 0; i < peerAddress.length; i++) {
                    String[] addrport = peerAddress[i].split(":");
                    vista.entrada((i + 1) + " - " + addrport[0] + ":" + addrport[1]);
                    vista.addPeer(i);
                }
            }
        } else {
            vista.entrada("Introduzca el nombre del archivo a buscar");
        }
    }

    public void descargarArchi() throws IOException {
        int optpeer;

        if (peerAddress.length == 0) {
            vista.entrada("Busca primero el peer");
        } else if (peerAddress.length == 1 && Integer.parseInt(peerAddress[0].split(":")[2]) == peer.getPeerId()) {
            vista.entrada("Este peer ya tiene el archivo, por lo tanto no lo descarga.");
        } else if (peerAddress.length == 1) {
            String[] addrport = peerAddress[0].split(":");
            vista.entrada("Descargando desde el peer " + addrport[2] + ": " + addrport[0] + ":" + addrport[1]);
            peer.download(addrport[0], Integer.parseInt(addrport[1]), fileName, -1);
            vista.entrada("\n\nPara descargar:");
            vista.entrada("1 - Buscar un archivo");
            vista.entrada("2 - Descargar el archivo");
        } else {
            optpeer = vista.getPeerNum();
            if (optpeer > peerAddress.length || optpeer < 1) {
                vista.entrada("Selecciona una Peer valida:");
                return;
            }
            String[] addrport = peerAddress[optpeer - 1].split(":");
            peer.download(addrport[0], Integer.parseInt(addrport[1]), fileName, -1);
            vista.entrada("\n\nPara descargar:");
            vista.entrada("1 - Buscar un archivo");
            vista.entrada("2 - Descargar el archivo");
        }
    }

    public void desconect() {

        vista.entrada("Peer desconectado!");
        vista.conectado(false);
        this.stop();
        return;
    }

    @Override
    public void run() {
        vista.entrada("Estableciendo conexion. Por favor espere...");
        try {
            conectarClient();
        } catch (IOException ioe) {
            //JOptionPane.showMessageDialog(vista,"Excepcion inesperada: " + ioe.getMessage());

        }
    }
}
