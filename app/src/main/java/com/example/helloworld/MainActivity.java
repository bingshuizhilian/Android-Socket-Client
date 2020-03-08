package com.example.helloworld;

import android.annotation.SuppressLint;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;


public class MainActivity extends AppCompatActivity {
    public static final String mp3url = "http://198.178.123.14:8216/";
    private MediaPlayer mp;
    ChatClient chatClient;
    Handler handler;
    TextView sendtv;
    TextInputEditText inputtxi;
    TextView recvtv;

    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sendtv = (TextView)findViewById(R.id.tv);
        inputtxi = (TextInputEditText)findViewById(R.id.ti);
        recvtv = (TextView)findViewById(R.id.tv2);

        handler = new Handler(){
            @Override
            public void handleMessage(Message msg){
                System.out.println("+++++++++++++++receive from server: " + msg.obj.toString());
                if(msg.what == 0x123)
                {
                    if(parseCmd(msg.obj.toString()) < 0)
                        recvtv.setText(msg.obj.toString());
                }
            }
        };

        try {
            System.out.println("create mp");
            mp = new MediaPlayer();
            mp.setDataSource(mp3url);
            mp.prepare();
//            startNetThread("192.168.0.101", 9999);
            chatClient = new ChatClient("192.168.0.101", 9999, handler);
            new Thread(chatClient).start();
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

    public int parseCmd(String cmd) {
        int retcode = -1;

        if(cmd.equals(new String("toast"))){
            Toast.makeText(this, "server request toast", Toast.LENGTH_SHORT).show();
        }
        else if(cmd.equals(new String("play"))){
            if(!mp.isPlaying())
                mp.start();

            retcode = 1;
        }

        return retcode;
    }

    static boolean playstate = false;
    public void playmusic(View view) {
        playstate = !playstate;
        Button bt = (Button)findViewById(R.id.btnplay);
        bt.setText(playstate ? "暂停" : "播放");

        Toast.makeText(this, playstate ? "播放网络电台" : "暂停播放网络电台",
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
        try {
            if(!inputtxi.getText().toString().isEmpty()) {
                System.out.println("+++++++++++++++send from client: " + inputtxi.getText().toString());
                sendtv.setText(inputtxi.getText());
                Message msg = new Message();
                msg.what = 0x345;
                msg.obj = inputtxi.getText().toString();
                chatClient.recvhandler.sendMessage(msg);
                inputtxi.setText("");
            }
        } catch (Exception e){
            e.printStackTrace();
        }
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

//////////////////////////////////////////////////////////////////////////////

class ChatClient implements Runnable{
    private String address;
    private int port;
    private Handler handler;
    public Handler recvhandler;
    private Socket socketClient = null;
    BufferedReader bufferedReader = null;
    InputStream inputs = null;
    OutputStream outputs = null;

    public ChatClient(String address, int port, Handler handler){
        this.address = address;
        this.port = port;
        this.handler = handler;

        Linker linker = new Linker();
        linker.start();
    }

    @SuppressLint("HandlerLeak")
    public void run(){
        try {
            Looper.prepare();
            recvhandler = new Handler(){
                @Override
                public void handleMessage(Message msg){
                    System.out.println("+++++++++++++++send to server: " + msg.obj.toString());
                    if(msg.what == 0x345){
                        send(msg.obj.toString());
                    }
                }
            };
            Looper.loop();
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    class Linker extends Thread{
        @Override
        public void run(){
            try {
                socketClient = new Socket(address, port);
                bufferedReader = new BufferedReader(new InputStreamReader(socketClient.getInputStream()));
                inputs = socketClient.getInputStream();
                outputs = socketClient.getOutputStream();

                MsgReceiver msgReceiver = new MsgReceiver();
                msgReceiver.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void send(String str){
        try {
            outputs.write(str.getBytes());
            outputs.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    class MsgReceiver extends Thread {
        final byte[] readBuffer = new byte[1024];
        int readBufferLen = 0;

        @Override
        public void run(){
            while (true){
                try {
                    readBufferLen = inputs.read(readBuffer);
                    Message msg = new Message();
                    msg.what = 0x123;
                    msg.obj = new String(readBuffer, 0, readBufferLen);
                    handler.sendMessage(msg);
                    System.out.println("+++++++++++++++client receive: " + msg.obj.toString());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        //bufferedReader.readLine()要求接收到的数据必须以'\n'结尾，需要服务器配合
//        public void run() {
//            String content = null;
//            try {
//                while ((content = bufferedReader.readLine()) != null) {
//                    System.out.println("+++++++++++++++client receive: " + content);
//                    Message msg = new Message();
//                    msg.what = 0x123;
//                    msg.obj = content;
//                    handler.sendMessage(msg);
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
    }

}

//////////////////////////////////////////////////////////////
