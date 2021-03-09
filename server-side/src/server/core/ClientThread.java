package server.core;

import net.core.SocketThread;
import net.core.SocketThreadListener;
import ru.sur.msc.common.Common;

import java.net.Socket;

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
        sendMessage(Common.getAuthAccept(nickname));
    }

    void authFail() {
        sendMessage(Common.getAuthDenied());
        close();
    }

    void msgFormatError(String msg) {
        sendMessage(Common.getMsgFormatError(msg));
        close();
    }

}
