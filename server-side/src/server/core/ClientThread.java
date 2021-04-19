package server.core;

import net.core.SocketThread;
import net.core.SocketThreadListener;
import ru.sur.msc.common.Common;

import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class ClientThread extends SocketThread {

    private static String nickname;
    private boolean isAuthorized;

    public ClientThread(SocketThreadListener listener, String name, Socket socket) {
        super(listener, name, socket);
    }

    public static void setNickname(String nickname) {
        ClientThread.nickname = nickname;
    }

    public static String getNickname() {
        return nickname;
    }

    public boolean isAuthorized() {
        return isAuthorized;
    }

    void authAccept(String nickname) {
        isAuthorized = true;
        this.nickname = nickname;
        ClientThread.setNickname(nickname);
        String encodedString = Base64.getEncoder().encodeToString(Common.getAuthAccept(nickname).getBytes());
        byte[] bytes = Base64.getDecoder().decode(encodedString);
        sendMessage(bytes);
    }

    void authFail() {
        String encodedString = Base64.getEncoder().encodeToString(Common.getAuthDenied().getBytes());
        byte[] bytes = Base64.getDecoder().decode(encodedString);
        sendMessage(bytes);
        close();
    }

    void msgFormatError(String msg) {
        String encodedString = Base64.getEncoder().encodeToString(Common.getMsgFormatError(msg).getBytes());
        byte[] bytes = Base64.getDecoder().decode(encodedString);
        sendMessage(bytes);
        close();
    }

}
