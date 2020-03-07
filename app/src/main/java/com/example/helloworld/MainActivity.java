package com.example.helloworld;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;


public class MainActivity extends AppCompatActivity {
    public static final String mp3url = "https://sharefs.yun.kugou.com/202003071257/9a7cce522e42981a92400c0cbd9a89d6/G003/M01/1F/01/o4YBAFS6S4eAEuG3ADs0MT24rM8722.mp3";
    private MediaPlayer mp = new MediaPlayer();
    ChatClient chatClient = new ChatClient("192.168.0.101", 9999);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        try {
            System.out.println("create mp");
            mp.setDataSource(mp3url);
            mp.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            System.out.println("create socket");
//            this.startNetThread("192.168.0.101", 9999);
            chatClient.start();
            System.out.println("create socket end");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startNetThread(final String host, final int port) {
        new Thread() {
            public void run() {
                try {
                    //创建客户端对象
                    System.out.println("host = " + host);
                    System.out.println("create socket internal");
                    Socket socket = new Socket(host, port);
                    String send_data = "please send cmd";
                    OutputStream outputStream = socket.getOutputStream();//获取客户端对象的输出流
                    outputStream.write(send_data.getBytes());//把内容以字节流的形式写入(data).getBytes();
                    outputStream.flush();//刷新流管道

                    InputStream is = socket.getInputStream(); // 获取 cmd
                    byte[] bytes = new byte[1024];//接收数据
                    int n = is.read(bytes);
                    String cmd_str = new String(bytes, 0, n);
                    System.out.println(cmd_str);
                    System.out.println("打印客户端中的内容：" + socket);

                    TextView txv = (TextView)findViewById(R.id.tv2);
                    txv.setText(cmd_str);

                    //关闭客户端
                    outputStream.close();
                    is.close();
                    socket.close();
                } catch (Exception e) {
                    System.out.println("create socket internalerror");
                    e.printStackTrace();
                }
            }
            //启动线程
        }.start();
    }

    //////////////////////////////////////////////////////////////////////////////

    public class ChatClient extends Thread {
        private String address;
        private int port;
        private Socket socketClient = null;
        private boolean keepConnect = true;
        private InputStream inputs = null;
        private OutputStream outputs = null;
        private MsgReceiver msgrecv = null;

        public ChatClient(String address, int port){
            this.address = address;
            this.port = port;
        }

        public void init() throws IOException {
            socketClient = new Socket(address, port);
            inputs = socketClient.getInputStream();
            outputs = socketClient.getOutputStream();
            msgrecv = new MsgReceiver();
            msgrecv.start();

            new Thread(()->{
                System.out.println("cc constructor++++++++++++++++++++++++++++");
                System.out.println(socketClient.isConnected());
                System.out.println("cc constructor++++++++++++++++++++++++++++2");
            }).start();
        }

        @Override
        public void run(){
            try {
                init();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void send(String str) throws IOException {
            outputs.write(str.getBytes());
            outputs.flush();
        }

        class MsgReceiver extends Thread {
            TextView txv = (TextView)findViewById(R.id.tv2);
            @Override
            public void run(){
                while (keepConnect){
                    try {
                        final byte[] readBuffer = new byte[1024];
                        final int readBufferLen;
                        readBufferLen = inputs.read(readBuffer);
                        txv.setText(new String(readBuffer, 0, readBufferLen));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    //////////////////////////////////////////////////////////////

    static boolean playstate = false;
    public void playmusic(View view) {
        playstate = !playstate;
        Button bt = (Button)findViewById(R.id.btnplay);
        bt.setText(playstate ? "暂停" : "播放");

        Toast.makeText(this, playstate ? "播放《让一切随风》" : "暂停播放《让一切随风》",
                Toast.LENGTH_SHORT).show();

        if(playstate){
            if(!mp.isPlaying())
                mp.start();
        }
        else{
            if(mp.isPlaying())
                mp.pause();
        }

        System.out.println("music is");
        System.out.println(mp.isPlaying());
    }

    public void send(View view) throws IOException {
        TextView txv = (TextView)findViewById(R.id.tv);
        TextInputEditText txi = (TextInputEditText)findViewById(R.id.ti);
        txv.setText(txi.getText());

        new Thread(()->{
            try {
                chatClient.send(txi.getText().toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    static boolean imgstate = true;
    public void showpic(View view) {
        ImageView iv = (ImageView)findViewById(R.id.iv);
        iv.setVisibility(imgstate ? View.VISIBLE : View.INVISIBLE);

        Button bt = (Button)findViewById(R.id.btnpic);
        bt.setText(imgstate ? "隐藏图片" : "显示图片");

        imgstate = !imgstate;
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        if(mp != null){
            mp.stop();
            mp.release();
        }
    }
}
