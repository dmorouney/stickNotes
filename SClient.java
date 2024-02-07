import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JLabel;
import java.awt.FlowLayout;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.JTextArea;
import java.awt.Font;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import java.io.BufferedReader;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.IOException;

import java.net.*;
import java.io.*;
import java.util.Scanner;


public class SClient {
    /********************************************************************
     * Main class will launch program.  GUI and other functionality encap-
     * sulated within this main structure.
     ********************************************************************/
   
    
    private Socket clientSocket;
    private BufferedReader input;
    private PrintWriter output;
    
    /**
     * Launch the application.
     */
    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    ClientWindow window = new ClientWindow();
                    window.frmClientserverGui.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
    private static class Note {
        /****************************************************************
         * Main Structure to hold and retrieve notes. 
         ****************************************************************/
        private int x, y, h, w, color;
        private int[] msg;

        public Note(int x, int y, int h, int w, int color, int[] msg) {
            /* Make a note given a message as a char6 integer list */
            /* this is used when we read notes from the server */
            this.x = x;
            this.y = y;
            this.h = h;
            this.w = w;
            this.color = color;
            this.msg = msg;
        }

        public Note(int x, int y, int h, int w, int color, String msg) {
            /* make a note given a message string */
            /* this is used when we make notes on the client */
            this.x = x;
            this.y = y;
            this.h = h;
            this.w = w;
            this.color = color;
            this.msg = Char6.chars2Ints(msg.toCharArray());
        }

        public int[] notePackets() {
            /* give note packets to send note to server. */
            /* Note: This includes the command code */
            int numPackets = 2 + msg.length;
            int[] response = new int[numPackets + 1];
            int i = 0;
            response[i++] = (0b001 << 29) | (color << 24) | (numPackets & 0xFFFF);
            response[i++] = (x << 16) | y;
            response[i++] = (h << 16) | w;
            for (int packet : msg) {
                response[i++] = packet;
            }
            return response;
        }

        public String noteMessage() {
            /* Given a note from the server which will be send in char6 format.
               convert the given message to plain english in the form of a
               String.
            */
            char[] converted = Char6.ints2Chars(msg);
            String message = new String(converted);
            return message;
        }
    };

    public static class ClientConnection {
        private final static int HIGH_16  = 0xFFFF0000;
        private final static int LOW_16   = 0x0000FFFF;
        private int maxWidth, maxHeight;
        private int port;
        private Socket socket;
        private int defaultColor;
        private int colorMask;
        

        public ClientConnection(String ip, int port) throws UnknownHostException, IOException {
            this.maxHeight = 0;
            this.maxWidth = 0;
            this.socket = new Socket(ip,port);
            DataInputStream in = new DataInputStream(socket.getInputStream());
            
            int packet = in.readInt();
            assert(packet == 0xE0000000);
            packet = in.readInt();
            this.colorMask = (packet & 0xFF000000) >>> 24;
            this.defaultColor = (packet & 0x00FF0000) >>> 16;
            packet = in.readInt();
            this.maxHeight = (packet & HIGH_16) >>> 16;
            this.maxWidth  = packet & LOW_16;
            
        }

        public void send(int packet) throws IOException {
        	DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            try {
				out.writeInt(packet);
			} catch (IOException e) {
				
				e.printStackTrace();
			}
        }

        public void sendAll(int[] packets) throws IOException {
        	DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            for (int packet : packets) {
				out.writeInt(packet);
            }
        }

        public int read() throws IOException {
        	DataInputStream in = new DataInputStream(socket.getInputStream());
            try {
				return in.readInt();
			} catch (IOException e) {
				
				e.printStackTrace();
			}
            return 0;
        }

        public void close() {
                try {
                socket.close();
            } catch (IOException e) {}
        }

    };

    public static class Commands {
        private final static int GET        = 0b0100_0000_0000_0000_0000_0000_0000_0000;
        private final static int PIN        = 0b0110_0000_0000_0000_0000_0000_0000_0000;
        private final static int UNPIN      = 0b1000_0000_0000_0000_0000_0000_0000_0000;
        private final static int CLEAR      = 0b1010_0000_0000_0000_0000_0000_0000_0000;
        private final static int HIGH_16    = 0b1111_1111_1111_1111_0000_0000_0000_0000;
        private final static int LOW_16     = 0b0000_0000_0000_0000_1111_1111_1111_1111;
        
