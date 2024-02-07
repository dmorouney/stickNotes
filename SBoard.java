import java.io.BufferedReader;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.IOException;
//import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;



public class SBoard {
    private final static int N_THREADS  = 32;
    private final static int PING       = 0b0000_0000_0000_0000_0000_0000_0000_0000;
    private final static int POST       = 0b0010_0000_0000_0000_0000_0000_0000_0000;
    private final static int GET        = 0b0100_0000_0000_0000_0000_0000_0000_0000;
    private final static int PIN        = 0b0110_0000_0000_0000_0000_0000_0000_0000;
    private final static int UNPIN      = 0b1000_0000_0000_0000_0000_0000_0000_0000;
    private final static int CLEAR      = 0b1010_0000_0000_0000_0000_0000_0000_0000;
    private final static int ERROR      = 0b1100_0000_0000_0000_0000_0000_0000_0000;
    private final static int DISCONNECT = 0b1110_0000_0000_0000_0000_0000_0000_0000;

    private final static int CMD_MASK = 0b1110_0000_0000_0000_0000_0000_0000_0000;
    private final static int RSV_MASK = 0b0001_1111_0000_0000_0000_0000_0000_0000;
    private final static int LEN_MASK = 0b0000_0000_1111_1111_1111_1111_0000_0000;
    private final static int RSP_MASK = 0b0000_0000_0000_0000_0000_0000_1111_1111;
    private final static int HIGH_16  = 0b1111_1111_1111_1111_0000_0000_0000_0000;
    private final static int LOW_16   = 0b0000_0000_0000_0000_1111_1111_1111_1111;
    private final static int COLOR_RED    = 0b0000_0001;
    private final static int COLOR_BLUE   = 0b0000_0010;
    private final static int COLOR_YELLOW = 0b0000_0100;
    private final static int COLOR_GREEN  = 0b0000_1000;
    private final static int COLOR_BLACK  = 0b0001_0000;
    private final static int COLOR_WHITE  = 0b0010_0000;
    private final static int WHITE_MASK   = 0b0000_0000; 
    
    private static void initError(String error_string) {
        System.err.println("Error! Proper Usage:");
        System.err.println("java SBoard <port> <width> <height> [defaultColor [, color2, ... , colorN]");
        System.err.println("    avalible colors: red, blue, yellow, green, black, white");
        System.err.println(error_string);
        System.exit(1);
    }

    private static int getColor(String s)
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

    private static int colors;
    private static int defaultColor;
    public static void main(String[] args) throws Exception {
        int port = 0, height = 0 , width = 0, defaultColor = 0, colors =0;
        if (args.length > 2) {
            try{
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                initError("Invalid port number");            
            }
            try{
                width = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                initError("Invalid board width");
            }
            try{
                height = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                initError("Invalid board height");
            }
            if (args.length > 3) {
                defaultColor = getColor(args[3]);
                for (int i = 4; i < args.length; i++)
                    colors |= getColor(args[i]);
                
            }
        } else {
            initError("Not enough arguments supplied");
        }

        ExecutorService pool = extracted();
        Integer clientNumber = 0;
        NoteBoard board = new NoteBoard(width, height, colors, defaultColor);
        try (ServerSocket listener = new ServerSocket(port)) {
            while (true) {
                pool.execute(new Client(board, listener.accept(), clientNumber++));
            }
        }

    }

    private static ExecutorService extracted() {
        ExecutorService pool = Executors.newFixedThreadPool(N_THREADS);
        return pool;
    }
    private static class Note {
        private int x,y,h,w,color,pinCount;
        private char[] msg;
        ArrayList<Integer> pinnedBy;

        public Note(int x, int y, int height, int width, int color, char[] message) {
            this.x = x;
            this.y = y;
            this.h = height;
            this.w = width;
            this.color = color;
            this.msg = message;
            this.pinCount = 0;
            this.pinnedBy = new ArrayList<Integer>();
        }
   
        public int getH() { return this.h;}
        public int getW() { return this.w;}
        public int getX() { return this.x;}
        public int getY() { return this.y;}
        public int getColor() { return this.color;}
        public int[] getIntMsg() { return Char6.chars2Ints(this.msg);}
        public int getpinCount() { return this.pinCount;}

