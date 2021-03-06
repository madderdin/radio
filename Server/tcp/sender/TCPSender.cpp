//
// Created by tomasz on 24.05.16.
//

#include <sys/socket.h>
#include "TCPSender.h"
#include "../domain/Header.h"


std::string TCPSender::MODULE_NAME = "TCPSender";

void TCPSender::sendMusic(const char *message, int messageSize) {
    char *header = Header::createHeaderStream(false, false, messageSize);
    sendMessage(header, message, messageSize);
}

void TCPSender::sendVotes(const char *message, int messageSize) {
    char *header = Header::createHeaderList(false, messageSize);
    sendMessage(header, message, messageSize);
}

void TCPSender::sendConnectionInfo(const char *message, int messageSize) {
    char *header = Header::createHeaderConnect();
    sendMessage(header, message, messageSize);
}

void TCPSender::sendMessage(char *header, const char *message, int messageSize) {
    char* data = new char[Header::SIZE + messageSize];
    memcpy(data, header, Header::SIZE);
    memcpy(data + Header::SIZE, message, messageSize);
    writeData(data, Header::SIZE + messageSize);
}

void TCPSender::writeData(char *dataToSend, int dataToSendSize) {
    if(dataToSend != nullptr) {
        sendN(dataToSend, dataToSendSize);
        delete dataToSend;
    }
}

void TCPSender::sendN(const char *dataToSend, int dataToSendSize) {
    while(!isConnectionClosed() && dataToSendSize > 0){
        int bytesSent = (int) send(socketDescriptor, dataToSend, dataToSendSize, MSG_NOSIGNAL);
        if (isClosedByRemote(bytesSent)) {
            logger.log("EPIPE error");
            setConnectionClosed();
        } else {
            dataToSendSize -= bytesSent;
            logger.log(std::to_string(bytesSent));
        }
    }
}

bool TCPSender::isClosedByRemote(int bytesSent) const { return bytesSent < 0 && errno == EPIPE; }

void TCPSender::setConnectionClosed() { connectionClosed = true; }

bool TCPSender::isConnectionClosed() { return connectionClosed.load(); }