        private final static int COLOR_RED    = 0b0000_0001;
        private final static int COLOR_BLUE   = 0b0000_0010;
        private final static int COLOR_YELLOW = 0b0000_0100;
        private final static int COLOR_GREEN  = 0b0000_1000;
        private final static int COLOR_BLACK  = 0b0001_0000;
        private final static int COLOR_WHITE  = 0b0010_0000;
        private final static int WHITE_MASK = 0b0000_0000;
        
        
        public static int[] pin(int x, int y) {
            assert (x < 0xFFFF && y < 0xFFFF);
            int[] request = new int[2];
            request[0] = PIN | 0b1 << 16;
            assert (x<=LOW_16 && y <= LOW_16);
            request[1] = (x << 16) | y;
            
            return request; 
        }

        public static int[] unpin(int x, int y) {
            assert (x < 0xFFFF && y < 0xFFFF);
            int[] request = new int[2];
            request[0] = UNPIN | 0b1 << 16;
            assert (x<=0xFFFF && y <=0xFFFF);
            request[1] = (x << 16) | y;
            
            return request; 
        }

        
        public static int[] getNotes(String x, String y, String color, String refers) {
            int req = 0;
            int mPackets = 1;
            if (!x.equals("") && !y.equals("")) {
                req |= 0b1;
                mPackets += 2;
            }
            if (!refers.equals("")) {
                req |= 0b100;
                mPackets += 2;
            }
            if (!color.equals("")) {
                req |= 0b1000;
                mPackets += (refers.length() / 5) + 1;
            }
            int[] response = new int[mPackets+1];
            int i = 0;
            if (req == 0) {
                response[i] = (GET << 29) | (req << 26) | (mPackets << 24);
            } else {
                response[i++] = (GET << 29) | (req << 26) | (mPackets << 24);
            }
            if ((req & 0b1) != 0) {
                int ix = Integer.parseInt(x);
                int iy = Integer.parseInt(y);
                response[i++] = (0b0001 << 28) | (ix << 24);
                response[i++] = (iy << 16);
            }

            if ((req & 0b100) != 0) {
                char[] chars = refers.toCharArray();
                int[] ints = Char6.chars2Ints(chars);
                for (int d : ints) {
                    response[i++] = d;
                }
            }

            if ((req & 0b1) != 0) {
                int iColor = getColor(color);
                response[i] = (0b0001 << 28) | (iColor << 24);
            }

            return response;

        }

        private static String getColorCode(int color)
        {
            if (color == WHITE_MASK) {
                return "white";
            } else if (color == COLOR_RED) {
                return "red";
            } else if (color == COLOR_BLUE) {
                return "blue";
            } else if (color == COLOR_GREEN) {
                return "green";
            } else if (color == COLOR_YELLOW) {
                return "yellow";
            } else if (color == COLOR_BLACK) {
                return "red";
            } else {
                return "unknown";
            }
        }

        public static String[] readNotes(int packet, ClientConnection cc) throws IOException {
            assert (((packet & 0xF0000000) >>> 28) == 0b1010);
            int numNotes = packet & 0xFFFF;

            String[] notes = new String[numNotes];
            for (int i = 0; i < numNotes; i++) {
                packet = cc.read();
                String x = String.valueOf((packet & HIGH_16) >>> 16);
                String y = String.valueOf(packet & LOW_16);
                packet = cc.read();
                String h = String.valueOf((packet & HIGH_16) >>> 16);
                String w = String.valueOf(packet & LOW_16);
                packet = cc.read();
                int iColor = (packet & HIGH_16) >>> 16;
                String color = getColorCode(iColor);
                int charPackets2Read = packet & LOW_16;
                int[] packets = new int[charPackets2Read];
                for (int j = 0; j < charPackets2Read; j++) {
                    packets[j] = cc.read();
                }
                String message = new String(Char6.ints2Chars(packets));
                notes[i] = "[ X=" + x + ", Y=" + y + ", H=" + h + ", W=" + w + ", Color=" + color + ", Msg="
                        + message + ".\n";
            }
            return notes;
        }
        