        public void pin(Integer clientNum) {
            this.pinCount++;
            if (!(pinnedBy.contains(clientNum))) {
                pinnedBy.add(clientNum);
            }
        }

        public void unpin(Integer clientNum) {
            if (pinnedBy.contains(clientNum)) {
                pinnedBy.remove(clientNum);
                pinCount = pinCount != 0 ? pinCount - 1 : 0;
            }
        }

        public boolean hasString(char[] searchString) {
            String message = new String(msg);
            String search = new String(searchString);

            return message.contains(search);
        }
    }
    private static class NoteBoard {
        private List<Note>[][] board;
        private int height;
        private int width;
        private int colors;
        private int defaultColor;
        
        public NoteBoard(int width, int height, int colors, int defaultColor) {
            this.width = width;
            this.height = height;
            this.colors = colors;
            this.defaultColor = defaultColor;
            board = new List[width][height];

            /*  This might not work but it would be the easiest solution
                https://docs.oracle.com/javase/7/docs/api/java/util/ArrayList.html#iterator()
                https://docs.oracle.com/javase/7/docs/api/java/util/Collections.html#synchronizedList(java.util.List)
            */
            for (int i = 0; i < width; i++) {
                for (int j = 0; j < height; j++) {
                    board[i][j] = Collections.synchronizedList(new ArrayList<Note>());
                }
            }
        }
            
        public int getHeight() { return height; }
        public int getWidth() { return width; }
        public int getColors() { return colors; }
        public int getDefaultColor() { return defaultColor; }
        
        public ArrayList<Note> getNotes(int x, int y) { return (ArrayList<Note>) board[x][y]; }
        
        public void clearNotes(int x, int y) {
            List<Note> found = new ArrayList<Note>();
            /*  Note Java 7+ for-each loops actually use an iterator 
                via ArrayList#iterator() which has a fail-safe for concurrency.
                Wrapped in synchonized list collection it should be atomic. 
            
                https://docs.oracle.com/javase/7/docs/api/java/util/ArrayList.html
            */
            for (Note note : board[x][y]) {
                if (note.getpinCount() == 0) {
                    found.add(note);
                }
            }
            synchronized (board[x][y]) {
                board[x][y].removeAll(found);
            }
        }

        public int addNote(int x, int y, Note n) {
            synchronized (board[x][y]) {
                board[x][y].add(n);
            }
            return 0xFFFFFFFF;
        }
    };

    /*server function to read commands. 
    */
    private static class Client implements Runnable {
    	private final static int SUCCESS_CODE 	= 0xFFFFFFFF;
    	private final static int ERROR_CODE 	= 0x00000000;
        private Socket socket;
        private NoteBoard board;
        private Integer clientNumber;

        public Client(NoteBoard board, Socket socket, Integer clientNumber) {
            this.board = board;
            this.socket = socket;
            this.clientNumber = clientNumber;
        }

        private int ping() {
            return (0b1001 << 28);
        }

        private int[] readChars(int nPackets, DataInputStream in) throws IOException {
            int[] packets = new int[nPackets];
            int i = 0;
            while (i < nPackets) {
                packets[i] = in.readInt();
                if ((packets[i++] & 0b11) == 0)
                    break;
            }

            if (i < nPackets)
                packets = null;

            return packets;
        }

        private int pin(int packet, DataInputStream in) throws IOException {
            packet = in.readInt();
            int xCord = (packet & HIGH_16) >>> 16;
            int yCord = (packet & LOW_16);
            List<Note> notes = new ArrayList<Note>(board.getNotes(xCord, yCord));
            synchronized (notes) {
                for (Note n : notes) {
                    n.pin(clientNumber);
                }
            }
            return SUCCESS_CODE;
        }

        private int unpin(int packet, DataInputStream in) throws IOException {
            packet = in.readInt();
            int xCord = (packet & HIGH_16) >>> 16;
            int yCord = (packet & LOW_16);
            List<Note> notes = board.getNotes(xCord, yCord);
            synchronized (notes) {
                for (Note n : notes) {
                    n.unpin(clientNumber);
                }
            }
            return SUCCESS_CODE;
        }

        private int clear(int packet, DataInputStream in) throws IOException {
            packet = in.readInt();
            int xCord = (packet & HIGH_16) >>> 16;
            int yCord = (packet & LOW_16);
            //TODO INSERT ERROR HANDLING
            board.clearNotes(xCord, yCord);
			return SUCCESS_CODE;
            
            
            
            
        }

