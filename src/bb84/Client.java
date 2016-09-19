    package bb84;

    import java.io.*;
    import java.net.*;

    //Represents Bob
    public class Client {
        
        private Socket requestSocket;
        private ObjectOutputStream out;
        private ObjectInputStream in;
        
        private boolean[] candidateKey;
        private char[] basis;
        private char[] serverBasis;
        private boolean[] validKeyPositions;

        Client() {
        }

        void run() {
            try {
                System.out.println("--NOTE:\n"
                        + "--The horizontal/vertical basis is represented by 'H'\n"
                        + "--The diagonal45 basis is represented by 'D'.\n"
                        + "--Empty/unknown contents are dentoted by '_'\n\n");
                
                //intialise global vars
                candidateKey = new boolean[BB84StaticVars.MESSAGE_SIZE];
                basis = new char[BB84StaticVars.MESSAGE_SIZE];
                serverBasis = new char[BB84StaticVars.MESSAGE_SIZE];
                validKeyPositions = new boolean[BB84StaticVars.MESSAGE_SIZE];

                //set up socket, connect to Alice
                System.out.println("1. Bob is is trying to connect to Alice...");
                requestSocket = new Socket("localhost", 5555);
                System.out.println("2. Bob has connected.");
                
                out = new ObjectOutputStream(requestSocket.getOutputStream());
                out.flush();
                in = new ObjectInputStream(requestSocket.getInputStream());
                          
                //receive the key - one qubit at a time
                System.out.println("\n3. Receiving the candidate key in qubit format from Alice:");
                try {
                    for (int i = 0; i < candidateKey.length; i++) {
                        Qubit qbit = (Qubit) in.readObject();
                        basis[i] = Basis.randomBasis();
                        candidateKey[i] = qbit.collapse(basis[i]);
                    }
                } catch (ClassNotFoundException e) {
                    System.err.println("Error: the key was corrupted.");
                }

                //display the key in binary
                for (int i = 0; i < candidateKey.length; i++) {
                    if (candidateKey[i]) {
                        System.out.print('1');
                    } else {
                        System.out.print('0');
                    }
                }

                //display the random basis that was used to collapse the qubits
                System.out.println("\n4. The random bases that were used to receive qubits that are displayed above are:");
                for (int i = 0; i < basis.length; i++) {
                    System.out.print(basis[i]);
                }
                System.out.println();
                
                //send the basis that was used
                System.out.println("\n5. Sending own bases for comparison.");
                sendMessage(new String(basis));

                //receive the basis that Alice used
                System.out.println("\n6. Receiving Alice's basis:");
                try {
                    String as = (String) in.readObject();
                    System.out.println(as);
                    serverBasis = (as.toCharArray());
                } catch (ClassNotFoundException e) {
                    System.err.println("Error: Alice's basis was corrupted.");
                }

                //generate a common basis
                getCommonBasis();

                //display the common basis
                System.out.println("\n7. Finding the common bases... The common bases are:");
                for (int i = 0; i < candidateKey.length; i++) {
                    if (validKeyPositions[i]) {
                        System.out.print(basis[i]);
                    }
                    else
                        System.out.print("_");
                }
                System.out.println();

                //receive a selection of bits from Alice
                System.out.println("\n8. Receiving valid key bits from Alice to test for the presence of Eve:");
                char[] aliceBits = null;
                try {
                    String as = (String) in.readObject();
                    System.out.println(as);
                    aliceBits = (as.toCharArray());
                } catch (ClassNotFoundException e) {
                    System.err.println("Data received in unknown format");
                }            

                //prepare corresponding bits and send them to Alice for comparison
                System.out.println("\n9. Sending corresponding, own bits of the key to test for Eve:");
                char[] list = new char[BB84StaticVars.MESSAGE_SIZE];
                for (int i = 0; i < BB84StaticVars.MESSAGE_SIZE; i++) {
                    if (aliceBits[i] == '_') {
                        list[i] = '_';
                    } else if (candidateKey[i]) {
                        list[i] = '1';
                    } else {
                        list[i] = '0';
                    }
                }
                System.out.println(new String(list));
                sendMessage(new String(list));

                //compare Alice's bits to own bits to determine the presence of Eve
                for (int i = 0; i < BB84StaticVars.MESSAGE_SIZE; i++) {
                    if (aliceBits[i] == '_') {
                        continue;
                    } else {
                        validKeyPositions[i] = false;
                    }                
                    if (list[i] != aliceBits[i]) {
                        System.out.println("\nAlice's and Bob's test bits do not match - the qubits were tampered with! Exiting!");
                        System.exit(0);
                    }
                }
                System.out.println("\n10. The two sets of test bits are the same - Eve has not been detected.");

                //print the common key
                System.out.println("\n11. Key exchange was successful, your key is: ");
                for (int i = 0; i < BB84StaticVars.MESSAGE_SIZE; i++) {
                    if (validKeyPositions[i]) {
                        if (candidateKey[i]) {
                            System.out.print('1');
                        } else {
                            System.out.print('0');
                        }
                    }
                }
                
                //close the sockets
                try {
                    in.close();
                    out.close();
                    requestSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (UnknownHostException e) {
                System.err.println("Alice could not be found!");
            } catch (IOException e) {
                e.printStackTrace();
            }                
        }

        //find the common parts in the basis of Bob and Alice
        public void getCommonBasis() {
            for (int i = 0; i < candidateKey.length; i++) {
                if (basis[i] != serverBasis[i]) {
                    validKeyPositions[i] = false;
                } else {
                    validKeyPositions[i] = true;
                }
            }
        }

        //send a string to the server
        void sendMessage(String msg) {
            try {
                out.writeObject(msg);
                out.flush();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }

        public static void main(String args[]) {
            Client client = new Client();
            client.run();
        }
    }
