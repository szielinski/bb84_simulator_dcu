    package bb84;

    import java.util.Random;

    public class RandomBB84 {
        
        //returns a random boolean with a 50/50 probability 
        static boolean getRandomBinary(){
            Random randGen = new Random();
            
            double rand = randGen.nextDouble();
            if(rand > 0.5)
                return true;
            else
                return false;
                
        }
    }
