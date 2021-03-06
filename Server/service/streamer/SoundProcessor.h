//
// Created by tomasz on 28.05.16.
//

#ifndef SERVER_SOUNDPROCESSOR_H
#define SERVER_SOUNDPROCESSOR_H

#include "../file/FileManager.h"
#include "../playlist/PlaylistManager.h"
#include "../connection/ClientManager.h"


class SoundProcessor {
public:

    SoundProcessor(FileManager &fileManager, PlaylistManager &playlistManager, const std::shared_ptr< ClientsMap<int, ClientManager * > > &clients) :
            fileManager(fileManager), playlistManager(playlistManager), clients(clients), logger(MODULE_NAME){};

    void stream();

    const bool isRunning() const { return running.load(); }

private:

    std::shared_ptr<ClientsMap<int, ClientManager *>> clients;

    FileManager &fileManager;

    PlaylistManager &playlistManager;

    Logger logger;

    std::atomic<bool> running;

    static std::chrono::milliseconds TIME_INTERVAL_MS;

    static int PACKAGE_SIZE_B;

    static std::string MODULE_NAME;

    void divideFile(std::shared_ptr<std::ifstream> fileStream, int songID);

    bool endOf(std::shared_ptr<std::ifstream> fileStream) const;

    void pushStreamData(std::shared_ptr<std::ifstream> fileStream, int songID);

    std::shared_ptr<Data> readFile(std::shared_ptr<std::ifstream> fileStream, int streamData);

    void sleep() const;

    static int ID_SIZE;

    void saveSongID(char *streamData, int songID);
};



#endif //SERVER_SOUNDPROCESSOR_H