        private int err(int packet, DataInputStream in) {
			return packet;
            /// handle error (need to see client first)
        }

        private int post(int packet, DataInputStream in) throws IOException {
            int color = (packet & RSV_MASK) >>> 24;
            if (color == WHITE_MASK)
                color = COLOR_WHITE;
            int nCharPackets = (packet & LEN_MASK) >>> 8;
            if ((packet & 0b11) != 0)
                packet = in.readInt();
            else
                return ERROR_CODE; //TODO
            int xCoord = (packet & HIGH_16) >>> 16;
            int yCoord = (packet & LOW_16);
            packet = in.readInt();
            int pHeight = (packet & HIGH_16) >>> 16;
            int pWidth = (packet & LOW_16);
            int[] charPackets = readChars(nCharPackets, in);
            if (charPackets == null) {

            }
            char[] message = Char6.ints2Chars(charPackets);
            Note n = new Note(xCoord, yCoord, pHeight, pWidth, color, message);
            
			return board.addNote(xCoord, yCoord, n);

        }

        private int getNotes(int packet, DataInputStream in) throws IOException {
            int request = packet & RSV_MASK;
            ArrayList<Note> notes = new ArrayList<Note>();
            if (request != 0) {
                /* will always do in order */
                if ((request & 0b1) != 0) {
                    packet = in.readInt();
                    int x = (packet & LEN_MASK) >>> 8;
                    packet = in.readInt();
                    int y = (packet & HIGH_16) >>> 16;
                    if (x < board.getWidth() && y < board.getHeight()) {
                        ArrayList<Note> temp = board.getNotes(x, y);
                        synchronized (temp) {
                            for (Note n : temp) {
                                notes.add(n);
                            }
                        }
                    } else {
                        return ERROR_CODE; //TODO
                    }
                }

                if ((request & 0b10) != 0) {
                    packet = in.readInt();
                    int height = (packet & LEN_MASK) >>> 8;
                    packet = in.readInt();
                    int width = (packet & HIGH_16) >>> 16;
                    if (!notes.isEmpty()) {
                        List<Note> found = new ArrayList<Note>();
                        for (Note n : notes) {
                            if (height != n.getH() && width != n.getW()) {
                                found.add(n);
                            }
                        }
                        notes.removeAll(found);
                    } else {
                        for (int x = 0; x < board.getWidth(); x++) {
                            for (int y = 0; y < board.getHeight(); y++) {
                                ArrayList<Note> temp = board.getNotes(x, y);
                                synchronized (temp) {
                                    for (Note n : temp) {
                                        if (n.getH() == height && n.getW() == width)
                                            notes.add(n);

                                    }
                                }
                            }
                        }
                    }
                }

                if ((request & 0b100) != 0) {
                    packet = in.readInt();
                    int charLen = (packet & LEN_MASK) >>> 8;
                    int[] charPackets = readChars(charLen, in);
                    char[] searchString = Char6.ints2Chars(charPackets);
                    if (!notes.isEmpty()) {
                        List<Note> found = new ArrayList<Note>();
                        for (Note n : notes) {
                            if (n.hasString(searchString))
                                found.add(n);
                        }
                        notes.removeAll(found);
                    } else {
                        for (int x = 0; x < board.getWidth(); x++) {
                            for (int y = 0; y < board.getHeight(); y++) {
                                List<Note> temp = board.getNotes(x, y);
                                synchronized (temp) {
                                    for (Note n : temp)
                                        if (n.hasString(searchString))
                                            notes.add(n);

                                }
                            }
                        }
                    }
                }
            }

            if ((request & 0b1000) != 0) {
                packet = in.readInt();
                int color = (packet & 0x0F800000) >>> 27;
                if (!notes.isEmpty()) {
                    List<Note> found = new ArrayList<Note>();
                    for (Note n : notes) {
                        if (n.getColor() != color)
                            found.add(n);
                    }
                    notes.removeAll(found);
                } else {
                    for (int x = 0; x < board.getWidth(); x++) {
                        for (int y = 0; y < board.getHeight(); y++) {
                            List<Note> temp = board.getNotes(x, y);
                            synchronized (temp) {
                                for (Note n : temp)
                                    if (n.getColor() == color)
                                        notes.add(n);

                            }
                        }
                    }
                }
            }
			return sendNotes(notes);
        }
   