        public static int[] post(int x, int y, int h, int w, int color, String msg) {
            Note n = new Note(x, y, h, w, color, msg);
            int[] request = n.notePackets();

            return request;
        }

        public static int[] clear(int x, int y) {
            int[] request = new int[2];
            request[0] = CLEAR;
            request[1] = (x << 16) | y;
            return request;

        }

        public static int[] error(int errorCode) {
            //TODO

            return null;
        }

        public static int[] data2Ints(String data) {
            String[] numbers = data.split(" ");
            assert (numbers.length == 2);
            int[] response = new int[2];
            response[0] = Integer.parseInt(numbers[0]);
            response[1] = Integer.parseInt(numbers[1]);
            return response;
        }

        public static int getColor(String s)
        {
            int color = 0;
            if(s.equals("white")){
                color = COLOR_WHITE;
            } else if (s.equals("red")) {
                color = COLOR_RED;
            } else if (s.equals("blue")) {
                color = COLOR_BLUE;
            } else if (s.equals("yellow")) {
                color = COLOR_YELLOW;
            } else if (s.equals("green")) {
                color = COLOR_GREEN;
            } else if (s.equals("black")) {
                color = COLOR_BLACK;
            }
            return color;
        }
        
    };

    public static class Char6 {
        private static final int[] CHAR_TBL = { 0, 0x20, 0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29, 0x2A, 0x2B,
                0x2C, 0x2D, 0x2E, 0x2F, 0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x3A, 0x3B, 0x3C, 0x3D,
                0x3E, 0x3F, 0x40, 0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48, 0x49, 0x4A, 0x4B, 0x4C, 0x4D, 0x4E, 0x4F,
                0x50, 0x51, 0x52, 0x53, 0x54, 0x55, 0x56, 0x57, 0x58, 0x59, 0x5A, 0x5C, 0x5E, 0x5F, 0x10 
        };
        
        public static char int2Char(int i) {
            // check if number is negative or out of bounds 
            if ((i & 0x80000000) != 0 || i > 0x3F) {
                throw new ArithmeticException("int2Char passed invalid value");
            }
            char c = (char) CHAR_TBL[i];
            return c;
        }

        public static int char2Int(char c) {
            int i = (int) c;
            if (i == 0x10) {
                return 0x3F;
            } else if (i >= 0x20 && i <= 0x5A) {
                return (i - 0x20) + 1;
            } else if (i >= 0x61 && i <= 0x7A) {
                return (i - 0x61) + 0x22;
            } else if (i == 0x5C) {
                return 0x3C;
            } else if (i == 0x5E) {
                return 0x3D;
            } else if (i == 0x5F) {
                return 0x3E;
            } else {
                return 0x00;
            }

        }

        public static int[] chars2Ints(char[] chars) {
            int[] ints = new int[(chars.length)/5 + 1];

            for (int i = 0, j = 0; i < chars.length && j < ints.length; i++) {
                int tmp = i % 5;
                int cint = char2Int(chars[i]);

                if (cint != 0) ints[j] |= cint << (6 * (tmp));
                if (tmp == 0) j++;
            }

            return ints;    

        }

        public static char[] ints2Chars(int[] ints) {
            char[] chars = new char[ints.length * 5];
            for (int i = 0; i < ints.length; i++) {
                int justChars = ints[i] >>> 2;
                for (int j = 0; j < 6; j++) {
                    chars[(5 * i) + j] = int2Char((justChars >>> (5*(6-j))) & 0b11_1111);
                }
            }
            return chars;
        }
    };


 /*=====================================================================================
    GARBAGE CODE FOR GUI CREATED BY ECLIPSE 
 =====================================================================================*/
    public static class ClientWindow {
        private JFrame frmClientserverGui;
        private JTextField txtIP;
        private JTextField txtPort;
        private JTextField txtXCo;
        private JTextField txtYCo;
        private JTextField txtColour;
        private JTextField txtXY;
        private JTextField txtRefers;
        private JTextField txtHW;
        private JTextField txtNoteInput;
        private ClientConnection cc;

