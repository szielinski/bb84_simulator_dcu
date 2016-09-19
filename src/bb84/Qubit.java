    package bb84;

    /*
     * A quibit with private variables without public access that simulate the 
     * collapse of the wave function of the qubit. The only public function here
     * is collapse() which takes in a basis represented as a char. 
     * 
     * Depending on the basis that's used, the qubit can collapse into different 
     * values. If the basis is the same as the original basis used when creating 
     * the quibit, it will collapse to the original bit value. Otherwise, it will
     * collapse to a 1 or a 0 with equal probabilities. 
     */

    import java.io.Serializable;

    public class Qubit implements Serializable{
        private char orgBasis;
        private char polarisation;
        
        public Qubit(boolean bit, char basis){
            if(bit)
                polarisation = Basis.VERTICAL_BASIS;
            else
                polarisation = Basis.HORIZONTAL_BASIS;
            if(!(basis == Basis.HOR_VER_BASIS || basis == Basis.DIAG_BASIS))
                throw new IllegalArgumentException("ERROR: Illegal basis used when creating the wave function!");
            this.orgBasis = basis;
        }
        
        public boolean collapse(char basis){
            if(!(basis == Basis.HOR_VER_BASIS || basis == Basis.DIAG_BASIS))
                throw new IllegalArgumentException("ERROR: Illegal basis used when collapsing the wave function!");
            if(!equalBasis(basis)){
                return RandomBB84.getRandomBinary();
            }
            return (polarisation == Basis.VERTICAL_BASIS);
        }
        
        private boolean equalBasis(char otherB){
            return orgBasis==otherB;
        }
    }
