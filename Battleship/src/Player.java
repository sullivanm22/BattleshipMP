import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class Player {
	private Socket socket;
	private BufferedReader in;
	private DataOutputStream out;
	public Player opponent;
	public Ship[][] board;
	
	public Player(Socket socket) throws IOException {
		this.socket = socket;
		this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		this.out = new DataOutputStream(socket.getOutputStream());
	}
	public String readLine() throws IOException {
		return in.readLine();
	}
	public void writeLine(String data) throws IOException {
		out.writeBytes(data + "\n");
	}
	public void writeInt(int data) throws IOException {
		out.write(data);
	}
	public boolean readBoolean() throws IOException {
		return in.read() == 1;
	}
	public void close() throws IOException {
		socket.close();
		in.close();
		out.close();
		opponent = null;
		board = null;
	}
	public void sendBoard() throws IOException {
		for (int i = 0; i < 8; i++) {
			for (int j = 0; j < 8; j++) {
				if (board[i][j] == null) {
					writeInt(0);
				} else {
					writeInt(board[i][j].length);
				}
			}
		}
	}
	public boolean isDead() {
		for (Ship[] row: board) {
			for (Ship sq: row) {
				if (sq != null) {
					return false;
				}
			}
		}
		return true;
	}
	
}
