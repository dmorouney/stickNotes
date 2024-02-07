Network Working Group                                          R. Morouney
Request for Comments: DRAFT					   N. Popadic
Category: Experimental						    Group 061                      
                                                                   
                                                               January 2019


                  STICKY NOTE TRANSFER PROTOCOL – SNTP/0.1

Status of This Memo

   This memo provides information for the Internet community.  It does
   not specify an Internet standard of any kind.  Distribution of this
   memo is unlimited.

Copyright Notice

   Copyright (C) 2019 R. Morouney, N. Popadic.  
 
IESG Note:

The IESG has no connection with this protocol, will never assess its 
validity and may not even be informed of its existence. This RFC was 
created for a school project for Wilfrid Laurier University. 

Abstract

   The Sticky Note Transfer Protocol (SNTP) is an application level 
protocol created for the specifications of assignment 1 in the winter 
term of CP372 in 2019. The protocol is designed to give distributed 
clients the ability to GET, POST, PIN, and UNPIN notes which are stored 
on a centralized server. The central server will store and maintain the 
notes of multiple clients; servicing various commands from those 
clients in a synchronized multi-threaded environment. The protocol is 
designed to be as light as possible to allow the maximum number of 
simultaneous client connections. 

Table of Contents

   1.  Introduction  . . . . . . . . . . . . . . . . . . . . . . . . . 1
   2.  Terminology . . . . . . . . . . . . . . . . . . . . . . . . . . 2
   3.  Initial Command Structures  . . . . . . . . . . . . . . . . . . 3
   4.  Specific Command Packet Structures  . . . . . . . . . . . . . . 3
   5.  Specific Server Responses . . . . . . . . . . . . . . . . . . . 6
   6.  Synchronization policies  . . . . . . . . . . . . . . . . . . . 8
   7.  Error handling  . . . . . . . . . . . . . . . . . . . . . . . . 8
   8.  Border case behavior  . . . . . . . . . . . . . . . . . . . . . 8
   9.  References  . . . . . . . . . . . . . . . . . . . . . . . . . . 9
Appx.  6-bit ASCII Table . . . . . . . . . . . . . . . . . . . . . . . 9


1.	Introduction
The Sticky Note Transfer Protocol (SNTP) is intended to support 
multiple distributed clients by providing an API to allow each client 
to store, retrieve, pin and unpin notes from a centralized server. To 
accomplish this each client will connect to the server by sending a 
connection request header (section 3). The server will respond with the 
maximum values for height and width of the coordinate system and the 
available colors the server supports for graphically displaying the 
notes.  The clients will accept plain text commands from the user and 
convert them to 32-bit binary formatted packets specified broadly in 
section 2 and more specifically in section 3. Each command will be sent 
as a set of packets along with a running total of packets sent and 
received to test the validity of each packet stream. To start a 
command, the command type and command length are sent to the server. If 
a successful command is received the client will send the remaining 
data to complete the command. The server will read each packet and 
check its validity asking the client to re-send any packets which 
cannot be verified (section 6). After all packets are received and 
verified, the server will process and interoperate the command and send 
an appropriate response (section 5). The client-server interaction will 
continue in this fashion until either the server receives a disconnect 
command or a time out occurs.
2.	Terminology

note – a note is defined by a set of 4 properties.  The coordinates of 
the lower left corner, the height and width of the note, the contents 
of the note (see below), and the color of the note. 

note contents – The contents of a note can only contain uppercase 
English ascii characters, numbers and specific punctuation. For this 
reason, we can pack the value of a character into 6 bits (section 7) 
instead of 8.  This will allow us to only send 25% of the data 
increasing both speed and efficiency of the transmission of long notes.
More importantly by packing the same number of characters in each 32bit 
packet the extra bits given by using a 6-bit character set allow the 
addition of validation bits at the end of each packet without 
increasing the packet size. Finally, in the future, the notes will be 
graphically displayed by the client so limiting the character set will 
only ease future considerations of how to draw the notes. 
 
data – A pair of integers used to define coordinates of a note or set 
of notes. These integers will be constrained to a discrete set of 
values which coincide with the maximum and minimum values of the 
coordinate system.

request – specific properties used by the client to request a note from 
the server which satisfies those properties. A request can consist of 
one or more of any items in the set of properties which define a note. 

