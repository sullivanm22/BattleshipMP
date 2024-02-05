import java.io.BufferedReader ;
import java.io.DataOutputStream ;
import java.io.IOException ;
import java.io.InputStreamReader ;
import java.net.Socket ;
import java.util.Arrays ;
import java.util.Scanner ;

/**
 * @author sullivanm22
 * @version 1.0.0 2021-11-21 Initial implementation
 */
public class BattleshipClient
    {

    // holds the server address used to create the socket
    private String serverAddress ;

    /**
     * @param serveradress
     */
    public BattleshipClient( String serveradress )
        {
        this.serverAddress = serveradress ;
        }

    //main method that controls the content of the game
    private void start() throws IOException
        {
        // create the socket
        Socket connectionSocket = new Socket( this.serverAddress, 12345 ) ;
        // create input/output to the server
        DataOutputStream outToServer = new DataOutputStream( connectionSocket.getOutputStream() ) ;
        BufferedReader inFromServer = new BufferedReader( new InputStreamReader( connectionSocket.getInputStream() ) ) ;
        Scanner input = new Scanner( System.in ) ;

        // print a welcome message to the user
        String welcomeMessage = inFromServer.readLine() ;
        System.out.println( welcomeMessage ) ;

        // print a message confirming the opponent is also in the game
        String welcomeOpponent = inFromServer.readLine() ;
        System.out.println( welcomeOpponent ) ;

        // create board
        int[][] board = initializeBoard() ;

        // being the ship placement process
        System.out.println( "It is now time to place your ships!" ) ;
        System.out.println( "The front of the ship will start in the location you choose." ) ;
        System.out.println( "Please type the row and column and then the direction you want your ship to face seperated by spaces. (Example: 4 5 right)" ) ;

        // call placeShip() 5 times once for each ship placement
        placeShip( "carrier", 5, input, inFromServer, outToServer, board ) ;

        placeShip( "battleship", 4, input, inFromServer, outToServer, board ) ;

        placeShip( "cruiser", 3, input, inFromServer, outToServer, board ) ;

        placeShip( "submarine", 3, input, inFromServer, outToServer, board ) ;

        placeShip( "patrol boat", 2, input, inFromServer, outToServer, board ) ;

        // print the starting board to the user
        System.out.println( "Your starting board is:" ) ;
        printBoard( board ) ;

        // wait for the opponent to place their ships before the current player can
        // start sending hits to the server
        System.out.println( "Waiting for opponent to place ships ..." ) ;
        int myPlayerId = inFromServer.read() ;

        while ( true )
            {

            // Take turn if first player
            if ( myPlayerId == 1 )
                {
                takeTurn( input, outToServer, inFromServer ) ;
                }
            // Opponent takes turn
            System.out.println( "It is now your opponent's turn." ) ;
            String opponentsHit = inFromServer.readLine() ;
            System.out.println( opponentsHit ) ;

            // Update and display your current board
            updateBoard( board, inFromServer ) ;
            System.out.println( "Here is your board: " ) ;
            printBoard( board ) ;

            // Take turn if second player
            if ( myPlayerId == 2 )
                {
                takeTurn( input, outToServer, inFromServer ) ;
                }
            // End-of-round cleanup

            // Either "Next Round", "You Won", or "You Lost"
            String endRound = inFromServer.readLine() ;
            System.out.println( endRound ) ;
            // Now the server tells the clients whether the game is over.
            if ( inFromServer.read() != 0 )
                {
                return ;
                }
            }// end while
        }// end start()


    public static void main( String[] args ) throws Exception
        {
        // create a client and set the address to localhost
        BattleshipClient client = new BattleshipClient( "localhost" ) ;
        client.start() ;
        }


    /**
     * initialize a 2-dimensional array with all starting elements set to 0
     *
     * @return board
     */
    public int[][] initializeBoard()
        {
        // this will create a 2Dimensional array of integers and populate the array
        // with all 0's and print its initialized state to the user
        int[][] board = new int[ 8 ][ 8 ] ;
        for ( int row = 0 ; row < board.length ; row++ )
            {
            for ( int col = 0 ; col < board[ row ].length ; col++ )
                {
                board[ row ][ col ] = 0 ;
                }
            }
        System.out.println( "Here is your starting empty board:" ) ;
        printBoard( board ) ;
        return board ;
        }


    /**
     * gets the current board from the server with updated positions by setting the
     * current boards elements to the new elements sent from the server.
     *
     * @param board
     * @param inFromServer
     * @throws IOException
     */
    public void updateBoard( int[][] board,
                             BufferedReader inFromServer )
        throws IOException
        {
        for ( int row = 0 ; row < board.length ; row++ )
            {
            for ( int col = 0 ; col < board[ row ].length ; col++ )
                {
                board[ row ][ col ] = inFromServer.read() ;
                }
            }
        }


    /**
     * place the ship in the desired position and send it to the server
     *
     * @param name
     * @param length
     * @param consoleInput
     * @param inFromServer
     * @param outToServer
     * @param board
     * @throws IOException
     */
    public void placeShip( String name,
                           int length,
                           Scanner consoleInput,
                           BufferedReader inFromServer,
                           DataOutputStream outToServer,
                           int[][] board )
        throws IOException
        {
        while ( true )
            {
            System.out.printf( "Input your %s(%d) ship placement: ", name, length ) ;
            String placement = consoleInput.nextLine() ;
            outToServer.writeBytes( placement + "\n" ) ;
            int result = inFromServer.read() ;
            if ( result == 1 )
                { // Valid
                updateBoard( board, inFromServer ) ;
                printBoard( board ) ;
                return ;
                }
            else
                { // Invalid
                System.out.println( inFromServer.readLine() ) ;
                }
            }// end while
        }


    /**
     * print the board to the console
     *
     * @param board
     */
    public static void printBoard( int[][] board )
        {

        for ( int i = 0 ; i < board.length ; i++ )
            {
            for ( int j = 0 ; j < board[ i ].length ; j++ )
                {
                System.out.print( board[ i ][ j ] + "  " ) ;
                }
            System.out.println() ;
            }
        }


    /**
     * controls the turn for each player
     *
     * @param input
     * @param outToServer
     * @param inFromServer
     * @throws IOException
     */
    public static void takeTurn( Scanner input,
                                 DataOutputStream outToServer,
                                 BufferedReader inFromServer )
        throws IOException
        {
        System.out.println( "It is now your turn." ) ;
        while ( true )
            {
            System.out.println( "Choose a location to hit on the opponents board. (Format: 2 7)" ) ;
            String hit = input.nextLine() ;
            outToServer.writeBytes( hit + "\n" ) ;
            int suceeded = inFromServer.read() ;
            if ( suceeded != 0 )
                {
                break ;
                }
            System.out.println( "Bad guess format." ) ;
            }// end while
        String yourHit = inFromServer.readLine() ;
        System.out.println( yourHit ) ;
        }
    }

// end class Client
