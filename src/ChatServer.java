import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

public class ChatServer extends JFrame {

	private JPanel contentPane;


	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					ChatServer frame = new ChatServer();
					frame.setVisible(true);


					frame.listen(55555);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	public ChatServer() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 450, 300);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BorderLayout(0, 0));
		setContentPane(contentPane);

	}



	private void listen(int port) {
		Thread th = new Thread() {			// 待ち受け用スレッド
			private ServerSocket server;			// 待ち受け用ソケット

			public void run() {
				try {
					server = new ServerSocket(port);
					while( true ) {
						Socket socket = server.accept();
						System.out.println("accept");

						// 通信用クラスの作成
						Communication comm = new Communication(socket);
						CommunicationManager.getInstance().addMember(comm);
						comm.start();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		};
		th.start();
	}

}


// 通信管理クラス
class CommunicationManager {

	// インスタンス
	private static CommunicationManager instance = new CommunicationManager();

	// チャットメンバー
	private List<Communication> chatMembers;



	private  CommunicationManager(){
		chatMembers = new ArrayList<Communication>();
	}

	// インスタンス取得
	public static CommunicationManager getInstance(){
		return instance;
	}


	// 全員にメッセージを送信する
	// ※今回は個別のメッセージ送信は不要
	public void sendMessageAllMember(byte [] byteMessage){

		// 全員に通知を送る
		for (Communication comm : chatMembers){
			comm.sendMessage(byteMessage);
		}
	}


	// メンバーを追加
	public void addMember(Communication member){
		chatMembers.add(member);
	}

	// メンバーから外す
	public void removeMember(Communication member){
		chatMembers.remove(member);
	}
}



class Communication extends Thread {

	// ヘッダ仕様
	public static final byte HEADER_SEND_NAME 				= 1;
	public static final byte HEADER_DISCONNECT 				= 2;
	public static final byte HEADER_INFORM_CONNECTION		= 3;
	public static final byte HEADER_INFORM_DISCONNECTION	= 4;
	public static final byte HEADER_TEXT 					= 5;
	public static final byte HEADER_STAMP 					= 6;

	private Socket socket;		// クライアントとの通信用
	private String userName;	// ユーザ名


	// コンストラクタ
	public Communication(Socket socket) {
		this.socket = socket;
	}


	// Getter
	public String getUserName(){return userName;}


	// メッセージ送信
	public void sendMessage(byte [] byteMessage) {
		BufferedOutputStream out;
		try {
			out = new BufferedOutputStream(socket.getOutputStream());

			out.write(byteMessage);
			out.flush();

			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	public void run() {
		try {
			BufferedInputStream in = new BufferedInputStream(socket.getInputStream());

			// 受信用ループ
			while( true ) {

				// 先頭1byteはヘッダ
				byte [] byteHeader = new byte [1];

				in.read(byteHeader);


				// 名前が送られてきた
				if (byteHeader[0] == HEADER_SEND_NAME){

					byte [] byteData = new byte [255];
					ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
					byteBuffer.clear();

					// 送信データをすべて読む
					while (in.read(byteData) != -1){
						byteBuffer.put(byteData);
					}

					// 名前のセット
					userName = new String(byteBuffer.array(), "UTF-8");

					System.out.println("接続：" + userName + "さんが参加");
				}


				// 切断要求
				else if (byteHeader[0] == HEADER_DISCONNECT){

					// メンバーから外す
					CommunicationManager.getInstance().removeMember(this);

					ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
					byteBuffer.clear();
					byteBuffer.put(HEADER_INFORM_DISCONNECTION);	// ヘッダの格納
					byteBuffer.put(("" + userName + "さんが退出しました").getBytes("UTF-8"));

					// 全員に通知を送る
					CommunicationManager.getInstance().sendMessageAllMember(byteBuffer.array());

					// ループを抜けて終了処理
					break;
				}


				// テキストメッセージ
				else if (byteHeader[0] == HEADER_TEXT){

					byte [] byteData = new byte [255];
					ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
					byteBuffer.clear();

					// 送信データをすべて読む
					while (in.read(byteData) != -1){
						byteBuffer.put(byteData);
					}

					// 全員に転送
					CommunicationManager.getInstance().sendMessageAllMember(byteBuffer.array());
				}

				// スタンプ
				else if (byteHeader[0] == HEADER_STAMP){

					byte [] byteData = new byte [255];
					ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
					byteBuffer.clear();

					// 送信データをすべて読む
					while (in.read(byteData) != -1){
						byteBuffer.put(byteData);
					}

					// 全員に転送
					CommunicationManager.getInstance().sendMessageAllMember(byteBuffer.array());
				}
			}

			// 入力バッファを閉じる
			in.close();

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}



	// 次は実際にデバッグをしてみよう
}


