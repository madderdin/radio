//
// Created by erysman on 18.05.16.
//

#ifndef SERVER_CLIENTMANAGER_H
#define SERVER_CLIENTMANAGER_H


#include "Dispatcher.h"

#include "Sender.h"
#include "SocketListener.h"

class ClientManager {
public:
    ClientManager(Dispatcher* dispatcher, int newSocketDescriptor);
    ~ClientManager();

    void handle();

    void read();

private:
    SocketListener *socketListener;

    std::thread *socketListnerThread;
    Sender *sender;

    std::thread *senderThread;

    int socketDescriptor;

    void deleteClient();

    void log(const char message[38]) const;
};


#endif //SERVER_CLIENTMANAGER_H