        private int sendNotes(ArrayList<Note> notes) throws IOException {
            int notesToSend = notes.size();
            // send note-response command and number of notes to expect
            int packet = (0b1010 << 28) | (notesToSend & LOW_16);
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            out.writeInt(packet);
            for (Note n : notes) {
                packet = (n.getX() << 16) | n.getY();
                out.writeInt(packet);
                packet = (n.getH() << 16) | n.getW();
                out.writeInt(packet);
                int[] msg = n.getIntMsg();
                packet = ((n.getColor() & 0b11111) << 16) | (msg.length);
                out.writeInt(packet);
                for (int m : msg) {
                    out.writeInt(m);
                }
            }
			return SUCCESS_CODE;
        }

        private int disconnect() {
            try {
                socket.close();
            } catch (IOException e) {
            	return ERROR_CODE;
            }
            return SUCCESS_CODE;
        }
        
        private int[] respondConnection(){
        
            int[] response = new int[3];
            response[0] = 0b1110_0000_0000_0000_0000_0000_0000_0000;
            response[1] = this.board.colors << 24;
            response[1] |= this.board.defaultColor << 16;
            response[2] = this.board.height << 16;
            response[2] |= this.board.width;
            return response;

        }
        private int parseCommandPacket(int packet, DataInputStream in) throws IOException {
            int cmd = (packet & CMD_MASK);
            int response;
            switch (cmd) {
            case PING:
                response = ping();
                System.out.println("PING");
                break;
            case POST:
                response = post(packet, in);
                System.out.println("POST RECIEVED");
                break;
            case GET:
                response = getNotes(packet,in);
                System.out.println("GET RECIEVED");
                break;
            case PIN:
                response = pin(packet, in);
                System.out.println("PIN RECIEVED");

                break;
            case UNPIN:
                response = unpin(packet,in);
                System.out.println("UNPIN RECIEVED");
                break;
            case CLEAR:
                response = clear(packet, in);
                System.out.println("CLEAR RECIEVED");
                break;
            case ERROR:
                response = err(packet,in);
                System.out.println("ERROR RECIEVED");
                break;
            case DISCONNECT:
                response = disconnect();
                System.out.println("DISCONNECT RECIEVED");
                break;
            default:
                response = -1;
                break;
            }
            return response;
        }


        public void run() {
            try {
                System.out.println("CLIENT CONNECTED");
                DataInputStream in = new DataInputStream(socket.getInputStream());
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                for (int a : respondConnection()) {
                    out.writeInt(a);
                }
                for (int packet; (packet = in.readInt()) != -1;) {
                    //keep reading commands until disconnect command
                    int response = parseCommandPacket(packet, in);
                    //out.writeInt(response); should add this for error handling
                }
                
            } catch (Exception e) {

            } finally {
                try {
                    socket.close();
                } catch (IOException e) {}
        
            }
        }
    };

    public static class Char6 {
        private static final int[] CHAR_TBL = { 0, 0x20, 0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29, 0x2A, 0x2B,
            0x2C, 0x2D, 0x2E, 0x2F, 0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x3A, 0x3B, 0x3C, 0x3D,
            0x3E, 0x3F, 0x40, 0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48, 0x49, 0x4A, 0x4B, 0x4C, 0x4D, 0x4E, 0x4F,
            0x50, 0x51, 0x52, 0x53, 0x54, 0x55, 0x56, 0x57, 0x58, 0x59, 0x5A, 0x5C, 0x5E, 0x5F, 0x10 
        };

        public Char6() {

        }
        
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

                if (cint != 0) ints[j] |= cint << ((6 * (5-tmp))+2);
                if (tmp == 0) ints[j++] |= (j+1 == ints.length) ? 0b00 : 0b11;
            }

            return ints;    

        }

        public static char[] ints2Chars(int[] ints) {
            char[] chars = new char[ints.length * 5];
            for (int i = 0; i < ints.length; i++) {
                int justChars = ints[i] >>> 2;
                for (int j = 0; j < 6; j++) {
                    chars[(5 * i) + j] = int2Char((justChars >>> 5*(6-j)) & 0b11_1111);
                }
            }
            return chars;
        }
    };

};
