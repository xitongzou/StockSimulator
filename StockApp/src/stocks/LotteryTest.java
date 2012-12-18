package stocks;

import java.util.Random;

public class LotteryTest {

    public static void main(String args[]) {
        
        try {

        tryYourLuck(10000,10000);

        }
        catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }
    
    public static void tryYourLuck(int numWeeks,int numTickets) {
        for (int week=0;week<numWeeks;week++) {
            Random random = new Random();
            //random number generator 5 x 1-56 and 1x1-46
            int n1 = random.nextInt(56);
            int n2 = random.nextInt(56);
            int n3 = random.nextInt(56);
            int n4 = random.nextInt(56);
            int n5 = random.nextInt(56);
            int n6 = random.nextInt(46);
            String lotteryTicket = "" + n1 + n2 + n3 + n4 + n5 + n6;
            int[][] tickets = generateTicket(numTickets);
            for (int i=0;i<numTickets;i++) {
                String ticket = "";
                for (int j=0;j<6;j++) {
                    ticket += tickets[i][j];
                }
               // System.out.println(ticket);
                if (lotteryTicket.equals(ticket)) {
                    System.out.println("You won on week " + week);
                }
            }
        }
    }
    
    public static int[][] generateTicket(int numTickets) {
        Random random = new Random();
        int[][] tickets = new int[numTickets][];
        for (int i=0;i<numTickets;i++) {
            tickets[i] = new int[]{random.nextInt(56),random.nextInt(56),random.nextInt(56),random.nextInt(56),random.nextInt(56),random.nextInt(46)};
        }
        return tickets;
    }

}
