package com.nedap.university;

import java.net.DatagramPacket;

public interface Computer {

    void handlePacket(DatagramPacket packet);

}
