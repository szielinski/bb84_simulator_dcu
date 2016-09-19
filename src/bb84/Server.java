    package bb84;

    import java.io.*;
    import java.net.*;
    import java.util.*;

    //Represents Alice
    public class Server {
        
        private ServerSocket serverSocket;
        private Socket clientSocket = null;
        private ObjectOutputStream out;
        private ObjectInputStream in;
        
        private boolean[] candidateKey;
        private char[] basis;
        private char[] clientBasis;
        private boolean[] validKeyPositions;
        
        private int validKeyLength = 0;
        
        private boolean evePresent = false;

        Server() {
        }

        void run() {
            try {         
                System.out.println("--NOTE:\n"
                        + "--The horizontal/vertical basis is represented by 'H'\n"
                        + "--The diagonal45 basis is represented by 'D'.\n"
                        + "--Empty/unknown contents are dentoted by '_'\n\n"
                        + "Whenever the program pauses, press ENTER to continue.");
                
                //ask whether the program should simulate Eve
                System.out.println("Would you like to simulate an eavesdropper (y/n): ");
                Scanner scanner = new Scanner(System.in);
                String eve = scanner.next();
                if(eve.equals("y")){
                    evePresent = true;
                    System.out.println("Eve will be simulated.\n");            
                }else{
                    System.out.println("Eve will not be simulated.\n");                           
                }                    
                //intialise global vars
                candidateKey = new boolean[BB84StaticVars.MESSAGE_SIZE];
                basis = new char[BB84StaticVars.MESSAGE_SIZE];
                clientBasis = new char[BB84StaticVars.MESSAGE_SIZE];
                validKeyPositions = new boolean[BB84StaticVars.MESSAGE_SIZE];

                //set up socket, wait for connection
                serverSocket = new ServerSocket(5555);
                System.out.println("1. Alice is waiting for Bob...");
                clientSocket = serverSocket.accept();
                
                System.out.println("2. Bob has connected.");
                out = new ObjectOutputStream(clientSocket.getOutputStream());
                out.flush();
                in = new ObjectInputStream(clientSocket.getInputStream());
                
                //generate and send a key qubit by qubit
                System.out.println("\n3. Generating a 1024-bit candidate key, converting it into Qubit form using a random basis and sending it to Bob one qubit at a time:");
                for (int i = 0; i < candidateKey.length; i++) {
                    candidateKey[i] = RandomBB84.getRandomBinary();
                    basis[i] = Basis.randomBasis();
                    if(candidateKey[i])
                        System.out.print(1);
                    else
                        System.out.print(0);
                    
                    Qubit temp = new Qubit(candidateKey[i], basis[i]);
                    if(evePresent){
                        temp = simulateEve(temp);
                    }

                    sendQubit(temp);
                }

                System.out.println("\n4. The random bases that were used to generate qubits that are displayed above are:");
                for (int i = 0; i < basis.length; i++) {
                    System.out.print(basis[i]);
                }
                System.out.println();

                //receive client's basis
                System.out.println("\n5. Receiving bases that were used by Bob:");
                try {
                    String as = (String) in.readObject();
                    System.out.println(as);
                    clientBasis = (as.toCharArray());
                } catch (ClassNotFoundException e) {
                    System.err.println("Error: Bob's bases were corrupted.");
                }

                //send own basis
                System.out.println("\n6. Sending own bases for comparison.");
                sendMessage(new String(basis));

                //compare basis
                getCommonBasis();

                System.out.println("\n7. Finding the common bases... The common bases are:");
                for (int i = 0; i < candidateKey.length; i++) {
                    if (validKeyPositions[i]) {
                        System.out.print(basis[i]);
                    }
                    else
                        System.out.print("_");
                }
                System.out.println();

                //pick bits to compare to test for Eve
                char[] bitsToCompare = prepareBitsToCompare();

                //send bits to be compared to Bob
                System.out.println("\n8. Sending own, random bits from the valid key to test for the presence of Eve:");
                System.out.println(new String(bitsToCompare));
                sendMessage(new String(bitsToCompare));

                //receive Bob's bits
                System.out.println("\n9. Receiving Bob's corresponding bits to test for Eve:");
                char[] clientBits = null;
                try {
                    String receivedBits = (String) in.readObject();
                    clientBits = (receivedBits.toCharArray());
                } catch (ClassNotFoundException e) {
                    System.err.println("Data received in unknown format");
                }
                
                for (int i = 0; i < BB84StaticVars.MESSAGE_SIZE; i++) {
                    System.out.print(clientBits[i]);
                }
                System.out.println();

                //check for Eve
                for (int i = 0; i < BB84StaticVars.MESSAGE_SIZE; i++) {
                    if (clientBits[i] == '_') {
                        continue;
                    }
                    if (clientBits[i] != bitsToCompare[i]) {
                        System.out.println("\nAlice's and Bob's test bits do not match - the qubits were tampered with! Exiting!");
                        System.exit(0);
                    }
                }
                
                System.out.println("\n10. The two sets of test bits are the same - Eve has not been detected.");

                //print the common secret key
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
                System.out.println();
                
                //done - close the connection
                try {
                    in.close();
                    out.close();
                    serverSocket.close();
                    System.exit(0);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //send a String message
        void sendMessage(String msg) {
            try {
                out.writeObject(msg);
                out.flush();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }

        //send a qubit
        void sendQubit(Qubit qbit) {
            try {
                out.writeObject(qbit);
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        //simulate the presence of an eavesdropper
        public Qubit simulateEve(Qubit qubit) {
            char randomBasis = Basis.randomBasis();
            boolean interceptedBit = qubit.collapse(randomBasis);
            return(new Qubit(interceptedBit, randomBasis));
        }

        //pick random bits to test for the presence of an eavesdropper. Use 1/2 of the valid remaining key bits.
        public char[] prepareBitsToCompare() {
            Random gen = new Random();
            char[] list = new char[BB84StaticVars.MESSAGE_SIZE];
            Arrays.fill(list, '_');
            
            int compared = 0;
            int toCompare = validKeyLength / 2;
            
            while (compared < toCompare) {
                int a = (int) (gen.nextDouble() * (BB84StaticVars.MESSAGE_SIZE));
                if (validKeyPositions[a]) {
                    if (candidateKey[a]) {
                        list[a] = '1';
                    } else {
                        list[a] = '0';
                    }
                    compared++;
                    validKeyPositions[a] = false;
                }
            }
            return list;
        }

        //find the common parts in the basis of Bob and Alice
        public void getCommonBasis() {
            for (int i = 0; i < candidateKey.length; i++) {
                if (basis[i] != clientBasis[i]) {
                    validKeyPositions[i] = false;
                } else {
                    validKeyPositions[i] = true;
                    validKeyLength++;
                }
            }
        }

        public static void main(String args[]) {
            Server alice = new Server();
            while (true) {
                alice.run();
            }
        }
    }
