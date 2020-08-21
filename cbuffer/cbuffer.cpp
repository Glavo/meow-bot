#include <cstdio>
#include <cstring>
#include <unistd.h>
#include <cstdlib>
#include <sys/stat.h>
#include <sys/types.h>
#include <fcntl.h>
#include <atomic>
#include <mutex>
#include <thread>
#include <condition_variable>
#include <deque>
#include <utility>


static std::mutex              d_mutex{};
static std::condition_variable d_condition{};
static std::deque<std::pair<char *, ssize_t>>      d_queue{};

static void queue_add(char * value, ssize_t n) {
    {
        std::unique_lock<std::mutex> lock(d_mutex);
        d_queue.push_front(std::pair<char *, ssize_t>(value, n));
    }
    d_condition.notify_one();
}

static std::pair<char *, ssize_t> queue_take() {
    std::unique_lock<std::mutex> lock(d_mutex);
    d_condition.wait(lock, []{ return !d_queue.empty(); });
    auto rc = d_queue.back();
    d_queue.pop_back();
    return rc;
}


static std::atomic_bool c;

int main(int argc, char *argv[]) {
    if(argc < 2) {
        fprintf(stderr, "too few arguments\n");
        return -1;
    }
    if(argc > 2) {
        fprintf(stderr, "too many arguments\n");
        return -1;
    }
    
    c = true;

    int fd = open(argv[1], O_WRONLY);
    if(fd == -1) {
        fprintf(stderr, "open file failure\n");
        return -1;
    }

    std::thread t([fd]() {
        while (true)
        {   
            if(!c && d_queue.empty()) {
                break;
            }
            auto str = queue_take();
            write(fd, str.first, str.second);
            free(str.first);
        }
        
    });
    t.detach();

    while (true)
    {
        char *str = nullptr;
        size_t n = 0;
        ssize_t num;
        if((num = getline(&str, &n, stdin)) == -1) {
            c = false;
            return 0;
        }
        queue_add(str, num);
    }
    return 0;
}