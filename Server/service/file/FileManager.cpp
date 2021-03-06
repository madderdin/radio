//
// Created by tomasz on 28.05.16.
//

#include "FileManager.h"

std::string FileManager::MODULE_NAME = "FileManager";

std::shared_ptr<std::ifstream> FileManager::getFileStream(std::shared_ptr<Song> song) {
    std::shared_ptr<std::ifstream> fileStream = std::make_shared<std::ifstream>();
    fileStream->open(prefix + song->getFileName());
    return fileStream;
}

std::string FileManager::addMusicFile(const char *content, int size, std::string author, std::string title) {
    const std::string fileName = author + title;
    saveNewFile(content, size, author, title, fileName);
    return fileName;
}

void FileManager::saveNewFile(const char *content, int size, const std::string &author, const std::string &title, const std::string &fileName) const {
    std::ofstream newFile(prefix + fileName);
    newFile.write(content + author.size() + title.size() + 1, size - author.size() - title.size() - 1);
    newFile.close();
}

