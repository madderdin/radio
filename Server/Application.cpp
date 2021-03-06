//
// Created by tomasz on 05.05.16.
//

#include <cstdlib>
#include <iostream>
#include "service/streamer/SoundProcessor.h"
#include "tcp/connection/ConnectionManager.h"


int main(int argc, char *arg[]) {

    int port = atoi(arg[1]);
    std::string fileSystemPrefix = std::string(arg[2]);
    Logger logger("Application");

    std::shared_ptr< ClientsMap<int, ClientManager *> > clients = std::make_shared< ClientsMap<int, ClientManager *> >("Clients");

    FileManager fileManager(fileSystemPrefix);
    PlaylistManager playlistManager(fileSystemPrefix);

    Dispatcher *dispatcher = new Dispatcher(fileManager, playlistManager, clients);
    std::thread dispatcherThread = std::thread(&Dispatcher::start, dispatcher);
    logger.log("Dispatcher has been created");

    ConnectionManager connectionManager(dispatcher, port, clients);
    std::thread connectionManagerThread = std::thread(&ConnectionManager::start, &connectionManager);
    logger.log("ConnectionManager has been created");

    SoundProcessor soundProcessor(fileManager, playlistManager, clients);
    std::thread soundProcessorThread = std::thread(&SoundProcessor::stream, &soundProcessor);
    logger.log("SoundProcessor has been created");

    connectionManagerThread.join();
    dispatcherThread.join();
    soundProcessorThread.join();

    delete dispatcher;
    logger.log("Has been closed");
    return 0;
}