        public ClientWindow(){
            initialize();
        }
        private void initialize() {
            frmClientserverGui = new JFrame();
            frmClientserverGui.setTitle("Client/Server GUI");
            frmClientserverGui.setBounds(100, 100, 593, 460);
            frmClientserverGui.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            
            JButton btnDisconnect = new JButton("Disconnect");
            btnDisconnect.setEnabled(false);
            
            txtXCo = new JTextField();
            txtXCo.setColumns(10);
            
            txtYCo = new JTextField();
            txtYCo.setColumns(10);
            JLabel lblX = new JLabel("X:");
            lblX.setFont(new Font("Tahoma", Font.BOLD, 11));
            
            JLabel lblY = new JLabel("Y:");
            lblY.setFont(new Font("Tahoma", Font.BOLD, 11));
            
            JButton btnPin = new JButton("Pin");
            
            btnPin.setEnabled(false);
            
            JButton btnUnpin = new JButton("Unpin");
            
            btnUnpin.setEnabled(false);
            
            JTextArea txtareaInput = new JTextArea();
            
            JButton btnPost = new JButton("Post");
            btnPost.setEnabled(false);
            
            JLabel lblNoteInput = new JLabel("Note Input");
            lblNoteInput.setFont(new Font("Tahoma", Font.BOLD, 11));
            
            JLabel lblGetInputs = new JLabel("Get Inputs");
            lblGetInputs.setFont(new Font("Tahoma", Font.BOLD, 11));
            
            txtColour = new JTextField();
            txtColour.setColumns(10);
            
            txtXY = new JTextField();
            txtXY.setColumns(10);
            
            JLabel lblColour = new JLabel("Colour:");
            
            JLabel lblXy = new JLabel("X,Y:");
            
            txtRefers = new JTextField();
            txtRefers.setColumns(10);
            
            JLabel lblRefersTo = new JLabel("Refers To:");
            
            JButton btnGetNotes = new JButton("Get Notes");
            btnGetNotes.setEnabled(false);
            
            JTextArea txtareaResult = new JTextArea();
            
            JLabel lblResult = new JLabel("Result");
            lblResult.setFont(new Font("Tahoma", Font.BOLD, 11));
            
            JLabel lblIpAddress = new JLabel("IP Address:");
            lblIpAddress.setFont(new Font("Tahoma", Font.BOLD, 11));
            
            JLabel lblPort = new JLabel("Port:");
            lblPort.setFont(new Font("Tahoma", Font.BOLD, 11));
            
            txtIP = new JTextField();
            txtIP.setColumns(10);
            
            txtPort = new JTextField();
            txtPort.setColumns(10);
            
            JButton btnConnect = new JButton("Connect");
            JButton btnClear = new JButton("Clear");
            
            btnClear.setEnabled(false);
            
            JLabel lblHW = new JLabel("Height,Weight:");
            
            txtHW = new JTextField();
            txtHW.setColumns(10);
    
           
        //--------------------------- BUTTONS--------------------------------------------
        
            btnConnect.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                
                String address;
                String portstr;
                int port;
                
                address = txtIP.getText();
                portstr = txtPort.getText();
                port = Integer.parseInt(portstr);
                
                try {
					cc = new ClientConnection(address, port);
				} catch (UnknownHostException e) {
					e.printStackTrace();
				} catch (IOException e) {	
					e.printStackTrace();
				}
                btnDisconnect.setEnabled(true);
                btnPin.setEnabled(true);
                btnUnpin.setEnabled(true);
                btnPost.setEnabled(true);
                btnClear.setEnabled(true);
                btnGetNotes.setEnabled(true);
                }
            });
                
                
                
            btnPost.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent arg0) {
                    int x = Integer.parseInt(txtXCo.getText());
                    int y = Integer.parseInt(txtYCo.getText());
                    int[] hw = Commands.data2Ints(txtHW.getText());
                    String message = txtNoteInput.getText();
                    int color = Commands.getColor(txtColour.getText().replaceAll("\\s+", ""));
                    int[] response = Commands.post(x, y, hw[0], hw[1], color, message);
                    assert (response.length > 0);
                    try {
						cc.sendAll(response);
					} catch (IOException e) {
						e.printStackTrace();
					}
                }
            });

            btnGetNotes.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent arg0) {
                    int[] request = Commands.getNotes(txtXCo.getText(), txtYCo.getText(), txtRefers.getText(), txtColour.getText());
                    try {
						cc.sendAll (request);
					} catch (IOException e) {
						e.printStackTrace();
					}
                    int packet;
                    String[] notes;
					try {
						packet = cc.read();
						notes = Commands.readNotes(packet, cc);
						txtareaResult.removeAll();
	                    for(String note: notes) {
	                        txtareaResult.append(note);
	                    }
					} catch (IOException e) {
						e.printStackTrace();
					}
                    
                }
            });
            

            btnDisconnect.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent arg0) {
                    cc.close();
                    
                }
            });
            
            
            btnPin.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent arg0) {
                int x = Integer.parseInt(txtXCo.getText());
                int y = Integer.parseInt(txtYCo.getText());    
                int[] request = Commands.pin(x, y);
                int response = 0;
                try {
					cc.sendAll(request);
				} catch (IOException e) {			
					e.printStackTrace();
				}
                try {
					response = cc.read();
				} catch (IOException e) {	
					e.printStackTrace();
				}
                    assert (response != 0);
                }
            });
        
            btnUnpin.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent arg0) {
                int x = Integer.parseInt(txtXCo.getText());
                int y = Integer.parseInt(txtYCo.getText());              
                int[] request = Commands.unpin(x, y);
                int response = 0;
                try {
					cc.sendAll(request);
				} catch (IOException e) {
					e.printStackTrace();
				}
                try {
					response = cc.read();
				} catch (IOException e) {
					e.printStackTrace();
                }
                    assert (response != 0);
                }
            });
        
            btnClear.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent arg0) {
                    int x = Integer.parseInt(txtXCo.getText());
                    int y = Integer.parseInt(txtYCo.getText());              
                    int[] request = Commands.clear(x, y);
                    int response = 0;
                    try {
						cc.sendAll(request);
					} catch (IOException e) {
						
						e.printStackTrace();
					}
                    try {
						response = cc.read();
					} catch (IOException e) {
						
						e.printStackTrace();
					}

                    assert (response != 0);
                }
            });
            
            txtNoteInput = new JTextField();
            txtNoteInput.setColumns(10);
            
        
                    
            //---------------------LAYOUT CODE (DESIGN GENERATED)----------------------------------------
                
            
            GroupLayout groupLayout = new GroupLayout(frmClientserverGui.getContentPane());
            groupLayout.setHorizontalGroup(
                groupLayout.createParallelGroup(Alignment.LEADING)
                    .addGroup(groupLayout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
                            .addGroup(groupLayout.createSequentialGroup()
                                .addGroup(groupLayout.createParallelGroup(Alignment.TRAILING)
                                    .addComponent(txtareaInput, Alignment.LEADING, GroupLayout.PREFERRED_SIZE, 181, GroupLayout.PREFERRED_SIZE)
                                    .addGroup(groupLayout.createSequentialGroup()
                                        .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
                                            .addComponent(lblGetInputs)
                                            .addComponent(lblNoteInput)
                                            .addGroup(groupLayout.createSequentialGroup()
                                                .addGroup(groupLayout.createParallelGroup(Alignment.TRAILING)
                                                    .addComponent(lblColour)
                                                    .addComponent(lblXy)
                                                    .addComponent(lblHW)
                                                    .addComponent(lblRefersTo))
                                                .addGap(18)
                                                .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
                                                    .addComponent(txtHW, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                    .addComponent(btnGetNotes)
                                                    .addGroup(groupLayout.createParallelGroup(Alignment.TRAILING)
                                                        .addComponent(txtColour, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(txtRefers, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(txtXY, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))))
                                            .addComponent(txtNoteInput, GroupLayout.PREFERRED_SIZE, 211, GroupLayout.PREFERRED_SIZE))
                                        .addPreferredGap(ComponentPlacement.RELATED, 77, Short.MAX_VALUE)
                                        .addGroup(groupLayout.createParallelGroup(Alignment.TRAILING)
                                            .addGroup(groupLayout.createSequentialGroup()
                                                .addComponent(lblResult)
                                                .addGap(215))
                                            .addGroup(groupLayout.createSequentialGroup()
                                                .addComponent(txtareaResult, GroupLayout.PREFERRED_SIZE, 251, GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(ComponentPlacement.RELATED)))))
                                .addGap(28))
                            .addGroup(groupLayout.createSequentialGroup()
                                .addComponent(btnConnect)
                                .addPreferredGap(ComponentPlacement.RELATED)
                                .addComponent(btnDisconnect)
                                .addContainerGap(403, Short.MAX_VALUE))
                            .addGroup(groupLayout.createSequentialGroup()
                                .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
                                    .addGroup(groupLayout.createSequentialGroup()
                                        .addGap(37)
                                        .addComponent(lblPort))
                                    .addComponent(lblIpAddress))
                                .addPreferredGap(ComponentPlacement.UNRELATED)
                                .addGroup(groupLayout.createParallelGroup(Alignment.LEADING, false)
                                    .addComponent(txtIP)
                                    .addComponent(txtPort, GroupLayout.PREFERRED_SIZE, 166, GroupLayout.PREFERRED_SIZE))
                                .addGap(47)
                                .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
                                    .addComponent(lblX)
                                    .addComponent(lblY))
                                .addPreferredGap(ComponentPlacement.RELATED)
                                .addGroup(groupLayout.createParallelGroup(Alignment.LEADING, false)
                                    .addGroup(groupLayout.createSequentialGroup()
                                        .addComponent(txtXCo, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addGap(18)
                                        .addComponent(btnPin, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                    .addGroup(groupLayout.createSequentialGroup()
                                        .addComponent(txtYCo, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addGap(18)
                                        .addComponent(btnUnpin)))
                                .addPreferredGap(ComponentPlacement.RELATED)
                                .addComponent(btnClear)
                                .addContainerGap(40, Short.MAX_VALUE))
                            .addGroup(groupLayout.createSequentialGroup()
                                .addComponent(btnPost)
                                .addContainerGap(514, Short.MAX_VALUE))))
            );
            groupLayout.setVerticalGroup(
                groupLayout.createParallelGroup(Alignment.LEADING)
                    .addGroup(groupLayout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
                            .addGroup(groupLayout.createSequentialGroup()
                                .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                                    .addComponent(txtXCo, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                    .addComponent(lblX)
                                    .addComponent(btnPin)
                                    .addComponent(btnClear))
                                .addPreferredGap(ComponentPlacement.RELATED)
                                .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                                    .addComponent(txtYCo, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                    .addComponent(lblY)
                                    .addComponent(btnUnpin)))
                            .addGroup(groupLayout.createSequentialGroup()
                                .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                                    .addComponent(txtIP, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                    .addComponent(lblIpAddress))
                                .addPreferredGap(ComponentPlacement.RELATED)
                                .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                                    .addComponent(txtPort, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                    .addComponent(lblPort))))
                        .addPreferredGap(ComponentPlacement.UNRELATED)
                        .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                            .addComponent(btnConnect)
                            .addComponent(btnDisconnect))
                        .addGap(13)
                        .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                            .addComponent(lblGetInputs)
                            .addComponent(lblResult))
                        .addPreferredGap(ComponentPlacement.RELATED, 15, Short.MAX_VALUE)
                        .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                            .addComponent(txtareaResult, GroupLayout.PREFERRED_SIZE, 238, GroupLayout.PREFERRED_SIZE)
                            .addGroup(groupLayout.createSequentialGroup()
                                .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                                    .addComponent(txtColour, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                    .addComponent(lblColour))
                                .addGap(10)
                                .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                                    .addComponent(txtXY, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                    .addComponent(lblXy))
                                .addPreferredGap(ComponentPlacement.RELATED)
                                .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                                    .addComponent(lblRefersTo)
                                    .addComponent(txtRefers, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                .addGap(5)
                                .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                                    .addComponent(lblHW)
                                    .addComponent(txtHW, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                .addGap(17)
                                .addComponent(btnGetNotes)
                                .addGap(10)
                                .addComponent(lblNoteInput)
                                .addGap(7)
                                .addComponent(txtNoteInput, GroupLayout.PREFERRED_SIZE, 54, GroupLayout.PREFERRED_SIZE)))
                        .addPreferredGap(ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnPost)
                        .addGap(29)
                        .addComponent(txtareaInput, GroupLayout.PREFERRED_SIZE, 73, GroupLayout.PREFERRED_SIZE)
                        .addGap(23))
            );
            frmClientserverGui.getContentPane().setLayout(groupLayout);
        }
    };
};
