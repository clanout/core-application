package com.clanout.application.library.xmpp;

public class Main
{
    public static void main(String[] args) throws Exception
    {
        XmppLibrary xmppLibrary = new XmppLibrary();

        XmppManager.sendMessage("dummy", "Hello!!");

        xmppLibrary.destroy();
    }
}
