import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
	@SuppressWarnings("resource")
	public static void main(String[] args) throws IOException {
		ServerSocket serverSocket = new ServerSocket(12345);

		System.out.println("This server is ready to receive");
		
		Player firstPlayer = null;
		while (true) {
			Socket thisSocket = serverSocket.accept();
			Player thisPlayer = new Player(thisSocket);
			if (firstPlayer != null) {
				Player myFirstPlayer = firstPlayer;
				try {
					firstPlayer.writeLine("Player 2 has connected");
				} catch (IOException e) {
					// First player disconnected before second player joined
					// so ignore the previous first player and the second player becomes
					// the new first player.
					firstPlayer.close();
					thisPlayer.writeLine("Welcome, Player 1");
					firstPlayer = thisPlayer;
					continue;
				}
				// Second player joined, start a thread to handle the game and reset
				new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							startGame(myFirstPlayer, thisPlayer);
						} catch (IOException e) {
							throw new UncheckedIOException(e);
						}
					}
				}).start();
				firstPlayer = null;
			} else {
				// First player joined
				firstPlayer = thisPlayer;
				firstPlayer.writeLine("Welcome, Player 1");
			}
		}
	}
	public static void startGame(Player firstPlayer, Player secondPlayer) throws IOException {
		secondPlayer.writeLine("Welcome, Player 2");
		secondPlayer.writeLine("Player 1 has Already Connected");

		// Read both players' boards simultaneously
		Thread fpBoard = new Thread( new Runnable() {
			@Override
			public void run() {
				try {
					readBoard(firstPlayer);
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			}
		});
		Thread spBoard = new Thread( new Runnable() {
			@Override
			public void run() {
				try {
					readBoard(secondPlayer);
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			}
		});
		fpBoard.start();
		spBoard.start();
		
		// Now wait for both boards to be handled before moving on to guessing
		try {
			fpBoard.join();
			spBoard.join();
		} catch (InterruptedException e) {
			// Shouldn't happen
		}
		
        // Tells people the game has begun
        firstPlayer.writeInt(1);
        secondPlayer.writeInt(2);
        
		firstPlayer.opponent = secondPlayer;
		secondPlayer.opponent = firstPlayer;
		
		Player activePlayer = firstPlayer;
		
		while (true) {
			
			String[] chosen = activePlayer.readLine().split(" ");
			if (chosen.length != 2) {
				activePlayer.writeInt(0); // 0 = invalid, 1 = valid
				continue;
			}
			int row, col;
			try {
				row = Integer.parseInt(chosen[0]);
				col = Integer.parseInt(chosen[1]);
			} catch (NumberFormatException e) {
				activePlayer.writeInt(0); // 0 = invalid, 1 = valid
				continue;
			}
			activePlayer.writeInt(1);
			Ship[][] board = activePlayer.opponent.board;
			Ship target = null;
			try {
				target = board[row][col];
			} catch (ArrayIndexOutOfBoundsException e) {
				// Oh well, their shot missed the board entirely
				// No need to do anything
			}
			if (target == null) {
				activePlayer.writeLine("You miss!");
				activePlayer.opponent.writeLine("Your opponent missed!");
			} else {
				board[row][col] = null;
				// Check for any other squares on the board that have the same ship
				boolean sunk = true;
				for (int i = 0; i < 8; i++) {
					for (int j = 0; j < 8; j++) {
						if (board[i][j] == target) {
							sunk = false;
							break;
						}
					}
				}
				if (sunk) {
					activePlayer.writeLine("You hit and sunk!");
					activePlayer.opponent.writeLine("Your opponent sunk your ship!");
				} else {
					activePlayer.writeLine("You hit!");
					activePlayer.opponent.writeLine("Your opponent hit your ship!");
				}
			}
			activePlayer = activePlayer.opponent;
			activePlayer.sendBoard();
			if (activePlayer == firstPlayer) {
				if (firstPlayer.isDead()) {
					firstPlayer.writeLine("You lost!");
					secondPlayer.writeLine("You won!");
				} else if (secondPlayer.isDead()) {
					firstPlayer.writeLine("You won!");
					secondPlayer.writeLine("You lost!");
				} else {
					firstPlayer.writeLine("Next Round");
					secondPlayer.writeLine("Next Round");
					// "0" = "Game should continue"
					firstPlayer.writeInt(0);
					secondPlayer.writeInt(0);
					continue;
				}
				// "1" = "Game should end"
				firstPlayer.writeInt(1);
				secondPlayer.writeInt(1);
				break;
			}
		}
		firstPlayer.close();
		secondPlayer.close();
	}
	public static void readBoard(Player socket) throws IOException {
		socket.board = new Ship[8][8];
		int[] shipSizes = new int[] {5, 4, 3, 3, 2};
		for (int shipIdx = 0; shipIdx < shipSizes.length; shipIdx++) {
			int shipSize = shipSizes[shipIdx];
			Ship myShip = new Ship(shipSize);
			
		    System.out.println( "reading for "+ shipSize ) ;
			String[] placement = socket.readLine().split(" ");
			String invalid_msg = null;
			int row = -1, col = -1;
			String dir = null;
			boolean valid = true;
			
			if (placement.length == 3) {
				System.out.println( "got " + placement[0] + " " + placement[1] + " " + placement[2]);
				try {
					row = Integer.parseInt(placement[0]);
					col = Integer.parseInt(placement[1]);
				} catch (NumberFormatException e) {
					invalid_msg = "Row or column was not numeric.";
					valid = false;
				}
				dir = placement[2];
			} else {
				invalid_msg = "Could not understand ship placement, please try again.";
				valid = false;
			}
			// Validation pass
			if (invalid_msg == null) {
				try {
					for (int i = 0; i < shipSize; i++) {
						switch(dir) {
						case "up":
							valid = valid && (socket.board[row - i][col] == null);
							break;
						case "down":
							valid = valid && (socket.board[row + i][col] == null);
							break;
						case "left":
							valid = valid && (socket.board[row][col - i] == null);
							break;
						case "right":
							valid = valid && (socket.board[row][col + i] == null);
							break;
						default:
							valid = false;
							invalid_msg = "Unrecognized direction.";
						}
					}
					if (!valid && invalid_msg == null) {
						invalid_msg = "Ship overlaps existing ships.";
					}
				} catch (ArrayIndexOutOfBoundsException e) {
					valid = false;
					invalid_msg = "Ship exceeds bounds of board.";
				}
			}
			if (!valid) {
				socket.writeInt(-1);
				socket.writeLine(invalid_msg);
				shipIdx--;
				continue;
			}
			socket.writeInt(1);
			// Board update pass
			for (int i = 0; i < shipSize; i++) {
				switch(dir) {
					case "up":
						socket.board[row - i][col] = myShip;
						break;
					case "down":
						socket.board[row + i][col] = myShip;
						break;
					case "left":
						socket.board[row][col - i] = myShip;
						break;
					case "right":
						socket.board[row][col + i] = myShip;
						break;
				}
			}
			socket.sendBoard();
		}
	}
}