renumb - The response number is a number the client and server will 
keep track of. It is the number of packets sent to each other.  If the 
number of packets the client thinks it sent is different from what the 
server received an error is generated and packets are resent. The 
server will keep track of a number for each client that it is connected 
to.

3.	Initial Command Structures

Structure of initial connection header
0         3                                                32
+---------+-------------------------------------------------+
|   111   |        11111111111111111111111111111            |
+---------+-------------------------------------------------+
Server listens for connection. Upon receiving this signal a connection 
is 
made and the server responds with some initial configuration in the 
form 
of a header (section 5). 

Structure of command packet
0         3          8                  24                 32
+---------+----------+-------------------+------------------+
| command | RESERVED | length in packets | renumb           |
+---------+----------+-------------------+------------------+
Where,
command = 000 => PING 
command = 001 => POST 
command = 010 => GET
command = 011 => PIN
command = 100 => UNPIN
command = 101 => CLEAR
command = 110 => ERROR
command = 111 => DISCONNECT 
length in packets = the number of 32-bit packets that the client will 
send following this command. 
renumb = a calculated value used to verify the correct transmission of 
data has occurred. (4) 

4.	Specific Command Packet Structures

PING – 
0                                                           32
+---------+--------------------------------------------------+
|   000   | UNDEFINED                                        |
+---------+--------------------------------------------------+
Check if server is alive.
       POST <note> - 
0         3        8           16                24         32
+---------+--------+------------------------------+----------+
|   001   | COLOUR |  2 + N packets for contents  | renumb   |
+---------+--------+------------+----------------------------+
|          x-coordinate         |    y-coordinate            |
+-------------------------------+----------------------------+
|          height in chars      |    width in chars          |
+-------------------------------+----------------------------+
| CONTENTS frame 1                                           |
+------------------------------------------------------------+
| CONTENTS frame 2                                           |
+------------------------------------------------------------+
|       . . .                                                |
+------------------------------------------------------------+
| CONTENTS frame N                                           |
+------------------------------------------------------------+
Where,
x-coordinate = 0 – 65535
y-coordinate = 0 - 65535 
height       = 1 – 65535 defines the height of the note in characters.
width        = 1 – 65535 defines the height of the note in characters.
COLOR 	 = 00000 = white 
       	 = 00001 = red
	 	 = 00010 = blue
       	 = 00100 = green
       	 = 01000 = yellow
	 	 = 10000 = black

Contents of each frame packet: 
0          6          12        18        24       30       32
+----------+----------+----------+---------+--------+--------+
|   CHAR   |   CHAR   |   CHAR   |  CHAR   |  CHAR  | #chars |
+----------+----------+----------+---------+--------+--------+
If #chars + 1 < 4 discard portion of packet which is undefined. 

GET <request> - 
0         3         8                           24          32
+---------+---------+----------------------------+-----------+
|   010   | REQUEST |  M = packets for request   | renumb    |
+---------+---------+----------------------------+-----------+
|          REQUEST PACKET 1                                  |
-------------------------------------------------------------+
|          REQUEST PACKET …                                  |
-------------------------------------------------------------+
|          REQUEST PACKET M                                  |
-------------------------------------------------------------+ 
Where REQUEST represents a set of flags defining the 4 fundamental 
properties which are used to make a note, 
(note ‘X’ denotes a bit that is undefined, or its value is irrelevant)
REQUEST = X0000 = PIN = get all notes.
	  = X0001 = request defines coordinates of lower left corner to 
 	            retrieve.
	  = X0010 = request define width and height of note to retrieve.
	  = X0100 = request will search content strings for given phrase
			And retrieve all matching notes.
	  = X1000 = request will define color of note to retrieve. 
By combining these values, one or more properties can easily be 
defined. For instance, a request of X1101 would define the coordinates, 
the size, and the color of the note to retrieve. 
 
 


Contents of each request packet: 
0          4                                                 32
+----------+--------------------------------------------------+
|  REQUEST |   REQUEST SPECIFIC DATA (SEE BELOW)              |
+----------+--------------------------------------------------+ 

Specifically,
0          4       8          16                  24         32
+----------+-------+-----------+------------------------------+
|  0001    | UNDEF |  renumb   | x-coordinate                 |
+----------+-------+-----------+------------------------------+
| y-coordinate                 |  RESERVED        |  renumb   |
+------------------------------+------------------------------+ 

