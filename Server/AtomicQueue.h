//
// Created by erysman on 17.05.16.
//

#ifndef SERVER_BLOCKINGQUEUE_H
#define SERVER_BLOCKINGQUEUE_H

#include <queue>
#include <condition_variable>
#include <mutex>
#include <iostream>
#include <shared_mutex>
#include "Logger.h"

template <typename T>
class AtomicQueue {
public:

    AtomicQueue(std::string moduleName) : logger(moduleName) {}

    AtomicQueue(const AtomicQueue &) = delete;           // disable copying

    AtomicQueue &operator=(const AtomicQueue &) = delete; // disable assignment

    T pop();

    void push(const T &item);

    void pop(T &item);

private:

    std::queue<T> queue;

    std::mutex mutex;

    std::condition_variable condition_variable;

    bool isEmpty();

    Logger logger;
};

template <typename T>
T AtomicQueue<T>::pop() {

    std::unique_lock<std::mutex> lock(mutex);
    while(queue.empty()) {
        condition_variable.wait(lock);
    }
    T item = queue.front();
    logger.log("Popped message from queue");
    queue.pop();
    return item;
}

template <typename T>
void AtomicQueue<T>::pop(T &item) {

    std::unique_lock<std::mutex> lock(mutex);
    while(queue.empty()) {
        condition_variable.wait(lock);
    }
    item = queue.front();
    logger.log("Popped message from queue");
    queue.pop();
}

template <typename T>
void AtomicQueue<T>::push(const T &item) {

    std::unique_lock<std::mutex> lock(mutex);
    queue.push(item);
    logger.log("Pushed message to queue");
    lock.unlock();
    condition_variable.notify_one();
}

template <typename T>
bool AtomicQueue<T>::isEmpty() {

    std::unique_lock<std::mutex> lock(mutex);
    bool val = queue.empty();
    lock.unlock();
    return val;
}

#endif //SERVER_BLOCKINGQUEUE_H