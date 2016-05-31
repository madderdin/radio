//
// Created by erysman on 18.05.16.
//

#ifndef SERVER_CLIENTMANAGER_H
#define SERVER_CLIENTMANAGER_H


#include "Sender.h"
#include "SocketListener.h"
#include "AtomicMap.h"
#include "Dispatcher.h"
#include <thread>

class Dispatcher;

class SocketListener;

class Sender;

class ClientManager {
public:

    ClientManager(const std::shared_ptr<Dispatcher> &dispatcher, int newSocketDescriptor);

    ~ClientManager();

    void handle(const std::shared_ptr<AtomicMap<int, ClientManager *>> &blockingMap);

    void send(Data *data);

    void notifyListner();

    int getSocketDescriptor() const { return socketDescriptor; }

    void addReadRequest();

    void setNoReadRequests();

private:

    void deleteClient();

    SocketListener *socketListener;

    Sender *sender;

    int socketDescriptor;

    static std::string MODULE_NAME;

    void registerThread(const std::shared_ptr<AtomicMap<int, ClientManager *>> &blockingMap);

    void unregisterThread(const std::shared_ptr<AtomicMap<int, ClientManager *>> &blockingMap);

    Logger *logger;
};


#endif //SERVER_CLIENTMANAGER_H