0          4       8            16                 24        32
+----------+-------+-------------+----------------------------+
|  0010    | UNDEF |  renumb     |        height              |
+----------+-------+-------------+------------------+---------+
|             width              |    RESERVED      | renumb  |
+--------------------------------+------------------+---------+

0          4       8                               24        32
+----------+-------+--------------------------------+---------+
|  0010    | UNDEF |   # of characters to receive   | renumb  |
+----------+-------+--------------------------------+---------+
Then, 
0          6         12         18         24       30       32
+----------+----------+----------+----------+--------+--------+
|   CHAR   |   CHAR   |   CHAR   |   CHAR   |  CHAR  | #chars |
+----------+----------+----------+----------+--------+--------+
| . . . until total characters received                       |
+-------------------------------------------------------------+

0          4       8          16                             32
+----------+-------+-----------+------------------------------+
|  1000    | COLOR |  renumb   | UNDEFINED                    |
-----------+-------+-----------+------------------------------+

PIN <data> - 
0         3           8        16               24           32
+---------+-----------+--------------------------+------------+
|   011   | UNDEFINED |           1              |  renumb    |
+---------+-----------+--------------------------+------------+
|          x-coordinate         |    y-coordinate             |
+-------------------------------+-----------------------------+
Pins all unpinned notes at coordinate supplied.







UNPIN <data> - 
0         3           8        16               24          32
+---------+-----------+--------------------------+-----------+
|   100   | UNDEFINED |           1              |  renumb   |
+---------+-----------+--------------------------------------+
|          x-coordinate         |    y-coordinate            |
+-------------------------------+----------------------------+
Unpins all pinned notes at coordinate supplied. “However, remember that 
a note can be pinned by more than one pin. So, UNPIN command does not 
necessary changes the status of a note.” 

CLEAR –
0         3           8        16               24          32
+---------+-----------+--------------------------+-----------+
|   101   | UNDEFINED |           1              |  renumb   |
+---------+-----------+---------+----------------------------+
|          x-coordinate         |    y-coordinate            |
+-------------------------------+----------------------------+
Deletes all unpinned notes at coordinate supplied. 

ERROR – 
0         3           8                         24          32
+---------+-----------+--------------------------+-----------+
|   110   |  LAST CMD | SPECIFIC ERROR #         | renumb    |
+---------+-----------+--------------------------+-----------+
Received bad packet from server. Request a new one. 
Also send the last command that was sent by the client and a specific 
8-bit error code which is reserved for future border cases. 

DISCONNECT – 
0         3                                                 32
+---------+--------------------------------------------------+
|   111   | UNDEFINED                                        |
+---------+--------------------------------------------------+
Sever connection to server. 

5.	Server Response For: 

CONNECTION REQUEST – 
0           4           8      16           24              32
+-----------+-----------+------------------------------------+
|   1111    |    ---N   |  renumb  = 0                       |
+-----------+-----------+--------------------+---------------+
|   COLORS ALLOWED      | DEFAULT COLOR MASK |  RESERVED     |
+-------------------------------+------------+---------------+ 
|          height in pixels     |    width in pixels         |
+-------------------------------+----------------------------+ 
Where, 
	N = 1 if new user, 0 otherwise
	Colors allowed = 000XXXX where XXXX are the 5 supported colors
	Default color mask = a single bit which defines the default of
				   the supported colors. 


 
COMMAND PACKET – 
+-----------+---------------------------------+--------------+
|   1110    | # of data packets to read       | renumb       |
+-----------+---------------------------------+--------------+


DATA PACKET – 
0           4                                24             32
+-----------+---------------------------------+--------------+
|   1100    | # of data packets left to read  | renumb       |
+-----------+---------------------------------+--------------+


DISCONNECTION –
0           4                                               32
+-----------+------------------------------------------------+
|   1000    | UNDEFINED                                      |
+-----------+------------------------------------------------+
Successful disconnection packet received sever connection after sending 
this packet. 

