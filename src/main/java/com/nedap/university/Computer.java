package com.nedap.university;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

public interface Computer {

    void handlePacket(DatagramPacket packet);

}
