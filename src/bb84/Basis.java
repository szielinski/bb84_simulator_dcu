    package bb84;

    /*
     * 2 types of basis are used in BB84: Horizontal/Vertical basis and a Diagonal
     * basis that is at a 45 degree angle to the H/V basis. 
     * When executing the algorithm, basis is chosen at random from the 2 with
     * equal probability - represented by randomBasis() below.
     */
    public class Basis {
        static final char HORIZONTAL_BASIS = 'H';
        static final char VERTICAL_BASIS = 'V';
        
        static final char HOR_VER_BASIS = 'D';  
        static final char DIAG_BASIS = 'H';
        
        static char randomBasis(){
            if(RandomBB84.getRandomBinary())
                return DIAG_BASIS;
            else
                return HOR_VER_BASIS;
        }
    }