ERROR -
0           4           8                     24            32
+-----------+-----------+----------------------+-------------+
|   0000    | COMMAND   | SPECIFIC ERROR CODE  | renumb      |
+-----------+-----------+----------------------+-------------+
Where,
COMMAND = The last command received by the server in the format 
XBBB where BBB is the command and X is undefined.  
SPECIFIC ERROR CODE = a code depending on the error received.
	00000000 – missed packet
	00000001 – height out of bounds
	00000010 – width out of bounds
	00000100 – invalid x-coordinate
	00001000 – invalid y-coordinate
	00010000 – unrecognized character
	00100000 – bad command
	01000000 – PIN / UNPIN ERROR
	10000000 – RESERVED / FATAL

PING – 
0           4                                  24           32
+-----------+-----------------------------------+------------+
|   1001    |     UNDEFINED                     | renumb     |
+-----------+-----------------------------------+------------+
Server is alive





6.	Synchronization policies

Because multiple notes can be located at the same coordinates and can 
have the status of either PINNED or UNPINNED, a 2D array data structure 
will be kept updated by the server. Each cell in the 2D array will 
contain a linked list of notes each with a status variable which 
determines if the note is PINNED or UNPINNED. The server will use an 
atomic variable to access the array when posting, pinning or unpinning 
notes to make sure all requests are serviced in order. Since GET 
requests do not affect the underlying data structure, we can service 
them without worrying about race conditions.  
	Each packet sent by the server or the client (with some exceptions) 
will be suffixed with an 8-bit renumb number. This number will be 
constantly checked to assure the server and client are in sync. 8-bits 
will be sufficient as a rollover every 256 packets can easily be 
accounted for.
	Finally, since the server and clients will be implemented using the 
Java Virtual Machine Socket class, we can assume the byte order of the 
packets will always be in big-endian format. (6) This greatly simplifies 
the transmission of data.

7.	Error handling

When an error is received by either the server or the client a special 
reserved command is sent (section 4) (section 5).  This command will 
contain the last command given and the current renumb number (see 
above). Depending where the error occurred the client or the server will 
try to recover by re-sending the packets which caused an error. If the 
error persists the server and client will attempt to sever the 
connection and display an error report. In such a case all UNPINNED 
notes will be saved by the server if possible. A specific error number 
is sent in the case of various specified errors defined in section 5. 

8.	Border case behavior  

Client disconnects without sending disconnect command – 
In this case the server will save the interaction to a log file and 
report to the GUI.  All unpinned notes will be lost. 

Server disconnect unexpectedly – 
Client will report to user and try to reconnect. If a connection cannot 
be made after a set amount of tries, the system will report to the user. 

Server’s renumb number doesn’t match client – 
This indicates the client missed a packet sent by the server.  Since the 
client and server independently keep track of the order of their packets 
the client will send an error command (section 3) requesting the missing 
packet. 

Client’s renumb number doesn’t match server -  
The reverse is true for the server. If a packet sent by the client is 
not received the renumb number will not match the expected renumb 
number. In this case the server will send an error response requesting 
the missing packet. 


9.	References
(1)	https://www.rfc-editor.org/pubprocess/ RFC Publication process. 
(2)	https://www.rfc-editor.org/about/independent/ submissions
(3)	https://en.wikipedia.org/wiki/Request_for_Comments RFC Definition
(4)   https://en.wikipedia.org/wiki/Checksum Checksums
(5)   https://en.wikipedia.org/wiki/Six-bit_character_code 6-bit ASCII 
(6)	https://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html  
	JVM byte order. 

A.	ASCII TABLE 
----+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+
|   |.0 |.1 |.2 |.3 |.4 |.5 |.6 |.7 |.8 |.9 |.A |.B |.C |.D |.E |.F | 
----+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+ 
0.  |\0 |   | ! | " | # | $ | % | & | ' | ( | ) | * | + | , | - | . | 
----+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+ 
1.  | / | 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | : | ; | < | = | > |  
----+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+ 
2.  | ? | @ | A | B | C | D | E | F | G | H | I | J | K | L | M | N |  
----+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+ 
3.  | O | P | Q | R | S | T | U | V | W | X | Y | Z | \ | ^ | _ |\n | 
----+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+ 
The above ascii table is a personalized variation of other 6-bit 
character codes listed in the references. The rational behind the 
choices of characters is to match the most commonly used punctuation 
while disregarding any lower-case letters. An added line break and NULL 
character help with formatting. (5)
		061
1 | Page

